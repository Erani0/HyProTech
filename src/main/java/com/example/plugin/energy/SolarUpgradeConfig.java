package com.example.plugin.energy;

public final class SolarUpgradeConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;

    public static final class Requirement {
        private final String itemId;
        private final int quantity;

        public Requirement(String itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    private static final Requirement[] NO_REQUIREMENTS = new Requirement[0];
    private static final String[] TIER_NAMES = {
            "Basic",
            "Reinforced",
            "Industrial",
            "Advanced",
            "Precision",
            "Quantum"
    };
    private static final int[] CAPACITY = {
            5000,
            10000,
            20000,
            40000,
            80000,
            160000
    };
    private static final int[] GENERATION = {
            500,
            910,
            1660,
            3020,
            5490,
            10000
    };

    private SolarUpgradeConfig() {
    }

    private static Requirement req(String itemId, int quantity) {
        return new Requirement(itemId, quantity);
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

    public static boolean hasNextTier(int tier) {
        return clampTier(tier) < MAX_TIER;
    }

    public static String getTierName(int tier) {
        int safeTier = clampTier(tier);
        return TIER_NAMES[safeTier];
    }

    public static int getCapacityForTier(int tier) {
        int safeTier = clampTier(tier);
        return CAPACITY[safeTier];
    }

    public static int getGenerationForTier(int tier) {
        int safeTier = clampTier(tier);
        return GENERATION[safeTier];
    }

    public static int getMaxTransferForTier(int tier) {
        return getGenerationForTier(tier);
    }

    public static Requirement[] getUpgradeRequirements(int currentTier) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        switch (safeTier) {
            case 0:
                return new Requirement[] {
                        req("Ingredient_Bar_Copper", 1)
                };
            case 1:
                return new Requirement[] {
                        req("Ingredient_Bar_Iron", 1)
                };
            case 2:
                return new Requirement[] {
                        req("Ingredient_Bar_Silver", 1)
                };
            case 3:
                return new Requirement[] {
                        req("Ingredient_Bar_Gold", 1)
                };
            case 4:
                return new Requirement[] {
                        req("Ingredient_Bar_Cobalt", 1)
                };
            default:
                return NO_REQUIREMENTS;
        }
    }
}
