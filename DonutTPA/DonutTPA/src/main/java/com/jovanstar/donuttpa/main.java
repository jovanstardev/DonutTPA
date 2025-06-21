package com.jovanstar.donuttpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main class for DonutTPA. On enable, we remove any existing /tpa*, /tpahere*, /tpaccept*, /tpacancel*
 * (including Essentials’ registrations) so that only this plugin’s commands remain.
 */
public class main extends JavaPlugin {
    private static main instance;

    private File togglesFile;
    private FileConfiguration togglesCfg;
    
    @Override
    public void onEnable() {
        instance = this;

        // 1) Unregister any existing “tpa” commands (Essentials or other plugin)
        List<String> toRemove = Arrays.asList("tpa", "tpahere", "tpaccept", "tpacancel");
        unregisterOldCommands(toRemove);

        // 2) Now register our own commands as usual
        saveDefaultConfig();
        saveResource("messages.yml", false);
        Messages.load(this);
        TeleportRequestManager.initialize();

        getCommand("tpa").setExecutor(new TpaCommand());
        getCommand("tpahere").setExecutor(new TpahereCommand());
        getCommand("tpa").setTabCompleter(new TpaTabCompleter());
        getCommand("tpahere").setTabCompleter(new TpaTabCompleter());
        getCommand("tpaccept").setExecutor(new TpacceptCommand());
        getCommand("tpaccept").setTabCompleter(new TpacceptTabCompleter());
        getCommand("tpacancel").setExecutor(new TpacancelCommand());
        getCommand("tpatoggle").setExecutor(new TpaToggleCommand());
        getCommand("tpaheretoggle").setExecutor(new TpahereToggleCommand());
        this.getCommand("tpareload").setExecutor(new ReloadCommand());
        getCommand("tpauto").setExecutor(new TpautoCommand());
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String bar = Messages.get("tpauto-on-actionbar");
            for (UUID uuid : TeleportRequestManager.getAutoAcceptList()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        new TextComponent(bar)
                    );
                }
            }
        }, 0L, 0L);

        togglesFile = new File(getDataFolder(), "saves.yml");
        if (!togglesFile.exists()) {
            saveResource("saves.yml", false); // put an empty toggles.yml in your jar under resources/
        }
        togglesCfg = YamlConfiguration.loadConfiguration(togglesFile);

        // 2️⃣ Load each list into your manager sets
        togglesCfg.getStringList("disabled-tpa").forEach(s ->
            TeleportRequestManager.tpaDisabled.add(UUID.fromString(s))
        );
        togglesCfg.getStringList("disabled-tpahere").forEach(s ->
            TeleportRequestManager.tpahereDisabled.add(UUID.fromString(s))
        );
        togglesCfg.getStringList("auto-accept").forEach(s ->
            TeleportRequestManager.autoAccept.add(UUID.fromString(s))
        );
    
        
        getServer().getPluginManager().registerEvents(new TeleportListeners(), this);
    }

    @Override
    public void onDisable() {
    	
        togglesCfg.set("disabled-tpa",
                TeleportRequestManager.tpaDisabled.stream()
                    .map(UUID::toString).toList()
            );
            togglesCfg.set("disabled-tpahere",
                TeleportRequestManager.tpahereDisabled.stream()
                    .map(UUID::toString).toList()
            );
            togglesCfg.set("auto-accept",
                TeleportRequestManager.autoAccept.stream()
                    .map(UUID::toString).toList()
            );
            
            try {
                togglesCfg.save(togglesFile);
            } catch (IOException e) {
                getLogger().severe("Could not save saves.yml!");
                e.printStackTrace();
            }
            
        TeleportRequestManager.shutdown();
    }

    public static main getInstance() {
        return instance;
    }

    /**
     * Use reflection to remove any entries in Bukkit’s command map whose key matches
     * one of the provided labels (exact or namespace:<label>). This ensures Essentials’
     * registrations are dropped before we register our own.
     */
    @SuppressWarnings("unchecked")
    private void unregisterOldCommands(List<String> labels) {
        try {
            // Grab the server’s PluginManager
            if (!(Bukkit.getPluginManager() instanceof SimplePluginManager)) {
                getLogger().warning("Cannot access SimplePluginManager; old TPA commands may remain.");
                return;
            }

            Field commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(Bukkit.getPluginManager());

            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            // Iterate over a copy of the key set to avoid CME
            for (String key : List.copyOf(knownCommands.keySet())) {
                String lowerKey = key.toLowerCase(); // keys are lower‐case by default
                for (String label : labels) {
                    // If exactly “tpa” or if namespace ends with “:tpa” (e.g. “essentials:tpa”)
                    if (lowerKey.equals(label) || lowerKey.endsWith(":" + label)) {
                        Command cmd = knownCommands.get(key);
                        if (cmd instanceof PluginCommand) {
                            PluginCommand pc = (PluginCommand) cmd;
                            getLogger().info("Removing old command registration: " 
                                    + key + " (plugin=" + pc.getPlugin().getName() + ")");
                        } else {
                            getLogger().info("Removing old command registration: " + key);
                        }
                        knownCommands.remove(key);
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            getLogger().severe("Failed to unregister old TPA commands: " + ex);
            ex.printStackTrace();
        }
        
        
    }
    
    
}
