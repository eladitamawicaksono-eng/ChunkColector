package com.lifesteal.chunkcollector.storage;

import com.lifesteal.chunkcollector.ChunkCollectorPlugin;
import com.lifesteal.chunkcollector.CollectorData;
import com.lifesteal.chunkcollector.CollectorManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Menyimpan seluruh CollectorData ke file collectors.yml
 * dan memuatnya kembali saat server start.
 */
public class DataStorage {

    private final ChunkCollectorPlugin plugin;
    private final File file;

    public DataStorage(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "collectors.yml");
    }

    public void save(CollectorManager manager) {
        YamlConfiguration yml = new YamlConfiguration();
        int index = 0;
        for (CollectorData data : manager.getAll()) {
            String path = "collectors." + index;
            yml.set(path + ".id", data.getId().toString());
            yml.set(path + ".owner", data.getOwner().toString());
            yml.set(path + ".world", data.getLocation().getWorld().getName());
            yml.set(path + ".x", data.getLocation().getBlockX());
            yml.set(path + ".y", data.getLocation().getBlockY());
            yml.set(path + ".z", data.getLocation().getBlockZ());
            yml.set(path + ".storage", serializeInventory(data.getStorage()));
            index++;
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan data collectors.yml", e);
        }
    }

    public void load(CollectorManager manager) {
        manager.clearAll();
        if (!file.exists()) return;

        FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.contains("collectors")) return;

        for (String key : yml.getConfigurationSection("collectors").getKeys(false)) {
            String path = "collectors." + key;
            try {
                UUID id = UUID.fromString(yml.getString(path + ".id"));
                UUID owner = UUID.fromString(yml.getString(path + ".owner"));
                String worldName = yml.getString(path + ".world");
                int x = yml.getInt(path + ".x");
                int y = yml.getInt(path + ".y");
                int z = yml.getInt(path + ".z");

                if (Bukkit.getWorld(worldName) == null) continue; // dunia tidak ada, skip

                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                int chunkKey = 0;
                long ck = CollectorManager.chunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, worldName);

                int size = plugin.getConfig().getInt("storage-size", 45);
                Inventory inv = Bukkit.createInventory(null, size, "Chunk Collector Storage");
                String encoded = yml.getString(path + ".storage");
                if (encoded != null && !encoded.isEmpty()) {
                    ItemStack[] items = deserializeInventory(encoded, size);
                    inv.setContents(items);
                }

                CollectorData data = new CollectorData(id, owner, loc, ck, inv);
                manager.register(data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Gagal memuat salah satu data collector, dilewati.", e);
            }
        }
        plugin.getLogger().info("Berhasil memuat " + manager.getAll().size() + " Chunk Collector dari penyimpanan.");
    }

    private String serializeInventory(Inventory inventory) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(out)) {

            ItemStack[] contents = inventory.getContents();
            dataOutput.writeInt(contents.length);
            for (ItemStack item : contents) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal serialize inventory collector", e);
            return "";
        }
    }

    private ItemStack[] deserializeInventory(String data, int fallbackSize) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(in)) {

            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal deserialize inventory collector", e);
            return new ItemStack[fallbackSize];
        }
    }
}
