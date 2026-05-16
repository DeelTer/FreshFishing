package ru.deelter.freshFishing.listeners;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.utils.PhysicsUtils;

public class GrapplingHookListener implements Listener {

	private final boolean enabled;
	private final double pullStrength;
	private final double pullEntityStrength;
	private final Sound hookSound;
	private final Sound playerPullSound;
	private final double yOffsetCheck;

	// Настройки физики
	private final boolean usePhysics;
	private final double upwardBoost;
	private final int pullDelayTicks;
	private final double dragHorizontal;
	private final double dragVertical;
	private final double velocityMultiplier;

	public GrapplingHookListener(FreshFishing plugin) {
		FreshFishingConfig config = plugin.getConfigManager();
		this.enabled = config.isGrapplingHookEnabled();
		this.pullStrength = config.getPullStrength();
		this.pullEntityStrength = config.getPullEntityStrength();
		this.hookSound = config.getHookSound();
		this.playerPullSound = config.getPlayerPullSound();
		this.yOffsetCheck = config.getYOffsetCheck();

		this.usePhysics = config.isGrapplingHookUsePhysics();
		this.upwardBoost = config.getGrapplingHookUpwardBoost();
		this.pullDelayTicks = config.getGrapplingHookPullDelayTicks();
		this.dragHorizontal = config.getGrapplingHookDragHorizontal();
		this.dragVertical = config.getGrapplingHookDragVertical();
		this.velocityMultiplier = config.getGrapplingHookVelocityMultiplier();

		if (enabled) plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFishGround(PlayerFishEvent event) {
		if (!enabled) return;
		if (event.getState() != PlayerFishEvent.State.IN_GROUND) return;
		FishHook hook = event.getHook();
		if (hook.getHookedEntity() != null) return;
		Player player = event.getPlayer();
		pullEntity(player, hook.getLocation(), pullStrength);
		playEffects(player.getLocation(), hook.getLocation());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFishEntity(PlayerFishEvent event) {
		if (!enabled) return;
		if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
		Entity hooked = event.getHook().getHookedEntity();
		if (hooked == null) return;
		Player player = event.getPlayer();

		boolean entityHigher = hooked.getLocation().getY() > player.getEyeLocation().getY() + yOffsetCheck;
		if (player.isSneaking()) {
			pullEntity(hooked, player.getLocation(), pullEntityStrength);
		} else {
			if (entityHigher) {
				pullEntity(player, hooked.getLocation(), pullStrength);
			} else {
				pullEntity(hooked, player.getLocation(), pullEntityStrength);
			}
		}
		playEffects(player.getLocation(), hooked.getLocation());
	}

	private void pullEntity(Entity entity, Location target, double strength) {
		if (usePhysics) {
			PhysicsUtils.pullEntityToLocation(entity, target,
					velocityMultiplier * strength,
					upwardBoost, pullDelayTicks,
					dragHorizontal, dragVertical);
		} else {
			// Упрощённый вариант
			Vector toTarget = target.toVector().subtract(entity.getLocation().toVector());
			double len = toTarget.length();
			if (len < 0.1) return;
			Vector vel = toTarget.normalize().multiply(Math.min(strength, len));
			entity.setVelocity(vel);
		}
	}

	private void playEffects(@NonNull Location loc1, @NonNull Location loc2) {
		loc1.getWorld().playSound(loc1, hookSound, 1f, 1f);
		loc2.getWorld().playSound(loc2, playerPullSound, 1f, 1f);
		loc1.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc1, 1);
	}
}