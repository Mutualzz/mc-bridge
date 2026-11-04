package com.mutualzz.bridge.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.JsonObject;
import com.mutualzz.bridge.BridgeSocketClient;
import com.mutualzz.bridge.MutualzzBridgePlugin;

public class InventoryWatcher {
  private static final long LEASE_MS = 30000;
  private static final int BACKUP_INTERVAL_SECONDS = 300;
  private static final long MIN_BACKUP_GAP_MS = 30000;

  private final MutualzzBridgePlugin plugin;
  private final BridgeSocketClient client;
  private final Map<UUID, Long> leases = new HashMap<>();
  private final Map<UUID, String> lastSent = new HashMap<>();
  private final Map<UUID, Integer> lastBackupHash = new HashMap<>();
  private final Map<UUID, Long> lastBackupAt = new HashMap<>();
  private final Set<UUID> dirty = new HashSet<>();
  private long tickCount = 0;
  private int task = -1;

  public InventoryWatcher(MutualzzBridgePlugin plugin, BridgeSocketClient client) {
    this.plugin = plugin;
    this.client = client;
  }

  public void markDirty(UUID uuid) {
    dirty.add(uuid);
  }

  public void watch(UUID uuid) {
    boolean isNew = leases.put(uuid, System.currentTimeMillis() + LEASE_MS) == null;
    if (isNew)
      lastSent.remove(uuid);
  }

  public void start() {
    stop();
    task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 20L);
  }

  public void stop() {
    if (task != -1) {
      Bukkit.getScheduler().cancelTask(task);
      task = -1;
    }

    leases.clear();
    lastSent.clear();
    lastBackupHash.clear();
    dirty.clear();
    tickCount = 0;
  }

  private void tick() {
    if (!client.isReady())
      return;

    long now = System.currentTimeMillis();
    leases.entrySet().removeIf(entry -> {
      if (entry.getValue() >= now)
        return false;
      lastSent.remove(entry.getKey());
      return true;
    });

    for (UUID uuid : leases.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player == null)
        continue;

      JsonObject snapshot = InventorySnapshot.of(player);
      String json = snapshot.toString();
      if (json.equals(lastSent.get(uuid)))
        continue;

      lastSent.put(uuid, json);
      client.sendInventory(snapshot);
    }

    tickCount++;
    backupOnlinePlayers();
  }

  private void backupOnlinePlayers() {
    long now = System.currentTimeMillis();
    int turn = (int) (tickCount % BACKUP_INTERVAL_SECONDS);

    lastBackupHash.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    lastBackupAt.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    dirty.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

    for (Player player : Bukkit.getOnlinePlayers()) {
      UUID uuid = player.getUniqueId();
      if (leases.containsKey(uuid))
        continue;

      boolean scheduled = Math.floorMod(uuid.hashCode(), BACKUP_INTERVAL_SECONDS) == turn;
      boolean changed = dirty.contains(uuid)
          && now - lastBackupAt.getOrDefault(uuid, 0L) >= MIN_BACKUP_GAP_MS;
      if (!scheduled && !changed)
        continue;

      dirty.remove(uuid);
      lastBackupAt.put(uuid, now);

      JsonObject snapshot = InventorySnapshot.of(player);
      int hash = snapshot.toString().hashCode();
      if (Integer.valueOf(hash).equals(lastBackupHash.get(uuid)))
        continue;

      lastBackupHash.put(uuid, hash);
      client.sendInventory(snapshot);
    }
  }

}
