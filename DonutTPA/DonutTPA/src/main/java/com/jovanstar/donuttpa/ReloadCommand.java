package com.jovanstar.donuttpa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.UUID;

/**
 * /tpreload – Reloads both config.yml and toggles.yml (disabled/auto flags)
 */
public class ReloadCommand implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donuttpa.reload")) {
            sender.sendMessage(Messages.get("no-permission"));
            return true;
        }

        // 1️⃣ Reload main config
        main plugin = main.getInstance();
        plugin.reloadConfig();

        // 2️⃣ Reload TeleportRequestManager settings
        TeleportRequestManager.initialize();

        // 3️⃣ Reload toggles.yml
        File togglesFile = new File(plugin.getDataFolder(), "saves.yml");
        FileConfiguration togglesCfg = YamlConfiguration.loadConfiguration(togglesFile);

        // Clear existing sets
        TeleportRequestManager.tpaDisabled.clear();
        TeleportRequestManager.tpahereDisabled.clear();
        TeleportRequestManager.autoAccept.clear();

        // Load from toggles.yml
        togglesCfg.getStringList("disabled-tpa").forEach(s ->
                TeleportRequestManager.tpaDisabled.add(UUID.fromString(s))
        );
        togglesCfg.getStringList("disabled-tpahere").forEach(s ->
                TeleportRequestManager.tpahereDisabled.add(UUID.fromString(s))
        );
        togglesCfg.getStringList("auto-accept").forEach(s ->
                TeleportRequestManager.autoAccept.add(UUID.fromString(s))
        );

        sender.sendMessage(Messages.get("reload-success"));
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
