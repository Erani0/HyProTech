package com.shailist.hytale.api.transfer.v1.storage.base;

import com.google.common.base.Supplier;
import com.shailist.hytale.api.transfer.v1.storage.Storage;
import com.shailist.hytale.api.transfer.v1.storage.StorageView;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import java.util.Iterator;

public class FilteringStorage<T> implements Storage<T> {
    protected final Supplier<Storage<T>> backingStorage;

    public FilteringStorage(Storage<T> backingStorage) {
        this(() -> backingStorage);
    }

    public FilteringStorage(Supplier<Storage<T>> backingStorage) {
        this.backingStorage = backingStorage;
    }

    protected boolean canInsert(T resource, long amount) {
        return true;
    }

    protected boolean canExtract(T resource, long amount) {
        return true;
    }

    @Override
    public long insert(T resource, long maxAmount, TransactionContext transaction) {
        if (canInsert(resource, maxAmount)) {
            return backingStorage.get().insert(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public long extract(T resource, long maxAmount, TransactionContext transaction) {
        if (canExtract(resource, maxAmount)) {
            return backingStorage.get().extract(resource, maxAmount, transaction);
        }
        return 0;
    }

    @Override
    public Iterator<StorageView<T>> iterator() {
        Iterator<StorageView<T>> backingIterator = backingStorage.get().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return backingIterator.hasNext();
            }

            @Override
            public StorageView<T> next() {
                return new FilteringStorageView(backingIterator.next());
            }

            @Override
            public void remove() {
                backingIterator.remove();
            }
        };
    }

    private class FilteringStorageView implements StorageView<T> {
        private final StorageView<T> backingView;

        private FilteringStorageView(StorageView<T> backingView) {
            this.backingView = backingView;
        }

        @Override
        public boolean isResourceBlank() {
            return backingView.isResourceBlank();
        }

        @Override
        public T getResource() {
            return backingView.getResource();
        }

        @Override
        public long getAmount() {
            return backingView.getAmount();
        }

        @Override
        public long getCapacity() {
            return backingView.getCapacity();
        }

        @Override
        public long extract(T resource, long maxAmount, TransactionContext transaction) {
            if (canExtract(resource, maxAmount)) {
                return backingView.extract(resource, maxAmount, transaction);
            }
            return 0;
        }

        @Override
        public StorageView<T> getUnderlyingView() {
            return backingView.getUnderlyingView();
        }
    }
}
