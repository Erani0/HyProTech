package com.example.plugin.energy;

public final class CableUpgradeConfig {
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
    private static final int[] ENERGY_CAPACITY = {
            720,
            4725,
            31003,
            203440,
            1334965,
            8760000
    };
    private static final int[] ENERGY_MAX_TRANSFER = {
            5000,
            30724,
            188787,
            1160039,
            7128092,
            43800000
    };
    private static final int[] ITEM_MAX_TRANSFER = {
            16,
            64,
            160,
            320,
            640,
            1000
    };

    private CableUpgradeConfig() {
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

    public static int getEnergyCapacityForTier(int tier) {
        int safeTier = clampTier(tier);
        return ENERGY_CAPACITY[safeTier];
    }

    public static int getEnergyMaxTransferForTier(int tier) {
        int safeTier = clampTier(tier);
        return ENERGY_MAX_TRANSFER[safeTier];
    }

    public static int getItemMaxTransferForTier(int tier) {
        int safeTier = clampTier(tier);
        return ITEM_MAX_TRANSFER[safeTier];
    }

    public static Requirement[] getUpgradeRequirements(int currentTier, int cableCount) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        int count = Math.max(1, cableCount);
        switch (safeTier) {
            case 0:
                return new Requirement[] {
                        req("Ingredient_Bar_Iron", count)
                };
            case 1:
                return new Requirement[] {
                        req("Ingredient_Bar_Silver", count)
                };
            case 2:
                return new Requirement[] {
                        req("Ingredient_Bar_Gold", count)
                };
            case 3:
                return new Requirement[] {
                        req("Ingredient_Bar_Adamantite", count)
                };
            case 4:
                return new Requirement[] {
                        req("Ingredient_Bar_Adamantite", count * 2)
                };
            default:
                return NO_REQUIREMENTS;
        }
    }
}
