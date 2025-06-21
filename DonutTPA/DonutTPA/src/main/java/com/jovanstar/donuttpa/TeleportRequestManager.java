package com.jovanstar.donuttpa;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all teleport requests, cooldowns, toggles, and in-progress teleports.
 */
public class TeleportRequestManager {

    public enum TeleportType { TO, HERE }

    // ─── PRIVATE STATE ────────────────────────────────────────────────────────

    /** Key = requesterUUID:targetUUID **/
    public static final Map<String, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();

    /** Map from target UUID → their pending TeleportRequest **/
    private static final Map<UUID, TeleportRequest> targetToRequest = new ConcurrentHashMap<>();

    /** Players who have disabled incoming TPA requests. **/
    public static final Set<UUID> tpaDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Players who have disabled incoming TPAHERE requests. **/
    public static final Set<UUID> tpahereDisabled = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Players currently in the “teleport countdown.” Key = playerUUID **/
    private static final Map<UUID, BukkitTask> teleportTasks = new ConcurrentHashMap<>();

    /** Players’ starting locations when a teleport countdown begins. Key = playerUUID **/
    private static final Map<UUID, org.bukkit.Location> teleportStartLocations = new ConcurrentHashMap<>();

    /** Temporary storage while a sender is confirming “Send TPA.” **/
    public static final Map<UUID, PendingSendInfo> pendingSends = new ConcurrentHashMap<>();

    /** Players who auto‐accept all incoming TPA/TPAHERE requests **/
    public static final Set<UUID> autoAccept = ConcurrentHashMap.newKeySet();

    public static Set<UUID> getAutoAcceptList() {
        return Collections.unmodifiableSet(autoAccept);
    }
    /** Helper to remember: sender → (target + type + send‐GUI‐inventory) **/
    public static class PendingSendInfo {
        public final UUID requesterId;
        public final UUID targetId;
        public final TeleportType type;
        public final Inventory sendGUI;
        public PendingSendInfo(UUID req, UUID tgt, TeleportType t, Inventory gui) {
            this.requesterId = req;
            this.targetId = tgt;
            this.type = t;
            this.sendGUI = gui;
        }
    }

    private static int REQUEST_TIMEOUT;
    private static int COUNTDOWN_SECONDS;
    private static boolean WORLD_ICONS_ENABLED;

    private TeleportRequestManager() { /* no-instantiation */ }

    // ─── INITIALIZATION / SHUTDOWN ───────────────────────────────────────────

    public static void initialize() {
        REQUEST_TIMEOUT     = main.getInstance().getConfig().getInt("request-timeout", 60);
        COUNTDOWN_SECONDS   = main.getInstance().getConfig().getInt("countdown-seconds", 5);
        WORLD_ICONS_ENABLED = main.getInstance().getConfig().getBoolean("world-icons-enabled", true);
    }

    public static void shutdown() {
        for (BukkitTask task : teleportTasks.values()) {
            task.cancel();
        }
        pendingRequests.clear();
        targetToRequest.clear();
        tpaDisabled.clear();
        tpahereDisabled.clear();
        teleportTasks.clear();
        teleportStartLocations.clear();
        pendingSends.clear();
    }

    // ─── INTERNAL HELPERS ────────────────────────────────────────────────────

    private static String key(UUID requester, UUID target) {
        return requester.toString() + ":" + target.toString();
    }

    private static void removePendingRequest(String mapKey) {
        TeleportRequest tr = pendingRequests.remove(mapKey);
        if (tr != null) {
            if (tr.getTimeoutTask() != null) {
                tr.getTimeoutTask().cancel();
            }
            targetToRequest.remove(tr.getTargetUUID());
        }
    }

    // ─── PUBLIC HELPERS ──────────────────────────────────────────────────────

    public static boolean isRequestPending(UUID requester, UUID target) {
        return pendingRequests.containsKey(key(requester, target));
    }

    public static boolean isTpaDisabled(UUID player) {
        return tpaDisabled.contains(player);
    }

    public static boolean isTpahereDisabled(UUID player) {
        return tpahereDisabled.contains(player);
    }

    public static void toggleTpa(UUID player) {
        if (!tpaDisabled.add(player)) tpaDisabled.remove(player);
    }

    public static void toggleTpahere(UUID player) {
        if (!tpahereDisabled.add(player)) tpahereDisabled.remove(player);
    }

