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

        // Get the base message of the configuration
        String baseJoinMessage = plugin.getConfig().getString("default_messages.join", "&f[&a+&f] %player% &ase conectó &7(&6%playerjoinmessage%&7)");

        // Get the player's personalized message
        String customJoinMessage = plugin.getJoinMessage(playerId);

        // Replaces the placeholder %playerjoinmessage% with the custom message
        String finalMessage = baseJoinMessage.replace("%playerjoinmessage%", customJoinMessage);

        // Replaces the placeholder %player% with the player's name
        finalMessage = finalMessage.replace("%player%", player.getName());

        // If PlaceholderAPI is available. It replaces the PlaceholderAPI placeholders.
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Translates color codes
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Sets the player's join message
        event.setJoinMessage(finalMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Get the base message of the configuration
        String baseQuitMessage = plugin.getConfig().getString("default_messages.quit", "&f[&c-&f] %player% &cse desconectó &7(&6%playerquitmessage%&7)");

        // Get the player's personalized message
        String customQuitMessage = plugin.getQuitMessage(playerId);

        // Replaces the placeholder %playerquitmessage% with the custom message
        String finalMessage = baseQuitMessage.replace("%playerquitmessage%", customQuitMessage);

        // Replaces the placeholder %player% with the player's name
        finalMessage = finalMessage.replace("%player%", player.getName());

        // If PlaceholderAPI is available. It replaces the PlaceholderAPI placeholders.
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Replaces the placeholder %player% with the player's name
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Sets the player's quit message
        event.setQuitMessage(finalMessage);
    }
}
