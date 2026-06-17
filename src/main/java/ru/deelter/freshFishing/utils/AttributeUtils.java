package ru.deelter.freshFishing.utils;


import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AttributeUtils {

	public static final double DEFAULT_SCALE = 1.0;

	public static void resetScale(@NotNull Attributable attributable) {
		AttributeInstance scale = attributable.getAttribute(Attribute.SCALE);

		if (scale != null) {
			scale.setBaseValue(DEFAULT_SCALE);
		}
	}

	public static void setScale(Attributable attributable, double scaleValue) {
		if (scaleValue == DEFAULT_SCALE) {
			resetScale(attributable);
			return;
		}
		AttributeInstance scale = attributable.getAttribute(Attribute.SCALE);
		if (scale == null) return;

		scale.setBaseValue(scaleValue);
	}

	public static void setMaxHealth(@NonNull LivingEntity fish, double healthValue) {
		AttributeInstance attribute = fish.getAttribute(Attribute.MAX_HEALTH);
		if (attribute == null) return;
		if (attribute.getBaseValue() > healthValue) {
			healthValue = attribute.getBaseValue();
		}
		attribute.setBaseValue(healthValue);
		fish.setHealth(healthValue);
	}

	public static void applyFishAttributes(@NotNull Fish fish, double scale, double health) {
		setScale(fish, scale);
		setMaxHealth(fish, health);
	}
}
