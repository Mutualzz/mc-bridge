package com.mutualzz.bridge;

import com.mutualzz.bridge.commands.LinkCommand;
import com.mutualzz.bridge.commands.VoiceCommand;
import com.mutualzz.bridge.listeners.ChatListener;
import com.mutualzz.bridge.listeners.PlayerConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MutualzzBridgePlugin extends JavaPlugin {
    private BridgeSocketClient bridgeClient;
    private VoicePluginChannel voiceChannel;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String hubUrl = getConfig().getString("hubUrl", "wss://bridge.mutualzz.com");
        String token = getConfig().getString("token", "");
        String serverId = getConfig().getString("serverId", "survival");

        if (token == null || token.isBlank()) {
            getLogger().severe("No token set in config.yml — get one from Mutualzz (create a bridge).");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        voiceChannel = new VoicePluginChannel(this);
        voiceChannel.register();

        bridgeClient = new BridgeSocketClient(this, hubUrl, token, serverId);
        bridgeClient.connect();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        var link = getCommand("mzlink");
        if (link != null) {
            link.setExecutor(new LinkCommand(this));
        }

        var voice = getCommand("mzvoice");
        if (voice != null) {
            VoiceCommand voiceCommand = new VoiceCommand(this);
            voice.setExecutor(voiceCommand);
            voice.setTabCompleter(voiceCommand);
        }

        getLogger().info("Mutualzz Bridge enabled (serverId=" + serverId + ")");
    }

    @Override
    public void onDisable() {
        if (bridgeClient != null) {
            bridgeClient.shutdown();
        }
        if (voiceChannel != null) {
            voiceChannel.unregister();
        }
    }

    public BridgeSocketClient getBridgeClient() {
        return bridgeClient;
    }

    public VoicePluginChannel getVoiceChannel() {
        return voiceChannel;
    }
}
