package org.dark.customjoinmessage.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.dark.customjoinmessage.CustomJoinMessage;

public class CJMCommand implements CommandExecutor {
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
            plugin.reloadConfig();
            player.sendMessage("Config reloaded successfully!");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setjoinmessage")) {
            plugin.setPlayerConfiguring(player.getUniqueId(), true);
            plugin.setConfiguringJoinMessage(player.getUniqueId(), true);
            player.sendTitle("Configuración", "Escribe tu mensaje de conexión en el chat", 10, 70, 20);
            player.sendMessage(ChatColor.GOLD + "Ahora estás en el modo de configuración de tu mensaje de conexión, escribe " + ChatColor.RED + "cancel" + ChatColor.GOLD + " para salir.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("setquitmessage")) {
            plugin.setPlayerConfiguring(player.getUniqueId(), true);
            plugin.setConfiguringJoinMessage(player.getUniqueId(), false);
            player.sendTitle("Configuración", "Escribe tu mensaje de desconexión en el chat", 10, 70, 20);
            player.sendMessage(ChatColor.GOLD + "Ahora estás en el modo de configuración de tu mensaje de desconexión, escribe " + ChatColor.RED + "cancel" + ChatColor.GOLD + " para salir.");
            return true;
        }

        player.sendMessage("Invalid command. Usage: /jpm setjoinmessage | setquitmessage | reload");
        return true;
    }
}