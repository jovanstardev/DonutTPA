package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * /tpa <player>
 * Now: open a “Send Confirmation” GUI for the sender. Only after Confirm do we actually send a request.
 */
public class TpaCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("only-in-game"));
            return true;
        }
        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(Messages.format("invalid-usage", "usage", "/tpa <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(Messages.get("not-online"));
            requester.playSound(requester.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Build & open the “Send Confirmation” GUI to the requester:
        boolean worldIconsEnabled = main.getInstance()
                .getConfig().getBoolean("world-icons-enabled", true);
        Inventory sendGUI = TeleportGUIFactory.buildSendConfirmationGUI(
                requester, target,
                TeleportRequestManager.TeleportType.TO,
                worldIconsEnabled
        );
        requester.openInventory(sendGUI);

        // Remember in pendingSends so the listener knows who-clicked-what:
        TeleportRequestManager.pendingSends.put(
                requester.getUniqueId(),
                new TeleportRequestManager.PendingSendInfo(
                        requester.getUniqueId(),
                        target.getUniqueId(),
                        TeleportRequestManager.TeleportType.TO,
                        sendGUI
                )
        );
        return true;
    }
}
