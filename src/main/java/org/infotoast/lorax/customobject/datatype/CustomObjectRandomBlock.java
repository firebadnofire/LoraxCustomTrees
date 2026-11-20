package org.infotoast.lorax.customobject.datatype;

import org.bukkit.block.data.BlockData;
import org.infotoast.lorax.customobject.creator.CustomObjectWriter;
import org.infotoast.lorax.customobject.exception.InvalidObjectCommandException;

public class CustomObjectRandomBlock implements CustomObjectItem {
    private BlockData[] blocks;
    private int[] chances;
    private ObjectLocation location;
    public CustomObjectRandomBlock(ObjectLocation location, BlockData[] blocks, int[] chances) throws InvalidObjectCommandException {
        if (blocks.length != chances.length) {
            throw new InvalidObjectCommandException("blocks and chances must be the same length");
        }
        this.location = location;
        this.blocks = blocks;
        this.chances = chances;
    }



    @Override
    public String getName() {
        return "RandomBlock";
    }

    @Override
    public ObjectLocation getLocation() {
        return location;
    }

    public static String[] getKeywords() {
        return new String[]{"RandomBlock", "RB"};
    }

    public static String getRESyntax() {
        return "R.*\\(-?[0-9]+,-?[0-9]+,-?[0-9]+,[A-Z0-9,_:=\\[\\]]+\\)";
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RB(");
        str.append(location);
        str.append(",");
        for (int i = 0; i < blocks.length; i++) {
            str.append(CustomObjectWriter.getBlockName(blocks[i]));
            str.append(",");
            str.append(chances[i]);
            if (i+1 <  blocks.length) {
                str.append(",");
            }
        }
        str.append(")");
        return str.toString();
    }
}
