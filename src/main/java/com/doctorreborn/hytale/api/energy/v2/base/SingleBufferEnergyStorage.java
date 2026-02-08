/*
 * Copyright (C) 2026 DoctorReborn and contributors
 *
 * This file is part of Energy API for Hytale.
 *
 * Energy API is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Energy API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Energy API. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2025 Shai List and contributors
 * Licensed under the MIT license. See LICENSE-SHAILIST file in the project root for details.
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 */

package com.doctorreborn.hytale.api.energy.v2.base;

import org.jspecify.annotations.NonNull;

import com.doctorreborn.hytale.api.energy.v2.Energy;
import com.shailist.hytale.api.transfer.v1.storage.StoragePreconditions;
import com.shailist.hytale.api.transfer.v1.storage.base.ResourceAmount;
import com.shailist.hytale.api.transfer.v1.storage.base.SingleSlotStorage;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import com.shailist.hytale.api.transfer.v1.transaction.base.SnapshotParticipant;

public class SingleBufferEnergyStorage extends SnapshotParticipant<ResourceAmount<Energy>>
        implements SingleSlotStorage<Energy> {
    protected long amount;
    protected long capacity;

    public SingleBufferEnergyStorage(long capacity) {
        this(capacity, 0);
    }

    protected SingleBufferEnergyStorage(long capacity, long amount) {
        this.capacity = capacity;
        this.amount = amount;
    }

    public SingleBufferEnergyStorage copy() {
        return new SingleBufferEnergyStorage(capacity, amount);
    }

    @Override
    public long insert(Energy resource, long maxAmount, @NonNull TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);

        long inserted = Math.min(maxAmount, getCapacity() - amount);

        if (inserted > 0) {
            updateSnapshots(transaction);
            amount += inserted;
            return inserted;
        }

        return 0;
    }

    public long insert(long maxAmount, @NonNull TransactionContext transaction) {
        return insert(Energy.INSTANCE, maxAmount, transaction);
    }

    @Override
    public long extract(Energy resource, long maxAmount, @NonNull TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);

        long extracted = Math.min(amount, maxAmount);

        if (extracted > 0) {
            this.updateSnapshots(transaction);
            amount -= extracted;
            return extracted;
        }

        return 0;
    }

    public long extract(long maxAmount, @NonNull TransactionContext transaction) {
        return extract(Energy.INSTANCE, maxAmount, transaction);
    }

    @Override
    public boolean isResourceBlank() {
        return false;
    }

    @Override
    public Energy getResource() {
        return Energy.INSTANCE;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    protected ResourceAmount<Energy> createSnapshot() {
        return new ResourceAmount<>(Energy.INSTANCE, amount);
    }

    @Override
    protected void readSnapshot(ResourceAmount<Energy> snapshot) {
        amount = snapshot.amount();
    }

    @Override
    public String toString() {
        return "SingleBufferEnergyStorage[" + amount + "/" + capacity + "]";
    }
}
