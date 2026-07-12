package com.mutualzz.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Plugin channel to the Fabric Mutualzz Voice mod.
 * Payload is UTF-8 JSON (small control messages only — audio rides a separate WS).
 */
public final class VoicePluginChannel implements PluginMessageListener {
    public static final String CHANNEL = "mutualzz:voice";

    private final MutualzzBridgePlugin plugin;

    public VoicePluginChannel(MutualzzBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void unregister() {
        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void sendJoin(Player player, String audioWsUrl, String audioToken, String userId,
                         String room, String channelName) {
        JsonObject d = new JsonObject();
        d.addProperty("t", "voice_join");
        d.addProperty("audioWsUrl", audioWsUrl);
        d.addProperty("audioToken", audioToken);
        if (userId != null) {
            d.addProperty("userId", userId);
        }
        if (room != null && !room.isBlank()) {
            d.addProperty("room", room);
        }
        if (channelName != null && !channelName.isBlank()) {
            d.addProperty("channelName", channelName);
        }
        send(player, d);
    }

    public void sendLeave(Player player) {
        JsonObject d = new JsonObject();
        d.addProperty("t", "voice_leave");
        send(player, d);
    }

    public void sendLeave(UUID playerUuid) {
        var player = plugin.getServer().getPlayer(playerUuid);
        if (player != null) {
            sendLeave(player);
        }
    }

    private void send(Player player, JsonObject payload) {
        byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(plugin, CHANNEL, data);
    }

    @Override
    public void onPluginMessageReceived(
            @NotNull String channel,
            @NotNull Player player,
            byte @NotNull [] message
    ) {
        if (!CHANNEL.equals(channel)) return;
        try {
            String raw = new String(message, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            String type = obj.has("t") ? obj.get("t").getAsString() : "";
            if (!"voice_state".equals(type)) return;

            boolean selfMute = obj.has("selfMute") && obj.get("selfMute").getAsBoolean();
            boolean selfDeaf = obj.has("selfDeaf") && obj.get("selfDeaf").getAsBoolean();
            var bridge = plugin.getBridgeClient();
            if (bridge != null) {
                plugin.getLogger().fine(
                        "voice_state from " + player.getName()
                                + " mute=" + selfMute + " deaf=" + selfDeaf);
                bridge.sendVoiceState(player.getUniqueId(), player.getName(), selfMute, selfDeaf);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("voice plugin message: " + e.getMessage());
        }
    }
}
