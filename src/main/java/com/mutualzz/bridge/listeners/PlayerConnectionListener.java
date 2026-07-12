package com.mutualzz.bridge.listeners;

import com.mutualzz.bridge.MutualzzBridgePlugin;
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
        if (client == null || !client.isReady()) return;
        var player = event.getPlayer();
        client.sendJoin(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var client = plugin.getBridgeClient();
        var player = event.getPlayer();
        if (client == null || !client.isReady()) return;
        client.sendVoiceLeave(player.getUniqueId(), player.getName());
        client.sendLeave(player.getUniqueId(), player.getName());
    }
}
