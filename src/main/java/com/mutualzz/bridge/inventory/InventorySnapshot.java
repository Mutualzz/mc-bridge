package com.mutualzz.bridge.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mutualzz.bridge.ServerCompat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Base64;

public final class InventorySnapshot {
  private static final boolean ARMOR_TRIMS = ServerCompat.hasArmorTrims();
  private static final boolean GLINT_OVERRIDE = ServerCompat.hasGlintOverride();

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

      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        if (hasGlint(meta))
          entry.addProperty("enchanted", true);
        if (meta instanceof LeatherArmorMeta leather) {
          entry.addProperty("color", leather.getColor().asRGB());
        }
        if (ARMOR_TRIMS) {
          JsonObject trim = TrimReader.read(meta);
          if (trim != null)
            entry.add("trim", trim);
        }
      }

      out.add(entry);
    }

    return out;
  }

  private static boolean hasGlint(ItemMeta meta) {
    if (GLINT_OVERRIDE && meta.hasEnchantmentGlintOverride()) {
      return Boolean.TRUE.equals(meta.getEnchantmentGlintOverride());
    }
    return meta.hasEnchants();
  }

}
