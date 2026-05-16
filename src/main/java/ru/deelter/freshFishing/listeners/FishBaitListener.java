package ru.deelter.freshFishing.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.config.FreshFishingConfig;

import java.util.HashSet;
import java.util.Set;

public class FishBaitListener implements Listener {

    private final Set<ItemStack> requiredBaits = new HashSet<>();
    private final boolean consumeOnCatch;
    private final Component noBaitMessage;

    public FishBaitListener(FreshFishing plugin) {
        FreshFishingConfig cfg = plugin.getConfigManager();
        this.consumeOnCatch = cfg.isConsumeBaitOnCatch();
        String msg = cfg.getNoBaitActionBar();
        this.noBaitMessage = (msg != null && !msg.isEmpty()) ? MiniMessage.miniMessage().deserialize(msg) : null;
        this.requiredBaits.addAll(cfg.getRequiredBaits());
        if (!requiredBaits.isEmpty()) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (requiredBaits.isEmpty()) return;

        Player player = event.getPlayer();
        if (!hasBait(player)) {
            if (noBaitMessage != null) player.sendActionBar(noBaitMessage);
            event.setCancelled(true);
            event.getHook().remove();
        }
    }

    private boolean hasBait(Player player) {
        for (ItemStack bait : requiredBaits) {
            int amount = 0;
            for (ItemStack content : player.getInventory().getContents()) {
                if (content != null && content.isSimilar(bait)) {
                    amount += content.getAmount();
                }
            }
            if (amount >= 1) {
                if (consumeOnCatch) {
                    int toRemove = 1;
                    for (int i = 0; i < player.getInventory().getSize(); i++) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (item != null && item.isSimilar(bait)) {
                            int amt = item.getAmount();
                            if (amt > toRemove) {
                                item.setAmount(amt - toRemove);
                                break;
                            } else {
                                player.getInventory().setItem(i, null);
                                toRemove -= amt;
                                if (toRemove <= 0) break;
                            }
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}