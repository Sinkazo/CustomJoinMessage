package org.dark.customjoinmessage.Utilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.dark.customjoinmessage.CustomJoinMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessagesGUI implements Listener {
    private final CustomJoinMessage plugin;
    private final Map<UUID, Inventory> openInventories;
    private boolean isReloading = false;

    private static class MessagesHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private final static MessagesHolder holder = new MessagesHolder();

    public MessagesGUI(CustomJoinMessage plugin) {
        this.plugin = plugin;
        this.openInventories = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Método para convertir códigos %% a &
    private String convertColorCodes(String message) {
        if (message == null) return "";
        return message.replace("%%", "&");
    }

    // Método para aplicar colores a un mensaje
    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void open(Player player) {
        if (isReloading) return;

        String title = colorize(plugin.getConfig().getString("inventory.title", "&eCustom Inventory"));
        int size = plugin.getConfig().getInt("inventory.size", 9);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        updateInventoryItems(inventory, player);
        openInventories.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    private void updateInventoryItems(Inventory inventory, Player player) {
        inventory.clear();

        // Convertir y colorear los mensajes
        String rawJoinMessage = plugin.getPlayerJoinMessages().getOrDefault(player.getUniqueId(), "");
        String rawQuitMessage = plugin.getPlayerQuitMessages().getOrDefault(player.getUniqueId(), "");

        String joinMessage = convertColorCodes(rawJoinMessage);
        String quitMessage = convertColorCodes(rawQuitMessage);

        if (player.hasPermission("cjm.join")) {
            setConfigItem(inventory, "join_message", joinMessage, player);
        } else {
            setBarrierItem(inventory, "join_message", "&cYou don't have permissions.");
        }

        if (player.hasPermission("cjm.quit")) {
            setConfigItem(inventory, "quit_message", quitMessage, player);
        } else {
            setBarrierItem(inventory, "quit_message", "&cYou don't have permissions.");
        }

        setFillerItems(inventory);
    }

    private void setConfigItem(Inventory inventory, String path, String message, Player player) {
        String materialName = plugin.getConfig().getString("inventory.slots." + path + ".material", "PAPER");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.PAPER;

        String name = colorize(plugin.getConfig().getString("inventory.slots." + path + ".name", "&eItem"));
        List<String> lore = new ArrayList<>(plugin.getConfig().getStringList("inventory.slots." + path + ".lore"));

        // Reemplazar las variables en el lore y aplicar colores
        lore.replaceAll(line -> {
            String processedLine = line;
            if (processedLine.contains("%playerjoinmessage%")) {
                String joinMessage = plugin.getPlayerJoinMessages().getOrDefault(player.getUniqueId(), "");
                processedLine = processedLine.replace("%playerjoinmessage%", colorize(convertColorCodes(joinMessage)));
            }
            if (processedLine.contains("%playerquitmessage%")) {
                String quitMessage = plugin.getPlayerQuitMessages().getOrDefault(player.getUniqueId(), "");
                processedLine = processedLine.replace("%playerquitmessage%", colorize(convertColorCodes(quitMessage)));
            }
            return colorize(processedLine);
        });

        int slot = plugin.getConfig().getInt("inventory.slots." + path + ".slot", -1);

        if (slot >= 0 && slot < inventory.getSize()) {
            ItemStack item = createMessageItem(material, name, lore);
            inventory.setItem(slot, item);
        }
    }

    // El resto del código permanece igual...
    // (Mantener los métodos setBarrierItem, createMessageItem, setFillerItems, createFillerItem, etc.)
    private void setBarrierItem(Inventory inventory, String path, String noPermissionMessage) {
        Material material = Material.BARRIER;
        String name = colorize(plugin.getConfig().getString("inventory.slots." + path + ".name", "&cNo Permission"));
        List<String> lore = new ArrayList<>();
        lore.add(colorize(noPermissionMessage));
        int slot = plugin.getConfig().getInt("inventory.slots." + path + ".slot", -1);

        if (slot >= 0 && slot < inventory.getSize()) {
            ItemStack item = createMessageItem(material, name, lore);
            inventory.setItem(slot, item);
        }
    }

    private ItemStack createMessageItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setFillerItems(Inventory inventory) {
        List<Integer> fillerSlots = plugin.getConfig().getIntegerList("inventory.slots.filler.slot");
        String materialName = plugin.getConfig().getString("inventory.slots.filler.material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;

        String name = colorize(plugin.getConfig().getString("inventory.slots.filler.name", "&8"));

        ItemStack filler = createFillerItem(material, name);
        for (int slot : fillerSlots) {
            if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack createFillerItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MessagesHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null ||
                event.getClickedInventory().getType() != InventoryType.CHEST ||
                !(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        int joinSlot = plugin.getConfig().getInt("inventory.slots.join_message.slot", -1);
        int quitSlot = plugin.getConfig().getInt("inventory.slots.quit_message.slot", -1);

        if ((slot == joinSlot && !player.hasPermission("cjm.join")) ||
                (slot == quitSlot && !player.hasPermission("cjm.quit"))) {
            return;
        }

        if (slot != joinSlot && slot != quitSlot) {
            return;
        }

        if (plugin.isPlayerConfiguring(player.getUniqueId())) {
            return;
        }

        boolean isJoinMessage = (slot == joinSlot);
        plugin.setPlayerConfiguring(player.getUniqueId(), true);
        plugin.setConfiguringJoinMessage(player.getUniqueId(), isJoinMessage);

        String configMode = colorize(plugin.getConfig().getString("messages.config_mode", "&6Now you are in configuration mode. Type &c'cancel' &6to exit."));
        String title = colorize(plugin.getConfig().getString("messages.config_title", "&eConfiguration"));
        String subtitle = isJoinMessage ?
                colorize(plugin.getConfig().getString("messages.join_subtitle", "&aWrite your join message in the chat")) :
                colorize(plugin.getConfig().getString("messages.quit_subtitle", "&aWrite your quit message in the chat"));

        player.sendTitle(title, subtitle, 10, 70, 20);
        player.sendMessage(configMode);
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MessagesHolder) {
            event.setCancelled(true);
        }
    }

    public void reloadConfig() {
        isReloading = true;
        plugin.reloadConfig();
        updateAllOpenInventories();
        isReloading = false;
    }

    public void updateAllOpenInventories() {
        if (isReloading) return;

        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                Inventory newInventory = Bukkit.createInventory(holder,
                        entry.getValue().getSize(),
                        colorize(plugin.getConfig().getString("inventory.title", "&eCustom Inventory")));
                updateInventoryItems(newInventory, player);
                player.openInventory(newInventory);
                openInventories.put(entry.getKey(), newInventory);
            }
        }
    }

    public void closeAll() {
        for (UUID uuid : openInventories.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        openInventories.clear();
    }
}