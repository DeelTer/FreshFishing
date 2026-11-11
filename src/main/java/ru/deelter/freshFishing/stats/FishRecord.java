package ru.deelter.freshFishing.stats;

import java.util.Locale;
import java.util.UUID;

public record FishRecord(UUID playerId, String playerName, double centimeters, String rarity,
                         String rarityDisplay, double value, double score, long caughtAt) {

    public String sizeText() {
        return java.math.BigDecimal.valueOf(centimeters).stripTrailingZeros().toPlainString();
    }

    public String valueText() {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public String scoreText() {
        return String.format(Locale.ROOT, "%.2f", score);
    }
}
