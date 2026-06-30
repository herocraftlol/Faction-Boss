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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossManager {

    private final FactionBoss plugin;
    private final NamespacedKey bossKey;

    // Boss actuellement en vie
    private LivingEntity currentBoss = null;
    private BossType currentBossType = null;

    // Minions actuellement invoqués par le boss et toujours en vie
    private final List<LivingEntity> activeMinions = new ArrayList<>();

    // Timers
    private BukkitTask spawnTask = null;
    private BukkitTask powerTask = null;   // Pouvoirs du boss (toutes les 60s)
    private BukkitTask healthDisplayTask = null; // Affichage de la vie au-dessus de la tête

    // Nom de base du boss actuel (sans la vie), utilisé pour reconstruire l'affichage
    private String currentBossBaseName = null;
    private NamedTextColor currentBossNameColor = null;

    // Intervalle entre chaque spawn de boss : 1h15 = 90000 ticks
    private static final long SPAWN_DELAY = 90000L;
    private static final long POWER_TICK  = 1200L; // 60 secondes
    private static final long HEALTH_DISPLAY_TICK = 10L; // 0.5 seconde : actualisation en temps réel

    // Distance minimale de spawn par rapport aux joueurs (en blocs)
    private static final double MIN_SPAWN_DISTANCE = 50.0;
    // Rayon max de recherche autour du point médian
    private static final int SEARCH_RADIUS = 200;
    // Nombre de tentatives max pour trouver un emplacement valide
    private static final int MAX_ATTEMPTS  = 30;

    public BossManager(FactionBoss plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "faction_boss");
    }

    // ─── Timer ───────────────────────────────────────────────────────────────

    public void startSpawnTimer() {
        scheduleNextSpawn();
    }

    private void scheduleNextSpawn() {
        long delay = SPAWN_DELAY;
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
        if (spawnTask != null) { spawnTask.cancel(); spawnTask = null; }
        cancelPowerTask();
        cancelHealthDisplayTask();
    }

    private void cancelPowerTask() {
        if (powerTask != null) { powerTask.cancel(); powerTask = null; }
    }

    private void cancelHealthDisplayTask() {
        if (healthDisplayTask != null) { healthDisplayTask.cancel(); healthDisplayTask = null; }
    }

    // ─── Spawn ───────────────────────────────────────────────────────────────

    public boolean spawnBoss() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.isEmpty()) {
            plugin.getLogger().info("Aucun joueur en ligne, spawn du boss annulé.");
            return false;
        }

        Location spawnLoc = findSafeSpawnLocation(players);
        if (spawnLoc == null) {
            plugin.getLogger().warning("Impossible de trouver un emplacement valide (50+ blocs des joueurs).");
            return false;
        }

        // Choisir le type de boss aléatoirement parmi tous les types
        BossType[] types = BossType.values();
        BossType type = types[(int) (Math.random() * types.length)];

        LivingEntity boss = switch (type) {
            case ZOMBIE         -> spawnZombieBoss(spawnLoc);
            case SKELETON       -> spawnSkeletonBoss(spawnLoc);
            case WITHER_SKELETON -> spawnWitherSkeletonBoss(spawnLoc);
            case SPIDER         -> spawnSpiderBoss(spawnLoc);
            case BLAZE          -> spawnBlazeBoss(spawnLoc);
            case WITCH          -> spawnWitchBoss(spawnLoc);
        };

        currentBoss     = boss;
        currentBossType = type;
        activeMinions.clear();
        currentBossBaseName  = getBossDisplayName(type);
        currentBossNameColor = getBossColor(type);

        startPowerTask(boss, type);
        startHealthDisplayTask(boss);

        // Annonce dans le chat
        int bx = spawnLoc.getBlockX();
        int by = spawnLoc.getBlockY();
        int bz = spawnLoc.getBlockZ();

        Component message = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text("  ☠ UN BOSS VIENT D'APPARAÎTRE ! ☠", NamedTextColor.RED, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Type : ", NamedTextColor.GRAY))
                .append(Component.text(getBossDisplayName(type), getBossColor(type), TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Pouvoir : ", NamedTextColor.GRAY))
                .append(Component.text(getBossPowerDescription(type), NamedTextColor.LIGHT_PURPLE))
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

    // ─── Recherche d'emplacement sécurisé ────────────────────────────────────

    /**
     * Trouve un emplacement à au moins MIN_SPAWN_DISTANCE blocs de tous les joueurs,
     * autour du point médian des joueurs dans un rayon SEARCH_RADIUS.
     */
    private Location findSafeSpawnLocation(List<Player> players) {
        Location median = calculateMedianLocation(players);
        if (median == null) return null;

        World world = median.getWorld();
        Random rng  = new Random();

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Décalage aléatoire dans le rayon
            double angle   = rng.nextDouble() * 2 * Math.PI;
            double distance = MIN_SPAWN_DISTANCE + rng.nextDouble() * (SEARCH_RADIUS - MIN_SPAWN_DISTANCE);

            double candidateX = median.getX() + Math.cos(angle) * distance;
            double candidateZ = median.getZ() + Math.sin(angle) * distance;

            int highestY = world.getHighestBlockYAt((int) candidateX, (int) candidateZ);
            Location candidate = new Location(world, candidateX, highestY + 1, candidateZ);

            // Vérifier que le point est bien à 50+ blocs de TOUS les joueurs
            if (isFarEnoughFromAllPlayers(candidate, players)) {
                return candidate;
            }
        }

        // Dernier recours : forcer à MIN_SPAWN_DISTANCE exactement dans une direction aléatoire
        double angle = rng.nextDouble() * 2 * Math.PI;
        double cx    = median.getX() + Math.cos(angle) * (MIN_SPAWN_DISTANCE + 10);
        double cz    = median.getZ() + Math.sin(angle) * (MIN_SPAWN_DISTANCE + 10);
        int hy       = world.getHighestBlockYAt((int) cx, (int) cz);
        return new Location(world, cx, hy + 1, cz);
    }

    private boolean isFarEnoughFromAllPlayers(Location loc, List<Player> players) {
        for (Player p : players) {
            if (p.getWorld().equals(loc.getWorld())) {
                double dist = loc.distance(p.getLocation());
                if (dist < MIN_SPAWN_DISTANCE) return false;
            }
        }
        return true;
    }

    // ─── Pouvoirs du boss (toutes les 60 secondes) ───────────────────────────

    private void startPowerTask(LivingEntity boss, BossType type) {
        cancelPowerTask();
        powerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (boss == null || boss.isDead()) {
                cancelPowerTask();
                return;
            }
            activateBossPower(boss, type);
        }, POWER_TICK, POWER_TICK);
    }

    /**
     * Démarre la tâche qui affiche et actualise en temps réel la vie du boss
     * au-dessus de sa tête (ex : "Zombie Boss ❤ 85/150").
     */
    private void startHealthDisplayTask(LivingEntity boss) {
        cancelHealthDisplayTask();
        updateBossHealthDisplay(boss);
        healthDisplayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (boss == null || boss.isDead() || !boss.isValid()) {
                cancelHealthDisplayTask();
                return;
            }
            updateBossHealthDisplay(boss);
        }, HEALTH_DISPLAY_TICK, HEALTH_DISPLAY_TICK);
    }

    /** Met à jour le nom affiché au-dessus du boss avec sa vie actuelle. */
    private void updateBossHealthDisplay(LivingEntity boss) {
        if (currentBossBaseName == null) return;

        double health    = Math.max(0, boss.getHealth());
        double maxHealth = Objects.requireNonNull(boss.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();

        Component name = Component.text(currentBossBaseName + " ", currentBossNameColor, TextDecoration.BOLD)
                .append(Component.text("❤ ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text((int) Math.ceil(health) + "/" + (int) Math.ceil(maxHealth),
                        NamedTextColor.WHITE, TextDecoration.BOLD));

        boss.customName(name);
        boss.setCustomNameVisible(true);
    }

    private void activateBossPower(LivingEntity boss, BossType type) {
        // Nettoyer la liste des minions morts/invalides
        activeMinions.removeIf(m -> m == null || m.isDead() || !m.isValid());

        // Ne pas invoquer de nouveaux monstres tant que ceux déjà invoqués sont en vie
        if (!activeMinions.isEmpty()) {
            plugin.getLogger().info("Invocation du boss reportée : " + activeMinions.size()
                    + " minion(s) encore en vie.");
            return;
        }

        Location bossLoc = boss.getLocation();
        World world      = bossLoc.getWorld();

        switch (type) {

            case ZOMBIE -> {
                // Invoque 3–5 zombies autour du boss
                int count = 3 + (int) (Math.random() * 3);
                for (int i = 0; i < count; i++) {
                    Location ml = randomNearby(bossLoc, 5);
                    Zombie minion = (Zombie) world.spawnEntity(ml, EntityType.ZOMBIE);
                    minion.customName(Component.text("§cServiteur Zombie", NamedTextColor.RED));
                    minion.setCustomNameVisible(true);
                    Objects.requireNonNull(minion.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(20.0);
                    minion.setHealth(20.0);
                    activeMinions.add(minion);
                }
                broadcastPower(bossLoc, "§c☠ Le Zombie Boss invoque ses serviteurs !");
            }

            case SKELETON -> {
                // Invoque 3 squelettes + fait pleuvoir des flèches sur les joueurs proches
                int count = 3;
                for (int i = 0; i < count; i++) {
                    Location ml = randomNearby(bossLoc, 5);
                    Skeleton minion = (Skeleton) world.spawnEntity(ml, EntityType.SKELETON);
                    minion.customName(Component.text("Archer Minion", NamedTextColor.AQUA));
                    minion.setCustomNameVisible(true);
                    activeMinions.add(minion);
                }
                // Éclairs autour des joueurs proches (effet visuel)
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(bossLoc) < 30) {
                        world.strikeLightningEffect(p.getLocation());
                    }
                }
                broadcastPower(bossLoc, "§9☠ Le Squelette Boss convoque ses archers !");
            }

            case WITHER_SKELETON -> {
                // Applique l'effet Wither aux joueurs proches + invoque 2 wither skeletons
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(bossLoc) < 20) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                    }
                }
                for (int i = 0; i < 2; i++) {
                    Location ml = randomNearby(bossLoc, 5);
                    WitherSkeleton ws = (WitherSkeleton) world.spawnEntity(ml, EntityType.WITHER_SKELETON);
                    ws.customName(Component.text("Ombre Noire", NamedTextColor.DARK_GRAY));
                    ws.setCustomNameVisible(true);
                    activeMinions.add(ws);
                }
                // Explosion visuelle (aucun dégât)
                world.createExplosion(bossLoc, 0f, false, false);
                broadcastPower(bossLoc, "§8☠ Le Wither Boss répand la corruption !");
            }

            case SPIDER -> {
                // Invoque 4 araignées + applique Poison et Cécité aux proches
                for (int i = 0; i < 4; i++) {
                    Location ml = randomNearby(bossLoc, 6);
                    Spider spider = (Spider) world.spawnEntity(ml, EntityType.SPIDER);
                    spider.customName(Component.text("Toile Venimeuse", NamedTextColor.DARK_GREEN));
                    spider.setCustomNameVisible(true);
                    activeMinions.add(spider);
                }
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(bossLoc) < 15) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    }
                }
                broadcastPower(bossLoc, "§2☠ L'Araignée Boss tisse sa toile de poison !");
            }

            case BLAZE -> {
                // Met le feu aux joueurs proches + invoque 3 blazes + frappe d'éclair enflammé
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(bossLoc) < 20) {
                        p.setFireTicks(100); // 5 secondes de feu
                        world.strikeLightning(p.getLocation());
                    }
                }
                for (int i = 0; i < 3; i++) {
                    Location ml = randomNearby(bossLoc, 5);
                    Blaze blaze = (Blaze) world.spawnEntity(ml, EntityType.BLAZE);
                    blaze.customName(Component.text("Flamme Infernale", NamedTextColor.GOLD));
                    blaze.setCustomNameVisible(true);
                    activeMinions.add(blaze);
                }
                broadcastPower(bossLoc, "§6☠ Le Blaze Boss embrase les cieux !");
            }

            case WITCH -> {
                // Lance des potions de splash sur les joueurs proches + invoque 2 sorcières
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(bossLoc) < 20) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    }
                }
                for (int i = 0; i < 2; i++) {
                    Location ml = randomNearby(bossLoc, 6);
                    Witch witch = (Witch) world.spawnEntity(ml, EntityType.WITCH);
                    witch.customName(Component.text("Apprentie Sorcière", NamedTextColor.DARK_PURPLE));
                    witch.setCustomNameVisible(true);
                    activeMinions.add(witch);
                }
                // Téléporte la sorcière aléatoirement (comportement chaotique)
                Location teleportLoc = randomNearby(bossLoc, 10);
                teleportLoc.setY(world.getHighestBlockYAt(teleportLoc.getBlockX(), teleportLoc.getBlockZ()) + 1);
                boss.teleport(teleportLoc);
                world.spawnParticle(Particle.WITCH, bossLoc, 30);
                broadcastPower(bossLoc, "§5☠ La Sorcière Boss disparaît dans les ombres !");
            }
        }
    }

    /** Diffuse un message de pouvoir uniquement aux joueurs proches du boss. */
    private void broadcastPower(Location loc, String msg) {
        Component c = Component.text(msg, NamedTextColor.LIGHT_PURPLE);
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) < 100) {
                p.sendMessage(c);
            }
        }
    }

    /** Génère une location aléatoire autour d'un point dans un rayon donné. */
    private Location randomNearby(Location center, double radius) {
        Random rng = new Random();
        double angle = rng.nextDouble() * 2 * Math.PI;
        double dist  = rng.nextDouble() * radius;
        double nx    = center.getX() + Math.cos(angle) * dist;
        double nz    = center.getZ() + Math.sin(angle) * dist;
        World world  = center.getWorld();
        int ny       = world.getHighestBlockYAt((int) nx, (int) nz);
        return new Location(world, nx, ny + 1, nz);
    }

    // ─── Spawn des boss ───────────────────────────────────────────────────────

    private Zombie spawnZombieBoss(Location loc) {
        Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);

        zombie.customName(Component.text("☠ Zombie Boss ☠", NamedTextColor.RED, TextDecoration.BOLD));
        zombie.setCustomNameVisible(true);

        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(150.0);
        zombie.setHealth(150.0);
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.35);
        Objects.requireNonNull(zombie.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(8.0);

        EntityEquipment eq = zombie.getEquipment();
        eq.setItemInMainHand(enchantedSword(Material.DIAMOND_SWORD, NamedTextColor.RED, "Lame du Mort-Vivant", 4, 2));
        setFullDiamondArmor(eq);

        zombie.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "zombie");
        zombie.setRemoveWhenFarAway(false);
        return zombie;
    }

    private Skeleton spawnSkeletonBoss(Location loc) {
        Skeleton skeleton = (Skeleton) loc.getWorld().spawnEntity(loc, EntityType.SKELETON);

        skeleton.customName(Component.text("☠ Squelette Boss ☠", NamedTextColor.AQUA, TextDecoration.BOLD));
        skeleton.setCustomNameVisible(true);

        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(120.0);
        skeleton.setHealth(120.0);
        Objects.requireNonNull(skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.30);

        EntityEquipment eq = skeleton.getEquipment();
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.POWER, 3, true);
        bowMeta.addEnchant(Enchantment.PUNCH, 2, true);
        bowMeta.addEnchant(Enchantment.MENDING, 1, true);
        bowMeta.displayName(Component.text("Arc du Boss", NamedTextColor.AQUA, TextDecoration.BOLD));
        bow.setItemMeta(bowMeta);
        eq.setItemInMainHand(bow);
        setFullDiamondArmor(eq);

        skeleton.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "skeleton");
        skeleton.setRemoveWhenFarAway(false);
        return skeleton;
    }

    private WitherSkeleton spawnWitherSkeletonBoss(Location loc) {
        WitherSkeleton ws = (WitherSkeleton) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);

        ws.customName(Component.text("☠ Wither Boss ☠", NamedTextColor.DARK_GRAY, TextDecoration.BOLD));
        ws.setCustomNameVisible(true);

        Objects.requireNonNull(ws.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(200.0);
        ws.setHealth(200.0);
        Objects.requireNonNull(ws.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.32);
        Objects.requireNonNull(ws.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(10.0);

        // Armure noire (netherite)
        EntityEquipment eq = ws.getEquipment();
        eq.setItemInMainHand(enchantedSword(Material.NETHERITE_SWORD, NamedTextColor.DARK_GRAY, "Épée du Néant", 5, 3));
        setFullNetheriteArmor(eq);

        ws.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "wither_skeleton");
        ws.setRemoveWhenFarAway(false);
        return ws;
    }

    private Spider spawnSpiderBoss(Location loc) {
        Spider spider = (Spider) loc.getWorld().spawnEntity(loc, EntityType.SPIDER);

        spider.customName(Component.text("☠ Araignée Boss ☠", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
        spider.setCustomNameVisible(true);

        Objects.requireNonNull(spider.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(100.0);
        spider.setHealth(100.0);
        Objects.requireNonNull(spider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.40);
        Objects.requireNonNull(spider.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)).setBaseValue(7.0);

        // Effet permanent : Vitesse + Vision nocturne
        spider.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        spider.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));

        spider.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "spider");
        spider.setRemoveWhenFarAway(false);
        return spider;
    }

    private Blaze spawnBlazeBoss(Location loc) {
        Blaze blaze = (Blaze) loc.getWorld().spawnEntity(loc, EntityType.BLAZE);

        blaze.customName(Component.text("☠ Blaze Boss ☠", NamedTextColor.GOLD, TextDecoration.BOLD));
        blaze.setCustomNameVisible(true);

        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(130.0);
        blaze.setHealth(130.0);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.28);
        Objects.requireNonNull(blaze.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(48.0);

        blaze.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        blaze.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "blaze");
        blaze.setRemoveWhenFarAway(false);
        return blaze;
    }

    private Witch spawnWitchBoss(Location loc) {
        Witch witch = (Witch) loc.getWorld().spawnEntity(loc, EntityType.WITCH);

        witch.customName(Component.text("☠ Sorcière Boss ☠", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        witch.setCustomNameVisible(true);

        Objects.requireNonNull(witch.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(110.0);
        witch.setHealth(110.0);
        Objects.requireNonNull(witch.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(0.28);

        witch.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));

        witch.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, "witch");
        witch.setRemoveWhenFarAway(false);
        return witch;
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────

    private ItemStack enchantedSword(Material mat, NamedTextColor color, String name, int sharpness, int fire) {
        ItemStack sword = new ItemStack(mat);
        ItemMeta meta = sword.getItemMeta();
        meta.addEnchant(Enchantment.SHARPNESS, sharpness, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, fire, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.displayName(Component.text(name, color, TextDecoration.BOLD));
        sword.setItemMeta(meta);
        return sword;
    }

    private void setFullDiamondArmor(EntityEquipment eq) {
        eq.setHelmet(enchantedArmor(Material.DIAMOND_HELMET));
        eq.setChestplate(enchantedArmor(Material.DIAMOND_CHESTPLATE));
        eq.setLeggings(enchantedArmor(Material.DIAMOND_LEGGINGS));
        eq.setBoots(enchantedArmor(Material.DIAMOND_BOOTS));
        eq.setItemInMainHandDropChance(0f);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
    }

    private void setFullNetheriteArmor(EntityEquipment eq) {
        eq.setHelmet(enchantedArmor(Material.NETHERITE_HELMET));
        eq.setChestplate(enchantedArmor(Material.NETHERITE_CHESTPLATE));
        eq.setLeggings(enchantedArmor(Material.NETHERITE_LEGGINGS));
        eq.setBoots(enchantedArmor(Material.NETHERITE_BOOTS));
        eq.setItemInMainHandDropChance(0f);
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
    }

    private ItemStack enchantedArmor(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Calcule la localisation médiane entre tous les joueurs en ligne.
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

        int highestY = world.getHighestBlockYAt((int) medX, (int) medZ);
        return new Location(world, medX, highestY + 1, medZ);
    }

    /**
     * Vérifie si une entité est un boss.
     */
    public boolean isBoss(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING);
    }

    /**
     * Gère la mort du boss : drops et annonce.
     */
    public void handleBossDeath(LivingEntity boss, Player killer) {
        cancelPowerTask();
        cancelHealthDisplayTask();

        Location loc = boss.getLocation();
        String bossTypeStr = boss.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);

        // Drops selon le type de boss
        dropRewards(loc, bossTypeStr);

        currentBoss     = null;
        currentBossType = null;
        activeMinions.clear();
        currentBossBaseName  = null;
        currentBossNameColor = null;

        String killerName = (killer != null) ? killer.getName() : "un inconnu";
        String bossDisplayName = getBossDisplayNameFromKey(bossTypeStr);

        Component deathMsg = Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED)
                .appendNewline()
                .append(Component.text("  ✔ LE BOSS A ÉTÉ ÉLIMINÉ !", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Boss : ", NamedTextColor.GRAY))
                .append(Component.text(bossDisplayName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Tué par : ", NamedTextColor.GRAY))
                .append(Component.text(killerName, NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  Les récompenses ont été droppées sur place !", NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED));

        Bukkit.broadcast(deathMsg);
    }

    private void dropRewards(Location loc, String key) {
        World world = loc.getWorld();
        boolean dropWeapon = Math.random() < 0.5;

        switch (key != null ? key : "") {
            case "zombie" -> {
                if (dropWeapon) world.dropItemNaturally(loc, namedEnchantedSword(Material.DIAMOND_SWORD,
                        "✦ Lame du Mort-Vivant ✦", NamedTextColor.RED, 4));
                else world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 5 + (int)(Math.random() * 6)));
            }
            case "skeleton" -> {
                if (dropWeapon) {
                    ItemStack bow = new ItemStack(Material.BOW);
                    ItemMeta m = bow.getItemMeta();
                    m.addEnchant(Enchantment.POWER, 3, true);
                    m.addEnchant(Enchantment.PUNCH, 2, true);
                    m.displayName(Component.text("✦ Arc du Chasseur ✦", NamedTextColor.AQUA, TextDecoration.BOLD));
                    bow.setItemMeta(m);
                    world.dropItemNaturally(loc, bow);
                } else world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 5 + (int)(Math.random() * 6)));
            }
            case "wither_skeleton" -> {
                // Tête de wither skeleton garantie + récompense bonus
                world.dropItemNaturally(loc, new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
                if (dropWeapon) world.dropItemNaturally(loc, namedEnchantedSword(Material.NETHERITE_SWORD,
                        "✦ Épée du Néant ✦", NamedTextColor.DARK_GRAY, 5));
                else world.dropItemNaturally(loc, new ItemStack(Material.NETHERITE_INGOT, 2 + (int)(Math.random() * 3)));
            }
            case "spider" -> {
                // Toile d'araignée + oeil de spider
                world.dropItemNaturally(loc, new ItemStack(Material.COBWEB, 3));
                world.dropItemNaturally(loc, new ItemStack(Material.SPIDER_EYE, 5));
                if (dropWeapon) {
                    ItemStack trident = new ItemStack(Material.TRIDENT);
                    ItemMeta m = trident.getItemMeta();
                    m.addEnchant(Enchantment.SHARPNESS, 4, true);
                    m.addEnchant(Enchantment.LOYALTY, 3, true);
                    m.displayName(Component.text("✦ Crochet Venimeux ✦", NamedTextColor.DARK_GREEN, TextDecoration.BOLD));
                    trident.setItemMeta(m);
                    world.dropItemNaturally(loc, trident);
                } else world.dropItemNaturally(loc, new ItemStack(Material.EMERALD, 8 + (int)(Math.random() * 5)));
            }
            case "blaze" -> {
                // Baguette de Blaze + récompense
                world.dropItemNaturally(loc, new ItemStack(Material.BLAZE_ROD, 3 + (int)(Math.random() * 3)));
                if (dropWeapon) {
                    ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                    ItemMeta m = sword.getItemMeta();
                    m.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
                    m.addEnchant(Enchantment.SHARPNESS, 4, true);
                    m.displayName(Component.text("✦ Lame Infernale ✦", NamedTextColor.GOLD, TextDecoration.BOLD));
                    sword.setItemMeta(m);
                    world.dropItemNaturally(loc, sword);
                } else world.dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, 10 + (int)(Math.random() * 6)));
            }
            case "witch" -> {
                // Potions et récompenses magiques
                world.dropItemNaturally(loc, new ItemStack(Material.GLOWSTONE_DUST, 5));
                world.dropItemNaturally(loc, new ItemStack(Material.REDSTONE, 5));
                if (dropWeapon) {
                    ItemStack staff = new ItemStack(Material.BLAZE_ROD);
                    ItemMeta m = staff.getItemMeta();
                    m.displayName(Component.text("✦ Bâton de la Sorcière ✦", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
                    staff.setItemMeta(m);
                    world.dropItemNaturally(loc, staff);
                    world.dropItemNaturally(loc, new ItemStack(Material.EXPERIENCE_BOTTLE, 5));
                } else world.dropItemNaturally(loc, new ItemStack(Material.AMETHYST_SHARD, 8 + (int)(Math.random() * 5)));
            }
            default -> world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 5));
        }
    }

    private ItemStack namedEnchantedSword(Material mat, String name, NamedTextColor color, int sharpness) {
        ItemStack sword = new ItemStack(mat);
        ItemMeta m = sword.getItemMeta();
        m.addEnchant(Enchantment.SHARPNESS, sharpness, true);
        m.addEnchant(Enchantment.MENDING, 1, true);
        m.displayName(Component.text(name, color, TextDecoration.BOLD));
        sword.setItemMeta(m);
        return sword;
    }

    // ─── Helpers d'affichage ─────────────────────────────────────────────────

    public String getBossDisplayName(BossType type) {
        return switch (type) {
            case ZOMBIE          -> "Zombie Boss";
            case SKELETON        -> "Squelette Boss";
            case WITHER_SKELETON -> "Wither Boss";
            case SPIDER          -> "Araignée Boss";
            case BLAZE           -> "Blaze Boss";
            case WITCH           -> "Sorcière Boss";
        };
    }

    public NamedTextColor getBossColor(BossType type) {
        return switch (type) {
            case ZOMBIE          -> NamedTextColor.RED;
            case SKELETON        -> NamedTextColor.AQUA;
            case WITHER_SKELETON -> NamedTextColor.DARK_GRAY;
            case SPIDER          -> NamedTextColor.DARK_GREEN;
            case BLAZE           -> NamedTextColor.GOLD;
            case WITCH           -> NamedTextColor.DARK_PURPLE;
        };
    }

    public String getBossPowerDescription(BossType type) {
        return switch (type) {
            case ZOMBIE          -> "Invoque des zombies serviteurs";
            case SKELETON        -> "Convoque des archers + éclairs";
            case WITHER_SKELETON -> "Répand le Wither + Lenteur";
            case SPIDER          -> "Envoie des araignées + Poison + Cécité";
            case BLAZE           -> "Flammes + éclairs + blazes";
            case WITCH           -> "Potions maléfiques + téléportation";
        };
    }

    private String getBossDisplayNameFromKey(String key) {
        if (key == null) return "Boss Inconnu";
        return switch (key) {
            case "zombie"          -> "Zombie Boss";
            case "skeleton"        -> "Squelette Boss";
            case "wither_skeleton" -> "Wither Boss";
            case "spider"          -> "Araignée Boss";
            case "blaze"           -> "Blaze Boss";
            case "witch"           -> "Sorcière Boss";
            default -> "Boss";
        };
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public LivingEntity getCurrentBoss()   { return currentBoss; }
    public BossType getCurrentBossType()   { return currentBossType; }
    public boolean isBossAlive()           { return currentBoss != null && !currentBoss.isDead(); }
}
