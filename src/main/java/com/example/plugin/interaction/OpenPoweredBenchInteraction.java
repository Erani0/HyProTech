package com.example.plugin.interaction;

import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.builtin.crafting.window.ProcessingBenchWindow;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import java.util.Map;
import java.util.UUID;

public class OpenPoweredBenchInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<OpenPoweredBenchInteraction> CODEC =
            BuilderCodec.builder(
                            OpenPoweredBenchInteraction.class,
                            OpenPoweredBenchInteraction::new,
                            SimpleInstantInteraction.CODEC)
                    .documentation("Opens the processing bench.")
                    .build();

    public OpenPoweredBenchInteraction() {
        super();
    }

    @Override
    protected void firstRun(
            InteractionType interactionType,
            InteractionContext context,
            CooldownHandler cooldownHandler) {
        if (context == null || context.getCommandBuffer() == null) {
            return;
        }

        Ref<EntityStore> playerEntityRef = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = commandBuffer.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        PageManager pageManager = player.getPageManager();
        if (pageManager.getCustomPage() != null) {
            return;
        }

        PlayerRef playerRef = commandBuffer.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();
        EntityStore entityStore = store.getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null) {
            return;
        }

        Vector3i targetPos = resolveTargetBlockPosition(context, world);
        if (targetPos == null) {
            return;
        }

        Window[] windows = createBenchWindows(world, targetPos, commandBuffer, playerEntityRef);
        if (windows == null || windows.length == 0) {
            return;
        }

        Page basePage = resolveBasePage(windows);
        pageManager.setPageWithWindows(
                playerEntityRef,
                store,
                basePage,
                true,
                windows);
    }

    @SuppressWarnings("removal")
    private Vector3i resolveTargetBlockPosition(InteractionContext context, World world) {
        BlockPosition target = context.getTargetBlock();
        if (target == null || world == null) {
            return null;
        }

        BlockPosition baseBlock = world.getBaseBlock(target);
        if (baseBlock == null) {
            return null;
        }

        return new Vector3i(baseBlock.x, baseBlock.y, baseBlock.z);
    }

    @SuppressWarnings("removal")
    private Window[] createBenchWindows(
            World world,
            Vector3i pos,
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> playerEntityRef) {
        if (world == null || pos == null || commandBuffer == null || playerEntityRef == null) {
            return null;
        }

        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return null;
        }

        BlockType blockType = world.getBlockType(pos.getX(), pos.getY(), pos.getZ());
        if (blockType == null || blockType.getBench() == null) {
            return null;
        }

        ProcessingBenchState benchState = null;
        try {
            benchState = BlockModule.get().getComponent(
                    BlockStateModule.get().getComponentType(ProcessingBenchState.class),
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        } catch (RuntimeException ignored) {
            // Missing or invalid blockstate ref; we'll create one below.
        }
        if (benchState == null) {
            int localX = ChunkUtil.localCoordinate((long) pos.getX());
            int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
            BlockState.ensureState(chunk, localX, pos.getY(), localZ);
            benchState = BlockModule.get().getComponent(
                    BlockStateModule.get().getComponentType(ProcessingBenchState.class),
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        }

        if (benchState == null) {
            return null;
        }

        if (!ensureBenchInitialized(benchState, blockType)) {
            return null;
        }

        UUIDComponent uuidComponent =
                commandBuffer.getComponent(playerEntityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return null;
        }

        UUID playerId = uuidComponent.getUuid();
        Map<UUID, ProcessingBenchWindow> windows = benchState.getWindows();
        ProcessingBenchWindow existing = windows.get(playerId);
        if (existing != null) {
            return new Window[] { existing };
        }

        ProcessingBenchWindow window = new ProcessingBenchWindow(benchState);
        ProcessingBenchWindow prior = windows.putIfAbsent(playerId, window);
        if (prior != null) {
            return new Window[] { prior };
        }

        benchState.updateFuelValues();
        window.registerCloseEvent(event -> windows.remove(playerId, window));
        return new Window[] { window };
    }

    private boolean ensureBenchInitialized(ProcessingBenchState benchState, BlockType blockType) {
        if (benchState == null || blockType == null || blockType.getBench() == null) {
            return false;
        }

        boolean needsInit = benchState.getBench() == null
                || !blockType.getBench().equals(benchState.getBench());
        if (!needsInit && benchState.getItemContainer() == null) {
            needsInit = true;
        }
        if (needsInit && !benchState.initialize(blockType)) {
            return false;
        }
        return benchState.getItemContainer() != null;
    }

    private Page resolveBasePage(Window[] windows) {
        if (windows == null) {
            return Page.Inventory;
        }
        for (Window window : windows) {
            if (window instanceof ProcessingBenchWindow) {
                return Page.Bench;
            }
        }
        return Page.Inventory;
    }
}
