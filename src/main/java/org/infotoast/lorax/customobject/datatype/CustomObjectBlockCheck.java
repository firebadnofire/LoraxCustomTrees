package org.infotoast.lorax.customobject.datatype;

import org.bukkit.block.data.BlockData;
import org.infotoast.lorax.customobject.creator.CustomObjectWriter;

public class CustomObjectBlockCheck extends CustomObjectCheck {
    private BlockData block;
    public CustomObjectBlockCheck(ObjectLocation location, BlockData block) {
        super(location);
        this.block = block;
    }


    @Override
    public String getName() {
        return "BlockCheck";
    }

    public static String[] getKeywords() {
        return new String[]{"BlockCheck", "BC"};
    }

    public static String getRESyntax() {
        return "B.*\\(-?[0-9]+,-?[0-9]+,-?[0-9]+,[A-Z0-9,_:=\\[\\]]+\\)";
    }

    @Override
    public String toString() {
        String blockName = CustomObjectWriter.getBlockName(block);
        return "BC(" + location + "," +  blockName + ")";
    }
}
