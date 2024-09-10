package org.dark.customjoinmessage.Utilities;

import org.bukkit.Bukkit;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    private Connection connection;
    private final CustomJoinMessage plugin;

    public DatabaseManager(CustomJoinMessage plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            String url = "jdbc:mysql://" + plugin.getFileManager().getConfig().getString("mysql.host") + ":" +
                    plugin.getFileManager().getConfig().getInt("mysql.port") + "/" + plugin.getFileManager().getConfig().getString("mysql.database");
            connection = DriverManager.getConnection(url, plugin.getFileManager().getConfig().getString("mysql.username"),
                    plugin.getFileManager().getConfig().getString("mysql.password"));
            plugin.getLogger().info("Database connected successfully.");
            createTablesIfNotExists();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not establish a connection to the database. Using local storage instead.");
            e.printStackTrace();
        }
    }

    private void createTablesIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_messages (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "join_message TEXT DEFAULT '', " +
                    "quit_message TEXT DEFAULT '')";
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Table 'player_messages' ensured.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error while creating 'player_messages' table.");
            e.printStackTrace();
        }
    }

    public void loadMessagesFromDatabase(Map<UUID, String> playerJoinMessages, Map<UUID, String> playerQuitMessages) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, join_message, quit_message FROM player_messages")) {
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                UUID playerId = UUID.fromString(resultSet.getString("uuid"));
                String joinMessage = resultSet.getString("join_message");
                String quitMessage = resultSet.getString("quit_message");

                playerJoinMessages.put(playerId, joinMessage != null ? joinMessage : "");
                playerQuitMessages.put(playerId, quitMessage != null ? quitMessage : "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveMessageToDatabase(UUID playerId, String joinMessage, String quitMessage) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO player_messages (uuid, join_message, quit_message) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE join_message = VALUES(join_message), quit_message = VALUES(quit_message)")) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, joinMessage);
            stmt.setString(3, quitMessage);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
