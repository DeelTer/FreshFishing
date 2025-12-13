package ru.deelter.freshFishing.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class FishRarity {

	public static final Set<FishRarity> RARITIES = new HashSet<>();

	private final String id;
	private final Component name;
	private final int blocks;
	private final double multiplier;

	public static FishRarity getById(@Nullable String stringId) {
		if (stringId == null) return null;
		for (FishRarity rarity : RARITIES) {
			if (rarity.getId().equalsIgnoreCase(stringId))
				return rarity;
		}
		return null;
	}

}
