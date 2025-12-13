import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Cod;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Salmon;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Fish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.lang3.tuple.Pair; // или ваш Pair
import ru.deelter.vr.freshFishing.data.FishRarity;
import ru.deelter.vr.freshFishing.utils.ScaleUtils;

public class FishUtil {

	private static final String TAG_SIZE = "size";
	private static final String TAG_RARITY = "rarity";
	private static final double MIN_SCALE = 0.5;
	private static final double MAX_SCALE = 15;

	// Замените на ваш способ получения экземпляра плагина
	private static final NamespacedKey KEY_SIZE = new NamespacedKey(YourPlugin.getInstance(), TAG_SIZE);
	private static final NamespacedKey KEY_RARITY = new NamespacedKey(YourPlugin.getInstance(), TAG_RARITY);

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
		return getFishByMaterial(material) != null;
	}

	public static boolean isFish(@NotNull ItemStack item) {
		return isFish(item.getType());
	}

	public static boolean hasSize(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		return pdc.has(KEY_SIZE, PersistentDataType.DOUBLE);
	}

	public static double getSize(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return 0.0;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		Double d = pdc.get(KEY_SIZE, PersistentDataType.DOUBLE);
		return d == null ? 0.0 : d;
	}

	public static boolean hasFishType(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		return pdc.has(KEY_RARITY, PersistentDataType.STRING);
	}

	public static FishRarity getRarity(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return FishRarity.DEFAULT; // замените на ваш дефолт
		PersistentDataContainer pdc = meta.getPersistentDataContainer();
		String s = pdc.get(KEY_RARITY, PersistentDataType.STRING);
		return FishRarity.getById(s);
	}

	public static double getRandomSize() {
		return FISH_SIZES.get().getRandomRoundedSize();
	}

	public static FishRarity getRandomType() {
		return FISH_TYPES.get();
	}

	@Contract("_, _, _ -> param1")
	public static @NotNull ItemStack editFishItem(@NotNull ItemStack fishItem, @NotNull FishRarity fishRarity, double size) {
		fishItem.editMeta(meta -> {
			PersistentDataContainer nbt = meta.getPersistentDataContainer();
			nbt.set(KEY_RARITY, PersistentDataType.STRING, fishRarity.toString());
			nbt.set(KEY_SIZE, PersistentDataType.DOUBLE, size);
		});
		return fishItem;
	}

	public static @Nullable Pair<FishRarity, Double> getAttributes(@NotNull ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) return null;
		return getAttributes(meta.getPersistentDataContainer());
	}

	public static @Nullable Pair<FishRarity, Double> getAttributes(@NotNull PersistentDataContainer container) {
		if (!container.has(KEY_SIZE, PersistentDataType.DOUBLE)) return null;
		String rarityId = container.get(KEY_RARITY, PersistentDataType.DOUBLE);
		Double size = container.get(KEY_SIZE, PersistentDataType.DOUBLE);

		return Pair.of(FishRarity.getById(rarityId), size);
	}

	public static void editFish(Fish fish, double size, @NotNull FishRarity rarity) {
		PersistentDataContainer pdc = fish.getPersistentDataContainer();
		pdc.set(KEY_RARITY, PersistentDataType.STRING, rarity.toString());
		pdc.set(KEY_SIZE, PersistentDataType.DOUBLE, size);

		double scale = (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (size - TIER_1.getMin()) / (TIER_5.getMax() - TIER_1.getMin())) * rarity.getMultiplier();
		ScaleUtils.setScale(fish, scale);
	}

	public static double getSize(Fish fish) {
		PersistentDataContainer pdc = fish.getPersistentDataContainer();
		Double d = pdc.get(KEY_SIZE, PersistentDataType.DOUBLE);
		return d == null ? 0.0 : d;
	}

	public static FishRarity getRarity(Fish fish) {
		PersistentDataContainer pdc = fish.getPersistentDataContainer();
		String s = pdc.get(KEY_RARITY, PersistentDataType.STRING);
		return FishRarity.getById(s);
	}
}
