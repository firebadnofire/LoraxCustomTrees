package org.infotoast.lorax.forge12110.probe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import org.infotoast.lorax.common.Generation;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.forge12110.ForgeLorax;
import org.infotoast.lorax.forge12110.ForgeWorldAccess;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod("loraxprobe")
public final class ForgeProbe {
    private int assertions;
    private final org.slf4j.Logger logger=com.mojang.logging.LogUtils.getLogger();
    public ForgeProbe() { ServerStartedEvent.BUS.addListener(this::run); }
    private void require(boolean condition,String message) {
        assertions++;
        if(!condition) throw new AssertionError(message);
    }
    private ServerLevel world(MinecraftServer server,String name) {
        var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("loraxprobe",name));
        var world=server.getLevel(key);
        require(world!=null,"test dimension registered "+name);
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) world.getChunk(x,z);
        return world;
    }
    private long digest(ServerLevel world) {
        int custom=0;
        long hash=1;
        for(int x=-48;x<64;x++) for(int z=-48;z<64;z++) for(int y=0;y<130;y++) {
            String name=BuiltInRegistries.BLOCK.getKey(world.getBlockState(new BlockPos(x,y,z)).getBlock()).toString();
            // Native forest trees cannot create these bundled custom oak decorations.
            if(name.equals("minecraft:jungle_leaves") || name.equals("minecraft:spruce_leaves") || name.equals("minecraft:oak_wood")) custom++;
            hash=hash*31+name.hashCode();
        }
        require(custom>0,"native feature placed custom tree content in "+world.dimension());
        return hash;
    }
    private String candidates(ServerLevel world) {
        StringBuilder result=new StringBuilder();
        for(var candidate:Generation.select(ForgeLorax.structures,new ForgeWorldAccess(world),0,0)) {
            result.append(candidate.structure.name).append(candidate.center.x).append(',').append(candidate.center.y).append(',').append(candidate.center.z).append('/').append(candidate.rotation).append(';');
        }
        return result.toString();
    }
    private void run(ServerStartedEvent event) {
        Path result=Path.of("lorax-test","result.json");
        try {
            Files.createDirectories(result.getParent());
            require(ForgeLorax.structures.size()==40,"all 40 structures initialized");
            var first=world(event.getServer(),"a");
            long before=digest(first);
            var second=world(event.getServer(),"b");
            boolean layout=before==digest(second);
            require(candidates(first).equals(candidates(first)),"candidate selection deterministic");
            require(before==digest(first),"dimension isolation");
            for(Structure structure:ForgeLorax.structures.values()) for(int rotation=0;rotation<4;rotation++) {
                for(int x=-1;x<=2;x++) for(int z=-1;z<=2;z++) first.getChunk(x,z);
                Position center=new Position(15,180,15);
                require(Placement.place(structure,center,rotation,new ForgeWorldAccess(first),true)==Placement.Result.PLACED,"place "+structure.name);
                for(var entry:structure.entries) {
                    if(entry.kind!=Structure.Kind.BLOCK) continue;
                    int turns=structure.rotateRandomly?rotation:0;
                    var position=entry.position.rotate(turns).add(center);
                    var expected=ForgeWorldAccess.parse(entry.blocks.get(0).rotate(turns));
                    var actual=first.getBlockState(new BlockPos(position.x,position.y,position.z));
                    require(actual.equals(expected),structure.name+" block mismatch at "+position.x+","+position.y+","+position.z+": "+actual+" expected "+expected);
                }
            }
            Files.writeString(result,"{\"passed\":true,\"assertions\":"+assertions+",\"assets\":40,\"candidate_determinism\":true,\"layout_reproducible\":"+layout+"}",StandardCharsets.UTF_8);
            logger.info("LORAX_INTEGRATION_PASSED assertions={}",assertions);
        } catch(Throwable error) {
            logger.error("LORAX_INTEGRATION_FAILED",error);
            try { Files.writeString(result,"{\"passed\":false}"); }
            catch(Exception failure) { logger.error("Cannot save probe result",failure); }
        }
    }
}


