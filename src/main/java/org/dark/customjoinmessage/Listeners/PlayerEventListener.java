package org.dark.customjoinmessage.Listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.util.UUID;

public class PlayerEventListener implements Listener {

    private final CustomJoinMessage plugin;

    public PlayerEventListener(CustomJoinMessage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Obtén el mensaje base de la configuración
        String baseJoinMessage = plugin.getConfig().getString("default_messages.join", "&f[&a+&f] %player% &ase conectó &7(&6%playerjoinmessage%&7)");

        // Obtén el mensaje personalizado del jugador
        String customJoinMessage = plugin.getJoinMessage(playerId);

        // Reemplaza el placeholder %playerjoinmessage% con el mensaje personalizado
        String finalMessage = baseJoinMessage.replace("%playerjoinmessage%", customJoinMessage);

        // Reemplaza el placeholder %player% con el nombre del jugador
        finalMessage = finalMessage.replace("%player%", player.getName());

        // Si PlaceholderAPI está disponible, reemplaza los placeholders de PlaceholderAPI
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Traduce los códigos de color
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Establece el mensaje de entrada del jugador
        event.setJoinMessage(finalMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Obtén el mensaje base de la configuración
        String baseQuitMessage = plugin.getConfig().getString("default_messages.quit", "&f[&c-&f] %player% &cse desconectó &7(&6%playerquitmessage%&7)");

        // Obtén el mensaje personalizado del jugador
        String customQuitMessage = plugin.getQuitMessage(playerId);

        // Reemplaza el placeholder %playerquitmessage% con el mensaje personalizado
        String finalMessage = baseQuitMessage.replace("%playerquitmessage%", customQuitMessage);

        // Reemplaza el placeholder %player% con el nombre del jugador
        finalMessage = finalMessage.replace("%player%", player.getName());

        // Si PlaceholderAPI está disponible, reemplaza los placeholders de PlaceholderAPI
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Traduce los códigos de color
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Establece el mensaje de salida del jugador
        event.setQuitMessage(finalMessage);
    }
}
