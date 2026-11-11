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
import ru.deelter.freshFishing.stats.FishLeaderboard;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public class FishCommand implements CommandExecutor, TabCompleter {

	private static final String USAGE = "Usage: /fish menu [player] | /fish top [5|10|15|reset] | /fish reload";

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, @NonNull String[] args) {
		if (args.length == 0) {
			sender.sendMessage(Component.text(USAGE, NamedTextColor.GRAY));
			return true;
		}

		if (args[0].equalsIgnoreCase("top")) {
			showTop(sender, args);
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
			sender.sendMessage(Component.text(USAGE, NamedTextColor.GRAY));
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

	private void showTop(CommandSender sender, String[] args) {
		String option = args.length == 1 ? "10" : args[1].toLowerCase(java.util.Locale.ROOT);
		if (args.length > 2 || !List.of("5", "10", "15", "reset").contains(option)) {
			sender.sendMessage(Component.text("Usage: /fish top [5|10|15|reset]", NamedTextColor.GRAY));
			return;
		}
		boolean reset = option.equals("reset");
		if (reset && !sender.hasPermission("freshfishing.admin")) {
			sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
			return;
		}
		FishLeaderboard leaderboard = FreshFishing.getInstance().getLeaderboard();
		if (leaderboard == null || (!reset && !leaderboard.available())) {
			sender.sendMessage(Component.text("Fishing leaderboard is unavailable.", NamedTextColor.RED));
			return;
		}
		if (reset) {
			leaderboard.reset(sender);
			return;
		}
		var rows = leaderboard.snapshot().rows();
		if (rows.isEmpty()) {
			sender.sendMessage(Component.text("No fish records yet.", NamedTextColor.GRAY));
			return;
		}
		int limit = Integer.parseInt(option);
		sender.sendMessage(Component.text("Fishing leaderboard", NamedTextColor.GOLD));
		for (var row : rows.subList(0, Math.min(limit, rows.size()))) {
			var fish = row.fish();
			sender.sendMessage(Component.text("#" + row.place() + " " + fish.playerName() + " · "
					+ fish.sizeText() + " cm · ", NamedTextColor.GRAY)
					.append(MiniMessage.miniMessage().deserialize(fish.rarityDisplay()))
					.append(Component.text(" · " + fish.scoreText() + " pts", NamedTextColor.YELLOW)));
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1) {
			return List.of("menu", "reload", "top").stream()
					.filter(s -> s.startsWith(args[0].toLowerCase()))
					.toList();
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
			return List.of("5", "10", "15", "reset").stream()
					.filter(s -> !s.equals("reset") || sender.hasPermission("freshfishing.admin"))
					.filter(s -> s.startsWith(args[1].toLowerCase(java.util.Locale.ROOT)))
					.toList();
		}
		return List.of();
	}
}