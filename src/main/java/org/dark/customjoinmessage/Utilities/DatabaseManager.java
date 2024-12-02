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
            plugin.getLogger().info("Conexión exitosa a MySQL.");
        } catch (SQLException e) {
            plugin.getLogger().severe("No se pudo conectar a MySQL. Usando SQLite como respaldo.");
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
            plugin.getLogger().info("Conexión exitosa a SQLite.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al conectar con SQLite:");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        if (connection == null) {
            plugin.getLogger().severe("La conexión a la base de datos no está inicializada.");
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
                plugin.getLogger().warning("Error al verificar/agregar la columna player_name: " + e.getMessage());
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error al crear las tablas:");
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
                    plugin.getLogger().warning("UUID inválido encontrado en la base de datos: " + e.getMessage());
                }
            }

            plugin.getLogger().info("Cargados " + joinMessages.size() + " mensajes de entrada y " +
                    quitMessages.size() + " mensajes de salida.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar mensajes de la base de datos:");
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
            plugin.getLogger().warning("Error al buscar UUID en la base de datos: " + e.getMessage());
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
            plugin.getLogger().severe("Intento de guardar mensaje con UUID nulo");
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
            plugin.getLogger().severe("Error al guardar mensajes para UUID " + playerId);
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
            plugin.getLogger().severe("Error al obtener mensaje de entrada para UUID " + playerId);
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
            plugin.getLogger().severe("Error al obtener mensaje de salida para UUID " + playerId);
            e.printStackTrace();
        }
        return null;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Conexión con la base de datos cerrada correctamente.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Error al cerrar la conexión con la base de datos:");
                e.printStackTrace();
            }
        }
    }
}
