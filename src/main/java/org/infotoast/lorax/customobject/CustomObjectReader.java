package org.infotoast.lorax.customobject;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.infotoast.lorax.Lorax;
import org.infotoast.lorax.PaperBlocks;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.common.StructureReader;
import org.infotoast.lorax.customobject.datatype.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class CustomObjectReader {
    private CustomObjectReader() { }
    public static CustomObject read(ObjectFile file) {
        InputStream input = Lorax.plugin.getResource(file.getFileName());
        if (input == null) throw new IllegalArgumentException("Missing bundled structure: " + file.getFileName());
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Structure structure = StructureReader.read(file.getName(), reader);
            ArrayList<CustomObjectItem> items = new ArrayList<>();
            ArrayList<CustomObjectCheck> checks = new ArrayList<>();
            for (Structure.Entry entry : structure.entries) {
                ObjectLocation position = new ObjectLocation(entry.position.x, entry.position.y, entry.position.z);
                CustomObjectItem item;
                switch (entry.kind) {
                    case GROUND_CHECK: item = new CustomObjectGroundCheck(position); break;
                    case BLOCK_CHECK: item = new CustomObjectBlockCheck(position, PaperBlocks.data(entry.blocks.get(0))); break;
                    case RANDOM_BLOCK:
                        BlockData[] blocks = new BlockData[entry.blocks.size()];
                        int[] chances = new int[blocks.length];
                        for (int i = 0; i < blocks.length; i++) {
                            blocks[i] = PaperBlocks.data(entry.blocks.get(i));
                            chances[i] = entry.chances.get(i);
                        }
                        item = new CustomObjectRandomBlock(position, blocks, chances); break;
                    default: item = new CustomObjectBlock(position, PaperBlocks.data(entry.blocks.get(0)));
                }
                items.add(item);
                if (item instanceof CustomObjectCheck) checks.add((CustomObjectCheck) item);
            }
            CustomObjectSettings settings = new CustomObjectSettings();
            settings.setAuthor(structure.author);
            settings.setDescription(structure.description);
            settings.setVersion(structure.version);
            settings.setMinHeight(structure.minHeight);
            settings.setMaxHeight(structure.maxHeight);
            settings.setRotateRandomly(structure.rotateRandomly);
            CustomObject object = new CustomObject(file.getName(), items, checks, settings);
            object.setDefinition(structure);
            return object;
        } catch (IOException | org.infotoast.lorax.customobject.exception.InvalidObjectCommandException error) {
            throw new IllegalArgumentException(error.getMessage(), error);
        }
    }
}
