package org.infotoast.lorax;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.generator.WorldInfo;

import java.util.*;

public class WorldImproved {
    public static final HashMap<String, WorldImproved> WORLDS = new HashMap<>();
    private final String worldName;
    private final long seed;

    private WorldImproved(String name, long seed) {
        this.worldName = name;
        this.seed = seed;
    }

    private WorldImproved(World world) {
        worldName = world.getName();
        seed = world.getSeed();
    }

    private WorldImproved(WorldInfo world) {
        worldName = world.getName();
        seed = world.getSeed();
    }

    public static WorldImproved get(World world) {
        return WORLDS.computeIfAbsent(world.getName(), (k) -> new WorldImproved(world));
    }

    public static WorldImproved get(WorldInfo world) {
        return WORLDS.computeIfAbsent(world.getName(), (k) -> new WorldImproved(world));
    }

    public static WorldImproved get(String name, long seed) {
        return WORLDS.computeIfAbsent(name, (k) -> new WorldImproved(name, seed));
    }

    public long getSeed() {
        return seed;
    }

    public Random getRand(long d) {
        return new Random(seed * d);
    }

    public Random getHashedRand(long x, int y, int z) {
        return new Random(Objects.hash(seed, x, y, z));
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }





}
