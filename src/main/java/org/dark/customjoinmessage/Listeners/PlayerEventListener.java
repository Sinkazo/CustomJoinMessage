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

        // Get the player's custom join message only if they have permission
        String customJoinMessage = "";
        if (player.hasPermission("cjm.join")) {
            String savedMessage = plugin.getJoinMessage(playerId);
            if (savedMessage != null && !savedMessage.isEmpty()) {
                customJoinMessage = savedMessage;
            }
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
            if (!customJoinMessage.isEmpty()) {
                // Filter color codes for Discord message
                String filteredMessage = filterColorCodes(customJoinMessage);
                discordMessage += " - " + filteredMessage;
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

        // Get the player's custom quit message only if they have permission
        String customQuitMessage = "";
        if (player.hasPermission("cjm.quit")) {
            String savedMessage = plugin.getQuitMessage(playerId);
            if (savedMessage != null && !savedMessage.isEmpty()) {
                customQuitMessage = savedMessage;
            }
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
            if (!customQuitMessage.isEmpty()) {
                // Filter color codes for Discord message
                String filteredMessage = filterColorCodes(customQuitMessage);
                discordMessage += " - " + filteredMessage;
            }
            sendDiscordMessage("global", discordMessage);
        }
    }

    /**
     * Filters Minecraft color codes while preserving & characters that are part of the text
     * @param input The input string to filter
     * @return The filtered string without color codes but preserving legitimate & characters
     */
    private String filterColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&') {
                // Check if this is a color code
                if (i + 1 < chars.length) {
                    char nextChar = chars[i + 1];
                    // Check if the next character is a valid color code character
                    if (isColorCode(nextChar)) {
                        // Skip both the & and the color code
                        i++;
                        continue;
                    }
                }

                // If we're here, it's either the last character or not followed by a color code
                // Check if it's surrounded by spaces or at the start/end of the string
                boolean isAtStart = i == 0;
                boolean isAtEnd = i == chars.length - 1;
                boolean hasSpaceBefore = !isAtStart && Character.isWhitespace(chars[i - 1]);
                boolean hasSpaceAfter = !isAtEnd && Character.isWhitespace(chars[i + 1]);

                if (isAtStart || isAtEnd || hasSpaceBefore || hasSpaceAfter) {
                    result.append('&');
                }
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    /**
     * Checks if a character is a valid Minecraft color code
     * @param c The character to check
     * @return true if it's a valid color code character
     */
    private boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
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