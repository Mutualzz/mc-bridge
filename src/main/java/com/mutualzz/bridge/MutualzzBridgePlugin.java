package com.mutualzz.bridge;

import com.mutualzz.bridge.commands.LinkCommand;
import com.mutualzz.bridge.commands.VoiceCommand;
import com.mutualzz.bridge.listeners.LegacyChatListener;
import com.mutualzz.bridge.listeners.PaperChatListener;
import com.mutualzz.bridge.listeners.PlayerConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class MutualzzBridgePlugin extends JavaPlugin {
    private BridgeSocketClient bridgeClient;
    private VoicePluginChannel voiceChannel;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!ServerCompat.meetsMinimumMinecraft()) {
            getLogger().severe(
                    "Mutualzz Bridge requires Minecraft/Paper 1.18.2 or newer (detected "
                            + ServerCompat.describeMinecraftVersion()
                            + "). Disabling."
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        registerChatListener();

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

        getLogger().info(
                "Mutualzz Bridge enabled (serverId="
                        + serverId
                        + ", mc="
                        + ServerCompat.describeMinecraftVersion()
                        + ")"
        );
    }

    private void registerChatListener() {
        if (ServerCompat.hasPaperAsyncChat()) {
            getServer().getPluginManager().registerEvents(new PaperChatListener(this), this);
            getLogger().info("Chat bridge: Paper AsyncChatEvent");
        } else {
            getServer().getPluginManager().registerEvents(new LegacyChatListener(this), this);
            getLogger().info("Chat bridge: Bukkit AsyncPlayerChatEvent (legacy)");
        }
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
