package com.jovanstar.donuttpa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /tpatoggle
 * Toggles incoming TPA requests on/off.
 */
public class TpaToggleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only usable in-game.");
            return true;
        }
        Player p = (Player) sender;
        TeleportRequestManager.toggleTpa(p.getUniqueId());
        if (TeleportRequestManager.isTpaDisabled(p.getUniqueId())) {
            p.sendMessage(Messages.get("tpa-toggle-disabled"));
        } else {
            p.sendMessage(Messages.get("tpa-toggle-enabled"));
        }
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        return true;
    }
}
