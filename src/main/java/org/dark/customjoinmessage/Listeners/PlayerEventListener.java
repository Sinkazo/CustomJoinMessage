package org.dark.customjoinmessage.Listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.dark.customjoinmessage.CustomJoinMessage;
import org.bukkit.plugin.Plugin;

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

        // Get the base join message from config.yml
        String baseJoinMessage = plugin.getConfig().getString("messages.join", "&f[&a+&f] %player% &ajoined &7(&6%playerjoinmessage%&7)");

        // Get the player's custom join message
        String customJoinMessage = plugin.getJoinMessage(playerId);
        if (customJoinMessage == null) {
            customJoinMessage = ""; // Default value if null
        }

        // Replace placeholders
        String finalMessage = baseJoinMessage.replace("%playerjoinmessage%", customJoinMessage)
                .replace("%player%", player.getName());

        // If PlaceholderAPI is enabled, replace additional placeholders
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Translate color codes
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Set the join message
        event.setJoinMessage(finalMessage);

        // Send Discord message only if DiscordSRV is properly loaded, enabled and discord integration is enabled in config
        if (isDiscordSRVEnabled() && plugin.getConfig().getBoolean("discord.enabled", true)) {
            String discordMessage = ":inbox_tray: " + player.getName();
            if (customJoinMessage != null && !customJoinMessage.isEmpty()) {
                discordMessage += " - " + customJoinMessage;
            }
            sendDiscordMessage("global", discordMessage);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Get the base quit message from config.yml
        String baseQuitMessage = plugin.getConfig().getString("messages.quit", "&f[&c-&f] %player% &cleft &7(&6%playerquitmessage%&7)");

        // Get the player's custom quit message
        String customQuitMessage = plugin.getQuitMessage(playerId);
        if (customQuitMessage == null) {
            customQuitMessage = ""; // Default value if null
        }

        // Replace placeholders
        String finalMessage = baseQuitMessage.replace("%playerquitmessage%", customQuitMessage)
                .replace("%player%", player.getName());

        // If PlaceholderAPI is enabled, replace additional placeholders
        if (plugin.isPlaceholderAPIEnabled()) {
            finalMessage = PlaceholderAPI.setPlaceholders(player, finalMessage);
        }

        // Translate color codes
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Set the quit message
        event.setQuitMessage(finalMessage);

        // Send Discord message only if DiscordSRV is properly loaded, enabled and discord integration is enabled in config
        if (isDiscordSRVEnabled() && plugin.getConfig().getBoolean("discord.enabled", true)) {
            String discordMessage = ":outbox_tray: " + player.getName();
            if (customQuitMessage != null && !customQuitMessage.isEmpty()) {
                discordMessage += " - " + customQuitMessage;
            }
            sendDiscordMessage("global", discordMessage);
        }
    }

    private boolean isDiscordSRVEnabled() {
        try {
            Plugin discordSRV = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
            return discordSRV != null && discordSRV.isEnabled() && Class.forName("github.scarsz.discordsrv.DiscordSRV") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void sendDiscordMessage(String channelName, String message) {
        if (!isDiscordSRVEnabled()) return;

        try {
            Class.forName("github.scarsz.discordsrv.DiscordSRV");
            github.scarsz.discordsrv.DiscordSRV discordSRV = github.scarsz.discordsrv.DiscordSRV.getPlugin();
            if (discordSRV != null) {
                github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel channel =
                        discordSRV.getDestinationTextChannelForGameChannelName(channelName);
                if (channel != null) {
                    channel.sendMessage(message).queue();
                }
            }
        } catch (Exception e) {
            // Silently ignore any Discord-related errors
        }
    }
}