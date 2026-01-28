package com.shailist.hytale.api.transfer.v1.transaction.base;

import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;

public abstract class SnapshotParticipant<T> {
    protected abstract T createSnapshot();

    protected abstract void readSnapshot(T snapshot);

    protected void onFinalCommit() {
    }

    protected void onFinalAbort() {
    }

    public final Object createSnapshotInternal() {
        return createSnapshot();
    }

    @SuppressWarnings("unchecked")
    public final void readSnapshotInternal(Object snapshot) {
        readSnapshot((T) snapshot);
    }

    public final void onFinalCommitInternal() {
        onFinalCommit();
    }

    public final void onFinalAbortInternal() {
        onFinalAbort();
    }

    protected final void updateSnapshots(TransactionContext transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("TransactionContext may not be null.");
        }
        if (!(transaction instanceof Transaction tx)) {
            throw new IllegalArgumentException("Unsupported TransactionContext implementation.");
        }
        tx.trackParticipant(this);
    }
}
