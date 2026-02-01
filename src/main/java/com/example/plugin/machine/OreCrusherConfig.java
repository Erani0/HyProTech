package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class OreCrusherConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    public static final int INPUT_SLOT_COUNT = 1;
    public static final int OUTPUT_SLOT_COUNT = 6;
    public static final int CONTAINER_CAPACITY = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    public static final int MAX_BONUS_DROPS = 5;
    private static final int TICKS_PER_SECOND = 20;
    private static final double BASIC_SPEED_SECONDS = 5.0;
    private static final double QUANTUM_SPEED_SECONDS = 0.1;
    private static final double[] PROCESSING_SECONDS_BY_TIER = {
            5.0,  // Basic
            3.0,  // Reinforced
            2.2,  // Industrial
            1.5,  // Advanced
            0.9,  // Precision
            0.1   // Quantum
    };

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
            3,
            3,
            3,
            3
    };

    private static final double[] BONUS_POWDER_CHANCE = {
            0.05,
            0.10,
            0.15,
            0.25,
            0.35,
            0.50
    };

    private static final double[] BONUS_SCRAP_CHANCE = {
            0.01,
            0.02,
            0.03,
            0.05,
            0.07,
            0.10
    };

    private static final double[] BONUS_SLAG_CHANCE = {
            0.02,
            0.03,
            0.04,
            0.06,
            0.08,
            0.12
    };

    private static final double[] BONUS_CHIPS_CHANCE = {
            0.03,
            0.05,
            0.07,
            0.10,
            0.13,
            0.18
    };

    private static final double[] BONUS_CRYSTAL_CHANCE = {
            0.01,
            0.02,
            0.03,
            0.05,
            0.08,
            0.12
    };

    private static final double[] BONUS_DUST_CHANCE = {
            0.04,
            0.06,
            0.08,
            0.10,
            0.12,
            0.15
    };

    private static final String RANDOM_CRYSTAL_ID = "__random_crystal__";
    private static final String[] CRYSTAL_ITEMS = {
            "Ingredient_Crystal_Blue",
            "Ingredient_Crystal_Cyan",
            "Ingredient_Crystal_Green",
            "Ingredient_Crystal_Purple",
            "Ingredient_Crystal_Red",
            "Ingredient_Crystal_White",
            "Ingredient_Crystal_Yellow"
    };

    private static final BonusDrop BONUS_SCRAP =
            new BonusDrop(MachinariumIds.ITEM_SCRAP, 1, BONUS_SCRAP_CHANCE);
    private static final BonusDrop BONUS_SLAG =
            new BonusDrop(MachinariumIds.ITEM_SLAG, 1, BONUS_SLAG_CHANCE);
    private static final BonusDrop BONUS_CHIPS =
            new BonusDrop(MachinariumIds.ITEM_ORE_CHIPS, 1, BONUS_CHIPS_CHANCE);
    private static final BonusDrop BONUS_CRYSTAL =
            new BonusDrop(RANDOM_CRYSTAL_ID, 1, BONUS_CRYSTAL_CHANCE);
    private static final BonusDrop BONUS_DUST =
            new BonusDrop(MachinariumIds.ITEM_STONE_DUST, 1, BONUS_DUST_CHANCE);

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
        if (safeTier >= 0 && safeTier < PROCESSING_SECONDS_BY_TIER.length) {
            return PROCESSING_SECONDS_BY_TIER[safeTier];
        }
        return BASIC_SPEED_SECONDS;
    }

    public static int getOutputMultiplierForTier(int tier) {
        return OUTPUT_MULTIPLIER[clampTier(tier)];
    }

    public static String getOutputItemId(String oreItemId) {
        String base = getOreBase(oreItemId);
        if (base == null) {
            return MachinariumIds.ITEM_COPPER_POWDER;
        }
        switch (base) {
            case "Adamantite":
                return MachinariumIds.ITEM_ADAMANTITE_POWDER;
            case "Cobalt":
                return MachinariumIds.ITEM_COBALT_POWDER;
            case "Copper":
                return MachinariumIds.ITEM_COPPER_POWDER;
            case "Gold":
                return MachinariumIds.ITEM_GOLD_POWDER;
            case "Iron":
                return MachinariumIds.ITEM_IRON_POWDER;
            case "Silver":
                return MachinariumIds.ITEM_SILVER_POWDER;
            case "Thorium":
                return MachinariumIds.ITEM_THORIUM_POWDER;
            default:
                return MachinariumIds.ITEM_COPPER_POWDER;
        }
    }

    public static List<BonusDrop> getBonusDropsForOre(String oreItemId) {
        String base = getOreBase(oreItemId);
        if (base == null) {
            return List.of();
        }

        List<BonusDrop> drops = new ArrayList<>();
        drops.add(new BonusDrop(getOutputItemId(oreItemId), 1, BONUS_POWDER_CHANCE));
        drops.add(BONUS_SCRAP);

        switch (base) {
            case "Copper":
                drops.add(BONUS_SLAG);
                drops.add(BONUS_DUST);
                drops.add(BONUS_CHIPS);
                break;
            case "Iron":
                drops.add(BONUS_SLAG);
                drops.add(BONUS_DUST);
                drops.add(BONUS_CRYSTAL);
                break;
            case "Cobalt":
                drops.add(BONUS_CRYSTAL);
                drops.add(BONUS_CHIPS);
                drops.add(BONUS_SLAG);
                break;
            case "Gold":
                drops.add(BONUS_CRYSTAL);
                drops.add(BONUS_CHIPS);
                drops.add(BONUS_DUST);
                break;
            case "Silver":
                drops.add(BONUS_CRYSTAL);
                drops.add(BONUS_SLAG);
                drops.add(BONUS_DUST);
                break;
            case "Adamantite":
                drops.add(BONUS_CRYSTAL);
                drops.add(BONUS_SLAG);
                drops.add(BONUS_DUST);
                break;
            case "Thorium":
                drops.add(BONUS_CHIPS);
                drops.add(BONUS_SLAG);
                drops.add(BONUS_DUST);
                break;
            default:
                break;
        }

        return drops;
    }

    public static boolean isOreItem(String itemId) {
        return itemId != null && ORE_ITEM_SET.contains(itemId);
    }

    public static List<ItemStack> rollBonusDrops(String oreItemId, int tier, Random random) {
        if (random == null) {
            random = new Random();
        }
        List<BonusDrop> drops = getBonusDropsForOre(oreItemId);
        if (drops.isEmpty()) {
            return List.of();
        }
        List<ItemStack> results = new ArrayList<>();
        int safeTier = clampTier(tier);
        for (BonusDrop drop : drops) {
            if (drop == null) {
                continue;
            }
            double chance = drop.getChance(safeTier);
            if (chance <= 0) {
                continue;
            }
            if (random.nextDouble() <= chance) {
                String itemId = drop.getItemId();
                if (RANDOM_CRYSTAL_ID.equals(itemId)) {
                    if (CRYSTAL_ITEMS.length == 0) {
                        continue;
                    }
                    itemId = CRYSTAL_ITEMS[random.nextInt(CRYSTAL_ITEMS.length)];
                }
                results.add(new ItemStack(itemId, drop.getQuantity()));
            }
        }
        if (results.size() > MAX_BONUS_DROPS) {
            Collections.shuffle(results, random);
            return new ArrayList<>(results.subList(0, MAX_BONUS_DROPS));
        }
        return results;
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

    private static String getOreBase(String oreItemId) {
        if (oreItemId == null || !oreItemId.startsWith("Ore_")) {
            return null;
        }
        int start = "Ore_".length();
        int nextUnderscore = oreItemId.indexOf('_', start);
        if (nextUnderscore == -1) {
            return oreItemId.substring(start);
        }
        return oreItemId.substring(start, nextUnderscore);
    }

    public static final class BonusDrop {
        private final String itemId;
        private final int quantity;
        private final double[] chanceByTier;

        public BonusDrop(String itemId, int quantity, double[] chanceByTier) {
            this.itemId = itemId;
            this.quantity = Math.max(1, quantity);
            this.chanceByTier = chanceByTier == null ? new double[0] : chanceByTier.clone();
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getChance(int tier) {
            if (chanceByTier.length == 0) {
                return 0.0;
            }
            int safeTier = clampTier(tier);
            if (safeTier >= 0 && safeTier < chanceByTier.length) {
                return chanceByTier[safeTier];
            }
            return chanceByTier[chanceByTier.length - 1];
        }
    }
}
