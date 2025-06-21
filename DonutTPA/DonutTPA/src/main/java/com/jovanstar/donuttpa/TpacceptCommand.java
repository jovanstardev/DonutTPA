package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * /tpaccept <player>
 * Opens the Accept‐GUI; does NOT immediately accept.
 */
public class TpacceptCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { }
        Player target = (Player) sender;

        TeleportRequest tr;
        if (args.length == 1) {
            Player requester = Bukkit.getPlayerExact(args[0]);
            // … same as before, fetch by getPendingRequest(requester, target)
            tr = TeleportRequestManager.getPendingRequest(requester.getUniqueId(), target.getUniqueId());
        } else {
            // No args: accept the latest request (if any)
            tr = TeleportRequestManager.getRequestForTarget(target.getUniqueId());
        }

        if (tr == null) {
            target.sendMessage(Messages.get("no-pending-request-target")
                .replace("%requester%", args.length == 1 ? args[0] : ""));
            target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Build & open GUI as before using tr.getRequesterUUID()/tr.getType()
        Player requester = Bukkit.getPlayer(tr.getRequesterUUID());
        Inventory gui = TeleportGUIFactory.buildAcceptConfirmationGUI(
            requester, target, tr.getType(),
            main.getInstance().getConfig().getBoolean("world-icons-enabled", true)
        );
        tr.setGui(gui);
        target.openInventory(gui);
        return true;
    }
}