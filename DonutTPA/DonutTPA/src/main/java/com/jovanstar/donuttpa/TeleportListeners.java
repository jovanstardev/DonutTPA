package com.jovanstar.donuttpa;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listens for clicks in both the Send and Accept confirmation GUIs.
 */
public class TeleportListeners implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        Player clicker = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        String sendTitle = Messages.get(TeleportGUIFactory.SEND_TITLE_KEY);
        String acceptTitle = Messages.get("accept-inventory-title");

        // Handle Send GUI
        if (title.equals(sendTitle)) {
            e.setCancelled(true);
            TeleportRequestManager.PendingSendInfo info = TeleportRequestManager.pendingSends.get(clicker.getUniqueId());
            if (info != null) {
                handleSendGui(clicker, slot, info);
            }
        }
        // Handle Accept GUI
        else if (title.equals(acceptTitle)) {
            e.setCancelled(true);
            Inventory inv = e.getView().getTopInventory();
            if (slot == 10) {
                // Deny
                TeleportRequestManager.cancelRequest(inv, clicker);
            } else if (slot == 16) {
                // Accept
                clicker.closeInventory();
                TeleportRequestManager.acceptRequest(inv, clicker);
            }
        }
    }

    /**
     * Handles clicks in the Send Confirmation GUI.
     */
    private void handleSendGui(Player clicker, int slot, TeleportRequestManager.PendingSendInfo info) {
        if (slot == 10) {
            // Cancel send
            clicker.closeInventory();
            TeleportRequestManager.pendingSends.remove(clicker.getUniqueId());
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        } else if (slot == 16) {
            // Confirm send
            clicker.closeInventory();
            Player requester = clicker;
            Player target = Bukkit.getPlayer(info.targetId);
            TeleportRequestManager.TeleportType type = info.type;
            TeleportRequestManager.pendingSends.remove(clicker.getUniqueId());
            
            if (target == null || !target.isOnline()) {
                requester.sendMessage(Messages.get("not-online"));
                requester.playSound(requester.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            boolean ok = TeleportRequestManager.createRequest(requester, target, type);
            String actionKey = ok ? "send-actionbar-success" : "send-actionbar-fail";
            String msg = Messages.get(actionKey);
            requester.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            requester.playSound(requester.getLocation(), ok ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
}
