package HyProTechTeam.machine;

import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

final class MachineCommonUtil {
    private MachineCommonUtil() {
    }

    static boolean consumeEnergy(EnergyStorage storage, int amount) {
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

    static int getMaxStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return 100;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || item == Item.UNKNOWN) {
            return 100;
        }
        int max = item.getMaxStack();
        return max > 0 ? max : 100;
    }

    static boolean canFitOutput(
            ItemContainer container,
            String itemId,
            int quantity,
            short outputSlotStart,
            int outputSlotCount) {
        if (container == null || itemId == null || itemId.isEmpty()) {
            return false;
        }
        int remaining = Math.max(1, quantity);
        int maxStack = getMaxStack(itemId);
        for (int i = 0; i < outputSlotCount && remaining > 0; i++) {
            short slot = (short) (outputSlotStart + i);
            if (slot < 0 || slot >= container.getCapacity()) {
                continue;
            }
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || ItemStack.isEmpty(existing)) {
                remaining -= maxStack;
                continue;
            }
            if (!existing.getItemId().equalsIgnoreCase(itemId)) {
                continue;
            }
            int space = Math.max(0, maxStack - existing.getQuantity());
            if (space > 0) {
                remaining -= space;
            }
        }
        return remaining <= 0;
    }

    static boolean addToOutputSlots(
            ItemContainer container,
            String itemId,
            int quantity,
            short outputSlotStart,
            int outputSlotCount) {
        if (container == null || itemId == null || itemId.isEmpty()) {
            return false;
        }
        int remaining = Math.max(1, quantity);
        int maxStack = getMaxStack(itemId);

        for (int i = 0; i < outputSlotCount && remaining > 0; i++) {
            short slot = (short) (outputSlotStart + i);
            if (slot < 0 || slot >= container.getCapacity()) {
                continue;
            }
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || ItemStack.isEmpty(existing)) {
                continue;
            }
            if (!existing.getItemId().equalsIgnoreCase(itemId)) {
                continue;
            }
            int space = Math.max(0, maxStack - existing.getQuantity());
            if (space <= 0) {
                continue;
            }
            int add = Math.min(space, remaining);
            ItemStack merged = new ItemStack(itemId, existing.getQuantity() + add, existing.getMetadata());
            container.setItemStackForSlot(slot, merged, true);
            remaining -= add;
        }

        for (int i = 0; i < outputSlotCount && remaining > 0; i++) {
            short slot = (short) (outputSlotStart + i);
            if (slot < 0 || slot >= container.getCapacity()) {
                continue;
            }
            ItemStack existing = container.getItemStack(slot);
            if (existing != null && !ItemStack.isEmpty(existing)) {
                continue;
            }
            int add = Math.min(maxStack, remaining);
            ItemStack stack = new ItemStack(itemId, add);
            container.setItemStackForSlot(slot, stack, true);
            remaining -= add;
        }

        return remaining <= 0;
    }
}
