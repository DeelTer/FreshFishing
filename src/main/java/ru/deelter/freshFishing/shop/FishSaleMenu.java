package ru.deelter.freshFishing.shop;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;

import java.util.List;

public class FishSaleMenu implements InventoryHolder {

	private final Inventory inventory;
	@Getter
	private final int saleSlot;

	public FishSaleMenu() {
		var config = FreshFishing.getInstance().getConfigManager();
		int size = config.getSaleShopSize();
		String titleRaw = config.getSaleShopTitle();
		Component title = MiniMessage.miniMessage().deserialize(titleRaw);
		this.inventory = Bukkit.createInventory(this, size, title);
		this.saleSlot = config.getSaleButtonSlot();

		setSellButton();
		fillBottomBorder();
	}

	private void fillBottomBorder() {
		int size = inventory.getSize();
		int bottomRowStart = size - 9;

		for (int i = bottomRowStart; i < size; i++) {
			if (i == saleSlot) continue;

			ItemStack border = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
			ItemMeta meta = border.getItemMeta();
			Component displayName = Component.text(" ")
					.decoration(TextDecoration.ITALIC, false);
			meta.displayName(displayName);
			border.setItemMeta(meta);
			inventory.setItem(i, border);
		}
	}

	private void setSellButton() {
		ItemStack sellButton = new ItemStack(Material.GOLD_INGOT);
		ItemMeta meta = sellButton.getItemMeta();
		Component displayName = Component.text("> ")
				.decoration(TextDecoration.ITALIC, false)
				.color(NamedTextColor.GOLD);
		meta.displayName(displayName);
		sellButton.setItemMeta(meta);
		inventory.setItem(saleSlot, sellButton);
	}

	public void updateSellButton(double total) {
		ItemStack button = inventory.getItem(saleSlot);
		if (button == null || button.getType() == Material.AIR) {
			setSellButton();
			button = inventory.getItem(saleSlot);
		}
		if (button == null) return;

		ItemMeta meta = button.getItemMeta();
		String formattedTotal = String.format("%.2f", total);

		Component displayName = Component.text("💰 Sell All - " + formattedTotal)
				.decoration(TextDecoration.ITALIC, false)
				.color(NamedTextColor.GOLD);
		meta.displayName(displayName);

		Component loreLine = Component.text("Click to sell all items")
				.decoration(TextDecoration.ITALIC, false)
				.color(NamedTextColor.GRAY);
		meta.lore(List.of(loreLine));

		button.setItemMeta(meta);
		inventory.setItem(saleSlot, button);
	}

	@Override
	public @NonNull Inventory getInventory() {
		return inventory;
	}
}