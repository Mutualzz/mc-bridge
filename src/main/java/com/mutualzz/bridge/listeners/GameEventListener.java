package com.mutualzz.bridge.listeners;

import com.google.gson.JsonObject;
import com.mutualzz.bridge.MutualzzBridgePlugin;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.advancement.Advancement;
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
    send("death", event.getEntity(), event.deathMessage(), null);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onAdvancement(PlayerAdvancementDoneEvent event) {
    send("advancement", event.getPlayer(), event.message(), describe(event.getAdvancement()));
  }

  private JsonObject describe(Advancement advancement) {
    AdvancementDisplay display = advancement.getDisplay();
    if (display == null)
      return null;

    var plainText = PlainTextComponentSerializer.plainText();
    JsonObject details = new JsonObject();
    details.addProperty("title", plainText.serialize(display.title()));
    details.addProperty("description", plainText.serialize(display.description()));
    details.addProperty("frame", display.frame().name().toLowerCase(Locale.ROOT));
    var icon = display.icon();
    if (icon != null)
      details.addProperty("icon", icon.getType().getKey().toString());
    return details;
  }

  private void send(String kind, Player player, Component message, JsonObject advancement) {
    if (message == null)
      return;
    var client = plugin.getBridgeClient();
    if (client == null || !client.isReady())
      return;

    String text = PlainTextComponentSerializer.plainText().serialize(message);
    if (text.isBlank())
      return;
    client.sendGameEvent(kind, player.getUniqueId(), player.getName(), text, advancement);
  }
}
