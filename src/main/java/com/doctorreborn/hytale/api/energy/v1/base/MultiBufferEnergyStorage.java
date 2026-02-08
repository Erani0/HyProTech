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

package com.doctorreborn.hytale.api.energy.v1.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.doctorreborn.hytale.api.energy.v1.EnergyStorageView;
import com.doctorreborn.hytale.api.energy.v1.SlottedEnergyStorage;
import com.google.common.collect.Iterators;
import com.shailist.hytale.api.transfer.v1.storage.StoragePreconditions;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import com.shailist.hytale.api.transfer.v1.transaction.base.SnapshotParticipant;

/**
 * An implementation of {@code EnergyStorage} for multiple energy buffers.
 */
public class MultiBufferEnergyStorage extends SnapshotParticipant<List<SingleBufferEnergyStorage>>
        implements SlottedEnergyStorage {
    protected List<SingleBufferEnergyStorage> slots;

    public MultiBufferEnergyStorage(List<SingleBufferEnergyStorage> slots) {
        this.slots = slots;
    }

    @Override
    public int getSlotCount() {
        return slots.size();
    }

    @Override
    public SingleSlotEnergyStorage getSlot(int slot) {
        return slots.get(slot);
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long amount = 0;

        for (SingleBufferEnergyStorage slot : slots) {
            amount += slot.insert(maxAmount - amount, transaction);
            if (amount == maxAmount)
                return amount;
        }

        return amount;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long amount = 0;

        for (SingleBufferEnergyStorage slot : slots) {
            amount += slot.extract(maxAmount - amount, transaction);
            if (amount == maxAmount)
                return amount;
        }

        return amount;
    }

    @Override
    public Iterator<EnergyStorageView> iterator() {
        return Iterators.transform(slots.iterator(), slot -> (EnergyStorageView) slot);
    }

    @Override
    protected List<SingleBufferEnergyStorage> createSnapshot() {
        return new ArrayList<>(slots);
    }

    @Override
    protected void readSnapshot(List<SingleBufferEnergyStorage> snapshot) {
        slots = new ArrayList<>(snapshot);
    }

    @Override
    public String toString() {
        return "MultiBufferEnergyStorage[" + slots + "]";
    }
}
