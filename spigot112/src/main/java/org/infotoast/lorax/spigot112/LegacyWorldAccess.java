package org.infotoast.lorax.spigot112;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.FlowerPot;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.WorldAccess;

import java.util.Locale;

@SuppressWarnings("deprecation")
public final class LegacyWorldAccess implements WorldAccess {
    private final World world;
    private final int chunkX, chunkZ;
    private final boolean manual;

    public LegacyWorldAccess(World world, int chunkX, int chunkZ, boolean manual) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.manual = manual;
    }
    public long seed() { return world.getSeed(); }
    public int minHeight() { return 0; }
    public int maxHeight() { return world.getMaxHeight(); }
    public boolean contains(int x, int y, int z) {
        return y >= 0 && y < maxHeight() && (manual || (Math.abs((x >> 4) - chunkX) <= 1 && Math.abs((z >> 4) - chunkZ) <= 1))
                && world.isChunkLoaded(x >> 4, z >> 4);
    }
    private Block at(int x, int y, int z) {
        if (!contains(x, y, z)) throw new IllegalArgumentException("Outside loaded legacy placement context: " + x + "," + y + "," + z);
        return world.getBlockAt(x, y, z);
    }
    public int surface(int x, int z) {
        if (!contains(x, 0, z)) return 0;
        for (int y = Math.min(world.getHighestBlockYAt(x, z), maxHeight() - 1); y > 30; y--) {
            switch (at(x, y, z).getType()) {
                case GRASS: case DIRT: case SAND: case GRAVEL: case SNOW: case SNOW_BLOCK:
                case CLAY: case STONE: case HARD_CLAY: case STAINED_CLAY: case MYCEL: case SANDSTONE:
                case RED_SANDSTONE: case GRASS_PATH: return y + 1 < maxHeight() ? y + 1 : 0;
                default: break;
            }
        }
        return 0;
    }
    public String biome(int x, int y, int z) { return world.getBiome(x, z).name().toLowerCase(Locale.ROOT); }
    public String block(int x, int y, int z) {
        Material material = at(x,y,z).getType();
        return "minecraft:" + (material == Material.GRASS ? "grass_block" : material.name().toLowerCase(Locale.ROOT));
    }
    public boolean replaceable(int x, int y, int z) {
        switch (at(x,y,z).getType()) {
            case AIR: case LEAVES: case LEAVES_2: case SNOW: case LONG_GRASS: case DEAD_BUSH: case VINE: return true;
            default: return false;
        }
    }
    public void validate(org.infotoast.lorax.common.BlockState state) {
        LegacyBlocks block = LegacyBlocks.map(state);
        if (Material.getMaterial(block.id) == null) throw new IllegalArgumentException("Unknown legacy ID " + block.id);
    }
    public void set(int x, int y, int z, org.infotoast.lorax.common.BlockState state) {
        LegacyBlocks mapped = LegacyBlocks.map(state);
        Block block = at(x,y,z);
        block.setTypeIdAndData(mapped.id, (byte) mapped.metadata, false);
        if (mapped.tile == LegacyBlocks.Tile.PLAYER_HEAD) {
            Skull skull = (Skull) block.getState();
            skull.setSkullType(SkullType.PLAYER);
            if (!skull.update(true, false)) throw new IllegalStateException("Could not initialize player head");
        } else if (mapped.tile == LegacyBlocks.Tile.POTTED_ALLIUM) {
            FlowerPot pot = (FlowerPot) block.getState();
            pot.setContents(new org.bukkit.material.MaterialData(Material.RED_ROSE, (byte) 2));
            if (!pot.update(true, false)) throw new IllegalStateException("Could not initialize allium pot");
        }
    }
}
