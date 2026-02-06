package com.example.plugin.energy;

import com.example.plugin.config.ConfigArrays;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.array.IntArrayCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import java.util.ArrayList;
import java.util.List;

public final class CableUpgradeConfig {
    public static final int MIN_TIER = 0;
    public static final int MAX_TIER = 5;
    private static final int TIER_COUNT = MAX_TIER - MIN_TIER + 1;

    private static final StringCodec STRING_CODEC = new StringCodec();
    private static final IntegerCodec INTEGER_CODEC = new IntegerCodec();
    private static final IntArrayCodec INT_ARRAY_CODEC = new IntArrayCodec();
    private static final ArrayCodec<String> STRING_ARRAY_CODEC =
            new ArrayCodec<>(STRING_CODEC, String[]::new);

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

    private static final ArrayCodec<Requirement> REQUIREMENT_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(Requirement.CODEC, Requirement[]::new);
    private static final ArrayCodec<Requirement[]> REQUIREMENT_TABLE_CODEC =
            new ArrayCodec<>(REQUIREMENT_ARRAY_CODEC, Requirement[][]::new);

    private static final Requirement[] NO_REQUIREMENTS = new Requirement[0];
    private static final String[] DEFAULT_TIER_NAMES = {
            "Basic",
            "Reinforced",
            "Industrial",
            "Advanced",
            "Precision",
            "Quantum"
    };
    private static final int[] DEFAULT_ENERGY_CAPACITY = {
            720,
            4725,
            31003,
            203440,
            1334965,
            8760000
    };
    private static final int[] DEFAULT_ENERGY_MAX_TRANSFER = {
            5000,
            30724,
            188787,
            1160039,
            7128092,
            43800000
    };
    private static final int[] DEFAULT_ITEM_MAX_TRANSFER = {
            16,
            64,
            160,
            320,
            640,
            1000
    };
    private static final Requirement[][] DEFAULT_UPGRADE_REQUIREMENTS = {
            new Requirement[] {
                    req("Ingredient_Bar_Iron", 1)
            },
            new Requirement[] {
                    req("Ingredient_Bar_Silver", 1)
            },
            new Requirement[] {
                    req("Ingredient_Bar_Gold", 1)
            },
            new Requirement[] {
                    req("Ingredient_Bar_Adamantite", 1)
            },
            new Requirement[] {
                    req("Ingredient_Bar_Adamantite", 2)
            },
            new Requirement[0]
    };

    private static String[] tierNames = DEFAULT_TIER_NAMES.clone();
    private static int[] energyCapacity = DEFAULT_ENERGY_CAPACITY.clone();
    private static int[] energyMaxTransfer = DEFAULT_ENERGY_MAX_TRANSFER.clone();
    private static int[] itemMaxTransfer = DEFAULT_ITEM_MAX_TRANSFER.clone();
    private static Requirement[][] upgradeRequirements = copyRequirements(DEFAULT_UPGRADE_REQUIREMENTS);

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
                        .addField(new KeyedCodec<>("EnergyCapacity", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.energyCapacity = value;
                                    }
                                },
                                config -> config.energyCapacity)
                        .addField(new KeyedCodec<>("EnergyMaxTransfer", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.energyMaxTransfer = value;
                                    }
                                },
                                config -> config.energyMaxTransfer)
                        .addField(new KeyedCodec<>("ItemMaxTransfer", INT_ARRAY_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.itemMaxTransfer = value;
                                    }
                                },
                                config -> config.itemMaxTransfer)
                        .addField(new KeyedCodec<>("UpgradeRequirements", REQUIREMENT_TABLE_CODEC),
                                (config, value) -> {
                                    if (value != null) {
                                        config.upgradeRequirements = value;
                                    }
                                },
                                config -> config.upgradeRequirements)
                        .build();

        private String[] tierNames = DEFAULT_TIER_NAMES.clone();
        private int[] energyCapacity = DEFAULT_ENERGY_CAPACITY.clone();
        private int[] energyMaxTransfer = DEFAULT_ENERGY_MAX_TRANSFER.clone();
        private int[] itemMaxTransfer = DEFAULT_ITEM_MAX_TRANSFER.clone();
        private Requirement[][] upgradeRequirements = copyRequirements(DEFAULT_UPGRADE_REQUIREMENTS);

        public ConfigData() {
        }
    }

    private CableUpgradeConfig() {
    }

    private static Requirement req(String itemId, int quantity) {
        return new Requirement(itemId, quantity);
    }

    public static void applyConfig(ConfigData data) {
        if (data == null) {
            return;
        }
        tierNames = ConfigArrays.mergeStringArray(data.tierNames, DEFAULT_TIER_NAMES, TIER_COUNT);
        energyCapacity = ConfigArrays.mergeIntArray(data.energyCapacity, DEFAULT_ENERGY_CAPACITY, TIER_COUNT, 0);
        energyMaxTransfer = ConfigArrays.mergeIntArray(
                data.energyMaxTransfer,
                DEFAULT_ENERGY_MAX_TRANSFER,
                TIER_COUNT,
                0);
        itemMaxTransfer = ConfigArrays.mergeIntArray(
                data.itemMaxTransfer,
                DEFAULT_ITEM_MAX_TRANSFER,
                TIER_COUNT,
                0);
        upgradeRequirements = mergeRequirements(data.upgradeRequirements, DEFAULT_UPGRADE_REQUIREMENTS);
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
        return tierNames[safeTier];
    }

    public static int getEnergyCapacityForTier(int tier) {
        int safeTier = clampTier(tier);
        return energyCapacity[safeTier];
    }

    public static int getEnergyMaxTransferForTier(int tier) {
        int safeTier = clampTier(tier);
        return energyMaxTransfer[safeTier];
    }

    public static int getItemMaxTransferForTier(int tier) {
        int safeTier = clampTier(tier);
        return itemMaxTransfer[safeTier];
    }

    public static Requirement[] getUpgradeRequirements(int currentTier, int cableCount) {
        int safeTier = clampTier(currentTier);
        if (safeTier >= MAX_TIER) {
            return NO_REQUIREMENTS;
        }
        Requirement[] base = upgradeRequirements[safeTier];
        if (base == null || base.length == 0) {
            return NO_REQUIREMENTS;
        }
        int count = Math.max(1, cableCount);
        Requirement[] scaled = new Requirement[base.length];
        for (int i = 0; i < base.length; i++) {
            Requirement requirement = base[i];
            if (requirement == null) {
                continue;
            }
            long scaledQuantity = (long) requirement.quantity * count;
            int quantity = scaledQuantity > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) scaledQuantity;
            scaled[i] = new Requirement(requirement.itemId, quantity);
        }
        return scaled;
    }

    private static Requirement[][] mergeRequirements(Requirement[][] value, Requirement[][] defaults) {
        Requirement[][] result = new Requirement[TIER_COUNT][];
        for (int i = 0; i < TIER_COUNT; i++) {
            Requirement[] fallback = defaults != null && i < defaults.length ? defaults[i] : NO_REQUIREMENTS;
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
            return NO_REQUIREMENTS;
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
            return NO_REQUIREMENTS;
        }
        return cleaned.toArray(new Requirement[0]);
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
}
