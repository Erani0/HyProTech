package com.example.plugin.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OreCrusherConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    public static final int OUTPUT_SLOT_COUNT = 3;
    private static final int TICKS_PER_SECOND = 20;

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
            9000,
            15000,
            24000,
            36000,
            52000
    };

    private static final int[] CONSUMPTION_PER_SECOND = {
            300,
            600,
            1200,
            2000,
            3200,
            4800
    };

    private static final int[] OUTPUT_MULTIPLIER = {
            1,
            1,
            2,
            2,
            3,
            3
    };

    private static final double[] PROCESSING_SECONDS = {
            10.0,
            8.0,
            7.0,
            6.0,
            5.0,
            4.5
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

    public static final class BonusDrop {
        private final String itemId;
        private final int quantity;
        private final double baseChance;
        private final double perTierChance;

        public BonusDrop(String itemId, int quantity, double baseChance, double perTierChance) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.baseChance = baseChance;
            this.perTierChance = perTierChance;
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getChance(int tier) {
            int safeTier = clampTier(tier);
            double value = baseChance + perTierChance * safeTier;
            if (value < 0.0) {
                return 0.0;
            }
            return Math.min(1.0, value);
        }
    }

    private static final Requirement[][] UPGRADE_REQUIREMENTS = {
            new Requirement[] {
                    new Requirement("Ingredient_Bar_Copper", 20),
                    new Requirement("Ingredient_Bar_Iron", 10),
                    new Requirement("Ingredient_Leather_Medium", 10),
                    new Requirement("Ingredient_Bar_Silver", 5)
            },
            new Requirement[] {
                    new Requirement("Ingredient_Bar_Iron", 20),
                    new Requirement("Ingredient_Bar_Silver", 10),
                    new Requirement("Ingredient_Bar_Gold", 8),
                    new Requirement("Ingredient_Bar_Cobalt", 6)
            },
            new Requirement[] {
                    new Requirement("Ingredient_Bar_Cobalt", 12),
                    new Requirement("Ingredient_Bar_Adamantite", 4),
                    new Requirement("Machinarium_Slag", 12),
                    new Requirement("Machinarium_Ore_Chips", 8),
                    new Requirement("Machinarium_Cobalt_Powder", 10)
            },
            new Requirement[] {
                    new Requirement("Ingredient_Bar_Adamantite", 10),
                    new Requirement("Ingredient_Bar_Thorium", 6),
                    new Requirement("Machinarium_Slag", 18),
                    new Requirement("Machinarium_Ore_Chips", 12),
                    new Requirement("Machinarium_Iron_Powder", 16)
            },
            new Requirement[] {
                    new Requirement("Ingredient_Bar_Adamantite", 20),
                    new Requirement("Ingredient_Bar_Thorium", 10),
                    new Requirement("Machinarium_Slag", 25),
                    new Requirement("Machinarium_Ore_Chips", 18),
                    new Requirement("Machinarium_Gold_Powder", 20)
            },
            new Requirement[0]
    };

    private static final List<BonusDrop> DEFAULT_BONUS_DROPS;

    static {
        List<BonusDrop> drops = new ArrayList<>(2);
        drops.add(new BonusDrop("Machinarium_Slag", 1, 0.60, 0.03));
        drops.add(new BonusDrop("Machinarium_Ore_Chips", 1, 0.10, 0.02));
        DEFAULT_BONUS_DROPS = Collections.unmodifiableList(drops);
    }

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

    public static int getOutputMultiplierForTier(int tier) {
        return OUTPUT_MULTIPLIER[clampTier(tier)];
    }

    public static double getProcessingSecondsForTier(int tier) {
        return PROCESSING_SECONDS[clampTier(tier)];
    }

    public static int getProcessingDelayTicks(int tier) {
        double seconds = getProcessingSecondsForTier(tier);
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    public static Requirement[] getUpgradeRequirements(int currentTier) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return new Requirement[0];
        }
        Requirement[] requirements = UPGRADE_REQUIREMENTS[safeTier];
        if (requirements == null) {
            return new Requirement[0];
        }
        return requirements;
    }

    public static List<BonusDrop> getBonusDropsForOre(String oreId) {
        if (oreId == null || oreId.isEmpty()) {
            return Collections.emptyList();
        }
        if (!oreId.startsWith("Ore_")) {
            return Collections.emptyList();
        }
        return DEFAULT_BONUS_DROPS;
    }
}
