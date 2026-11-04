package com.mutualzz.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mutualzz.bridge.inventory.InventoryWatcher;

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
 * Speaks only the Mutualzz bridge protocol
 */
public final class BridgeSocketClient {
  private static final Gson GSON = new Gson();

  private final MutualzzBridgePlugin plugin;
  private final String hubUrl;
  private final String token;
  private final String serverId;
  private final boolean inventorySync;
  private final AtomicBoolean intentionalClose = new AtomicBoolean(false);

  private WebSocketClient socket;
  private int heartbeatTask = -1;
  private int statusTask = -1;
  private int reconnectTask = -1;
  private long heartbeatIntervalMs = 15_000;
  private boolean identified = false;
  private final List<String> voiceRooms = new CopyOnWriteArrayList<>();
  private final InventoryWatcher inventoryWatcher;

  private static final long STATUS_INTERVAL_TICKS = 20L * 30; // 30 seconds

  public BridgeSocketClient(
      MutualzzBridgePlugin plugin,
      String hubUrl,
      String token,
      String serverId, boolean inventorySync) {
    this.plugin = plugin;
    this.hubUrl = hubUrl;
    this.token = token;
    this.serverId = serverId;
    this.inventorySync = inventorySync;
    this.inventoryWatcher = new InventoryWatcher(plugin, this);
  }

