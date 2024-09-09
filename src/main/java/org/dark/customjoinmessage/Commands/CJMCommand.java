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
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("cjm.reload")) {
                plugin.reloadConfig();
                player.sendMessage("Config reloaded successfully!");
            } else {
                player.sendMessage("You don't have sufficient permissions.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setjoinmessage")) {
            if (player.hasPermission("cjm.join")) {
                plugin.setPlayerConfiguring(player.getUniqueId(), true);
                plugin.setConfiguringJoinMessage(player.getUniqueId(), true);
                player.sendTitle("Configuration", "Write your connection message in the chat", 10, 70, 20);
                player.sendMessage(ChatColor.GOLD + "Now you are in the configuration mode. Type" + ChatColor.RED + " cancel " + ChatColor.GOLD + "to exit.");
            } else {
                player.sendMessage("You don't have sufficient permissions.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setquitmessage")) {
            if (player.hasPermission("cjm.quit")) {
                plugin.setPlayerConfiguring(player.getUniqueId(), true);
                plugin.setConfiguringJoinMessage(player.getUniqueId(), false);
                player.sendTitle("Configuration", "Write your disconnection message in chat", 10, 70, 20);
                player.sendMessage(ChatColor.GOLD + "Now you are in the configuration mode. Type" + ChatColor.RED + " cancel " + ChatColor.GOLD + "to exit.");
            } else {
                player.sendMessage("You don't have sufficient permissions.");
            }
            return true;
        }

        player.sendMessage("Invalid command. Usage: /cjm setjoinmessage | setquitmessage | reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Devuelve la lista de subcomandos disponibles para el primer argumento
            return Arrays.asList("reload", "setjoinmessage", "setquitmessage");
        }
        return new ArrayList<>();
    }
}
