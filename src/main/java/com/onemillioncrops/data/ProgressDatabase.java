package com.onemillioncrops.data;

import com.onemillioncrops.model.ProgressSnapshot;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class ProgressDatabase implements AutoCloseable {
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private final Path dataFolder;
    private final Logger logger;
    private final Path databasePath;
    private Connection connection;

    public ProgressDatabase(JavaPlugin plugin, String fileName) {
        this(plugin.getDataFolder().toPath(), plugin.getLogger(), fileName);
    }

    public ProgressDatabase(Path dataFolder, Logger logger, String fileName) {
        this.dataFolder = dataFolder.toAbsolutePath().normalize();
        this.logger = logger;
        this.databasePath = this.dataFolder.resolve(fileName).normalize();
        if (!databasePath.startsWith(this.dataFolder)) {
            throw new IllegalArgumentException("storage.database-file must remain inside the plugin folder");
        }
    }

    public synchronized void open() throws SQLException {
        try {
            Files.createDirectories(databasePath.getParent());
            Class.forName("org.sqlite.JDBC");
        } catch (IOException | ClassNotFoundException exception) {
            throw new SQLException("Unable to initialise SQLite", exception);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS crop_progress (
                        crop_id TEXT PRIMARY KEY,
                        amount INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS contributions (
                        player_uuid TEXT NOT NULL,
                        crop_id TEXT NOT NULL,
                        amount INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, crop_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS pending_celebrations (
                        player_uuid TEXT NOT NULL,
                        crop_id TEXT NOT NULL,
                        grand_finale INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, crop_id)
                    )
                    """);
        }
        ensurePendingFinaleColumn();
    }

    private void ensurePendingFinaleColumn() throws SQLException {
        boolean present = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(pending_celebrations)")) {
            while (columns.next()) {
                if ("grand_finale".equals(columns.getString("name"))) {
                    present = true;
                    break;
                }
            }
        }
        if (!present) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE pending_celebrations "
                        + "ADD COLUMN grand_finale INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    public synchronized ProgressSnapshot load() throws SQLException {
        Map<String, Long> totals = new LinkedHashMap<>();
        Map<String, Boolean> completed = new LinkedHashMap<>();
        Map<UUID, Map<String, Long>> contributions = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT crop_id, amount, completed FROM crop_progress")) {
            while (results.next()) {
                totals.put(results.getString("crop_id"), results.getLong("amount"));
                completed.put(results.getString("crop_id"), results.getBoolean("completed"));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT player_uuid, crop_id, amount FROM contributions")) {
            while (results.next()) {
                try {
                    UUID uuid = UUID.fromString(results.getString("player_uuid"));
                    contributions.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>())
                            .put(results.getString("crop_id"), results.getLong("amount"));
                } catch (IllegalArgumentException exception) {
                    logger.warning("Ignoring corrupt contribution UUID in progress.db");
                }
            }
        }
        return new ProgressSnapshot(totals, contributions, completed);
    }

    public synchronized void save(ProgressSnapshot snapshot) throws SQLException {
        transaction(() -> replaceSnapshot(snapshot));
    }

    /**
     * Replaces progress for the currently managed crops while retaining rows for disabled crops.
     * This lets a crop be switched off temporarily without losing its total or contributions.
     */
    public synchronized void save(ProgressSnapshot snapshot, Set<String> managedCropIds) throws SQLException {
        transaction(() -> replaceManagedSnapshot(snapshot, managedCropIds));
    }

    public synchronized void saveCompletion(ProgressSnapshot snapshot, String cropId, Set<UUID> players,
                                            boolean grandFinale) throws SQLException {
        transaction(() -> {
            mergeSnapshot(snapshot);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO pending_celebrations(player_uuid, crop_id, grand_finale) VALUES (?, ?, ?) "
                            + "ON CONFLICT(player_uuid, crop_id) DO UPDATE SET "
                            + "grand_finale=MAX(grand_finale, excluded.grand_finale)")) {
                for (UUID player : players) {
                    statement.setString(1, player.toString());
                    statement.setString(2, cropId);
                    statement.setBoolean(3, grandFinale);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private void mergeSnapshot(ProgressSnapshot snapshot) throws SQLException {
        try (PreparedStatement progress = connection.prepareStatement("""
                 INSERT INTO crop_progress(crop_id, amount, completed) VALUES (?, ?, ?)
                 ON CONFLICT(crop_id) DO UPDATE SET
                     amount=MAX(crop_progress.amount, excluded.amount),
                     completed=MAX(crop_progress.completed, excluded.completed)
                 """);
             PreparedStatement contribution = connection.prepareStatement("""
                 INSERT INTO contributions(player_uuid, crop_id, amount) VALUES (?, ?, ?)
                 ON CONFLICT(player_uuid, crop_id) DO UPDATE SET
                     amount=MAX(contributions.amount, excluded.amount)
                 """)) {
            for (Map.Entry<String, Long> entry : snapshot.totals().entrySet()) {
                progress.setString(1, entry.getKey());
                progress.setLong(2, entry.getValue());
                progress.setBoolean(3, snapshot.completed().getOrDefault(entry.getKey(), false));
                progress.addBatch();
            }
            progress.executeBatch();
            for (Map.Entry<UUID, Map<String, Long>> player : snapshot.contributions().entrySet()) {
                for (Map.Entry<String, Long> entry : player.getValue().entrySet()) {
                    contribution.setString(1, player.getKey().toString());
                    contribution.setString(2, entry.getKey());
                    contribution.setLong(3, entry.getValue());
                    contribution.addBatch();
                }
            }
            contribution.executeBatch();
        }
    }

    private void replaceSnapshot(ProgressSnapshot snapshot) throws SQLException {
        try (Statement clear = connection.createStatement()) {
            clear.executeUpdate("DELETE FROM crop_progress");
            clear.executeUpdate("DELETE FROM contributions");
        }
        try (PreparedStatement progress = connection.prepareStatement("""
                 INSERT INTO crop_progress(crop_id, amount, completed) VALUES (?, ?, ?)
                 """);
             PreparedStatement contribution = connection.prepareStatement("""
                 INSERT INTO contributions(player_uuid, crop_id, amount) VALUES (?, ?, ?)
                 """)) {
            for (Map.Entry<String, Long> entry : snapshot.totals().entrySet()) {
                progress.setString(1, entry.getKey());
                progress.setLong(2, entry.getValue());
                progress.setBoolean(3, snapshot.completed().getOrDefault(entry.getKey(), false));
                progress.addBatch();
            }
            progress.executeBatch();
            for (Map.Entry<UUID, Map<String, Long>> player : snapshot.contributions().entrySet()) {
                for (Map.Entry<String, Long> entry : player.getValue().entrySet()) {
                    contribution.setString(1, player.getKey().toString());
                    contribution.setString(2, entry.getKey());
                    contribution.setLong(3, entry.getValue());
                    contribution.addBatch();
                }
            }
            contribution.executeBatch();
        }
    }

    private void replaceManagedSnapshot(ProgressSnapshot snapshot, Set<String> managedCropIds) throws SQLException {
        try (PreparedStatement progress = connection.prepareStatement(
                "DELETE FROM crop_progress WHERE crop_id=?");
             PreparedStatement contributions = connection.prepareStatement(
                     "DELETE FROM contributions WHERE crop_id=?")) {
            for (String cropId : managedCropIds) {
                progress.setString(1, cropId);
                progress.addBatch();
                contributions.setString(1, cropId);
                contributions.addBatch();
            }
            progress.executeBatch();
            contributions.executeBatch();
        }
        insertSnapshot(snapshot);
    }

    private void insertSnapshot(ProgressSnapshot snapshot) throws SQLException {
        try (PreparedStatement progress = connection.prepareStatement("""
                 INSERT INTO crop_progress(crop_id, amount, completed) VALUES (?, ?, ?)
                 """);
             PreparedStatement contribution = connection.prepareStatement("""
                 INSERT INTO contributions(player_uuid, crop_id, amount) VALUES (?, ?, ?)
                 """)) {
            for (Map.Entry<String, Long> entry : snapshot.totals().entrySet()) {
                progress.setString(1, entry.getKey());
                progress.setLong(2, entry.getValue());
                progress.setBoolean(3, snapshot.completed().getOrDefault(entry.getKey(), false));
                progress.addBatch();
            }
            progress.executeBatch();
            for (Map.Entry<UUID, Map<String, Long>> player : snapshot.contributions().entrySet()) {
                for (Map.Entry<String, Long> entry : player.getValue().entrySet()) {
                    contribution.setString(1, player.getKey().toString());
                    contribution.setString(2, entry.getKey());
                    contribution.setLong(3, entry.getValue());
                    contribution.addBatch();
                }
            }
            contribution.executeBatch();
        }
    }

    private void transaction(SqlOperation operation) throws SQLException {
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            operation.run();
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    public synchronized void resetAll() throws SQLException {
        transaction(() -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM crop_progress");
                statement.executeUpdate("DELETE FROM contributions");
                statement.executeUpdate("DELETE FROM pending_celebrations");
            }
        });
    }

    public synchronized void resetCrop(String cropId) throws SQLException {
        transaction(() -> {
            try (PreparedStatement progress = connection.prepareStatement("DELETE FROM crop_progress WHERE crop_id=?");
                 PreparedStatement contributions = connection.prepareStatement("DELETE FROM contributions WHERE crop_id=?");
                 PreparedStatement pending = connection.prepareStatement("DELETE FROM pending_celebrations WHERE crop_id=?")) {
                for (PreparedStatement statement : new PreparedStatement[]{progress, contributions, pending}) {
                    statement.setString(1, cropId);
                    statement.executeUpdate();
                }
            }
        });
    }

    public synchronized List<PendingCelebration> pendingCelebrations(UUID player) throws SQLException {
        List<PendingCelebration> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT crop_id, grand_finale FROM pending_celebrations WHERE player_uuid=? ORDER BY rowid")) {
            statement.setString(1, player.toString());
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    result.add(new PendingCelebration(results.getString("crop_id"),
                            results.getBoolean("grand_finale")));
                }
            }
        }
        return result;
    }

    public synchronized void clearPending(UUID player, String cropId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM pending_celebrations WHERE player_uuid=? AND crop_id=?")) {
            statement.setString(1, player.toString());
            statement.setString(2, cropId);
            statement.executeUpdate();
        }
    }

    public synchronized Path backup() throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
        }
        Path backupDirectory = dataFolder.resolve("backups");
        Files.createDirectories(backupDirectory);
        Path destination = backupDirectory.resolve("progress-" + BACKUP_TIME.format(LocalDateTime.now()) + ".db");
        return Files.copy(databasePath, destination, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public Path path() {
        return databasePath;
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @FunctionalInterface
    private interface SqlOperation {
        void run() throws SQLException;
    }

    public record PendingCelebration(String cropId, boolean grandFinale) {
    }
}
