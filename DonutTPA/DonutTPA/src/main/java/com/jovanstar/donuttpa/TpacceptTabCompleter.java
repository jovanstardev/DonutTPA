package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TpacceptTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) return List.of();
        Player target = (Player) sender;
        UUID tgtId = target.getUniqueId();

        // Find all TeleportRequests where this player is the target
        List<String> names = TeleportRequestManager.pendingRequests.values().stream()
            .filter(tr -> tr.getTargetUUID().equals(tgtId))
            .map(tr -> tr.getRequesterUUID())
            .map(Bukkit::getPlayer)
            .filter(p -> p != null && p.isOnline())
            .map(Player::getName)
            .collect(Collectors.toList());

        return names;
    }
}
