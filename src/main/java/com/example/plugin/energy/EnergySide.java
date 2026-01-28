package com.example.plugin.energy;

public enum EnergySide {
    EAST(1, 0, 0, "East"),
    WEST(-1, 0, 0, "West"),
    UP(0, 1, 0, "Up"),
    DOWN(0, -1, 0, "Down"),
    SOUTH(0, 0, 1, "South"),
    NORTH(0, 0, -1, "North");

    public static final EnergySide[] VALUES = values();
    public static final int ALL_MASK = (1 << VALUES.length) - 1;

    private final int dx;
    private final int dy;
    private final int dz;
    private final String label;
    private final int mask;

    EnergySide(int dx, int dy, int dz, String label) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.label = label;
        this.mask = 1 << ordinal();
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public int mask() {
        return mask;
    }

    public String label() {
        return label;
    }

    public EnergySide opposite() {
        switch (this) {
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case SOUTH:
                return NORTH;
            case NORTH:
                return SOUTH;
            default:
                return this;
        }
    }

    public static EnergySide fromName(String name) {
        if (name == null) {
            return null;
        }
        for (EnergySide side : VALUES) {
            if (side.name().equalsIgnoreCase(name) || side.label.equalsIgnoreCase(name)) {
                return side;
            }
        }
        return null;
    }

    public static EnergySide fromDelta(int dx, int dy, int dz) {
        for (EnergySide side : VALUES) {
            if (side.dx == dx && side.dy == dy && side.dz == dz) {
                return side;
            }
        }
        return null;
    }
}
