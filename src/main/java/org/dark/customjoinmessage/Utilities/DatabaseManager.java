package org.dark.customjoinmessage.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private Connection connection;
    private final CustomJoinMessage plugin;
    private final boolean useMySQL;

    public DatabaseManager(CustomJoinMessage plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getFileManager().getConfig().getBoolean("mysql.enabled", false);
    }

    public void connect() {
        if (useMySQL) {
            connectToMySQL();
        } else {
            connectToSQLite();
        }
        createTablesIfNotExists();
    }

    private void connectToMySQL() {
        try {
            String host = plugin.getFileManager().getConfig().getString("mysql.host");
            int port = plugin.getFileManager().getConfig().getInt("mysql.port");
            String database = plugin.getFileManager().getConfig().getString("mysql.database");
            String username = plugin.getFileManager().getConfig().getString("mysql.username");
            String password = plugin.getFileManager().getConfig().getString("mysql.password");

            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false", host, port, database);
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Connection successful to MySQL.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Cannot connect to MySQL. Using SQLite.");
            e.printStackTrace();
            connectToSQLite();
        }
    }

    private void connectToSQLite() {
        try {
            File databaseFile = new File(plugin.getDataFolder(), "data.db");
            if (!databaseFile.exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Connection successful to SQLite.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error to connect to the SQLite:");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            plugin.getLogger().severe("Connection to database is not inicializate.");
            return null;
        }
        return connection;
    }

    private void createTablesIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_messages (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "join_message TEXT DEFAULT '', " +
                    "quit_message TEXT DEFAULT '')";
            stmt.execute(createTableSQL);

            try {
                DatabaseMetaData md = connection.getMetaData();
                ResultSet rs = md.getColumns(null, null, "player_messages", "player_name");
                if (!rs.next()) {
                    String alterTableSQL = useMySQL ?
                            "ALTER TABLE player_messages ADD COLUMN player_name VARCHAR(16)" :
                            "ALTER TABLE player_messages ADD COLUMN player_name TEXT";
                    stmt.execute(alterTableSQL);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error adding/load the column player_name: " + e.getMessage());
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading tables:");
            e.printStackTrace();
        }
    }
    public void loadMessagesFromDatabase(Map<UUID, String> joinMessages, Map<UUID, String> quitMessages) {
        String query = "SELECT uuid, join_message, quit_message FROM player_messages";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String joinMessage = rs.getString("join_message");
                    String quitMessage = rs.getString("quit_message");

                    if (joinMessage != null && !joinMessage.isEmpty()) {
                        joinMessages.put(uuid, joinMessage);
                    }
                    if (quitMessage != null && !quitMessage.isEmpty()) {
                        quitMessages.put(uuid, quitMessage);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in the database: " + e.getMessage());
                }
            }

            plugin.getLogger().info("Loaded " + joinMessages.size() + " join messages and " +
                    quitMessages.size() + " exit messages.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading messages from database:");
            e.printStackTrace();
        }
    }

    public UUID getPlayerUUIDByName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        String query = "SELECT uuid FROM player_messages WHERE LOWER(player_name) = LOWER(?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String uuidStr = rs.getString("uuid");
                return UUID.fromString(uuidStr);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error searching the UUID in the database: " + e.getMessage());
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return player.getUniqueId();
        }

        @SuppressWarnings("deprecation")
        UUID offlineUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        return offlineUUID;
    }

    public void saveMessageToDatabase(UUID playerId, String joinMessage, String quitMessage) {
        if (playerId == null) {
            plugin.getLogger().severe("Trying to save message from a UUID null");
            return;
        }

        try {
            String playerName = null;
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                playerName = player.getName();
            } else {
                playerName = Bukkit.getOfflinePlayer(playerId).getName();
            }

            String sql;
            if (useMySQL) {
                sql = "INSERT INTO player_messages (uuid, player_name, join_message, quit_message) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "player_name = VALUES(player_name), " +
                        "join_message = VALUES(join_message), " +
                        "quit_message = VALUES(quit_message)";
            } else {
                sql = "INSERT OR REPLACE INTO player_messages (uuid, player_name, join_message, quit_message) " +
                        "VALUES (?, ?, ?, ?)";
            }

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, joinMessage != null ? joinMessage : "");
                stmt.setString(4, quitMessage != null ? quitMessage : "");
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving messages to UUID " + playerId);
            e.printStackTrace();
        }
    }

    public String getJoinMessage(UUID playerId) {
        String query = "SELECT join_message FROM player_messages WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("join_message");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting input message for UUID " + playerId);
            e.printStackTrace();
        }
        return null;
    }

    public String getQuitMessage(UUID playerId) {
        String query = "SELECT quit_message FROM player_messages WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("quit_message");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting output message for UUID " + playerId);
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Connection to the database successfully closed.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Error when closing the connection to the database:");
                e.printStackTrace();
            }
        }
    }
}
