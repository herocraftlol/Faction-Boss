package fr.factionboss.managers;

import fr.factionboss.FactionBoss;
import fr.factionboss.models.BossType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossManager {

    private final FactionBoss plugin;
    private final NamespacedKey bossKey;

    // Boss actuellement en vie
    private LivingEntity currentBoss = null;
    private BossType currentBossType = null;

    // Timer
    private BukkitTask spawnTask = null;

    // Intervalle : 45min = 54000 ticks, 1h = 72000 ticks
    private static final long MIN_DELAY = 54000L;
    private static final long MAX_DELAY = 72000L;

    public BossManager(FactionBoss plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "faction_boss");
    }

    // ─── Timer ───────────────────────────────────────────────────────────────

    public void startSpawnTimer() {
        scheduleNextSpawn();
    }

    private void scheduleNextSpawn() {
        long delay = MIN_DELAY + (long) (Math.random() * (MAX_DELAY - MIN_DELAY));
        double minutes = delay / 20.0 / 60.0;
        plugin.getLogger().info(String.format("Prochain boss dans %.1f minutes.", minutes));

        spawnTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentBoss == null || currentBoss.isDead()) {
                spawnBoss();
            } else {
                plugin.getLogger().info("Un boss est déjà en vie, report du spawn.");
            }
            scheduleNextSpawn();
        }, delay);
    }

    public void cancelTimer() {
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
    }

    // ─── Spawn ───────────────────────────────────────────────────────────────

    public boolean spawnBoss() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.isEmpty()) {
            plugin.getLogger().info("Aucun joueur en ligne, spawn du boss annulé.");
            return false;
        }

        Location spawnLoc = calculateMedianLocation(players);
        if (spawnLoc == null) {
            plugin.getLogger().warning("Impossible de calculer l'emplacement médian.");
            return false;
        }

        // Choisir le type de boss aléatoirement
        BossType type = Math.random() < 0.5 ? BossType.ZOMBIE : BossType.SKELETON;

        LivingEntity boss;
        if (type == BossType.ZOMBIE) {
            boss = spawnZombieBoss(spawnLoc);
        } else {
            boss = spawnSkeletonBoss(spawnLoc);
        }

        currentBoss = boss;
        currentBossType = type;

        // Annonce dans le chat
        int bx = spawnLoc.getBlockX();
        int by = spawnLoc.getBlockY();
        int bz = spawnLoc.getBlockZ();

        String typeName = (type == BossType.ZOMBIE) ? "§cZombie Boss" : "§9Squelette Boss";

        Component message = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("  ☠ UN BOSS VIENT D'APPARAÎTRE ! ☠", NamedTextColor.RED, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Type : ", NamedTextColor.GRAY))
                .append(Component.text(type == BossType.ZOMBIE ? "Zombie Boss" : "Squelette Boss",
                        type == BossType.ZOMBIE ? NamedTextColor.RED : NamedTextColor.AQUA, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Coordonnées : ", NamedTextColor.GRAY))
                .append(Component.text("X:" + bx + " Y:" + by + " Z:" + bz, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Tuez-le pour obtenir une récompense !", NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        Bukkit.broadcast(message);

        plugin.getLogger().info("Boss spawné : " + type.name() + " en " + bx + " " + by + " " + bz);
        return true;
    }

    // ─── Zombie Boss ─────────────────────────────────────────────────────────

    private Zombie spawnZombieBoss(Location loc) {
        Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        // Nom du boss
        zombie.customName(Component.text("☠ Zombie Boss ☠", NamedTextColor.RED, TextDecoration.BOLD));
        zombie.setCustomNameVisible(true);

        // Santé augmentée
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(100.0);
        zombie.setHealth(100.0);

        // Speed 2
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);

        // Équipement
        EntityEquipment eq = zombie.getEquipment();

        // Épée en diamant : Tranchant 3 + Mending
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.SHARPNESS, 3, true);
        swordMeta.addEnchant(Enchantment.MENDING, 1, true);
        swordMeta.displayName(Component.text("Lame du Boss", NamedTextColor.RED, TextDecoration.BOLD));
        sword.setItemMeta(swordMeta);

        // Armure en diamant enchantée
        ItemStack helmet     = enchantedArmor(Material.DIAMOND_HELMET);
        ItemStack chestplate = enchantedArmor(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings   = enchantedArmor(Material.DIAMOND_LEGGINGS);
        ItemStack boots      = enchantedArmor(Material.DIAMOND_BOOTS);

        eq.setItemInMainHand(sword);
        eq.setHelmet(helmet);
        eq.setChestplate(chestplate);
        eq.setLeggings(leggings);
        eq.setBoots(boots);

        // Taux de drop à 0% pour l'équipement par défaut (on gère le drop manuellement)
        eq.setItemInMainHandDropChance(0f);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);

        // Marquer comme boss
        zombie.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "zombie");

        // Empêcher le despawn
        zombie.setRemoveWhenFarAway(false);

        return zombie;
    }

    // ─── Skeleton Boss ───────────────────────────────────────────────────────

    private Skeleton spawnSkeletonBoss(Location loc) {
        Skeleton skeleton = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);

        // Nom du boss
        skeleton.customName(Component.text("☠ Squelette Boss ☠", NamedTextColor.AQUA, TextDecoration.BOLD));
        skeleton.setCustomNameVisible(true);

        // Santé augmentée
        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(80.0);
        skeleton.setHealth(80.0);

        // Speed 1
        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.30);

        // Équipement
        EntityEquipment eq = skeleton.getEquipment();

        // Arc : Power 2 + Mending
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.POWER, 2, true);
        bowMeta.addEnchant(Enchantment.MENDING, 1, true);
        bowMeta.displayName(Component.text("Arc du Boss", NamedTextColor.AQUA, TextDecoration.BOLD));
        bow.setItemMeta(bowMeta);

        // Armure en diamant enchantée
        ItemStack helmet     = enchantedArmor(Material.DIAMOND_HELMET);
        ItemStack chestplate = enchantedArmor(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings   = enchantedArmor(Material.DIAMOND_LEGGINGS);
        ItemStack boots      = enchantedArmor(Material.DIAMOND_BOOTS);

        eq.setItemInMainHand(bow);
        eq.setHelmet(helmet);
        eq.setChestplate(chestplate);
        eq.setLeggings(leggings);
        eq.setBoots(boots);

        eq.setItemInMainHandDropChance(0f);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);

        // Marquer comme boss
        skeleton.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "skeleton");

        // Empêcher le despawn
        skeleton.setRemoveWhenFarAway(false);

        return skeleton;
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    /**
     * Crée une pièce d'armure en diamant avec Protection 3 + Unbreaking 3.
     */
    private ItemStack enchantedArmor(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.PROTECTION, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Calcule la localisation médiane entre tous les joueurs en ligne.
     * On prend la médiane des X et Z, et on cherche un Y solide.
     */
    private Location calculateMedianLocation(List<Player> players) {
        if (players.isEmpty()) return null;

        List<Double> xs = new ArrayList<>();
        List<Double> zs = new ArrayList<>();
        World world = null;

        for (Player p : players) {
            xs.add(p.getLocation().getX());
            zs.add(p.getLocation().getZ());
            if (world == null) world = p.getWorld();
        }

        Collections.sort(xs);
        Collections.sort(zs);

        double medX = xs.get(xs.size() / 2);
        double medZ = zs.get(zs.size() / 2);

        // Trouver le Y le plus haut à cet endroit (sol)
        Location candidate = new Location(world, medX, 64, medZ);
        int highestY = world.getHighestBlockYAt((int) medX, (int) medZ);
        candidate.setY(highestY + 1);

        return candidate;
    }

    /**
     * Vérifie si une entité est le boss actuel.
     */
    public boolean isBoss(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    /**
     * Gère la mort du boss : drops et annonce.
     */
    public void handleBossDeath(LivingEntity boss, Player killer) {
        Location loc = boss.getLocation();

        // Déterminer le type
        String bossTypeStr = boss.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        boolean isZombie = "zombie".equals(bossTypeStr);

        // Drop aléatoire : item enchanté OU diamants
        boolean dropItem = Math.random() < 0.5;

        if (dropItem) {
            if (isZombie) {
                // Drop épée Tranchant 3 + Mending
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta meta = sword.getItemMeta();
                meta.addEnchant(Enchantment.SHARPNESS, 3, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.displayName(Component.text("✦ Lame du Boss ✦", NamedTextColor.RED, TextDecoration.BOLD));
                sword.setItemMeta(meta);
                loc.getWorld().dropItemNaturally(loc, sword);
            } else {
                // Drop arc Power 2 + Mending
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta meta = bow.getItemMeta();
                meta.addEnchant(Enchantment.POWER, 2, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.displayName(Component.text("✦ Arc du Boss ✦", NamedTextColor.AQUA, TextDecoration.BOLD));
                bow.setItemMeta(meta);
                loc.getWorld().dropItemNaturally(loc, bow);
            }
        } else {
            // Drop 5 à 10 diamants
            int amount = 5 + (int) (Math.random() * 6);
            loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, amount));
        }

        // Réinitialiser le boss courant
        currentBoss = null;
        currentBossType = null;

        // Annonce dans le chat
        String killerName = (killer != null) ? killer.getName() : "un inconnu";
        String bossDisplayName = isZombie ? "Zombie Boss" : "Squelette Boss";

        Component deathMsg = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED)
                .appendNewline()
                .append(Component.text("  ✔ LE BOSS A ÉTÉ ÉLIMINÉ !", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Boss : ", NamedTextColor.GRAY))
                .append(Component.text(bossDisplayName, isZombie ? NamedTextColor.RED : NamedTextColor.AQUA, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Tué par : ", NamedTextColor.GRAY))
                .append(Component.text(killerName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Les récompenses ont été droppées sur place !", NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED));

        Bukkit.broadcast(deathMsg);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public LivingEntity getCurrentBoss() { return currentBoss; }
    public BossType getCurrentBossType() { return currentBossType; }
    public boolean isBossAlive() { return currentBoss != null && !currentBoss.isDead(); }
}
