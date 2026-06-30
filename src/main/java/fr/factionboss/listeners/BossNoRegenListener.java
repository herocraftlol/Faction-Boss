package fr.factionboss.listeners;

import fr.factionboss.managers.BossManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * Empêche les boss de régénérer leur vie (naturellement via la faim,
 * ou via tout autre effet de soin).
 */
public class BossNoRegenListener implements Listener {

    private final BossManager bossManager;

    public BossNoRegenListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!bossManager.isBoss(entity)) return;

        event.setCancelled(true);
    }
}
