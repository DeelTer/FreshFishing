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
	public boolean onCommand(CommandSender sender, Command command, String label, @NonNull String[] args) {
		if (args.length == 0 || !args[0].equalsIgnoreCase("menu")) {
			sender.sendMessage(Component.text("Usage: /fish menu [player]", NamedTextColor.GRAY));
			return true;
		}

		Player target;
		boolean isSelf = args.length == 1;

		if (isSelf) {
			if (!(sender instanceof Player player)) {
				sender.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
				return true;
			}
			target = player;
		} else {
			if (!sender.hasPermission("freshfishing.sell-menu")) {
				sender.sendMessage(Component.text("You don't have permission to open menu for others.", NamedTextColor.RED));
				return true;
			}
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
				return true;
			}
		}

		target.openInventory(new FishSaleMenu().getInventory());

		if (!isSelf && !sender.equals(target)) {
			sender.sendMessage(Component.text("Opened fish shop for " + target.getName(), NamedTextColor.GREEN));
		}
		return true;
	}
}