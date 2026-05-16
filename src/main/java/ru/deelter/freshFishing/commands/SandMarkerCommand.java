package ru.deelter.freshFishing.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.freshFishing.sandloot.SandLootManager;

import java.util.ArrayList;
import java.util.List;

public class SandMarkerCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Only players can use this.");
			return true;
		}
		if (!player.hasPermission("freshfishing.admin")) {
			player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
			return true;
		}
		if (args.length == 0) {
			player.sendMessage(Component.text("Usage: /sandmarker <create|list|remove|tp>", NamedTextColor.GRAY));
			return true;
		}

		SandLootManager manager = SandLootManager.get();
		if (manager == null) {
			player.sendMessage(Component.text("Sand loot feature is disabled.", NamedTextColor.RED));
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "create":
				Block standingBlock = player.getLocation().getBlock();
				Location markerLoc = standingBlock.getLocation().add(0.5, 0, 0.5);
				manager.addMarker(markerLoc);
				player.sendMessage(Component.text("Marker created at "
								+ standingBlock.getX() + ", " + standingBlock.getY() + ", " + standingBlock.getZ(),
						NamedTextColor.GREEN));
				break;

			case "remove":
				boolean removed = manager.removeMarker(player.getLocation());
				if (removed) {
					player.sendMessage(Component.text("Marker removed.", NamedTextColor.GREEN));
				} else {
					player.sendMessage(Component.text("No marker found nearby.", NamedTextColor.RED));
				}
				break;

			case "list":
				var markers = manager.getMarkers();
				if (markers.isEmpty()) {
					player.sendMessage(Component.text("No markers.", NamedTextColor.GRAY));
					return true;
				}
				player.sendMessage(Component.text("Markers:", NamedTextColor.GOLD));
				int index = 1;
				for (var marker : markers) {
					Location loc = marker.getLocation();
					int x = loc.getBlockX();
					int y = loc.getBlockY();
					int z = loc.getBlockZ();

					Component teleportBtn = Component.text(" [TP]")
							.color(NamedTextColor.GREEN)
							.clickEvent(ClickEvent.runCommand("/sandmarker tp " + x + " " + y + " " + z))
							.hoverEvent(HoverEvent.showText(Component.text("Click to teleport to this marker")));

					Component removeBtn = Component.text(" [X]")
							.color(NamedTextColor.RED)
							.clickEvent(ClickEvent.runCommand("/sandmarker remove"))
							.hoverEvent(HoverEvent.showText(Component.text("Click to remove nearest marker")));

					Component line = Component.text(index++ + ". " + x + ", " + y + ", " + z)
							.color(NamedTextColor.YELLOW)
							.append(teleportBtn)
							.append(removeBtn);
					player.sendMessage(line);
				}
				break;

			case "tp":
				if (args.length < 4) {
					player.sendMessage(Component.text("Usage: /sandmarker tp <x> <y> <z>", NamedTextColor.GRAY));
					return true;
				}
				try {
					int x = Integer.parseInt(args[1]);
					int y = Integer.parseInt(args[2]);
					int z = Integer.parseInt(args[3]);
					Location target = new Location(player.getWorld(), x + 0.5, y + 1, z + 0.5);
					player.teleport(target);
					player.sendMessage(Component.text("Teleported to marker at " + x + ", " + y + ", " + z, NamedTextColor.GREEN));
				} catch (NumberFormatException e) {
					player.sendMessage(Component.text("Invalid coordinates!", NamedTextColor.RED));
				}
				break;

			default:
				player.sendMessage(Component.text("Unknown subcommand. Available: create, list, remove, tp", NamedTextColor.RED));
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (!(sender instanceof Player) || !sender.hasPermission("freshfishing.admin")) {
			return new ArrayList<>();
		}

		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			completions.add("create");
			completions.add("list");
			completions.add("remove");
			completions.add("tp");
			return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
		}

		if (args.length == 4 && args[0].equalsIgnoreCase("tp")) {
			completions.add("0");
		}

		return completions;
	}
}