package com.example.plugin.machine;

import com.example.plugin.config.ConfigArrays;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.array.DoubleArrayCodec;
import com.hypixel.hytale.codec.codecs.array.IntArrayCodec;
import com.hypixel.hytale.codec.codecs.simple.DoubleCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class OreCrusherConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    public static final int OUTPUT_SLOT_COUNT = 6;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TIER_COUNT = MAX_TIER - MIN_TIER + 1;

    private static final StringCodec STRING_CODEC = new StringCodec();
    private static final IntegerCodec INTEGER_CODEC = new IntegerCodec();
    private static final DoubleCodec DOUBLE_CODEC = new DoubleCodec();
    private static final IntArrayCodec INT_ARRAY_CODEC = new IntArrayCodec();
    private static final DoubleArrayCodec DOUBLE_ARRAY_CODEC = new DoubleArrayCodec();
    private static final ArrayCodec<String> STRING_ARRAY_CODEC =
            new ArrayCodec<>(STRING_CODEC, String[]::new);

    private static final String[] DEFAULT_TIER_NAMES = {
            "Basic",
            "Reinforced",
            "Industrial",
            "Advanced",
            "Precision",
            "Quantum"
    };

    private static final int[] DEFAULT_CAPACITY = {
            5000,
            9000,
            15000,
            24000,
            36000,
            52000
    };

    private static final int[] DEFAULT_CONSUMPTION_PER_SECOND = {
            300,
            600,
            1200,
            2000,
            3200,
            4800
    };

    private static final int[] DEFAULT_OUTPUT_MULTIPLIER = {
            2,
            3,
            3,
            3,
            3,
            3
    };

    private static final double[] DEFAULT_PROCESSING_SECONDS = {
            10.0,
            8.2,
            6.4,
            4.6,
            2.8,
            1.0
    };

    public static final class Requirement {
        private String itemId;
        private int quantity;

        public static final BuilderCodec<Requirement> CODEC =
                BuilderCodec.builder(Requirement.class, Requirement::new)
                        .addField(new KeyedCodec<>("ItemId", STRING_CODEC),
                                (requirement, value) -> requirement.itemId = normalizeItemId(value),
                                requirement -> requirement.itemId)
                        .addField(new KeyedCodec<>("Quantity", INTEGER_CODEC),
                                (requirement, value) -> requirement.quantity = normalizeQuantity(value),
                                requirement -> requirement.quantity)
                        .build();

        public Requirement() {
            this("", 0);
        }

        public Requirement(String itemId, int quantity) {
            this.itemId = normalizeItemId(itemId);
            this.quantity = normalizeQuantity(quantity);
        }

        public String getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static final class BonusDrop {
        private String itemId;
        private int quantity;
        private double baseChance;
        private double perTierChance;

        public static final BuilderCodec<BonusDrop> CODEC =
                BuilderCodec.builder(BonusDrop.class, BonusDrop::new)
                        .addField(new KeyedCodec<>("ItemId", STRING_CODEC),
                                (drop, value) -> drop.itemId = normalizeItemId(value),
                                drop -> drop.itemId)
                        .addField(new KeyedCodec<>("Quantity", INTEGER_CODEC),
                                (drop, value) -> drop.quantity = normalizeQuantity(value),
                                drop -> drop.quantity)
                        .addField(new KeyedCodec<>("BaseChance", DOUBLE_CODEC),
                                (drop, value) -> drop.baseChance = normalizeChance(value),
                                drop -> drop.baseChance)
                        .addField(new KeyedCodec<>("PerTierChance", DOUBLE_CODEC),
                                (drop, value) -> drop.perTierChance = normalizeChance(value),
                                drop -> drop.perTierChance)
                        .build();

        public BonusDrop() {
            this("", 0, 0.0, 0.0);
        }

        public BonusDrop(String itemId, int quantity, double baseChance, double perTierChance) {
            this.itemId = normalizeItemId(itemId);
            this.quantity = normalizeQuantity(quantity);
            this.baseChance = normalizeChance(baseChance);
            this.perTierChance = normalizeChance(perTierChance);
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

        public double getBaseChance() {
            return baseChance;
        }

        public double getPerTierChance() {
            return perTierChance;
        }
    }

    private static final ArrayCodec<Requirement> REQUIREMENT_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(Requirement.CODEC, Requirement[]::new);
    private static final ArrayCodec<Requirement[]> REQUIREMENT_TABLE_CODEC =
            new ArrayCodec<>(REQUIREMENT_ARRAY_CODEC, Requirement[][]::new);
    private static final ArrayCodec<BonusDrop> BONUS_DROP_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(BonusDrop.CODEC, BonusDrop[]::new);

    private static final Requirement[][] DEFAULT_UPGRADE_REQUIREMENTS = {
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

    private static final BonusDrop[] DEFAULT_BONUS_DROPS = {
            new BonusDrop("Machinarium_Slag", 1, 0.50, 0.00),
            new BonusDrop("Machinarium_Ore_Chips", 1, 0.10, 0.00),
            new BonusDrop("Machinarium_Stone_Dust", 1, 0.50, 0.00),
            new BonusDrop("Machinarium_Scrap", 1, 0.70, 0.00)
    };
    private static final BonusDrop DEFAULT_POWDER_DROP = new BonusDrop("__ore_powder__", 1, 0.0, 0.20);

    private static String[] tierNames = DEFAULT_TIER_NAMES.clone();
    private static int[] capacity = DEFAULT_CAPACITY.clone();
    private static int[] consumptionPerSecond = DEFAULT_CONSUMPTION_PER_SECOND.clone();
    private static int[] outputMultiplier = DEFAULT_OUTPUT_MULTIPLIER.clone();
    private static double[] processingSeconds = DEFAULT_PROCESSING_SECONDS.clone();
    private static Requirement[][] upgradeRequirements = copyRequirements(DEFAULT_UPGRADE_REQUIREMENTS);
    private static BonusDrop[] bonusDrops = copyBonusDrops(DEFAULT_BONUS_DROPS);
    private static BonusDrop powderBonusDrop = DEFAULT_POWDER_DROP;
    private static List<BonusDrop> bonusDropList = toBonusDropList(bonusDrops);

    public static final class ConfigData {
        public static final BuilderCodec<ConfigData> CODEC =
                BuilderCodec.builder(ConfigData.class, ConfigData::new)
                        .addField(new KeyedCodec<>("TierNames", STRING_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.tierNames = value;
                                    }
                                },
                                config -> config.tierNames)
                        .addField(new KeyedCodec<>("Capacity", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.capacity = value;
                                    }
                                },
                                config -> config.capacity)
                        .addField(new KeyedCodec<>("ConsumptionPerSecond", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.consumptionPerSecond = value;
                                    }
                                },
                                config -> config.consumptionPerSecond)
                        .addField(new KeyedCodec<>("OutputMultiplier", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.outputMultiplier = value;
                                    }
                                },
                                config -> config.outputMultiplier)
                        .addField(new KeyedCodec<>("ProcessingSeconds", DOUBLE_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.processingSeconds = value;
                                    }
                                },
                                config -> config.processingSeconds)
                        .addField(new KeyedCodec<>("UpgradeRequirements", REQUIREMENT_TABLE_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.upgradeRequirements = value;
                                    }
                                },
                                config -> config.upgradeRequirements)
                        .addField(new KeyedCodec<>("BonusDrops", BONUS_DROP_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.bonusDrops = value;
                                    }
                                },
                                config -> config.bonusDrops)
                        .addField(new KeyedCodec<>("PowderBonusDrop", BonusDrop.CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.powderBonusDrop = value;
                                    }
                                },
                                config -> config.powderBonusDrop)
                        .build();

        private String[] tierNames = DEFAULT_TIER_NAMES.clone();
        private int[] capacity = DEFAULT_CAPACITY.clone();
        private int[] consumptionPerSecond = DEFAULT_CONSUMPTION_PER_SECOND.clone();
        private int[] outputMultiplier = DEFAULT_OUTPUT_MULTIPLIER.clone();
        private double[] processingSeconds = DEFAULT_PROCESSING_SECONDS.clone();
        private Requirement[][] upgradeRequirements = copyRequirements(DEFAULT_UPGRADE_REQUIREMENTS);
        private BonusDrop[] bonusDrops = copyBonusDrops(DEFAULT_BONUS_DROPS);
        private BonusDrop powderBonusDrop = DEFAULT_POWDER_DROP;

        public ConfigData() {
        }
    }

    private OreCrusherConfig() {
    }

    public static void applyConfig(ConfigData data) {
        if (data == null) {
            return;
        }
        tierNames = ConfigArrays.mergeStringArray(data.tierNames, DEFAULT_TIER_NAMES, TIER_COUNT);
        capacity = ConfigArrays.mergeIntArray(data.capacity, DEFAULT_CAPACITY, TIER_COUNT, 0);
        consumptionPerSecond = ConfigArrays.mergeIntArray(
                data.consumptionPerSecond,
                DEFAULT_CONSUMPTION_PER_SECOND,
                TIER_COUNT,
                0);
        outputMultiplier = ConfigArrays.mergeIntArray(
                data.outputMultiplier,
                DEFAULT_OUTPUT_MULTIPLIER,
                TIER_COUNT,
                1);
        processingSeconds = ConfigArrays.mergeDoubleArray(
                data.processingSeconds,
                DEFAULT_PROCESSING_SECONDS,
                TIER_COUNT,
                0.01);
        upgradeRequirements = mergeRequirements(data.upgradeRequirements, DEFAULT_UPGRADE_REQUIREMENTS);
        bonusDrops = mergeBonusDrops(data.bonusDrops, DEFAULT_BONUS_DROPS);
        powderBonusDrop = mergePowderDrop(data.powderBonusDrop, DEFAULT_POWDER_DROP);
        bonusDropList = toBonusDropList(bonusDrops);
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
        return tierNames[clampTier(tier)];
    }

    public static int getCapacityForTier(int tier) {
        return capacity[clampTier(tier)];
    }

    public static int getConsumptionPerSecond(int tier) {
        return consumptionPerSecond[clampTier(tier)];
    }

    public static int getMaxTransferForTier(int tier) {
        return Math.max(1, getConsumptionPerSecond(tier));
    }

    public static int getOutputMultiplierForTier(int tier) {
        return outputMultiplier[clampTier(tier)];
    }

    public static double getProcessingSecondsForTier(int tier) {
        return processingSeconds[clampTier(tier)];
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
        Requirement[] requirements = upgradeRequirements[safeTier];
        if (requirements == null) {
            return new Requirement[0];
        }
        return requirements;
    }

    public static List<BonusDrop> getBonusDropsForOre(String oreId) {
        return getBonusDropsForOre(oreId, null);
    }

    public static List<BonusDrop> getBonusDropsForOre(String oreId, String recipeOutputId) {
        if (oreId == null || oreId.isEmpty()) {
            return Collections.emptyList();
        }
        if (!isOreId(oreId)) {
            return Collections.emptyList();
        }
        String powderId = resolvePowderId(oreId, recipeOutputId);
        if (powderId == null || powderId.isEmpty() || powderBonusDrop == null) {
            return bonusDropList;
        }
        List<BonusDrop> combined = new ArrayList<>(bonusDropList.size() + 1);
        combined.addAll(bonusDropList);
        combined.add(powderBonusDrop);
        return combined;
    }

    public static List<BonusDrop> getBonusDropConfig() {
        return bonusDropList;
    }

    public static BonusDrop getPowderBonusDropConfig() {
        return powderBonusDrop;
    }

    public static String resolvePowderId(String oreId, String recipeOutputId) {
        if (recipeOutputId != null && recipeOutputId.contains("Powder")) {
            return recipeOutputId;
        }
        if (oreId == null || oreId.isEmpty()) {
            return null;
        }
        String normalized = oreId;
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        String base = normalized;
        if (normalized.startsWith("Ore_")) {
            int extra = normalized.indexOf('_', 4);
            if (extra > 4) {
                base = normalized.substring(0, extra);
            }
        }
        String override = resolvePowderOverride(base);
        if (override != null) {
            return override;
        }
        if (base.startsWith("Ore_") && base.length() > 4) {
            return "Machinarium_" + base.substring(4) + "_Powder";
        }
        if (normalized.startsWith("Machinarium_") && normalized.endsWith("_Ore")) {
            return normalized.substring(0, normalized.length() - 4) + "_Powder";
        }
        return null;
    }

    private static String resolvePowderOverride(String oreId) {
        if (oreId == null) {
            return null;
        }
        switch (oreId) {
            case "Ore_Bauxite":
                return "Machinarium_Aluminum_Powder";
            case "Ore_Cassiterite":
                return "Machinarium_Tin_Powder";
            case "Ore_Chromite":
                return "Machinarium_Chromium_Powder";
            case "Ore_Ilmenite":
                return "Machinarium_Titanium_Powder";
            case "Ore_Manganese":
                return "Machinarium_Manganese_Powder";
            case "Ore_Pentlandite":
                return "Machinarium_Nickel_Powder";
            case "Ore_Quartzite":
                return "Machinarium_Silicon_Powder";
            case "Ore_Scheelite":
                return "Machinarium_Tungsten_Powder";
            case "Ore_Spodumene":
                return "Machinarium_Lithium_Powder";
            case "Ore_Uraninite":
                return "Machinarium_Uranium_Powder";
            case "Ore_Vanadinite":
                return "Machinarium_Vanadium_Powder";
            default:
                return null;
        }
    }

    private static boolean isOreId(String oreId) {
        if (oreId == null || oreId.isEmpty()) {
            return false;
        }
        if (oreId.startsWith("Ore_")) {
            return true;
        }
        return oreId.startsWith("Machinarium_") && oreId.endsWith("_Ore");
    }

    private static Requirement[][] mergeRequirements(Requirement[][] value, Requirement[][] defaults) {
        Requirement[][] result = new Requirement[TIER_COUNT][];
        for (int i = 0; i < TIER_COUNT; i++) {
            Requirement[] fallback = defaults != null && i < defaults.length ? defaults[i] : new Requirement[0];
            result[i] = sanitizeRequirements(fallback);
        }
        if (value != null) {
            int limit = Math.min(value.length, TIER_COUNT);
            for (int i = 0; i < limit; i++) {
                Requirement[] cleaned = sanitizeRequirements(value[i]);
                if (cleaned != null) {
                    result[i] = cleaned;
                }
            }
        }
        return result;
    }

    private static Requirement[][] copyRequirements(Requirement[][] source) {
        return mergeRequirements(source, source);
    }

    private static Requirement[] sanitizeRequirements(Requirement[] requirements) {
        if (requirements == null || requirements.length == 0) {
            return new Requirement[0];
        }
        List<Requirement> cleaned = new ArrayList<>(requirements.length);
        for (Requirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            String itemId = normalizeItemId(requirement.itemId);
            int quantity = normalizeQuantity(requirement.quantity);
            if (itemId.isEmpty() || quantity <= 0) {
                continue;
            }
            cleaned.add(new Requirement(itemId, quantity));
        }
        if (cleaned.isEmpty()) {
            return new Requirement[0];
        }
        return cleaned.toArray(new Requirement[0]);
    }

    private static BonusDrop[] mergeBonusDrops(BonusDrop[] value, BonusDrop[] defaults) {
        if (value == null) {
            return copyBonusDrops(defaults);
        }
        return sanitizeBonusDrops(value);
    }

    private static BonusDrop[] copyBonusDrops(BonusDrop[] source) {
        if (source == null || source.length == 0) {
            return new BonusDrop[0];
        }
        return sanitizeBonusDrops(source);
    }

    private static BonusDrop[] sanitizeBonusDrops(BonusDrop[] drops) {
        if (drops == null || drops.length == 0) {
            return new BonusDrop[0];
        }
        List<BonusDrop> cleaned = new ArrayList<>(drops.length);
        for (BonusDrop drop : drops) {
            if (drop == null) {
                continue;
            }
            String itemId = normalizeItemId(drop.itemId);
            int quantity = normalizeQuantity(drop.quantity);
            double baseChance = normalizeChance(drop.baseChance);
            double perTierChance = normalizeChance(drop.perTierChance);
            if (itemId.isEmpty() || quantity <= 0) {
                continue;
            }
            cleaned.add(new BonusDrop(itemId, quantity, baseChance, perTierChance));
        }
        if (cleaned.isEmpty()) {
            return new BonusDrop[0];
        }
        return cleaned.toArray(new BonusDrop[0]);
    }

    private static BonusDrop mergePowderDrop(BonusDrop value, BonusDrop fallback) {
        if (value == null) {
            return sanitizeSingleDrop(fallback);
        }
        return sanitizeSingleDrop(value);
    }

    private static BonusDrop sanitizeSingleDrop(BonusDrop drop) {
        if (drop == null) {
            return null;
        }
        String itemId = normalizeItemId(drop.itemId);
        int quantity = normalizeQuantity(drop.quantity);
        double baseChance = normalizeChance(drop.baseChance);
        double perTierChance = normalizeChance(drop.perTierChance);
        if (itemId.isEmpty() || quantity <= 0) {
            return null;
        }
        return new BonusDrop(itemId, quantity, baseChance, perTierChance);
    }

    private static List<BonusDrop> toBonusDropList(BonusDrop[] drops) {
        if (drops == null || drops.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(drops));
    }

    private static String normalizeItemId(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private static int normalizeQuantity(Integer value) {
        if (value == null) {
            return 0;
        }
        return normalizeQuantity(value.intValue());
    }

    private static int normalizeQuantity(int value) {
        return Math.max(0, value);
    }

    private static double normalizeChance(Double value) {
        if (value == null) {
            return 0.0;
        }
        return normalizeChance(value.doubleValue());
    }

    private static double normalizeChance(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return value;
    }
}
