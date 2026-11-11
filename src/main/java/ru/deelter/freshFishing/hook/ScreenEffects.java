package ru.deelter.freshFishing.hook;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Screen effects through the optional BetterEffects plugin. Every call is a no-op when BetterEffects
 * is not enabled, when the preset name is empty, or when the preset does not exist.
 */
public final class ScreenEffects {

	private static final String BETTER_EFFECTS = "BetterEffects";
	/** Players at the edge of a radius still feel a quarter of the effect. */
	private static final double MIN_FALLOFF = 0.25;

	private ScreenEffects() {
	}

	public static void playPreset(@NonNull Player player, @Nullable String preset) {
		playPreset(player, preset, 1.0);
	}

	public static void playPreset(@NonNull Player player, @Nullable String preset, double intensityScale) {
		if (preset == null || preset.isBlank()) return;
		if (!Bukkit.getPluginManager().isPluginEnabled(BETTER_EFFECTS)) return;
		try {
			BetterEffectsHook.playPreset(player, preset, intensityScale);
		} catch (LinkageError ignored) {
			// BetterEffects without a compatible API: no effects
		}
	}

	public static void showOvertake(Player player, Component message) {
		if (Bukkit.getPluginManager().isPluginEnabled(BETTER_EFFECTS)) {
			try {
				if (BetterEffectsHook.showOvertake(player, message)) return;
			} catch (LinkageError ignored) {
				// Fall back to the vanilla action bar on an incompatible API.
			}
		}
		player.sendActionBar(message);
	}

	/** Plays a preset for every player within {@code radius}, weaker with distance. */
	public static void playPresetNearby(@NonNull Location center, double radius, @Nullable String preset) {
		if (preset == null || preset.isBlank() || radius <= 0 || center.getWorld() == null) return;
		if (!Bukkit.getPluginManager().isPluginEnabled(BETTER_EFFECTS)) return;
		for (Player player : center.getWorld().getNearbyPlayers(center, radius)) {
			double distance = player.getLocation().distance(center);
			playPreset(player, preset, Math.clamp(1.0 - distance / radius, MIN_FALLOFF, 1.0));
		}
	}
}
