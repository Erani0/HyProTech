package com.example.plugin.item;

public enum ItemDistributionMode {
    ROUND_ROBIN("Round Robin"),
    NEAREST("Nearest"),
    FURTHEST("Furthest");

    private final String label;

    ItemDistributionMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ItemDistributionMode next() {
        switch (this) {
            case ROUND_ROBIN:
                return NEAREST;
            case NEAREST:
                return FURTHEST;
            case FURTHEST:
            default:
                return ROUND_ROBIN;
        }
    }
}
