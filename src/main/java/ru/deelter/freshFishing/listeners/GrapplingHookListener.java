package ru.deelter.freshFishing.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
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

	private final boolean wallGrappleEnabled;
	private final boolean wallGrappleConsumesDurability;
	private final int grapplingCooldownTicks;

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
		this.wallGrappleEnabled = config.isGrapplingHookWallGrapple();
		this.wallGrappleConsumesDurability = config.isGrapplingHookWallConsumesDurability();
		this.grapplingCooldownTicks = config.getGrapplingCooldownTicks();

		if (enabled) plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	/** What the hook caught on, which decides both whether to pull and what it costs. */
	private enum Anchor {
		/** Nothing to grapple from — the hook is in the air or floating in water. */
		NONE,
		/** Solid ground under the hook. */
		GROUND,
		/** A solid block beside the hook. */
		WALL
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFishGround(PlayerFishEvent event) {
		if (!enabled) return;
		if (event.getState() == PlayerFishEvent.State.FISHING) return;

		FishHook hook = event.getHook();
		if (hook.getHookedEntity() != null) return;

		Anchor anchor = anchorOf(hook);
		if (anchor == Anchor.NONE) return;

		Player player = event.getPlayer();
		pullEntity(player, hook.getLocation(), pullStrength);
		playEffects(player.getLocation(), hook.getLocation());
		applyCooldown(player);

		// Climbing a wall is the one grapple that costs no durability: a wall run is many short
		// hops, and charging for each of them wears a rod out in a single climb. Ground pulls
		// still cost, so long-distance travel is not free.
		if (anchor == Anchor.WALL && !wallGrappleConsumesDurability) {
			refundRodDurability(player);
		}
	}

	/**
	 * Where the hook is anchored.
	 *
	 * <p>Water beats everything: a bobber sitting in water is <b>fishing</b>, not grappling. Without
	 * that check a waterlogged slab (or any water with a solid block under it) read as ground, and
	 * casting into shallow water yanked the player instead of catching anything — which made
	 * fishing in a half-block of water impossible.</p>
	 */
	private @NonNull Anchor anchorOf(@NonNull FishHook hook) {
		if (isInWater(hook)) return Anchor.NONE;

		if (hook.isOnGround()) return Anchor.GROUND;

		Location below = hook.getLocation().clone().subtract(0, 0.1, 0);
		if (below.getBlock().getType().isSolid()) return Anchor.GROUND;

		if (wallGrappleEnabled) {
			for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
				if (hook.getLocation().getBlock().getRelative(face).getType().isSolid()) return Anchor.WALL;
			}
		}
		return Anchor.NONE;
	}

	/**
	 * True if the bobber is floating in water — including a waterlogged block such as a slab,
	 * stairs or a fence, where the block itself is solid but the bobber is in the water inside it.
	 */
	private boolean isInWater(@NonNull FishHook hook) {
		if (hook.isInWater()) return true;

		Block block = hook.getLocation().getBlock();
		if (block.getType() == Material.WATER) return true;
		return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
	}

	/**
	 * Gives back the durability point vanilla takes for retrieving the rod.
	 *
	 * <p>Deferred a tick because the damage is applied by the retrieve that fires this event, so
	 * repairing during the event would be overwritten a moment later. Repairing rather than
	 * cancelling keeps this independent of where in the retrieve the damage happens.</p>
	 */
	private void refundRodDurability(@NonNull Player player) {
		FreshFishing.getInstance().getServer().getScheduler().runTask(
				FreshFishing.getInstance(), () -> {
					ItemStack rod = rodInHand(player);
					if (rod == null || !rod.hasData(DataComponentTypes.DAMAGE)) return;
					int damage = rod.getData(DataComponentTypes.DAMAGE);
					if (damage <= 0) return;
					rod.setData(DataComponentTypes.DAMAGE, damage - 1);
				});
	}

	/** The fishing rod the player is holding, main hand first. Null if they swapped it away. */
	private @Nullable ItemStack rodInHand(@NonNull Player player) {
		ItemStack main = player.getInventory().getItemInMainHand();
		if (main.getType() == Material.FISHING_ROD) return main;
		ItemStack off = player.getInventory().getItemInOffHand();
		return off.getType() == Material.FISHING_ROD ? off : null;
	}

	private void applyCooldown(@NonNull Player player) {
		if (grapplingCooldownTicks <= 0) return;
		player.setCooldown(Material.FISHING_ROD, grapplingCooldownTicks);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFishEntity(PlayerFishEvent event) {
		if (!enabled) return;
		if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) return;
		Entity hooked = event.getHook().getHookedEntity();
		if (hooked == null) return;
		Player player = event.getPlayer();

		Location hookedRef = hooked instanceof LivingEntity le ? le.getEyeLocation() : hooked.getLocation();
		boolean entityHigher = hookedRef.getY() > player.getEyeLocation().getY() + yOffsetCheck;
		Location playerRef = player.getEyeLocation();
		if (player.isSneaking()) {
			pullEntity(hooked, playerRef, pullEntityStrength);
		} else {
			if (entityHigher) {
				pullEntity(player, hookedRef, pullStrength);
			} else {
				pullEntity(hooked, playerRef, pullEntityStrength);
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
			Location entityRef = entity instanceof LivingEntity le ? le.getEyeLocation() : entity.getLocation();
			Vector toTarget = target.toVector().subtract(entityRef.toVector());
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