    public static void cleanupPlayer(UUID playerUUID) {
        // Cancel any countdown
        if (teleportTasks.containsKey(playerUUID)) {
            teleportTasks.get(playerUUID).cancel();
            teleportTasks.remove(playerUUID);
            teleportStartLocations.remove(playerUUID);
        }

        // Remove pending requests where requester or target is this player
        pendingRequests.keySet().removeIf(k -> {
            String[] parts = k.split(":");
            if (parts.length != 2) return false;
            boolean isReq = parts[0].equals(playerUUID.toString());
            boolean isTgt = parts[1].equals(playerUUID.toString());
            if (isReq || isTgt) {
                TeleportRequest tr = pendingRequests.get(k);
                if (tr != null) targetToRequest.remove(tr.getTargetUUID());
                return true;
            }
            return false;
        });

        pendingSends.remove(playerUUID);
    }

    public static TeleportRequest getPendingRequest(UUID requester, UUID target) {
        return pendingRequests.get(key(requester, target));
    }

    public static TeleportRequest getRequestForTarget(UUID target) {
        return targetToRequest.get(target);
    }

    // ─── CREATING / SENDING / TIMEOUT ─────────────────────────────────────────

    public static boolean createRequest(Player requester, Player target, TeleportType type) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(Messages.get("no-self-request"));
            return false;
        }
        if (type == TeleportType.TO && isTpaDisabled(target.getUniqueId())) {
            requester.sendMessage(Messages.format("tpa-disabled","target",target.getName()));
            return false;
        }
        if (type == TeleportType.HERE && isTpahereDisabled(target.getUniqueId())) {
            requester.sendMessage(Messages.format("tpahere-disabled","target",target.getName()));
            requester.playSound(requester.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);
            return false;
        }
        String mapKey = key(requester.getUniqueId(),target.getUniqueId());
        if (pendingRequests.containsKey(mapKey)) {
            requester.sendMessage(Messages.get("on-cooldown"));
            requester.playSound(requester.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);
            return false;
        }
        TeleportRequest tr = new TeleportRequest(requester.getUniqueId(),target.getUniqueId(),type,null);
        BukkitTask to = new BukkitRunnable(){public void run(){
            if (pendingRequests.containsKey(mapKey)){
                pendingRequests.remove(mapKey);
                targetToRequest.remove(target.getUniqueId());
                if (requester.isOnline()){requester.sendMessage(Messages.format("request-expired-sender","target",target.getName()));
                    requester.playSound(requester.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);}                
                if (target.isOnline()){target.sendMessage(Messages.format("request-expired-target","requester",requester.getName()));
                    target.playSound(target.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);}            }
        }}.runTaskLater(main.getInstance(),20L*REQUEST_TIMEOUT);
        tr.setTimeoutTask(to);
        pendingRequests.put(mapKey,tr);
        targetToRequest.put(target.getUniqueId(),tr);
        if (type==TeleportType.TO) requester.sendMessage(Messages.format("tpa-sent","target",target.getName()));
        else requester.sendMessage(Messages.format("tpahere-sent","target",target.getName()));
        requester.playSound(requester.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
        TextComponent base=new TextComponent(Messages.format("request-from","requester",requester.getName()));
        TextComponent click=new TextComponent("§a[CLICK TO ACCEPT]");
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new ComponentBuilder("§7Click to accept").create()));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/tpaccept "+requester.getName()));
        base.addExtra(" ");base.addExtra(click);
        target.spigot().sendMessage(base);
        String barKey = (type == TeleportType.TO)
        	    ? "receive-request-actionbar"
        	    : "receive-tpahere-actionbar";
        target.playSound(requester.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,1f,1f);
        	String receiveBar = Messages.format(barKey, "requester", requester.getName());
        	target.spigot().sendMessage(
        	    ChatMessageType.ACTION_BAR,
        	    new TextComponent(receiveBar)
        	);
        target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(receiveBar));
        
        if (isAutoAccept(target.getUniqueId())) {
            // skip the GUI—just teleport
            acceptRequest(null, target);
            return true;
        }
        return true;
        
    }

    public static void acceptRequest(Inventory clickedInventory, Player clicker) {
        TeleportRequest tr = getRequestForTarget(clicker.getUniqueId());
        if (tr==null){clicker.closeInventory();return;}
        String mapKey=tr.getRequesterId()+":"+tr.getTargetId();removePendingRequest(mapKey);
        Player req= Bukkit.getPlayer(tr.getRequesterUUID()), tgt=Bukkit.getPlayer(tr.getTargetUUID());
        if (req==null||!req.isOnline()||tgt==null||!tgt.isOnline()){clicker.sendMessage(Messages.get("not-online"));clicker.playSound(clicker.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);clicker.closeInventory();return;}
        clicker.closeInventory();
        String msg=Messages.get("accepted-title");req.sendMessage(msg);req.playSound(req.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
        tgt.sendMessage(msg);tgt.playSound(tgt.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f);
        if (tr.getType()==TeleportType.TO) startCountdown(req,tgt,TeleportType.TO);
        else startCountdown(tgt,req,TeleportType.HERE);
    }

    public static void cancelRequest(Inventory clickedInventory, Player clicker) {
        TeleportRequest tr=getRequestForTarget(clicker.getUniqueId());if(tr==null)return;
        String mapKey=tr.getRequesterId()+":"+tr.getTargetId();removePendingRequest(mapKey);
        Player req=Bukkit.getPlayer(tr.getRequesterUUID()), tgt=Bukkit.getPlayer(tr.getTargetUUID());
        if(tgt!=null&&tgt.isOnline()){tgt.sendMessage(Messages.format("deny-notification-target","requester",req.getName()));tgt.playSound(tgt.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);tgt.closeInventory();}
        if(req!=null&&req.isOnline()){req.sendMessage(Messages.format("deny-notification-sender","target",tgt.getName()));req.playSound(req.getLocation(),Sound.ENTITY_VILLAGER_NO,1f,1f);}    }


    private static void startCountdown(Player who, Player destination, TeleportType type) {
        UUID uuid = who.getUniqueId();
        teleportStartLocations.put(uuid, who.getLocation().clone());

        boolean safeTpa = main.getInstance()
            .getConfig().getBoolean("safe-tpa", false);
        double cancelRadius = main.getInstance()
            .getConfig().getDouble("cancel-move-radius", 0.5);
        int[] secs = { COUNTDOWN_SECONDS };

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!who.isOnline() || !destination.isOnline()) {
                    cancelTask(); return;
                }

                // 1) Movement cancel
                Location now = who.getLocation();
                Location start = teleportStartLocations.get(uuid);
                if (start != null && now.distance(start) > cancelRadius) {
                    sendCancel(who, "cancelled-moved-actionbar");
                    cancelTask(); return;
                }

                // 2) Final‐second flying cancel (only if safeTpa enabled)
                if (secs[0] == 1 && safeTpa) {
                    if (who.isFlying() || destination.isFlying()) {
                        sendCancel(who, "cancelled-flying-actionbar");
                        sendCancel(destination, "cancelled-flying-actionbar");
                        cancelTask(); return;
                    }
                }

                // 3) Countdown tick
                secs[0]--;
                if (secs[0] > 0) {
                    String bar = Messages.format("countdown-message", "seconds", "" + secs[0]);
                    who.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar));
                    who.playSound(who.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                } else {
                    // Final teleport
                    who.teleport(destination.getLocation());
                    who.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(Messages.get("teleported-message"))
                    );
                    who.playSound(who.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    cancelTask();
                }
            }

            private void sendCancel(Player p, String key) {
                p.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent(Messages.get(key))
                );
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }

            private void cancelTask() {
                this.cancel();
                teleportTasks.remove(uuid);
                teleportStartLocations.remove(uuid);
            }
        }.runTaskTimer(main.getInstance(), 0L, 20L);

        teleportTasks.put(uuid, task);
    }



    
    public static boolean isTeleporting(UUID player) {
        return teleportTasks.containsKey(player);
    }
    
    public static org.bukkit.Location getStartLocation(UUID player) {
        return teleportStartLocations.get(player);
    }
    
    public static void cancelTeleport(UUID player) {
        BukkitTask task = teleportTasks.remove(player);
        if (task != null) task.cancel();
        teleportStartLocations.remove(player);
    }
    
    /** Toggle auto‐accept for this player. Returns the new state (true = on). */
    public static boolean toggleAutoAccept(UUID player) {
        if (autoAccept.contains(player)) {
            autoAccept.remove(player);
            return false;
        } else {
            autoAccept.add(player);
            return true;
        }
    }
    /** True if player auto‐accepts all requests. */
    public static boolean isAutoAccept(UUID player) {
        return autoAccept.contains(player);
    }

}
    