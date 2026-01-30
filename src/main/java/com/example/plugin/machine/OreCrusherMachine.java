package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.energy.EnergyNodeComponent;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

public final class OreCrusherMachine extends MasterMachine {
    public static final String ID = "machinarium:ore_crusher";
    private static final short INPUT_SLOT = 0;
    private static final short OUTPUT_SLOT = 1;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean matchesBlockId(String blockId) {
        return TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_ORE_CRUSHER);
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
        return OreCrusherConfig.getCapacityForTier(OreCrusherConfig.MIN_TIER);
    }

    @Override
    protected int defaultMaxTransfer() {
        return OreCrusherConfig.getMaxTransferForTier(OreCrusherConfig.MIN_TIER);
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
        if (!machine.isEnabled()) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
            }
            return false;
        }

        boolean changed = applyTierSettings(machine, energy);
        if (changed) {
            context.markDirty();
        }

        ItemContainer container = context.getItemContainer();
        if (container == null || container.getCapacity() <= OUTPUT_SLOT) {
            return changed;
        }

        ItemStack inputStack = container.getItemStack(INPUT_SLOT);
        if (inputStack == null
                || ItemStack.isEmpty(inputStack)
                || !OreCrusherConfig.isOreItem(inputStack.getItemId())) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
                return true;
            }
            return changed;
        }

        int tier = OreCrusherConfig.clampTier(machine.getTier());
        int outputQty = OreCrusherConfig.getOutputMultiplierForTier(tier);
        ItemStack outputStack = new ItemStack(OreCrusherConfig.getOutputItemId(), outputQty);
        if (!container.canAddItemStack(outputStack)) {
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
            return changed;
        }

        int progressMax = Math.max(1, machine.getProgressMax());
        int progress = machine.getProgress() + 1;
        if (progress < progressMax) {
            machine.setProgress(progress);
            context.markDirty();
            return true;
        }
        machine.setProgress(0);

        ItemStackSlotTransaction removeTx =
                container.removeItemStackFromSlot(INPUT_SLOT, inputStack, 1);
        if (removeTx == null || !removeTx.succeeded()) {
            return changed;
        }

        ItemStackTransaction addTx = container.addItemStack(outputStack);
        if (addTx == null || !addTx.succeeded()) {
            container.addItemStack(new ItemStack(
                    inputStack.getItemId(),
                    1,
                    inputStack.getMetadata()));
            return changed;
        }

        context.markDirty();
        return true;
    }

    private boolean applyTierSettings(MachineComponent machine, EnergyNodeComponent energy) {
        if (machine == null || energy == null) {
            return false;
        }
        boolean changed = false;
        int tier = OreCrusherConfig.clampTier(machine.getTier());
        if (machine.getTier() != tier) {
            machine.setTier(tier);
            changed = true;
        }

        int capacity = OreCrusherConfig.getCapacityForTier(tier);
        if (energy.getCapacity() != capacity) {
            energy.setCapacity(capacity);
            if (energy.getEnergy() > capacity) {
                energy.setEnergy(capacity);
            }
            changed = true;
        }

        int consumption = OreCrusherConfig.getConsumptionPerSecond(tier);
        if (energy.getConsumption() != consumption) {
            energy.setConsumption(consumption);
            changed = true;
        }

        int maxTransfer = OreCrusherConfig.getMaxTransferForTier(tier);
        if (energy.getMaxTransfer() != maxTransfer) {
            energy.setMaxTransfer(maxTransfer);
            changed = true;
        }

        int progressMax = OreCrusherConfig.getProcessingDelayTicks(tier);
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
}
