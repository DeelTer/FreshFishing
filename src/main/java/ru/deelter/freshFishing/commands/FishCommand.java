package ru.deelter.freshFishing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.shop.FishSaleMenu;

import java.util.List;

public class FishCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, @NonNull String[] args) {
		if (args.length == 0) {
			sender.sendMessage(Component.text("Usage: /fish menu [player] | /fish reload", NamedTextColor.GRAY));
			return true;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("freshfishing.admin")) {
				sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
				return true;
			}
			FreshFishing.getInstance().getConfigManager().reload();
			FreshFishing.getInstance().getEventsConfig().reload();
			sender.sendMessage(Component.text("FreshFishing config reloaded.", NamedTextColor.GREEN));
			return true;
		}

		if (!args[0].equalsIgnoreCase("menu")) {
			sender.sendMessage(Component.text("Usage: /fish menu [player] | /fish reload", NamedTextColor.GRAY));
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
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
				return true;
			}
		}
		if (!sender.hasPermission("freshfishing.sell-menu")) {
			sender.sendMessage(Component.text("You don't have permission to open menu.", NamedTextColor.RED));
			return true;
		}

		target.openInventory(new FishSaleMenu().getInventory());

		if (!isSelf && !sender.equals(target)) {
			sender.sendMessage(Component.text("Opened fish shop for " + target.getName(), NamedTextColor.GREEN));
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			return List.of("menu", "reload").stream()
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.toList();
		}
		return List.of();
	}
}