package org.infotoast.lorax.common;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class Generation {
    private Generation() { }

    public static final class Candidate {
        public final Structure structure;
        public final Position center;
        public final int rotation;
        private Candidate(Structure structure, Position center, int rotation) {
            this.structure = structure;
            this.center = center;
            this.rotation = rotation;
        }
    }

    public static final String[][] TREES = {
        {"greatBirch1", "greatBirch2", "smallBirch1", "smallBirch2", "tallBirch1", "tallBirch2"},
        {"smallOak1", "smallOak2", "smallOak3", "tallOak1", "tallOak2", "greatOak1", "greatOak2", "greatOak3", "mediumOak1"},
        {"smallSpruce1", "tallSpruce1", "tallSpruce2", "tallSpruce3", "tallSpruce4", "tallSpruce5"},
        {"largeJungle1", "smallJungle1", "smallJungle2", "tallJungle1", "tallJungle2", "tallJungle3", "tinyJungle1", "tinyJungle2"},
        {"greatAcacia1", "greatAcacia2", "mediumAcacia1", "mediumAcacia2", "mediumAcacia3", "mediumAcacia4", "tallAcacia1"},
        {"smallDarkOak1", "mediumDarkOak1", "mediumDarkOak2", "mediumDarkOak3"}
    };

    public static int species(String biome) {
        if (biome.startsWith("minecraft:")) biome = biome.substring(10);
        if (biome.equals("birch_forest") || biome.equals("old_growth_birch_forest") || biome.equals("birch_forest_hills")
                || biome.equals("mutated_birch_forest") || biome.equals("mutated_birch_forest_hills")) return 0;
        if (biome.equals("forest") || biome.equals("flower_forest") || biome.equals("forest_hills") || biome.equals("mutated_forest")) return 1;
        if (biome.equals("taiga") || biome.equals("grove") || biome.equals("snowy_taiga") || biome.equals("taiga_hills")
                || biome.equals("taiga_cold") || biome.equals("taiga_cold_hills")) return 2;
        if (biome.equals("savanna") || biome.equals("savanna_plateau") || biome.equals("savanna_rock")) return 4;
        if (biome.equals("dark_forest") || biome.equals("roofed_forest")) return 5;
        return -1; // Preserve manual-only jungle policy.
    }

    public static List<Placement.Result> populate(Map<String, Structure> catalog, WorldAccess world, int chunkX, int chunkZ) {
        List<Placement.Result> results = new ArrayList<Placement.Result>();
        for (Candidate candidate : select(catalog, world, chunkX, chunkZ)) {
            results.add(Placement.place(candidate.structure, candidate.center, candidate.rotation, world, false));
        }
        return results;
    }

    public static List<Candidate> select(Map<String, Structure> catalog, WorldAccess world, int chunkX, int chunkZ) {
        Set<String> biomes = new LinkedHashSet<String>();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int rawX = (chunkX << 4) + x, rawZ = (chunkZ << 4) + z;
            int y = world.surface(rawX, rawZ);
            if (y > 0) biomes.add(world.biome(rawX, y, rawZ));
        }
        Random random = new Random(Objects.hash(world.seed(), world.seed(), chunkX, chunkZ));
        List<Candidate> results = new ArrayList<Candidate>();
        for (String biome : biomes) {
            int type = species(biome);
            if (type < 0) continue;
            int count = random.nextInt(4) + 1;
            for (int i = 0; i < count; i++) {
                // Keep the original sequence, including its 0..14 local offsets.
                int x = (chunkX << 4) + random.nextInt(15), z = (chunkZ << 4) + random.nextInt(15);
                int y = world.surface(x, z);
                if (y < 1) continue;
                String[] variants = TREES[type];
                Structure structure = catalog.get(variants[random.nextInt(variants.length)]);
                if (structure == null) throw new IllegalStateException("Missing tree in catalog for " + biome);
                results.add(new Candidate(structure, new Position(x, y, z), random.nextInt(4)));
            }
        }
        return results;
    }
}
