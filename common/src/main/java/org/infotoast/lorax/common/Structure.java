package org.infotoast.lorax.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Structure {
    public enum Kind { BLOCK, RANDOM_BLOCK, GROUND_CHECK, BLOCK_CHECK }

    public static final class Entry {
        public final Kind kind;
        public final Position position;
        public final List<BlockState> blocks;
        public final List<Integer> chances;

        public Entry(Kind kind, Position position, List<BlockState> blocks, List<Integer> chances) {
            this.kind = kind;
            this.position = position;
            this.blocks = Collections.unmodifiableList(new ArrayList<BlockState>(blocks));
            this.chances = Collections.unmodifiableList(new ArrayList<Integer>(chances));
        }
    }

    public final String name, author, description;
    public final int version, minHeight, maxHeight;
    public final boolean rotateRandomly;
    public final List<Entry> entries;

    public Structure(String name, String author, String description, int version, int minHeight,
                     int maxHeight, boolean rotateRandomly, List<Entry> entries) {
        this.name = name;
        this.author = author;
        this.description = description;
        this.version = version;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.rotateRandomly = rotateRandomly;
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
    }
}
