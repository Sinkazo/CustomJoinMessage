package org.dark.customjoinmessage.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ACJMCommand implements CommandExecutor, TabCompleter {

    private final CustomJoinMessage plugin;

    public ACJMCommand(CustomJoinMessage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        if (!sender.hasPermission("cjm.admin")) {
            sender.sendMessage(getConfigMessage("messages.no_permission"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (args.length < 2 && !subCommand.equalsIgnoreCase("reload")) {
            showHelp(sender);
            return true;
        }

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("cjm.reload")) {
                sender.sendMessage(getConfigMessage("messages.no_permission"));
                return true;
            }

            plugin.reloadConfig();
            sender.sendMessage(getConfigMessage("messages.config_reloaded"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(getConfigMessage("messages.invalid_command"));
            return true;
        }

        String playerName = args[1];
        UUID playerId = plugin.getDatabaseManager().getPlayerUUIDByName(playerName);

        if (playerId == null) {
            sender.sendMessage(getConfigMessage("messages.player_not_found").replace("%player%", playerName));
            return true;
        }

        if (!isPlayerInDatabase(playerId)) {
            sender.sendMessage(getConfigMessage("messages.player_not_registered").replace("%player%", playerName));
            return true;
        }

        switch (subCommand) {
            case "showjoinmessage":
                showJoinMessage(sender, playerName, playerId);
                break;
            case "showquitmessage":
                showQuitMessage(sender, playerName, playerId);
                break;
            case "setjoinmessage":
                if (args.length < 3) {
                    sender.sendMessage(getConfigMessage("messages.setjoin_usage"));
                    return true;
                }
                setJoinMessage(sender, playerName, playerId, buildMessage(args, 2));
                break;
            case "setquitmessage":
                if (args.length < 3) {
                    sender.sendMessage(getConfigMessage("messages.setquit_usage"));
                    return true;
                }
                setQuitMessage(sender, playerName, playerId, buildMessage(args, 2));
                break;
            case "deljoinmessage":
                deleteJoinMessage(sender, playerName, playerId);
                break;
            case "delquitmessage":
                deleteQuitMessage(sender, playerName, playerId);
                break;
            default:
                sender.sendMessage(getConfigMessage("messages.unknown_subcommand"));
                break;
        }
        return true;
    }

    // Método para el autocompletado
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "showjoinmessage", "showquitmessage",
                    "setjoinmessage", "setquitmessage",
                    "deljoinmessage", "delquitmessage");
            return filterStartingWith(args[0], subCommands);
        } else if (args.length == 2 && sender.hasPermission("cjm.admin")) {
            return null;
        }
        return new ArrayList<>();
    }

    private List<String> filterStartingWith(String input, List<String> options) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }

    private void showHelp(CommandSender sender) {
        for (String message : plugin.getConfig().getStringList("messages.help")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private String getConfigMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(path, "§cMissing message in config: " + path));
    }

    private boolean isPlayerInDatabase(UUID playerId) {
        String query = "SELECT uuid FROM player_messages WHERE uuid = ?";
        try (PreparedStatement stmt = plugin.getDatabaseManager().getConnection().prepareStatement(query)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking player in database: " + e.getMessage());
            return false;
        }
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder message = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }
        return message.toString().trim().replace("&", "§");
    }

    private void showJoinMessage(CommandSender sender, String playerName, UUID playerId) {
        String joinMessage = plugin.getDatabaseManager().getJoinMessage(playerId);
        if (joinMessage != null && !joinMessage.isEmpty()) {
            sender.sendMessage(getConfigMessage("messages.show_join_message")
                    .replace("%player%", playerName)
                    .replace("%message%", joinMessage));
        } else {
            sender.sendMessage(getConfigMessage("messages.no_join_message")
                    .replace("%player%", playerName));
        }
    }

    private void showQuitMessage(CommandSender sender, String playerName, UUID playerId) {
        String quitMessage = plugin.getDatabaseManager().getQuitMessage(playerId);
        if (quitMessage != null && !quitMessage.isEmpty()) {
            sender.sendMessage(getConfigMessage("messages.show_quit_message")
                    .replace("%player%", playerName)
                    .replace("%message%", quitMessage));
        } else {
            sender.sendMessage(getConfigMessage("messages.no_quit_message")
                    .replace("%player%", playerName));
        }
    }

    private void setJoinMessage(CommandSender sender, String playerName, UUID playerId, String message) {
        String currentQuitMessage = plugin.getDatabaseManager().getQuitMessage(playerId);
        plugin.getDatabaseManager().saveMessageToDatabase(playerId, message, currentQuitMessage);
        sender.sendMessage(getConfigMessage("messages.join_message_set")
                .replace("%player%", playerName)
                .replace("%message%", message));
        plugin.reloadPlayerMessages();
    }

    private void setQuitMessage(CommandSender sender, String playerName, UUID playerId, String message) {
        String currentJoinMessage = plugin.getDatabaseManager().getJoinMessage(playerId);
        plugin.getDatabaseManager().saveMessageToDatabase(playerId, currentJoinMessage, message);
        sender.sendMessage(getConfigMessage("messages.quit_message_set")
                .replace("%player%", playerName)
                .replace("%message%", message));
        plugin.reloadPlayerMessages();
    }

    private void deleteJoinMessage(CommandSender sender, String playerName, UUID playerId) {
        String currentQuitMessage = plugin.getDatabaseManager().getQuitMessage(playerId);
        plugin.getDatabaseManager().saveMessageToDatabase(playerId, "", currentQuitMessage);
        sender.sendMessage(getConfigMessage("messages.join_message_deleted")
                .replace("%player%", playerName));
        plugin.reloadPlayerMessages();
    }

    private void deleteQuitMessage(CommandSender sender, String playerName, UUID playerId) {
        String currentJoinMessage = plugin.getDatabaseManager().getJoinMessage(playerId);
        plugin.getDatabaseManager().saveMessageToDatabase(playerId, currentJoinMessage, "");
        sender.sendMessage(getConfigMessage("messages.quit_message_deleted")
                .replace("%player%", playerName));
        plugin.reloadPlayerMessages();
    }
}
