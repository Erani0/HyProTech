package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.energy.EnergyNodeComponent;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorage;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.PhysicsDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.SoftBlockDropType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.shailist.hytale.api.transfer.v1.transaction.Transaction;
import com.shailist.hytale.api.transfer.v1.transaction.TransactionContext;
import java.util.ArrayList;
import java.util.List;

public final class QuarryMachine extends MasterMachine {
    public static final String ID = "machinarium:quarry";
    private static final int BASIC_AREA = 5;
    private static final int MINING_OFFSET_Z = -2;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean matchesBlockId(String blockId) {
        return TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_QUARRY);
    }

    @Override
    public void configureDefaults(MachineComponent machine, EnergyNodeComponent energy) {
        super.configureDefaults(machine, energy);
        if (machine != null && (machine.getMachineId() == null || machine.getMachineId().isEmpty())) {
            machine.setMachineId(ID);
        }
        if (machine != null) {
            if (machine.getAreaWidth() <= 0) {
                machine.setAreaWidth(BASIC_AREA);
            }
            if (machine.getAreaDepth() <= 0) {
                machine.setAreaDepth(BASIC_AREA);
            }
        }
    }

    @Override
    protected int defaultCapacity() {
        return QuarryConfig.getCapacityForTier(QuarryConfig.MIN_TIER);
    }

    @Override
    protected int defaultMaxTransfer() {
        return QuarryConfig.getMaxTransferForTier(QuarryConfig.MIN_TIER);
    }

    @Override
    public boolean tick(MachineContext context, float deltaSeconds) {
        if (context == null) {
            return false;
        }
        MachineComponent machine = context.getMachine();
        EnergyNodeComponent energy = context.getEnergyNode();
        if (machine == null || energy == null) {
            return false;
        }
        if (!machine.isEnabled()) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
            }
            return false;
        }
        World world = context.getWorld();
        if (world == null) {
            return false;
        }

        boolean changed = applyTierSettings(machine, energy);
        if (changed) {
            context.markDirty();
        }

        int width = Math.max(QuarryConfig.MIN_AREA, machine.getAreaWidth());
        int depth = Math.max(QuarryConfig.MIN_AREA, machine.getAreaDepth());
        int originX = context.getX();
        int originY = context.getY();
        int originZ = context.getZ();

        EnergyStorage storage = context.getEnergyStorage();
        ItemContainer container = context.getItemContainer();
        Rotation yaw = getBlockYaw(world, originX, originY, originZ);
        Bounds bounds = computeMiningBounds(width, depth);

        BlockTarget target = findNextTarget(world, originX, originY, originZ, bounds, yaw);
        if (target == null) {
            if (machine.getProgress() != 0) {
                machine.setProgress(0);
                context.markDirty();
                return true;
            }
            return changed;
        }

        int progressMax = Math.max(1, machine.getProgressMax());
        int currentProgress = machine.getProgress();
        if (container != null && isContainerFull(container)) {
            return changed;
        }

        int consumptionPerSecond = Math.max(0, energy.getConsumption());
        int cost = 0;
        if (consumptionPerSecond > 0 && deltaSeconds > 0f) {
            cost = (int) Math.round(consumptionPerSecond * deltaSeconds);
            if (cost < 1) {
                cost = 1;
            }
        }
        if (cost > 0 && !consumeEnergy(storage, cost)) {
            return changed;
        }

        int progress = currentProgress + 1;
        if (progress < progressMax) {
            machine.setProgress(progress);
            context.markDirty();
            return true;
        }
        machine.setProgress(0);

        List<ItemStack> drops = resolveDrops(target.blockType);
        if (drops != null && !drops.isEmpty()) {
            if (container != null) {
                addDropsToContainerOrWorld(container, world, drops, target.pos);
            } else {
                spawnDrops(world, drops, target.pos);
            }
        }

        scheduleBlockBreak(world, target.pos);

        context.markDirty();
        return true;
    }

    private boolean applyTierSettings(MachineComponent machine, EnergyNodeComponent energy) {
        if (machine == null || energy == null) {
            return false;
        }
        boolean changed = false;
        int tier = QuarryConfig.clampTier(machine.getTier());
        if (machine.getTier() != tier) {
            machine.setTier(tier);
            changed = true;
        }

        int width = QuarryConfig.clampAreaForTier(tier, machine.getAreaWidth());
        int depth = QuarryConfig.clampAreaForTier(tier, machine.getAreaDepth());
        if (width != machine.getAreaWidth()) {
            machine.setAreaWidth(width);
            changed = true;
        }
        if (depth != machine.getAreaDepth()) {
            machine.setAreaDepth(depth);
            changed = true;
        }

        int capacity = QuarryConfig.getCapacityForTier(tier);
        if (energy.getCapacity() != capacity) {
            energy.setCapacity(capacity);
            if (energy.getEnergy() > capacity) {
                energy.setEnergy(capacity);
            }
            changed = true;
        }

        int consumption = QuarryConfig.computeConsumptionForArea(tier, width, depth);
        if (energy.getConsumption() != consumption) {
            energy.setConsumption(consumption);
            changed = true;
        }

        int maxTransfer = QuarryConfig.getMaxTransferForTier(tier);
        if (energy.getMaxTransfer() != maxTransfer) {
            energy.setMaxTransfer(maxTransfer);
            changed = true;
        }

        int progressMax = QuarryConfig.getMiningDelayTicks(tier);
        if (machine.getProgressMax() != progressMax) {
            machine.setProgressMax(progressMax);
            if (machine.getProgress() > progressMax) {
                machine.setProgress(progressMax);
            }
            changed = true;
        }

        return changed;
    }

    private BlockTarget findNextTarget(
            World world,
            int originX,
            int originY,
            int originZ,
            Bounds bounds,
            Rotation yaw) {
        int startY = originY - 1;
        int minY = ChunkUtil.MIN_Y;
        for (int y = startY; y >= minY; y--) {
            for (int localZ = bounds.maxZ; localZ >= bounds.minZ; localZ--) {
                for (int localX = bounds.minX; localX <= bounds.maxX; localX++) {
                    Vector3i offset = rotateLocalOffset(localX, localZ, yaw);
                    int worldX = originX + offset.getX();
                    int worldZ = originZ + offset.getZ();
                    long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
                    BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
                    if (accessor == null) {
                        continue;
                    }
                    BlockType blockType = accessor.getBlockType(worldX, y, worldZ);
                    if (!isMineable(blockType)) {
                        continue;
                    }
                    return new BlockTarget(new Vector3i(worldX, y, worldZ), blockType);
                }
            }
        }
        return null;
    }

    private boolean consumeEnergy(EnergyStorage storage, int amount) {
        if (storage == null || amount <= 0) {
            return false;
        }
        TransactionContext context = TransactionContext.current();
        try (Transaction transaction = Transaction.openNested(context)) {
            long extracted = storage.extract(amount, transaction);
            if (extracted >= amount) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }

    private boolean isContainerFull(ItemContainer container) {
        if (container == null) {
            return false;
        }
        short capacity = container.getCapacity();
        if (capacity <= 0) {
            return false;
        }
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> resolveDrops(BlockType blockType) {
        if (blockType == null) {
            return List.of();
        }
        BlockGathering gathering = blockType.getGathering();
        if (gathering != null) {
            BlockBreakingDropType breaking = gathering.getBreaking();
            if (breaking != null) {
                return resolveDropType(
                        blockType,
                        Math.max(1, breaking.getQuantity()),
                        breaking.getItemId(),
                        breaking.getDropListId());
            }
            HarvestingDropType harvest = gathering.getHarvest();
            if (harvest != null) {
                return resolveDropType(blockType, 1, harvest.getItemId(), harvest.getDropListId());
            }
            SoftBlockDropType soft = gathering.getSoft();
            if (soft != null) {
                return resolveDropType(blockType, 1, soft.getItemId(), soft.getDropListId());
            }
            PhysicsDropType physics = gathering.getPhysics();
            if (physics != null) {
                return resolveDropType(blockType, 1, physics.getItemId(), physics.getDropListId());
            }
        }
        return BlockHarvestUtils.getDrops(blockType, 1, null, null);
    }

    private List<ItemStack> resolveDropType(
            BlockType blockType,
            int quantity,
            String itemId,
            String dropListId) {
        int safeQuantity = Math.max(1, quantity);
        String resolvedItemId = emptyToNull(itemId);
        String resolvedDropListId = emptyToNull(dropListId);
        return BlockHarvestUtils.getDrops(blockType, safeQuantity, resolvedItemId, resolvedDropListId);
    }

    private String emptyToNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private void addDropsToContainerOrWorld(
            ItemContainer container,
            World world,
            List<ItemStack> drops,
            Vector3i pos) {
        if (drops == null || drops.isEmpty()) {
            return;
        }
        for (ItemStack drop : drops) {
            if (drop == null || ItemStack.isEmpty(drop)) {
                continue;
            }
            if (container.canAddItemStack(drop)) {
                container.addItemStack(drop);
            } else {
                spawnDrops(world, List.of(drop), pos);
            }
        }
    }

    private void spawnDrops(World world, List<ItemStack> stacks, Vector3i pos) {
        if (world == null || stacks == null || stacks.isEmpty() || pos == null) {
            return;
        }
        List<ItemStack> filtered = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            filtered.add(stack);
        }
        if (filtered.isEmpty()) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        Vector3d position = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vector3f velocity = new Vector3f(0f, 0.1f, 0f);
        Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(
                store,
                filtered,
                position,
                velocity);
        store.addEntities(holders, AddReason.SPAWN);
    }

    private boolean isMineable(BlockType blockType) {
        if (blockType == null || blockType == BlockType.EMPTY) {
            return false;
        }
        String id = blockType.getId();
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (containsIgnoreCase(id, "bedrock")) {
            return false;
        }
        if (isBorderId(id) || TieredIdUtil.isTieredId(id, MachinariumIds.BLOCK_QUARRY)) {
            return false;
        }
        return true;
    }

    private boolean isBorderId(String id) {
        if (id == null) {
            return false;
        }
        String baseId = MachinariumIds.BLOCK_QUARRY_BORDER;
        return id.equalsIgnoreCase(baseId)
                || id.regionMatches(true, 0, baseId, 0, baseId.length())
                || containsIgnoreCase(id, baseId);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        if (value == null || needle == null || needle.isEmpty()) {
            return false;
        }
        int limit = value.length() - needle.length();
        for (int i = 0; i <= limit; i++) {
            if (value.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
    }

    private Rotation getBlockYaw(World world, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        if (world.getChunkIfLoaded(chunkIndex) == null) {
            return Rotation.None;
        }
        RotationTuple rotation = RotationTuple.get(world.getBlockRotationIndex(x, y, z));
        if (rotation == null || rotation.yaw() == null) {
            return Rotation.None;
        }
        return rotation.yaw();
    }

    private Vector3i rotateLocalOffset(int localX, int localZ, Rotation yaw) {
        if (yaw == Rotation.Ninety) {
            return new Vector3i(localZ, 0, -localX);
        }
        if (yaw == Rotation.OneEighty) {
            return new Vector3i(-localX, 0, -localZ);
        }
        if (yaw == Rotation.TwoSeventy) {
            return new Vector3i(-localZ, 0, localX);
        }
        return new Vector3i(localX, 0, localZ);
    }

    private Bounds computeBounds(int width, int depth) {
        int halfWidth = width / 2;
        int halfDepth = depth / 2;
        int minX = -halfWidth;
        int maxX = width - 1 - halfWidth;
        int minZ = -halfDepth;
        int maxZ = depth - 1 - halfDepth;
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private Bounds computeMiningBounds(int width, int depth) {
        int halfWidth = width / 2;
        int minX = -halfWidth;
        int maxX = width - 1 - halfWidth;
        int maxZ = MINING_OFFSET_Z;
        int minZ = maxZ - (Math.max(1, depth) - 1);
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private void scheduleBlockBreak(World world, Vector3i pos) {
        if (world == null || pos == null) {
            return;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        world.execute(() -> {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
            if (accessor != null) {
                accessor.setBlock(x, y, z, BlockType.EMPTY);
            }
        });
    }

    private static final class BlockTarget {
        private final Vector3i pos;
        private final BlockType blockType;

        private BlockTarget(Vector3i pos, BlockType blockType) {
            this.pos = pos;
            this.blockType = blockType;
        }
    }

    private static final class Bounds {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private Bounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }
}
