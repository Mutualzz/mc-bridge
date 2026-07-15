package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Bukkit legacy chat — used on Paper/Spigot before Adventure {@code AsyncChatEvent}
 * (e.g. 1.18.2) and as a fallback when Paper chat is unavailable.
 */
public final class LegacyChatListener implements Listener {
    private final MutualzzBridgePlugin plugin;

    public LegacyChatListener(MutualzzBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        var client = plugin.getBridgeClient();
        if (client == null || !client.isReady()) return;

        String content = event.getMessage();
        if (content == null || content.isBlank()) return;

        var player = event.getPlayer();
        client.sendChat(player.getUniqueId(), player.getName(), content);
    }
}
