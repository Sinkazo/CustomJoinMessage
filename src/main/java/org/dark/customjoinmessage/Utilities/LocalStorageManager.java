package org.dark.customjoinmessage.Utilities;

import org.dark.customjoinmessage.CustomJoinMessage;
import java.util.Map;
import java.util.UUID;

public class LocalStorageManager {
    private final CustomJoinMessage plugin;
    private final DatabaseManager databaseManager;

    public LocalStorageManager(CustomJoinMessage plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        loadLocalStorage();
    }

    public void loadLocalStorage() {
        databaseManager.loadMessagesFromDatabase(
                plugin.getPlayerJoinMessages(),
                plugin.getPlayerQuitMessages()
        );
        plugin.getLogger().info("Loaded messages from database");
    }

    public void saveDataToFile(Map<UUID, String> playerJoinMessages, Map<UUID, String> playerQuitMessages) {
        for (Map.Entry<UUID, String> entry : playerJoinMessages.entrySet()) {
            UUID playerId = entry.getKey();
            String joinMessage = entry.getValue();
            String quitMessage = playerQuitMessages.get(playerId);
            databaseManager.saveMessageToDatabase(playerId, joinMessage, quitMessage);
        }
        plugin.getLogger().info("Messages saved to database");
    }
}