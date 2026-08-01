package com.lifesteal.chunkcollector.gui;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import com.lifesteal.chunkcollector.CollectorData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CollectorGUI {

    // Slot tombol kontrol pada baris terakhir GUI (dipakai GUIListener untuk mendeteksi klik)
    public static final int SELL_ALL_SLOT_OFFSET = 1; // relatif dari akhir (slot terakhir - 1)
    public static final int WITHDRAW_SLOT_OFFSET = 0;  // slot terakhir

    private final ChunkCollectorPlugin plugin;

    public CollectorGUI(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, CollectorData data) {
        int storageSize = plugin.getConfig().getInt("storage-size", 45);
        int totalSize = storageSize + 9; // baris tambahan untuk tombol kontrol
        if (totalSize > 54) totalSize = 54;

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.gui-title", "&8Chunk Collector"));

        Inventory gui = plugin.getServer().createInventory(new CollectorHolder(data), totalSize, title);

        // Salin isi storage collector ke GUI (baris atas)
        ItemStack[] contents = data.getStorage().getContents();
        for (int i = 0; i < storageSize && i < contents.length; i++) {
            gui.setItem(i, contents[i]);
        }

        // Tombol Withdraw All (slot terakhir)
        gui.setItem(totalSize - 1, buildButton(Material.HOPPER, "&eWithdraw All",
                List.of("&7Ambil semua item ke inventorymu")));

        // Tombol Sell All (slot kedua dari terakhir)
        gui.setItem(totalSize - 2, buildButton(Material.EMERALD, "&aSell All",
                List.of("&7Jual seluruh isi collector", "&7ke uang server (Vault)")));

        player.openInventory(gui);
    }

    private ItemStack buildButton(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Menandai sebuah Inventory sebagai GUI milik collector tertentu. */
    public static class CollectorHolder implements org.bukkit.inventory.InventoryHolder {
        private final CollectorData data;

        public CollectorHolder(CollectorData data) {
            this.data = data;
        }

        public CollectorData getData() {
            return data;
        }

        @Override
        public Inventory getInventory() {
            return null; // tidak dipakai, Bukkit menetapkan inventory-nya sendiri saat createInventory
        }
    }
}
