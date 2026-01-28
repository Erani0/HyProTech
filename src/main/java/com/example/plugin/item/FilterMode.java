package com.example.plugin.item;

public enum FilterMode {
    WHITELIST("Whitelist"),
    BLACKLIST("Blacklist");

    private final String label;

    FilterMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public FilterMode next() {
        return this == WHITELIST ? BLACKLIST : WHITELIST;
    }

    public static FilterMode fromName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("whitelist".equalsIgnoreCase(trimmed)) {
            return WHITELIST;
        }
        if ("blacklist".equalsIgnoreCase(trimmed)) {
            return BLACKLIST;
        }
        return null;
    }
}