  public boolean isInventorySyncEnabled() {
    return inventorySync;
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
          stopStatusReports();
          if (inventorySync) {
            inventoryWatcher.stop();
          }
          plugin.getLogger().warning(
              "Disconnected from Mutualzz hub (" + code + "): " + reason);
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
    stopStatusReports();
    inventoryWatcher.stop();
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
        "content", content));
  }

  public void sendJoin(UUID uuid, String name) {
    send("join", Map.of(
        "uuid", uuid.toString(),
        "name", name));
  }

  public void sendLeave(UUID uuid, String name) {
    send("leave", Map.of(
        "uuid", uuid.toString(),
        "name", name));
  }

  public void sendGameEvent(String kind, UUID uuid, String name, String message) {
    send("game_event", Map.of(
        "kind", kind,
        "uuid", uuid.toString(),
        "name", name,
        "message", message));
  }

  public void sendInventory(JsonObject snapshot) {
    sendRaw("inventory", snapshot);
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
        "name", name));
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
    if (socket == null || !socket.isOpen())
      return;
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
    if (payload == null || !payload.has("op"))
      return;

    String op = payload.get("op").getAsString();
    JsonObject d = object(payload, "d");

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
        startStatusReports();
        if (inventorySync) {
          inventoryWatcher.start();
        }
        plugin.getLogger().info(
            "Identified with Mutualzz (bridgeId="
                + string(d, "bridgeId", "?")
                + ", rooms=" + voiceRooms + ")");
        syncOnlinePlayers();
      }
      case "heartbeat_ack" -> {
        // ok
      }
      case "dispatch" -> handleDispatch(payload);
      case "error" -> plugin.getLogger().warning(
          "Hub error [" + string(d, "code", "unknown") + "]: " + string(d, "message", ""));
      default -> {
      }
    }
  }

  private void sendIdentify() {
    send("identify", Map.of(
        "token", token,
        "serverId", serverId));
  }

  private void handleDispatch(JsonObject payload) {
    String type = string(payload, "t", "");
    JsonObject d = object(payload, "d");

    switch (type) {
      case "CHAT" -> onChat(d);
      case "VOICE_JOIN", "VOICE_LEAVE" -> onVoiceAnnounce(d, "VOICE_JOIN".equals(type));
      case "LINK_RESULT" -> onLinkResult(d);
      case "VOICE_RESULT" -> onVoiceResult(d);
      case "INVENTORY_WATCH" -> onInventoryWatch(d);
      default -> {
      }
    }
  }

  private void onChat(JsonObject d) {
    String prefix = switch (string(d, "source", "unknown")) {
      case "discord" -> "Discord";
      case "app" -> "Mutualzz";
      default -> "Bridge";
    };
    Bukkit.broadcast(Component.text()
        .append(Component.text("[" + prefix + "] ", NamedTextColor.AQUA))
        .append(Component.text(string(d, "name", "Unknown") + ": ", NamedTextColor.GRAY))
        .append(Component.text(string(d, "content", ""), NamedTextColor.WHITE))
        .build());
  }

  private void onVoiceAnnounce(JsonObject d, boolean joining) {
    Bukkit.broadcast(Component.text(
        voiceAnnouncement(string(d, "name", "Someone"), joining, string(d, "channelName", "")),
        NamedTextColor.GRAY));
  }

  private void onLinkResult(JsonObject d) {
    boolean ok = bool(d, "ok");
    String message = d.has("code")
        ? "Your link code is " + string(d, "code", "")
            + " — enter it in Mutualzz within 10 minutes"
        : string(d, "message", ok ? "Account linked!" : "Link failed");
    plugin.getLogger().info("Link result: " + message);

    sendToPlayerOrEveryone(uuid(d, "minecraftUuid"), resultMessage(message, ok));
  }

  private void onVoiceResult(JsonObject d) {
    boolean ok = bool(d, "ok");
    String message = string(d, "message", ok ? "Voice updated" : "Voice failed");
    plugin.getLogger().info("Voice result: " + message);

    UUID playerUuid = uuid(d, "uuid");
    Player target = sendToPlayerOrEveryone(playerUuid, resultMessage(message, ok));

    String action = string(d, "action", "");
    boolean joining = "join".equals(action);
    boolean leaving = "leave".equals(action);
    String channelName = string(d, "channelName", "");

    if (ok && target != null && (joining || leaving)) {
      Component announce = Component.text(
          voiceAnnouncement(target.getName(), joining, channelName),
          NamedTextColor.GRAY);
      for (Player online : Bukkit.getOnlinePlayers()) {
        if (!online.getUniqueId().equals(target.getUniqueId())) {
          online.sendMessage(announce);
        }
      }
    }

    var voiceChannel = plugin.getVoiceChannel();
    if (voiceChannel == null || playerUuid == null) {
      return;
    }

    if (ok && joining && target != null && d.has("audioWsUrl") && d.has("audioToken")) {
      voiceChannel.sendJoin(
          target,
          string(d, "audioWsUrl", ""),
          string(d, "audioToken", ""),
          string(d, "userId", null),
          string(d, "room", "default"),
          channelName);
    } else if (leaving) {
      voiceChannel.sendLeave(playerUuid);
    }
  }

  private void onInventoryWatch(JsonObject d) {
    if (!inventorySync || !serverId.equals(string(d, "serverId", ""))) {
      return;
    }
    UUID playerUuid = uuid(d, "uuid");
    if (playerUuid != null) {
      inventoryWatcher.watch(playerUuid);
    }
  }

  private static Component resultMessage(String message, boolean ok) {
    return Component.text("[Mutualzz] " + message, ok ? NamedTextColor.GREEN : NamedTextColor.RED);
  }

  private static String voiceAnnouncement(String name, boolean joining, String channelName) {
    String channelLabel = channelName.isBlank() ? "Mutualzz voice" : "#" + channelName;
    return "[Mutualzz] " + name + (joining ? " joined " : " left ") + channelLabel;
  }

  private static Player sendToPlayerOrEveryone(UUID playerUuid, Component message) {
    Player target = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;
    if (target != null) {
      target.sendMessage(message);
    } else {
      Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }
    return target;
  }

  private static JsonObject object(JsonObject parent, String key) {
    return parent.has(key) && parent.get(key).isJsonObject()
        ? parent.getAsJsonObject(key)
        : new JsonObject();
  }

  private static String string(JsonObject d, String key, String fallback) {
    return d.has(key) && !d.get(key).isJsonNull() ? d.get(key).getAsString() : fallback;
  }

  private static boolean bool(JsonObject d, String key) {
    return d.has(key) && !d.get(key).isJsonNull() && d.get(key).getAsBoolean();
  }

  private static UUID uuid(JsonObject d, String key) {
    String value = string(d, key, null);
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public InventoryWatcher getInventoryWatcher() {
    return inventoryWatcher;
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
        heartbeatIntervalMs / 50);
  }

  private void stopHeartbeat() {
    if (heartbeatTask != -1) {
      Bukkit.getScheduler().cancelTask(heartbeatTask);
      heartbeatTask = -1;
    }
  }

  private void startStatusReports() {
    stopStatusReports();
    statusTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      if (isReady()) {
        sendRaw("server_status", ServerStatus.collect());
      }
    }, 20L, STATUS_INTERVAL_TICKS);
  }

  private void stopStatusReports() {
    if (statusTask != -1) {
      Bukkit.getScheduler().cancelTask(statusTask);
      statusTask = -1;
    }
  }

  private void scheduleReconnect() {
    if (intentionalClose.get())
      return;
    if (reconnectTask != -1)
      return;
    reconnectTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      reconnectTask = -1;
      if (!intentionalClose.get()) {
        plugin.getLogger().info("Reconnecting to Mutualzz hub…");
        connect();
      }
    }, 20L * 5); // 5 seconds
  }
}
