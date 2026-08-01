package com.lifesteal.chunkcollector.listeners;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import com.lifesteal.chunkcollector.CollectorData;
import com.lifesteal.chunkcollector.gui.CollectorGUI;
import com.lifesteal.chunkcollector.hooks.VaultHook;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final ChunkCollectorPlugin plugin;

    public GUIListener(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CollectorGUI.CollectorHolder collectorHolder)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory gui = event.getInventory();
        int totalSize = gui.getSize();
        int clickedSlot = event.getRawSlot();

        // Klik di area inventory pribadi player (bawah GUI) dibiarkan normal
        if (clickedSlot >= totalSize) return;

        int sellSlot = totalSize - 2;
        int withdrawSlot = totalSize - 1;

        if (clickedSlot == sellSlot) {
            event.setCancelled(true);
            sellAll(player, collectorHolder.getData(), gui, totalSize);
            return;
        }

        if (clickedSlot == withdrawSlot) {
            event.setCancelled(true);
            withdrawAll(player, gui, totalSize);
            return;
        }

        // Klik di slot storage (baris atas) dibiarkan bebas diambil/ditata pemain
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CollectorGUI.CollectorHolder collectorHolder)) return;

        syncStorage(collectorHolder.getData(), event.getInventory());
        plugin.getDataStorage().save(plugin.getCollectorManager());
    }

    private int storageSize() {
        return plugin.getConfig().getInt("storage-size", 45);
    }

    private void syncStorage(CollectorData data, Inventory gui) {
        int size = storageSize();
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = gui.getItem(i);
        }
        data.getStorage().setContents(items);
    }

    private void sellAll(Player player, CollectorData data, Inventory gui, int totalSize) {
        if (!VaultHook.isEnabled()) {
            sendMsg(player, "no-vault");
            return;
        }

        var prices = plugin.getConfig().getConfigurationSection("prices");
        double total = 0.0;
        int size = storageSize();

        for (int i = 0; i < size; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null) continue;
            String matName = item.getType().name();
            if (prices == null || !prices.contains(matName)) continue;

            double pricePerItem = prices.getDouble(matName);
            total += pricePerItem * item.getAmount();
            gui.setItem(i, null);
        }

        if (total <= 0) {
            sendMsg(player, "sell-empty");
            return;
        }

        VaultHook.deposit(player, total);
        syncStorage(data, gui);
        plugin.getDataStorage().save(plugin.getCollectorManager());

        String msg = plugin.getConfig().getString("messages.sell-success", "&aSold for $%amount%")
                .replace("%amount%", String.format("%.2f", total));
        player.sendMessage(color(plugin.getConfig().getString("messages.prefix", "")) + color(msg));
    }

    private void withdrawAll(Player player, Inventory gui, int totalSize) {
        int size = storageSize();
        for (int i = 0; i < size; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null) continue;
            var leftover = player.getInventory().addItem(item);
            if (leftover.isEmpty()) {
                gui.setItem(i, null);
            } else {
                gui.setItem(i, leftover.values().iterator().next());
            }
        }
    }

    private void sendMsg(Player player, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages." + key, "");
        player.sendMessage(color(prefix) + color(msg));
    }

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
