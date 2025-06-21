package com.jovanstar.donuttpa;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * A holder just for the “Send Confirmation” GUI.
 */
public class SendGUIHolder implements InventoryHolder {
    private Inventory inv;
    public void setInventory(Inventory inv) { this.inv = inv; }
    @Override
    public Inventory getInventory() { return inv; }
}
