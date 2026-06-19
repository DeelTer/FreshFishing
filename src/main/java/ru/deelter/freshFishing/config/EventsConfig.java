package ru.deelter.freshFishing.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.deelter.freshFishing.FreshFishing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class EventsConfig {

    private final FreshFishing plugin;

    private boolean bossFishEnabled;
    private double bossFishChance;
    private double bossFishHealthMin;
    private double bossFishHealthMax;
    private int lungeIntervalMin;
    private int lungeIntervalMax;
    private double steerStrength;
    private double jumpDamage;
    private double jumpDamageRadius;
    private int impactFallingBlockCount;
    private double naturalJumpDamage;
    private double naturalJumpDamageRadius;
    private double bossbarRadius;
    private BarColor bossbarColor;
    private BarStyle bossbarStyle;
    private int despawnTicks;
    private List<String> bossPrefixes;
    private List<String> bossSuffixes;

    private List<String> rewardRarities;
    private List<String> rewardSizeTiers;
    private int bonusXpLevels;
    private int secondaryLootMin;
    private int secondaryLootMax;
    private List<Material> secondaryLootMaterials;

    private boolean monsterBoostEnabled;
    private double monsterHealthMultiplier;

    public EventsConfig(FreshFishing plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("events.yml", false);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "events.yml"));

        bossFishEnabled = cfg.getBoolean("boss-fish.enabled", true);
        bossFishChance = cfg.getDouble("boss-fish.chance", 0.05);
        bossFishHealthMin = cfg.getDouble("boss-fish.health-min", 70.0);
        bossFishHealthMax = cfg.getDouble("boss-fish.health-max", 120.0);
        lungeIntervalMin = cfg.getInt("boss-fish.lunge-interval-min-ticks", 60);
        lungeIntervalMax = cfg.getInt("boss-fish.lunge-interval-max-ticks", 80);
        steerStrength = cfg.getDouble("boss-fish.steer-strength", 0.55);
        jumpDamage = cfg.getDouble("boss-fish.jump-damage", 3.0);
        jumpDamageRadius = cfg.getDouble("boss-fish.jump-damage-radius", 2.5);
        impactFallingBlockCount = cfg.getInt("boss-fish.impact-falling-block-count", 8);
        naturalJumpDamage = cfg.getDouble("boss-fish.natural-jump-damage", 2.0);
        naturalJumpDamageRadius = cfg.getDouble("boss-fish.natural-jump-damage-radius", 3.0);
        bossbarRadius = cfg.getDouble("boss-fish.bossbar-radius", 30.0);
        despawnTicks = cfg.getInt("boss-fish.despawn-ticks", 1200);
        bossPrefixes = cfg.getStringList("boss-fish.prefixes");
        bossSuffixes = cfg.getStringList("boss-fish.suffixes");

        bossbarColor = parseEnum(cfg.getString("boss-fish.bossbar-color", "RED"), BarColor.class, BarColor.RED);
        bossbarStyle = parseEnum(cfg.getString("boss-fish.bossbar-style", "SOLID"), BarStyle.class, BarStyle.SOLID);

        rewardRarities = cfg.getStringList("boss-fish.reward.drop-rarities");
        rewardSizeTiers = cfg.getStringList("boss-fish.reward.drop-size-tiers");
        bonusXpLevels = cfg.getInt("boss-fish.reward.bonus-xp-levels", 5);
        secondaryLootMin = cfg.getInt("boss-fish.reward.secondary-loot.count-min", 1);
        secondaryLootMax = cfg.getInt("boss-fish.reward.secondary-loot.count-max", 3);
        secondaryLootMaterials = new ArrayList<>();
        for (String name : cfg.getStringList("boss-fish.reward.secondary-loot.items")) {
            try {
                secondaryLootMaterials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        monsterBoostEnabled = cfg.getBoolean("monster-boost.enabled", true);
        monsterHealthMultiplier = cfg.getDouble("monster-boost.health-multiplier", 3.0);
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
