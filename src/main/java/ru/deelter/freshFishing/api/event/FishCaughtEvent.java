package ru.deelter.freshFishing.api.event;

import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.data.FishRarity;

/**
 * Fired after a fish entity is pulled out by a player and got its size, rarity and bait bonus.
 * Changing {@link #setSize(double)} resizes the fish; the result is capped at the largest configured size.
 */
public final class FishCaughtEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Fish fish;
    private final FishRarity rarity;
    private double size;

    public FishCaughtEvent(@NotNull Player player, @NotNull Fish fish, @NotNull FishRarity rarity, double size) {
        super(player);
        this.fish = fish;
        this.rarity = rarity;
        this.size = size;
    }

    public @NotNull Fish getFish() {
        return fish;
    }

    public @NotNull FishRarity getRarity() {
        return rarity;
    }

    /** Size in centimeters. */
    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
