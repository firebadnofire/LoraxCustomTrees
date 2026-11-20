package org.infotoast.lorax;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.infotoast.lorax.common.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PaperBlocks {
    private PaperBlocks() { }

    public static BlockData data(BlockState state) {
        // Old modern releases do not expose head power. False is equivalent to their default.
        // Never discard powered=true or any other unsupported property.
        if (state.name.equals("minecraft:player_wall_head") && "false".equals(state.properties.get("powered"))) {
            Map<String,String> properties = new LinkedHashMap<>(state.properties);
            properties.remove("powered");
            state = new BlockState(state.name, properties);
        }
        return Bukkit.createBlockData(state.toString());
    }
}
