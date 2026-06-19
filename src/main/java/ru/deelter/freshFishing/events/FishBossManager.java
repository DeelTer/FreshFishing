package ru.deelter.freshFishing.events;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BossBar;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityJumpEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.EventsConfig;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.utils.FishUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class FishBossManager implements Listener {

    private static final Random RANDOM = new Random();
    // Approximate Minecraft gravity per tick (blocks/tick²)
    private static final double GRAVITY = 0.08;

    private final FreshFishing plugin;
    private final EventsConfig config;
    private final Map<UUID, BossData> activeBosses = new HashMap<>();

    private static class BossData {
        final LivingEntity entity;
        final BossBar bar;
        final BukkitTask despawnTask;
        final FishRarity dropRarity;
        final double dropSize;
        BukkitTask lungeTask;

        BossData(LivingEntity entity, BossBar bar, BukkitTask despawnTask, FishRarity dropRarity, double dropSize) {
            this.entity = entity;
            this.bar = bar;
            this.despawnTask = despawnTask;
            this.dropRarity = dropRarity;
            this.dropSize = dropSize;
        }
    }

    public FishBossManager(@NonNull FreshFishing plugin) {
        this.plugin = plugin;
        this.config = plugin.getEventsConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void trySpawnBoss(@NonNull LivingEntity entity, @NonNull Player catcher) {
        if (!config.isBossFishEnabled()) return;
        if (!(entity instanceof Fish)) return;
        if (RANDOM.nextDouble() > config.getBossFishChance()) return;

        // Pick tier/rarity for scale + drop (same object used for both)
        FishRarity rarity = pickRandom(config.getRewardRarities().stream()
                .map(FishRarity::getById).filter(Objects::nonNull).collect(Collectors.toList()));
        FishSize tier = pickRandom(FishSize.SIZES.stream()
                .filter(s -> config.getRewardSizeTiers().contains(s.getId())).collect(Collectors.toList()));
        if (rarity == null || tier == null) return;

        double size = tier.getRandomRoundedSize();

        // Apply scale via editFish (sets visual size attribute), then override HP
        FishUtil.editFish((Fish) entity, size, rarity);
        double hp = config.getBossFishHealthMin()
                + RANDOM.nextDouble() * (config.getBossFishHealthMax() - config.getBossFishHealthMin());
        setMaxHealth(entity, hp);

        BossBar bar = Bukkit.createBossBar(randomName(), config.getBossbarColor(), config.getBossbarStyle());
        bar.setProgress(1.0);
        bar.addPlayer(catcher);

        BukkitTask despawnTask = Bukkit.getScheduler().runTaskLater(plugin,
                () -> removeBoss(entity.getUniqueId()), config.getDespawnTicks());

        BossData data = new BossData(entity, bar, despawnTask, rarity, size);
        activeBosses.put(entity.getUniqueId(), data);
        scheduleLunge(entity.getUniqueId());
    }

    private void scheduleLunge(@NonNull UUID id) {
        int range = config.getLungeIntervalMax() - config.getLungeIntervalMin();
        int delay = config.getLungeIntervalMin() + (range > 0 ? RANDOM.nextInt(range + 1) : 0);
        BossData data = activeBosses.get(id);
        if (data == null) return;
        data.lungeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tickBoss(id);
            scheduleLunge(id); // schedule next with new random delay
        }, delay);
    }

    private void tickBoss(@NonNull UUID id) {
        BossData data = activeBosses.get(id);
        if (data == null) return;

        LivingEntity entity = data.entity;
        if (entity.isDead() || !entity.isValid()) {
            removeBoss(id);
            return;
        }

        // Update bossbar
        double maxHp = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        data.bar.setProgress(Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHp)));
        updateBossBarViewers(data);

        // Lunge toward nearest player with ballistic arc
        Player target = getNearestPlayer(entity, config.getBossbarRadius());
        if (target == null) return;

        Location from = entity.getLocation();
        Location to = target.getLocation();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist < 0.5) return;

        double hSpeed = config.getSteerStrength();
        // Normalize horizontal component
        double vx = dx / hDist * hSpeed;
        double vz = dz / hDist * hSpeed;

        // Calculate vertical velocity for arc landing near target
        double t = hDist / hSpeed; // ticks to reach horizontal target
        double dy = to.getY() - from.getY();
        double vy = (dy + 0.5 * GRAVITY * t * t) / t;
        vy = Math.max(0.25, Math.min(vy, 1.8)); // reasonable clamp

        entity.setVelocity(new Vector(vx, vy, vz));
        entity.setFallDistance(0f); // reset so fall damage triggers on landing
    }

    // Detect landing via fall damage — use as impact trigger
    @EventHandler(priority = EventPriority.HIGH)
    public void onBossFallDamage(@NonNull EntityDamageEvent event) {
        if (!activeBosses.containsKey(event.getEntity().getUniqueId())) return;
        if (event.getDamageSource().getDamageType() != DamageType.FALL) return;

        event.setCancelled(true); // boss doesn't take fall damage, only triggers effects
        handleImpact((LivingEntity) event.getEntity());
    }

    private void handleImpact(@NonNull LivingEntity entity) {
        Location loc = entity.getLocation();

        dealImpactDamage(entity, loc);
        spawnImpactParticles(loc);
        spawnFallingBlocks(loc);
    }

    private void dealImpactDamage(@NonNull LivingEntity entity, @NonNull Location loc) {
        double r = config.getJumpDamageRadius();
        for (Entity nearby : entity.getNearbyEntities(r, r, r)) {
            if (nearby instanceof Player player) {
                player.damage(config.getJumpDamage(), entity);
            }
        }
    }

    private void spawnImpactParticles(@NonNull Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        // White smoke cloud — "heavy landing"
        world.spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.1, 0.5, 0.05);
        world.spawnParticle(Particle.WHITE_SMOKE, loc, 20, 0.4, 0.2, 0.4, 0.02);
        // Visual explosion (no griefing)
        world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.3, 0.1, 0.3, 0);
    }

    private void spawnFallingBlocks(@NonNull Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        // Collect solid blocks around impact point to use as debris material
        List<BlockData> materials = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block block = loc.clone().add(dx, -1, dz).getBlock();
                if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                    materials.add(block.getBlockData());
                }
            }
        }
        if (materials.isEmpty()) materials.add(Material.GRAVEL.createBlockData());

        int count = config.getImpactFallingBlockCount();
        for (int i = 0; i < count; i++) {
            BlockData bd = materials.get(RANDOM.nextInt(materials.size()));
            FallingBlock fb = world.spawnFallingBlock(loc.clone().add(0, 0.3, 0), bd);
            fb.setDropItem(false);
            fb.setHurtEntities(false);

            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double speed = 0.15 + RANDOM.nextDouble() * 0.45;
            double upward = 0.25 + RANDOM.nextDouble() * 0.45;
            fb.setVelocity(new Vector(Math.cos(angle) * speed, upward, Math.sin(angle) * speed));

            // Remove before it converts to a placed block
            Bukkit.getScheduler().runTaskLater(plugin, fb::remove, 60L);
        }
    }

    private void updateBossBarViewers(@NonNull BossData data) {
        Location loc = data.entity.getLocation();
        List<Player> inRange = loc.getWorld().getNearbyPlayers(loc, config.getBossbarRadius());
        for (Player existing : data.bar.getPlayers()) {
            if (!inRange.contains(existing)) data.bar.removePlayer(existing);
        }
        for (Player p : inRange) {
            if (!data.bar.getPlayers().contains(p)) data.bar.addPlayer(p);
        }
    }

    @EventHandler
    public void onBossJump(@NonNull EntityJumpEvent event) {
        if (!activeBosses.containsKey(event.getEntity().getUniqueId())) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        double r = config.getNaturalJumpDamageRadius();
        for (Entity nearby : entity.getNearbyEntities(r, r, r)) {
            if (nearby instanceof Player player) {
                player.damage(config.getNaturalJumpDamage(), entity);
            }
        }
    }

    @EventHandler
    public void onBossDeath(@NonNull EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        if (!activeBosses.containsKey(id)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        Location loc = event.getEntity().getLocation();

        // Guaranteed high-tier fish drop (same tier/rarity as boss scale)
        BossData bossData = activeBosses.get(id);
        ItemStack fishDrop = buildBossFishDrop(event.getEntity(),
                bossData != null ? bossData.dropRarity : null,
                bossData != null ? bossData.dropSize : -1);
        if (fishDrop != null) {
            loc.getWorld().dropItemNaturally(loc, fishDrop);
        }

        // Secondary coral/ocean loot
        List<ItemStack> secondary = buildSecondaryLoot();
        for (ItemStack item : secondary) {
            loc.getWorld().dropItemNaturally(loc, item);
        }

        // XP to killer
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            killer.giveExpLevels(config.getBonusXpLevels());
        }

        removeBoss(id);
    }

    private ItemStack buildBossFishDrop(@NonNull LivingEntity entity, FishRarity rarity, double size) {
        if (rarity == null || size <= 0) return null;

        Material mat = switch (entity.getType()) {
            case SALMON -> Material.SALMON;
            case TROPICAL_FISH -> Material.TROPICAL_FISH;
            case PUFFERFISH -> Material.PUFFERFISH;
            default -> Material.COD;
        };
        return FishUtil.editFishItem(new ItemStack(mat), rarity, size);
    }

    private <T> T pickRandom(List<T> list) {
        return list.isEmpty() ? null : list.get(RANDOM.nextInt(list.size()));
    }

    private List<ItemStack> buildSecondaryLoot() {
        List<Material> pool = config.getSecondaryLootMaterials();
        if (pool.isEmpty()) return List.of();

        int min = config.getSecondaryLootMin();
        int max = config.getSecondaryLootMax();
        int count = min + (max > min ? RANDOM.nextInt(max - min + 1) : 0);

        List<ItemStack> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new ItemStack(pool.get(RANDOM.nextInt(pool.size()))));
        }
        return result;
    }

    public void removeBoss(@NonNull UUID id) {
        BossData data = activeBosses.remove(id);
        if (data == null) return;
        if (data.lungeTask != null) data.lungeTask.cancel();
        data.despawnTask.cancel();
        data.bar.removeAll();
    }

    public void shutdown() {
        activeBosses.forEach((id, data) -> {
            if (data.lungeTask != null) data.lungeTask.cancel();
            data.despawnTask.cancel();
            data.bar.removeAll();
        });
        activeBosses.clear();
    }

    private Player getNearestPlayer(@NonNull LivingEntity entity, double radius) {
        Location entityLoc = entity.getLocation();
        return entityLoc.getWorld()
                .getNearbyPlayers(entityLoc, radius)
                .stream()
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(entityLoc)))
                .orElse(null);
    }

    private String randomName() {
        List<String> prefixes = config.getBossPrefixes();
        List<String> suffixes = config.getBossSuffixes();
        String prefix = prefixes.isEmpty() ? "Big" : prefixes.get(RANDOM.nextInt(prefixes.size()));
        String suffix = suffixes.isEmpty() ? "One" : suffixes.get(RANDOM.nextInt(suffixes.size()));
        return prefix + " " + suffix;
    }

    public static void setMaxHealth(@NonNull LivingEntity entity, double healthValue) {
        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) return;
        if (attribute.getBaseValue() > healthValue) {
            healthValue = attribute.getBaseValue();
        }
        attribute.setBaseValue(healthValue);
        entity.setHealth(healthValue);
    }
}
