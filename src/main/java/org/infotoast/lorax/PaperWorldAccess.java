package org.infotoast.lorax;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.infotoast.lorax.common.BlockState;
import org.infotoast.lorax.common.WorldAccess;
import org.infotoast.lorax.util.Utility;

import java.util.HashMap;
import java.util.Map;

/** Callback-local adapter. Never stored or scheduled. */
public final class PaperWorldAccess implements WorldAccess {
    private final WorldInfo info;
    private final LimitedRegion region;
    private final World manualWorld;
    private final Map<String, BlockData> states = new HashMap<>();

    public PaperWorldAccess(WorldInfo info, LimitedRegion region) {
        this.info = info;
        this.region = region;
        this.manualWorld = null;
    }

    public PaperWorldAccess(World world) {
        this.info = world;
        this.region = null;
        this.manualWorld = world;
    }

    @Override public long seed() { return info.getSeed(); }
    @Override public int minHeight() { return info.getMinHeight(); }
    @Override public int maxHeight() { return info.getMaxHeight(); }
    @Override public boolean contains(int x, int y, int z) {
        return y >= minHeight() && y < maxHeight() && (region != null
                ? region.isInRegion(x, y, z) : manualWorld.isChunkLoaded(x >> 4, z >> 4));
    }
    private Material material(int x, int y, int z) {
        if (!contains(x, y, z)) throw new IllegalArgumentException("Block outside placement context: " + x + "," + y + "," + z);
        return region != null ? region.getType(x, y, z) : manualWorld.getBlockAt(x, y, z).getType();
    }
    @Override public int surface(int x, int z) {
        if (!contains(x, minHeight(), z)) return 0;
        int highest = region != null ? region.getHighestBlockYAt(x, z) : manualWorld.getHighestBlockYAt(x, z);
        for (int y = Math.min(highest, maxHeight() - 1); y > Math.max(30, minHeight()); y--) {
            if (Utility.isGroundBlock(material(x, y, z))) return y + 1 < maxHeight() ? y + 1 : 0;
        }
        return 0;
    }
    @Override public String biome(int x, int y, int z) {
        return (region != null ? region.getBiome(x, y, z) : manualWorld.getBiome(x, y, z)).getKey().getKey();
    }
    @Override public String block(int x, int y, int z) { return material(x, y, z).getKey().toString(); }
    @Override public boolean replaceable(int x, int y, int z) {
        Material type = material(x, y, z);
        return type.isAir() || type.name().endsWith("_LEAVES") || type == Material.SNOW
                || type.name().equals("SHORT_GRASS") || type.name().equals("GRASS") || type == Material.TALL_GRASS || type == Material.FERN
                || type == Material.LARGE_FERN || type == Material.VINE || type == Material.DEAD_BUSH;
    }
    private BlockData data(BlockState state) {
        return states.computeIfAbsent(state.toString(), key -> PaperBlocks.data(state));
    }
    @Override public void validate(BlockState state) { data(state); }
    @Override public void set(int x, int y, int z, BlockState state) {
        if (!contains(x, y, z)) throw new IllegalArgumentException("Write outside placement context");
        if (region != null) region.setBlockData(x, y, z, data(state).clone());
        else manualWorld.getBlockAt(x, y, z).setBlockData(data(state).clone(), false);
    }
}
