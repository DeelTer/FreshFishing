package ru.deelter.freshFishing;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.deelter.freshFishing.listeners.EntityFishListener;
import ru.deelter.freshFishing.listeners.PlayerRestrictsListener;
import ru.deelter.freshFishing.listeners.UniqueFishParamsListener;

public final class FreshFishing extends JavaPlugin {

    @Getter
    private static FreshFishing instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        new MetricsManager(this, 31328);

        new UniqueFishParamsListener(this);
        new PlayerRestrictsListener(this);
        new EntityFishListener(this);

        getLogger().info("FreshFishing enabled with bStats metrics (ID: 31328)");
    }

    @Override
    public void onDisable() {
        getLogger().info("FreshFishing disabled");
    }
}