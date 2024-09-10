package org.dark.customjoinmessage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customjoinmessage.Commands.CJMCommand;
import org.dark.customjoinmessage.Listeners.PlayerChatListener;
import org.dark.customjoinmessage.Listeners.PlayerEventListener;
import org.dark.customjoinmessage.Utilities.DatabaseManager;
import org.dark.customjoinmessage.Utilities.FileManager;
import org.dark.customjoinmessage.Utilities.LocalStorageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomJoinMessage extends JavaPlugin {

    private DatabaseManager databaseManager;
    private LocalStorageManager localStorageManager;
    private FileManager fileManager;
    private PlayerChatListener playerChatListener;
    private boolean placeholderAPIEnabled = false;
    private boolean usingLocalStorage = false;

    private final Map<UUID, Boolean> playerConfiguring = new HashMap<>();
    private final Map<UUID, Boolean> configuringJoinMessage = new HashMap<>();
    private final Map<UUID, String> playerJoinMessages = new HashMap<>();
    private final Map<UUID, String> playerQuitMessages = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);

        if (getConfig().getBoolean("mysql.enabled", false)) {
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
            databaseManager.loadMessagesFromDatabase(playerJoinMessages, playerQuitMessages);
        } else {
            usingLocalStorage = true;
            localStorageManager = new LocalStorageManager(this);
        }

        playerChatListener = new PlayerChatListener(this);
        registerCommandsAndListeners();
        checkForPlaceholderAPI();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        if (localStorageManager != null) {
            localStorageManager.saveDataToFile(playerJoinMessages, playerQuitMessages);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        fileManager.reloadConfig();
        if (playerChatListener != null) {
            playerChatListener.updateBlockedPatterns();
        }
    }

    private void registerCommandsAndListeners() {
        CJMCommand cjmCommand = new CJMCommand(this);
        this.getCommand("cjm").setExecutor(cjmCommand);
        this.getCommand("cjm").setTabCompleter(cjmCommand);

        getServer().getPluginManager().registerEvents(playerChatListener, this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
    }

    private void checkForPlaceholderAPI() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        placeholderAPIEnabled = placeholderAPI != null;
        getLogger().info(placeholderAPIEnabled ? "PlaceholderAPI found and enabled." : "PlaceholderAPI not found.");
    }

    public Map<UUID, String> getPlayerJoinMessages() {
        return playerJoinMessages;
    }

    public Map<UUID, String> getPlayerQuitMessages() {
        return playerQuitMessages;
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
        if (usingLocalStorage) {
            localStorageManager.saveDataToFile(playerJoinMessages, playerQuitMessages);
        } else {
            databaseManager.saveMessageToDatabase(playerId, playerJoinMessages.get(playerId), playerQuitMessages.get(playerId));
        }
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
