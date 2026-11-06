package com.mutualzz.bridge.inventory;

import java.util.Base64;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class InventoryEditor {
  private InventoryEditor() {
  }

  public record Result(boolean ok, String message) {
    static Result ok(String message) {
      return new Result(true, message);
    }

    static Result fail(String message) {
      return new Result(false, message);
    }
  }

  private record SlotRef(Inventory inventory, int slot, String key) {
    ItemStack get() {
      return inventory.getItem(slot);
    }

    void set(ItemStack item) {
      inventory.setItem(slot, item);
    }
  }

  public static String encode(ItemStack item) {
    if (item == null || item.getType().isAir())
      return null;

    return Base64.getEncoder().encodeToString(item.serializeAsBytes());
  }

  private static SlotRef slot(Player player, JsonObject ref) {
    if (ref == null)
      return null;
    String container = ref.has("container") ? ref.get("container").getAsString() : "";
    int slot = ref.has("slot") ? ref.get("slot").getAsInt() : -1;

    return switch (container) {
      case "inventory" -> slot >= 0 && slot <= 40 ? new SlotRef(player.getInventory(), slot, "inventory:" + slot)
          : null;
      case "enderChest" -> slot >= 0 && slot <= 26
          ? new SlotRef(player.getEnderChest(), slot, "enderChest:" + slot)
          : null;
      default -> null;
    };
  }

  private static boolean matches(SlotRef ref, JsonObject expected) {
    JsonElement value = expected.get(ref.key());
    ItemStack current = ref.get();
    boolean empty = current == null || current.getType().isAir();

    if (value == null || value.isJsonNull())
      return empty;
    if (empty || !value.isJsonObject())
      return false;

    JsonObject want = value.getAsJsonObject();
    if (want.has("data") && !want.get("data").isJsonNull()) {
      return want.get("data").getAsString().equals(encode(current));
    }
    return want.has("type")
        && want.get("type").getAsString().equals(current.getType().getKey().toString());
  }

  public static Result apply(Player player, JsonObject op, JsonObject expected) {
    String kind = op.has("kind") ? op.get("kind").getAsString() : "";
    return switch (kind) {
      case "remove" -> remove(player, op, expected);
      case "move" -> move(player, op, expected);
      case "give" -> give(player, op, expected);
      case "amount" -> amount(player, op, expected);
      default -> Result.fail("Unknown edit: " + kind);
    };
  }

  private static Result remove(Player player, JsonObject op, JsonObject expected) {
    SlotRef target = slot(player, op);
    if (target == null)
      return Result.fail("Invalid slot");

    if (!matches(target, expected))
      return Result.fail("The inventory has changed");

    target.set(null);
    return Result.ok("Removed");
  }

  private static Result move(Player player, JsonObject op, JsonObject expected) {
    SlotRef from = slot(player, op.getAsJsonObject("from"));
    SlotRef to = slot(player, op.getAsJsonObject("to"));

    if (from == null || to == null)
      return Result.fail("Invalid slot");

    if (from.key().equals(to.key()))
      return Result.fail("Same slot");

    if (!matches(from, expected) || !matches(to, expected)) {
      return Result.fail("The inventory has changed");
    }

    ItemStack moving = from.get();
    ItemStack other = to.get();
    to.set(moving);
    from.set(other);
    return Result.ok("Moved");
  }

  private static Result give(Player player, JsonObject op, JsonObject expected) {
    SlotRef target = slot(player, op);
    if (target == null)
      return Result.fail("Invalid slot");
    Material material = Material.matchMaterial(op.has("type") ? op.get("type").getAsString() : "");
    if (material == null || !material.isItem() || material.isAir()) {
      return Result.fail("Unknown item");
    }
    if (!matches(target, expected))
      return Result.fail("The inventory has changed");
    int amount = clamp(op.has("amount") ? op.get("amount").getAsInt() : 1, material.getMaxStackSize());
    target.set(new ItemStack(material, amount));
    return Result.ok("Gave " + amount + " " + material.getKey().getKey());
  }

  private static Result amount(Player player, JsonObject op, JsonObject expected) {
    SlotRef target = slot(player, op);
    if (target == null)
      return Result.fail("Invalid slot");
    if (!matches(target, expected))
      return Result.fail("The inventory has changed");
    ItemStack item = target.get();
    if (item == null || item.getType().isAir())
      return Result.fail("Slot is empty");
    item.setAmount(clamp(op.has("amount") ? op.get("amount").getAsInt() : 1, item.getMaxStackSize()));
    target.set(item);
    return Result.ok("Amount set to " + item.getAmount());
  }

  private static int clamp(int amount, int max) {
    return Math.max(1, Math.min(max, amount));
  }
}
