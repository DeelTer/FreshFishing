package ru.deelter.freshFishing.utils;


import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.jetbrains.annotations.NotNull;

public class ScaleUtils {

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
}
