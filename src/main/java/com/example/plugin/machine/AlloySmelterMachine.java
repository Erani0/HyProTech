package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.sound.MachinariumSounds;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AlloySmelterMachine extends MasterMachine {
    public static final String ID = "machinarium:alloy_smelter";
    private static final short INPUT_SLOT_START = 0;
    private static final short INPUT_SLOT_END =
            (short) (INPUT_SLOT_START + AlloySmelterConfig.INPUT_SLOT_COUNT - 1);
    private static final short OUTPUT_SLOT_START =
            (short) (INPUT_SLOT_END + 1);
    private static final short OUTPUT_SLOT_END =
            (short) (OUTPUT_SLOT_START + AlloySmelterConfig.OUTPUT_SLOT_COUNT - 1);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean matchesBlockId(String blockId) {
        return TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_ALLOY_SMELTER);
    }

    @Override
    public void configureDefaults(MachineComponent machine, EnergyNodeComponent energy) {
        super.configureDefaults(machine, energy);
        if (machine != null && (machine.getMachineId() == null || machine.getMachineId().isEmpty())) {
            machine.setMachineId(ID);
        }
    }

    @Override
    protected int defaultCapacity() {
        return AlloySmelterConfig.getCapacityForTier(AlloySmelterConfig.MIN_TIER);
    }

    @Override
    protected int defaultMaxTransfer() {
        return AlloySmelterConfig.getMaxTransferForTier(AlloySmelterConfig.MIN_TIER);
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
        boolean shouldAnimate = false;
        if (!machine.isEnabled()) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
            }
            syncAnimationState(context, machine, false);
            return false;
        }

        boolean changed = applyTierSettings(machine, energy);
        if (changed) {
            context.markDirty();
        }

        ItemContainer container = context.getItemContainer();
        if (container == null || container.getCapacity() <= OUTPUT_SLOT_END) {
            return changed;
        }

        AlloySmelterConfig.Recipe recipe = findRecipe(container);
        if (recipe == null) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
                syncAnimationState(context, machine, false);
                return true;
            }
            syncAnimationState(context, machine, false);
            return changed;
        }

        int tier = AlloySmelterConfig.clampTier(machine.getTier());
        int outputQty = recipe.getOutputQuantity() * AlloySmelterConfig.getOutputMultiplierForTier(tier);
        ItemStack baseOutput = new ItemStack(recipe.getOutputItemId(), outputQty);
        List<ItemStack> outputs = new ArrayList<>();
        outputs.add(baseOutput);
        List<ItemStack> byproducts = AlloySmelterConfig.rollByproducts(tier, ThreadLocalRandom.current());
        if (byproducts != null) {
            for (ItemStack drop : byproducts) {
                if (drop != null && !ItemStack.isEmpty(drop)) {
                    outputs.add(drop);
                }
            }
        }

        if (!canAddOutputs(container, outputs)) {
            syncAnimationState(context, machine, false);
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
            syncAnimationState(context, machine, false);
            return changed;
        }

        int progressMax = Math.max(1, machine.getProgressMax());
        int progress = machine.getProgress() + 1;
        shouldAnimate = true;
        if (progress < progressMax) {
            machine.setProgress(progress);
            context.markDirty();
            syncAnimationState(context, machine, shouldAnimate);
            return true;
        }
        machine.setProgress(0);

        if (!addOutputs(container, outputs)) {
            return changed;
        }

        if (!consumeInputs(container, recipe)) {
            return changed;
        }

        MachineItemAccess.markContainerDirty(context.getWorld(), context.getX(), context.getY(), context.getZ());
        context.markDirty();
        syncAnimationState(context, machine, shouldAnimate);
        return true;
    }

    private AlloySmelterConfig.Recipe findRecipe(ItemContainer container) {
        if (container == null) {
            return null;
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (short slot = INPUT_SLOT_START; slot <= INPUT_SLOT_END && slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            inputs.add(stack);
        }
        return AlloySmelterConfig.findMatchingRecipe(inputs);
    }

    private boolean consumeInputs(ItemContainer container, AlloySmelterConfig.Recipe recipe) {
        if (container == null || recipe == null) {
            return false;
        }
        AlloySmelterConfig.Requirement[] requirements = recipe.getInputs();
        for (AlloySmelterConfig.Requirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            int remaining = requirement.getQuantity();
            for (short slot = INPUT_SLOT_START; slot <= INPUT_SLOT_END && slot < container.getCapacity(); slot++) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || ItemStack.isEmpty(stack)) {
                    continue;
                }
                if (!requirement.getItemId().equals(stack.getItemId())) {
                    continue;
                }
                int removeQty = Math.min(remaining, stack.getQuantity());
                ItemStackSlotTransaction removeTx =
                        container.removeItemStackFromSlot(slot, stack, removeQty);
                if (removeTx == null || !removeTx.succeeded()) {
                    return false;
                }
                remaining -= removeQty;
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private void syncAnimationState(MachineContext context, MachineComponent machine, boolean working) {
        if (context == null || machine == null) {
            return;
        }
        MachinariumSounds.tickLoop(
                context.getWorld(),
                context.getX(),
                context.getY(),
                context.getZ(),
                MachinariumSounds.EVENT_ALLOY_SMELTER,
                MachinariumSounds.FILE_ALLOY_SMELTER,
                MachinariumSounds.DEFAULT_LOOP_MS,
                working,
                machine);
        int tier = AlloySmelterConfig.clampTier(machine.getTier());
        if (machine.isWorking() == working && machine.getLastAnimTier() == tier) {
            return;
        }
        machine.setWorking(working);
        machine.setLastAnimTier(tier);
        context.markDirty();
        String baseState = "AlloySmelter_T" + tier;
        String stateName = working ? baseState + "_Working" : baseState;

        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(context.getX(), context.getZ());
        com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor accessor =
                context.getWorld().getChunkIfLoaded(chunkIndex);
        if (accessor == null) {
            return;
        }
        com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType blockType =
                accessor.getBlockType(context.getX(), context.getY(), context.getZ());
        if (blockType == null) {
            return;
        }
        accessor.setBlockInteractionState(context.getX(), context.getY(), context.getZ(), blockType, stateName, false);
    }

    private boolean applyTierSettings(MachineComponent machine, EnergyNodeComponent energy) {
        if (machine == null || energy == null) {
            return false;
        }
        boolean changed = false;
        int tier = AlloySmelterConfig.clampTier(machine.getTier());
        if (machine.getTier() != tier) {
            machine.setTier(tier);
            changed = true;
        }

        int capacity = AlloySmelterConfig.getCapacityForTier(tier);
        if (energy.getCapacity() != capacity) {
            energy.setCapacity(capacity);
            if (energy.getEnergy() > capacity) {
                energy.setEnergy(capacity);
            }
            changed = true;
        }

        int consumption = AlloySmelterConfig.getConsumptionPerSecond(tier);
        if (energy.getConsumption() != consumption) {
            energy.setConsumption(consumption);
            changed = true;
        }

        int maxTransfer = AlloySmelterConfig.getMaxTransferForTier(tier);
        if (energy.getMaxTransfer() != maxTransfer) {
            energy.setMaxTransfer(maxTransfer);
            changed = true;
        }

        int progressMax = AlloySmelterConfig.getProcessingDelayTicks(tier);
        if (machine.getProgressMax() != progressMax) {
            machine.setProgressMax(progressMax);
            if (machine.getProgress() > progressMax) {
                machine.setProgress(progressMax);
            }
            changed = true;
        }

        return changed;
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

    private boolean canAddOutputs(ItemContainer container, List<ItemStack> outputs) {
        if (container == null || outputs == null || outputs.isEmpty()) {
            return false;
        }
        short capacity = container.getCapacity();
        short outputSlots = (short) (OUTPUT_SLOT_END - OUTPUT_SLOT_START + 1);
        if (outputSlots <= 0 || capacity <= OUTPUT_SLOT_START) {
            return false;
        }
        SimpleItemContainer simulated = new SimpleItemContainer(outputSlots);
        for (short slot = OUTPUT_SLOT_START; slot <= OUTPUT_SLOT_END && slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            ItemStack copy = new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getMetadata());
            simulated.addItemStackToSlot((short) (slot - OUTPUT_SLOT_START), copy);
        }
        if (outputs.size() == 1) {
            return simulated.canAddItemStack(outputs.get(0));
        }
        return simulated.canAddItemStacks(outputs, true, true);
    }

    private boolean addOutputs(ItemContainer container, List<ItemStack> outputs) {
        if (container == null || outputs == null || outputs.isEmpty()) {
            return false;
        }
        for (ItemStack output : outputs) {
            if (output == null || ItemStack.isEmpty(output)) {
                continue;
            }
            if (!addOutputStack(container, output)) {
                return false;
            }
        }
        return true;
    }

    private boolean addOutputStack(ItemContainer container, ItemStack stack) {
        if (container == null || stack == null || ItemStack.isEmpty(stack)) {
            return false;
        }
        short capacity = container.getCapacity();
        if (capacity <= OUTPUT_SLOT_START) {
            return false;
        }
        // Prefer stacking onto existing output slots.
        for (short slot = OUTPUT_SLOT_START; slot <= OUTPUT_SLOT_END && slot < capacity; slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || ItemStack.isEmpty(existing)) {
                continue;
            }
            if (!existing.getItemId().equals(stack.getItemId())) {
                continue;
            }
            if (existing.getMetadata() == null ? stack.getMetadata() != null : !existing.getMetadata().equals(stack.getMetadata())) {
                continue;
            }
            if (!container.canAddItemStackToSlot(slot, stack, false, false)) {
                continue;
            }
            ItemStackSlotTransaction addTx = container.addItemStackToSlot(slot, stack);
            return addTx != null && addTx.succeeded();
        }

        // Otherwise use first empty output slot.
        for (short slot = OUTPUT_SLOT_START; slot <= OUTPUT_SLOT_END && slot < capacity; slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing != null && !ItemStack.isEmpty(existing)) {
                continue;
            }
            if (!container.canAddItemStackToSlot(slot, stack, false, false)) {
                continue;
            }
            ItemStackSlotTransaction addTx = container.addItemStackToSlot(slot, stack);
            return addTx != null && addTx.succeeded();
        }
        return false;
    }
}
