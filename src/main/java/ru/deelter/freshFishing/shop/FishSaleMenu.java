package ru.deelter.freshFishing.shop;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;

public class FishSaleMenu implements InventoryHolder {

    private final Inventory inventory;

    public FishSaleMenu() {
        var config = FreshFishing.getInstance().getConfigManager();
        int size = config.getSaleShopSize();
        String title = config.getSaleShopTitle();
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }
}