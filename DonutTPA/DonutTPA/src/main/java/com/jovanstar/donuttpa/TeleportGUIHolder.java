package com.jovanstar.donuttpa;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * A simple InventoryHolder that stores its own Inventory reference.
 * This allows us to detect “our” GUIs in InventoryClickEvent.
 */
public class TeleportGUIHolder implements InventoryHolder {

    private Inventory inv;

    public TeleportGUIHolder() { }

    public void setInventory(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
