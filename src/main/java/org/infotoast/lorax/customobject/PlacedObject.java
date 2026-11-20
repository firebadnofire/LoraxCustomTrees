package org.infotoast.lorax.customobject;

import org.bukkit.generator.LimitedRegion;
import org.infotoast.lorax.PaperWorldAccess;
import org.infotoast.lorax.WorldImproved;
import org.infotoast.lorax.common.Placement;
import org.infotoast.lorax.common.Position;
import org.infotoast.lorax.customobject.datatype.CustomObjectCheck;
import org.infotoast.lorax.customobject.datatype.CustomObjectItem;
import org.infotoast.lorax.customobject.datatype.ObjectLocation;
import java.util.ArrayList;

public class PlacedObject extends CustomObject {
    private final ObjectLocation center;
    private final int rotation;
    private final WorldImproved world;
    private boolean placed;

    public PlacedObject(String name, CustomObjectSettings settings, ArrayList<CustomObjectItem> items,
                        ArrayList<CustomObjectCheck> checks, ObjectLocation center, int rotation, WorldImproved world) {
        super(name, items, checks, settings);
        if (rotation < 0 || rotation > 3) throw new IllegalArgumentException("Rotation must be between 0 and 3");
        this.center = center;
        this.rotation = settings.isRotateRandomly() ? rotation : 0;
        this.world = world;
    }

    public void placeAll() {
        if (placed) return;
        Placement.Result result = Placement.place(getDefinition(), position(), rotation, new PaperWorldAccess(world.getWorld()), true);
        if (result != Placement.Result.PLACED) throw new IllegalStateException(result.toString());
        placed = true;
    }

    public void placeAll(LimitedRegion region) {
        if (placed) return;
        placed = Placement.place(getDefinition(), position(), rotation, new PaperWorldAccess(world.getWorld(), region), false) == Placement.Result.PLACED;
    }

    private Position position() { return new Position(center.getX(), center.getY(), center.getZ()); }
    public ObjectLocation getLocation() { return center; }
    public int getRotation() { return rotation; }
    public boolean isPlaced() { return placed; }
}
