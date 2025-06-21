package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TpaTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Only complete if one argument (the player name)
        if (!(sender instanceof Player) || args.length != 1) {
            return List.of();
        }
        String current = args[0].toLowerCase();
        // Return all online player names starting with the current input
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}
