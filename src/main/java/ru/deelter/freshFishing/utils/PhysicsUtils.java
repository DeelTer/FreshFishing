package ru.deelter.freshFishing.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import ru.deelter.freshFishing.FreshFishing;

public class PhysicsUtils {

	/**
	 * Притягивает сущность к месту с баллистикой (как в старом LocationUtils)
	 *
	 * @param entity         Сущность
	 * @param target         Целевая позиция
	 * @param multiplier     Множитель скорости (аналог multiply в старом методе)
	 * @param upwardBoost    Вертикальный подброс в начале (Y)
	 * @param delayTicks     Задержка перед основным расчётом (тики)
	 * @param dragHorizontal Горизонтальное сопротивление (0.07 в старом)
	 * @param dragVertical   Вертикальное сопротивление (0.03 в старом)
	 */
	public static void pullEntityToLocation(@NonNull Entity entity, Location target, double multiplier,
	                                        double upwardBoost, int delayTicks,
	                                        double dragHorizontal, double dragVertical) {
		Location entityLoc = entity.getLocation().clone();
		Vector boost = entity.getVelocity().clone();
		boost.setY(upwardBoost);
		entity.setVelocity(boost);

		Bukkit.getScheduler().runTaskLater(FreshFishing.getInstance(), () -> {
			double g = -0.08;
			if (target.getWorld() == null || !target.getWorld().equals(entityLoc.getWorld())) return;
			double d = target.distance(entityLoc);

			if (d < 0.1) {
				entity.setVelocity(new Vector(0, 0, 0));
				return;
			}

			double t = d;

			double v_x = (1.0 + dragHorizontal * t) * (target.getX() - entityLoc.getX()) / t;
			double v_y = (1.0 + dragVertical * t) * (target.getY() - entityLoc.getY()) / t - 0.5 * g * t;
			double v_z = (1.0 + dragHorizontal * t) * (target.getZ() - entityLoc.getZ()) / t;

			Vector vel = new Vector(v_x, v_y, v_z).multiply(multiplier);
			entity.setVelocity(vel);
		}, delayTicks);
	}
}