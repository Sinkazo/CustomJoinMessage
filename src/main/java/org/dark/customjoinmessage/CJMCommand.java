package org.dark.customjoinmessage;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // Verifica si el comando es /jpm reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();  // Recarga el config.yml
            player.sendMessage("Config reloaded successfully!");
            return true;
        }

        // Verifica si el comando es /jpm setjoinmessage
        if (args.length == 1 && args[0].equalsIgnoreCase("setjoinmessage")) {
            plugin.setPlayerConfiguring(player.getUniqueId(), true);
            plugin.setConfiguringJoinMessage(player.getUniqueId(), true); // Establecer que se está configurando el mensaje de conexión
            player.sendTitle("Configuración", "Escribe tu mensaje de conexión en el chat", 10, 70, 20);
            player.sendMessage(ChatColor.GOLD + "Ahora estás en el modo de configuración de tu mensaje de conexión, escribe" + ChatColor.RED + "cancel" + ChatColor.GOLD + "para salir." );
            return true;
        }

        // Verifica si el comando es /jpm setquitmessage
        if (args.length == 1 && args[0].equalsIgnoreCase("setquitmessage")) {
            plugin.setPlayerConfiguring(player.getUniqueId(), true);
            plugin.setConfiguringJoinMessage(player.getUniqueId(), false); // Establecer que se está configurando el mensaje de desconexión
            player.sendTitle("Configuración", "Escribe tu mensaje de desconexión en el chat", 10, 70, 20);
            player.sendMessage(ChatColor.GOLD + "Ahora estás en el modo de configuración de tu mensaje de desconexión, escribe" + ChatColor.RED + "cancel" + ChatColor.GOLD + "para salir." );
            return true;
        }

        // Si no se reconoce el comando, mostrar mensaje de uso
        player.sendMessage("Invalid command. Usage: /jpm setjoinmessage | setquitmessage | reload");
        return true;
    }
}
