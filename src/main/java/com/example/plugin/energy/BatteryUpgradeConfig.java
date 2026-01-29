package com.example.plugin.energy;

public final class BatteryUpgradeConfig {
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
            50_000,
            200_000,
            800_000,
            3_200_000,
            12_800_000,
            50_000_000
    };
    private static final int[] MAX_TRANSFER = {
            5_000,
            20_000,
            80_000,
            320_000,
            1_280_000,
            5_000_000
    };

    private BatteryUpgradeConfig() {
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
        return TIER_NAMES[clampTier(tier)];
    }

    public static int getCapacityForTier(int tier) {
        return CAPACITY[clampTier(tier)];
    }

    public static int getMaxTransferForTier(int tier) {
        return MAX_TRANSFER[clampTier(tier)];
    }

    public static Requirement[] getUpgradeRequirements(int currentTier) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        switch (safeTier) {
            case 0:
                return new Requirement[] {
                        req("Ingredient_Bar_Copper", 1),
                        req("Ingredient_Leather_Medium", 1)
                };
            case 1:
                return new Requirement[] {
                        req("Ingredient_Bar_Iron", 1),
                        req("Ingredient_Fire_Essence", 1)
                };
            case 2:
                return new Requirement[] {
                        req("Ingredient_Bar_Silver", 1),
                        req("Ingredient_Bar_Cobalt", 1)
                };
            case 3:
                return new Requirement[] {
                        req("Ingredient_Bar_Gold", 1),
                        req("Ingredient_Bar_Thorium", 1),
                        req("Ingredient_Fire_Essence", 1)
                };
            case 4:
                return new Requirement[] {
                        req("Ingredient_Bar_Adamantite", 1),
                        req("Ingredient_Bar_Cobalt", 1),
                        req("Ingredient_Leather_Medium", 1)
                };
            default:
                return NO_REQUIREMENTS;
        }
    }
}
