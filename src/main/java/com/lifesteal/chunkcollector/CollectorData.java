package com.lifesteal.chunkcollector;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * Merepresentasikan satu Chunk Collector yang sudah ditempatkan di dunia.
 */
public class CollectorData {

    private final UUID id;
    private final UUID owner;
    private final Location location;
    private final long chunkKey;
    private Inventory storage;

    public CollectorData(UUID id, UUID owner, Location location, long chunkKey, Inventory storage) {
        this.id = id;
        this.owner = owner;
        this.location = location;
        this.chunkKey = chunkKey;
        this.storage = storage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public long getChunkKey() {
        return chunkKey;
    }

    public Inventory getStorage() {
        return storage;
    }

    public void setStorage(Inventory storage) {
        this.storage = storage;
    }
}
