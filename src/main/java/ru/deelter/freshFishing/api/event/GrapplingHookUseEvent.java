package ru.deelter.freshFishing.api.event;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired before FreshFishing applies its grapple to a block or an entity. */
public final class GrapplingHookUseEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final FishHook hook;
    private boolean cancelled;

    public GrapplingHookUseEvent(@NotNull Player player, @NotNull FishHook hook) {
        super(player);
        this.hook = hook;
    }

    public @NotNull FishHook getHook() {
        return hook;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
