package com.shailist.hytale.api.transfer.v1.transaction;

public interface TransactionContext {
    static TransactionContext current() {
        return Transaction.current();
    }
}
