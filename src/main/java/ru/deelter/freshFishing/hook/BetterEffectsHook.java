package ru.deelter.freshFishing.hook;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import ru.deelter.effects.api.BetterEffectsAPI;
import ru.deelter.effects.api.BetterEffectsProvider;
import ru.deelter.effects.api.effect.EffectService;

/**
 * <b>The only class in this plugin that mentions BetterEffects.</b> The JVM resolves a class's
 * references when the class is loaded, so every BetterEffects type lives here and is reached only
 * through {@link ScreenEffects}, which checks that the plugin is enabled first.
 */
final class BetterEffectsHook {

	private BetterEffectsHook() {
	}

	/** Plays a preset if the API is registered, the preset exists and the client has the pack. */
	static void playPreset(@NonNull Player player, @NonNull String preset, double intensityScale) {
		BetterEffectsAPI api = BetterEffectsProvider.find().orElse(null);
		if (api == null) return;
		EffectService effects = api.effects();
		if (!effects.presets().contains(preset) || !effects.canReceive(player)) return;
		effects.playPreset(player, preset, intensityScale, 1.0);
	}
}
