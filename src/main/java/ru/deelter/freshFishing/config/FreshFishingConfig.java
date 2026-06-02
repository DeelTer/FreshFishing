package ru.deelter.freshFishing.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ItemStackParser;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

import java.util.*;

@Getter
public class FreshFishingConfig {

    private final FreshFishing plugin;

    private final Set<Biome> allowedBiomes = new HashSet<>();
    private String biomeWarningMessage;

    private int minHungerLevel;
    private int hungerFishCost;
    private int hungerItemCost;

    private List<String> itemLore = new ArrayList<>();

    private final ProbabilityCollection<FishRarity> rarityCollection = new ProbabilityCollection<>();
    private final ProbabilityCollection<FishSize> sizeCollection = new ProbabilityCollection<>();

    private final ProbabilityCollection<EntityType> entityCollection = new ProbabilityCollection<>();

    private double vectorMultiply, vectorX, vectorY, vectorZ;

    private boolean saleShopEnabled;
    private String saleShopTitle;
    private int saleShopSize;
    private int saleButtonSlot;
    private @org.jetbrains.annotations.Nullable String saleSound;
    private String saleSoundName;
    private final Map<String, FishPriceRange> fishPriceRanges = new HashMap<>();
    private final Map<Material, Double> trashPrices = new HashMap<>();
    private final Map<Enchantment, Double> enchantmentPrices = new HashMap<>();

    private final List<ItemStack> requiredBaits = new ArrayList<>();
    private boolean consumeBaitOnCatch;
    private String noBaitActionBar;

    private boolean grapplingHookEnabled;
    private boolean grapplingHookWallGrapple;
    private int grapplingCooldownTicks;
    private boolean grapplingHookUsePhysics;
    private double grapplingHookUpwardBoost;
    private int grapplingHookPullDelayTicks;
    private double grapplingHookDragHorizontal;
    private double grapplingHookDragVertical;
    private double grapplingHookVelocityMultiplier;
    private double pullStrength;
    private double pullEntityStrength;
    private Sound hookSound;
    private Sound playerPullSound;
    private double yOffsetCheck;

    private boolean sandLootEnabled;
    private String sandLootMode;
    private final List<ItemStack> sandLootItems = new ArrayList<>();
    private long sandLootIntervalTicks;
    private int sandLootMinItemsPerSpawn;
    private int sandLootMaxItemsPerSpawn;
    private int maxBlocksPerMarker;

    private boolean debug;
    private int itemCacheDuration;
    private boolean metricsEnabled;

