package ru.deelter.freshFishing.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.utils.FishUtil;

import java.util.Set;

public class PlayerRestrictsListener implements Listener {

    private final Set<Biome> onlyBiomes;
    private final Component onlyBiomesWarning;
    private final int hungerPointsWhenFish;
    private final int hungerPointsWhenItem;
    private final int minHungerLevel;

    public PlayerRestrictsListener(@NotNull FreshFishing plugin) {
        FreshFishingConfig cfg = plugin.getConfigManager();
        this.onlyBiomes = cfg.getAllowedBiomes();
        this.hungerPointsWhenFish = cfg.getHungerFishCost();
        this.hungerPointsWhenItem = cfg.getHungerItemCost();
        this.minHungerLevel = cfg.getMinHungerLevel();

        String warningString = cfg.getBiomeWarningMessage();
        if (warningString != null && !warningString.isBlank()) {
            onlyBiomesWarning = MiniMessage.miniMessage().deserialize(warningString);
        } else {
            onlyBiomesWarning = null;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFish(@NotNull PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caughtItem)) return;

        Player player = event.getPlayer();
        FishHook hook = event.getHook();

        if (!onlyBiomes.isEmpty()) {
            Biome biome = hook.getLocation().getBlock().getBiome();
            if (!onlyBiomes.contains(biome)) {
                if (onlyBiomesWarning != null) {
                    player.sendActionBar(onlyBiomesWarning);
                }
                event.setCancelled(true);
                return;
            }
        }

        int hungerPoints = FishUtil.isFish(caughtItem.getItemStack()) ? hungerPointsWhenFish : hungerPointsWhenItem;
        int playerFoodLevel = player.getFoodLevel();

        if (playerFoodLevel < minHungerLevel) {
            player.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f);
            event.setCancelled(true);
            return;
        }

        float saturation = player.getSaturation();
        if (saturation >= hungerPoints) {
            player.setSaturation(saturation - hungerPoints);
        } else {
            player.setSaturation(0f);
            player.setFoodLevel(playerFoodLevel - (hungerPoints - (int) saturation));
        }
    }
}