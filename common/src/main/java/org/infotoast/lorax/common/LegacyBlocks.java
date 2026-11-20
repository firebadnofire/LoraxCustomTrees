package org.infotoast.lorax.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Explicit 1.12.2 palette. Unknown blocks/properties fail; nothing falls back to air. */
public final class LegacyBlocks {
    public enum Tile { NONE, PLAYER_HEAD, POTTED_ALLIUM }
    public final int id, metadata;
    public final Tile tile;

    private LegacyBlocks(int id, int metadata, Tile tile) {
        this.id = id;
        this.metadata = metadata;
        this.tile = tile;
    }

    private static void properties(BlockState state, String... allowed) {
        Set<String> names = new HashSet<String>(Arrays.asList(allowed));
        for (String key : state.properties.keySet()) if (!names.contains(key)) {
            throw new IllegalArgumentException("Unsupported 1.12.2 property: " + state + " / " + key);
        }
    }

    private static int direction(String facing, String... names) {
        for (int i = 0; i < names.length; i++) if (names[i].equals(facing)) return i;
        throw new IllegalArgumentException("Unsupported facing: " + facing);
    }

    public static LegacyBlocks map(BlockState state) {
        String name = state.name;
        if (!name.startsWith("minecraft:")) throw new IllegalArgumentException("Non-vanilla legacy block: " + name);
        name = name.substring(10);
        String[] species = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
        for (int i = 0; i < species.length; i++) {
            String wood = species[i];
            if (name.equals(wood + "_log") || name.equals(wood + "_wood")) {
                properties(state, "axis");
                String axis = state.properties.containsKey("axis") ? state.properties.get("axis") : "y";
                int bits = name.endsWith("_wood") ? 12 : direction(axis, "y", "x", "z") * 4;
                return new LegacyBlocks(i < 4 ? 17 : 162, (i < 4 ? i : i - 4) | bits, Tile.NONE);
            }
            if (name.equals(wood + "_leaves")) {
                properties(state, "distance", "persistent");
                int bits = "true".equals(state.properties.get("persistent")) ? 4 : 8;
                return new LegacyBlocks(i < 4 ? 18 : 161, (i < 4 ? i : i - 4) | bits, Tile.NONE);
            }
            if (name.equals(wood + "_fence")) {
                properties(state, "north", "east", "south", "west", "waterlogged");
                if ("true".equals(state.properties.get("waterlogged"))) throw new IllegalArgumentException("Waterlogged fences unavailable in 1.12.2");
                return new LegacyBlocks(i == 0 ? 85 : i == 4 ? 192 : i == 5 ? 191 : 187 + i, 0, Tile.NONE);
            }
            if (name.equals(wood + "_fence_gate")) {
                properties(state, "facing", "open", "powered", "in_wall");
                int data = direction(value(state, "facing", "north"), "south", "west", "north", "east");
                if ("true".equals(state.properties.get("open"))) data |= 4;
                if ("true".equals(state.properties.get("powered"))) data |= 8;
                return new LegacyBlocks(i == 0 ? 107 : i == 4 ? 187 : i == 5 ? 186 : 182 + i, data, Tile.NONE);
            }
            if (name.equals(wood + "_slab")) {
                properties(state, "type", "waterlogged");
                if ("true".equals(state.properties.get("waterlogged"))) throw new IllegalArgumentException("Waterlogged slab unavailable");
                String type = value(state, "type", "bottom");
                if (!type.equals("bottom") && !type.equals("top") && !type.equals("double")) throw new IllegalArgumentException("Invalid slab type");
                return new LegacyBlocks(type.equals("double") ? 125 : 126, i | (type.equals("top") ? 8 : 0), Tile.NONE);
            }
        }
        if (name.equals("air")) { properties(state); return new LegacyBlocks(0, 0, Tile.NONE); }
        if (name.equals("melon")) { properties(state); return new LegacyBlocks(103, 0, Tile.NONE); }
        if (name.equals("green_terracotta") || name.equals("cyan_terracotta")) {
            properties(state); return new LegacyBlocks(159, name.startsWith("green") ? 13 : 9, Tile.NONE);
        }
        if (name.equals("cocoa")) {
            properties(state, "age", "facing");
            int age = Integer.parseInt(value(state, "age", "0"));
            if (age < 0 || age > 2) throw new IllegalArgumentException("Invalid cocoa age");
            return new LegacyBlocks(127, age * 4 + direction(value(state, "facing", "north"), "south", "west", "north", "east"), Tile.NONE);
        }
        if (name.equals("vine")) {
            properties(state, "north", "east", "south", "west", "up");
            int data = 0;
            String[] sides = {"south", "west", "north", "east"};
            for (int i = 0; i < 4; i++) if ("true".equals(state.properties.get(sides[i]))) data |= 1 << i;
            return new LegacyBlocks(106, data, Tile.NONE);
        }
        if (name.equals("oak_button") || name.equals("stone_button")) {
            properties(state, "face", "facing", "powered");
            String face = value(state, "face", "wall");
            int data = face.equals("floor") ? 5 : face.equals("ceiling") ? 0
                    : 1 + direction(value(state, "facing", "north"), "east", "west", "south", "north");
            if ("true".equals(state.properties.get("powered"))) data |= 8;
            return new LegacyBlocks(name.equals("oak_button") ? 143 : 77, data, Tile.NONE);
        }
        if (name.equals("torch") || name.equals("wall_torch")) {
            properties(state, "facing");
            return new LegacyBlocks(50, name.equals("torch") ? 5 : 1 + direction(value(state, "facing", "north"), "east", "west", "south", "north"), Tile.NONE);
        }
        if (name.equals("player_wall_head")) {
            properties(state, "facing", "powered");
            if ("true".equals(state.properties.get("powered"))) throw new IllegalArgumentException("Powered heads unavailable in 1.12.2");
            return new LegacyBlocks(144, 2 + direction(value(state, "facing", "north"), "north", "south", "west", "east"), Tile.PLAYER_HEAD);
        }
        if (name.equals("potted_allium")) {
            properties(state); return new LegacyBlocks(140, 0, Tile.POTTED_ALLIUM);
        }
        throw new IllegalArgumentException("No intentional 1.12.2 mapping for " + state);
    }

    private static String value(BlockState state, String key, String fallback) {
        return state.properties.containsKey(key) ? state.properties.get(key) : fallback;
    }
}
