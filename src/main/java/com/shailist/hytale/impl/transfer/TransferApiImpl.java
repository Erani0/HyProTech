package com.shailist.hytale.impl.transfer;

import java.util.Collections;
import java.util.Iterator;

public final class TransferApiImpl {
    private TransferApiImpl() {
    }

    public static <T> Iterator<T> singletonIterator(T value) {
        return Collections.singleton(value).iterator();
    }
}
