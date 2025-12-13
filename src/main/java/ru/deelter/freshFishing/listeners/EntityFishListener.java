package ru.deelter.vr.freshFishing.listeners;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ru.deelter.vr.freshFishing.FreshFishing;
import ru.deelter.vr.freshFishing.utils.FishUtil;
import ru.deelter.vr.freshFishing.utils.ProbabilityCollection;

import java.util.Objects;

public class EntityFishListener implements Listener {

	private final ProbabilityCollection<EntityType> possibleEntities = new ProbabilityCollection<>();
	private final double vectorMultiply, vectorX, vectorY, vectorZ;

	public EntityFishListener(@NotNull FreshFishing plugin) {
		FileConfiguration config = plugin.getConfig();

		vectorMultiply = config.getDouble("vector-caught-object.multiply");
		vectorX = config.getDouble("vector-caught-object.x");
		vectorY = config.getDouble("vector-caught-object.y");
		vectorZ = config.getDouble("vector-caught-object.z");

		ConfigurationSection possibleEntitiesSection = Objects.requireNonNull(config.getConfigurationSection("entities"));
		possibleEntitiesSection.getKeys(false).forEach(entityTypeString -> {

			EntityType entityType = EntityType.valueOf(entityTypeString.toUpperCase());
			int blocks = possibleEntitiesSection.getInt(entityTypeString);

			possibleEntities.add(entityType, blocks);
		});

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerFish(@NotNull PlayerFishEvent event) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
		if (!(event.getCaught() instanceof Item entityItem)) return;
		if (!FishUtil.isFish(entityItem.getItemStack())) {
			return;
		}
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
