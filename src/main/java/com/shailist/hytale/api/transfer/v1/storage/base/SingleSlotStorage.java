package com.shailist.hytale.api.transfer.v1.storage.base;

import com.shailist.hytale.api.transfer.v1.storage.Storage;
import com.shailist.hytale.api.transfer.v1.storage.StorageView;
import java.util.Collections;
import java.util.Iterator;

public interface SingleSlotStorage<T> extends Storage<T>, StorageView<T> {
    @Override
    default Iterator<StorageView<T>> iterator() {
        return Collections.singleton((StorageView<T>) this).iterator();
    }
}
