package com.shailist.hytale.api.transfer.v1.storage;

import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

public interface StorageView<T> {
    boolean isResourceBlank();

    T getResource();

    long getAmount();

    long getCapacity();

    long extract(T resource, long maxAmount, TransactionContext transaction);

    default StorageView<T> getUnderlyingView() {
        return this;
    }
}
