package ru.deelter.freshFishing.hook;

import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.entity.Player;
import ru.deelter.freshFishing.stats.FishLeaderboard;

import java.util.List;

public final class FishPlaceholders {
    private static final TextColor GOLD = TextColor.color(0xFFD700);
    private static final TextColor BEIGE = TextColor.color(0xC7B9A6);

    private FishPlaceholders() {}

    public static Runnable register(FishLeaderboard leaderboard) {
        Expansion.Builder builder = Expansion.builder("freshfishing");
        for (String field : List.of("player", "cm", "rarity", "value", "place")) {
            builder.audiencePlaceholder(Player.class, field, (player, queue, context) ->
                    render(leaderboard.snapshot().byPlayer().get(player.getUniqueId()), field));
        }
        builder.globalPlaceholder("top", (queue, context) -> {
            if (!queue.hasNext()) return Tag.selfClosingInserting(Component.empty());
            int place;
            try {
                place = Integer.parseInt(queue.pop().value());
            } catch (NumberFormatException e) {
                return Tag.selfClosingInserting(Component.empty());
            }
            return render(leaderboard.snapshot().at(place), queue.hasNext() ? queue.pop().value() : "player");
        });
        Expansion expansion = builder.build();
        expansion.register();
        return expansion::unregister;
    }

    private static Tag render(FishLeaderboard.Ranked row, String field) {
        if (row == null) return Tag.selfClosingInserting(Component.empty());
        var fish = row.fish();
        return Tag.selfClosingInserting(switch (field) {
            case "player" -> Component.text(fish.playerName());
            case "cm" -> Component.text(fish.sizeText());
            case "rarity" -> MiniMessage.miniMessage().deserialize(fish.rarityDisplay());
            case "value" -> Component.text(fish.valueText());
            case "place" -> Component.text(row.place());
            case "line" -> Component.text("#" + row.place() + "  ", GOLD)
                    .append(Component.text(fish.playerName(), BEIGE))
                    .append(Component.text(" · ", BEIGE))
                    .append(Component.text(fish.sizeText() + " см", GOLD))
                    .append(Component.text(" · ", BEIGE))
                    .append(MiniMessage.miniMessage().deserialize(fish.rarityDisplay()));
            default -> Component.empty();
        });
    }
}
