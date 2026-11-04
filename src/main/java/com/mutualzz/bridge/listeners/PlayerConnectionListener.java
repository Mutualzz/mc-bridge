package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import com.mutualzz.bridge.inventory.InventorySnapshot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {
  private final MutualzzBridgePlugin plugin;

  public PlayerConnectionListener(MutualzzBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var client = plugin.getBridgeClient();
    if (client == null || !client.isReady())
      return;
    var player = event.getPlayer();
    client.sendJoin(player.getUniqueId(), player.getName());
    client.getInventoryWatcher().watch(player.getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var client = plugin.getBridgeClient();
    var player = event.getPlayer();
    if (client == null || !client.isReady())
      return;

    if (client.isInventorySyncEnabled()) {
      client.sendInventory(InventorySnapshot.of(player));
    }
    client.sendVoiceLeave(player.getUniqueId(), player.getName());
    client.sendLeave(player.getUniqueId(), player.getName());
  }
}
