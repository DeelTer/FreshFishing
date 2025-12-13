package ru.deelter.freshFishing.data;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class FishSize {

	public static final Set<FishSize> SIZES = new HashSet<>();

	private final double min, max;
	private final int blocks, lambda;

	public double getRandomSize() {
		return expRandom(lambda) * (max - min) + min;
	}

	private double expRandom(double lambda) {
		double current = new Random().nextDouble();
		return -Math.log(1 - (1 - Math.exp(-lambda)) * current) / lambda;
	}

	public double getRandomRoundedSize() {
		return Math.round(this.getRandomSize() * 100.0) / 100.0;
	}
}
