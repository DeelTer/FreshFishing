package ru.deelter.freshFishing.listeners;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.damage.DamageType;
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
import ru.deelter.freshFishing.config.EventsConfig;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.events.FishBossManager;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

public class EntityFishListener implements Listener {

	private final ProbabilityCollection<EntityType> possibleEntities;
	private final double vectorMultiply, vectorX, vectorY, vectorZ;
	private final int fishConsumeDamage;
	private final FishBossManager bossManager;
	private final EventsConfig eventsConfig;

	public EntityFishListener(@NotNull FreshFishing plugin) {
		FreshFishingConfig config = plugin.getConfigManager();
		this.possibleEntities = config.getEntityCollection();
		this.vectorMultiply = config.getVectorMultiply();
		this.vectorX = config.getVectorX();
		this.vectorY = config.getVectorY();
		this.vectorZ = config.getVectorZ();
		this.fishConsumeDamage = config.getFishConsumeDamage();
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