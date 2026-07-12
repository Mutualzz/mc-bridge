package com.mutualzz.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket client to the Mutualzz hub.
 * Speaks only the Mutualzz bridge protocol — no Discord libraries.
 */
public final class BridgeSocketClient {
    private static final Gson GSON = new Gson();

    private final MutualzzBridgePlugin plugin;
    private final String hubUrl;
    private final String token;
    private final String serverId;
    private final AtomicBoolean intentionalClose = new AtomicBoolean(false);

    private WebSocketClient socket;
    private int heartbeatTask = -1;
    private int reconnectTask = -1;
    private long heartbeatIntervalMs = 15_000;
    private boolean identified = false;
    private final List<String> voiceRooms = new CopyOnWriteArrayList<>();

    public BridgeSocketClient(
            MutualzzBridgePlugin plugin,
            String hubUrl,
            String token,
            String serverId
    ) {
        this.plugin = plugin;
        this.hubUrl = hubUrl;
        this.token = token;
        this.serverId = serverId;
    }

    public void connect() {
        intentionalClose.set(false);
        try {
            socket = new WebSocketClient(URI.create(hubUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    plugin.getLogger().info("Connected to Mutualzz hub");
                }

                @Override
                public void onMessage(String message) {
                    Bukkit.getScheduler().runTask(plugin, () -> handleMessage(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    identified = false;
                    stopHeartbeat();
                    plugin.getLogger().warning(
                            "Disconnected from Mutualzz hub (" + code + "): " + reason
                    );
                    if (!intentionalClose.get()) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    plugin.getLogger().severe("Bridge socket error: " + ex.getMessage());
                }
            };
            socket.connect();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect: " + e.getMessage());
            scheduleReconnect();
        }
    }

    public void shutdown() {
        intentionalClose.set(true);
        stopHeartbeat();
        if (reconnectTask != -1) {
            Bukkit.getScheduler().cancelTask(reconnectTask);
            reconnectTask = -1;
        }
        if (socket != null) {
            socket.close();
        }
    }

    public boolean isReady() {
        return identified && socket != null && socket.isOpen();
    }

    /** Bound Mutualzz voice room keys for tab-complete. */
    public List<String> getVoiceRooms() {
        if (voiceRooms.isEmpty()) {
            return List.of("default");
        }
        return Collections.unmodifiableList(new ArrayList<>(voiceRooms));
    }

    public void sendChat(UUID uuid, String name, String content) {
        send("chat", Map.of(
                "uuid", uuid.toString(),
                "name", name,
                "content", content
        ));
    }

    public void sendJoin(UUID uuid, String name) {
        send("join", Map.of(
                "uuid", uuid.toString(),
                "name", name
        ));
    }

    public void sendLeave(UUID uuid, String name) {
        send("leave", Map.of(
                "uuid", uuid.toString(),
                "name", name
        ));
    }

    private void syncOnlinePlayers() {
        for (var player : Bukkit.getOnlinePlayers()) {
            sendJoin(player.getUniqueId(), player.getName());
        }
    }

    public void sendLink(UUID uuid, String name, String code) {
        JsonObject d = new JsonObject();
        d.addProperty("uuid", uuid.toString());
        d.addProperty("name", name);
        if (code != null && !code.isBlank()) {
            d.addProperty("code", code);
        }
        sendRaw("link", d);
    }

    public void sendVoiceJoin(UUID uuid, String name, String room) {
        JsonObject d = new JsonObject();
        d.addProperty("uuid", uuid.toString());
        d.addProperty("name", name);
        d.addProperty("room", room == null || room.isBlank() ? "default" : room);
        sendRaw("voice_join", d);
    }

    public void sendVoiceLeave(UUID uuid, String name) {
        send("voice_leave", Map.of(
                "uuid", uuid.toString(),
                "name", name
        ));
    }

    public void sendVoiceState(UUID uuid, String name, boolean selfMute, boolean selfDeaf) {
        JsonObject d = new JsonObject();
        d.addProperty("uuid", uuid.toString());
        d.addProperty("name", name);
        d.addProperty("selfMute", selfMute);
        d.addProperty("selfDeaf", selfDeaf);
        sendRaw("voice_state", d);
    }

    private void send(String op, Map<String, String> data) {
        JsonObject d = new JsonObject();
        data.forEach(d::addProperty);
        sendRaw(op, d);
    }

    private void sendRaw(String op, JsonObject d) {
        if (socket == null || !socket.isOpen()) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("op", op);
        payload.add("d", d);
        socket.send(GSON.toJson(payload));
    }

    private void handleMessage(String raw) {
        JsonObject payload;
        try {
            payload = GSON.fromJson(raw, JsonObject.class);
        } catch (Exception e) {
            return;
        }
        if (payload == null || !payload.has("op")) return;

        String op = payload.get("op").getAsString();
        JsonObject d = payload.has("d") && payload.get("d").isJsonObject()
                ? payload.getAsJsonObject("d")
                : new JsonObject();

        switch (op) {
            case "hello" -> {
                if (d.has("heartbeatInterval")) {
                    heartbeatIntervalMs = d.get("heartbeatInterval").getAsLong();
                }
                sendIdentify();
            }
            case "ready" -> {
                identified = true;
                voiceRooms.clear();
                if (d.has("voiceRooms") && d.get("voiceRooms").isJsonArray()) {
                    d.getAsJsonArray("voiceRooms").forEach(el -> {
                        if (el.isJsonPrimitive()) {
                            String name = el.getAsString();
                            if (name != null && !name.isBlank()) {
                                voiceRooms.add(name);
                            }
                        }
                    });
                }
                if (voiceRooms.isEmpty()) {
                    voiceRooms.add("default");
                }
                startHeartbeat();
                plugin.getLogger().info(
                        "Identified with Mutualzz (bridgeId="
                                + (d.has("bridgeId") ? d.get("bridgeId").getAsString() : "?")
                                + ", rooms=" + voiceRooms + ")"
                );
                syncOnlinePlayers();
            }
            case "heartbeat_ack" -> {
                // ok
            }
            case "dispatch" -> handleDispatch(payload);
            case "error" -> {
                String code = d.has("code") ? d.get("code").getAsString() : "unknown";
                String message = d.has("message") ? d.get("message").getAsString() : "";
                plugin.getLogger().warning("Hub error [" + code + "]: " + message);
            }
            default -> {
            }
        }
    }

    private void sendIdentify() {
        send("identify", Map.of(
                "token", token,
                "serverId", serverId
        ));
    }

    private void handleDispatch(JsonObject payload) {
        if (!payload.has("t")) return;
        String type = payload.get("t").getAsString();
        JsonObject d = payload.has("d") && payload.get("d").isJsonObject()
                ? payload.getAsJsonObject("d")
                : new JsonObject();

        switch (type) {
            case "CHAT" -> {
                String name = d.has("name") ? d.get("name").getAsString() : "Unknown";
                String content = d.has("content") ? d.get("content").getAsString() : "";
                String source = d.has("source") ? d.get("source").getAsString() : "unknown";
                String prefix = switch (source) {
                    case "discord" -> "Discord";
                    case "app" -> "Mutualzz";
                    default -> "Bridge";
                };
                Bukkit.broadcast(Component.text()
                        .append(Component.text("[" + prefix + "] ", NamedTextColor.AQUA))
                        .append(Component.text(name + ": ", NamedTextColor.GRAY))
                        .append(Component.text(content, NamedTextColor.WHITE))
                        .build());
            }
            case "VOICE_JOIN", "VOICE_LEAVE" -> {
                String name = d.has("name") ? d.get("name").getAsString() : "Someone";
                String channelName = d.has("channelName")
                        ? d.get("channelName").getAsString()
                        : "";
                String channelLabel = channelName.isBlank()
                        ? "Mutualzz voice"
                        : "#" + channelName;
                boolean joining = "VOICE_JOIN".equals(type);
                Bukkit.broadcast(Component.text(
                        "[Mutualzz] " + name
                                + (joining ? " joined " : " left ")
                                + channelLabel,
                        NamedTextColor.GRAY
                ));
            }
            case "LINK_RESULT" -> {
                boolean ok = d.has("ok") && d.get("ok").getAsBoolean();
                String message = d.has("message")
                        ? d.get("message").getAsString()
                        : (ok ? "Account linked!" : "Link failed");
                if (d.has("code")) {
                    message = "Your link code is " + d.get("code").getAsString()
                            + " — enter it in Mutualzz within 10 minutes";
                }
                plugin.getLogger().info("Link result: " + message);

                Component component = Component.text(
                        "[Mutualzz] " + message,
                        ok ? NamedTextColor.GREEN : NamedTextColor.RED
                );
                if (d.has("minecraftUuid")) {
                    try {
                        var player = Bukkit.getPlayer(
                                UUID.fromString(d.get("minecraftUuid").getAsString())
                        );
                        if (player != null) {
                            player.sendMessage(component);
                            break;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(component));
            }
            case "VOICE_RESULT" -> {
                boolean ok = d.has("ok") && d.get("ok").getAsBoolean();
                String message = d.has("message")
                        ? d.get("message").getAsString()
                        : (ok ? "Voice updated" : "Voice failed");
                plugin.getLogger().info("Voice result: " + message);

                Component component = Component.text(
                        "[Mutualzz] " + message,
                        ok ? NamedTextColor.GREEN : NamedTextColor.RED
                );

                UUID playerUuid = null;
                if (d.has("uuid")) {
                    try {
                        playerUuid = UUID.fromString(d.get("uuid").getAsString());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                Player target = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;
                if (target != null) {
                    target.sendMessage(component);
                } else {
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(component));
                }

                String action = d.has("action") ? d.get("action").getAsString() : "";

                // Same-server announce for everyone else (source session skips VOICE_JOIN echo).
                if (ok && target != null && ("join".equals(action) || "leave".equals(action))) {
                    String channelName = d.has("channelName")
                            ? d.get("channelName").getAsString()
                            : "";
                    String channelLabel = channelName == null || channelName.isBlank()
                            ? "Mutualzz voice"
                            : "#" + channelName;
                    String announce = target.getName()
                            + ("join".equals(action) ? " joined " : " left ")
                            + channelLabel;
                    Component broadcast = Component.text(
                            "[Mutualzz] " + announce,
                            NamedTextColor.GRAY
                    );
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.getUniqueId().equals(target.getUniqueId())) {
                            online.sendMessage(broadcast);
                        }
                    }
                }

                var voiceChannel = plugin.getVoiceChannel();
                if (voiceChannel == null || playerUuid == null) {
                    break;
                }

                if (ok && "join".equals(action)
                        && d.has("audioWsUrl")
                        && d.has("audioToken")
                        && target != null) {
                    String userId = d.has("userId") ? d.get("userId").getAsString() : null;
                    String room = d.has("room") ? d.get("room").getAsString() : "default";
                    String channelName = d.has("channelName")
                            ? d.get("channelName").getAsString()
                            : "";
                    voiceChannel.sendJoin(
                            target,
                            d.get("audioWsUrl").getAsString(),
                            d.get("audioToken").getAsString(),
                            userId,
                            room,
                            channelName
                    );
                } else if ("leave".equals(action)) {
                    voiceChannel.sendLeave(playerUuid);
                }
            }
            default -> {
            }
        }
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> {
                    if (socket != null && socket.isOpen()) {
                        JsonObject payload = new JsonObject();
                        payload.addProperty("op", "heartbeat");
                        payload.add("d", new JsonObject());
                        socket.send(GSON.toJson(payload));
                    }
                },
                heartbeatIntervalMs / 50,
                heartbeatIntervalMs / 50
        );
    }

    private void stopHeartbeat() {
        if (heartbeatTask != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTask);
            heartbeatTask = -1;
        }
    }

    private void scheduleReconnect() {
        if (intentionalClose.get()) return;
        if (reconnectTask != -1) return;
        reconnectTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            reconnectTask = -1;
            if (!intentionalClose.get()) {
                plugin.getLogger().info("Reconnecting to Mutualzz hub…");
                connect();
            }
        }, 20L * 5); // 5 seconds
    }
}
