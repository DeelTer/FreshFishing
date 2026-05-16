package ru.deelter.freshFishing;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;

public class FishMetrics {

	private final FreshFishing plugin;
	private final Metrics metrics;

	public FishMetrics(@NotNull FreshFishing plugin, int metricsId) {
		this.plugin = plugin;
		this.metrics = new Metrics(plugin, metricsId);
		setupCustomCharts();
	}

	private void setupCustomCharts() {
		FileConfiguration config = plugin.getConfig();

		// === ОСНОВНЫЕ НАСТРОЙКИ ===

		// Biome restriction enabled?
		metrics.addCustomChart(new SimplePie("biome_restriction_enabled", () -> {
			boolean hasBiomes = !config.getStringList("fish-only-in-biomes.list").isEmpty();
			return hasBiomes ? "enabled" : "disabled";
		}));

		// Hunger system enabled?
		metrics.addCustomChart(new SimplePie("hunger_enabled", () -> {
			int fishCost = config.getInt("fish", 1);
			int itemCost = config.getInt("item", 0);
			return (fishCost > 0 || itemCost > 0) ? "enabled" : "disabled";
		}));

		// === РЫБНАЯ ЛОВЛЯ ===

		// Number of rarities
		metrics.addCustomChart(new SimplePie("rarity_count", () -> {
			int size = FishRarity.RARITIES.size();
			return String.valueOf(size);
		}));

		// Number of size tiers
		metrics.addCustomChart(new SimplePie("size_tiers_count", () -> {
			int size = FishSize.SIZES.size();
			return String.valueOf(size);
		}));

		// Entity catch enabled?
		metrics.addCustomChart(new SimplePie("entity_catch_enabled", () -> {
			boolean hasEntities = config.getConfigurationSection("entities") != null &&
					!config.getConfigurationSection("entities").getKeys(false).isEmpty();
			return hasEntities ? "enabled" : "disabled";
		}));

		// Item lore enabled?
		metrics.addCustomChart(new SimplePie("item_lore_enabled", () -> {
			boolean hasLore = !config.getStringList("item-lore").isEmpty();
			return hasLore ? "enabled" : "disabled";
		}));

		// === НОВЫЕ МЕТРИКИ ===

		// Sale shop enabled?
		metrics.addCustomChart(new SimplePie("sale_shop_enabled", () -> {
			boolean enabled = config.getBoolean("sale-shop.enabled", true);
			return enabled ? "enabled" : "disabled";
		}));

		// Bait system enabled?
		metrics.addCustomChart(new SimplePie("bait_enabled", () -> {
			boolean hasBaits = !config.getStringList("bait.required-items").isEmpty();
			return hasBaits ? "enabled" : "disabled";
		}));

		// Bait consume on catch?
		metrics.addCustomChart(new SimplePie("bait_consume", () -> {
			boolean consume = config.getBoolean("bait.consume-on-catch", true);
			return consume ? "consume" : "keep";
		}));

		// Grappling hook enabled?
		metrics.addCustomChart(new SimplePie("grappling_hook_enabled", () -> {
			boolean enabled = config.getBoolean("grappling-hook.enabled", true);
			return enabled ? "enabled" : "disabled";
		}));

		// Sand loot enabled?
		metrics.addCustomChart(new SimplePie("sand_loot_enabled", () -> {
			boolean enabled = config.getBoolean("sand-loot.enabled", true);
			return enabled ? "enabled" : "disabled";
		}));

		// Sand loot mode (simple/complex)
		metrics.addCustomChart(new SimplePie("sand_loot_mode", () -> {
			String mode = config.getString("sand-loot.mode", "simple");
			return mode;
		}));

		// Sand loot items count
		metrics.addCustomChart(new SimplePie("sand_loot_items_count", () -> {
			int count = config.getList("sand-loot.loot-tables", new java.util.ArrayList<>()).size();
			return count <= 50 ? String.valueOf(count) : "50+";
		}));

		// Performance debug enabled?
		metrics.addCustomChart(new SimplePie("debug_enabled", () -> {
			boolean debug = config.getBoolean("performance.debug", false);
			return debug ? "enabled" : "disabled";
		}));

		// Metrics enabled? (мета-метрика)
		metrics.addCustomChart(new SimplePie("metrics_enabled", () -> {
			boolean enabled = config.getBoolean("metrics.enabled", true);
			return enabled ? "enabled" : "disabled";
		}));
	}
}