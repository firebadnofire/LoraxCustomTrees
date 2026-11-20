package org.infotoast.lorax.forge12110;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.infotoast.lorax.common.BlockState;
import org.infotoast.lorax.common.Ground;
import org.infotoast.lorax.common.WorldAccess;

import java.util.HashMap;
import java.util.Map;

public final class ForgeWorldAccess implements WorldAccess {
    private final WorldGenLevel world;
    private final Map<String,net.minecraft.world.level.block.state.BlockState> states = new HashMap<>();
    public ForgeWorldAccess(WorldGenLevel world) { this.world=world; }
    public long seed() { return world.getSeed(); }
    public int minHeight() { return world.getMinY(); }
    public int maxHeight() { return world.getMaxY()+1; }
    public boolean contains(int x,int y,int z) {
        if(y<minHeight() || y>=maxHeight()) return false;
        BlockPos position=new BlockPos(x,y,z);
        if(world instanceof WorldGenRegion region) return region.hasChunk(x>>4,z>>4) && Math.abs((x>>4)-region.getCenter().x)<=1 && Math.abs((z>>4)-region.getCenter().z)<=1 && region.ensureCanWrite(position);
        if(world instanceof ServerLevel level) return level.getChunkSource().hasChunk(x>>4,z>>4);
        return false;
    }
    private net.minecraft.world.level.block.state.BlockState at(int x,int y,int z) {
        if(!contains(x,y,z)) throw new IllegalArgumentException("Outside native Forge placement context");
        return world.getBlockState(new BlockPos(x,y,z));
    }
    public int surface(int x,int z) {
        if(!contains(x,minHeight(),z)) return 0;
        int top=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
        for(int y=Math.min(top,maxHeight()-1);y>Math.max(30,minHeight());y--) {
            if(Ground.allows(block(x,y,z))) return y+1<maxHeight()?y+1:0;
        }
        return 0;
    }
    public String biome(int x,int y,int z) { return world.getBiome(new BlockPos(x,y,z)).unwrapKey().orElseThrow().location().toString(); }
    public String block(int x,int y,int z) { return BuiltInRegistries.BLOCK.getKey(at(x,y,z).getBlock()).toString(); }
    public boolean replaceable(int x,int y,int z) {
        String name=block(x,y,z);
        return at(x,y,z).isAir() || name.endsWith("_leaves") || name.equals("minecraft:snow") || name.equals("minecraft:short_grass")
                || name.equals("minecraft:tall_grass") || name.equals("minecraft:fern") || name.equals("minecraft:large_fern")
                || name.equals("minecraft:vine") || name.equals("minecraft:dead_bush");
    }
    public static net.minecraft.world.level.block.state.BlockState parse(BlockState state) {
        try { return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK,state.toString(),false).blockState(); }
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalArgumentException("Unsupported Forge block state: "+state,error); }
    }
    private net.minecraft.world.level.block.state.BlockState data(BlockState state) { return states.computeIfAbsent(state.toString(),key->parse(state)); }
    public void validate(BlockState state) { data(state); }
    public void set(int x,int y,int z,BlockState state) {
        if(!contains(x,y,z)) throw new IllegalArgumentException("Write outside native Forge context");
        BlockPos position=new BlockPos(x,y,z);
        var target=data(state);
        int flags=net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        if(!world.getBlockState(position).equals(target) && !world.setBlock(position,target,flags)) throw new IllegalStateException("Forge refused block write at "+position);
    }
}


