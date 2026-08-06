package ru.deelter.freshFishing;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import ru.deelter.freshFishing.commands.FishCommand;
import ru.deelter.freshFishing.commands.SandMarkerCommand;
import ru.deelter.freshFishing.config.EventsConfig;
import ru.deelter.freshFishing.config.FreshFishingConfig;
import ru.deelter.freshFishing.events.FishBossManager;
import ru.deelter.freshFishing.listeners.*;
import ru.deelter.freshFishing.sandloot.SandLootManager;

@Getter
public final class FreshFishing extends JavaPlugin {

	@Getter
	private static FreshFishing instance;
	private NamespacedKey markerKey;
	private FreshFishingConfig configManager;
	private EventsConfig eventsConfig;
	private FishBossManager fishBossManager;

	@Override
	public void onLoad() {
		instance = this;
		markerKey = new NamespacedKey(this, "sand_marker");
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		this.configManager = new FreshFishingConfig(this);
		this.eventsConfig = new EventsConfig(this);
		this.fishBossManager = new FishBossManager(this);

		new FishMetrics(this, 31328);
		new UniqueFishParamsListener(this);
		new PlayerRestrictsListener(this);
		new EntityFishListener(this);
		new GrapplingHookListener(this);
		new FishBaitListener(this);
		new FishSaleListener(this);
		new SandLootManager(this);
		// Soft: registers fishing emote triggers only when BetterEmotes is installed.
		EmoteTriggerListener.register(this);

		FishCommand fishCommand = new FishCommand();
		getCommand("fish").setExecutor(fishCommand);
		getCommand("fish").setTabCompleter(fishCommand);

		SandMarkerCommand sandMarkerCommand = new SandMarkerCommand();
		getCommand("sandmarker").setExecutor(sandMarkerCommand);
		getCommand("sandmarker").setTabCompleter(sandMarkerCommand);
	}

	@Override
	public void onDisable() {
		if (SandLootManager.get() != null) SandLootManager.get().shutdown();
		if (fishBossManager != null) fishBossManager.shutdown();
	}
}