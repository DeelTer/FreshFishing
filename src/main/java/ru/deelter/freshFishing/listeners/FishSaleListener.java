package ru.deelter.freshFishing.listeners;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault2.economy.Economy;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.shop.FishSaleMenu;
import ru.deelter.freshFishing.utils.FishUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class FishSaleListener implements Listener {

	private final FreshFishingConfig config;
	private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

	public FishSaleListener(FreshFishing plugin) {
		this.config = plugin.getConfigManager();
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onOpen(@NonNull InventoryOpenEvent event) {
		if (!(event.getInventory().getHolder() instanceof FishSaleMenu menu)) return;
		double total = calculateTotal(event.getInventory());
		menu.updateSellButton(total);
	}

	@EventHandler
	public void onClick(@NonNull InventoryClickEvent event) {
		if (!(event.getInventory().getHolder() instanceof FishSaleMenu menu)) return;

		Player player = (Player) event.getWhoClicked();
		int slot = event.getRawSlot();
		Inventory inv = event.getInventory();
		int saleSlot = menu.getSaleSlot();
		int bottomRowStart = inv.getSize() - 9;

		boolean isBottomBorder = slot >= bottomRowStart && slot < inv.getSize() && slot != saleSlot;
		boolean isSellArea = slot >= 0 && slot < 18;
		boolean isSellButton = slot == saleSlot;
		boolean isPlayerInventory = slot >= 27;

		if (isBottomBorder) {
			event.setCancelled(true);
			return;
		}

		if (isPlayerInventory) {
			if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
				ItemStack current = event.getCurrentItem();
				if (current != null && !current.isEmpty()) {
					int firstEmpty = getFirstEmptySlot(inv, 0, 18);
					if (firstEmpty != -1) {
						inv.setItem(firstEmpty, current.clone());
						current.setAmount(0);
						event.setCancelled(true);
						updateTotalAndButton(inv, menu);
					}
				}
			}
			return;
		}

		if (isSellButton) {
			event.setCancelled(true);
			double total = calculateTotal(inv);
			if (total <= 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return;
			}

			Economy economy = setupEconomy();
			if (economy != null) {
				FreshFishing.getInstance().getLogger().info(String.format("Player %s sell and receive %s coins. Content: %s",
						player.getName(),
						total,
						formatItemsString(inv)
						));

				economy.deposit(FreshFishing.getInstance().getName(), player.getUniqueId(), BigDecimal.valueOf(total));
				if (config.getSaleSound() != null) {
					player.playSound(player.getLocation(), config.getSaleSound(), 1f, 1f);
				}
			} else {
				player.sendMessage(Component.text("You would get " + decimalFormat.format(total) + " coins, but economy is not set up!"));
			}

			for (int i = 0; i < 18; i++) {
				inv.setItem(i, null);
			}
			menu.updateSellButton(0);
			player.closeInventory();
			return;
		}

		if (isSellArea) {
			Bukkit.getScheduler().runTaskLater(FreshFishing.getInstance(), () -> {
				double total = calculateTotal(inv);
				menu.updateSellButton(total);
			}, 1L);
			return;
		}

		event.setCancelled(true);
	}

	private @NonNull String formatItemsString(Inventory inv) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 18; i++) {
			ItemStack item = inv.getItem(i);
			if (item == null || item.isEmpty()) continue;

			s.append(item.getType()).append(", ");
		}
		return s.toString();
	}

	private int getFirstEmptySlot(Inventory inv, int start, int end) {
		for (int i = start; i < end; i++) {
			ItemStack item = inv.getItem(i);
			if (item == null || item.isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	private void updateTotalAndButton(Inventory inv, FishSaleMenu menu) {
		Bukkit.getScheduler().runTaskLater(FreshFishing.getInstance(), () -> {
			double total = calculateTotal(inv);
			menu.updateSellButton(total);
		}, 1L);
	}

	@EventHandler
	public void onDrag(@NonNull InventoryDragEvent event) {
		if (!(event.getInventory().getHolder() instanceof FishSaleMenu menu)) return;

		boolean dragIntoSellArea = event.getNewItems().keySet().stream()
				.anyMatch(slot -> slot >= 0 && slot < 18);
		boolean dragFromSellButton = event.getInventory().getItem(menu.getSaleSlot()) != null &&
				event.getNewItems().containsKey(menu.getSaleSlot());

		int bottomRowStart = event.getInventory().getSize() - 9;
		boolean dragIntoBottomBorder = event.getNewItems().keySet().stream()
				.anyMatch(slot -> slot >= bottomRowStart && slot < event.getInventory().getSize() && slot != menu.getSaleSlot());

		if (dragIntoBottomBorder || dragFromSellButton) {
			event.setCancelled(true);
			return;
		}

		if (dragIntoSellArea) {
			Bukkit.getScheduler().runTaskLater(FreshFishing.getInstance(), () -> {
				double total = calculateTotal(event.getInventory());
				menu.updateSellButton(total);
			}, 1L);
			return;
		}

		boolean dragFromPlayerInv = event.getNewItems().keySet().stream().anyMatch(slot -> slot >= 27);
		if (dragFromPlayerInv) {
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler
	public void onClose(@NonNull InventoryCloseEvent event) {
		if (!(event.getInventory().getHolder() instanceof FishSaleMenu)) return;
		if (!(event.getPlayer() instanceof Player player)) return;

		Inventory inv = event.getInventory();

		for (int i = 0; i < 18; i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && !item.isEmpty()) {
				player.getWorld().dropItemNaturally(player.getLocation(), item);
				inv.setItem(i, null);
			}
		}
	}

	private double calculateTotal(Inventory inv) {
		double total = 0;
		for (int i = 0; i < 18; i++) {
			ItemStack item = inv.getItem(i);
			if (item == null || item.isEmpty()) continue;
			total += getItemPrice(item);
		}
		return total;
	}

	private double getItemPrice(ItemStack item) {
		if (FishUtil.isFish(item)) {
			if (item.getData(DataComponentTypes.DAMAGE) > 0) return 0;

			Pair<FishRarity, Double> attrs = FishUtil.getAttributes(item);
			if (attrs == null) return 0;

			FishRarity rarity = attrs.getLeft();
			double size = attrs.getRight();
			double maxSize = FishSize.SIZES.stream().mapToDouble(FishSize::getMax).max().orElse(600);
			double base = getPriceRangeForRarity(rarity, size, maxSize);
			return base * rarity.getMultiplier();
		} else {
			double base = config.getTrashPrices().getOrDefault(item.getType(), 0.15);
			double enchantBonus = calculateEnchantBonus(item);
			return (base * item.getAmount()) + enchantBonus;
		}
	}

	private double getPriceRangeForRarity(@NonNull FishRarity rarity, double size, double maxSize) {
		var range = config.getFishPriceRanges().get(rarity.getId());
		if (range == null) range = config.getFishPriceRanges().get("default");
		if (range == null) return 8 + (450 - 8) * (size / maxSize);
		double min = range.minPrice();
		double max = range.maxPrice();
		double fraction = Math.min(1.0, size / maxSize);
		return min + (max - min) * fraction;
	}

	private double calculateEnchantBonus(@NonNull ItemStack item) {
		double bonus = 0;
		for (var ench : item.getEnchantments().entrySet()) {
			double price = config.getEnchantmentPrices().getOrDefault(ench.getKey(), 10.0);
			bonus += price * ench.getValue();
		}
		return bonus;
	}

	private @Nullable Economy setupEconomy() {
		RegisteredServiceProvider<Economy> rsp = FreshFishing.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			FreshFishing.getInstance().getLogger().warning("VaultUnlocked economy not found! Money will not be given.");
			return null;
		}
		return rsp.getProvider();
	}
}