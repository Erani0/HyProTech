package com.shailist.hytale.api.transfer.v1.storage.base;

import com.shailist.hytale.api.transfer.v1.storage.Storage;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

public interface ExtractionOnlyStorage<T> extends Storage<T> {
    @Override
    default long insert(T resource, long maxAmount, TransactionContext transaction) {
        return 0;
    }
}
