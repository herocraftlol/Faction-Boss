package com.factionbosses.commands;

import com.factionbosses.utils.BossManager;
import com.factionbosses.utils.BossType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnBossCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur!");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("factionbosses.spawn")) {
            player.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande!");
            return true;
        }

        BossType type = BossType.ZOMBIE;
        
        if (args.length > 0) {
            try {
                type = BossType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Type invalide! Utilisez: zombie ou skeleton");
                return true;
            }
        }

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (onlinePlayers.size() < 1) {
            player.sendMessage(ChatColor.RED + "Il faut au moins 1 joueur en ligne!");
            return true;
        }

        Location spawnLocation = BossManager.getMedianLocation(onlinePlayers);
        
        if (spawnLocation == null) {
            player.sendMessage(ChatColor.RED + "Impossible de trouver une position!");
            return true;
        }

        LivingEntity boss = BossManager.spawnBoss(type, spawnLocation);
        
        String coords = String.format("x=%d, y=%d, z=%d", 
            spawnLocation.getBlockX(), 
            spawnLocation.getBlockY(), 
            spawnLocation.getBlockZ());

        String bossName = type == BossType.ZOMBIE ? "Zombie Boss" : "Squelette Boss";
        String message = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚔ " +
                        ChatColor.GOLD + "Un " + ChatColor.RED + bossName + 
                        ChatColor.GOLD + " vient d'apparaître en (" + coords + ")!" +
                        "\n" + ChatColor.GREEN + "Tuez-le pour obtenir une récompense! " +
                        ChatColor.DARK_PURPLE + "⚔";

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);

        player.sendMessage(ChatColor.GREEN + "Boss " + type.name() + " spawné avec succès!");
        
        return true;
    }
}