package com.factionbosses.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BossManager {

    private static final Random random = new Random();

    public static LivingEntity spawnBoss(BossType type, Location location) {
        LivingEntity boss = (LivingEntity) location.getWorld().spawnEntity(location, type.getEntityType());
        
        boss.setCustomName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "BOSS " + type.name());
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);
        boss.setGlowing(true);

        if (type == BossType.ZOMBIE) {
            setupZombieBoss((Zombie) boss);
        } else if (type == BossType.SKELETON) {
            setupSkeletonBoss((Skeleton) boss);
        }

        return boss;
    }

    private static void setupZombieBoss(Zombie zombie) {
        zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(100);
        zombie.setHealth(100);
        zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25);
        zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(10);

        EntityEquipment equipment = zombie.getEquipment();
        
        ItemStack sword = createSword();
        equipment.setItemInMainHand(sword);
        equipment.setItemInOffHand(null);
        
        ItemStack helmet = createEnchantedDiamondArmor(Material.DIAMOND_HELMET);
        ItemStack chestplate = createEnchantedDiamondArmor(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = createEnchantedDiamondArmor(Material.DIAMOND_LEGGINGS);
        ItemStack boots = createEnchantedDiamondArmor(Material.DIAMOND_BOOTS);
        
        equipment.setHelmet(helmet);
        equipment.setChestplate(chestplate);
        equipment.setLeggings(leggings);
        equipment.setBoots(boots);

        equipment.setHelmetDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setLeggingsDropChance(0);
        equipment.setBootsDropChance(0);
        equipment.setItemInMainHandDropChance(0);
    }

    private static void setupSkeletonBoss(Skeleton skeleton) {
        skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(80);
        skeleton.setHealth(80);
        skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.23);
        skeleton.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(8);

        EntityEquipment equipment = skeleton.getEquipment();
        
        ItemStack bow = createBow();
        equipment.setItemInMainHand(bow);
        equipment.setItemInOffHand(null);
        
        ItemStack helmet = createEnchantedDiamondArmor(Material.DIAMOND_HELMET);
        ItemStack chestplate = createEnchantedDiamondArmor(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = createEnchantedDiamondArmor(Material.DIAMOND_LEGGINGS);
        ItemStack boots = createEnchantedDiamondArmor(Material.DIAMOND_BOOTS);
        
        equipment.setHelmet(helmet);
        equipment.setChestplate(chestplate);
        equipment.setLeggings(leggings);
        equipment.setBoots(boots);

        equipment.setHelmetDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setLeggingsDropChance(0);
        equipment.setBootsDropChance(0);
        equipment.setItemInMainHandDropChance(0);
    }

    public static ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.setUnbreakable(true);
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Epée de Boss");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    public static ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.POWER, 2, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.setUnbreakable(true);
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Arc de Boss");
            bow.setItemMeta(meta);
        }
        return bow;
    }

    private static ItemStack createEnchantedDiamondArmor(Material material) {
        ItemStack armor = new ItemStack(material);
        ItemMeta meta = armor.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 2, true);
            meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            meta.setUnbreakable(true);
            armor.setItemMeta(meta);
        }
        return armor;
    }

    public static Location getMedianLocation(List<Player> players) {
        if (players.isEmpty()) {
            return null;
        }

        double medianX = 0;
        double medianY = 0;
        double medianZ = 0;

        for (Player player : players) {
            medianX += player.getLocation().getX();
            medianY += player.getLocation().getY();
            medianZ += player.getLocation().getZ();
        }

        medianX /= players.size();
        medianY /= players.size();
        medianZ /= players.size();

        Location medianLocation = players.get(0).getLocation().clone();
        medianLocation.setX(medianX);
        medianLocation.setY(medianY);
        medianLocation.setZ(medianZ);
        
        medianLocation.setY(medianLocation.getWorld().getHighestBlockYAt((int) medianX, (int) medianZ) + 1);

        return medianLocation;
    }

    public static int getRandomDiamonds() {
        return random.nextInt(6) + 5;
    }

    public static boolean shouldDropWeapon(BossType type) {
        return random.nextDouble() < 0.35;
    }

    public static boolean shouldDropDiamonds(BossType type) {
        return random.nextDouble() < 0.65;
    }

    public static void announceBossDeath(Player killer, BossType type) {
        String message = ChatColor.GOLD + "" + ChatColor.BOLD + "☠ " + 
                        ChatColor.RED + "Le Boss " + type.name() + 
                        ChatColor.GOLD + " a été tué par " + 
                        ChatColor.AQUA + killer.getName() + 
                        ChatColor.GOLD + " ! ☠";
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
}