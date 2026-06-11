package ru.deelter.freshFishing.utils;

import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;

import java.util.ArrayList;
import java.util.List;

public class FishUtil {

	public static final List<String> CONFIG_ITEM_LORE = new ArrayList<>();
	private static final String TAG_SIZE = "size";
	private static final String TAG_RARITY = "rarity";
	private static final double MIN_SCALE = 0.5;
	private static final double MAX_SCALE = 15;
	private static final NamespacedKey KEY_SIZE = new NamespacedKey(FreshFishing.getInstance(), TAG_SIZE);
	private static final NamespacedKey KEY_RARITY = new NamespacedKey(FreshFishing.getInstance(), TAG_RARITY);

	@Nullable
	public static Material getMaterialByFish(@NotNull Fish fish) {
		return switch (fish.getType()) {
			case SALMON -> Material.SALMON;
			case TROPICAL_FISH -> Material.TROPICAL_FISH;
			case PUFFERFISH -> Material.PUFFERFISH;
			case COD -> Material.COD;
			default -> null;
		};
	}

	@Nullable
	public static Class<? extends Entity> getFishByMaterial(@NotNull Material material) {
		return switch (material) {
			case SALMON -> Salmon.class;
			case TROPICAL_FISH -> TropicalFish.class;
			case PUFFERFISH -> PufferFish.class;
			case COD -> Cod.class;
			default -> null;
		};
	}

	public static boolean isFish(@NotNull Material material) {
		return MaterialTags.RAW_FISH.isTagged(material) || MaterialTags.COOKED_FISH.isTagged(material);
	}

	public static boolean isFish(@NotNull ItemStack item) {
		return isFish(item.getType());
	}

	public static boolean hasSize(@NotNull ItemStack item) {
		return item.getPersistentDataContainer().has(KEY_SIZE, PersistentDataType.DOUBLE);
	}

	public static double getSize(@NotNull ItemStack item) {
		Double size = item.getPersistentDataContainer().get(KEY_SIZE, PersistentDataType.DOUBLE);
		return size == null ? 0.0 : size;
	}

	public static int recalculateMaxDamage(@NotNull ItemStack item) {
		double size = getSize(item);
		if (size <= 0.0) return 0;

		int maxDamage = Math.max(1, (int) (size / FreshFishing.getInstance().getConfigManager().getFishConsumeDamage()));
		item.setData(DataComponentTypes.MAX_DAMAGE, maxDamage);
		return maxDamage;
	}

	public static @NotNull ItemStack editFishItem(@NotNull ItemStack fishItem, @NotNull FishRarity rarity, double size) {

		fishItem.setAmount(1);
		fishItem.setData(DataComponentTypes.MAX_STACK_SIZE, 1);

		fishItem.editPersistentDataContainer(persistentDataContainer -> {
			persistentDataContainer.set(KEY_RARITY, PersistentDataType.STRING, rarity.getId());
			persistentDataContainer.set(KEY_SIZE, PersistentDataType.DOUBLE, size);
		});

		List<Component> loreLines = new ArrayList<>();
		for (String line : CONFIG_ITEM_LORE) {
			Component loreComponent = MiniMessage.miniMessage().deserialize(
							line,
							Placeholder.component("size", Component.text(size)),
							Placeholder.component("rarity", rarity.getName()))
					.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
			loreLines.add(loreComponent);
		}
		fishItem.setData(DataComponentTypes.LORE, ItemLore.lore().addLines(loreLines).build());

		int maxDamage = recalculateMaxDamage(fishItem);
		if (maxDamage >= 0) {
			fishItem.setData(DataComponentTypes.DAMAGE, 0);
		}
		return fishItem;
	}

	public static @Nullable Pair<FishRarity, Double> getAttributes(@NotNull ItemStack item) {
		return getAttributes(item.getPersistentDataContainer());
	}

	public static @Nullable Pair<FishRarity, Double> getAttributes(@NotNull PersistentDataContainerView container) {
		if (!container.has(KEY_SIZE, PersistentDataType.DOUBLE)) {
			return null;
		}
		String rarityId = container.get(KEY_RARITY, PersistentDataType.STRING);
		Double size = container.get(KEY_SIZE, PersistentDataType.DOUBLE);

		return Pair.of(FishRarity.getById(rarityId), size);
	}

	public static void editFish(@NotNull Fish fish, double size, @NotNull FishRarity rarity) {
		PersistentDataContainer nbt = fish.getPersistentDataContainer();
		nbt.set(KEY_RARITY, PersistentDataType.STRING, rarity.getId());
		nbt.set(KEY_SIZE, PersistentDataType.DOUBLE, size);

		double tierMin = FishSize.SIZES.stream()
				.mapToDouble(FishSize::getMin)
				.min()
				.orElseThrow(() -> new RuntimeException("No FishSize defined"));
		double tierMax = FishSize.SIZES.stream()
				.mapToDouble(FishSize::getMax)
				.max()
				.orElseThrow(() -> new RuntimeException("No FishSize defined"));

		double scale = (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (size - tierMin) / (tierMax - tierMin)) * rarity.getMultiplier();
		ScaleUtils.setScale(fish, scale);
	}

	public static double getSize(@NotNull Fish fish) {
		PersistentDataContainer nbt = fish.getPersistentDataContainer();
		Double size = nbt.get(KEY_SIZE, PersistentDataType.DOUBLE);
		return size == null ? 0.0 : size;
	}

	public static @Nullable FishRarity getRarity(@NotNull Fish fish) {
		PersistentDataContainer nbt = fish.getPersistentDataContainer();
		if (!nbt.has(KEY_RARITY)) {
			return null;
		}
		String rarityId = nbt.get(KEY_RARITY, PersistentDataType.STRING);
		return FishRarity.getById(rarityId);
	}
}
