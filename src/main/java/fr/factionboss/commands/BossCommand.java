package fr.factionboss.commands;

import fr.factionboss.FactionBoss;
import fr.factionboss.managers.BossManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class BossCommand implements CommandExecutor {

    private final FactionBoss plugin;
    private final BossManager bossManager;

    public BossCommand(FactionBoss plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        switch (command.getName().toLowerCase()) {

            case "bossspawn" -> {
                if (!sender.hasPermission("factionboss.admin")) {
                    sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
                    return true;
                }
                if (bossManager.isBossAlive()) {
                    sender.sendMessage(Component.text("Un boss est déjà en vie ! Tue-le d'abord.", NamedTextColor.RED));
                    return true;
                }
                boolean success = bossManager.spawnBoss();
                if (!success) {
                    sender.sendMessage(Component.text("Impossible de faire spawner le boss (aucun joueur en ligne ?).", NamedTextColor.RED));
                }
                return true;
            }

            case "bossinfo" -> {
                if (!sender.hasPermission("factionboss.info")) {
                    sender.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
                    return true;
                }
                if (!bossManager.isBossAlive()) {
                    sender.sendMessage(Component.text("Aucun boss n'est actuellement en vie.", NamedTextColor.YELLOW));
                } else {
                    LivingEntity boss = bossManager.getCurrentBoss();
                    String type = bossManager.getCurrentBossType().name();
                    int x = boss.getLocation().getBlockX();
                    int y = boss.getLocation().getBlockY();
                    int z = boss.getLocation().getBlockZ();
                    double hp = Math.round(boss.getHealth() * 10.0) / 10.0;
                    double maxHp = Math.round(boss.getAttribute(
                            org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue() * 10.0) / 10.0;

                    sender.sendMessage(Component.text("--- Boss actuel ---", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Type : " + type, NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Position : X:" + x + " Y:" + y + " Z:" + z, NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("Vie : " + hp + " / " + maxHp, NamedTextColor.RED));
                }
                return true;
            }
        }

        return false;
    }
}
