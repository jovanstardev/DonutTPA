package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * /tpahere <player>
 * Now: open a “Send Confirmation” GUI for the sender.
 */
public class TpahereCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("only-in-game"));
            return true;
        }
        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(Messages.format("invalid-usage", "usage", "/tpahere <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(Messages.get("not-online"));
            requester.playSound(requester.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        boolean worldIconsEnabled = main.getInstance()
                .getConfig().getBoolean("world-icons-enabled", true);
        Inventory sendGUI = TeleportGUIFactory.buildSendConfirmationGUI(
                requester, target,
                TeleportRequestManager.TeleportType.HERE,
                worldIconsEnabled
        );
        requester.openInventory(sendGUI);

        TeleportRequestManager.pendingSends.put(
                requester.getUniqueId(),
                new TeleportRequestManager.PendingSendInfo(
                        requester.getUniqueId(),
                        target.getUniqueId(),
                        TeleportRequestManager.TeleportType.HERE,
                        sendGUI
                )
        );
        return true;
    }
}
