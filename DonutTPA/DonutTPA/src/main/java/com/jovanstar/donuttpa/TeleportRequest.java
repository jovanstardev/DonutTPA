package com.jovanstar.donuttpa;

import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Represents a single pending teleport request (TPA or TPAHERE).
 *
 * We’ve made `gui` nullable, and provided a setter so that TpacceptCommand
 * can build & assign the confirmation‐GUI at the moment of “accept.”
 */
public class TeleportRequest {

    private final UUID requesterId;
    private final UUID targetId;
    private final TeleportRequestManager.TeleportType type;
    private Inventory gui;          // ← now settable
    private BukkitTask timeoutTask;

    public TeleportRequest(UUID requesterId, UUID targetId,
                           TeleportRequestManager.TeleportType type, Inventory gui)
    {
        this.requesterId = requesterId;
        this.targetId = targetId;
        this.type = type;
        this.gui = gui;
    }

    public UUID getRequesterUUID() { return requesterId; }
    public UUID getTargetUUID()    { return targetId; }
    public String getRequesterId() { return requesterId.toString(); }
    public String getTargetId()    { return targetId.toString(); }
    public TeleportRequestManager.TeleportType getType() { return type; }

    public Inventory getGui()      { return gui; }
    public void setGui(Inventory gui) { this.gui = gui; }

    public BukkitTask getTimeoutTask() { return timeoutTask; }
    public void setTimeoutTask(BukkitTask timeoutTask) { this.timeoutTask = timeoutTask; }
}
