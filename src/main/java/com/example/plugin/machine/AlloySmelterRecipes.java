package com.example.plugin.machine;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AlloySmelterRecipes {
    private static volatile List<RecipeEntry> RECIPE_CACHE;

    private AlloySmelterRecipes() {
    }

    public static List<RecipeEntry> getRecipes() {
        if (RECIPE_CACHE != null && !RECIPE_CACHE.isEmpty()) {
            return RECIPE_CACHE;
        }
        RECIPE_CACHE = buildRecipeEntries();
        return RECIPE_CACHE;
    }

    public static RecipeEntry findByInput(String inputItemId) {
        if (inputItemId == null || inputItemId.isEmpty()) {
            return null;
        }
        for (RecipeEntry entry : getRecipes()) {
            if (entry != null && inputItemId.equalsIgnoreCase(entry.inputItemId)) {
                return entry;
            }
        }
        return null;
    }

    private static List<RecipeEntry> buildRecipeEntries() {
        Map<String, RecipeEntry> byOutput = new LinkedHashMap<>();
        Map<String, CraftingRecipe> map = CraftingRecipe.getAssetMap().getAssetMap();
        if (map != null && !map.isEmpty()) {
            for (CraftingRecipe recipe : map.values()) {
                if (recipe == null || !isAlloySmelterRecipe(recipe)) {
                    continue;
                }
                MaterialQuantity input = resolvePrimaryInput(recipe);
                if (input == null || input.getItemId() == null || input.getItemId().isEmpty()) {
                    continue;
                }
                String inputId = input.getItemId();
                MaterialQuantity output = resolvePrimaryOutput(recipe);
                if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
                    continue;
                }
                if (!isAlloyOutput(output.getItemId())) {
                    continue;
                }
                String outputId = output.getItemId();
                if (!byOutput.containsKey(outputId)) {
                    RecipeEntry entry = new RecipeEntry(
                            recipe.getId(),
                            inputId,
                            Math.max(1, input.getQuantity()),
                            outputId,
                            Math.max(1, output.getQuantity()),
                            recipe.getInput(),
                            recipe.getOutputs());
                    byOutput.put(outputId, entry);
                }
            }
        }

        addFallbackEntries(byOutput);
        List<RecipeEntry> entries = new ArrayList<>(byOutput.values());
        entries.sort(Comparator.comparing(o -> o.outputItemId.toLowerCase(Locale.ROOT)));
        return entries;
    }

    private static void addFallbackEntries(Map<String, RecipeEntry> byOutput) {
        addBronzeFallback(byOutput);
    }

    private static void addBronzeFallback(Map<String, RecipeEntry> byOutput) {
        String outputId = "Ingredient_Bar_Bronze";
        if (byOutput.containsKey(outputId)) {
            return;
        }
        String copperId = "Ingredient_Bar_Copper";
        String tinId = "Machinarium_Tin_Ingot";
        if (!isKnownItemId(outputId) || !isKnownItemId(copperId) || !isKnownItemId(tinId)) {
            return;
        }
        MaterialQuantity[] inputs = new MaterialQuantity[] {
                new MaterialQuantity(copperId, null, null, 3, null),
                new MaterialQuantity(tinId, null, null, 1, null)
        };
        MaterialQuantity[] outputs = new MaterialQuantity[] {
                new MaterialQuantity(outputId, null, null, 1, null)
        };
        RecipeEntry entry = new RecipeEntry(
                "fallback:" + outputId.toLowerCase(Locale.ROOT),
                copperId,
                3,
                outputId,
                1,
                inputs,
                outputs);
        byOutput.put(outputId, entry);
    }

    private static boolean isAlloySmelterRecipe(CraftingRecipe recipe) {
        BenchRequirement[] requirements = recipe.getBenchRequirement();
        if (requirements != null) {
            for (BenchRequirement requirement : requirements) {
                if (requirement == null || requirement.type != BenchType.Processing) {
                    continue;
                }
                if ("AlloySmelter".equalsIgnoreCase(requirement.id)) {
                    return true;
                }
            }
        }
        // Fallback: allow any processing recipe that outputs an alloy.
        MaterialQuantity output = resolvePrimaryOutput(recipe);
        return output != null && isAlloyOutput(output.getItemId());
    }

    private static MaterialQuantity resolvePrimaryInput(CraftingRecipe recipe) {
        MaterialQuantity[] inputs = recipe.getInput();
        if (inputs == null || inputs.length == 0) {
            return null;
        }
        for (MaterialQuantity input : inputs) {
            if (input != null && input.getItemId() != null && !input.getItemId().isEmpty()) {
                return input;
            }
        }
        return null;
    }

    private static MaterialQuantity resolvePrimaryOutput(CraftingRecipe recipe) {
        MaterialQuantity primary = recipe.getPrimaryOutput();
        if (primary != null && primary.getItemId() != null && !primary.getItemId().isEmpty()) {
            return primary;
        }
        MaterialQuantity[] outputs = recipe.getOutputs();
        if (outputs == null || outputs.length == 0) {
            return null;
        }
        for (MaterialQuantity output : outputs) {
            if (output != null && output.getItemId() != null && !output.getItemId().isEmpty()) {
                return output;
            }
        }
        return null;
    }

    private static boolean isAlloyOutput(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        if (isPowder(itemId) || itemId.contains("Smelter")) {
            return false;
        }
        if (itemId.startsWith("Machinarium_Alloy_")) {
            return true;
        }
        if (itemId.startsWith("Machinarium_Composite_Alloy")) {
            return true;
        }
        if ("Ingredient_Bar_Bronze".equalsIgnoreCase(itemId)) {
            return true;
        }
        if (itemId.equalsIgnoreCase("Machinarium_Titanium_Vanadium_Alloy")) {
            return true;
        }
        if (itemId.equalsIgnoreCase("Machinarium_High_Tier_Alloy_Blend")) {
            return true;
        }
        if (itemId.equalsIgnoreCase("Machinarium_Superalloy_Blend")) {
            return true;
        }
        if (itemId.contains("_Alloy_")) {
            return true;
        }
        return itemId.endsWith("_Alloy");
    }

    private static boolean isPowder(String itemId) {
        return itemId.endsWith("_Powder") || itemId.contains("_Powder_") || itemId.contains("_Powder");
    }

    private static boolean isKnownItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        return item != null && item != Item.UNKNOWN && !item.isState();
    }

    public static final class RecipeEntry {
        public final String recipeId;
        public final String inputItemId;
        public final int inputQuantity;
        public final String outputItemId;
        public final int outputQuantity;
        public final MaterialQuantity[] inputs;
        public final MaterialQuantity[] outputs;

        private RecipeEntry(
                String recipeId,
                String inputItemId,
                int inputQuantity,
                String outputItemId,
                int outputQuantity,
                MaterialQuantity[] inputs,
                MaterialQuantity[] outputs) {
            this.recipeId = recipeId == null ? "" : recipeId;
            this.inputItemId = inputItemId;
            this.inputQuantity = inputQuantity;
            this.outputItemId = outputItemId;
            this.outputQuantity = outputQuantity;
            this.inputs = inputs == null ? MaterialQuantity.EMPTY_ARRAY : inputs;
            this.outputs = outputs == null ? MaterialQuantity.EMPTY_ARRAY : outputs;
        }
    }
}
