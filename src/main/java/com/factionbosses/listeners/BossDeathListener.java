package com.factionbosses.listeners;

import com.factionbosses.utils.BossManager;
import com.factionbosses.utils.BossType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossDeathListener implements Listener {

    private static final Map<String, BossType> BOSS_CUSTOM_NAMES = new HashMap<>();
    
    static {
        BOSS_CUSTOM_NAMES.put("BOSS ZOMBIE", BossType.ZOMBIE);
        BOSS_CUSTOM_NAMES.put("BOSS SKELETON", BossType.SKELETON);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String customName = entity.getCustomName();
        
        if (customName == null) {
            return;
        }

        BossType bossType = null;
        for (Map.Entry<String, BossType> entry : BOSS_CUSTOM_NAMES.entrySet()) {
            if (customName.contains(entry.getKey())) {
                bossType = entry.getValue();
                break;
            }
        }

        if (bossType == null) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        
        if (killer == null) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        BossManager.announceBossDeath(killer, bossType);

        if (BossManager.shouldDropWeapon(bossType)) {
            if (bossType == BossType.ZOMBIE) {
                ItemStack sword = BossManager.createSword();
                entity.getWorld().dropItemNaturally(entity.getLocation(), sword);
            } else {
                ItemStack bow = BossManager.createBow();
                entity.getWorld().dropItemNaturally(entity.getLocation(), bow);
            }
        }

        if (BossManager.shouldDropDiamonds(bossType)) {
            int diamonds = BossManager.getRandomDiamonds();
            ItemStack diamondDrop = new ItemStack(Material.DIAMOND, diamonds);
            entity.getWorld().dropItemNaturally(entity.getLocation(), diamondDrop);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.AQUA + "" + diamonds + " diamants" + 
                    ChatColor.GOLD + " sont tombés du boss!");
            }
        }
    }

    public static ItemStack createEnchantedSword() {
        return BossManager.createSword();
    }

    public static ItemStack createEnchantedBow() {
        return BossManager.createBow();
    }
}