package fr.factionboss.listeners;

import fr.factionboss.FactionBoss;
import fr.factionboss.managers.BossManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossDeathListener implements Listener {

    private final FactionBoss plugin;
    private final BossManager bossManager;

    public BossDeathListener(FactionBoss plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!bossManager.isBoss(entity)) return;

        // Supprimer tous les drops vanilla (on gère les drops dans handleBossDeath)
        event.getDrops().clear();
        event.setDroppedExp(500); // XP bonus pour tuer un boss

        Player killer = event.getEntity().getKiller();
        bossManager.handleBossDeath(entity, killer);
    }
}
