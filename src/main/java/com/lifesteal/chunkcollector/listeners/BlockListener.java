package com.lifesteal.chunkcollector.listeners;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import com.lifesteal.chunkcollector.CollectorData;
import com.lifesteal.chunkcollector.CollectorManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class BlockListener implements Listener {

    private final ChunkCollectorPlugin plugin;

    public BlockListener(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        if (!plugin.getItemFactory().isCollectorItem(itemInHand)) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!player.hasPermission("chunkcollector.use")) {
            event.setCancelled(true);
            sendMsg(player, "no-permission");
            return;
        }

        CollectorManager manager = plugin.getCollectorManager();

        // Cek batas 1 collector per chunk (jika diaktifkan di config)
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        String world = block.getWorld().getName();

        if (plugin.getConfig().getBoolean("one-per-chunk", true)
                && manager.hasCollectorInChunk(chunkX, chunkZ, world)) {
            event.setCancelled(true);
            sendMsg(player, "one-per-chunk");
            return;
        }

        if (!manager.canPlace(player)) {
            event.setCancelled(true);
            String msg = plugin.getConfig().getString("messages.limit-reached", "&cLimit reached")
                    .replace("%limit%", String.valueOf(manager.getLimit(player)));
            player.sendMessage(color(plugin.getConfig().getString("messages.prefix", "")) + color(msg));
            return;
        }

        int storageSize = plugin.getConfig().getInt("storage-size", 45);
        Inventory storage = plugin.getServer().createInventory(null, storageSize, "Chunk Collector Storage");

        Location loc = block.getLocation();
        long chunkKey = CollectorManager.chunkKey(chunkX, chunkZ, world);
        UUID id = UUID.randomUUID();

        CollectorData data = new CollectorData(id, player.getUniqueId(), loc, chunkKey, storage);
        manager.register(data);
        plugin.getDataStorage().save(manager);

        String msg = plugin.getConfig().getString("messages.placed", "&aPlaced")
                .replace("%current%", String.valueOf(manager.getOwnedCount(player.getUniqueId())))
                .replace("%limit%", String.valueOf(manager.getLimit(player)));
        player.sendMessage(color(plugin.getConfig().getString("messages.prefix", "")) + color(msg));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CollectorManager manager = plugin.getCollectorManager();
        CollectorData data = manager.getByLocation(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        boolean isOwner = data.getOwner().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("chunkcollector.admin");

        if (!isOwner && !isAdmin) {
            event.setCancelled(true);
            sendMsg(player, "not-owner");
            return;
        }

        // Jangan drop item vanilla dari block asli, kita drop item custom + isi storage
        event.setDropItems(false);

        // Kembalikan isi storage ke pemain (atau jatuhkan ke tanah jika penuh)
        for (ItemStack item : data.getStorage().getContents()) {
            if (item == null) continue;
            player.getInventory().addItem(item).values()
                    .forEach(leftover -> block.getWorld().dropItemNaturally(block.getLocation(), leftover));
        }

        // Kembalikan item Chunk Collector itu sendiri
        block.getWorld().dropItemNaturally(block.getLocation(), plugin.getItemFactory().createCollectorItem(1));

        manager.unregister(data);
        plugin.getDataStorage().save(manager);
        sendMsg(player, "removed");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        CollectorManager manager = plugin.getCollectorManager();
        CollectorData data = manager.getByLocation(block.getLocation());
        if (data == null) return;

        event.setCancelled(true); // cegah GUI chest vanilla ikut terbuka
        plugin.getCollectorGUI().open(event.getPlayer(), data);
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
