package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

public final class InventoryChangeListener implements Listener {
  private final MutualzzBridgePlugin plugin;

  public InventoryChangeListener(MutualzzBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSlotChange(PlayerInventorySlotChangeEvent event) {
    markDirty(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClose(InventoryCloseEvent event) {
    if (event.getInventory().getType() != InventoryType.ENDER_CHEST)
      return;
    if (event.getPlayer() instanceof Player player)
      markDirty(player);
  }

  private void markDirty(Player player) {
    var client = plugin.getBridgeClient();
    if (client == null)
      return;
    client.getInventoryWatcher().markDirty(player.getUniqueId());
  }
}
