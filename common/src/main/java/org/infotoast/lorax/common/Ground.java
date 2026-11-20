package org.infotoast.lorax.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Ground {
    private static final Set<String> NAMES = new HashSet<String>(Arrays.asList(
            "grass_block", "dirt", "coarse_dirt", "podzol", "sand", "red_sand", "gravel", "snow", "snow_block",
            "clay", "stone", "andesite", "diorite", "granite", "tuff", "calcite", "terracotta", "mycelium",
            "sandstone", "red_sandstone", "dirt_path"));
    private Ground() { }
    public static boolean allows(String name) {
        if (name.startsWith("minecraft:")) name = name.substring(10);
        return NAMES.contains(name) || name.endsWith("_terracotta") && !name.endsWith("_glazed_terracotta");
    }
}
