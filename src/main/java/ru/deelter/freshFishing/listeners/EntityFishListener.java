package ru.deelter.freshFishing.listeners;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

public class EntityFishListener implements Listener {

	private final ProbabilityCollection<EntityType> possibleEntities;
	private final double vectorMultiply, vectorX, vectorY, vectorZ;
	private final int fishConsumeDamage;

	public EntityFishListener(@NotNull FreshFishing plugin) {
		FreshFishingConfig config = plugin.getConfigManager();
		this.possibleEntities = config.getEntityCollection();
		this.vectorMultiply = config.getVectorMultiply();
		this.vectorX = config.getVectorX();
		this.vectorY = config.getVectorY();
		this.vectorZ = config.getVectorZ();
		this.fishConsumeDamage = config.getFishConsumeDamage();
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

		Vector playerVector = player.getLocation().toVector();
		Vector fishedVector = fishedEntity.getLocation().toVector();
		Vector velocity = playerVector.subtract(fishedVector)
				.normalize()
				.add(new Vector(vectorX, vectorY, vectorZ))
				.multiply(vectorMultiply);
		fishedEntity.setVelocity(velocity);
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

		result.copyDataFrom(source, type -> type != DataComponentTypes.FOOD);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFishCampfireCook(@NotNull BlockCookEvent event) {
		ItemStack source = event.getSource();
		if (!FishUtil.isFish(source)) return;

		ItemStack result = event.getResult();
		if (result.isEmpty()) return;

		result.copyDataFrom(source, type -> type != DataComponentTypes.FOOD);
		event.setResult(result);
	}
}