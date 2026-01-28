package com.example.plugin.item;

public enum ItemMode {
    OFF("Off"),
    TAKE("Take"),
    PUT("Put"),
    BOTH("Take/Put");

    private final String label;

    ItemMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ItemMode next() {
        switch (this) {
            case OFF:
                return TAKE;
            case TAKE:
                return PUT;
            case PUT:
                return BOTH;
            case BOTH:
            default:
                return OFF;
        }
    }

    public boolean allowsTake() {
        return this == TAKE || this == BOTH;
    }

    public boolean allowsPut() {
        return this == PUT || this == BOTH;
    }
}
