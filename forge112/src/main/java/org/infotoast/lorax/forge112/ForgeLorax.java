package org.infotoast.lorax.forge112;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.infotoast.lorax.common.Catalog;
import org.infotoast.lorax.common.Generation;
import org.infotoast.lorax.common.LegacyBlocks;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.common.Structure;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

@Mod(modid="lorax", name="Lorax Custom Trees", version="6.2", acceptableRemoteVersions="*", serverSideOnly=true, acceptedMinecraftVersions="[1.12.2]")
public final class ForgeLorax {
    public static Map<String,Structure> structures=Collections.emptyMap();
    private boolean enabled, selectWorlds;
    private String[] worlds;

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        Configuration config=new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        enabled=config.getBoolean("enable-populator","generation",true,"Generate trees in new surface-world chunks.");
        selectWorlds=config.getBoolean("select-worlds","generation",false,"Restrict generation to enabled-worlds.");
        worlds=config.getStringList("enabled-worlds","generation",new String[]{"world"},"World save names allowed to generate trees.");
        if(config.hasChanged()) config.save();
        try {
            structures=Catalog.load(getClass().getClassLoader());
            for(Structure structure:structures.values()) for(Structure.Entry entry:structure.entries) {
                for(org.infotoast.lorax.common.BlockState state:entry.blocks) for(int rotation=0;rotation<4;rotation++) LegacyBlocks.map(state.rotate(rotation));
            }
        } catch(Exception error) { throw new IllegalStateException("Lorax Forge asset initialization failed",error); }
        event.getModLog().info("Lorax Engine loaded successfully! Loaded {} bundled structures.",structures.size());
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        GameRegistry.registerWorldGenerator((random,chunkX,chunkZ,world,generator,provider)->{
            if(!enabled || !world.provider.isSurfaceWorld()) return;
            if(selectWorlds && !Arrays.asList(worlds).contains(world.getWorldInfo().getWorldName())) return;
            Generation.populate(structures,new ForgeWorldAccess(world,chunkX,chunkZ,false),chunkX,chunkZ);
        },1000);
    }

    @Mod.EventHandler public void starting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            public String getName() { return "lorax"; }
            public String getUsage(ICommandSender sender) { return "/lorax spawn <structure> [rotation]"; }
            public int getRequiredPermissionLevel() { return 2; }
            public void execute(MinecraftServer server,ICommandSender sender,String[] args) {
                if(args.length==0 || args[0].equalsIgnoreCase("help")) { sender.sendMessage(new TextComponentString(getUsage(sender))); return; }
                if(args[0].equalsIgnoreCase("export")) { sender.sendMessage(new TextComponentString("WorldEdit export is available in the Paper distribution only.")); return; }
                if(!args[0].equalsIgnoreCase("spawn") || args.length<2 || args.length>3) { sender.sendMessage(new TextComponentString(getUsage(sender))); return; }
                if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) { sender.sendMessage(new TextComponentString("This command requires a player.")); return; }
                Structure structure=structures.get(args[1]);
                if(structure==null) { sender.sendMessage(new TextComponentString("Unknown structure: "+args[1])); return; }
                World world=sender.getEntityWorld();
                Position center=new Position(sender.getPosition().getX(),sender.getPosition().getY(),sender.getPosition().getZ());
                try {
                    int rotation=args.length==3?Integer.parseInt(args[2]):new Random(java.util.Objects.hash(world.getSeed(),(long)center.x,center.y,center.z)).nextInt(4);
                    if(rotation<0 || rotation>3) throw new IllegalArgumentException("Rotation must be 0..3");
                    Placement.Result result=Placement.place(structure,center,rotation,new ForgeWorldAccess(world,center.x>>4,center.z>>4,true),true);
                    sender.sendMessage(new TextComponentString("Lorax placement: "+result));
                } catch(IllegalArgumentException error) { sender.sendMessage(new TextComponentString(error.getMessage())); }
            }
        });
    }
}
