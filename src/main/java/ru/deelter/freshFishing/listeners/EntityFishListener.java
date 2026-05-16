package ru.deelter.freshFishing.listeners;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.utils.FishUtil;
import ru.deelter.freshFishing.utils.ProbabilityCollection;

public class EntityFishListener implements Listener {

	private final ProbabilityCollection<EntityType> possibleEntities;
	private final double vectorMultiply, vectorX, vectorY, vectorZ;

	public EntityFishListener(@NotNull FreshFishing plugin) {
		FreshFishingConfig config = plugin.getConfigManager();
		this.possibleEntities = config.getEntityCollection();
		this.vectorMultiply = config.getVectorMultiply();
		this.vectorX = config.getVectorX();
		this.vectorY = config.getVectorY();
		this.vectorZ = config.getVectorZ();
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
}