package org.infotoast.lorax.common;

public final class Position {
    public final int x, y, z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position rotate(int turns) {
        switch (turns) {
            case 0: return this;
            case 1: return new Position(-z, y, x);
            case 2: return new Position(-x, y, -z);
            case 3: return new Position(z, y, -x);
            default: throw new IllegalArgumentException("Rotation must be between 0 and 3");
        }
    }

    public Position add(Position other) {
        return new Position(x + other.x, y + other.y, z + other.z);
    }
}
