package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public final class GameEventListener implements Listener {
  private final MutualzzBridgePlugin plugin;

  public GameEventListener(MutualzzBridgePlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDeath(PlayerDeathEvent event) {
    send("death", event.getEntity(), event.deathMessage());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onAdvancement(PlayerAdvancementDoneEvent event) {
    send("advancement", event.getPlayer(), event.message());
  }

  private void send(String kind, Player player, Component message) {
    if (message == null)
      return;
    var client = plugin.getBridgeClient();
    if (client == null || !client.isReady())
      return;

    String text = PlainTextComponentSerializer.plainText().serialize(message);
    if (text.isBlank())
      return;
    client.sendGameEvent(kind, player.getUniqueId(), player.getName(), text);
  }
}
