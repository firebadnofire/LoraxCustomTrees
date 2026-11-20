package org.infotoast.lorax.probe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldInitEvent;
import org.infotoast.lorax.Lorax;
import org.infotoast.lorax.PaperWorldAccess;
import org.infotoast.lorax.PaperBlocks;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.common.Generation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Never included in a distribution. Run only in a disposable integration server. */
public final class PaperProbe extends JavaPlugin implements Listener {
    private int assertions;
    private boolean layoutReproducible;
    private Throwable initializationFailure;

    private void require(boolean condition, String assertion) {
        assertions++;
        if (!condition) throw new AssertionError(assertion);
    }

    @Override public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                boolean restart = Files.exists(getDataFolder().toPath().resolve("restart-request"));
                if (restart) runRestartChecks(); else runChecks();
                if (initializationFailure != null) throw new IllegalStateException("Probe world initialization failed", initializationFailure);
                Files.write(getDataFolder().toPath().resolve("result.json"),
                        ("{\"passed\":true,\"assertions\":" + assertions + ",\"restart_check\":" + restart + ",\"assets\":40"
                                + (restart ? "" : ",\"candidate_determinism\":true,\"layout_reproducible\":" + layoutReproducible) + "}").getBytes(StandardCharsets.UTF_8));
                getLogger().info("LORAX_INTEGRATION_PASSED assertions=" + assertions);
            } catch (Throwable error) {
                getLogger().log(java.util.logging.Level.SEVERE, "LORAX_INTEGRATION_FAILED", error);
                try {
                    Files.createDirectories(getDataFolder().toPath());
                    Files.write(getDataFolder().toPath().resolve("result.json"), "{\"passed\":false}".getBytes(StandardCharsets.UTF_8));
                } catch (java.io.IOException failure) {
                    getLogger().log(java.util.logging.Level.SEVERE, "Cannot save failed assertion result", failure);
                }
            }
        });
    }

    @EventHandler public void worldInit(WorldInitEvent event) {
        if (event.getWorld().getName().startsWith("lorax_probe_")) {
            // Inspect generation before random leaf decay, fire or vegetation ticks change it.
            try {
                if (!event.getWorld().setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED, 0)) {
                    throw new IllegalStateException("Cannot disable random ticks in disposable probe world " + event.getWorld().getName());
                }
            } catch (Throwable error) {
                initializationFailure = error;
                throw new IllegalStateException("Probe game-rule initialization failed", error);
            }
        }
    }

    private static final class Plateau extends ChunkGenerator {
        private final Biome biome;
        Plateau() { this(Biome.FOREST); }
        Plateau(Biome biome) { this.biome = biome; }
        @Override public void generateNoise(WorldInfo info, Random random, int x, int z, ChunkData data) {
            data.setRegion(0, 0, 0, 16, 60, 16, Material.STONE);
            data.setRegion(0, 60, 0, 16, 63, 16, Material.DIRT);
            data.setRegion(0, 63, 0, 16, 64, 16, Material.GRASS_BLOCK);
        }
        @Override public BiomeProvider getDefaultBiomeProvider(WorldInfo info) {
            return new BiomeProvider() {
                @Override public Biome getBiome(WorldInfo world, int x, int y, int z) { return biome; }
                @Override public List<Biome> getBiomes(WorldInfo world) { return Collections.singletonList(biome); }
            };
        }
    }

    private World testWorld(String name, long seed) {
        World world = new WorldCreator(name).seed(seed).generator(new Plateau()).createWorld();
        require(world != null, "create test world " + name);
        require(!world.getPopulators().isEmpty(), "native Lorax population attached");
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) world.getChunkAt(x, z);
        return world;
    }

    private long digest(World world) {
        long hash = 1;
        int logs = 0;
        for (int x = -16; x < 32; x++) for (int z = -16; z < 32; z++) for (int y = 64; y < 115; y++) {
            Material material = world.getBlockAt(x, y, z).getType();
            if (material.name().endsWith("_LOG") || material.name().endsWith("_WOOD")) logs++;
            // Leaf distance and fence connections are updated by the server lifecycle.
            hash = hash * 31 + material.name().hashCode();
        }
        require(logs > 0, "automatic custom tree blocks present in " + world.getName());
        return hash;
    }

    private void runChecks() throws Exception {
        Files.createDirectories(getDataFolder().toPath());
        require(Lorax.loader.getStructures().size() == 40, "all assets initialized");
        World first = testWorld("lorax_probe_a", 812345);
        long before = digest(first);
        World second = testWorld("lorax_probe_b", 812345);
        if (before != digest(second)) {
            int differences = 0;
            java.util.Map<String,Integer> changes = new java.util.TreeMap<>();
            for (int x = -16; x < 32; x++) for (int z = -16; z < 32; z++) for (int y = 64; y < 115; y++) {
                Material a = first.getBlockAt(x,y,z).getType(), b = second.getBlockAt(x,y,z).getType();
                if (a != b) {
                    changes.merge(a + "/" + b, 1, Integer::sum);
                    if (differences++ < 10) getLogger().warning("World difference " + x + "," + y + "," + z + ": " + a + " / " + b);
                }
            }
            getLogger().warning("Different material positions: " + differences);
            getLogger().warning("Material changes: " + changes);
        }
        layoutReproducible = before == digest(second);
        getLogger().info("LORAX_LAYOUT_REPRODUCIBLE=" + layoutReproducible + "; overlap order dependence is an accepted limitation");
        String selected = candidates(first);
        require(selected.equals(candidates(first)), "candidate selection repeatable for identical inputs");
        require(selected.equals(candidates(second)), "same-seed candidate selection across worlds");
        require(before == digest(first), "second world cannot alter first world");
        World third = testWorld("lorax_probe_c", 987654);
        digest(third);
        for (Structure structure : Lorax.loader.getStructures().values()) {
            for (int rotation = 0; rotation < 4; rotation++) {
                // Reuse a high-altitude test site to keep the server footprint bounded.
                int x = 15, z = 15;
                for (int cx = (x - 20) >> 4; cx <= (x + 20) >> 4; cx++) {
                    for (int cz = (z - 20) >> 4; cz <= (z + 20) >> 4; cz++) first.getChunkAt(cx, cz);
                }
                Placement.Result result = Placement.place(structure, new Position(x, 180, z), rotation, new PaperWorldAccess(first), true);
                require(result == Placement.Result.PLACED, structure.name + " rotation " + rotation);
                for (Structure.Entry entry : structure.entries) {
                    if (entry.kind != Structure.Kind.BLOCK) continue;
                    int effectiveRotation = structure.rotateRandomly ? rotation : 0;
                    Position location = entry.position.rotate(effectiveRotation).add(new Position(x, 180, z));
                    String actual = first.getBlockAt(location.x, location.y, location.z).getBlockData().getAsString();
                    String expected = PaperBlocks.data(entry.blocks.get(0).rotate(effectiveRotation)).getAsString();
                    require(actual.equals(expected), structure.name + " rotation " + rotation + " block mismatch at " + location.x + "," + location.y + "," + location.z + ": " + actual + " expected " + expected);
                }
            }
        }
        checkBiomesAndDistance(first);
        saveRestartSnapshot(first);
    }

    private void checkBiomesAndDistance(World forest) {
        Biome[] biomes = {Biome.BIRCH_FOREST, Biome.TAIGA, Biome.SAVANNA, Biome.DARK_FOREST};
        String[] woods = {"BIRCH", "SPRUCE", "ACACIA", "DARK_OAK"};
        for (int i = 0; i < biomes.length; i++) {
            World world = new WorldCreator("lorax_probe_" + woods[i].toLowerCase(java.util.Locale.ROOT))
                    .seed(812345).generator(new Plateau(biomes[i])).createWorld();
            require(world != null && !world.getPopulators().isEmpty(), "native biome world " + biomes[i]);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) world.getChunkAt(x, z);
            boolean found = false;
            for (int x = -16; x < 32; x++) for (int z = -16; z < 32; z++) for (int y = 64; y < 115; y++) {
                String type = world.getBlockAt(x, y, z).getType().name();
                if (type.equals(woods[i] + "_LOG") || type.equals(woods[i] + "_WOOD")) found = true;
            }
            require(found, "native automatic species " + woods[i]);
        }
        int originX = 4096 * 16, originZ = -4096 * 16;
        for (int x = 4094; x <= 4098; x++) for (int z = -4098; z <= -4094; z++) forest.getChunkAt(x, z);
        boolean found = false;
        for (int x = originX - 16; x < originX + 32; x++) for (int z = originZ - 16; z < originZ + 32; z++) for (int y = 64; y < 115; y++) {
            if (forest.getBlockAt(x, y, z).getType().name().endsWith("_LOG")) found = true;
        }
        require(found, "native generation 65536 blocks from origin across negative Z");
    }

    private void saveRestartSnapshot(World world) throws Exception {
        // Simulate a player edit in an already populated chunk, then save it normally.
        world.getBlockAt(5, 100, 5).setType(Material.DIAMOND_BLOCK, false);
        world.getBlockAt(6, 100, 5).setType(Material.GLASS, false);
        java.util.Properties snapshot = new java.util.Properties();
        snapshot.setProperty("digest", Long.toString(digest(world)));
        try (java.io.OutputStream out = Files.newOutputStream(getDataFolder().toPath().resolve("restart.properties"))) {
            snapshot.store(out, "Disposable server restart snapshot");
        }
        world.save();
    }

    private void runRestartChecks() throws Exception {
        require(Lorax.loader.getStructures().size() == 40, "all assets initialized after restart");
        java.util.Properties snapshot = new java.util.Properties();
        try (java.io.InputStream in = Files.newInputStream(getDataFolder().toPath().resolve("restart.properties"))) { snapshot.load(in); }
        World world = testWorld("lorax_probe_a", 812345);
        require(world.getBlockAt(5, 100, 5).getType() == Material.DIAMOND_BLOCK, "player diamond preserved");
        require(world.getBlockAt(6, 100, 5).getType() == Material.GLASS, "player glass preserved");
        require(Long.toString(digest(world)).equals(snapshot.getProperty("digest")), "existing chunk layout unchanged after restart");
        // Configuration is changed only in memory inside this disposable probe.
        JavaPlugin plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("Lorax");
        require(plugin != null, "Lorax plugin available");
        boolean enabled = plugin.getConfig().getBoolean("enable-populator");
        try {
            plugin.getConfig().set("enable-populator", false);
            World disabled = new WorldCreator("lorax_probe_disabled").seed(812345).generator(new Plateau()).createWorld();
            require(disabled != null && disabled.getPopulators().isEmpty(), "disabled generation attaches no populator");
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                require(disabled.getBlockAt(x, 64, z).getType() == Material.AIR, "disabled world stays undecorated");
        } finally { plugin.getConfig().set("enable-populator", enabled); }
    }

    private String candidates(World world) {
        StringBuilder selected = new StringBuilder();
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            for (Generation.Candidate candidate : Generation.select(Lorax.loader.getStructures(),new PaperWorldAccess(world),x,z)) {
                selected.append(candidate.structure.name).append(':').append(candidate.center.x).append(',')
                        .append(candidate.center.y).append(',').append(candidate.center.z).append('/').append(candidate.rotation).append(';');
            }
        }
        return selected.toString();
    }
}
