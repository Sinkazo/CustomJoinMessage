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
            String message = event.getMessage();

            if (containsBlockedWord(message)) {
                event.setCancelled(true);
                // Get the message from config.yml and translate color codes
                String blockedMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.blocked_message", "&cYour message contains a prohibited word and has been cancelled."));
                player.sendMessage(blockedMessage);
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
                // Get the message from config.yml and translate color codes
                String cancelMessage = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.cancel_message", "&eMessage configuration has been cancelled."));
                player.sendMessage(cancelMessage);
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (plugin.isPlaceholderAPIEnabled()) {
                message = PlaceholderAPI.setPlaceholders(player, message);
            }

            plugin.setPlayerMessage(player.getUniqueId(), message, plugin.isConfiguringJoinMessage(player.getUniqueId()));
            // Get the message from config.yml and translate color codes
            String setMessage = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.set_message", "&aYour message has been set to: &f%message%"));
            player.sendMessage(setMessage.replace("%message%", message));
            plugin.setPlayerConfiguring(player.getUniqueId(), false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        if (plugin.isPlayerConfiguring(p.getUniqueId())) {
            plugin.setPlayerConfiguring(p.getUniqueId(), false);
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
