package org.infotoast.lorax.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.LimitedRegion;
import org.infotoast.lorax.Lorax;
import org.infotoast.lorax.WorldImproved;

import java.util.ArrayList;
import java.util.Random;

public class Utility {

    public static boolean isGroundBlock(Material block) {
        return org.infotoast.lorax.common.Ground.allows(block.getKey().toString());
    }

    public static boolean isGroundBlock(Block block) {
        return isGroundBlock(block.getType());
    }

    public static boolean isGroundBlock(BlockData block) {
        return isGroundBlock(block.getMaterial());
    }

    public static Material getRandomMaterial(RandomMaterial[] r, Random rand, Material def) {
        int randInt = rand.nextInt(1000)+1;
        for (RandomMaterial mat : r) {
            if (mat.isYes(randInt)) return mat.getMaterial();
        }
        return def;
    }

    public static int randInt(Random rand, int d, int max) {
        boolean negative = false;
        if (d < 0 && max < 0) {
            negative = true;
            d = -d;
            max = -max;
        }

        if (max < d) {
            int temp = d;
            d = max;
            max = temp;
        }

        int randomNum = rand.nextInt((max - d) + 1) + d;
        return negative ? -randomNum : randomNum;
    }

    public static boolean isLocationInChunk(int x, int z, int chunkX, int chunkZ) {
        return x >> 4 == chunkX && z >> 4 == chunkZ;
    }


}
