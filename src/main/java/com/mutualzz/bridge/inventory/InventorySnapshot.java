package com.mutualzz.bridge.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class InventorySnapshot {
  private InventorySnapshot() {
  }

  public static JsonObject of(Player player) {
    JsonObject data = new JsonObject();

    data.addProperty("uuid", player.getUniqueId().toString());
    data.addProperty("name", player.getName());
    data.add("inventory", slots(player.getInventory().getContents()));
    data.add("enderChest", slots(player.getEnderChest().getContents()));

    return data;
  }

  private static JsonArray slots(ItemStack[] contents) {
    JsonArray out = new JsonArray();
    for (int slot = 0; slot < contents.length; slot++) {
      ItemStack item = contents[slot];
      if (item == null || item.getType().isAir())
        continue;

      JsonObject entry = new JsonObject();
      entry.addProperty("slot", slot);
      entry.addProperty("type", item.getType().getKey().toString());
      entry.addProperty("amount", item.getAmount());
      entry.addProperty("data", Base64.getEncoder().encodeToString(item.serializeAsBytes()));
      out.add(entry);
    }
    return out;
  }
}
