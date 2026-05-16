package ru.deelter.freshFishing;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.deelter.freshFishing.commands.FishCommand;
import ru.deelter.freshFishing.commands.SandMarkerCommand;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.listeners.*;
import ru.deelter.freshFishing.sandloot.SandLootManager;

@Getter
public final class FreshFishing extends JavaPlugin {

	@Getter
	private static FreshFishing instance;
	private NamespacedKey markerKey;
	private FreshFishingConfig configManager;

	@Override
	public void onLoad() {
		instance = this;
		markerKey = new NamespacedKey(this, "sand_marker");
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		this.configManager = new FreshFishingConfig(this);

		new FishMetrics(this, 31328);
		new UniqueFishParamsListener(this);
		new PlayerRestrictsListener(this);
		new EntityFishListener(this);
		new GrapplingHookListener(this);
		new FishBaitListener(this);
		new FishSaleListener(this);
		new SandLootManager(this);

		getCommand("fish").setExecutor(new FishCommand());
		getCommand("sandmarker").setExecutor(new SandMarkerCommand());
	}

	@Override
	public void onDisable() {
		if (SandLootManager.get() != null) SandLootManager.get().shutdown();
	}
}