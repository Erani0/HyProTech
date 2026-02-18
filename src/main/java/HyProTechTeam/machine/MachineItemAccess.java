package HyProTechTeam.machine;

import HyProTechTeam.BlockIdUtil;
import HyProTechTeam.HyProTechIds;
import HyProTechTeam.TieredIdUtil;
import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class MachineItemAccess {
    private static final short QUARRY_STORAGE_CAPACITY = 10;
    private static final short ORE_CRUSHER_STORAGE_CAPACITY = 7;
    private static final short ALLOY_SMELTER_STORAGE_CAPACITY =
            (short) (AlloySmelterConfig.INPUT_SLOT_COUNT + AlloySmelterConfig.OUTPUT_SLOT_COUNT);
    private static final String HyProTech_PREFIX = "HyProTech_";

    private MachineItemAccess() {
    }

    @SuppressWarnings("removal")
    public static ItemContainerBlockState getContainerState(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }

        BlockType blockType = world.getBlockType(x, y, z);
        if (blockType != null && blockType != BlockType.EMPTY) {
            String blockId = blockType.getId();
            if (blockId != null && isIdOrState(blockId, HyProTechIds.BLOCK_ORE_CRUSHER)) {
                ItemContainerState containerState = BlockModule.get().getComponent(
                        BlockStateModule.get().getComponentType(ItemContainerState.class),
                        world,
                        x,
                        y,
                        z);
                if (containerState == null) {
                    scheduleContainerState(world, x, y, z, blockType);
                    return null;
                }
                ensureMachineContainers(world, x, y, z, containerState);
                return containerState;
            }
            if (blockId != null && isIdOrState(blockId, HyProTechIds.BLOCK_ALLOY_SMELTER)) {
                ItemContainerState containerState = BlockModule.get().getComponent(
                        BlockStateModule.get().getComponentType(ItemContainerState.class),
                        world,
                        x,
                        y,
                        z);
                if (containerState == null) {
                    scheduleContainerState(world, x, y, z, blockType);
                    return null;
                }
                ensureMachineContainers(world, x, y, z, containerState);
                return containerState;
            }
        }

        ProcessingBenchState benchState = BlockModule.get().getComponent(
                BlockStateModule.get().getComponentType(ProcessingBenchState.class),
                world,
                x,
                y,
                z);
        if (benchState != null) {
            return benchState;
        }

        ItemContainerState containerState = BlockModule.get().getComponent(
                BlockStateModule.get().getComponentType(ItemContainerState.class),
                world,
                x,
                y,
                z);
        if (containerState != null) {
            ensureMachineContainers(world, x, y, z, containerState);
            return containerState;
        }
        if (blockType == null || blockType == BlockType.EMPTY) {
            return null;
        }
        String blockId = blockType.getId();
        if (blockId == null || MachineRegistry.findByBlockId(blockId) == null) {
            return null;
        }

        scheduleContainerState(world, x, y, z, blockType);
        return null;
    }

    public static ItemContainer getContainer(World world, int x, int y, int z) {
        ItemContainerBlockState state = getContainerState(world, x, y, z);
        return state == null ? null : state.getItemContainer();
    }

    public static void markContainerDirty(World world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        ItemContainerBlockState state = getContainerState(world, x, y, z);
        if (state instanceof ItemContainerState) {
            ((ItemContainerState) state).markNeedsSave();
            return;
        }
        world.execute(() -> {
            ItemContainerBlockState ensured = ensureContainerState(world, x, y, z);
            if (ensured instanceof ItemContainerState) {
                ((ItemContainerState) ensured).markNeedsSave();
            }
        });
    }

    @SuppressWarnings("removal")
    private static void scheduleContainerState(
            World world,
            int x,
            int y,
            int z,
            BlockType blockType) {
        if (world == null) {
            return;
        }

        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return;
        }

        int localX = ChunkUtil.localCoordinate((long) x);
        int localZ = ChunkUtil.localCoordinate((long) z);
        world.execute(() -> {
            BlockState.ensureState(chunk, localX, y, localZ);
            ItemContainerState state = BlockModule.get().getComponent(
                    BlockStateModule.get().getComponentType(ItemContainerState.class),
                    world,
                    x,
                    y,
                    z);
            if (state != null && state.getItemContainer() == null && blockType != null) {
                state.initialize(blockType);
            }
            if (state != null) {
                ensureMachineContainers(world, x, y, z, state);
            }
        });
    }

    @SuppressWarnings("removal")
    public static ItemContainerBlockState ensureContainerState(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return null;
        }

        int localX = ChunkUtil.localCoordinate((long) x);
        int localZ = ChunkUtil.localCoordinate((long) z);
        BlockState.ensureState(chunk, localX, y, localZ);

        ItemContainerState state = BlockModule.get().getComponent(
                BlockStateModule.get().getComponentType(ItemContainerState.class),
                world,
                x,
                y,
                z);
        if (state == null) {
            return null;
        }
        if (state.getItemContainer() == null) {
            BlockType blockType = world.getBlockType(x, y, z);
            if (blockType != null) {
                state.initialize(blockType);
            }
        }
        ensureMachineContainers(world, x, y, z, state);
        return state;
    }

    private static void ensureMachineContainers(
            World world,
            int x,
            int y,
            int z,
            ItemContainerState state) {
        ensureQuarryContainer(world, x, y, z, state);
        ensureOreCrusherContainer(world, x, y, z, state);
        ensureAlloySmelterContainer(world, x, y, z, state);
    }

    private static void ensureQuarryContainer(
            World world,
            int x,
            int y,
            int z,
            ItemContainerState state) {
        if (world == null || state == null) {
            return;
        }
        BlockType blockType = world.getBlockType(x, y, z);
        if (blockType == null || blockType == BlockType.EMPTY) {
            return;
        }
        String blockId = blockType.getId();
        if (blockId == null || isQuarryBorder(blockId) || !isIdOrState(blockId, HyProTechIds.BLOCK_QUARRY)) {
            return;
        }
        ItemContainer container = state.getItemContainer();
        if (container != null && container.getCapacity() == QUARRY_STORAGE_CAPACITY) {
            return;
        }

        SimpleItemContainer replacement = new SimpleItemContainer(QUARRY_STORAGE_CAPACITY);
        if (container != null) {
            copyContainerItems(world, x, y, z, container, replacement, QUARRY_STORAGE_CAPACITY);
        }
        state.setItemContainer(replacement);
    }

    private static void ensureOreCrusherContainer(
            World world,
            int x,
            int y,
            int z,
            ItemContainerState state) {
        if (world == null || state == null) {
            return;
        }
        BlockType blockType = world.getBlockType(x, y, z);
        if (blockType == null || blockType == BlockType.EMPTY) {
            return;
        }
        String blockId = blockType.getId();
        if (blockId == null || !isIdOrState(blockId, HyProTechIds.BLOCK_ORE_CRUSHER)) {
            return;
        }
        ItemContainer container = state.getItemContainer();
        if (container != null && container.getCapacity() == ORE_CRUSHER_STORAGE_CAPACITY) {
            return;
        }

        SimpleItemContainer replacement = new SimpleItemContainer(ORE_CRUSHER_STORAGE_CAPACITY);
        if (container != null) {
            copyContainerItems(world, x, y, z, container, replacement, ORE_CRUSHER_STORAGE_CAPACITY);
        }
        state.setItemContainer(replacement);
    }

    private static void ensureAlloySmelterContainer(
            World world,
            int x,
            int y,
            int z,
            ItemContainerState state) {
        if (world == null || state == null) {
            return;
        }
        BlockType blockType = world.getBlockType(x, y, z);
        if (blockType == null || blockType == BlockType.EMPTY) {
            return;
        }
        String blockId = blockType.getId();
        if (blockId == null || !isIdOrState(blockId, HyProTechIds.BLOCK_ALLOY_SMELTER)) {
            return;
        }
        ItemContainer container = state.getItemContainer();
        if (container != null && container.getCapacity() == ALLOY_SMELTER_STORAGE_CAPACITY) {
            return;
        }

        SimpleItemContainer replacement = new SimpleItemContainer(ALLOY_SMELTER_STORAGE_CAPACITY);
        if (container != null) {
            copyContainerItems(world, x, y, z, container, replacement, ALLOY_SMELTER_STORAGE_CAPACITY);
        }
        state.setItemContainer(replacement);
    }


    private static boolean isIdOrState(String blockId, String baseId) {
        if (blockId == null) {
            return false;
        }
        return BlockIdUtil.isIdOrState(blockId, baseId)
                || blockId.regionMatches(true, 0, HyProTech_PREFIX, 0, HyProTech_PREFIX.length());
    }

    private static boolean isQuarryBorder(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return false;
        }
        String baseId = HyProTechIds.BLOCK_QUARRY_BORDER;
        return blockId.equalsIgnoreCase(baseId)
                || blockId.regionMatches(true, 0, baseId, 0, baseId.length())
                || containsIgnoreCase(blockId, baseId);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
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

    private static void copyContainerItems(
            World world,
            int x,
            int y,
            int z,
            ItemContainer source,
            SimpleItemContainer target,
            short maxCapacity) {
        if (source == null || target == null) {
            return;
        }
        short capacity = source.getCapacity();
        short limit = (short) Math.min(capacity, maxCapacity);
        for (short slot = 0; slot < limit; slot++) {
            ItemStack stack = source.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            ItemStack copy = new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getMetadata());
            target.addItemStackToSlot(slot, copy);
        }
    }

}
