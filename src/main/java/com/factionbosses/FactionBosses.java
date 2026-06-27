package com.factionbosses;

import com.factionbosses.commands.SpawnBossCommand;
import com.factionbosses.listeners.BossDeathListener;
import com.factionbosses.tasks.AutoBossSpawnTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionBosses extends JavaPlugin {

    private static FactionBosses instance;
    private BukkitTask autoSpawnTask;
    private boolean autoSpawnEnabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        autoSpawnEnabled = getConfig().getBoolean("auto-spawn.enabled", true);
        int minInterval = getConfig().getInt("auto-spawn.min-interval-minutes", 45);
        int maxInterval = getConfig().getInt("auto-spawn.max-interval-minutes", 60);
        
        getLogger().info("FactionBosses has been enabled!");
        
        getCommand("spawnboss").setExecutor(new SpawnBossCommand());
        getServer().getPluginManager().registerEvents(new BossDeathListener(), this);
        
        if (autoSpawnEnabled) {
            startAutoSpawn(minInterval, maxInterval);
        }
    }

    @Override
    public void onDisable() {
        if (autoSpawnTask != null) {
            autoSpawnTask.cancel();
        }
        getLogger().info("FactionBosses has been disabled!");
    }

    public static FactionBosses getInstance() {
        return instance;
    }

    public void startAutoSpawn(int minMinutes, int maxMinutes) {
        if (autoSpawnTask != null) {
            autoSpawnTask.cancel();
        }
        
        autoSpawnTask = new AutoBossSpawnTask(minMinutes, maxMinutes).runTaskTimer(this, 0L, 1200L);
        getLogger().info("Auto-spawn started! Bosses will spawn every " + minMinutes + "-" + maxMinutes + " minutes.");
    }

    public void stopAutoSpawn() {
        if (autoSpawnTask != null) {
            autoSpawnTask.cancel();
            autoSpawnTask = null;
            getLogger().info("Auto-spawn stopped.");
        }
    }

    public boolean isAutoSpawnEnabled() {
        return autoSpawnEnabled;
    }
}