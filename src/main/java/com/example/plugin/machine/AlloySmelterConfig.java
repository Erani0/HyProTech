package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class AlloySmelterConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    public static final int INPUT_SLOT_COUNT = 4;
    public static final int OUTPUT_SLOT_COUNT = 3;
    public static final int CONTAINER_CAPACITY = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    private static final int TICKS_PER_SECOND = 20;

    private static final double[] PROCESSING_SECONDS_BY_TIER = {
            30.0, // Basic
            20.0, // Reinforced
            12.0, // Industrial
            6.0,  // Advanced
            3.0,  // Precision
            1.0   // Quantum
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

    public static final class Recipe {
        private final String name;
        private final Requirement[] inputs;
        private final String outputItemId;
        private final int outputQuantity;

        public Recipe(String name, String outputItemId, int outputQuantity, Requirement... inputs) {
            this.name = name == null ? "" : name;
            this.outputItemId = outputItemId;
            this.outputQuantity = Math.max(1, outputQuantity);
            this.inputs = inputs == null ? new Requirement[0] : inputs.clone();
        }

        public String getName() {
            return name;
        }

        public Requirement[] getInputs() {
            return inputs.clone();
        }

        public String getOutputItemId() {
            return outputItemId;
        }

        public int getOutputQuantity() {
            return outputQuantity;
        }

        public boolean matches(Map<String, Integer> counts) {
            if (counts == null) {
                return false;
            }
            for (Requirement requirement : inputs) {
                if (requirement == null) {
                    continue;
                }
                int have = counts.getOrDefault(requirement.getItemId(), 0);
                if (have < requirement.getQuantity()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final Requirement[] NO_REQUIREMENTS = new Requirement[0];

    private static final int[] CAPACITY = {
            8000,
            25000,
            60000,
            120000,
            240000,
            500000
    };

    private static final int[] CONSUMPTION_PER_SECOND = {
            300,
            700,
            1500,
            3000,
            6000,
            12000
    };

    private static final int[] OUTPUT_MULTIPLIER = {
            1,
            1,
            1,
            1,
            1,
            1
    };

    private static final double[] BYPRODUCT_SLAG_CHANCE = {
            0.60,
            0.60,
            0.60,
            0.60,
            0.60,
            0.60
    };

    private static final double[] BYPRODUCT_CHIPS_CHANCE = {
            0.10,
            0.10,
            0.10,
            0.10,
            0.10,
            0.10
    };

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

    private static final BonusDrop BYPRODUCT_SLAG =
            new BonusDrop(MachinariumIds.ITEM_SLAG, 1, BYPRODUCT_SLAG_CHANCE);
    private static final BonusDrop BYPRODUCT_CHIPS =
            new BonusDrop(MachinariumIds.ITEM_ORE_CHIPS, 1, BYPRODUCT_CHIPS_CHANCE);

    private static final List<Recipe> RECIPES;
    private static final Set<String> VALID_INPUT_ITEM_IDS;
    private static final String RECIPE_LIST_TEXT;

    static {
        List<Recipe> list = new ArrayList<>();
        list.add(new Recipe(
                "Bronze",
                "Ingredient_Bar_Bronze",
                1,
                new Requirement("Ingredient_Bar_Copper", 3),
                new Requirement("Ingredient_Bar_Iron", 1)));
        list.add(new Recipe(
                "Electrum",
                "Machinarium_Alloy_Electrum",
                1,
                new Requirement("Ingredient_Bar_Silver", 2),
                new Requirement("Ingredient_Bar_Gold", 1)));
        list.add(new Recipe(
                "Steel",
                "Machinarium_Alloy_Steel",
                1,
                new Requirement("Ingredient_Bar_Iron", 2),
                new Requirement("Ingredient_Bar_Cobalt", 1)));
        list.add(new Recipe(
                "Invar",
                "Machinarium_Alloy_Invar",
                1,
                new Requirement("Ingredient_Bar_Iron", 2),
                new Requirement("Ingredient_Bar_Silver", 1)));
        list.add(new Recipe(
                "Constantan",
                "Machinarium_Alloy_Constantan",
                1,
                new Requirement("Ingredient_Bar_Copper", 2),
                new Requirement("Ingredient_Bar_Cobalt", 1)));
        list.add(new Recipe(
                "Thorium Alloy",
                "Machinarium_Alloy_Thorium",
                1,
                new Requirement("Ingredient_Bar_Thorium", 2),
                new Requirement("Ingredient_Bar_Adamantite", 1)));
        RECIPES = Collections.unmodifiableList(list);
        Set<String> validInputs = new HashSet<>();
        for (Recipe recipe : RECIPES) {
            if (recipe == null) {
                continue;
            }
            for (Requirement input : recipe.getInputs()) {
                if (input != null && input.getItemId() != null && !input.getItemId().isBlank()) {
                    validInputs.add(input.getItemId());
                }
            }
        }
        VALID_INPUT_ITEM_IDS = Collections.unmodifiableSet(validInputs);

        StringBuilder builder = new StringBuilder();
        for (Recipe recipe : RECIPES) {
            builder.append("- ")
                    .append(recipe.getName())
                    .append(": ");
            Requirement[] inputs = recipe.getInputs();
            for (int i = 0; i < inputs.length; i++) {
                Requirement input = inputs[i];
                if (input == null) {
                    continue;
                }
                if (i > 0) {
                    builder.append(" + ");
                }
                builder.append(input.getQuantity())
                        .append("x ")
                        .append(formatItemName(input.getItemId()));
            }
            builder.append(" -> ")
                    .append(recipe.getOutputQuantity())
                    .append("x ")
                    .append(formatItemName(recipe.getOutputItemId()))
                    .append('\n');
        }
        RECIPE_LIST_TEXT = builder.toString().trim();
    }

    private AlloySmelterConfig() {
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
        return PROCESSING_SECONDS_BY_TIER[0];
    }

    public static int getOutputMultiplierForTier(int tier) {
        return OUTPUT_MULTIPLIER[clampTier(tier)];
    }

    public static Requirement[] getUpgradeRequirements(int currentTier) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        switch (safeTier) {
            case 0:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Iron", 6),
                        new Requirement("Ingredient_Bar_Copper", 4)
                };
            case 1:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Silver", 6),
                        new Requirement("Ingredient_Fire_Essence", 2)
                };
            case 2:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Gold", 6),
                        new Requirement("Ingredient_Bar_Cobalt", 4)
                };
            case 3:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Thorium", 6),
                        new Requirement("Ingredient_Fire_Essence", 4)
                };
            case 4:
                return new Requirement[] {
                        new Requirement("Ingredient_Bar_Adamantite", 8),
                        new Requirement("Ingredient_Bar_Thorium", 6)
                };
            default:
                return NO_REQUIREMENTS;
        }
    }

    public static List<Recipe> getRecipes() {
        return RECIPES;
    }

    public static Recipe findMatchingRecipe(List<ItemStack> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : inputs) {
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            counts.merge(stack.getItemId(), stack.getQuantity(), Integer::sum);
        }
        if (counts.isEmpty()) {
            return null;
        }
        for (Recipe recipe : RECIPES) {
            if (recipe != null && recipe.matches(counts)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<BonusDrop> getByproductDrops(int tier) {
        List<BonusDrop> drops = new ArrayList<>();
        drops.add(BYPRODUCT_SLAG);
        drops.add(BYPRODUCT_CHIPS);
        return drops;
    }

    public static List<ItemStack> rollByproducts(int tier, Random random) {
        if (random == null) {
            random = new Random();
        }
        List<ItemStack> drops = new ArrayList<>();
        if (random.nextDouble() < BYPRODUCT_SLAG.getChance(tier)) {
            drops.add(new ItemStack(BYPRODUCT_SLAG.getItemId(), BYPRODUCT_SLAG.getQuantity()));
        }
        if (random.nextDouble() < BYPRODUCT_CHIPS.getChance(tier)) {
            drops.add(new ItemStack(BYPRODUCT_CHIPS.getItemId(), BYPRODUCT_CHIPS.getQuantity()));
        }
        return drops;
    }

    public static String getRecipeListText() {
        return RECIPE_LIST_TEXT;
    }

    public static boolean isValidInputItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        return VALID_INPUT_ITEM_IDS.contains(itemId);
    }

    private static String formatItemName(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        String cleaned = itemId;
        if (cleaned.startsWith("Ingredient_")) {
            cleaned = cleaned.substring("Ingredient_".length());
        }
        if (cleaned.startsWith("Machinarium_")) {
            cleaned = cleaned.substring("Machinarium_".length());
        }
        String lower = cleaned.replace('_', ' ').toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
