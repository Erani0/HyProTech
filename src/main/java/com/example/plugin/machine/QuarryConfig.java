package com.example.plugin.machine;

public final class QuarryConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    public static final int BASE_AREA = 5;
    public static final int MIN_AREA = 2;
    private static final int TICKS_PER_SECOND = 20;
    private static final double BASIC_SPEED_SECONDS = 0.7;
    private static final double QUANTUM_SPEED_SECONDS = 0.1;

    private static final String[] TIER_NAMES = {
            "Basic",
            "Reinforced",
            "Industrial",
            "Advanced",
            "Precision",
            "Quantum"
    };


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

    private static final int[] CAPACITY = {
            5000,
            12500,
            31000,
            79000,
            200000,
            500000
    };

    private static final int[] CONSUMPTION_PER_SECOND = {
            2000,
            4000,
            7900,
            15500,
            30500,
            60000
    };

    private static final int[] MAX_AREA = {
            5,
            10,
            20,
            30,
            40,
            50
    };

    private QuarryConfig() {
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

    public static int getConsumptionPerSecond(int tier) {
        return CONSUMPTION_PER_SECOND[clampTier(tier)];
    }

    public static int getMaxTransferForTier(int tier) {
        return Math.max(1, getConsumptionPerSecond(tier));
    }

    public static int getMaxAreaForTier(int tier) {
        return MAX_AREA[clampTier(tier)];
    }

    public static int clampAreaForTier(int tier, int value) {
        int max = getMaxAreaForTier(tier);
        if (value < MIN_AREA) {
            return MIN_AREA;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static int computeConsumptionForArea(int tier, int width, int depth) {
        int basePerSecond = getConsumptionPerSecond(tier);
        int stepPerSecond = Math.max(1, (int) Math.round(basePerSecond * 0.1));
        int extraWidth = Math.max(0, width - BASE_AREA);
        int extraDepth = Math.max(0, depth - BASE_AREA);
        int extraSteps = extraWidth + extraDepth;
        long totalPerSecond = (long) basePerSecond + (long) extraSteps * stepPerSecond;
        return totalPerSecond > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalPerSecond;
    }

    public static int getMiningDelayTicks(int tier) {
        double seconds = getMiningSecondsForTier(tier);
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    public static double getMiningSecondsForTier(int tier) {
        int safeTier = clampTier(tier);
        if (MAX_TIER == MIN_TIER) {
            return BASIC_SPEED_SECONDS;
        }
        double ratio = (double) (safeTier - MIN_TIER) / (double) (MAX_TIER - MIN_TIER);
        return BASIC_SPEED_SECONDS + (QUANTUM_SPEED_SECONDS - BASIC_SPEED_SECONDS) * ratio;
    }


    public static Requirement[] getUpgradeRequirements(int currentTier) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        switch (safeTier) {
            case 0:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Copper", 1),
                        new Requirement("Ingredient_Hide_Medium", 1)
                };
            case 1:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Iron", 1),
                        new Requirement("Ingredient_Fire_Essence", 1)
                };
            case 2:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Silver", 1),
                        new Requirement("Ingredient_Bar_Cobalt", 1)
                };
            case 3:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Gold", 1),
                        new Requirement("Ingredient_Bar_Thorium", 1),
                        new Requirement("Ingredient_Fire_Essence", 1)
                };
            case 4:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Adamantite", 1),
                        new Requirement("Ingredient_Bar_Cobalt", 1),
                        new Requirement("Ingredient_Hide_Medium", 1)
                };
            default:
                return NO_REQUIREMENTS;
        }
    }
}
