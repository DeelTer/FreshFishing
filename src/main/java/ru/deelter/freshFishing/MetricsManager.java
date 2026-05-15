package ru.deelter.freshFishing;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;

public class MetricsManager {

	private final FreshFishing plugin;
	private final Metrics metrics;

	public MetricsManager(@NotNull FreshFishing plugin, int metricsId) {
		this.plugin = plugin;
		this.metrics = new Metrics(plugin, metricsId);
		setupCustomCharts();
	}

	private void setupCustomCharts() {
		FileConfiguration config = plugin.getConfig();

		// Biome restriction enabled?
		metrics.addCustomChart(new SimplePie("biome_restriction_enabled", () -> {
			boolean hasBiomes = !config.getStringList("fish-only-in-biomes.list").isEmpty();
			return hasBiomes ? "enabled" : "disabled";
		}));

		// Hunger system enabled?
		metrics.addCustomChart(new SimplePie("hunger_enabled", () -> {
			int fishCost = config.getInt("hunger.fish", 1);
			int itemCost = config.getInt("hunger.item", 0);
			return (fishCost > 0 || itemCost > 0) ? "enabled" : "disabled";
		}));

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
	}
}