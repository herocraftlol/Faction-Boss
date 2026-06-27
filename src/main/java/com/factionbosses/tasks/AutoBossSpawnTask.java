package com.factionbosses.tasks;

import com.factionbosses.FactionBosses;
import com.factionbosses.utils.BossManager;
import com.factionbosses.utils.BossType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoBossSpawnTask extends BukkitRunnable {

    private final int minMinutes;
    private final int maxMinutes;
    private final Random random;
    private int currentDelay;
    private int ticksUntilNextSpawn;

    public AutoBossSpawnTask(int minMinutes, int maxMinutes) {
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
        this.random = new Random();
        scheduleNextSpawn();
    }

    private void scheduleNextSpawn() {
        int delayMinutes = minMinutes + random.nextInt(maxMinutes - minMinutes + 1);
        this.ticksUntilNextSpawn = delayMinutes * 60 * 20;
        this.currentDelay = delayMinutes;
        
        FactionBosses.getInstance().getLogger().info("Prochain boss dans " + delayMinutes + " minutes.");
    }

    @Override
    public void run() {
        if (!FactionBosses.getInstance().isAutoSpawnEnabled()) {
            return;
        }

        ticksUntilNextSpawn -= 1200;

        if (ticksUntilNextSpawn <= 0) {
            spawnBoss();
            scheduleNextSpawn();
        }
    }

    private void spawnBoss() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (onlinePlayers.isEmpty()) {
            FactionBosses.getInstance().getLogger().warning("Aucun joueur en ligne! Le boss ne peut pas spawner automatiquement.");
            return;
        }

        BossType[] types = BossType.values();
        BossType randomType = types[random.nextInt(types.length)];

        Location spawnLocation = BossManager.getMedianLocation(onlinePlayers);

        if (spawnLocation == null) {
            FactionBosses.getInstance().getLogger().warning("Impossible de trouver une position pour le boss!");
            return;
        }

        LivingEntity boss = BossManager.spawnBoss(randomType, spawnLocation);

        String coords = String.format("x=%d, y=%d, z=%d",
            spawnLocation.getBlockX(),
            spawnLocation.getBlockY(),
            spawnLocation.getBlockZ());

        String bossName = randomType == BossType.ZOMBIE ? "Zombie Boss" : "Squelette Boss";
        String message = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚔ " +
                ChatColor.GOLD + "Un " + ChatColor.RED + bossName +
                ChatColor.GOLD + " vient d'apparaître en (" + coords + ")!" +
                "\n" + ChatColor.GREEN + "Tuez-le pour obtenir une récompense! " +
                ChatColor.DARK_PURPLE + "⚔";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);

        FactionBosses.getInstance().getLogger().info("Boss " + randomType.name() + " spawné automatiquement!");
    }
}