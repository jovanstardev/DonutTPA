package com.jovanstar.donuttpa;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpautoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("only-in-game"));
            return true;
        }
        Player p = (Player) sender;
        boolean nowOn = TeleportRequestManager.toggleAutoAccept(p.getUniqueId());

        // Play toggle sound
        p.playSound(p.getLocation(),
            nowOn ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_BASS,
            1f, 1f
        );

        // Action bar
        String key = nowOn
            ? "tpauto-on-actionbar"
            : "tpauto-off-actionbar";
        String msg = Messages.get(key);
        p.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            new TextComponent(msg)
        );
        return true;
    }
}
