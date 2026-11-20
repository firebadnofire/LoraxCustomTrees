package org.infotoast.lorax.forge26;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.infotoast.lorax.common.Catalog;
import org.infotoast.lorax.common.Structure;

import java.util.List;
import java.util.Map;

@Mod("lorax")
public final class ForgeLorax {
    public static Map<String,Structure> structures;
    private static final DeferredRegister<MapCodec<? extends Feature>> FEATURES=DeferredRegister.create(Registries.FEATURE_TYPE,"lorax");
    private static final ForgeConfigSpec.Builder CONFIG=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue ENABLED=CONFIG.define("enable-populator",true);
    public static final ForgeConfigSpec.BooleanValue SELECT_WORLDS=CONFIG.define("select-worlds",false);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WORLDS=CONFIG.defineListAllowEmpty("enabled-worlds",List.of("minecraft:overworld"),value->value instanceof String);

    public ForgeLorax(FMLJavaModLoadingContext context) {
        try { structures=Catalog.load(getClass().getClassLoader()); }
        catch(java.io.IOException error) { throw new IllegalStateException("Lorax asset initialization failed",error); }
        FEATURES.register("trees",()->LoraxFeature.CODEC);
        FEATURES.register(context.getModBusGroup());
        context.registerConfig(ModConfig.Type.SERVER,CONFIG.build());
        RegisterCommandsEvent.BUS.addListener(event -> LoraxCommands.register(event.getDispatcher()));
        ServerAboutToStartEvent.BUS.addListener(event->{
            for(Structure structure:structures.values()) for(Structure.Entry entry:structure.entries) {
                for(var state:entry.blocks) for(int rotation=0;rotation<4;rotation++) ForgeWorldAccess.parse(state.rotate(rotation));
            }
            com.mojang.logging.LogUtils.getLogger().info("Lorax Engine loaded successfully! Loaded {} bundled structures.",structures.size());
        });
    }
}
