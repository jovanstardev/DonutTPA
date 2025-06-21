package com.jovanstar.donuttpa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /tpacancel <player>
 * Deny a pending request programmatically.
 */
public class TpacancelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("only-in-game"));
            return true;
        }
        Player target = (Player) sender;

        if (args.length != 1) {
            target.sendMessage(Messages.format("invalid-usage", "usage", "/tpacancel <player>"));
            return true;
        }

        Player requester = target.getServer().getPlayerExact(args[0]);
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Messages.get("not-online"));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (!TeleportRequestManager.isRequestPending(requester.getUniqueId(), target.getUniqueId())) {
            target.sendMessage(Messages.format("no-pending-request-target", "requester", requester.getName()));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        TeleportRequest tr = TeleportRequestManager.pendingRequests
                .get(requester.getUniqueId().toString() + ":" + target.getUniqueId().toString());
        if (tr != null) {
            TeleportRequestManager.cancelRequest(tr.getGui(), target);
        }
        return true;
    }
}
