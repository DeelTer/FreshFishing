package ru.deelter.freshFishing.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;

public class ItemStackParser {

	@Nullable
	public static ItemStack parseItemString(@NotNull String itemString) {
		if (itemString.isBlank()) return null;

		String clean = itemString.trim().replace("minecraft:", "").toUpperCase();
		try {
			Material material = Material.valueOf(clean);
			return new ItemStack(material);
		} catch (IllegalArgumentException e) {
			FreshFishing.getInstance().getLogger().warning("Failed to parse item (invalid material): " + itemString);
			return null;
		}
	}
}