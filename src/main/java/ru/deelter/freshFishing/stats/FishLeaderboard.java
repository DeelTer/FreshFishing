package ru.deelter.freshFishing.stats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import ru.deelter.freshFishing.FreshFishing;
import ru.deelter.freshFishing.hook.ScreenEffects;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class FishLeaderboard implements AutoCloseable {
    private static final String TABLE = "freshfishing_records";
    private static final String CREATE = """
            CREATE TABLE IF NOT EXISTS freshfishing_records (
                player_id VARCHAR(36) PRIMARY KEY, player_name VARCHAR(64) NOT NULL,
                centimeters DOUBLE NOT NULL, rarity VARCHAR(255) NOT NULL,
                rarity_display TEXT NOT NULL, score DOUBLE NOT NULL, caught_at BIGINT NOT NULL
            )
            """;
    private static final String INSERT = """
            INSERT INTO freshfishing_records
            (player_id, player_name, centimeters, rarity, rarity_display, score, caught_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String SQLITE_UPSERT = INSERT + """
            ON CONFLICT(player_id) DO UPDATE SET player_name = excluded.player_name,
                centimeters = excluded.centimeters, rarity = excluded.rarity,
                rarity_display = excluded.rarity_display, caught_at = excluded.caught_at, score = excluded.score
            WHERE excluded.score > freshfishing_records.score
            """;
    // MySQL evaluates these assignments from left to right, so score must be assigned last.
    private static final String MYSQL_UPSERT = INSERT + """
            AS incoming ON DUPLICATE KEY UPDATE
                player_name = IF(incoming.score > freshfishing_records.score, incoming.player_name, freshfishing_records.player_name),
                centimeters = IF(incoming.score > freshfishing_records.score, incoming.centimeters, freshfishing_records.centimeters),
                rarity = IF(incoming.score > freshfishing_records.score, incoming.rarity, freshfishing_records.rarity),
                rarity_display = IF(incoming.score > freshfishing_records.score, incoming.rarity_display, freshfishing_records.rarity_display),
                caught_at = IF(incoming.score > freshfishing_records.score, incoming.caught_at, freshfishing_records.caught_at),
                score = GREATEST(freshfishing_records.score, incoming.score)
            """;

    private final FreshFishing plugin;
    private final boolean mysql;
    private final String url;
    private final Properties properties = new Properties();
    private final java.util.concurrent.ScheduledExecutorService worker;
    private boolean initialized;
    private boolean storageFailed;
    private volatile Snapshot snapshot = Snapshot.of(List.of());
    private volatile boolean available;
    private volatile long round;

    public FishLeaderboard(FreshFishing plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        String type = config.getString("leaderboard.storage.type", "sqlite").toLowerCase(Locale.ROOT);
        if (!type.equals("sqlite") && !type.equals("mysql")) {
            throw new IllegalArgumentException("leaderboard.storage.type must be sqlite or mysql");
        }
        mysql = type.equals("mysql");
        if (mysql) {
            String path = "leaderboard.storage.mysql.";
            url = "jdbc:mysql://" + config.getString(path + "host", "localhost")
                    + ":" + config.getInt(path + "port", 3306)
                    + "/" + config.getString(path + "database", "minecraft");
            properties.setProperty("user", config.getString(path + "user", "minecraft"));
            properties.setProperty("password", config.getString(path + "password", ""));
            properties.setProperty("sslMode", config.getString(path + "ssl-mode", "PREFERRED"));
            properties.setProperty("connectTimeout", "3000");
            properties.setProperty("socketTimeout", "5000");
            properties.setProperty("useAffectedRows", "true");
        } else {
            url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "fishing-leaderboard.db").getAbsolutePath();
            properties.setProperty("busy_timeout", "3000");
        }
        worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "FreshFishing leaderboard");
            thread.setDaemon(true);
            return thread;
        });
        worker.scheduleWithFixedDelay(this::refresh, 0, 15, TimeUnit.SECONDS);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public boolean available() {
        return available;
    }

    public void record(FishRecord fish) {
        worker.execute(() -> {
            try (Connection connection = open()) {
                Snapshot before = mysql ? read(connection) : snapshot;
                Ranked previous = before.byPlayer().get(fish.playerId());
                if (previous != null && fish.value() <= previous.fish().value()) return;
                try (PreparedStatement statement = connection.prepareStatement(mysql ? MYSQL_UPSERT : SQLITE_UPSERT)) {
                    statement.setString(1, fish.playerId().toString());
                    statement.setString(2, fish.playerName());
                    statement.setDouble(3, fish.centimeters());
                    statement.setString(4, fish.rarity());
                    statement.setString(5, fish.rarityDisplay());
                    statement.setDouble(6, fish.value());
                    statement.setLong(7, fish.caughtAt());
                    if (statement.executeUpdate() == 0) return;
                }
                Snapshot after = read(connection);
                publish(after);
                Ranked current = after.byPlayer().get(fish.playerId());
                if (current == null || current.place() > 5 || !current.fish().equals(fish)) return;
                Ranked passed = before.at(current.place());
                if (passed == null || passed.fish().playerId().equals(fish.playerId())
                        || fish.value() <= passed.fish().value()
                        || (previous != null && current.place() >= previous.place())) return;
                long catchRound = round;
                onMainThread(() -> {
                    if (catchRound != round) return;
                    if (!current.equals(snapshot.byPlayer().get(fish.playerId()))) return;
                    var player = Bukkit.getPlayer(fish.playerId());
                    if (player != null) {
                        ScreenEffects.showOvertake(player, Component.text("#" + current.place() + "  ·  "
                                + fish.sizeText() + " cm  ·  Passed " + passed.fish().playerName(), NamedTextColor.GOLD));
                    }
                });
            } catch (SQLException e) {
                report(e);
            }
        });
    }

    public void reset(CommandSender sender) {
        worker.execute(() -> {
            try (Connection connection = open(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + TABLE);
                round++;
                publish(Snapshot.of(List.of()));
                onMainThread(() -> sender.sendMessage(Component.text("Fishing leaderboard reset.", NamedTextColor.GREEN)));
            } catch (SQLException e) {
                report(e);
                onMainThread(() -> sender.sendMessage(Component.text("Could not reset the fishing leaderboard.", NamedTextColor.RED)));
            }
        });
    }

    private Connection open() throws SQLException {
        if (!initialized) {
            try {
                Class.forName(mysql ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Paper JDBC driver is missing", e);
            }
        }
        Connection connection = DriverManager.getConnection(url, properties);
        if (!initialized) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE);
                initialized = true;
            } catch (SQLException e) {
                connection.close();
                throw e;
            }
        }
        return connection;
    }

    private void refresh() {
        try (Connection connection = open()) {
            publish(read(connection));
        } catch (SQLException e) {
            report(e);
        }
    }

    private Snapshot read(Connection connection) throws SQLException {
        List<FishRecord> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM " + TABLE
                     + " ORDER BY score DESC, caught_at ASC, player_id ASC")) {
            while (result.next()) {
                rows.add(new FishRecord(UUID.fromString(result.getString("player_id")), result.getString("player_name"),
                        result.getDouble("centimeters"), result.getString("rarity"), result.getString("rarity_display"),
                        result.getDouble("score"), result.getLong("caught_at")));
            }
        }
        return Snapshot.of(rows);
    }

    private void publish(Snapshot updated) {
        snapshot = updated;
        available = true;
        storageFailed = false;
    }

    private void report(SQLException failure) {
        if (!storageFailed) plugin.getLogger().log(Level.WARNING, "Fishing leaderboard database operation failed", failure);
        storageFailed = true;
    }

    private void onMainThread(Runnable action) {
        if (!plugin.isEnabled()) return;
        try {
            Bukkit.getScheduler().runTask(plugin, action);
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
            // Shutdown can start between the enabled check and scheduler submission.
        }
    }

    @Override
    public void close() {
        worker.shutdown();
        try {
            if (worker.awaitTermination(10, TimeUnit.SECONDS)) return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worker.shutdownNow();
        plugin.getLogger().warning("Timed out flushing fishing leaderboard operations.");
    }

    public record Ranked(int place, FishRecord fish) {}

    public record Snapshot(List<Ranked> rows, Map<UUID, Ranked> byPlayer) {
        private static Snapshot of(List<FishRecord> records) {
            List<Ranked> rows = new ArrayList<>(records.size());
            Map<UUID, Ranked> byPlayer = new HashMap<>();
            for (FishRecord fish : records) {
                Ranked entry = new Ranked(rows.size() + 1, fish);
                rows.add(entry);
                byPlayer.put(fish.playerId(), entry);
            }
            return new Snapshot(List.copyOf(rows), Map.copyOf(byPlayer));
        }

        public Ranked at(int place) {
            return place > 0 && place <= rows.size() ? rows.get(place - 1) : null;
        }
    }
}
