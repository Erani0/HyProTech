package com.shailist.hytale.api.transfer.v1.storage.base;

import com.shailist.hytale.api.transfer.v1.storage.Storage;
import com.shailist.hytale.api.transfer.v1.storage.StorageView;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import java.util.Iterator;
import java.util.List;

public class CombinedSlottedStorage<T, S extends SingleSlotStorage<T>> implements Storage<T> {
    protected final List<S> parts;

    public CombinedSlottedStorage(List<S> parts) {
        this.parts = parts;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;
        for (S part : parts) {
            amount += part.insert(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) {
                return amount;
            }
        }
        return amount;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        long amount = 0;
        for (S part : parts) {
            amount += part.extract(resource, maxAmount - amount, transaction);
            if (amount == maxAmount) {
                return amount;
            }
        }
        return amount;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        Iterator<S> iterator = parts.iterator();
        return new Iterator<StorageView<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public StorageView<T> next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    public int getSlotCount() {
        return parts.size();
    }

    public S getSlot(int slot) {
        return parts.get(slot);
    }
}
