package com.mutualzz.bridge.moderation;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RemoteModeration {
  private static final Pattern PLAYER_NAME = Pattern.compile("^\\.?[A-Za-z0-9_]{1,16}$");
  private static final int MAX_REASON = 200;

  private final MutualzzBridgePlugin plugin;
  private final boolean enabled;
  private final String banCommand;
  private final String unbanCommand;

  public RemoteModeration(MutualzzBridgePlugin plugin) {
    this.plugin = plugin;
    var config = plugin.getConfig();
    this.enabled = config.getBoolean("remoteModeration", false);
    this.banCommand = config.getString("banCommand", "ban {name} {reason}");
    this.unbanCommand = config.getString("unbanCommand", "pardon {name}");
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Result handle(String action, UUID uuid, String name, String reason) {
    if (!enabled)
      return Result.fail("Remote moderation is disabled on this server");
    if (name == null || !PLAYER_NAME.matcher(name).matches()) {
      return Result.fail("Invalid player name");
    }

    String cleanReason = cleanReason(reason);
    return switch (action) {
      case "kick" -> kick(uuid, cleanReason);
      case "ban" -> runCommand(banCommand, name, cleanReason);
      case "unban" -> runCommand(unbanCommand, name, cleanReason);
      default -> Result.fail("Unknown action: " + action);
    };
  }

  private Result kick(UUID uuid, String reason) {
    Player player = uuid != null ? Bukkit.getPlayer(uuid) : null;
    if (player == null)
      return Result.fail("Player is not online on this server");
    player.kick(Component.text(reason));
    return Result.ok("Kicked " + player.getName());
  }

  private Result runCommand(String template, String name, String reason) {
    String command = template.replace("{name}", name).replace("{reason}", reason).trim();
    boolean found = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    plugin.getLogger().info("Remote moderation ran: " + command);
    return found
        ? Result.ok("Ran: " + command)
        : Result.fail("Command not found: " + command.split(" ")[0]);
  }

  private static String cleanReason(String reason) {
    String text = reason == null ? "" : reason.replaceAll("[\\p{Cntrl}§]", " ").trim();
    if (text.isEmpty())
      text = "Moderated via Mutualzz";
    return text.length() > MAX_REASON ? text.substring(0, MAX_REASON) : text;
  }

  public record Result(boolean ok, String message) {
    static Result ok(String message) {
      return new Result(true, message);
    }

    static Result fail(String message) {
      return new Result(false, message);
    }
  }
}
