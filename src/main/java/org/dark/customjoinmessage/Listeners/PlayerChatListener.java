package org.dark.customjoinmessage.Listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
                player.sendMessage("Your message contains a prohibited word and has been canceled.");
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
                player.sendMessage("Message configuration has been canceled.");
                plugin.setPlayerConfiguring(player.getUniqueId(), false);
                return;
            }

            if (plugin.isPlaceholderAPIEnabled()) {
                message = PlaceholderAPI.setPlaceholders(player, message);
            }

            plugin.setPlayerMessage(player.getUniqueId(), message, plugin.isConfiguringJoinMessage(player.getUniqueId()));
            player.sendMessage("Your message has been set to: " + message);
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