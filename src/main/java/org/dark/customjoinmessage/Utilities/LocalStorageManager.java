package org.dark.customjoinmessage.Utilities;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class LocalStorageManager {
    private FileConfiguration dataConfig;
    private File dataFile;
    private final CustomJoinMessage plugin;

    public LocalStorageManager(CustomJoinMessage plugin) {
        this.plugin = plugin;
        loadLocalStorage();
    }

    public void loadLocalStorage() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            plugin.saveResource("data.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : dataConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            String joinMessage = dataConfig.getString(key + ".join_message");
            String quitMessage = dataConfig.getString(key + ".quit_message");

            plugin.getPlayerJoinMessages().put(playerId, joinMessage != null ? joinMessage : "");
            plugin.getPlayerQuitMessages().put(playerId, quitMessage != null ? quitMessage : "");
        }
        plugin.getLogger().info("Loaded messages from data.yml");
    }

    public void saveDataToFile(Map<UUID, String> playerJoinMessages, Map<UUID, String> playerQuitMessages) {
        if (dataConfig == null || dataFile == null) return;

        for (Map.Entry<UUID, String> entry : playerJoinMessages.entrySet()) {
            UUID playerId = entry.getKey();
            dataConfig.set(playerId.toString() + ".join_message", entry.getValue());
            dataConfig.set(playerId.toString() + ".quit_message", playerQuitMessages.get(playerId));
        }

        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("Messages saved to data.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data to data.yml");
            e.printStackTrace();
        }
    }
}
