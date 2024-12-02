package org.dark.customjoinmessage.Listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlayerChatListener implements Listener {
    private final CustomJoinMessage plugin;
    private List<Pattern> blockedPatterns;

    public PlayerChatListener(CustomJoinMessage plugin) {
        this.plugin = plugin;
        updateBlockedPatterns();
    }

    public void updateBlockedPatterns() {
        List<String> blockedWords = plugin.getFileManager().getConfig().getStringList("blocked_words");
        this.blockedPatterns = blockedWords.stream()
                .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerConfiguring(player.getUniqueId())) {
            event.setCancelled(true);

            String message = event.getMessage();

            int maxChars = plugin.getConfig().getInt("message_limits.max_characters", 100);
            if (message.length() > maxChars) {
                String limitMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.character_limit",
                                        "&cYour message exceeds the maximum limit of %limit% characters.")
                                .replace("%limit%", String.valueOf(maxChars)));
                player.sendMessage(limitMessage);
                return;
            }

            if (containsBlockedWord(message)) {
                String blockedMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.blocked_message",
                                "&cYour message contains a prohibited word and has been cancelled."));
                player.sendMessage(blockedMessage);
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                String cancelMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.cancel_message",
                                "&eMessage configuration has been cancelled."));
                player.sendMessage(cancelMessage);
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (plugin.isPlaceholderAPIEnabled()) {
                message = PlaceholderAPI.setPlaceholders(player, message);
            }

            if (message.length() > maxChars) {
                String limitMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.character_limit_after_placeholder",
                                        "&cYour message exceeds the character limit after processing placeholders.")
                                .replace("%limit%", String.valueOf(maxChars)));
                player.sendMessage(limitMessage);
                return;
            }

            plugin.setPlayerMessage(player.getUniqueId(), message, plugin.isConfiguringJoinMessage(player.getUniqueId()));

            String setMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.set_message", "&aYour message has been set to: &f%message%"));

            String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
            player.sendMessage(setMessage.replace("%message%", coloredMessage));

            plugin.setPlayerConfiguring(player.getUniqueId(), false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.isPlayerConfiguring(player.getUniqueId())) {
            plugin.setPlayerConfiguring(player.getUniqueId(), false);
        }
    }

    private boolean containsBlockedWord(String message) {
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }
}