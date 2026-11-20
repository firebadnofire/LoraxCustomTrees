package org.infotoast.lorax;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.infotoast.lorax.common.Generation;
import java.util.Random;

public class Populator extends BlockPopulator {
    @Override
    public void populate(WorldInfo world, Random random, int x, int z, LimitedRegion region) {
        if (world.getEnvironment() == World.Environment.NORMAL) {
            Generation.populate(Lorax.loader.getStructures(), new PaperWorldAccess(world, region), x, z);
        }
    }
}
