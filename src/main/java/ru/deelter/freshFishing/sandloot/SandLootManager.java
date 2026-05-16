package ru.deelter.freshFishing.sandloot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrushableBlock;
import org.bukkit.entity.Marker;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SandLootManager {

	private static SandLootManager instance;
	private final FreshFishing plugin;
	private final boolean enabled;
	private final List<ItemStack> lootPool = new ArrayList<>();
	private final long intervalTicks;
	private final int maxBlocksPerMarker;
	private final int minItemsPerSpawn;
	private final int maxItemsPerSpawn;
	private int taskId = -1;

	private static final List<BlockFace> BLOCK_FACES = Arrays.asList(
			BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
			BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
			BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH_NORTH_EAST,
			BlockFace.EAST_NORTH_EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
			BlockFace.SOUTH_SOUTH_WEST, BlockFace.WEST_SOUTH_WEST, BlockFace.SELF
	);

	public static SandLootManager get() {
		return instance;
	}

	public SandLootManager(@NonNull FreshFishing plugin) {
		this.plugin = plugin;
		FreshFishingConfig cfg = plugin.getConfigManager();
		this.enabled = cfg.isSandLootEnabled();
		this.intervalTicks = cfg.getSandLootIntervalTicks();
		this.maxBlocksPerMarker = cfg.getMaxBlocksPerMarker();
		this.minItemsPerSpawn = cfg.getSandLootMinItemsPerSpawn();
		this.maxItemsPerSpawn = cfg.getSandLootMaxItemsPerSpawn();
		this.lootPool.addAll(cfg.getSandLootItems());
		instance = this;

		if (!enabled) return;

		startTask();
	}

	private void startTask() {
		if (taskId != -1) return;
		taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (!enabled || lootPool.isEmpty()) return;

			for (World world : Bukkit.getWorlds()) {
				for (Marker marker : world.getEntitiesByClass(Marker.class)) {
					if (!marker.isValid()) continue;
					if (!marker.getPersistentDataContainer().has(plugin.getMarkerKey(), PersistentDataType.BOOLEAN))
						continue;

					Block center = marker.getLocation().getBlock();
					List<Block> candidates = findNearbyBlocks(center);
					if (candidates.isEmpty()) continue;

					int maxPossible = Math.min(candidates.size(), maxItemsPerSpawn);
					if (maxPossible < minItemsPerSpawn) maxPossible = minItemsPerSpawn;
					int itemsToSpawn = new Random().nextInt(maxPossible - minItemsPerSpawn + 1) + minItemsPerSpawn;

					for (int i = 0; i < itemsToSpawn && !candidates.isEmpty(); i++) {
						Block target = candidates.remove(new Random().nextInt(candidates.size()));
						ItemStack loot = lootPool.get(new Random().nextInt(lootPool.size())).clone();
						setBrushableItem(target, loot);
					}
				}
			}
		}, intervalTicks, intervalTicks).getTaskId();
	}

	private @NonNull List<Block> findNearbyBlocks(Block center) {
		List<Block> result = new ArrayList<>();
		Random random = new Random();

		for (int yOffset = 0; yOffset >= -1; yOffset--) {
			Block yLevel = center.getRelative(0, yOffset, 0);

			for (BlockFace face : BLOCK_FACES) {
				if (result.size() >= maxBlocksPerMarker) break;
				if (random.nextBoolean()) continue;

				Block rel = yLevel.getRelative(face);
				Material type = rel.getType();

				if ((type == Material.SAND || type == Material.GRAVEL) && isBlockAccessible(rel)) {
					result.add(rel);
				} else if (rel.getState() instanceof BrushableBlock brushable && brushable.getItem().isEmpty() && isBlockAccessible(rel)) {
					result.add(rel);
				}
			}
		}
		return result;
	}

	private boolean isBlockAccessible(Block block) {
		BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
		for (BlockFace face : sides) {
			Block neighbor = block.getRelative(face);
			if (neighbor.isPassable()) {
				return true;
			}
		}
		return false;
	}

	private void setBrushableItem(@NonNull Block block, ItemStack item) {
		if (block.getState() instanceof BrushableBlock brushable) {
			brushable.setItem(item.clone());
			brushable.update();
		} else {
			Material target = block.getType() == Material.SAND ? Material.SUSPICIOUS_SAND : Material.SUSPICIOUS_GRAVEL;
			block.setType(target);
			if (block.getState() instanceof BrushableBlock newBrushable) {
				newBrushable.setItem(item.clone());
				newBrushable.update();
			}
		}
	}

	public void addMarker(@NonNull Location location) {
		World world = location.getWorld();
		Marker marker = world.spawn(location, Marker.class);
		marker.getPersistentDataContainer().set(plugin.getMarkerKey(), PersistentDataType.BOOLEAN, true);
	}

	public boolean removeMarker(@NonNull Location location) {
		Marker closest = null;
		double minDist = 2.0;
		for (Marker marker : location.getWorld().getEntitiesByClass(Marker.class)) {
			if (!marker.getPersistentDataContainer().has(plugin.getMarkerKey(), PersistentDataType.BOOLEAN)) continue;
			double dist = marker.getLocation().distance(location);
			if (dist < minDist) {
				minDist = dist;
				closest = marker;
			}
		}
		if (closest != null) {
			closest.remove();
			return true;
		}
		return false;
	}

	public List<Marker> getMarkers() {
		List<Marker> allMarkers = new ArrayList<>();
		for (World world : Bukkit.getWorlds()) {
			for (Marker marker : world.getEntitiesByClass(Marker.class)) {
				if (marker.getPersistentDataContainer().has(plugin.getMarkerKey(), PersistentDataType.BOOLEAN)) {
					allMarkers.add(marker);
				}
			}
		}
		return allMarkers;
	}

	public void shutdown() {
		if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
	}
}