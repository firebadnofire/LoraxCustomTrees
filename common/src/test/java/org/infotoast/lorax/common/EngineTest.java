package org.infotoast.lorax.common;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Dependency-free executable contract tests, also runnable on a Java 8 VM. */
public final class EngineTest {
    private static int assertions;

    private static void expect(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class World implements WorldAccess {
        final Map<String, String> writes = new LinkedHashMap<String, String>();
        int edge = 64;
        boolean blocked;
        String biome = "forest";
        public long seed() { return 42; }
        public int minHeight() { return 0; }
        public int maxHeight() { return 256; }
        public boolean contains(int x, int y, int z) { return Math.abs(x) < edge && Math.abs(z) < edge; }
        public int surface(int x, int z) { return 64; }
        public String biome(int x, int y, int z) { return biome; }
        public String block(int x, int y, int z) { return "minecraft:air"; }
        public boolean replaceable(int x, int y, int z) { return !blocked && !writes.containsKey(x + "," + y + "," + z); }
        public void validate(BlockState state) { }
        public void set(int x, int y, int z, BlockState state) { writes.put(x + "," + y + "," + z, state.toString()); }
    }

    public static void main(String[] args) throws Exception {
        int count = 0;
        Map<String, Structure> catalog = new LinkedHashMap<String, Structure>();
        for (File file : new File(args[0]).listFiles()) {
            if (!file.getName().endsWith(".bo5")) continue;
            Structure structure;
            try (FileReader reader = new FileReader(file)) { structure = StructureReader.read(file.getName(), reader); }
            catalog.put(file.getName().replace(".bo5", ""), structure);
            expect(!structure.entries.isEmpty(), file + " empty");
            for (Structure.Entry entry : structure.entries) {
                Position position = entry.position;
                for (int i = 0; i < 4; i++) position = position.rotate(1);
                expect(position.x == entry.position.x && position.z == entry.position.z, "coordinate rotation");
                for (BlockState state : entry.blocks) {
                    BlockState rotated = state;
                    for (int i = 0; i < 4; i++) rotated = rotated.rotate(1);
                    expect(state.properties.equals(rotated.properties), "state rotation: " + state);
                    for (int rotation = 0; rotation < 4; rotation++) {
                        LegacyBlocks legacy = LegacyBlocks.map(state.rotate(rotation));
                        expect(legacy.id >= 0 && legacy.metadata >= 0 && legacy.metadata < 16, "legacy mapping " + state);
                    }
                }
            }
            count++;
        }
        expect(count == 40, "Expected all 40 bundled structures, got " + count);
        expect(Generation.species("jungle") == -1, "preserve automatic jungle policy");
        expect(Generation.species("roofed_forest") == 5, "legacy dark forest alias");
        String[] eligible = {"birch_forest", "forest", "taiga", "savanna", "dark_forest"};
        for (String biome : eligible) {
            World world = new World();
            world.biome = biome;
            java.util.List<Generation.Candidate> selected = Generation.select(catalog, world, 0, 0);
            expect(selected.size() >= 1 && selected.size() <= 4, "original candidate density in " + biome);
            String signature = signature(selected);
            expect(signature.equals(signature(Generation.select(catalog, world, 0, 0))), "seeded candidates in " + biome);
            world.blocked = true;
            expect(signature.equals(signature(Generation.select(catalog, world, 0, 0))), "obstruction must not alter candidates in " + biome);
            for (Generation.Candidate candidate : selected) {
                expect(java.util.Arrays.asList(Generation.TREES[Generation.species(biome)]).contains(candidate.structure.name.replace(".bo5", "")), "eligible species in " + biome);
            }
        }
        for (String biome : new String[]{"jungle", "desert", "ocean", "nether_wastes"}) {
            World world = new World();
            world.biome = biome;
            expect(Generation.select(catalog, world, 0, 0).isEmpty(), "preserve excluded biome " + biome);
        }
        expect(LegacyBlocks.map(BlockState.parse("acacia_fence_gate")).id == 187, "acacia gate ID");
        expect(LegacyBlocks.map(BlockState.parse("dark_oak_fence_gate")).id == 186, "dark oak gate ID");
        BlockState facing = BlockState.parse("cocoa[age=2,facing=north]").rotate(1);
        expect("east".equals(facing.properties.get("facing")), "north -> east");
        expect("z".equals(BlockState.parse("oak_log[axis=x]").rotate(1).properties.get("axis")), "log axis");
        expect("true".equals(BlockState.parse("vine[north=true]").rotate(1).properties.get("east")), "vine sides");
        String text = "RotateRandomly: true\nGC(1,-1,0)\nB(1,0,0,oak_log[axis=x])\nB(2,0,0,OAK_LEAVES)";
        Structure structure = StructureReader.read("fixture", new StringReader(text));
        World first = new World();
        expect(Placement.place(structure, new Position(0,64,0), 1, first, false) == Placement.Result.PLACED, "valid placement");
        expect(first.writes.containsKey("0,64,1"), "quarter-turn coordinate");
        expect(Placement.place(structure, new Position(0,64,0), 1, first, false) == Placement.Result.OBSTRUCTED, "repeat placement rejected");
        expect(first.writes.size() == 2, "repeat placement must not duplicate writes");
        World second = new World();
        second.edge = 2;
        expect(Placement.place(structure, new Position(0,64,0), 0, second, false) == Placement.Result.OUTSIDE_CONTEXT, "boundary rejection");
        expect(second.writes.isEmpty(), "boundary rejection must not partially write");
        expect(first.writes.size() == 2, "world isolation");
        second.edge = 64;
        second.blocked = true;
        expect(Placement.place(structure, new Position(0,64,0), 0, second, false) == Placement.Result.OBSTRUCTED, "protect structures");
        expect(second.writes.isEmpty(), "obstruction must not partially write");
        Structure atSurface = StructureReader.read("surface check", new StringReader("GC(0,0,0)\nB(0,0,0,OAK_LOG)"));
        expect(Placement.place(atSurface, new Position(0,64,0), 0, new World(), false) == Placement.Result.PLACED, "preserve inclusive ground check");
        Structure attachments = StructureReader.read("attachments", new StringReader("B(0,1,0,wall_torch[facing=north])\nB(0,1,1,OAK_LOG)"));
        World support = new World();
        Placement.place(attachments, new Position(0,64,0), 0, support, false);
        expect(support.writes.values().iterator().next().contains("oak_log"), "supports must precede attachments");
        expect(Placement.place(structure, new Position(0,255,0), 0, second, false) != Placement.Result.PLACED, "terrain checks not cached");
        StructureReader.read("old spelling", new StringReader("RotateRandomy: false\nB(0,0,0,AIR)"));
        try {
            StructureReader.read("bad.bo5", new StringReader("# comment\nNoSuchCommand(0,0,0)"));
            throw new AssertionError("Unknown command accepted");
        } catch (java.io.IOException expected) {
            expect(expected.getMessage().contains("bad.bo5:2:"), "line diagnostics");
        }
        System.out.println("PASS: " + assertions + " assertions; " + count + " bundled structures");
    }

    private static String signature(java.util.List<Generation.Candidate> candidates) {
        StringBuilder result = new StringBuilder();
        for (Generation.Candidate candidate : candidates) result.append(candidate.structure.name).append(':')
                .append(candidate.center.x).append(',').append(candidate.center.y).append(',').append(candidate.center.z)
                .append('/').append(candidate.rotation).append(';');
        return result.toString();
    }
}
