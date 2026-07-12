package com.mutualzz.bridge.commands;

import com.mutualzz.bridge.MutualzzBridgePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LinkCommand implements CommandExecutor {
    private final MutualzzBridgePlugin plugin;

    public LinkCommand(MutualzzBridgePlugin plugin) {
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

        var client = plugin.getBridgeClient();
        if (client == null || !client.isReady()) {
            player.sendMessage(Component.text(
                    "Mutualzz bridge is not connected.",
                    NamedTextColor.RED
            ));
            return true;
        }

        String code = args.length > 0 ? args[0] : null;
        client.sendLink(player.getUniqueId(), player.getName(), code);

        if (code == null) {
            player.sendMessage(Component.text(
                    "Requesting a link code… check chat in a moment, then enter it in Mutualzz.",
                    NamedTextColor.YELLOW
            ));
        } else {
            player.sendMessage(Component.text(
                    "Submitting link code…",
                    NamedTextColor.YELLOW
            ));
        }
        return true;
    }
}
