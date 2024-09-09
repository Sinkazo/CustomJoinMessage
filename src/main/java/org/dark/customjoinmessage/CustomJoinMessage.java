package org.dark.customjoinmessage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customjoinmessage.Commands.CJMCommand;
import org.dark.customjoinmessage.Listeners.PlayerChatListener;
import org.dark.customjoinmessage.Listeners.PlayerEventListener;
import org.dark.customjoinmessage.Utilities.FileManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomJoinMessage extends JavaPlugin {

    private Connection connection;
    private FileManager fileManager;
    private final Map<UUID, Boolean> playerConfiguring = new HashMap<>();
    private final Map<UUID, Boolean> configuringJoinMessage = new HashMap<>();
    private final Map<UUID, String> playerJoinMessages = new HashMap<>();
    private final Map<UUID, String> playerQuitMessages = new HashMap<>();
    private boolean placeholderAPIEnabled = false;
    private PlayerChatListener playerChatListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);
        connectToDatabase();
        CJMCommand cjmCommand = new CJMCommand(this);
        this.getCommand("cjm").setExecutor(cjmCommand);
        this.getCommand("cjm").setTabCompleter(cjmCommand);
        playerChatListener = new PlayerChatListener(this);
        getServer().getPluginManager().registerEvents(playerChatListener, this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);

        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI found and enabled.");
        } else {
            getLogger().info("PlaceholderAPI not found, continuing without placeholder support.");
        }
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        fileManager.reloadConfig();
        playerChatListener.updateBlockedPatterns();
    }

    public void connectToDatabase() {
        try {
            String url = "jdbc:mysql://" + fileManager.getConfig().getString("mysql.host") + ":" +
                    fileManager.getConfig().getInt("mysql.port") + "/" + fileManager.getConfig().getString("mysql.database");
            connection = DriverManager.getConnection(url, fileManager.getConfig().getString("mysql.username"),
                    fileManager.getConfig().getString("mysql.password"));
            getLogger().info("Database connected successfully.");
            createTablesIfNotExists();
            loadMessagesFromDatabase();
        } catch (SQLException e) {
            getLogger().severe("Could not establish a connection to the database. Disabling plugin.");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void createTablesIfNotExists() {
        try (Statement stmt = connection.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_messages (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "join_message TEXT DEFAULT '', " +
                    "quit_message TEXT DEFAULT '')";
            stmt.execute(createTableSQL);
            getLogger().info("Table 'player_messages' ensured.");
        } catch (SQLException e) {
            getLogger().severe("Error while creating 'player_messages' table.");
            e.printStackTrace();
        }
    }

    private void loadMessagesFromDatabase() {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid, join_message, quit_message FROM player_messages")) {
            var resultSet = stmt.executeQuery();
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

    private void saveMessageToDatabase(UUID playerId, String joinMessage, String quitMessage) {
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

    public boolean isPlayerConfiguring(UUID playerId) {
        return playerConfiguring.getOrDefault(playerId, false);
    }

    public void setPlayerConfiguring(UUID playerId, boolean configuring) {
        playerConfiguring.put(playerId, configuring);
    }

    public void setConfiguringJoinMessage(UUID playerId, boolean isJoinMessage) {
        configuringJoinMessage.put(playerId, isJoinMessage);
    }

    public boolean isConfiguringJoinMessage(UUID playerId) {
        return configuringJoinMessage.getOrDefault(playerId, true);
    }

    public void setPlayerMessage(UUID playerId, String message, boolean isJoinMessage) {
        if (isJoinMessage) {
            playerJoinMessages.put(playerId, message);
        } else {
            playerQuitMessages.put(playerId, message);
        }
        saveMessageToDatabase(playerId,
                playerJoinMessages.getOrDefault(playerId, ""),
                playerQuitMessages.getOrDefault(playerId, ""));
    }

    public String getJoinMessage(UUID playerId) {
        return playerJoinMessages.getOrDefault(playerId, fileManager.getConfig().getString("default_messages.join"));
    }

    public String getQuitMessage(UUID playerId) {
        return playerQuitMessages.getOrDefault(playerId, fileManager.getConfig().getString("default_messages.quit"));
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public FileManager getFileManager() {
        return fileManager;
    }
}