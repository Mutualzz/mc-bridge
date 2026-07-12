package com.mutualzz.bridge.commands;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoiceCommand implements CommandExecutor, TabCompleter {
    private final MutualzzBridgePlugin plugin;

    public VoiceCommand(MutualzzBridgePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text(
                    "Usage: /mzvoice <join|leave> [room]",
                    NamedTextColor.YELLOW
            ));
            return true;
        }

        var client = plugin.getBridgeClient();
        if (client == null || !client.isReady()) {
            player.sendMessage(Component.text(
                    "Mutualzz bridge is not connected.",
                    NamedTextColor.RED
            ));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "join" -> {
                String room = args.length > 1 ? args[1] : "default";
                client.sendVoiceJoin(player.getUniqueId(), player.getName(), room);
                player.sendMessage(Component.text(
                        "Joining Mutualzz voice…",
                        NamedTextColor.YELLOW
                ));
            }
            case "leave" -> {
                client.sendVoiceLeave(player.getUniqueId(), player.getName());
                player.sendMessage(Component.text(
                        "Leaving Mutualzz voice…",
                        NamedTextColor.YELLOW
                ));
            }
            default -> player.sendMessage(Component.text(
                    "Usage: /mzvoice <join|leave> [room]",
                    NamedTextColor.YELLOW
            ));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : List.of("join", "leave")) {
                if (option.startsWith(prefix)) out.add(option);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            var client = plugin.getBridgeClient();
            List<String> rooms = client != null ? client.getVoiceRooms() : List.of("default");
            for (String room : rooms) {
                if (room.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(room);
                }
            }
        }
        return out;
    }
}
