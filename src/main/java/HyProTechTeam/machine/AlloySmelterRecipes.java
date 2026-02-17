package HyProTechTeam.machine;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AlloySmelterRecipes {
    private static volatile List<RecipeEntry> RECIPE_CACHE;
    private static volatile Field itemRecipeField;
    private static HashSet<String> allowed_alloy = new HashSet<>();

    static {
        allowed_alloy.add("HyProTech_Ingot_High_Tier_Alloy");
        allowed_alloy.add("HyProTech_Ingot_TiV");
        allowed_alloy.add("HyProTech_Ingot_Superalloy");
        allowed_alloy.add("HyProTech_Ingot_Composite_Alloy");
        allowed_alloy.add("HyProTech_Ingot_Constantan");
        allowed_alloy.add("HyProTech_Ingot_Invar");
        allowed_alloy.add("HyProTech_Ingot_Steel");
    }

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
                addRecipeIfMatch(byOutput, recipe);
            }
        }
        addItemGeneratedRecipes(byOutput);

        addFallbackEntries(byOutput);
        List<RecipeEntry> entries = new ArrayList<>(byOutput.values());
        entries.sort(Comparator.comparing(o -> o.outputItemId.toLowerCase(Locale.ROOT)));
        return entries;
    }

    private static void addItemGeneratedRecipes(Map<String, RecipeEntry> byOutput) {
        Map<String, Item> items = Item.getAssetMap().getAssetMap();
        if (items == null || items.isEmpty()) {
            return;
        }
        List<CraftingRecipe> generated = new ArrayList<>();
        for (Item item : items.values()) {
            if (item == null || item == Item.UNKNOWN) {
                continue;
            }
            CraftingRecipe direct = getRecipeToGenerate(item);
            if (direct != null) {
                addRecipeIfMatch(byOutput, direct);
            }
            if (item.hasRecipesToGenerate()) {
                generated.clear();
                item.collectRecipesToGenerate(generated);
                if (!generated.isEmpty()) {
                    for (CraftingRecipe recipe : generated) {
                        addRecipeIfMatch(byOutput, recipe);
                    }
                }
            }
        }
    }

    private static CraftingRecipe getRecipeToGenerate(Item item) {
        if (item == null) {
            return null;
        }
        Field field = itemRecipeField;
        if (field == null) {
            try {
                field = Item.class.getDeclaredField("recipeToGenerate");
                field.setAccessible(true);
                itemRecipeField = field;
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
        try {
            return (CraftingRecipe) field.get(item);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void addRecipeIfMatch(Map<String, RecipeEntry> byOutput, CraftingRecipe recipe) {
        if (recipe == null || !isAlloySmelterRecipe(recipe)) {
            return;
        }
        MaterialQuantity input = resolvePrimaryInput(recipe);
        if (input == null || input.getItemId() == null || input.getItemId().isEmpty()) {
            return;
        }
        String inputId = input.getItemId();
        MaterialQuantity output = resolvePrimaryOutput(recipe);
        if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
            return;
        }
        if (!isAlloyOutput(output.getItemId())) {
            return;
        }
        
        String outputId = output.getItemId();
        if (!byOutput.containsKey(outputId)) {
            if (!isKnownItemId(outputId) || !isKnownItemId(inputId)) {
                return;
            }
            if (!areMaterialsKnown(recipe.getInput())) {
                return;
            }
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

    private static void addFallbackEntries(Map<String, RecipeEntry> byOutput) {
        addBronzeFallback(byOutput);
    }

    private static void addBronzeFallback(Map<String, RecipeEntry> byOutput) {
        String outputId = "Ingredient_Bar_Bronze";
        if (byOutput.containsKey(outputId)) {
            return;
        }
        String copperId = "Ingredient_Bar_Copper";
        String tinId = "HyProTech_Ingot_Tin";
        if (!isKnownItemId(outputId) || !isKnownItemId(copperId) || !isKnownItemId(tinId)) {
            return;
        }
        MaterialQuantity[] inputs = new MaterialQuantity[] {
                new MaterialQuantity(copperId, null, null, 3, null),
                new MaterialQuantity(tinId, null, null, 1, null)
        };
        MaterialQuantity[] outputs = new MaterialQuantity[] {
                new MaterialQuantity(outputId, null, null, 4, null)
        };
        RecipeEntry entry = new RecipeEntry(
                "fallback:" + outputId.toLowerCase(Locale.ROOT),
                copperId,
                3,
                outputId,
                4,
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
        if (itemId.startsWith("HyProTech_Alloy_")) {
            return true;
        }
        // if (itemId.startsWith("HyProTech_Ingot_Composite_Alloy")) {
        //     return true;
        // }
        if ("Ingredient_Bar_Bronze".equalsIgnoreCase(itemId)) {
            return true;
        }
        // if (itemId.equalsIgnoreCase("HyProTech_Ingot_TiV")) {
        //     return true;
        // }
        // if (itemId.equalsIgnoreCase("HyProTech_Ingot_High_Tier_Alloy")) {
        //     return true;
        // }
        // if (itemId.equalsIgnoreCase("HyProTech_Ingot_Superalloy")) {
        //     return true;
        // }
        if (itemId.contains("_Ingot_")) {
            return true;
        }

        return allowed_alloy.contains(itemId); // TMP Need full rework
        // if (itemId.contains("_Alloy_")) {
        //     return true;
        // }
        // return itemId.endsWith("_Alloy");
    }

    private static boolean isPowder(String itemId) {
        return itemId.endsWith("_Powder") || itemId.contains("_Powder_") || itemId.contains("_Powder") || itemId.contains("Powder_");
    }

    private static boolean isKnownItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        return item != null && item != Item.UNKNOWN && !item.isState();
    }

    private static boolean areMaterialsKnown(MaterialQuantity[] materials) {
        if (materials == null || materials.length == 0) {
            return true;
        }
        for (MaterialQuantity material : materials) {
            if (material == null) {
                continue;
            }
            String itemId = material.getItemId();
            if (itemId == null || itemId.isEmpty()) {
                continue;
            }
            if (!isKnownItemId(itemId)) {
                return false;
            }
        }
        return true;
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
