package org.infotoast.lorax.customobject;

import org.infotoast.lorax.WorldImproved;
import org.infotoast.lorax.customobject.datatype.CustomObjectCheck;
import org.infotoast.lorax.customobject.datatype.CustomObjectItem;
import org.infotoast.lorax.customobject.datatype.ObjectLocation;

import java.util.ArrayList;
import org.infotoast.lorax.common.Structure;
import org.infotoast.lorax.common.StructureReader;
import java.io.StringReader;
import java.io.IOException;

public class CustomObject {
    protected final String objectName;
    protected final ArrayList<CustomObjectItem> items;
    protected final ArrayList<CustomObjectCheck> checks;
    protected final CustomObjectSettings settings;
    private Structure definition;

    public CustomObject(String objectName, ArrayList<CustomObjectItem> items, ArrayList<CustomObjectCheck> checks, CustomObjectSettings settings) {
        this.objectName = objectName;
        this.items = items;
        this.checks = checks;
        this.settings = settings;
    }

    public String getObjectName() {
        return objectName;
    }

    public ArrayList<CustomObjectItem> getItems() {
        return items;
    }

    public CustomObjectSettings getSettings() {
        return settings;
    }

    public Structure getDefinition() {
        if (definition == null) {
            try {
                definition = StructureReader.read(objectName, new StringReader(toString()));
            } catch (IOException error) {
                throw new IllegalArgumentException("Invalid structure " + objectName, error);
            }
        }
        return definition;
    }

    public void setDefinition(Structure definition) {
        this.definition = definition;
    }

    public PlacedObject getPlacedObject(ObjectLocation centerLocation, int rotation, WorldImproved world) {
        PlacedObject placed = new PlacedObject(objectName, settings, items, checks, centerLocation, rotation, world);
        placed.setDefinition(getDefinition());
        return placed;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(settings + "\n\n");
        for (CustomObjectItem item : items) {
            str.append(item).append("\n");
        }
        return str.toString();
    }
}
