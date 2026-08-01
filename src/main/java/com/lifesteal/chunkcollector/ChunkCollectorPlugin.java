package com.lifesteal.chunkcollector;

import com.lifesteal.chunkcollector.commands.CollectorCommand;
import com.lifesteal.chunkcollector.gui.CollectorGUI;
import com.lifesteal.chunkcollector.hooks.VaultHook;
import com.lifesteal.chunkcollector.listeners.BlockListener;
import com.lifesteal.chunkcollector.listeners.GUIListener;
import com.lifesteal.chunkcollector.listeners.ItemSpawnListener;
import com.lifesteal.chunkcollector.storage.DataStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkCollectorPlugin extends JavaPlugin {

    private CollectorManager collectorManager;
    private DataStorage dataStorage;
    private ItemFactory itemFactory;
    private CollectorGUI collectorGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.collectorManager = new CollectorManager(this);
        this.dataStorage = new DataStorage(this);
        this.itemFactory = new ItemFactory(this);
        this.collectorGUI = new CollectorGUI(this);

        // Setup Vault (opsional, hanya untuk fitur Sell All)
        if (VaultHook.setup()) {
            getLogger().info("Vault economy terdeteksi, fitur Sell All aktif.");
        } else {
            getLogger().warning("Vault economy TIDAK ditemukan. Fitur Sell All di GUI tidak akan berfungsi sampai Vault + plugin economy terpasang.");
        }

        // Muat data collector yang tersimpan
        dataStorage.load(collectorManager);

        // Registrasi listener
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Registrasi command
        getCommand("collector").setExecutor(new CollectorCommand(this));

        getLogger().info("ChunkCollector berhasil aktif dengan " + collectorManager.getAll().size() + " collector terdaftar.");
    }

    @Override
    public void onDisable() {
        if (dataStorage != null && collectorManager != null) {
            dataStorage.save(collectorManager);
        }
        getLogger().info("ChunkCollector dinonaktifkan, data tersimpan.");
    }

    public CollectorManager getCollectorManager() {
        return collectorManager;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    public CollectorGUI getCollectorGUI() {
        return collectorGUI;
    }
}
