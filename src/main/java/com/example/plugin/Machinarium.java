package com.example.plugin;

import com.example.plugin.energy.EnergyNetworkSystem;
import com.example.plugin.energy.EnergyNodeComponent;
import com.doctorreborn.hytale.api.energy.v1.EnergyStorageLookup;
import com.example.plugin.item.ItemNetworkSystem;
import com.example.plugin.item.ItemNodeComponent;
import com.example.plugin.item.ItemStorageConfigChunk;
import com.example.plugin.item.ItemStorageConfigComponent;
import com.example.plugin.machine.MachineComponent;
import com.example.plugin.machine.MachineRegistry;
import com.example.plugin.machine.MachineSystem;
import com.example.plugin.machine.QuarryAreaManager;
import com.example.plugin.machine.OreCrusherMachine;
import com.example.plugin.machine.QuarryMachine;
import com.example.plugin.sound.MachinariumSounds;
import com.example.plugin.interaction.CableSideToolInteraction;
import com.example.plugin.interaction.CableNetworkUpgradeInteraction;
import com.example.plugin.ui.BatteryPage;
import com.example.plugin.ui.CablePage;
import com.example.plugin.ui.ItemCablePage;
import com.example.plugin.ui.OreCrusherPage;
import com.example.plugin.ui.QuarryPage;
import com.example.plugin.ui.OpenCustomUIWithWindowsInteraction;
import com.example.plugin.ui.PlayerUiSystem;
import com.example.plugin.ui.SolarPage;
import com.example.plugin.ui.WindPage;
import com.example.plugin.interaction.OpenPoweredBenchInteraction;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class Machinarium extends JavaPlugin {
    private static final String TUTBOOKS_DOWNLOAD_URL =
            "https://github.com/YoofeCZ/HyProTechBook/releases/latest/download/HyProTechBook.zip";
    private static final String TUTBOOKS_MOD_ID = "HyProTech";
    private static final int WIND_TURBINE_BLOCK_HEIGHT = 5;
    private final AtomicBoolean tutbooksDownloadQueued = new AtomicBoolean(false);

    public Machinarium(@NonNullDecl JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        MachinariumSounds.registerDefaultsIfMissing();

        registerTutbooksDownloadOnPlayerJoin();

        ComponentType<ChunkStore, EnergyNodeComponent> energyType =
                getChunkStoreRegistry().registerComponent(
                        EnergyNodeComponent.class,
                        MachinariumIds.ENERGY_COMPONENT_ID,
                        EnergyNodeComponent.CODEC);
        ComponentType<ChunkStore, ItemNodeComponent> itemType =
                getChunkStoreRegistry().registerComponent(
                        ItemNodeComponent.class,
                        MachinariumIds.ITEM_COMPONENT_ID,
                        ItemNodeComponent.CODEC);
        ComponentType<ChunkStore, ItemStorageConfigComponent> storageType =
                getChunkStoreRegistry().registerComponent(
                        ItemStorageConfigComponent.class,
                        MachinariumIds.ITEM_STORAGE_COMPONENT_ID,
                        ItemStorageConfigComponent.CODEC);
        ComponentType<ChunkStore, ItemStorageConfigChunk> storageChunkType =
                getChunkStoreRegistry().registerComponent(
                        ItemStorageConfigChunk.class,
                        MachinariumIds.ITEM_STORAGE_CHUNK_COMPONENT_ID,
                        ItemStorageConfigChunk.CODEC);
        ComponentType<ChunkStore, MachineComponent> machineType =
                getChunkStoreRegistry().registerComponent(
                        MachineComponent.class,
                        MachinariumIds.MACHINE_COMPONENT_ID,
                        MachineComponent.CODEC);
        MachinariumComponents.init(
                energyType,
                itemType,
                storageType,
                storageChunkType,
                machineType);

        MachineRegistry.register(new QuarryMachine());
        MachineRegistry.register(new OreCrusherMachine());

        EnergyStorageLookup.register((world, x, y, z) -> {
            if (world == null || y < ChunkUtil.MIN_Y || y >= ChunkUtil.HEIGHT) {
                return null;
            }
            ChunkStore chunkStore = world.getChunkStore();
            if (chunkStore == null) {
                return null;
            }
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            BlockComponentChunk components =
                    chunkStore.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (components == null) {
                return null;
            }
            int localX = ChunkUtil.localCoordinate((long) x);
            int localZ = ChunkUtil.localCoordinate((long) z);
            int blockIndex = ChunkUtil.indexBlockInColumn(localX, y, localZ);
            EnergyNodeComponent node = components.getComponent(blockIndex, energyType);
            if (node == null) {
                return null;
            }
            return node.getStorage(components::markNeedsSaving);
        });

        getChunkStoreRegistry().registerSystem(new EnergyNetworkSystem(energyType));
        getChunkStoreRegistry().registerSystem(new ItemNetworkSystem(itemType));
        getChunkStoreRegistry().registerSystem(new MachineSystem(machineType, energyType));
        getEntityStoreRegistry().registerSystem(new PlayerUiSystem(energyType, itemType, machineType));

        getCodecRegistry(Interaction.CODEC).register(
                MachinariumIds.OPEN_UI_INTERACTION_ID,
                OpenCustomUIInteraction.class,
                OpenCustomUIInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(
                MachinariumIds.OPEN_UI_WITH_WINDOWS_INTERACTION_ID,
                OpenCustomUIWithWindowsInteraction.class,
                OpenCustomUIWithWindowsInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(
                MachinariumIds.OPEN_POWERED_BENCH_INTERACTION_ID,
                OpenPoweredBenchInteraction.class,
                OpenPoweredBenchInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(
                MachinariumIds.TOGGLE_CABLE_SIDE_INTERACTION_ID,
                CableSideToolInteraction.class,
                CableSideToolInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(
                MachinariumIds.UPGRADE_CABLE_NETWORK_INTERACTION_ID,
                CableNetworkUpgradeInteraction.class,
                CableNetworkUpgradeInteraction.CODEC);

        getCommandRegistry().registerCommand(new MachinariumCommand());

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.BATTERY_PAGE_ID,
                (playerRef, blockRef) -> new BatteryPage(playerRef, blockRef, energyType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.SOLAR_PAGE_ID,
                (playerRef, blockRef) -> new SolarPage(playerRef, blockRef, energyType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.WIND_PAGE_ID,
                (playerRef, blockRef) -> new WindPage(playerRef, blockRef, energyType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.CABLE_PAGE_ID,
                (playerRef, blockRef) -> new CablePage(playerRef, blockRef, energyType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.ITEM_CABLE_PAGE_ID,
                (playerRef, blockRef) -> new ItemCablePage(playerRef, blockRef, itemType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.ORE_CRUSHER_PAGE_ID,
                (playerRef, blockRef) -> new OreCrusherPage(playerRef, blockRef, energyType, machineType));

        OpenCustomUIInteraction.registerBlockEntityCustomPage(
                this,
                OpenCustomUIInteraction.CustomPageSupplier.class,
                MachinariumIds.QUARRY_PAGE_ID,
                (playerRef, blockRef) -> new QuarryPage(playerRef, blockRef, energyType, machineType));

        getEventRegistry().registerGlobal(
                DamageBlockEvent.class,
                event -> {
                    if (event.isCancelled()) {
                        return;
                    }
                    BlockType blockType = event.getBlockType();
                    String blockId = blockType == null ? null : blockType.getId();
                    if (!UpgradePersistence.isUpgradeableBlockId(blockId)) {
                        return;
                    }
                    Vector3i pos = event.getTargetBlock();
                    if (pos == null) {
                        return;
                    }
                    World world = UpgradePersistence.findWorld(pos, blockType);
                    if (world == null) {
                        return;
                    }
                    List<ItemStack> drops = UpgradePersistence.snapshotBreakDrops(world, pos);
                    UpgradePersistence.cacheBreakDrops(world, pos, drops);
                });

        getEventRegistry().registerGlobal(
                BreakBlockEvent.class,
                event -> {
                    if (event.isCancelled()) {
                        return;
                    }
                    BlockType blockType = event.getBlockType();
                    String blockId = blockType == null ? null : blockType.getId();
                    Vector3i pos = event.getTargetBlock();
                    if (pos != null && blockId != null) {
                        World world = UpgradePersistence.findWorld(pos, blockType);
                        if (world != null) {
                            stopMachineSound(world, pos, blockId);
                        }
                    }
                    if (pos != null && TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_QUARRY)) {
                        World world = UpgradePersistence.findWorld(pos, blockType);
                        if (world != null) {
                            MachineComponent machine = getMachineAt(world, pos, machineType);
                            int width = machine == null ? 5 : machine.getAreaWidth();
                            int depth = machine == null ? 5 : machine.getAreaDepth();
                            QuarryAreaManager.hideArea(world, pos, width, depth);
                        }
                    }
                    if (!UpgradePersistence.isUpgradeableBlockId(blockId)) {
                        return;
                    }
                    if (pos == null) {
                        return;
                    }
                    World world = UpgradePersistence.findWorld(pos, blockType);
                    if (world == null) {
                        world = UpgradePersistence.findWorld(pos, null);
                    }
                    if (world == null) {
                        return;
                    }
                    ItemStack drop = UpgradePersistence.buildDropStack(world, pos, blockType, blockId);
                    if (drop == null) {
                        return;
                    }
                    List<ItemStack> extraDrops = UpgradePersistence.consumeBreakDrops(world, pos);
                    if (extraDrops == null || extraDrops.isEmpty()) {
                        extraDrops = UpgradePersistence.snapshotBreakDrops(world, pos);
                    }
                    event.setCancelled(true);
                    UpgradePersistence.queueBreakAndDrop(world, pos, blockType, drop, extraDrops);
                });
        getEventRegistry().registerGlobal(
                PlaceBlockEvent.class,
                event -> {
                    ItemStack stack = event.getItemInHand();
                    if (stack == null || ItemStack.isEmpty(stack)) {
                        return;
                    }
                    Vector3i pos = event.getTargetBlock();
                    if (pos == null) {
                        return;
                    }
                    World world = UpgradePersistence.findWorld(pos, null);
                    if (world == null) {
                        return;
                    }
                    String blockId = stack.getBlockKey();
                    if (!TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_WIND_TURBINE)) {
                        if (isBlockedByWindTurbine(world, pos)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                    if (!UpgradePersistence.isUpgradeableBlockId(blockId)) {
                        return;
                    }
                    UpgradePersistence.storePending(world, pos, blockId, stack);
                });
        // Furnace custom UI removed; vanilla bench opens via interaction.
    }

    private void registerTutbooksDownloadOnPlayerJoin() {
        getEventRegistry().registerGlobal(
                PlayerReadyEvent.class,
                event -> {
                    if (!tutbooksDownloadQueued.compareAndSet(false, true)) {
                        return;
                    }
                    CommandManager commandManager = CommandManager.get();
                    if (commandManager == null) {
                        tutbooksDownloadQueued.set(false);
                        getLogger().atWarning().log("[HyProTech] CommandManager not available for TutBooks download.");
                        return;
                    }
                    String downloadCommand = String.format(
                            "tutbooks download %s %s",
                            TUTBOOKS_MOD_ID,
                            TUTBOOKS_DOWNLOAD_URL);
                    getLogger().atInfo().log("[HyProTech] Running: %s", downloadCommand);
                    commandManager
                            .handleCommand(ConsoleSender.INSTANCE, downloadCommand)
                            .thenCompose(ignored -> commandManager.handleCommand(ConsoleSender.INSTANCE, "tutbooks reload"))
                            .thenAccept(ignored -> getLogger().atInfo().log("[HyProTech] TutBooks download/reload complete."))
                            .exceptionally(ex -> {
                                tutbooksDownloadQueued.set(false);
                                getLogger().atWarning().log(
                                        "[HyProTech] TutBooks download/reload failed: %s",
                                        ex.getMessage());
                                return null;
                            });
                });
    }

    private static MachineComponent getMachineAt(
            World world,
            Vector3i pos,
            ComponentType<ChunkStore, MachineComponent> machineType) {
        if (world == null || pos == null) {
            return null;
        }
        if (pos.getY() < ChunkUtil.MIN_Y || pos.getY() >= ChunkUtil.HEIGHT) {
            return null;
        }
        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore == null) {
            return null;
        }
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
        BlockComponentChunk blockComponents =
                chunkStore.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
        if (blockComponents == null) {
            return null;
        }
        int localX = ChunkUtil.localCoordinate((long) pos.getX());
        int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
        int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
        return blockComponents.getComponent(blockIndex, machineType);
    }

    private static boolean isBlockedByWindTurbine(World world, Vector3i pos) {
        if (world == null || pos == null) {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int minY = Math.max(ChunkUtil.MIN_Y, y - (WIND_TURBINE_BLOCK_HEIGHT - 1));
        for (int checkY = y; checkY >= minY; checkY--) {
            BlockType blockType = world.getBlockType(x, checkY, z);
            if (blockType == null) {
                continue;
            }
            String blockId = blockType.getId();
            if (TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_WIND_TURBINE)) {
                int baseY = checkY;
                if (y < baseY + WIND_TURBINE_BLOCK_HEIGHT) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void stopMachineSound(World world, Vector3i pos, String blockId) {
        if (world == null || pos == null || blockId == null) {
            return;
        }
        if (TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_WIND_TURBINE)) {
            MachinariumSounds.stopSound(
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    MachinariumSounds.EVENT_WIND_TURBINE,
                    MachinariumSounds.FILE_WIND_TURBINE);
            return;
        }
        if (TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_SOLAR_PANEL)) {
            MachinariumSounds.stopSound(
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    MachinariumSounds.EVENT_SOLAR_PANEL,
                    MachinariumSounds.FILE_SOLAR_PANEL);
            return;
        }
        if (TieredIdUtil.isTieredId(blockId, MachinariumIds.BLOCK_ELECTRIC_FURNACE)) {
            MachinariumSounds.stopSound(
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    MachinariumSounds.EVENT_ELECTRIC_FURNACE,
                    MachinariumSounds.FILE_ELECTRIC_FURNACE);
            return;
        }
    }

}
