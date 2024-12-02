package org.dark.customjoinmessage;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customjoinmessage.Commands.ACJMCommand;
import org.dark.customjoinmessage.Commands.CJMCommand;
import org.dark.customjoinmessage.Utilities.MessagesGUI;
import org.dark.customjoinmessage.Listeners.PlayerChatListener;
import org.dark.customjoinmessage.Listeners.PlayerEventListener;
import org.dark.customjoinmessage.Utilities.DatabaseManager;
import org.dark.customjoinmessage.Utilities.FileManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomJoinMessage extends JavaPlugin {

    private DatabaseManager databaseManager;
    private FileManager fileManager;
    private PlayerChatListener playerChatListener;
    private MessagesGUI messagesGUI;
    private boolean placeholderAPIEnabled = false;

    private final Map<UUID, Boolean> playerConfiguring = new HashMap<>();
    private final Map<UUID, Boolean> configuringJoinMessage = new HashMap<>();
    private final Map<UUID, String> playerJoinMessages = new HashMap<>();
    private final Map<UUID, String> playerQuitMessages = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        fileManager = new FileManager(this);

        // Inicializar DatabaseManager
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();
        databaseManager.loadMessagesFromDatabase(playerJoinMessages, playerQuitMessages);

        // Inicializar MessagesGUI
        messagesGUI = new MessagesGUI(this);

        playerChatListener = new PlayerChatListener(this);
        registerCommandsAndListeners();
        checkForPlaceholderAPI();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            for (UUID playerId : playerJoinMessages.keySet()) {
                databaseManager.saveMessageToDatabase(
                        playerId,
                        playerJoinMessages.get(playerId),
                        playerQuitMessages.get(playerId)
                );
            }
            databaseManager.closeConnection();
        }

        // Cerrar todos los inventarios abiertos
        if (messagesGUI != null) {
            messagesGUI.closeAll();
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

        ACJMCommand acjmCommand = new ACJMCommand(this);
        this.getCommand("acjm").setExecutor(acjmCommand);

        getServer().getPluginManager().registerEvents(playerChatListener, this);
        getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        getServer().getPluginManager().registerEvents(messagesGUI, this);
    }

    private void checkForPlaceholderAPI() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        placeholderAPIEnabled = placeholderAPI != null;
        getLogger().info(placeholderAPIEnabled ? "PlaceholderAPI found and enabled." : "PlaceholderAPI not found.");
    }

    public MessagesGUI getMessagesGUI() {
        return messagesGUI;
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

        databaseManager.saveMessageToDatabase(
                playerId,
                playerJoinMessages.get(playerId),
                playerQuitMessages.get(playerId)
        );

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

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void reloadPlayerMessages() {
        playerJoinMessages.clear();
        playerQuitMessages.clear();

        databaseManager.loadMessagesFromDatabase(playerJoinMessages, playerQuitMessages);

        getLogger().info("Mensajes de entrada y salida recargados desde la base de datos.");
    }

}