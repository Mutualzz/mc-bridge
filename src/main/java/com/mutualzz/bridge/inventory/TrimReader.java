package com.mutualzz.bridge.inventory;

import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;

import com.google.gson.JsonObject;

public final class TrimReader {
  private TrimReader() {
  }

  static JsonObject read(ItemMeta meta) {
    if (!(meta instanceof ArmorMeta armor) || !armor.hasTrim())
      return null;

    ArmorTrim trim = armor.getTrim();

    JsonObject data = new JsonObject();

    data.addProperty("pattern", trim.getPattern().getKey().getKey());
    data.addProperty("material", trim.getMaterial().getKey().getKey());

    return data;
  }
}
