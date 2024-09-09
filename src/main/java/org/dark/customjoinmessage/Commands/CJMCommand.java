package org.dark.customjoinmessage.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CJMCommand implements CommandExecutor, TabCompleter {
    private final CustomJoinMessage plugin;

    public CJMCommand(CustomJoinMessage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // Get message from config.yml and translate color codes
            String consoleMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.console_only", "&cThis command can only be used by players."));
            sender.sendMessage(consoleMessage);
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("cjm.reload")) {
                plugin.reloadConfig();
                // Get message from config.yml and translate color codes
                String reloadMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.config_reloaded", "&aConfig reloaded successfully!"));
                player.sendMessage(reloadMessage);
            } else {
                // Get message from config.yml and translate color codes
                String noPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.no_permission", "&cYou don't have sufficient permissions."));
                player.sendMessage(noPermissionMessage);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setjoinmessage")) {
            if (player.hasPermission("cjm.join")) {
                plugin.setPlayerConfiguring(player.getUniqueId(), true);
                plugin.setConfiguringJoinMessage(player.getUniqueId(), true);
                // Get title and subtitle from config.yml and translate color codes
                String configTitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.config_title", "&eConfiguration"));
                String joinSubtitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.join_subtitle", "&aWrite your join message in the chat"));
                player.sendTitle(configTitle, joinSubtitle, 10, 70, 20);
                // Get configuration mode message from config.yml and translate color codes
                String configModeMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.config_mode", "&6Now you are in configuration mode. Type &c'cancel' &6to exit."));
                player.sendMessage(configModeMessage);
            } else {
                // Get message from config.yml and translate color codes
                String noPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.no_permission", "&cYou don't have sufficient permissions."));
                player.sendMessage(noPermissionMessage);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setquitmessage")) {
            if (player.hasPermission("cjm.quit")) {
                plugin.setPlayerConfiguring(player.getUniqueId(), true);
                plugin.setConfiguringJoinMessage(player.getUniqueId(), false);
                // Get title and subtitle from config.yml and translate color codes
                String configTitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.config_title", "&eConfiguration"));
                String quitSubtitle = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.quit_subtitle", "&aWrite your quit message in the chat"));
                player.sendTitle(configTitle, quitSubtitle, 10, 70, 20);
                // Get configuration mode message from config.yml and translate color codes
                String configModeMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.config_mode", "&6Now you are in configuration mode. Type &c'cancel' &6to exit."));
                player.sendMessage(configModeMessage);
            } else {
                // Get message from config.yml and translate color codes
                String noPermissionMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.no_permission", "&cYou don't have sufficient permissions."));
                player.sendMessage(noPermissionMessage);
            }
            return true;
        }

        // Get invalid command usage message from config.yml and translate color codes
        String invalidCommandMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.invalid_command", "&cInvalid command. Usage: /cjm setjoinmessage | setquitmessage | reload"));
        player.sendMessage(invalidCommandMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Return a list of available subcommands for the first argument
            return Arrays.asList("reload", "setjoinmessage", "setquitmessage");
        }
        return new ArrayList<>();
    }
}
