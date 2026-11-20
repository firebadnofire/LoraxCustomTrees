package org.infotoast.lorax.forge26;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.infotoast.lorax.common.Generation;
import org.infotoast.lorax.common.Placement;

public record LoraxFeature() implements Feature {
    public static final MapCodec<LoraxFeature> CODEC=MapCodec.unit(LoraxFeature::new);
    @Override public MapCodec<? extends Feature> codec() { return CODEC; }
    @Override public boolean place(WorldGenLevel world,ChunkGenerator generator,RandomSource random,BlockPos origin) {
        if(!ForgeLorax.ENABLED.get() || !world.getLevel().dimensionType().hasSkyLight()) return false;
        String dimension=world.getLevel().dimension().identifier().toString();
        if(ForgeLorax.SELECT_WORLDS.get() && !ForgeLorax.WORLDS.get().contains(dimension)) return false;
        return Generation.populate(ForgeLorax.structures,new ForgeWorldAccess(world),origin.getX()>>4,origin.getZ()>>4).contains(Placement.Result.PLACED);
    }
}
