package com.example.plugin.machine;

import com.example.plugin.BlockIdUtil;
import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.energy.EnergySide;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import java.util.List;

public final class OreCrusherMachine implements MachineDefinition {
    public static final String ID = "machinarium:ore_crusher";
    private static final short INPUT_SLOT = 0;
    private static final short OUTPUT_SLOT_START = 1;
    private static final int OUTPUT_SLOT_COUNT = OreCrusherConfig.OUTPUT_SLOT_COUNT;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean matchesBlockId(String blockId) {
        return TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_ORE_CRUSHER)
                || isIdOrState(blockId, MachinariumIds.BLOCK_ORE_CRUSHER);
    }

    @Override
    public MachineComponent createMachineComponent() {
        MachineComponent component = new MachineComponent();
        component.setMachineId(ID);
        component.setTier(OreCrusherConfig.MIN_TIER);
        component.setProgressMax(OreCrusherConfig.getProcessingDelayTicks(OreCrusherConfig.MIN_TIER));
        return component;
    }

    @Override
    public EnergyNodeComponent createEnergyNode() {
        EnergyNodeComponent node = new EnergyNodeComponent();
        node.setNodeType(EnergyNodeComponent.NodeType.MACHINE);
        node.setCapacity(OreCrusherConfig.getCapacityForTier(OreCrusherConfig.MIN_TIER));
        node.setMaxTransfer(OreCrusherConfig.getMaxTransferForTier(OreCrusherConfig.MIN_TIER));
        node.setConsumption(OreCrusherConfig.getConsumptionPerSecond(OreCrusherConfig.MIN_TIER));
        node.setInputMask(EnergySide.ALL_MASK);
        node.setOutputMask(EnergySide.ALL_MASK);
        return node;
    }

    @Override
    public void configureDefaults(MachineComponent machine, EnergyNodeComponent energy) {
        if (machine == null || energy == null) {
            return;
        }
        if (energy.getNodeType() != EnergyNodeComponent.NodeType.MACHINE) {
            energy.setNodeType(EnergyNodeComponent.NodeType.MACHINE);
        }
        if (energy.getInputMask() != EnergySide.ALL_MASK) {
            energy.setInputMask(EnergySide.ALL_MASK);
        }
        if (energy.getOutputMask() != EnergySide.ALL_MASK) {
            energy.setOutputMask(EnergySide.ALL_MASK);
        }
        int tier = OreCrusherConfig.clampTier(machine.getTier());
        if (machine.getTier() != tier) {
            machine.setTier(tier);
        }
        int capacity = OreCrusherConfig.getCapacityForTier(tier);
        if (energy.getCapacity() != capacity) {
            energy.setCapacity(capacity);
            if (energy.getEnergy() > capacity) {
                energy.setEnergy(capacity);
            }
        }
        int consumption = OreCrusherConfig.getConsumptionPerSecond(tier);
        if (energy.getConsumption() != consumption) {
            energy.setConsumption(consumption);
        }
        int maxTransfer = OreCrusherConfig.getMaxTransferForTier(tier);
        if (energy.getMaxTransfer() != maxTransfer) {
            energy.setMaxTransfer(maxTransfer);
        }
        int desiredProgressMax = OreCrusherConfig.getProcessingDelayTicks(tier);
        if (machine.getProgressMax() != desiredProgressMax) {
            machine.setProgressMax(desiredProgressMax);
            if (machine.getProgress() > desiredProgressMax) {
                machine.setProgress(0);
            }
        }
        if (energy.getProgressMax() != desiredProgressMax) {
            energy.setProgressMax(desiredProgressMax);
        }
        if (energy.getProgress() != machine.getProgress()) {
            energy.setProgress(machine.getProgress());
        }
    }

    @Override
    public boolean tick(MachineContext context, float deltaSeconds) {
        if (context == null) {
            return false;
        }
        MachineComponent machine = context.getMachine();
        EnergyNodeComponent energy = context.getEnergyNode();
        if (machine == null || energy == null) {
            return false;
        }

        int beforeProgress = machine.getProgress();
        int beforeProgressMax = machine.getProgressMax();
        int beforeTier = machine.getTier();
        int beforeCapacity = energy.getCapacity();
        int beforeConsumption = energy.getConsumption();
        int beforeMaxTransfer = energy.getMaxTransfer();

        configureDefaults(machine, energy);

        boolean changed = beforeProgress != machine.getProgress()
                || beforeProgressMax != machine.getProgressMax()
                || beforeTier != machine.getTier()
                || beforeCapacity != energy.getCapacity()
                || beforeConsumption != energy.getConsumption()
                || beforeMaxTransfer != energy.getMaxTransfer();

        if (!machine.isEnabled()) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                changed = true;
            }
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        ItemContainer container = context.getItemContainer();
        if (container == null || container.getCapacity() <= INPUT_SLOT) {
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        ItemStack input = container.getItemStack(INPUT_SLOT);
        if (input == null || ItemStack.isEmpty(input)) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                changed = true;
            }
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        OreCrusherRecipes.RecipeEntry recipe = OreCrusherRecipes.findByInput(input.getItemId());
        if (recipe == null) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                changed = true;
            }
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        int requiredInput = Math.max(1, recipe.inputQuantity);
        if (input.getQuantity() < requiredInput) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                changed = true;
            }
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        int tier = OreCrusherConfig.clampTier(machine.getTier());
        if (!canFitOutputs(container, recipe, tier, input.getItemId())) {
            if (changed) {
                context.markDirty();
            }
            return changed;
        }

        int consumptionPerSecond = Math.max(0, energy.getConsumption());
        int cost = 0;
        if (consumptionPerSecond > 0 && deltaSeconds > 0f) {
            cost = (int) Math.round(consumptionPerSecond * deltaSeconds);
            if (cost < 1) {
                cost = 1;
            }
        }
        if (cost > 0 && !consumeEnergy(context.getEnergyStorage(), cost)) {
            int available = energy.getEnergy();
            if (available >= cost) {
                energy.setEnergy(available - cost);
            } else {
                if (changed) {
                    context.markDirty();
                }
                return changed;
            }
        }

        int progressMax = Math.max(1, machine.getProgressMax());
        int progress = machine.getProgress() + 1;
        if (progress < progressMax) {
            machine.setProgress(progress);
            changed = true;
            context.markDirty();
            return true;
        }

        machine.setProgress(0);
        boolean outputAdded = applyOutputs(container, recipe, tier, input.getItemId());
        if (outputAdded) {
            container.removeItemStackFromSlot(INPUT_SLOT, requiredInput);
            MachineItemAccess.markContainerDirty(context.getWorld(), context.getX(), context.getY(), context.getZ());
            changed = true;
        }
        context.markDirty();
        return changed;
    }

    private boolean canFitOutputs(
            ItemContainer container,
            OreCrusherRecipes.RecipeEntry recipe,
            int tier,
            String inputItemId) {
        List<MaterialQuantity> outputs = buildOutputList(recipe, tier, inputItemId, false);
        if (outputs.isEmpty()) {
            return false;
        }
        for (MaterialQuantity output : outputs) {
            if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
                continue;
            }
            if (!canFitOutput(container, output.getItemId(), Math.max(1, output.getQuantity()))) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitOutput(ItemContainer container, String itemId, int quantity) {
        return MachineCommonUtil.canFitOutput(
                container,
                itemId,
                quantity,
                OUTPUT_SLOT_START,
                OUTPUT_SLOT_COUNT);
    }

    private boolean applyOutputs(
            ItemContainer container,
            OreCrusherRecipes.RecipeEntry recipe,
            int tier,
            String inputItemId) {
        boolean addedAny = false;
        List<MaterialQuantity> outputs = buildOutputList(recipe, tier, inputItemId, true);
        for (MaterialQuantity output : outputs) {
            if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
                continue;
            }
            int qty = Math.max(1, output.getQuantity());
            ItemStack stack = new ItemStack(output.getItemId(), qty);
            boolean placed = false;
            for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
                short slot = (short) (OUTPUT_SLOT_START + i);
                if (slot < 0 || slot >= container.getCapacity()) {
                    continue;
                }
                ItemStack existing = container.getItemStack(slot);
                if (existing != null && !ItemStack.isEmpty(existing)
                        && !existing.getItemId().equalsIgnoreCase(output.getItemId())) {
                    continue;
                }
                if (addToOutputSlots(container, output.getItemId(), qty)) {
                    placed = true;
                    addedAny = true;
                    break;
                }
            }
            if (!placed) {
                return false;
            }
        }
        return addedAny;
    }

    private boolean addToOutputSlots(ItemContainer container, String itemId, int quantity) {
        return MachineCommonUtil.addToOutputSlots(
                container,
                itemId,
                quantity,
                OUTPUT_SLOT_START,
                OUTPUT_SLOT_COUNT);
    }

    private List<MaterialQuantity> buildOutputList(
            OreCrusherRecipes.RecipeEntry recipe,
            int tier,
            String inputItemId,
            boolean rollBonuses) {
        List<MaterialQuantity> list = new java.util.ArrayList<>();
        int multiplier = Math.max(1, OreCrusherConfig.getOutputMultiplierForTier(tier));

        MaterialQuantity[] outputs = recipe.outputs;
        if (outputs == null || outputs.length == 0) {
            list.add(new MaterialQuantity(
                    recipe.outputItemId,
                    null,
                    null,
                    Math.max(1, recipe.outputQuantity) * multiplier,
                    null));
        } else {
            for (MaterialQuantity output : outputs) {
                if (output == null || output.getItemId() == null || output.getItemId().isEmpty()) {
                    continue;
                }
                int qty = Math.max(1, output.getQuantity()) * multiplier;
                list.add(new MaterialQuantity(output.getItemId(), null, null, qty, null));
            }
        }

        String recipeOutputId = recipe.outputItemId;
        List<OreCrusherConfig.BonusDrop> bonuses =
                OreCrusherConfig.getBonusDropsForOre(inputItemId, recipeOutputId);
        if (bonuses.isEmpty()) {
            return list;
        }

        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (OreCrusherConfig.BonusDrop bonus : bonuses) {
            if (bonus == null) {
                continue;
            }
            double chance = bonus.getChance(tier);
            if (chance <= 0.0) {
                continue;
            }
            if (rollBonuses && random.nextDouble() > chance) {
                continue;
            }
            String bonusItemId = bonus.getItemId();
            if ("__ore_powder__".equals(bonusItemId)) {
                bonusItemId = OreCrusherConfig.resolvePowderId(inputItemId, recipeOutputId);
            }
            if (bonusItemId == null || bonusItemId.isEmpty()) {
                continue;
            }
            list.add(new MaterialQuantity(bonusItemId, null, null, Math.max(1, bonus.getQuantity()), null));
        }

        return list;
    }

    private boolean consumeEnergy(EnergyStorage storage, int amount) {
        return MachineCommonUtil.consumeEnergy(storage, amount);
    }

    private static boolean isIdOrState(String blockId, String baseId) {
        return BlockIdUtil.isIdOrState(blockId, baseId);
    }
}
