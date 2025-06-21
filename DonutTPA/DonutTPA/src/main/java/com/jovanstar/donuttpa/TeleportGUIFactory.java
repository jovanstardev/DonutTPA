package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class TeleportGUIFactory {

    public static final String SEND_TITLE_KEY              = "confirm-inventory-title";
    public static final String CANCEL_NAME_KEY             = "cancel-pane-name";
    public static final String CANCEL_LORE_KEY             = "cancel-pane-lore";
    public static final String CONFIRM_NAME_KEY            = "confirm-pane-name";
    public static final String CONFIRM_LORE_TPA_KEY        = "confirm-pane-lore-tpa";
    public static final String CONFIRM_LORE_TPAHERE_KEY    = "confirm-pane-lore-tpahere";
    public static final String PLAYER_HEAD_NAME_KEY        = "player-head-name";
    public static final String PLAYER_HEAD_LORE_KEY        = "player-head-lore";
    public static final String FEATHER_NAME_KEY            = "feather-name";
    public static final String FEATHER_LORE_KEY            = "feather-lore";
    public static final String WORLD_ICON_NAME_KEY         = "world-icon-name";
    public static final String DEFAULT_WORLD_ICON_LORE_KEY = "default-world-icon-lore";

    /**
     * Builds the “Send Confirmation” GUI.
     */
    public static Inventory buildSendConfirmationGUI(
            Player sender,
            Player target,
            TeleportRequestManager.TeleportType type,
            boolean worldIconsEnabled)
    {
        String title = Messages.get(SEND_TITLE_KEY);
        Inventory inv = Bukkit.createInventory(null, 3 * 9, title);
        
        String flyingLore = Messages.get("flying-lore")
        	    .replace("%isflying%", target.isFlying() ? "Yes" : "No");

        // ─── Cancel ───────────────────────────────────────────────
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cm = cancel.getItemMeta();
        cm.setDisplayName(Messages.get(CANCEL_NAME_KEY));
        cm.setLore(List.of(Messages.get(CANCEL_LORE_KEY)));
        cancel.setItemMeta(cm);
        inv.setItem(10, cancel);

        // ─── World Icon ───────────────────────────────────────────
        if (worldIconsEnabled) {
            String worldName = target.getWorld().getName();
            Material mat = Material.BARRIER;
            String lore = Messages.get(DEFAULT_WORLD_ICON_LORE_KEY);

            var cfg = main.getInstance().getConfig();
            if (cfg.isConfigurationSection("world-icons." + worldName)) {
                mat = Material.valueOf(
                    cfg.getString("world-icons." + worldName + ".material", "BARRIER").toUpperCase()
                );
                lore = cfg.getString("world-icons." + worldName + ".lore", lore);
            }

            ItemStack icon = new ItemStack(mat);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName(Messages.get(WORLD_ICON_NAME_KEY));
            // Use the lore from config, colored gray
            im.setLore(List.of("§7" + lore));
            icon.setItemMeta(im);
            inv.setItem(12, icon);
        }

        // ─── Player Head ──────────────────────────────────────────
        OfflinePlayer off = target;
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(off);
        sm.setDisplayName(Messages.get(PLAYER_HEAD_NAME_KEY));
        sm.setLore(List.of(
            Messages.format(PLAYER_HEAD_LORE_KEY, "requester", target.getName())
        ));
        head.setItemMeta(sm);
        inv.setItem(13, head);

        // ─── Region / Ping ─────────────────────────────────────────
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta fm = feather.getItemMeta();
        fm.setDisplayName(Messages.get(FEATHER_NAME_KEY));
        int ping = Bukkit.getPlayer(target.getUniqueId()).getPing();
        String loreTpl = Messages.get(FEATHER_LORE_KEY)
                           .replace("%%ping%%", String.valueOf(ping));
        String isFlying = target.isFlying() ? "Yes" : "No";
        loreTpl = loreTpl.replace("%isflying%", isFlying);
        fm.setLore(List.of(loreTpl));
        feather.setItemMeta(fm);
        inv.setItem(14, feather);

        // ─── Confirm ───────────────────────────────────────────────
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta qm = confirm.getItemMeta();
        qm.setDisplayName(Messages.get(CONFIRM_NAME_KEY));
        String loreKey = (type == TeleportRequestManager.TeleportType.TO)
                ? CONFIRM_LORE_TPA_KEY
                : CONFIRM_LORE_TPAHERE_KEY;
        qm.setLore(List.of(
            Messages.format(loreKey, "requester", target.getName())
        ));
        confirm.setItemMeta(qm);
        inv.setItem(16, confirm);

        return inv;
    }

    /**
     * Builds the “Accept Confirmation” GUI (reuses send‐GUI logic).
     */
    public static Inventory buildAcceptConfirmationGUI(

            Player requester,
            Player acceptor,
            TeleportRequestManager.TeleportType type,
            boolean worldIconsEnabled)
    {
        // 1) Title
        String title = Messages.get("accept-inventory-title");
        Inventory inv = Bukkit.createInventory(null, 3 * 9, title);

        // 2) Deny button (slot 10)
        ItemStack deny = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta dm = deny.getItemMeta();
        dm.setDisplayName(Messages.get("deny-pane-name"));
        dm.setLore(List.of(Messages.get("deny-pane-lore")));
        deny.setItemMeta(dm);
        inv.setItem(10, deny);

        // 3) World icon (slot 12)
        if (worldIconsEnabled) {
            String worldName = requester.getWorld().getName();
            Material mat = Material.BARRIER;
            String lore = Messages.get(DEFAULT_WORLD_ICON_LORE_KEY);
            var cfg = main.getInstance().getConfig();
            if (cfg.isConfigurationSection("world-icons." + worldName)) {
                mat = Material.valueOf(
                    cfg.getString("world-icons." + worldName + ".material").toUpperCase()
                );
                lore = cfg.getString("world-icons." + worldName + ".lore");
            }
            ItemStack icon = new ItemStack(mat);
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName(Messages.get(WORLD_ICON_NAME_KEY));
            im.setLore(List.of("§7" + lore));
            icon.setItemMeta(im);
            inv.setItem(12, icon);
        }

        // 4) Player head (slot 13): show acceptor's head with acceptor name
        OfflinePlayer offAcceptor = acceptor;
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(offAcceptor);
        sm.setDisplayName(Messages.format(PLAYER_HEAD_NAME_KEY, "player", acceptor.getName()));
        sm.setLore(List.of(
            Messages.format(PLAYER_HEAD_LORE_KEY, "requester", requester.getName())
        ));
        head.setItemMeta(sm);
        inv.setItem(13, head);

        // 5) Feather (slot 14): show flying status
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta fm = feather.getItemMeta();
        fm.setDisplayName(Messages.get(FEATHER_NAME_KEY));
        String flyingLore = Messages.get(FEATHER_LORE_KEY)
            .replace("%isflying%", acceptor.isFlying() ? "Yes" : "No");
        fm.setLore(List.of(flyingLore));
        feather.setItemMeta(fm);
        inv.setItem(14, feather);

        // 6) Accept button (slot 16)
        ItemStack accept = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta am = accept.getItemMeta();
        am.setDisplayName(Messages.get("accept-pane-name"));
        am.setLore(List.of(Messages.get("accept-pane-lore")));
        accept.setItemMeta(am);
        inv.setItem(16, accept);

        return inv;
    }
}
