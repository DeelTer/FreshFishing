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
import org.bukkit.entity.Player;
import ru.deelter.freshFishing.sandloot.SandLootManager;

public class SandMarkerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }
        if (!player.hasPermission("freshfishing.admin")) {
            player.sendMessage(Component.text("No permission!", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /sandmarker <create|list|remove>", NamedTextColor.GRAY));
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
                for (var marker : markers) {
                    Component line = Component.text("• " + marker.getLocation().getBlockX() + ", " + marker.getLocation().getBlockZ())
                            .color(NamedTextColor.YELLOW)
                            .append(Component.text(" [X]").color(NamedTextColor.RED)
                                    .clickEvent(ClickEvent.runCommand("/sandmarker remove"))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to remove nearest marker"))));
                    player.sendMessage(line);
                }
                break;
            default:
                player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }
}