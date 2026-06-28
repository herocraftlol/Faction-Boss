package fr.factionboss;

import fr.factionboss.commands.BossCommand;
import fr.factionboss.listeners.BossDeathListener;
import fr.factionboss.managers.BossManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionBoss extends JavaPlugin {

    private BossManager bossManager;

    @Override
    public void onEnable() {
        bossManager = new BossManager(this);

        // Enregistrement des listeners
        getServer().getPluginManager().registerEvents(new BossDeathListener(this, bossManager), this);

        // Enregistrement des commandes
        BossCommand bossCommand = new BossCommand(this, bossManager);
        getCommand("bossspawn").setExecutor(bossCommand);
        getCommand("bossinfo").setExecutor(bossCommand);

        // Démarrage du timer de spawn aléatoire
        bossManager.startSpawnTimer();

        getLogger().info("FactionBoss activé ! Le boss spawne toutes les 45min-1h.");
    }

    @Override
    public void onDisable() {
        bossManager.cancelTimer();
        getLogger().info("FactionBoss désactivé.");
    }

    public BossManager getBossManager() {
        return bossManager;
    }
}
