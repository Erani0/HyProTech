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

public final class OreCrusherRecipes {
    private static volatile List<RecipeEntry> RECIPE_CACHE;

    private OreCrusherRecipes() {
    }

    public static List<RecipeEntry> getRecipes() {
        if (RECIPE_CACHE != null) {
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
        Map<String, RecipeEntry> byInput = new LinkedHashMap<>();
        Map<String, CraftingRecipe> map = CraftingRecipe.getAssetMap().getAssetMap();
        if (map != null && !map.isEmpty()) {
            for (CraftingRecipe recipe : map.values()) {
                if (recipe == null || !isOreCrusherRecipe(recipe)) {
                    continue;
                }
                MaterialQuantity input = resolvePrimaryInput(recipe);
                if (input == null || input.getItemId() == null || input.getItemId().isEmpty()) {
                    continue;
                }
                String inputId = input.getItemId();
                if (!isBaseOreId(inputId)) {
                    continue;
                }
                MaterialQuantity output = resolvePrimaryOutput(recipe);
                if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
                    continue;
                }
                if (!byInput.containsKey(inputId)) {
                    RecipeEntry entry = new RecipeEntry(
                            recipe.getId(),
                            inputId,
                            Math.max(1, input.getQuantity()),
                            output.getItemId(),
                            Math.max(1, output.getQuantity()),
                            recipe.getInput(),
                            recipe.getOutputs());
                    byInput.put(inputId, entry);
                }
            }
        }

        addVanillaOreEntries(byInput);
        List<RecipeEntry> entries = new ArrayList<>(byInput.values());
        entries.sort(Comparator.comparing(o -> o.inputItemId.toLowerCase(Locale.ROOT)));
        return entries;
    }

    private static void addVanillaOreEntries(Map<String, RecipeEntry> byInput) {
        String[][] vanilla = {
                {"Ore_Copper", "Machinarium_Copper_Powder"},
                {"Ore_Iron", "Machinarium_Iron_Powder"},
                {"Ore_Gold", "Machinarium_Gold_Powder"},
                {"Ore_Silver", "Machinarium_Silver_Powder"},
                {"Ore_Cobalt", "Machinarium_Cobalt_Powder"},
                {"Ore_Thorium", "Machinarium_Thorium_Powder"},
                {"Ore_Adamantite", "Machinarium_Adamantite_Powder"}
        };
        for (String[] pair : vanilla) {
            String inputId = pair[0];
            String outputId = pair[1];
            if (byInput.containsKey(inputId)) {
                continue;
            }
            if (!isKnownItemId(inputId) || !isKnownItemId(outputId)) {
                continue;
            }
            RecipeEntry entry = new RecipeEntry(
                    "vanilla:" + inputId.toLowerCase(Locale.ROOT),
                    inputId,
                    1,
                    outputId,
                    1,
                    new MaterialQuantity[] { new MaterialQuantity(inputId, null, null, 1, null) },
                    new MaterialQuantity[] { new MaterialQuantity(outputId, null, null, 1, null) });
            byInput.put(inputId, entry);
        }
    }

    private static boolean isOreCrusherRecipe(CraftingRecipe recipe) {
        BenchRequirement[] requirements = recipe.getBenchRequirement();
        if (requirements == null || requirements.length == 0) {
            return false;
        }
        for (BenchRequirement requirement : requirements) {
            if (requirement == null || requirement.type != BenchType.Processing) {
                continue;
            }
            if ("OreCrusher".equalsIgnoreCase(requirement.id)) {
                return true;
            }
        }
        return false;
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

    private static boolean isBaseOreId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        if (itemId.startsWith("Ore_")) {
            return itemId.indexOf('_', 4) < 0;
        }
        if (itemId.endsWith("_Ore")) {
            return !itemId.contains("Block") && !itemId.contains("block");
        }
        return false;
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
