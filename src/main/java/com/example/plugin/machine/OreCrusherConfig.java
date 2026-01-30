package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class OreCrusherConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    private static final int TICKS_PER_SECOND = 20;
    private static final double BASIC_SPEED_SECONDS = 0.4;
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
            20000,
            50000,
            100000,
            200000,
            400000
    };

    private static final int[] CONSUMPTION_PER_SECOND = {
            200,
            500,
            1000,
            2000,
            4000,
            8000
    };

    private static final int[] OUTPUT_MULTIPLIER = {
            2,
            3,
            4,
            5,
            6,
            6
    };

    private static final String[] ORE_ITEMS = {
            "Ore_Adamantite",
            "Ore_Adamantite_Basalt",
            "Ore_Adamantite_Shale",
            "Ore_Adamantite_Slate",
            "Ore_Adamantite_Stone",
            "Ore_Adamantite_Volcanic",
            "Ore_Cobalt",
            "Ore_Cobalt_Basalt",
            "Ore_Cobalt_Sandstone",
            "Ore_Cobalt_Shale",
            "Ore_Cobalt_Slate",
            "Ore_Cobalt_Stone",
            "Ore_Cobalt_Volcanic",
            "Ore_Copper",
            "Ore_Copper_Basalt",
            "Ore_Copper_Sandstone",
            "Ore_Copper_Shale",
            "Ore_Copper_Stone",
            "Ore_Copper_Volcanic",
            "Ore_Gold",
            "Ore_Gold_Basalt",
            "Ore_Gold_Sandstone",
            "Ore_Gold_Shale",
            "Ore_Gold_Stone",
            "Ore_Gold_Volcanic",
            "Ore_Iron",
            "Ore_Iron_Basalt",
            "Ore_Iron_Sandstone",
            "Ore_Iron_Shale",
            "Ore_Iron_Slate",
            "Ore_Iron_Stone",
            "Ore_Iron_Volcanic",
            "Ore_Mithril",
            "Ore_Mithril_Basalt",
            "Ore_Mithril_Magma",
            "Ore_Mithril_Slate",
            "Ore_Mithril_Stone",
            "Ore_Mithril_Volcanic",
            "Ore_Onyxium",
            "Ore_Onyxium_Basalt",
            "Ore_Onyxium_Sandstone",
            "Ore_Onyxium_Shale",
            "Ore_Onyxium_Stone",
            "Ore_Onyxium_Volcanic",
            "Ore_Prisma",
            "Ore_Silver",
            "Ore_Silver_Basalt",
            "Ore_Silver_Sandstone",
            "Ore_Silver_Shale",
            "Ore_Silver_Slate",
            "Ore_Silver_Stone",
            "Ore_Silver_Volcanic",
            "Ore_Thorium",
            "Ore_Thorium_Basalt",
            "Ore_Thorium_Sandstone",
            "Ore_Thorium_Shale",
            "Ore_Thorium_Stone",
            "Ore_Thorium_Volcanic"
    };

    private static final Set<String> ORE_ITEM_SET =
            new HashSet<>(Arrays.asList(ORE_ITEMS));

    private OreCrusherConfig() {
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

    public static int getProcessingDelayTicks(int tier) {
        double seconds = getProcessingSecondsForTier(tier);
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    public static double getProcessingSecondsForTier(int tier) {
        int safeTier = clampTier(tier);
        if (MAX_TIER == MIN_TIER) {
            return BASIC_SPEED_SECONDS;
        }
        double ratio = (double) (safeTier - MIN_TIER) / (double) (MAX_TIER - MIN_TIER);
        return BASIC_SPEED_SECONDS + (QUANTUM_SPEED_SECONDS - BASIC_SPEED_SECONDS) * ratio;
    }

    public static int getOutputMultiplierForTier(int tier) {
        return OUTPUT_MULTIPLIER[clampTier(tier)];
    }

    public static String getOutputItemId() {
        return MachinariumIds.ITEM_COPPER_POWDER;
    }

    public static boolean isOreItem(String itemId) {
        return itemId != null && ORE_ITEM_SET.contains(itemId);
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
