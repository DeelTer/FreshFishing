package ru.deelter.freshFishing.listeners;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.deelter.emotes.api.EmoteTriggers;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.utils.FishUtil;

/**
 * Fires emote triggers when a player reels something in, so an emote can play by itself.
 *
 * <p>Two separate ids on purpose: pulling up a fish and pulling up a boot are different moments and
 * deserve different reactions — a player will want to celebrate one and despair at the other.</p>
 *
 * <p><b>The only class in this plugin that mentions BetterEmotes.</b> A soft dependency has to be
 * isolated to one class: the JVM resolves a class's references when the class is loaded, not when a
 * method is called, so a check inside a class that also names {@code EmoteTriggers} would already
 * be too late — the class fails to load and the plugin dies on enable. {@link #register} does the
 * check from outside and is the only entry point.</p>
 */
public final class EmoteTriggerListener implements Listener {

	/** A fish came up. */
	private static final String TRIGGER_FISH = "fish-caught";

	/** Anything else came up — treasure, junk, a boot. */
	private static final String TRIGGER_ITEM = "item-caught";

	/**
	 * Declares the triggers and starts listening, but only if BetterEmotes is installed.
	 * Call from {@code onEnable}; does nothing when the plugin is absent.
	 */
	public static void register(@NotNull FreshFishing plugin) {
		if (plugin.getServer().getPluginManager().getPlugin("BetterEmotes") == null) return;

		EmoteTriggers.register(TRIGGER_FISH, "Caught a fish");
		EmoteTriggers.register(TRIGGER_ITEM, "Fished up an item");
		plugin.getServer().getPluginManager().registerEvents(new EmoteTriggerListener(), plugin);
		plugin.getLogger().info("BetterEmotes found — fishing emote triggers registered.");
	}

	private EmoteTriggerListener() {}

	/**
	 * MONITOR and not cancelled-sensitive in either direction: this only reacts, never influences
	 * the catch. Runs after {@link EntityFishListener}, which may remove the item entity and put a
	 * mob in its place — the item stack has already been read by then, so the emote still reflects
	 * what the player actually hooked.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFish(@NotNull PlayerFishEvent event) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
		if (!(event.getCaught() instanceof Item caught)) return;

		Player player = event.getPlayer();
		ItemStack stack = caught.getItemStack();
		EmoteTriggers.fire(player, FishUtil.isFish(stack) ? TRIGGER_FISH : TRIGGER_ITEM);
	}
}
