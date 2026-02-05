package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.energy.EnergySide;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

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
        int progressMax = energy.getProgressMax();
        if (progressMax > 0 && machine.getProgressMax() != progressMax) {
            machine.setProgressMax(progressMax);
        }
        int progress = energy.getProgress();
        if (machine.getProgress() != progress) {
            machine.setProgress(progress);
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

        if (!canFitOutputs(container, recipe)) {
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
            if (changed) {
                context.markDirty();
            }
            return changed;
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
        boolean outputAdded = applyOutputs(container, recipe);
        if (outputAdded) {
            container.removeItemStackFromSlot(INPUT_SLOT, requiredInput);
            MachineItemAccess.markContainerDirty(context.getWorld(), context.getX(), context.getY(), context.getZ());
            changed = true;
        }
        context.markDirty();
        return changed;
    }

    private boolean canFitOutputs(ItemContainer container, OreCrusherRecipes.RecipeEntry recipe) {
        MaterialQuantity[] outputs = recipe.outputs;
        if (outputs == null || outputs.length == 0) {
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
        ItemStack stack = new ItemStack(itemId, Math.max(1, quantity));
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            short slot = (short) (OUTPUT_SLOT_START + i);
            if (slot < 0 || slot >= container.getCapacity()) {
                continue;
            }
            ItemStack existing = container.getItemStack(slot);
            if (existing != null && !ItemStack.isEmpty(existing)
                    && !existing.getItemId().equalsIgnoreCase(itemId)) {
                continue;
            }
            if (container.canAddItemStackToSlot(slot, stack, true, true)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyOutputs(ItemContainer container, OreCrusherRecipes.RecipeEntry recipe) {
        boolean addedAny = false;
        MaterialQuantity[] outputs = recipe.outputs;
        if (outputs == null || outputs.length == 0) {
            outputs = new MaterialQuantity[] {
                    new MaterialQuantity(recipe.outputItemId, null, null, recipe.outputQuantity, null)
            };
        }
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
                if (!container.canAddItemStackToSlot(slot, stack, true, true)) {
                    continue;
                }
                if (container.addItemStackToSlot(slot, stack).succeeded()) {
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

    private boolean consumeEnergy(EnergyStorage storage, int amount) {
        if (storage == null || amount <= 0) {
            return false;
        }
        TransactionContext context = TransactionContext.current();
        try (Transaction transaction = Transaction.openNested(context)) {
            long extracted = storage.extract(amount, transaction);
            if (extracted >= amount) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }

    private static boolean isIdOrState(String blockId, String baseId) {
        if (blockId == null || baseId == null) {
            return false;
        }
        if (TieredIdUtil.isTieredId(blockId, baseId)) {
            return true;
        }
        String normalized = TieredIdUtil.stripNamespace(blockId, baseId);
        if (normalized == null) {
            return false;
        }
        if (normalized.equalsIgnoreCase(baseId)) {
            return true;
        }
        if (!normalized.regionMatches(true, 0, baseId, 0, baseId.length())) {
            return false;
        }
        if (normalized.length() == baseId.length()) {
            return false;
        }
        char separator = normalized.charAt(baseId.length());
        return !Character.isLetterOrDigit(separator);
    }
}
