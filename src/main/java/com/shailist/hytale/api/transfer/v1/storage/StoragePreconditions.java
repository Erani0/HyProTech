package com.shailist.hytale.api.transfer.v1.storage;

public final class StoragePreconditions {
    private StoragePreconditions() {
    }

    public static void notNegative(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative.");
        }
    }
}
