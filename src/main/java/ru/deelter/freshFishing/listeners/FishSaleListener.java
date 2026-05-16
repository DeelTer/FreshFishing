package ru.deelter.freshFishing.listeners;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.data.FishRarity;
import ru.deelter.freshFishing.data.FishSize;
import ru.deelter.freshFishing.shop.FishSaleMenu;
import ru.deelter.freshFishing.utils.FishUtil;

public class FishSaleListener implements Listener {

    private final FreshFishingConfig config;
    private final int saleSlot;

    public FishSaleListener(FreshFishing plugin) {
        this.config = plugin.getConfigManager();
        this.saleSlot = config.getSaleButtonSlot();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishSaleMenu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();

        if (slot == saleSlot) {
            double total = calculateTotal(inv);
            if (total <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            player.sendMessage(Component.text("You sold fish for " + total + " coins!"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            for (int i = 0; i < 18; i++) inv.setItem(i, null);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof FishSaleMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof FishSaleMenu)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        for (int i = 0; i < 18; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
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

    private double getPriceRangeForRarity(FishRarity rarity, double size, double maxSize) {
        var range = config.getFishPriceRanges().get(rarity.getId());
        if (range == null) range = config.getFishPriceRanges().get("default");
        if (range == null) return 8 + (450 - 8) * (size / maxSize);
        double min = range.minPrice();
        double max = range.maxPrice();
        double fraction = Math.min(1.0, size / maxSize);
        return min + (max - min) * fraction;
    }

    private double calculateEnchantBonus(ItemStack item) {
        double bonus = 0;
        for (var ench : item.getEnchantments().entrySet()) {
            double price = config.getEnchantmentPrices().getOrDefault(ench.getKey(), 10.0);
            bonus += price * ench.getValue();
        }
        return bonus;
    }
}