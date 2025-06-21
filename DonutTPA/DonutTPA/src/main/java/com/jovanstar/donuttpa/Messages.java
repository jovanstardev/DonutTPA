package com.jovanstar.donuttpa;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

import java.io.File;

/**
 * Simple utility to load messages.yml and provide getters.
 */
public class Messages {

    private static FileConfiguration msgs;

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        msgs = YamlConfiguration.loadConfiguration(file);
    }
     /**
     * Retrieve a single string and translate color codes.
     */
    public static String get(String path) {
        if (msgs.isSet(path)) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', msgs.getString(path));
        }
        // Fallback: raw path
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', path);
    }
    
    public static String format(String key, Player player) {
        String message = get(key);
        if (player != null) {
            message = message.replace("%player%", player.getName());
            message = message.replace("%isflying%", player.isFlying() ? "Yes" : "No");
            // Add any other placeholders as needed
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Format placeholders: %requester%, %target%, %seconds%, etc.
     */
    public static String format(String path, Object... replacements) {
        String raw = get(path);
        if (replacements.length % 2 != 0) return raw;
        String formatted = raw;
        for (int i = 0; i < replacements.length; i += 2) {
            String key = "%" + replacements[i].toString() + "%";
            String val = replacements[i + 1].toString();
            formatted = formatted.replace(key, val);
        }
        return formatted;
    }
   
}
