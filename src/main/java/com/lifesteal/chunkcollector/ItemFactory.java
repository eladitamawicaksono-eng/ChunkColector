package com.lifesteal.chunkcollector;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemFactory {

    private final ChunkCollectorPlugin plugin;
    private final NamespacedKey key;

    public ItemFactory(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "chunk_collector_item");
    }

    public ItemStack createCollectorItem(int amount) {
        String materialName = plugin.getConfig().getString("item.material", "CHEST");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.CHEST;

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("item.name", "&6&lChunk Collector");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

        List<String> loreConfig = plugin.getConfig().getStringList("item.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreConfig) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCollectorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
