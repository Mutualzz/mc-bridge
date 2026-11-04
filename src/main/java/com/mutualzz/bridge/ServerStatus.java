package com.mutualzz.bridge;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ServerStatus {
  private ServerStatus() {

  }

  public static JsonObject collect() {
    JsonObject data = new JsonObject();

    data.addProperty("tps", Math.min(20.0, Bukkit.getTPS()[0]));
    data.addProperty("mspt", Bukkit.getAverageTickTime());
    data.addProperty("maxPlayers", Bukkit.getMaxPlayers());
    data.addProperty("version", Bukkit.getMinecraftVersion());
    data.addProperty("motd", PlainTextComponentSerializer.plainText().serialize(Bukkit.motd()));

    return data;
  }
}
