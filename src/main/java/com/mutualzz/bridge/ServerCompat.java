package com.mutualzz.bridge;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight server version checks for multi-version Paper/Spigot support.
 * Minimum supported game version: Minecraft / Paper <strong>1.18.2</strong>.
 * Paper calendar versions (26.x, …) are accepted as newer than 1.x.
 */
public final class ServerCompat {
    private static final Pattern VERSION =
            Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private ServerCompat() {
    }

    /** True when Paper Adventure {@code AsyncChatEvent} is on the classpath. */
    public static boolean hasPaperAsyncChat() {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean meetsMinimumMinecraft() {
        int[] v = parseMinecraftVersion();
        if (v == null) {
            // Unknown format — don't block; chat fallbacks still apply.
            return true;
        }
        int major = v[0];
        int minor = v[1];
        int patch = v[2];
        // Paper year-based versions (26.1.x, …)
        if (major >= 26) return true;
        if (major > 1) return true;
        if (major < 1) return false;
        if (minor > 18) return true;
        if (minor < 18) return false;
        return patch >= 2;
    }

    public static String describeMinecraftVersion() {
        int[] v = parseMinecraftVersion();
        if (v == null) {
            return Bukkit.getBukkitVersion();
        }
        if (v[2] == 0 && !Bukkit.getBukkitVersion().contains("." + v[1] + ".")) {
            return v[0] + "." + v[1];
        }
        return v[0] + "." + v[1] + "." + v[2];
    }

    /**
     * @return {@code [major, minor, patch]} or null if unparseable
     */
    public static int[] parseMinecraftVersion() {
        String raw = Bukkit.getBukkitVersion();
        if (raw == null || raw.isBlank()) {
            raw = Bukkit.getVersion();
        }
        if (raw == null) return null;
        Matcher m = VERSION.matcher(raw);
        if (!m.find()) return null;
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        return new int[]{major, minor, patch};
    }
}
