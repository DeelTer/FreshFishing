package ru.deelter.freshFishing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.shop.FishSaleMenu;

public class FishCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String @NonNull [] args) {
		// /fish menu
		if (args.length == 1 && args[0].equalsIgnoreCase("menu")) {
			if (sender instanceof Player player) {
				player.openInventory(new FishSaleMenu().getInventory());
				return true;
			} else {
				sender.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
				return true;
			}
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("menu")) {
			if (!sender.hasPermission("freshfishing.sell-menu")) {
				return true;
			}
			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
				return true;
			}
			target.openInventory(new FishSaleMenu().getInventory());

			if (!sender.equals(target)) {
				sender.sendMessage(Component.text("Opened fish shop for " + target.getName(), NamedTextColor.GREEN));
			}
			return true;
		}
		sender.sendMessage(Component.text("Usage: /fish menu [player]", NamedTextColor.GRAY));
		return true;
	}
}