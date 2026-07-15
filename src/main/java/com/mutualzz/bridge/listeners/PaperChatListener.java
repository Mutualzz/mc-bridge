package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Paper Adventure chat (1.19+). Registered only when {@code AsyncChatEvent} exists.
 */
public final class PaperChatListener implements Listener {
    private final MutualzzBridgePlugin plugin;

    public PaperChatListener(MutualzzBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        var client = plugin.getBridgeClient();
        if (client == null || !client.isReady()) return;

        String content = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (content.isBlank()) return;

        var player = event.getPlayer();
        client.sendChat(player.getUniqueId(), player.getName(), content);
    }
}
