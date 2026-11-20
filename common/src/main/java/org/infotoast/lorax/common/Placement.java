package org.infotoast.lorax.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

/** Synchronous complete-footprint preflight. No queues, persistence or world references. */
public final class Placement {
    public enum Result { PLACED, OUTSIDE_CONTEXT, HEIGHT, GROUND_CHECK, BLOCK_CHECK, OBSTRUCTED }

    private static final class Write {
        final Position position;
        final BlockState state;
        Write(Position position, BlockState state) { this.position = position; this.state = state; }
    }

    private Placement() { }

    public static Result place(Structure structure, Position center, int rotation, WorldAccess world, boolean manual) {
        if (!structure.rotateRandomly) rotation = 0;
        List<Write> writes = new ArrayList<Write>();
        for (Structure.Entry entry : structure.entries) {
            Position position = entry.position.rotate(rotation).add(center);
            if (position.y < world.minHeight() || position.y >= world.maxHeight()
                    || (!manual && (position.y < structure.minHeight || position.y > structure.maxHeight))) return Result.HEIGHT;
            if (!world.contains(position.x, position.y, position.z)) return Result.OUTSIDE_CONTEXT;
            if (entry.kind == Structure.Kind.GROUND_CHECK) {
                if (!manual && position.y > world.surface(position.x, position.z)) return Result.GROUND_CHECK;
                continue;
            }
            if (entry.kind == Structure.Kind.BLOCK_CHECK) {
                if (!manual && !entry.blocks.get(0).name.equals(world.block(position.x, position.y, position.z))) return Result.BLOCK_CHECK;
                continue;
            }
            BlockState state;
            if (entry.kind == Structure.Kind.RANDOM_BLOCK) {
                // Preserve the original coordinate-seeded per-block draw.
                int draw = new Random(Objects.hash(world.seed(), (long) position.x, position.y, position.z)).nextInt(100);
                int total = 0;
                state = null;
                for (int i = 0; i < entry.blocks.size(); i++) {
                    total += entry.chances.get(i);
                    if (draw < total) { state = entry.blocks.get(i); break; }
                }
                if (state == null) continue;
            } else state = entry.blocks.get(0);
            if (!manual && !world.replaceable(position.x, position.y, position.z)) return Result.OBSTRUCTED;
            state = state.rotate(rotation);
            world.validate(state);
            writes.add(new Write(position, state));
        }
        // Legacy Minecraft validates attachments even when neighbor notifications are disabled.
        // Place structural supports before attached decorations, preserving order within each tier.
        Collections.sort(writes, new Comparator<Write>() {
            @Override public int compare(Write first, Write second) {
                return Integer.compare(attachment(first.state), attachment(second.state));
            }
        });
        for (Write write : writes) world.set(write.position.x, write.position.y, write.position.z, write.state);
        return Result.PLACED;
    }

    private static int attachment(BlockState state) {
        String name = state.name;
        return name.endsWith("torch") || name.endsWith("_button") || name.endsWith("_head")
                || name.equals("minecraft:cocoa") || name.equals("minecraft:vine") || name.startsWith("minecraft:potted_") ? 1 : 0;
    }
}
