package com.jovanstar.donuttpa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /tpaheretoggle
 * Toggles incoming TPAHERE requests on/off.
 */
public class TpahereToggleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("only-in-game"));
            return true;
        }
        Player p = (Player) sender;
        TeleportRequestManager.toggleTpahere(p.getUniqueId());
        if (TeleportRequestManager.isTpahereDisabled(p.getUniqueId())) {
            p.sendMessage(Messages.get("tpahere-toggle-disabled"));
        } else {
            p.sendMessage(Messages.get("tpahere-toggle-enabled"));
        }
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        return true;
    }
}
