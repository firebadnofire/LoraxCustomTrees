package org.infotoast.lorax.common;

/** A short-lived generation context. Implementations must never load chunks. */
public interface WorldAccess {
    long seed();
    int minHeight();
    int maxHeight();
    boolean contains(int x, int y, int z);
    int surface(int x, int z);
    String biome(int x, int y, int z);
    String block(int x, int y, int z);
    boolean replaceable(int x, int y, int z);
    void validate(BlockState state);
    void set(int x, int y, int z, BlockState state);
}
