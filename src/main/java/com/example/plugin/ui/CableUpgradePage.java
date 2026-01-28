package com.example.plugin.ui;

import com.example.plugin.MachinariumComponents;
import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.UpgradePersistence;
import com.example.plugin.energy.CableUpgradeConfig;
import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.energy.EnergySide;
import com.example.plugin.energy.EnergyUnits;
import com.example.plugin.item.ItemNodeComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CableUpgradePage extends InteractiveCustomUIPage<CableUpgradeEvent> {
    public enum CableType {
        ENERGY,
        ITEM
    }

    private static final String ACTION_UPGRADE = "Upgrade";
    private static final String[] UPGRADE_ROW_IDS = {
            "#UpgradeReqRow1",
            "#UpgradeReqRow2",
            "#UpgradeReqRow3",
            "#UpgradeReqRow4"
    };
    private static final String[] UPGRADE_SLOT_IDS = {
            "#UpgradeReqSlot1",
            "#UpgradeReqSlot2",
            "#UpgradeReqSlot3",
            "#UpgradeReqSlot4"
    };
    private static final String[] UPGRADE_NAME_IDS = {
            "#UpgradeReqName1",
            "#UpgradeReqName2",
            "#UpgradeReqName3",
            "#UpgradeReqName4"
    };
    private static final String[] UPGRADE_QTY_IDS = {
            "#UpgradeReqQty1",
            "#UpgradeReqQty2",
            "#UpgradeReqQty3",
            "#UpgradeReqQty4"
    };

    private final Vector3i targetPos;
    private final CableType cableType;

    public CableUpgradePage(PlayerRef playerRef, Vector3i targetPos, CableType cableType) {
        super(playerRef, CustomPageLifetime.CanDismiss, CableUpgradeEvent.CODEC);
        this.targetPos = targetPos;
        this.cableType = cableType == null ? CableType.ENERGY : cableType;
    }

    @Override
    public void build(
            Ref<EntityStore> playerRef,
            UICommandBuilder uiCommandBuilder,
            UIEventBuilder uiEventBuilder,
            Store<EntityStore> store) {
        uiCommandBuilder.append("Machinarium_CableUpgrade.ui");
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#UpgradeButton",
                EventData.of("Action", ACTION_UPGRADE));
        World world = getWorld(store);
        ItemContainer inventory = getPlayerInventory(store);
        refresh(uiCommandBuilder, world, inventory);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, CableUpgradeEvent data) {
        if (data == null || data.getAction() == null) {
            return;
        }
        if (!ACTION_UPGRADE.equalsIgnoreCase(data.getAction())) {
            return;
        }

        World world = getWorld(store);
        if (world == null) {
            return;
        }

        if (cableType == CableType.ENERGY) {
            handleEnergyUpgrade(playerRef, store, world);
        } else {
            handleItemUpgrade(playerRef, store, world);
        }
    }

    private void handleEnergyUpgrade(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            World world) {
        EnergyNetworkSnapshot snapshot = collectEnergyNetwork(world);
        if (snapshot == null) {
            sendPlayerMessage(playerRef, store, "No cable network found.");
            return;
        }

        int currentTier = snapshot.minTier;
        if (!CableUpgradeConfig.hasNextTier(currentTier)) {
            sendPlayerMessage(playerRef, store, "Cable network is already at max tier.");
            return;
        }

        ItemContainer inventory = getPlayerInventory(store);
        if (inventory == null) {
            return;
        }

        CableUpgradeConfig.Requirement[] requirements =
                CableUpgradeConfig.getUpgradeRequirements(currentTier, snapshot.cableCount);
        List<ItemStack> stacks = new ArrayList<>(requirements.length);
        for (CableUpgradeConfig.Requirement requirement : requirements) {
            stacks.add(new ItemStack(requirement.getItemId(), requirement.getQuantity()));
        }

        if (!stacks.isEmpty() && !inventory.canRemoveItemStacks(stacks)) {
            sendPlayerMessage(playerRef, store, "Missing upgrade materials.");
            return;
        }

        if (!stacks.isEmpty()) {
            ListTransaction<ItemStackTransaction> transaction = inventory.removeItemStacks(stacks);
            if (transaction == null || !transaction.succeeded()) {
                sendPlayerMessage(playerRef, store, "Upgrade failed.");
                return;
            }
        }

        int nextTier = currentTier + 1;
        String upgradedId = TieredIdUtil.buildTieredId(MachinariumIds.BLOCK_ENERGY_CABLE, nextTier);
        for (EnergyCableEntry cable : snapshot.cables) {
            if (cable.node.getCableTier() >= nextTier) {
                continue;
            }
            cable.node.setCableTier(nextTier);
            applyEnergyCableTier(cable.node, nextTier);
            cable.components.markNeedsSaving();
            if (cable.position != null) {
                queueCableUpgrade(world, cable.position, MachinariumIds.BLOCK_ENERGY_CABLE, upgradedId, cable.node, null);
            }
        }

        sendPlayerMessage(
                playerRef,
                store,
                "Upgraded cable network to " + CableUpgradeConfig.getTierName(nextTier) + ".");
        refreshAndSend(store, world);
    }

    private void handleItemUpgrade(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            World world) {
        ItemNetworkSnapshot snapshot = collectItemNetwork(world);
        if (snapshot == null) {
            sendPlayerMessage(playerRef, store, "No cable network found.");
            return;
        }

        int currentTier = snapshot.minTier;
        if (!CableUpgradeConfig.hasNextTier(currentTier)) {
            sendPlayerMessage(playerRef, store, "Cable network is already at max tier.");
            return;
        }

        ItemContainer inventory = getPlayerInventory(store);
        if (inventory == null) {
            return;
        }

        CableUpgradeConfig.Requirement[] requirements =
                CableUpgradeConfig.getUpgradeRequirements(currentTier, snapshot.cableCount);
        List<ItemStack> stacks = new ArrayList<>(requirements.length);
        for (CableUpgradeConfig.Requirement requirement : requirements) {
            stacks.add(new ItemStack(requirement.getItemId(), requirement.getQuantity()));
        }

        if (!stacks.isEmpty() && !inventory.canRemoveItemStacks(stacks)) {
            sendPlayerMessage(playerRef, store, "Missing upgrade materials.");
            return;
        }

        if (!stacks.isEmpty()) {
            ListTransaction<ItemStackTransaction> transaction = inventory.removeItemStacks(stacks);
            if (transaction == null || !transaction.succeeded()) {
                sendPlayerMessage(playerRef, store, "Upgrade failed.");
                return;
            }
        }

        int nextTier = currentTier + 1;
        String upgradedId = TieredIdUtil.buildTieredId(MachinariumIds.BLOCK_ITEM_CABLE, nextTier);
        for (ItemCableEntry cable : snapshot.cables) {
            if (cable.node.getCableTier() >= nextTier) {
                continue;
            }
            cable.node.setCableTier(nextTier);
            applyItemCableTier(cable.node, nextTier);
            cable.components.markNeedsSaving();
            if (cable.position != null) {
                queueCableUpgrade(world, cable.position, MachinariumIds.BLOCK_ITEM_CABLE, upgradedId, null, cable.node);
            }
        }

        sendPlayerMessage(
                playerRef,
                store,
                "Upgraded cable network to " + CableUpgradeConfig.getTierName(nextTier) + ".");
        refreshAndSend(store, world);
    }

    private void refreshAndSend(Store<EntityStore> store, World world) {
        UICommandBuilder update = new UICommandBuilder();
        ItemContainer inventory = getPlayerInventory(store);
        refresh(update, world, inventory);
        sendUpdate(update);
    }

    private void refresh(UICommandBuilder update, World world, ItemContainer inventory) {
        if (world == null || targetPos == null) {
            showNoNetwork(update);
            return;
        }

        if (cableType == CableType.ENERGY) {
            EnergyNetworkSnapshot snapshot = collectEnergyNetwork(world);
            if (snapshot == null) {
                showNoNetwork(update);
                return;
            }
            update.set("#CableNetworkType.Text", "Energy Cable Network");
            update.set("#CableNetworkSize.Text", "Cables: " + snapshot.cableCount);
            update.set("#CableNetworkStats.Text", buildEnergyStats(snapshot));
            updateUpgradePanel(update, snapshot.minTier, snapshot.maxTier, snapshot.cableCount, inventory, true);
        } else {
            ItemNetworkSnapshot snapshot = collectItemNetwork(world);
            if (snapshot == null) {
                showNoNetwork(update);
                return;
            }
            update.set("#CableNetworkType.Text", "Item Cable Network");
            update.set("#CableNetworkSize.Text", "Cables: " + snapshot.cableCount);
            update.set("#CableNetworkStats.Text", buildItemStats(snapshot));
            updateUpgradePanel(update, snapshot.minTier, snapshot.maxTier, snapshot.cableCount, inventory, false);
        }
    }

    private void showNoNetwork(UICommandBuilder update) {
        update.set("#CableNetworkType.Text", "Cable Network");
        update.set("#CableNetworkSize.Text", "Cables: 0");
        update.set("#CableNetworkStats.Text", "");
        update.set("#UpgradeTier.Text", "Tier: -");
        update.set("#UpgradeNextTier.Text", "Next: -");
        update.set("#UpgradeStats.Text", "");
        update.set("#UpgradeButton.Text", "No Network");
        update.set("#UpgradeReqEmpty.Text", "No cable network found.");
        update.set("#UpgradeReqEmpty.Visible", true);
        for (int i = 0; i < UPGRADE_ROW_IDS.length; i++) {
            update.set(UPGRADE_ROW_IDS[i] + ".Visible", false);
            update.set(UPGRADE_SLOT_IDS[i] + ".ItemId", "");
            update.set(UPGRADE_NAME_IDS[i] + ".Text", "");
            update.set(UPGRADE_QTY_IDS[i] + ".Text", "");
        }
    }

    private void updateUpgradePanel(
            UICommandBuilder update,
            int minTier,
            int maxTier,
            int cableCount,
            ItemContainer inventory,
            boolean energyCable) {
        String tierLabel = minTier == maxTier
                ? CableUpgradeConfig.getTierName(minTier)
                : "Mixed (" + CableUpgradeConfig.getTierName(minTier)
                        + "-" + CableUpgradeConfig.getTierName(maxTier) + ")";
        update.set("#UpgradeTier.Text", "Tier: " + tierLabel);

        boolean hasNextTier = CableUpgradeConfig.hasNextTier(minTier);
        String nextText = hasNextTier
                ? "Next: " + CableUpgradeConfig.getTierName(minTier + 1)
                : "Next: Max";
        update.set("#UpgradeNextTier.Text", nextText);

        update.set("#UpgradeStats.Text", buildUpgradeStatsText(minTier, hasNextTier, energyCable));

        CableUpgradeConfig.Requirement[] requirements =
                hasNextTier ? CableUpgradeConfig.getUpgradeRequirements(minTier, cableCount)
                        : new CableUpgradeConfig.Requirement[0];
        int[] owned = new int[requirements.length];
        boolean canUpgrade = hasNextTier && inventory != null;
        for (int i = 0; i < requirements.length; i++) {
            CableUpgradeConfig.Requirement requirement = requirements[i];
            owned[i] = inventory == null ? 0 : countItem(inventory, requirement.getItemId());
            if (owned[i] < requirement.getQuantity()) {
                canUpgrade = false;
            }
        }

        String buttonText = hasNextTier
                ? (canUpgrade ? "Upgrade" : "Missing Items")
                : "Max Tier";
        update.set("#UpgradeButton.Text", buttonText);

        update.set("#UpgradeReqEmpty.Text", "No further upgrades.");
        update.set("#UpgradeReqEmpty.Visible", !hasNextTier);
        for (int i = 0; i < UPGRADE_ROW_IDS.length; i++) {
            boolean visible = hasNextTier && i < requirements.length;
            update.set(UPGRADE_ROW_IDS[i] + ".Visible", visible);
            if (visible) {
                CableUpgradeConfig.Requirement requirement = requirements[i];
                update.set(UPGRADE_SLOT_IDS[i] + ".ItemId", UiItemIds.safeItemId(requirement.getItemId()));
                update.set(UPGRADE_NAME_IDS[i] + ".Text", formatRequirementName(requirement.getItemId()));
                update.set(UPGRADE_QTY_IDS[i] + ".Text", owned[i] + "/" + requirement.getQuantity());
            } else {
                update.set(UPGRADE_SLOT_IDS[i] + ".ItemId", "");
                update.set(UPGRADE_NAME_IDS[i] + ".Text", "");
                update.set(UPGRADE_QTY_IDS[i] + ".Text", "");
            }
        }
    }

    private String buildEnergyStats(EnergyNetworkSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        return "Stored: " + EnergyUnits.formatEnergyWithCapacity(snapshot.totalEnergy, snapshot.totalCapacity)
                + " | Max: " + snapshot.maxTransfer + " J/s";
    }

    private String buildItemStats(ItemNetworkSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        return "Max Transfer: " + snapshot.maxTransfer + " items/s";
    }

    private String buildUpgradeStatsText(int tier, boolean hasNextTier, boolean energyCable) {
        if (!hasNextTier) {
            return "Max tier reached.";
        }
        int nextTier = tier + 1;
        if (energyCable) {
            int capacity = CableUpgradeConfig.getEnergyCapacityForTier(nextTier);
            int maxTransfer = CableUpgradeConfig.getEnergyMaxTransferForTier(nextTier);
            return "Next capacity: " + capacity + " each | Max: " + maxTransfer + " J/s";
        }
        int maxTransfer = CableUpgradeConfig.getItemMaxTransferForTier(nextTier);
        return "Next max transfer: " + maxTransfer + " items/s";
    }

    private EnergyNetworkSnapshot collectEnergyNetwork(World world) {
        if (world == null || MachinariumComponents.ENERGY == null) {
            return null;
        }

        EnergyNetworkSnapshot snapshot = new EnergyNetworkSnapshot();
        Deque<Vector3i> queue = new ArrayDeque<>();
        Long2ObjectMap<IntOpenHashSet> visited = new Long2ObjectOpenHashMap<>();

        if (!markVisited(visited, targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
            return null;
        }
        queue.add(targetPos);

        while (!queue.isEmpty()) {
            Vector3i pos = queue.removeFirst();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk components =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (components == null) {
                continue;
            }

            int localX = ChunkUtil.localCoordinate((long) pos.getX());
            int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
            int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
            EnergyNodeComponent node = components.getComponent(blockIndex, MachinariumComponents.ENERGY);
            if (node == null || node.getNodeType() != EnergyNodeComponent.NodeType.CABLE) {
                continue;
            }

            if (snapshot.cableCount == 0) {
                snapshot.cableColor = node.getCableColor();
            } else if (node.getCableColor() != snapshot.cableColor) {
                continue;
            }

            snapshot.addCable(node, components, blockIndex, new Vector3i(pos.getX(), pos.getY(), pos.getZ()));

            for (EnergySide side : EnergySide.VALUES) {
                if (!canCableConnect(node, side)) {
                    continue;
                }
                int nx = pos.getX() + side.dx();
                int ny = pos.getY() + side.dy();
                int nz = pos.getZ() + side.dz();
                if (ny < ChunkUtil.MIN_Y || ny >= ChunkUtil.HEIGHT) {
                    continue;
                }
                if (!markVisited(visited, nx, ny, nz)) {
                    continue;
                }

                long neighborChunkIndex = ChunkUtil.indexChunkFromBlock(nx, nz);
                BlockComponentChunk neighborComponents =
                        world.getChunkStore().getChunkComponent(neighborChunkIndex, BlockComponentChunk.getComponentType());
                if (neighborComponents == null) {
                    continue;
                }
                int nLocalX = ChunkUtil.localCoordinate((long) nx);
                int nLocalZ = ChunkUtil.localCoordinate((long) nz);
                int neighborIndex = ChunkUtil.indexBlockInColumn(nLocalX, ny, nLocalZ);
                EnergyNodeComponent neighbor =
                        neighborComponents.getComponent(neighborIndex, MachinariumComponents.ENERGY);
                EnergySide neighborSide = side.opposite();
                if (neighbor != null
                        && neighbor.getNodeType() == EnergyNodeComponent.NodeType.CABLE
                        && neighbor.getCableColor() == snapshot.cableColor
                        && canCableConnect(neighbor, neighborSide)) {
                    queue.add(new Vector3i(nx, ny, nz));
                }
            }
        }

        return snapshot.cableCount > 0 ? snapshot : null;
    }

    private ItemNetworkSnapshot collectItemNetwork(World world) {
        if (world == null || MachinariumComponents.ITEM == null) {
            return null;
        }

        ItemNetworkSnapshot snapshot = new ItemNetworkSnapshot();
        Deque<Vector3i> queue = new ArrayDeque<>();
        Long2ObjectMap<IntOpenHashSet> visited = new Long2ObjectOpenHashMap<>();

        if (!markVisited(visited, targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
            return null;
        }
        queue.add(targetPos);

        while (!queue.isEmpty()) {
            Vector3i pos = queue.removeFirst();
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk components =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (components == null) {
                continue;
            }

            int localX = ChunkUtil.localCoordinate((long) pos.getX());
            int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
            int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
            ItemNodeComponent node = components.getComponent(blockIndex, MachinariumComponents.ITEM);
            if (node == null) {
                continue;
            }

            snapshot.addCable(node, components, blockIndex, new Vector3i(pos.getX(), pos.getY(), pos.getZ()));

            for (EnergySide side : EnergySide.VALUES) {
                int nx = pos.getX() + side.dx();
                int ny = pos.getY() + side.dy();
                int nz = pos.getZ() + side.dz();
                if (ny < ChunkUtil.MIN_Y || ny >= ChunkUtil.HEIGHT) {
                    continue;
                }
                if (!markVisited(visited, nx, ny, nz)) {
                    continue;
                }
                long neighborChunkIndex = ChunkUtil.indexChunkFromBlock(nx, nz);
                BlockComponentChunk neighborComponents =
                        world.getChunkStore().getChunkComponent(neighborChunkIndex, BlockComponentChunk.getComponentType());
                if (neighborComponents == null) {
                    continue;
                }
                int nLocalX = ChunkUtil.localCoordinate((long) nx);
                int nLocalZ = ChunkUtil.localCoordinate((long) nz);
                int neighborIndex = ChunkUtil.indexBlockInColumn(nLocalX, ny, nLocalZ);
                ItemNodeComponent neighbor =
                        neighborComponents.getComponent(neighborIndex, MachinariumComponents.ITEM);
                if (neighbor != null) {
                    queue.add(new Vector3i(nx, ny, nz));
                }
            }
        }

        return snapshot.cableCount > 0 ? snapshot : null;
    }

    private boolean canCableConnect(EnergyNodeComponent node, EnergySide side) {
        return node.allowsInput(side) || node.allowsOutput(side);
    }

    private boolean markVisited(Long2ObjectMap<IntOpenHashSet> visited, int x, int y, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        int localX = ChunkUtil.localCoordinate((long) x);
        int localZ = ChunkUtil.localCoordinate((long) z);
        int blockIndex = ChunkUtil.indexBlockInColumn(localX, y, localZ);

        IntOpenHashSet set = visited.get(chunkIndex);
        if (set == null) {
            set = new IntOpenHashSet();
            visited.put(chunkIndex, set);
        }
        return set.add(blockIndex);
    }

    private void applyEnergyCableTier(EnergyNodeComponent node, int tier) {
        int capacity = CableUpgradeConfig.getEnergyCapacityForTier(tier);
        node.setCapacity(capacity);
        if (node.getEnergy() > capacity) {
            node.setEnergy(capacity);
        }
        node.setMaxTransfer(CableUpgradeConfig.getEnergyMaxTransferForTier(tier));
    }

    private void applyItemCableTier(ItemNodeComponent node, int tier) {
        node.setMaxTransfer(CableUpgradeConfig.getItemMaxTransferForTier(tier));
    }

    private ItemContainer getPlayerInventory(Store<EntityStore> store) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return null;
        }
        Player player = store.getComponent(playerEntityRef, Player.getComponentType());
        if (player == null) {
            return null;
        }
        Inventory inventory = player.getInventory();
        return inventory == null ? null : inventory.getStorage();
    }

    private int countItem(ItemContainer container, String itemId) {
        if (container == null || itemId == null) {
            return 0;
        }
        short capacity = container.getCapacity();
        int total = 0;
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            if (itemId.equals(stack.getItemId())) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    private String formatRequirementName(String itemId) {
        if (itemId == null) {
            return "";
        }
        String name = itemId;
        String prefix = "Ingredient_Bar_";
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length()) + " Bar";
        }
        return name.replace('_', ' ');
    }

    private void sendPlayerMessage(Ref<EntityStore> playerRef, Store<EntityStore> store, String text) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.sendMessage(Message.raw(text));
    }

    private World getWorld(Store<EntityStore> store) {
        EntityStore entityStore = store.getExternalData();
        return entityStore == null ? null : entityStore.getWorld();
    }

    private void queueCableUpgrade(
            World world,
            Vector3i pos,
            String baseId,
            String upgradedId,
            EnergyNodeComponent energyNode,
            ItemNodeComponent itemNode) {
        if (world == null || pos == null || baseId == null || upgradedId == null) {
            return;
        }
        BlockType blockType = world.getBlockType(pos.getX(), pos.getY(), pos.getZ());
        if (blockType == null || blockType.getId() == null) {
            return;
        }
        String blockId = blockType.getId();
        String resolvedUpgradedId = TieredIdUtil.applyNamespace(blockId, baseId, upgradedId);
        if (isIdOrState(blockId, resolvedUpgradedId)) {
            return;
        }
        UpgradePersistence.queueBlockSwap(world, pos, resolvedUpgradedId, energyNode, itemNode);
    }

    private boolean isIdOrState(String blockId, String baseId) {
        if (blockId == null || baseId == null) {
            return false;
        }
        String normalized = TieredIdUtil.stripNamespace(blockId, baseId);
        if (normalized == null) {
            return false;
        }
        if (normalized.equalsIgnoreCase(baseId)) {
            return true;
        }
        if (!normalized.regionMatches(true, 0, baseId, 0, baseId.length())) {
            return false;
        }
        if (normalized.length() == baseId.length()) {
            return false;
        }
        char separator = normalized.charAt(baseId.length());
        return !Character.isLetterOrDigit(separator);
    }

    private static final class EnergyNetworkSnapshot {
        private final List<EnergyCableEntry> cables = new ArrayList<>();
        private int cableCount;
        private int totalEnergy;
        private int totalCapacity;
        private int maxTransfer = Integer.MAX_VALUE;
        private int cableColor;
        private int minTier = Integer.MAX_VALUE;
        private int maxTier = Integer.MIN_VALUE;

        private void addCable(
                EnergyNodeComponent node,
                BlockComponentChunk components,
                int blockIndex,
                Vector3i position) {
            cables.add(new EnergyCableEntry(node, components, blockIndex, position));
            cableCount++;
            totalEnergy += node.getEnergy();
            totalCapacity += node.getCapacity();
            maxTransfer = Math.min(maxTransfer, node.getMaxTransfer());
            int tier = CableUpgradeConfig.clampTier(node.getCableTier());
            minTier = Math.min(minTier, tier);
            maxTier = Math.max(maxTier, tier);
        }
    }

    private static final class EnergyCableEntry {
        private final EnergyNodeComponent node;
        private final BlockComponentChunk components;
        private final int blockIndex;
        private final Vector3i position;

        private EnergyCableEntry(
                EnergyNodeComponent node,
                BlockComponentChunk components,
                int blockIndex,
                Vector3i position) {
            this.node = node;
            this.components = components;
            this.blockIndex = blockIndex;
            this.position = position;
        }
    }

    private static final class ItemNetworkSnapshot {
        private final List<ItemCableEntry> cables = new ArrayList<>();
        private int cableCount;
        private int maxTransfer = Integer.MAX_VALUE;
        private int minTier = Integer.MAX_VALUE;
        private int maxTier = Integer.MIN_VALUE;

        private void addCable(
                ItemNodeComponent node,
                BlockComponentChunk components,
                int blockIndex,
                Vector3i position) {
            cables.add(new ItemCableEntry(node, components, blockIndex, position));
            cableCount++;
            maxTransfer = Math.min(maxTransfer, node.getMaxTransfer());
            int tier = CableUpgradeConfig.clampTier(node.getCableTier());
            minTier = Math.min(minTier, tier);
            maxTier = Math.max(maxTier, tier);
        }
    }

    private static final class ItemCableEntry {
        private final ItemNodeComponent node;
        private final BlockComponentChunk components;
        private final int blockIndex;
        private final Vector3i position;

        private ItemCableEntry(
                ItemNodeComponent node,
                BlockComponentChunk components,
                int blockIndex,
                Vector3i position) {
            this.node = node;
            this.components = components;
            this.blockIndex = blockIndex;
            this.position = position;
        }
    }
}
