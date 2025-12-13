package ru.deelter.vr.freshFishing;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ru.deelter.vr.freshFishing.listeners.EntityFishListener;
import ru.deelter.vr.freshFishing.listeners.PlayerRestrictsListener;
import ru.deelter.vr.freshFishing.listeners.UniqueFishParamsListener;

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

		new UniqueFishParamsListener(this);
		new PlayerRestrictsListener(this);
		new EntityFishListener(this);

	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
	}
}
