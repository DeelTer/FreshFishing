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

import java.util.*;

public class SandLootManager {

    private static SandLootManager instance;
    private final FreshFishing plugin;
    private final boolean enabled;
    private final List<ItemStack> lootPool = new ArrayList<>();
    private final long intervalTicks;
    private final int maxBlocksPerMarker;
    private final Set<Marker> markers = new HashSet<>();
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
        this.lootPool.addAll(cfg.getSandLootItems());
        instance = this;

        if (!enabled) return;

        for (World world : Bukkit.getWorlds()) {
            for (Marker marker : world.getEntitiesByClass(Marker.class)) {
                if (marker.getPersistentDataContainer().has(plugin.getMarkerKey(), PersistentDataType.BOOLEAN)) {
                    markers.add(marker);
                }
            }
        }

        startTask();
    }

    private void startTask() {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled || lootPool.isEmpty()) return;
            for (Marker marker : markers) {
                if (!marker.isValid()) continue;
                Block center = marker.getLocation().getBlock();
                List<Block> candidates = findNearbyBlocks(center);
                if (candidates.isEmpty()) continue;
                Block target = candidates.get(new Random().nextInt(candidates.size()));
                if (target == null) continue;
                ItemStack loot = lootPool.get(new Random().nextInt(lootPool.size()));
                setBrushableItem(target, loot);
            }
        }, intervalTicks, intervalTicks).getTaskId();
    }

    private List<Block> findNearbyBlocks(Block center) {
        List<Block> result = new ArrayList<>();
        for (BlockFace face : BLOCK_FACES) {
            if (result.size() >= maxBlocksPerMarker) break;
            Block rel = center.getRelative(face);
            Material type = rel.getType();
            if (type == Material.SAND || type == Material.GRAVEL) {
                result.add(rel);
            } else if (rel.getState() instanceof BrushableBlock brushable && brushable.getItem().isEmpty()) {
                result.add(rel);
            }
        }
        return result;
    }

    private void setBrushableItem(Block block, ItemStack item) {
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

    public void addMarker(Location location) {
        World world = location.getWorld();
        Marker marker = world.spawn(location, Marker.class);
        marker.getPersistentDataContainer().set(plugin.getMarkerKey(), PersistentDataType.BOOLEAN, true);
        markers.add(marker);
    }

    public boolean removeMarker(Location location) {
        Marker closest = null;
        double minDist = 2.0;
        for (Marker m : markers) {
            double dist = m.getLocation().distance(location);
            if (dist < minDist) {
                minDist = dist;
                closest = m;
            }
        }
        if (closest != null) {
            closest.remove();
            markers.remove(closest);
            return true;
        }
        return false;
    }

    public List<Marker> getMarkers() {
        return new ArrayList<>(markers);
    }

    public void shutdown() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
    }
}