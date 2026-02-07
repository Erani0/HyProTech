package com.example.plugin.mac;

import java.util.List;

public final class MacSettings {
    public Battery battery = new Battery();
    public Solar solar = new Solar();
    public Wind wind = new Wind();
    public Cable cable = new Cable();
    public Furnace furnace = new Furnace();
    public OreCrusher oreCrusher = new OreCrusher();
    public AlloySmelter alloySmelter = new AlloySmelter();
    public Quarry quarry = new Quarry();

    public static final class Requirement {
        public String itemId;
        public Integer quantity;
    }

    public static final class RequirementGroup {
        public List<Requirement> items;
    }

    public static final class BonusDrop {
        public String itemId;
        public Integer quantity;
        public Double baseChance;
        public Double perTierChance;
    }

    public static final class Battery {
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> maxTransfer;
        public List<RequirementGroup> upgradeRequirements;
    }

    public static final class Solar {
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> generation;
        public List<RequirementGroup> upgradeRequirements;
    }

    public static final class Wind {
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> generation;
        public List<RequirementGroup> upgradeRequirements;
    }

    public static final class Cable {
        public List<String> tierNames;
        public List<Integer> energyCapacity;
        public List<Integer> energyMaxTransfer;
        public List<Integer> itemMaxTransfer;
        public List<RequirementGroup> upgradeRequirements;
    }

    public static final class Furnace {
        public Integer consumptionMultiplier;
    }

    public static final class OreCrusher {
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> consumptionPerSecond;
        public List<Integer> outputMultiplier;
        public List<Double> processingSeconds;
        public List<RequirementGroup> upgradeRequirements;
        public List<BonusDrop> bonusDrops;
        public BonusDrop powderBonusDrop;
    }

    public static final class AlloySmelter {
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> consumptionPerSecond;
        public List<Integer> outputMultiplier;
        public List<Double> processingSeconds;
        public List<RequirementGroup> upgradeRequirements;
        public List<BonusDrop> bonusDrops;
    }

    public static final class Quarry {
        public Integer baseArea;
        public Integer minArea;
        public Double basicSpeedSeconds;
        public Double quantumSpeedSeconds;
        public Integer backfillPlaceDelayTicks;
        public Integer backfillPostDelayTicks;
        public List<String> tierNames;
        public List<Integer> capacity;
        public List<Integer> consumptionPerSecond;
        public List<Integer> maxArea;
        public List<RequirementGroup> upgradeRequirements;
        public Boolean forceReplaceBlocks;
    }
}
