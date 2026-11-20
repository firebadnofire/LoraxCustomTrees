package org.infotoast.lorax.forge26;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;

public final class LoraxCommands {
    private LoraxCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lorax")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(context -> help(context.getSource()))
            .then(Commands.literal("help").executes(context -> help(context.getSource())))
            .then(Commands.literal("export").executes(context -> {
                context.getSource().sendFailure(Component.literal("WorldEdit export is available in the Paper distribution only."));
                return 0;
            }))
            .then(Commands.literal("spawn").then(Commands.argument("structure",StringArgumentType.word())
                .suggests((context,builder) -> { ForgeLorax.structures.keySet().forEach(builder::suggest); return builder.buildFuture(); })
                .executes(context -> spawn(context,null))
                .then(Commands.argument("rotation",IntegerArgumentType.integer(0,3)).executes(context -> spawn(context,IntegerArgumentType.getInteger(context,"rotation")))))));
    }
    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("/lorax spawn <structure> [rotation]"),false);
        return 1;
    }
    private static int spawn(CommandContext<CommandSourceStack> context,Integer rotation) throws CommandSyntaxException {
        var source=context.getSource();
        source.getPlayerOrException();
        var structure=ForgeLorax.structures.get(StringArgumentType.getString(context,"structure"));
        if(structure==null) { source.sendFailure(Component.literal("Unknown structure")); return 0; }
        var position=source.getPosition();
        Position center=new Position((int)Math.floor(position.x()),(int)Math.floor(position.y()),(int)Math.floor(position.z()));
        if(rotation==null) rotation=new java.util.Random(java.util.Objects.hash(source.getLevel().getSeed(),(long)center.x,center.y,center.z)).nextInt(4);
        var result=Placement.place(structure,center,rotation,new ForgeWorldAccess(source.getLevel()),true);
        source.sendSuccess(() -> Component.literal("Lorax placement: "+result),false);
        return result==Placement.Result.PLACED?1:0;
    }
}
