package com.lifesteal.chunkcollector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectorManager {

    private final ChunkCollectorPlugin plugin;

    // ID collector -> data
    private final Map<UUID, CollectorData> collectors = new ConcurrentHashMap<>();

    // chunkKey -> daftar id collector di chunk tsb
    private final Map<Long, Set<UUID>> byChunk = new ConcurrentHashMap<>();

    // pemilik -> daftar id collector miliknya
    private final Map<UUID, Set<UUID>> byOwner = new ConcurrentHashMap<>();

    // lokasi block (serialized) -> id collector, untuk lookup cepat saat interact/break
    private final Map<String, UUID> byLocation = new ConcurrentHashMap<>();

    public CollectorManager(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;
    }

    public static long chunkKey(int chunkX, int chunkZ, String world) {
        // Gabungkan world hash dengan koordinat chunk menjadi satu key unik
        long worldPart = ((long) world.hashCode()) << 32;
        long x = chunkX & 0xFFFFL;
        long z = chunkZ & 0xFFFFL;
        return worldPart ^ (x << 16) ^ z;
    }

    public static String locationKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public int getLimit(Player player) {
        if (player.hasPermission("chunkcollector.admin")) {
            return Integer.MAX_VALUE;
        }
        int best = plugin.getConfig().getInt("default-limit", 1);
        for (int i = 1; i <= 1000; i++) {
            if (player.hasPermission("chunkcollector.limit." + i)) {
                best = Math.max(best, i);
            }
        }
        return best;
    }

    public int getOwnedCount(UUID owner) {
        return byOwner.getOrDefault(owner, Collections.emptySet()).size();
    }

    public boolean canPlace(Player player) {
        return getOwnedCount(player.getUniqueId()) < getLimit(player);
    }

    public boolean hasCollectorInChunk(int chunkX, int chunkZ, String world) {
        long key = chunkKey(chunkX, chunkZ, world);
        Set<UUID> set = byChunk.get(key);
        return set != null && !set.isEmpty();
    }

    public void register(CollectorData data) {
        collectors.put(data.getId(), data);
        byChunk.computeIfAbsent(data.getChunkKey(), k -> ConcurrentHashMap.newKeySet()).add(data.getId());
        byOwner.computeIfAbsent(data.getOwner(), k -> ConcurrentHashMap.newKeySet()).add(data.getId());
        byLocation.put(locationKey(data.getLocation()), data.getId());
    }

    public void unregister(CollectorData data) {
        collectors.remove(data.getId());
        Set<UUID> chunkSet = byChunk.get(data.getChunkKey());
        if (chunkSet != null) {
            chunkSet.remove(data.getId());
            if (chunkSet.isEmpty()) byChunk.remove(data.getChunkKey());
        }
        Set<UUID> ownerSet = byOwner.get(data.getOwner());
        if (ownerSet != null) {
            ownerSet.remove(data.getId());
            if (ownerSet.isEmpty()) byOwner.remove(data.getOwner());
        }
        byLocation.remove(locationKey(data.getLocation()));
    }

    public CollectorData getByLocation(Location loc) {
        UUID id = byLocation.get(locationKey(loc));
        return id == null ? null : collectors.get(id);
    }

    /** Ambil collector pertama yang aktif pada chunk tertentu (dipakai saat item drop). */
    public CollectorData getFirstInChunk(int chunkX, int chunkZ, String world) {
        long key = chunkKey(chunkX, chunkZ, world);
        Set<UUID> set = byChunk.get(key);
        if (set == null || set.isEmpty()) return null;
        UUID id = set.iterator().next();
        return collectors.get(id);
    }

    public Collection<CollectorData> getAll() {
        return collectors.values();
    }

    public void clearAll() {
        collectors.clear();
        byChunk.clear();
        byOwner.clear();
        byLocation.clear();
    }
}
