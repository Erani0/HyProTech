package com.doctorreborn.hytale.api.energy.v2.base;

import com.doctorreborn.hytale.api.energy.v2.Energy;
import com.shailist.hytale.api.transfer.v1.storage.Storage;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;

public class BufferedEnergyProducer<T extends Storage<Energy>> extends BaseEnergyProducer {
    private long lastProduced = 0;
    private T storage;

    public BufferedEnergyProducer(T storage, long energyProduction) {
        super(energyProduction);
        this.storage = storage;
    }

    @Override
    public long getLastProduced() {
        return lastProduced;
    }

    @Override
    public long produce() {
        try (Transaction transaction = Transaction.openOuter()) {
            lastProduced = storage.insert(Energy.INSTANCE, getEnergyProduction(), transaction);
            transaction.commit();
        }
        return lastProduced;
    }
}
