package ru.deelter.freshFishing.lostitems;

import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.utils.FishUtil;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Valuable items that despawn in water go to a local SQLite database (Paper bundles the driver) and
 * may be fished up later by anyone. Writes run on one background thread; the rare read on a catch is
 * a single indexed query on the main thread.
 */
public class LostItemsManager implements Listener {

	private final FreshFishing plugin;
	private final FreshFishingConfig config;
	private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "FreshFishing-LostItems"));
	private final Object lock = new Object();
	private Connection connection;

	public LostItemsManager(@NotNull FreshFishing plugin) throws SQLException {
		this.plugin = plugin;
		this.config = plugin.getConfigManager();
		plugin.getDataFolder().mkdirs();
		File file = new File(plugin.getDataFolder(), "lost-items.db");
		connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS lost_items ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT, data BLOB NOT NULL, created INTEGER NOT NULL)");
		}
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void close() {
		writer.shutdown();
		try {
			writer.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		synchronized (lock) {
			try {
				if (connection != null) connection.close();
			} catch (SQLException ignored) {
			}
			connection = null;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDespawn(@NotNull ItemDespawnEvent event) {
		Item entity = event.getEntity();
		if (!entity.isInWater()) return;
		ItemStack item = entity.getItemStack();
		if (!isWorthKeeping(item)) return;
		byte[] data = item.serializeAsBytes();
		writer.execute(() -> store(data));
	}

	/**
	 * Replaces a non-fish catch with a lost item. HIGHEST: the row is deleted when taken, so this runs
	 * after every plugin that may still cancel the catch. Fish are skipped, so FreshFishing's fish
	 * handling (NORMAL) is not affected.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCatch(@NotNull PlayerFishEvent event) {
		if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
		if (!(event.getCaught() instanceof Item caught)) return;
		if (FishUtil.isFish(caught.getItemStack())) return;
		// Another plugin already replaced this catch (a vanilla catch has no entity data)
		if (!caught.getPersistentDataContainer().isEmpty()) return;
		if (ThreadLocalRandom.current().nextDouble() >= config.getLostItemsCatchChance()) return;
		ItemStack lost = takeRandom();
		if (lost != null) caught.setItemStack(lost);
	}

	public boolean isWorthKeeping(@NotNull ItemStack item) {
		if (item.isEmpty()) return false;
		if (FishUtil.isFish(item) || config.getTrashPrices().containsKey(item.getType())) return true;
		if (!item.getEnchantments().isEmpty() || item.hasData(DataComponentTypes.STORED_ENCHANTMENTS)) return true;
		return item.hasData(DataComponentTypes.CUSTOM_NAME)
				|| item.isDataOverridden(DataComponentTypes.ITEM_NAME)
				|| item.isDataOverridden(DataComponentTypes.LORE);
	}

	private void store(byte[] data) {
		synchronized (lock) {
			if (connection == null) return;
			try (PreparedStatement insert = connection.prepareStatement("INSERT INTO lost_items (data, created) VALUES (?, ?)")) {
				insert.setBytes(1, data);
				insert.setLong(2, System.currentTimeMillis());
				insert.executeUpdate();
			} catch (SQLException e) {
				plugin.getLogger().warning("Could not save a lost item: " + e.getMessage());
				return;
			}
			try (PreparedStatement trim = connection.prepareStatement(
					"DELETE FROM lost_items WHERE id NOT IN (SELECT id FROM lost_items ORDER BY id DESC LIMIT ?)")) {
				trim.setInt(1, Math.max(1, config.getLostItemsMaxStored()));
				trim.executeUpdate();
			} catch (SQLException e) {
				plugin.getLogger().warning("Could not trim lost items: " + e.getMessage());
			}
		}
	}

	/** Removes and returns a random lost item, or null when there is none. */
	public @Nullable ItemStack takeRandom() {
		synchronized (lock) {
			if (connection == null) return null;
			try (Statement select = connection.createStatement();
				 ResultSet result = select.executeQuery("SELECT id, data FROM lost_items ORDER BY RANDOM() LIMIT 1")) {
				if (!result.next()) return null;
				long id = result.getLong(1);
				byte[] data = result.getBytes(2);
				try (PreparedStatement delete = connection.prepareStatement("DELETE FROM lost_items WHERE id = ?")) {
					delete.setLong(1, id);
					delete.executeUpdate();
				}
				return ItemStack.deserializeBytes(data);
			} catch (SQLException | IllegalArgumentException e) {
				plugin.getLogger().warning("Could not read a lost item: " + e.getMessage());
				return null;
			}
		}
	}
}
