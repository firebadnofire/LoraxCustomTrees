package org.infotoast.lorax.forge26;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.infotoast.lorax.common.Generation;
import org.infotoast.lorax.common.Placement;

public final class LoraxFeature extends Feature<NoneFeatureConfiguration> {
    public LoraxFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world=context.level();
        if(!ForgeLorax.ENABLED.get() || !world.getLevel().dimensionType().hasSkyLight()) return false;
        String dimension=world.getLevel().dimension().identifier().toString();
        if(ForgeLorax.SELECT_WORLDS.get() && !ForgeLorax.WORLDS.get().contains(dimension)) return false;
        var origin=context.origin();
        return Generation.populate(ForgeLorax.structures,new ForgeWorldAccess(world),origin.getX()>>4,origin.getZ()>>4).contains(Placement.Result.PLACED);
    }
}

