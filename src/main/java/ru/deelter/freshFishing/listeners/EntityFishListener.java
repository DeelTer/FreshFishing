package ru.deelter.freshFishing.listeners;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.api.event.FishCaughtEvent;
import ru.deelter.freshFishing.config.EventsConfig;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.events.FishBossManager;
import ru.deelter.freshFishing.stats.FishLeaderboard;
import ru.deelter.freshFishing.stats.FishRecord;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

import java.util.IdentityHashMap;
import java.util.Map;

public class EntityFishListener implements Listener {

	private final FreshFishingConfig config;
	private final FishLeaderboard leaderboard;
	private final Map<PlayerFishEvent, FishRecord> pendingRecords = new IdentityHashMap<>();

	private final ProbabilityCollection<EntityType> possibleEntities;
	private final double vectorMultiply, vectorX, vectorY, vectorZ;
	private final int fishConsumeDamage;
	private final double baitSizeBonusPerNutrition;
	private final boolean consumeBaitOnCatch;
	private final boolean hookToCatch;
	private final FreshFishing plugin;
	private final FishBossManager bossManager;
	private final EventsConfig eventsConfig;

	public EntityFishListener(@NotNull FreshFishing plugin) {
		this.config = plugin.getConfigManager();
		this.leaderboard = plugin.getLeaderboard();
		this.possibleEntities = config.getEntityCollection();
		this.vectorMultiply = config.getVectorMultiply();
		this.vectorX = config.getVectorX();
		this.vectorY = config.getVectorY();
		this.vectorZ = config.getVectorZ();
		this.fishConsumeDamage = config.getFishConsumeDamage();
		this.baitSizeBonusPerNutrition = config.getBaitSizeBonusPerNutrition();
		this.consumeBaitOnCatch = config.isConsumeBaitOnCatch();
		this.hookToCatch = config.isHookToCatch();
		this.plugin = plugin;
		this.bossManager = plugin.getFishBossManager();
		this.eventsConfig = plugin.getEventsConfig();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerFish(@NotNull PlayerFishEvent event) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
		if (!(event.getCaught() instanceof Item entityItem)) return;
		if (!FishUtil.isFish(entityItem.getItemStack())) return;

		entityItem.remove();
		FishHook hook = event.getHook();
		Player player = event.getPlayer();
		Entity fishedEntity = player.getWorld().spawnEntity(
				hook.getLocation(),
				possibleEntities.get(),
				CreatureSpawnEvent.SpawnReason.CUSTOM
		);

		if (fishedEntity instanceof Fish fish) {
			applyBait(player, fish);
			callCaughtEvent(player, fish);
		}

		if (fishedEntity instanceof LivingEntity living) {
			if (eventsConfig.isMonsterBoostEnabled() && living instanceof Monster) {
				double base = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
				FishBossManager.setMaxHealth(living, base * eventsConfig.getMonsterHealthMultiplier());
			}
			bossManager.trySpawnBoss(living, player);
		}

		Vector playerVector = player.getLocation().toVector();
		Vector fishedVector = fishedEntity.getLocation().toVector();
		Vector velocity = playerVector.subtract(fishedVector)
				.normalize()
				.add(new Vector(vectorX, vectorY, vectorZ))
				.multiply(vectorMultiply);
		fishedEntity.setVelocity(velocity);
		if (hookToCatch) hookToCatch(player, fishedEntity);
		if (leaderboard != null && fishedEntity instanceof Fish fish && fish.isValid()) {
			FishRarity rarity = FishUtil.getRarity(fish);
			double size = FishUtil.getSize(fish);
			if (rarity == null || !Double.isFinite(size) || size <= 0) return;
			double value = config.fishValue(rarity, size);
			double score = config.contestScore(rarity, size);
			if (!Double.isFinite(value) || !Double.isFinite(score)) return;
			pendingRecords.put(event, new FishRecord(player.getUniqueId(), player.getName(), size,
					rarity.getId(), MiniMessage.miniMessage().serialize(rarity.getName()), value, score, System.currentTimeMillis()));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onCatchCompleted(PlayerFishEvent event) {
		FishRecord record = pendingRecords.remove(event);
		if (record != null && !event.isCancelled()) leaderboard.record(record);
	}

	/*
		Bait: food in the off hand makes the caught fish bigger, by a percent per hunger point it restores
	 */
	private void applyBait(@NotNull Player player, @NotNull Fish fish) {
		if (baitSizeBonusPerNutrition <= 0) return;

		ItemStack bait = player.getInventory().getItemInOffHand();
		if (bait.isEmpty()) return;

		FoodProperties food = bait.getData(DataComponentTypes.FOOD);
		if (food == null || food.nutrition() <= 0) return;

		FishRarity rarity = FishUtil.getRarity(fish);
		if (rarity == null) return;

		double maxSize = FishSize.SIZES.stream().mapToDouble(FishSize::getMax).max().orElse(Double.MAX_VALUE);
		double boosted = FishUtil.getSize(fish) * (1 + food.nutrition() * baitSizeBonusPerNutrition / 100.0);
		double size = Math.round(Math.min(boosted, maxSize) * 100.0) / 100.0;
		FishUtil.editFish(fish, size, rarity);

		if (consumeBaitOnCatch) {
			bait.subtract();
		}
	}

	/**
	 * Keeps the player's line on the catch: vanilla removes the hook right after this event, so a new
	 * hook is cast next tick and attached to the caught entity. Reeling in then pulls it like a hooked mob.
	 */
	private void hookToCatch(@NotNull Player player, @NotNull Entity caught) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (!player.isOnline() || !caught.isValid() || caught.isDead()) return;
			if (player.getFishHook() != null) return;
			if (player.getInventory().getItemInMainHand().getType() != Material.FISHING_ROD
					&& player.getInventory().getItemInOffHand().getType() != Material.FISHING_ROD) return;
			FishHook hook = player.launchProjectile(FishHook.class);
			hook.teleport(caught.getLocation());
			hook.setHookedEntity(caught);
		});
	}

	/** Lets other plugins change the size of a freshly caught fish. */
	private void callCaughtEvent(@NotNull Player player, @NotNull Fish fish) {
		FishRarity rarity = FishUtil.getRarity(fish);
		if (rarity == null) return;
		double size = FishUtil.getSize(fish);
		FishCaughtEvent caughtEvent = new FishCaughtEvent(player, fish, rarity, size);
		caughtEvent.callEvent();
		if (caughtEvent.getSize() == size || !Double.isFinite(caughtEvent.getSize()) || caughtEvent.getSize() <= 0) return;

		double maxSize = FishSize.SIZES.stream().mapToDouble(FishSize::getMax).max().orElse(Double.MAX_VALUE);
		double newSize = Math.round(Math.min(caughtEvent.getSize(), maxSize) * 100.0) / 100.0;
		FishUtil.editFish(fish, newSize, rarity);
	}

	/*
		Items Functionality
	 */
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onConsumeFish(@NotNull PlayerItemConsumeEvent event) {
		ItemStack item = event.getItem();

		if (!FishUtil.isFish(item)) return;
		if (fishConsumeDamage <= 0) return;
		if (!item.hasData(DataComponentTypes.MAX_DAMAGE)) return;

		int maxDamage = FishUtil.recalculateMaxDamage(item);
		int newDamage = item.getData(DataComponentTypes.DAMAGE) + 1;

		if (newDamage >= maxDamage) return;

		item.setData(DataComponentTypes.DAMAGE, newDamage);
		event.setReplacement(item);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishSmelt(@NotNull FurnaceSmeltEvent event) {
		ItemStack source = event.getSource();
		if (!FishUtil.isFish(source)) return;

		ItemStack result = event.getResult();
		if (result.isEmpty()) return;

		result.copyDataFrom(source, this::canCopy);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishCampfireCook(@NotNull BlockCookEvent event) {
		ItemStack source = event.getSource();
		if (!FishUtil.isFish(source)) return;

		ItemStack result = event.getResult();
		if (result.isEmpty()) return;

		result.copyDataFrom(source, this::canCopy);
		event.setResult(result);
	}

	private boolean canCopy(@NotNull DataComponentType type) {
		return type != DataComponentTypes.FOOD && type != DataComponentTypes.ITEM_NAME && type != DataComponentTypes.ITEM_MODEL;
	}
}
