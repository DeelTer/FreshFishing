package ru.deelter.vr.freshFishing.listeners;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import ru.deelter.vr.freshFishing.FreshFishing;
import ru.deelter.vr.freshFishing.utils.FishUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PlayerRestrictsListener implements Listener {

	private final Set<Biome> onlyBiomes = new HashSet<>();
	private final Component onlyBiomesWarning;
	private final int hungerPointsWhenFish;
	private final int hungerPointsWhenItem;
	private final int minHungerLevel;

	public PlayerRestrictsListener(@NotNull FreshFishing plugin) {
		FileConfiguration config = plugin.getConfig();
		hungerPointsWhenFish = config.getInt("hunger.fish");
		hungerPointsWhenItem = config.getInt("hunger.item");
		minHungerLevel = config.getInt("hunger.min-level-for-fishing");

		String warningString = config.getString("fish-only-in-biomes.warning");
		if (warningString != null && !warningString.isBlank()) {
			onlyBiomesWarning = MiniMessage.miniMessage().deserialize(warningString);
		} else {
			onlyBiomesWarning = null;
		}
		config.getStringList("fish-only-in-biomes.biomes").forEach(biomeString -> {
			Biome biome = RegistryAccess.registryAccess()
					.getRegistry(RegistryKey.BIOME)
					.get(Objects.requireNonNull(NamespacedKey.fromString(biomeString)));
			onlyBiomes.add(biome);
		});

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerFish(@NotNull PlayerFishEvent event) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
		if (!(event.getCaught() instanceof Item caughtItem)) return;

		Player player = event.getPlayer();
		FishHook hook = event.getHook();

		if (!onlyBiomes.isEmpty()) {
			Biome biome = hook.getLocation().getBlock().getBiome();

			if (!onlyBiomes.contains(biome)) {
				if (onlyBiomesWarning != null) {
					player.sendActionBar(onlyBiomesWarning);
				}
				event.setCancelled(true);
				return;
			}
		}

		int hungerPoints = FishUtil.isFish(caughtItem.getItemStack()) ? hungerPointsWhenFish : hungerPointsWhenItem;
		int playerFoodLevel = player.getFoodLevel();

		if (playerFoodLevel < minHungerLevel) {
			player.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f);
			event.setCancelled(true);
			return;
		}
		player.setFoodLevel(playerFoodLevel - hungerPoints);
	}
}
