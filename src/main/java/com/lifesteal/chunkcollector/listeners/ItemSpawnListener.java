package com.lifesteal.chunkcollector.listeners;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import com.lifesteal.chunkcollector.CollectorData;
import com.lifesteal.chunkcollector.CollectorManager;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemSpawnListener implements Listener {

    private final ChunkCollectorPlugin plugin;

    public ItemSpawnListener(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item entity = event.getEntity();
        ItemStack stack = entity.getItemStack();

        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.contains(stack.getType().name())) return;

        int chunkX = entity.getLocation().getBlockX() >> 4;
        int chunkZ = entity.getLocation().getBlockZ() >> 4;
        String world = entity.getWorld().getName();

        CollectorManager manager = plugin.getCollectorManager();
        CollectorData data = manager.getFirstInChunk(chunkX, chunkZ, world);
        if (data == null) return;

        var leftover = data.getStorage().addItem(stack);
        if (leftover.isEmpty()) {
            // Semua item berhasil masuk storage, hapus entity drop dari dunia
            entity.remove();
        } else if (leftover.size() < 1 || leftover.values().iterator().next().getAmount() < stack.getAmount()) {
            // Sebagian masuk, sisakan sisanya di entity supaya tidak duplikat/hilang
            entity.setItemStack(leftover.values().iterator().next());
        }
        // Jika storage penuh sepenuhnya (leftover == stack asli), biarkan item tetap jatuh normal di tanah
    }
}
