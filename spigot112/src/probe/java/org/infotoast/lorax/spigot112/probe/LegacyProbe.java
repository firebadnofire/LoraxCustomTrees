package org.infotoast.lorax.spigot112.probe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.Skull;
import org.bukkit.SkullType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.spigot112.LegacyLorax;
import org.infotoast.lorax.spigot112.LegacyWorldAccess;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

@SuppressWarnings("deprecation")
public final class LegacyProbe extends JavaPlugin {
    private int assertions;
    private boolean layoutReproducible;
    private void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, new Runnable() {
            public void run() {
                try {
                    Files.createDirectories(getDataFolder().toPath());
                    checks();
                    Files.write(getDataFolder().toPath().resolve("result.json"), ("{\"passed\":true,\"assertions\":" + assertions + ",\"assets\":40,\"candidate_determinism\":true,\"layout_reproducible\":" + layoutReproducible + "}").getBytes(StandardCharsets.UTF_8));
                    getLogger().info("LORAX_INTEGRATION_PASSED assertions=" + assertions);
                } catch (Throwable error) {
                    getLogger().log(java.util.logging.Level.SEVERE, "LORAX_INTEGRATION_FAILED", error);
                    try { Files.write(getDataFolder().toPath().resolve("result.json"), "{\"passed\":false}".getBytes(StandardCharsets.UTF_8)); }
                    catch (Exception failure) { getLogger().log(java.util.logging.Level.SEVERE, "Cannot save assertion result", failure); }
                }
            }
        });
    }
    private static final class Plateau extends ChunkGenerator {
        @Override public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biomes) {
            ChunkData data = createChunkData(world);
            data.setRegion(0,0,0,16,60,16,Material.STONE);
            data.setRegion(0,60,0,16,63,16,Material.DIRT);
            data.setRegion(0,63,0,16,64,16,Material.GRASS);
            for (int bx=0; bx<16; bx++) for (int bz=0; bz<16; bz++) biomes.setBiome(bx,bz,Biome.FOREST);
            return data;
        }
    }
    private World world(String name, long seed) {
        World world = new WorldCreator(name).seed(seed).generator(new Plateau()).createWorld();
        require(world != null, "create " + name);
        require(!world.getPopulators().isEmpty(), "native population attached");
        for (int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.getChunkAt(x,z);
        return world;
    }
    private long digest(World world) {
        long hash=1;
        int logs=0;
        for(int x=-16;x<32;x++) for(int z=-16;z<32;z++) for(int y=64;y<115;y++) {
            int type=world.getBlockAt(x,y,z).getTypeId();
            if(type==17 || type==162) logs++;
            hash=hash*31+type;
        }
        require(logs>0, "natural custom trees in " + world.getName());
        return hash;
    }
    private void checks() {
        LegacyLorax lorax=(LegacyLorax) Bukkit.getPluginManager().getPlugin("Lorax");
        require(lorax != null && lorax.isEnabled(), "Lorax enabled");
        require(lorax.getStructures().size()==40, "all 40 assets");
        World first=world("lorax_probe_a",812345);
        long before=digest(first);
        World second=world("lorax_probe_b",812345);
        layoutReproducible=before==digest(second);
        require(candidates(lorax.getStructures(),new LegacyWorldAccess(first,0,0,true))
                .equals(candidates(lorax.getStructures(),new LegacyWorldAccess(second,0,0,true))), "same-seed candidate selection");
        require(before==digest(first), "multiworld isolation");
        digest(world("lorax_probe_c",987654));
        for(Structure structure:lorax.getStructures().values()) for(int rotation=0;rotation<4;rotation++) {
            for(int x=-1;x<=2;x++) for(int z=-1;z<=2;z++) first.getChunkAt(x,z);
            Position center=new Position(15,180,15);
            require(Placement.place(structure,center,rotation,new LegacyWorldAccess(first,0,0,true),true)==Placement.Result.PLACED, "place " + structure.name);
            for(Structure.Entry entry:structure.entries) {
                if(entry.kind!=Structure.Kind.BLOCK) continue;
                int turns=structure.rotateRandomly?rotation:0;
                Position position=entry.position.rotate(turns).add(center);
                LegacyBlocks mapped=LegacyBlocks.map(entry.blocks.get(0).rotate(turns));
                org.bukkit.block.Block actual=first.getBlockAt(position.x,position.y,position.z);
                // Neighbor updates toggle CHECK_DECAY (bit 8), even on non-decaying leaves.
                int mask = mapped.id == 18 || mapped.id == 161 ? 7 : 15;
                require(actual.getTypeId()==mapped.id && (actual.getData() & mask)==(mapped.metadata & mask),
                        structure.name+" mismatch " + position.x+","+position.y+","+position.z+" actual "+actual.getTypeId()+":"+actual.getData()+" expected "+mapped.id+":"+mapped.metadata);
                if(mapped.tile==LegacyBlocks.Tile.PLAYER_HEAD) require(((Skull)actual.getState()).getSkullType()==SkullType.PLAYER,"player head tile");
                if(mapped.tile==LegacyBlocks.Tile.POTTED_ALLIUM) {
                    org.bukkit.block.FlowerPot pot=(org.bukkit.block.FlowerPot)actual.getState();
                    require(pot.getContents().getItemType()==Material.RED_ROSE && pot.getContents().getData()==2,"allium pot tile");
                }
            }
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
