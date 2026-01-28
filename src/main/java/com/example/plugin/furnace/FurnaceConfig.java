package com.example.plugin.furnace;

public final class FurnaceConfig {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 6;

    private FurnaceConfig() {
    }

    public static int clampTier(int tier) {
        if (tier < MIN_TIER) {
            return MIN_TIER;
        }
        if (tier > MAX_TIER) {
            return MAX_TIER;
        }
        return tier;
    }

    public static int computeConsumption(int baseConsumption, int tier) {
        long value = Math.max(0L, baseConsumption);
        int safeTier = clampTier(tier);
        for (int i = 1; i < safeTier; i++) {
            value = Math.min((long) Integer.MAX_VALUE, value * 3L);
        }
        return (int) value;
    }
}
