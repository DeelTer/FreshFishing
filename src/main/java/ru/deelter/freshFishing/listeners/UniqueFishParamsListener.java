package ru.deelter.freshFishing.listeners;

import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.entity.Bucketable;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

import java.util.Objects;

public class UniqueFishParamsListener implements Listener {

	private final ProbabilityCollection<FishSize> sizes = new ProbabilityCollection<>();
	private final ProbabilityCollection<FishRarity> rarities = new ProbabilityCollection<>();

	public UniqueFishParamsListener(@NotNull FreshFishing plugin) {
		FileConfiguration config = plugin.getConfig();

		ConfigurationSection sizesSection = Objects.requireNonNull(config.getConfigurationSection("sizes"));
		ConfigurationSection raritiesSection = Objects.requireNonNull(config.getConfigurationSection("rarities"));

		sizesSection.getKeys(false).forEach(sizeId -> {
			ConfigurationSection sizeSection = Objects.requireNonNull(sizesSection.getConfigurationSection(sizeId));
			FishSize fishSize = FishSize.builder()
					.min(sizeSection.getDouble("min"))
					.max(sizeSection.getDouble("max"))
					.lambda(sizeSection.getInt("lambda"))
					.blocks(sizeSection.getInt("blocks"))
					.build();
			sizes.add(fishSize, fishSize.getBlocks());
			FishSize.SIZES.add(fishSize);
		});

		raritiesSection.getKeys(false).forEach(rarityId -> {
			ConfigurationSection raritySection = Objects.requireNonNull(raritiesSection.getConfigurationSection(rarityId));
			FishRarity fishRarity = FishRarity.builder()
					.id(rarityId)
					.name(MiniMessage.miniMessage().deserialize(Objects.requireNonNull(raritySection.getString("name"))))
					.multiplier(raritySection.getDouble("multiplier"))
					.blocks(raritySection.getInt("blocks"))
					.build();
			rarities.add(fishRarity, fishRarity.getBlocks());
			FishRarity.RARITIES.add(fishRarity);
		});

		FishUtil.ITEM_LORE.addAll(config.getStringList("item-lore"));
		plugin.getLogger().info(String.format("Registered %s rarities and %s sizes", rarities.size(), sizes.size()));

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishSpawn(@NotNull CreatureSpawnEvent event) {
		if (!(event.getEntity() instanceof Fish fish)) return;

		double size = sizes.get().getRandomRoundedSize();
		FishRarity rarity = rarities.get();

		FishUtil.editFish(fish, size, rarity);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDeath(@NotNull EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Fish fish)) return;

		double size = FishUtil.getSize(fish);
		FishRarity rarity = FishUtil.getRarity(fish);
		if (rarity == null) {
			rarity = rarities.get();
		}
		for (ItemStack drop : event.getDrops()) {
			if (!FishUtil.isFish(drop)) continue;

			FishUtil.editFishItem(drop, rarity, size);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onSpawnFishItem(@NotNull ItemSpawnEvent event) {
		Item item = event.getEntity();
		ItemStack itemStack = item.getItemStack();
		if (!FishUtil.isFish(itemStack)) return;
		if (FishUtil.hasSize(itemStack)) return;

		double size = sizes.get().getRandomRoundedSize();
		FishRarity rarity = rarities.get();

		ItemStack fishItem = FishUtil.editFishItem(itemStack, rarity, size);
		item.setItemStack(fishItem);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBucketFish(@NotNull PlayerBucketEntityEvent event) {
		if (!(event.getEntity() instanceof Fish fish)) return;

		var size = FishUtil.getSize(fish);
		var rarity = FishUtil.getRarity(fish);
		if (rarity == null) {
			rarity = rarities.get();
		}

		FishUtil.editFishItem(event.getEntityBucket(), rarity, size);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBucketFishEmpty(@NotNull PlayerBucketEmptyEvent event) {
		PlayerInventory inventory = event.getPlayer().getInventory();
		ItemStack item = getBucketItem(inventory);
		if (item == null) return;

		Pair<FishRarity, Double> params = FishUtil.getAttributes(item);
		if (params == null) return;

		Block block = event.getBlockClicked();
		Bukkit.getScheduler().runTaskLater(FreshFishing.getInstance(), () -> {
			Fish fish = block.getLocation().add(0, 1, 0).getNearbyEntitiesByType(Fish.class, 1.5)
					.stream()
					.filter(Bucketable::isFromBucket)
					.findAny()
					.orElse(null);
			if (fish == null) return;

			FishUtil.editFish(fish, params.getRight(), params.getLeft());
		}, 1);

	}

	private @Nullable ItemStack getBucketItem(@NotNull PlayerInventory inventory) {
		ItemStack item = inventory.getItemInMainHand();
		if (MaterialTags.FISH_BUCKETS.isTagged(item)) return item;

		item = inventory.getItemInOffHand();
		if (MaterialTags.FISH_BUCKETS.isTagged(item)) {
			return item;
		}
		return null;
	}
}
