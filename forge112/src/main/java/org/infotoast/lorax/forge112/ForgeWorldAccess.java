package org.infotoast.lorax.forge112;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.infotoast.lorax.common.BlockState;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.WorldAccess;

public final class ForgeWorldAccess implements WorldAccess {
    private final World world;
    private final int chunkX, chunkZ;
    private final boolean manual;
    public ForgeWorldAccess(World world, int chunkX, int chunkZ, boolean manual) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.manual = manual;
    }
    public long seed() { return world.getSeed(); }
    public int minHeight() { return 0; }
    public int maxHeight() { return world.getHeight(); }
    public boolean contains(int x, int y, int z) {
        return y >= 0 && y < maxHeight() && (manual || (Math.abs((x >> 4) - chunkX) <= 1 && Math.abs((z >> 4) - chunkZ) <= 1))
                && world.isBlockLoaded(new BlockPos(x,y,z), false);
    }
    private IBlockState at(int x, int y, int z) {
        if (!contains(x,y,z)) throw new IllegalArgumentException("Outside loaded Forge placement context");
        return world.getBlockState(new BlockPos(x,y,z));
    }
    public int surface(int x, int z) {
        if (!contains(x,0,z)) return 0;
        for (int y=Math.min(world.getHeight(new BlockPos(x,0,z)).getY(),maxHeight()-1);y>30;y--) {
            Block block=at(x,y,z).getBlock();
            if (block==Blocks.GRASS || block==Blocks.DIRT || block==Blocks.SAND || block==Blocks.GRAVEL
                    || block==Blocks.SNOW_LAYER || block==Blocks.SNOW || block==Blocks.CLAY || block==Blocks.STONE
                    || block==Blocks.HARDENED_CLAY || block==Blocks.STAINED_HARDENED_CLAY || block==Blocks.MYCELIUM
                    || block==Blocks.SANDSTONE || block==Blocks.RED_SANDSTONE || block==Blocks.GRASS_PATH) return y+1 < maxHeight()? y+1:0;
        }
        return 0;
    }
    public String biome(int x, int y, int z) { return world.getBiome(new BlockPos(x,y,z)).getRegistryName().toString(); }
    public String block(int x, int y, int z) { return at(x,y,z).getBlock().getRegistryName().toString(); }
    public boolean replaceable(int x, int y, int z) {
        Block block=at(x,y,z).getBlock();
        return block==Blocks.AIR || block==Blocks.LEAVES || block==Blocks.LEAVES2 || block==Blocks.SNOW_LAYER
                || block==Blocks.TALLGRASS || block==Blocks.DEADBUSH || block==Blocks.VINE;
    }
    public void validate(BlockState state) { LegacyBlocks.map(state); }
    public void set(int x, int y, int z, BlockState state) {
        if (!contains(x,y,z)) throw new IllegalArgumentException("Write outside loaded Forge placement context");
        LegacyBlocks mapped=LegacyBlocks.map(state);
        Block block=Block.getBlockById(mapped.id);
        BlockPos position=new BlockPos(x,y,z);
        IBlockState target=block.getStateFromMeta(mapped.metadata);
        if (!world.getBlockState(position).equals(target) && !world.setBlockState(position,target,2)) {
            throw new IllegalStateException("Forge refused block write at " + position);
        }
        if (mapped.tile!=LegacyBlocks.Tile.NONE) {
            TileEntity tile=world.getTileEntity(position);
            if (mapped.tile==LegacyBlocks.Tile.PLAYER_HEAD && tile instanceof TileEntitySkull) {
                ((TileEntitySkull)tile).setType(3);
            } else if (mapped.tile==LegacyBlocks.Tile.POTTED_ALLIUM && tile instanceof TileEntityFlowerPot) {
                ((TileEntityFlowerPot)tile).setItemStack(new ItemStack(Item.getItemFromBlock(Blocks.RED_FLOWER),1,2));
            } else throw new IllegalStateException("Missing expected tile entity at " + position);
            tile.markDirty();
        }
    }
}