    public FreshFishingConfig(FreshFishing plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // === BIOME ===
        allowedBiomes.clear();
        List<String> biomeList = config.getStringList("fish-only-in-biomes.list");
        for (String biomeName : biomeList) {
            Biome biome = getBiomeByName(biomeName);
            if (biome != null) allowedBiomes.add(biome);
        }
        biomeWarningMessage = config.getString("fish-only-in-biomes.warning", "");

        // === HUNGER ===
        minHungerLevel = config.getInt("min-level-for-fishing", 3);
        hungerFishCost = config.getInt("fish", 1);
        hungerItemCost = config.getInt("item", 0);

        // === ITEM LORE ===
        itemLore = config.getStringList("item-lore");
        FishUtil.ITEM_LORE.clear();
        FishUtil.ITEM_LORE.addAll(itemLore);

        // === RARITIES ===
        rarityCollection.clear();
        FishRarity.RARITIES.clear();
        ConfigurationSection raritiesSec = config.getConfigurationSection("rarities");
        if (raritiesSec != null) {
            for (String id : raritiesSec.getKeys(false)) {
                String nameStr = raritiesSec.getString(id + ".name");
                int blocks = raritiesSec.getInt(id + ".blocks");
                double multiplier = raritiesSec.getDouble(id + ".multiplier");
                FishRarity rarity = FishRarity.builder()
                        .id(id)
                        .name(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(nameStr))
                        .blocks(blocks)
                        .multiplier(multiplier)
                        .build();
                rarityCollection.add(rarity, blocks);
                FishRarity.RARITIES.add(rarity);
            }
        }

        // === SIZES ===
        sizeCollection.clear();
        FishSize.SIZES.clear();
        ConfigurationSection sizesSec = config.getConfigurationSection("sizes");
        if (sizesSec != null) {
            for (String id : sizesSec.getKeys(false)) {
                double min = sizesSec.getDouble(id + ".min");
                double max = sizesSec.getDouble(id + ".max");
                int blocks = sizesSec.getInt(id + ".blocks");
                int lambda = sizesSec.getInt(id + ".lambda");
                FishSize size = FishSize.builder()
                        .min(min).max(max).blocks(blocks).lambda(lambda)
                        .build();
                sizeCollection.add(size, blocks);
                FishSize.SIZES.add(size);
            }
        }

        // === ENTITIES ===
        entityCollection.clear();
        ConfigurationSection entitiesSec = config.getConfigurationSection("entities");
        if (entitiesSec != null) {
            for (String key : entitiesSec.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    int weight = entitiesSec.getInt(key);
                    entityCollection.add(type, weight);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // === VECTOR ===
        vectorMultiply = config.getDouble("vector-caught-object.multiply", 1.4);
        vectorX = config.getDouble("vector-caught-object.x", 0);
        vectorY = config.getDouble("vector-caught-object.y", 0.4);
        vectorZ = config.getDouble("vector-caught-object.z", 0);

        // === SALE SHOP ===
        saleShopEnabled = config.getBoolean("sale-shop.enabled", true);
        saleShopTitle = config.getString("sale-shop.title", "Fish Market");
        saleShopSize = config.getInt("sale-shop.size", 27);
        saleButtonSlot = config.getInt("sale-shop.sale-button-slot", 22);
        saleSound = config.getString("sale-shop.sale-sound", Sound.BLOCK_NOTE_BLOCK_CHIME.toString());

        fishPriceRanges.clear();
        ConfigurationSection rangesSec = config.getConfigurationSection("sale-shop.fish-sale-ranges");
        if (rangesSec != null) {
            for (String rarityId : rangesSec.getKeys(false)) {
                double min = rangesSec.getDouble(rarityId + ".min-price");
                double max = rangesSec.getDouble(rarityId + ".max-price");
                fishPriceRanges.put(rarityId, new FishPriceRange(min, max));
            }
        }

        trashPrices.clear();
        ConfigurationSection trashSec = config.getConfigurationSection("sale-shop.trash-prices");
        if (trashSec != null) {
            for (String matName : trashSec.getKeys(false)) {
                try {
                    Material material = Material.valueOf(matName.toUpperCase());
                    trashPrices.put(material, trashSec.getDouble(matName));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        enchantmentPrices.clear();
        ConfigurationSection enchantSection = config.getConfigurationSection("sale-shop.enchantment-prices");
        if (enchantSection != null) {
            for (String enchKey : enchantSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                if (enchantment != null) {
                    enchantmentPrices.put(enchantment, enchantSection.getDouble(enchKey));
                }
            }
        }

        // === BAIT ===
        requiredBaits.clear();
        List<String> baitItems = config.getStringList("bait.required-items");
        for (String itemStr : baitItems) {
            ItemStack bait = parseBaitItem(itemStr);
            if (bait != null) requiredBaits.add(bait);
        }
        consumeBaitOnCatch = config.getBoolean("bait.consume-on-catch", true);
        noBaitActionBar = config.getString("bait.no-bait-action-bar", "");

        // === GRAPPLING HOOK ===
        grapplingHookEnabled = config.getBoolean("grappling-hook.enabled", true);
        grapplingHookWallGrapple = config.getBoolean("grappling-hook.wall-grapple", true);
        grapplingCooldownTicks = config.getInt("grappling-hook.cooldown-ticks", 40);
        grapplingHookUsePhysics = config.getBoolean("grappling-hook.use-physics", true);
        grapplingHookUpwardBoost = config.getDouble("grappling-hook.upward-boost", 0.3);
        grapplingHookPullDelayTicks = config.getInt("grappling-hook.pull-delay-ticks", 1);
        grapplingHookDragHorizontal = config.getDouble("grappling-hook.drag-horizontal", 0.07);
        grapplingHookDragVertical = config.getDouble("grappling-hook.drag-vertical", 0.03);
        grapplingHookVelocityMultiplier = config.getDouble("grappling-hook.velocity-multiplier", 1.0);
        pullStrength = config.getDouble("grappling-hook.pull-strength", 1.0);
        pullEntityStrength = config.getDouble("grappling-hook.pull-entity-strength", 1.0);
        hookSound = getSoundSafe(config.getString("grappling-hook.hook-sound", "entity.wind_charge.wind_burst"), Sound.ENTITY_WIND_CHARGE_WIND_BURST);
        playerPullSound = getSoundSafe(config.getString("grappling-hook.player-pull-sound", "entity.wind_charge.wind_burst"), Sound.ENTITY_WIND_CHARGE_WIND_BURST);
        yOffsetCheck = config.getDouble("grappling-hook.y-offset-check", 3.0);

        // === SAND LOOT ===
        sandLootEnabled = config.getBoolean("sand-loot.enabled", true);
        sandLootMode = config.getString("sand-loot.mode", "simple");
        sandLootIntervalTicks = config.getLong("sand-loot.spawn-interval-ticks", 6000);
        sandLootMinItemsPerSpawn = config.getInt("sand-loot.min-items-per-spawn", 2);
        sandLootMaxItemsPerSpawn = config.getInt("sand-loot.max-items-per-spawn", 5);
        sandLootItems.clear();
        List<?> lootList = config.getList("sand-loot.loot-tables", new ArrayList<>());
        for (Object obj : lootList) {
            if (obj instanceof String str) {
                ItemStack item = null;
                if ("complex".equalsIgnoreCase(sandLootMode)) {
                    item = ItemStackParser.parseItemString(str);
                } else {
                    try {
                        item = new ItemStack(Material.valueOf(str.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (item != null) sandLootItems.add(item);
            }
        }

        // === PERFORMANCE ===
        debug = config.getBoolean("performance.debug", false);
        maxBlocksPerMarker = config.getInt("performance.max-blocks-per-marker", 48);
        itemCacheDuration = config.getInt("performance.item-cache-duration", 300);

        // === METRICS ===
        metricsEnabled = config.getBoolean("metrics.enabled", true);
    }

    private @Nullable Biome getBiomeByName(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            Biome biome = Registry.BIOME.get(key);
            if (biome != null) return biome;
        } catch (Exception ignored) {
        }
        return null;
    }

    private ItemStack parseBaitItem(@NonNull String s) {
        try {
            Material material = Material.valueOf(s.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            return ItemStackParser.parseItemString(s);
        }
    }

    private Sound getSoundSafe(String soundName, Sound fallback) {
        if (soundName == null || soundName.isBlank()) return fallback;
        try {
            NamespacedKey key = NamespacedKey.minecraft(soundName.toLowerCase().replace("minecraft:", ""));
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) return sound;
        } catch (Exception ignored) {
        }
        return fallback;
    }

    public record FishPriceRange(double minPrice, double maxPrice) {
    }
}