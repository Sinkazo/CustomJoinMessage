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
import java.util.stream.Collectors;

public class CJMCommand implements CommandExecutor, TabCompleter {
    private final CustomJoinMessage plugin;

    public CJMCommand(CustomJoinMessage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String consoleMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.console_only", "&cThis command can only be used by players."));
            sender.sendMessage(consoleMessage);
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("gui")) {
                plugin.getMessagesGUI().open(player);
                return true;
            } else if (args[0].equalsIgnoreCase("setjoinmessage")) {
                if (player.hasPermission("cjm.join")) {
                    startMessageConfiguration(player, true);
                } else {
                    sendNoPermissionMessage(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("setquitmessage")) {
                if (player.hasPermission("cjm.quit")) {
                    startMessageConfiguration(player, false);
                } else {
                    sendNoPermissionMessage(player);
                }
                return true;
            } else {
                String invalidCommandMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.invalid_command",
                                "&cInvalid command. Usage: /cjm gui | setjoinmessage | setquitmessage | "));
                player.sendMessage(invalidCommandMessage);
                return true;
            }
        }

        String usageMessage = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.usage", "&cUsage: /cjm gui | setjoinmessage | setquitmessage | "));
        player.sendMessage(usageMessage);
        return true;
    }

    private void startMessageConfiguration(Player player, boolean isJoinMessage) {
        plugin.setPlayerConfiguring(player.getUniqueId(), true);
        plugin.setConfiguringJoinMessage(player.getUniqueId(), isJoinMessage);

        String configTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.config_title", "&eConfiguration"));
        String subtitle = isJoinMessage ?
                plugin.getConfig().getString("messages.join_subtitle", "&aWrite your join message in the chat") :
                plugin.getConfig().getString("messages.quit_subtitle", "&aWrite your quit message in the chat");
        subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);

        player.sendTitle(configTitle, subtitle, 10, 70, 20);

        String configMode = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.config_mode",
                        "&6Now you are in configuration mode. Type &c'cancel' &6to exit."));
        player.sendMessage(configMode);
    }

    private void sendNoPermissionMessage(Player player) {
        String message = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.no_permission",
                        "&cYou don't have sufficient permissions."));
        player.sendMessage(message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("setjoinmessage", "setquitmessage", "gui"));
            String input = args[0].toLowerCase();
            return commands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
