package com.shailist.hytale.api.transfer.v1.storage;

import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import java.util.Collections;
import java.util.Iterator;

public interface Storage<T> extends Iterable<StorageView<T>> {
    long insert(T resource, long maxAmount, TransactionContext transaction);

    long extract(T resource, long maxAmount, TransactionContext transaction);

    @Override
    default Iterator<StorageView<T>> iterator() {
        return Collections.emptyIterator();
    }
}
