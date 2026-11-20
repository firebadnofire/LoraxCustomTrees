package org.infotoast.lorax.forge112.probe;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.forge112.ForgeLorax;
import org.infotoast.lorax.forge112.ForgeWorldAccess;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Test-only dimensions and assertions; never packaged with Lorax. */
@Mod(modid="loraxprobe",name="Lorax Integration Probe",version="1",dependencies="required-after:lorax",serverSideOnly=true,acceptableRemoteVersions="*")
public final class ForgeProbe {
    private int assertions;
    private org.apache.logging.log4j.Logger logger;
    private void require(boolean condition,String message) {
        assertions++;
        if(!condition) throw new AssertionError(message);
    }
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        logger=event.getModLog();
        for(int id=101;id<=103;id++) {
            DimensionType type=DimensionType.register("LoraxProbe"+id,"_lorax_probe_"+id,id,Plateau.class,false);
            DimensionManager.registerDimension(id,type);
        }
    }
    public static final class Plateau extends WorldProviderSurface {
        @Override protected void init() {
            biomeProvider=new BiomeProviderSingle(Biomes.FOREST);
            hasSkyLight=true;
        }
        @Override public DimensionType getDimensionType() { return DimensionManager.getProviderType(getDimension()); }
        @Override public long getSeed() { return getDimension()==103?987654:812345; }
        @Override public IChunkGenerator createChunkGenerator() {
            return new ChunkGeneratorFlat(world,getSeed(),false,"3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;4;");
        }
    }
    private WorldServer world(int id) {
        DimensionManager.initDimension(id);
        WorldServer world=DimensionManager.getWorld(id);
        require(world!=null,"create dimension "+id);
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.getChunkFromChunkCoords(x,z);
        return world;
    }
    private long digest(WorldServer world) {
        int logs=0;
        long hash=1;
        for(int x=-16;x<32;x++) for(int z=-16;z<32;z++) for(int y=64;y<115;y++) {
            int id=Block.getIdFromBlock(world.getBlockState(new BlockPos(x,y,z)).getBlock());
            if(id==17 || id==162) logs++;
            hash=hash*31+id;
        }
        require(logs>0,"native Forge automatic generation in "+world.provider.getDimension());
        return hash;
    }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        Path result=Paths.get("lorax-test","result.json");
        try {
            Files.createDirectories(result.getParent());
            require(ForgeLorax.structures.size()==40,"all 40 structures");
            WorldServer first=world(101);
            long before=digest(first);
            WorldServer second=world(102);
            boolean layoutReproducible=before==digest(second);
            require(candidates(ForgeLorax.structures,new ForgeWorldAccess(first,0,0,true))
                    .equals(candidates(ForgeLorax.structures,new ForgeWorldAccess(second,0,0,true))),"same-seed candidate selection");
            require(before==digest(first),"dimension isolation");
            digest(world(103));
            for(Structure structure:ForgeLorax.structures.values()) for(int rotation=0;rotation<4;rotation++) {
                for(int x=-1;x<=2;x++) for(int z=-1;z<=2;z++) first.getChunkFromChunkCoords(x,z);
                Position center=new Position(15,180,15);
                require(Placement.place(structure,center,rotation,new ForgeWorldAccess(first,0,0,true),true)==Placement.Result.PLACED,"place "+structure.name);
                for(Structure.Entry entry:structure.entries) {
                    if(entry.kind!=Structure.Kind.BLOCK) continue;
                    int turns=structure.rotateRandomly?rotation:0;
                    Position position=entry.position.rotate(turns).add(center);
                    LegacyBlocks mapped=LegacyBlocks.map(entry.blocks.get(0).rotate(turns));
                    BlockPos location=new BlockPos(position.x,position.y,position.z);
                    IBlockState actual=first.getBlockState(location);
                    int mask=mapped.id==18 || mapped.id==161?7:15;
                    require(Block.getIdFromBlock(actual.getBlock())==mapped.id && (actual.getBlock().getMetaFromState(actual)&mask)==(mapped.metadata&mask),structure.name+" block mismatch at "+location);
                    if(mapped.tile==LegacyBlocks.Tile.PLAYER_HEAD) require(((TileEntitySkull)first.getTileEntity(location)).getSkullType()==3,"player head tile");
                    if(mapped.tile==LegacyBlocks.Tile.POTTED_ALLIUM) require(((TileEntityFlowerPot)first.getTileEntity(location)).getFlowerPotData()==2,"allium pot tile");
                }
            }
            Files.write(result,("{\"passed\":true,\"assertions\":"+assertions+",\"assets\":40,\"candidate_determinism\":true,\"layout_reproducible\":"+layoutReproducible+"}").getBytes(StandardCharsets.UTF_8));
            logger.info("LORAX_INTEGRATION_PASSED assertions={}",assertions);
        } catch(Throwable error) {
            logger.error("LORAX_INTEGRATION_FAILED",error);
            try { Files.write(result,"{\"passed\":false}".getBytes(StandardCharsets.UTF_8)); }
            catch(Exception failure) { logger.error("Cannot save probe result",failure); }
        }
    }
    private static String candidates(java.util.Map<String,Structure> catalog, org.infotoast.lorax.common.WorldAccess world) {
        StringBuilder selected=new StringBuilder();
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            for(org.infotoast.lorax.common.Generation.Candidate candidate:org.infotoast.lorax.common.Generation.select(catalog,world,x,z)) {
                selected.append(candidate.structure.name).append(':').append(candidate.center.x).append(',')
                        .append(candidate.center.y).append(',').append(candidate.center.z).append('/').append(candidate.rotation).append(';');
            }
        }
        return selected.toString();
    }
}
