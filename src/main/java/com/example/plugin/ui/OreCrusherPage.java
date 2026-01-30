package com.example.plugin.ui;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.example.plugin.UpgradePersistence;
import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.energy.EnergyUnits;
import com.example.plugin.machine.MachineComponent;
import com.example.plugin.machine.MachineItemAccess;
import com.example.plugin.machine.OreCrusherConfig;
import com.example.plugin.machine.OreCrusherMachine;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
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
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
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
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OreCrusherPage extends InteractiveCustomUIPage<SideToggleEvent> implements WindowProvider {
    private static final String ACTION_UPGRADE = "Upgrade";
    private static final String ACTION_TOGGLE = "ToggleEnabled";
    private static final String PAGE_LAYOUT = "Machinarium_OreCrusher.ui";
    private static final long UPDATE_INTERVAL_MS = 250L;
    private static final String ENABLE_LABEL = "TURN ON";
    private static final String DISABLE_LABEL = "TURN OFF";
    private static final short INPUT_SLOT = 0;
    private static final short OUTPUT_SLOT = 1;
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

    private Ref<ChunkStore> blockRef;
    private final ComponentType<ChunkStore, EnergyNodeComponent> energyType;
    private final ComponentType<ChunkStore, MachineComponent> machineType;
    private Vector3i blockPosition;
    private int lastEnergy = Integer.MIN_VALUE;
    private int lastCapacity = Integer.MIN_VALUE;
    private int lastConsumption = Integer.MIN_VALUE;
    private int lastTier = Integer.MIN_VALUE;
    private int lastYield = Integer.MIN_VALUE;
    private int lastProgress = Integer.MIN_VALUE;
    private int lastProgressMax = Integer.MIN_VALUE;
    private String lastReqKey = "";
    private String lastButtonText = "";
    private String lastInputKey = "";
    private String lastOutputKey = "";
    private Boolean lastEnabled;
    private long lastUpdateMs;

    public OreCrusherPage(
            PlayerRef playerRef,
            Ref<ChunkStore> blockRef,
            ComponentType<ChunkStore, EnergyNodeComponent> energyType,
            ComponentType<ChunkStore, MachineComponent> machineType) {
        super(playerRef, CustomPageLifetime.CanDismiss, SideToggleEvent.CODEC);
        this.blockRef = blockRef;
        this.energyType = energyType;
        this.machineType = machineType;
    }

    public Ref<ChunkStore> getBlockRef() {
        return blockRef;
    }

    @Override
    public void build(
            Ref<EntityStore> playerRef,
            UICommandBuilder uiCommandBuilder,
            UIEventBuilder uiEventBuilder,
            Store<EntityStore> store) {
        uiCommandBuilder.append(PAGE_LAYOUT);
        initStaticUi(uiCommandBuilder);
        bindButtons(uiEventBuilder);

        World world = getWorld(store);
        ItemContainer inventory = getPlayerInventory(store);
        updateForWorld(uiCommandBuilder, world, inventory);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, SideToggleEvent data) {
        if (data == null || data.getAction() == null) {
            return;
        }

        World world = getWorld(store);
        if (world == null) {
            return;
        }

        MachineComponent machine = resolveMachine(world);
        if (machine == null) {
            machine = new MachineComponent();
        }
        EnergyNodeComponent node = resolveNode(world);

        String action = data.getAction();
        if (ACTION_UPGRADE.equalsIgnoreCase(action)) {
            if (!handleUpgrade(playerRef, store, world, machine, node)) {
                sendUpdate(new UICommandBuilder());
            }
            return;
        }
        if (ACTION_TOGGLE.equalsIgnoreCase(action)) {
            machine.setEnabled(!machine.isEnabled());
            machine.setProgress(0);
            storeMachine(world, machine);
            UICommandBuilder update = new UICommandBuilder();
            updateControls(update, machine);
            sendUpdate(update);
        }
    }

    @Override
    public Window[] createWindows(Ref<EntityStore> playerEntityRef, Store<EntityStore> store) {
        World world = getWorld(store);
        if (world == null) {
            return null;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos == null) {
            return null;
        }
        ItemContainerBlockState state = MachineItemAccess.ensureContainerState(
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ());
        ItemContainer container = state == null ? null : state.getItemContainer();
        if (container == null) {
            return null;
        }
        BlockType blockType = world.getBlockType(pos.getX(), pos.getY(), pos.getZ());
        if (blockType == null) {
            return null;
        }
        int rotationIndex = world.getBlockRotationIndex(pos.getX(), pos.getY(), pos.getZ());
        return new Window[] {
                new ContainerBlockWindow(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        rotationIndex,
                        blockType,
                        container)
        };
    }

    public void update(EnergyNodeComponent node, MachineComponent machine) {
        update(node, machine, false);
    }

    private void update(EnergyNodeComponent node, MachineComponent machine, boolean force) {
        if (node == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!force && lastUpdateMs != 0L && now - lastUpdateMs < UPDATE_INTERVAL_MS) {
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        Store<EntityStore> store = playerEntityRef == null ? null : playerEntityRef.getStore();
        ItemContainer inventory = store == null ? null : getPlayerInventory(store);

        UICommandBuilder update = new UICommandBuilder();
        boolean changed = updateEnergy(update, node);
        changed |= updateProgress(update, machine);
        changed |= updateYield(update, machine);
        changed |= updateControls(update, machine);
        String reqKey = updateUpgradePanel(update, machine, inventory);
        if (!reqKey.equals(lastReqKey)) {
            lastReqKey = reqKey;
            changed = true;
        }
        changed |= updateSlots(update);

        if (changed) {
            sendUpdate(update);
        } else if (force) {
            sendUpdate(new UICommandBuilder());
        }
        lastUpdateMs = now;
    }

    private void initStaticUi(UICommandBuilder update) {
        update.set("#CrusherTier.Text", "Tier: " + OreCrusherConfig.getTierName(OreCrusherConfig.MIN_TIER));
        update.set("#CrusherEnergy.Text", "Energy: 0 / 0");
        update.set("#CrusherConsumption.Text", "Consumption: 0 J/s");
        update.set("#CrusherProgress.Text", "Progress: 0%");
        update.set("#CrusherYield.Text", "Yield: 2x");
        update.set("#CrusherStatus.Text", "Status: ON");
        update.set("#CrusherToggleButton.Text", DISABLE_LABEL);
        updateUpgradePanel(update, null, null);
        update.set("#InputSlot.ItemId", "");
        update.set("#InputQty.Text", "");
        update.set("#OutputSlot.ItemId", "");
        update.set("#OutputQty.Text", "");
    }

    private boolean updateForWorld(UICommandBuilder update, World world, ItemContainer inventory) {
        if (world == null) {
            return false;
        }
        EnergyNodeComponent node = resolveNode(world);
        MachineComponent machine = resolveMachine(world);
        if (node == null) {
            return false;
        }
        boolean changed = updateEnergy(update, node);
        changed |= updateProgress(update, machine);
        changed |= updateYield(update, machine);
        changed |= updateControls(update, machine);
        String reqKey = updateUpgradePanel(update, machine, inventory);
        if (!reqKey.equals(lastReqKey)) {
            lastReqKey = reqKey;
            changed = true;
        }
        changed |= updateSlots(update);
        return changed;
    }

    private boolean updateEnergy(UICommandBuilder update, EnergyNodeComponent node) {
        boolean changed = false;
        int energy = node.getEnergy();
        int capacity = node.getCapacity();
        if (energy != lastEnergy || capacity != lastCapacity) {
            update.set("#CrusherEnergy.Text",
                    "Energy: " + EnergyUnits.formatEnergyWithCapacity(energy, capacity));
            lastEnergy = energy;
            lastCapacity = capacity;
            changed = true;
        }

        int consumption = node.getConsumption();
        if (consumption != lastConsumption) {
            update.set("#CrusherConsumption.Text",
                    "Consumption: " + EnergyUnits.formatJoulesPerSecond(consumption));
            lastConsumption = consumption;
            changed = true;
        }

        return changed;
    }

    private boolean updateProgress(UICommandBuilder update, MachineComponent machine) {
        if (machine == null) {
            return false;
        }
        int progressMax = Math.max(1, machine.getProgressMax());
        int progress = Math.max(0, machine.getProgress());
        if (progress != lastProgress || progressMax != lastProgressMax) {
            int percent = (int) Math.round(100.0 * progress / progressMax);
            update.set("#CrusherProgress.Text", "Progress: " + percent + "%");
            lastProgress = progress;
            lastProgressMax = progressMax;
            return true;
        }
        return false;
    }

    private boolean updateYield(UICommandBuilder update, MachineComponent machine) {
        int tier = machine == null ? OreCrusherConfig.MIN_TIER : OreCrusherConfig.clampTier(machine.getTier());
        int yield = OreCrusherConfig.getOutputMultiplierForTier(tier);
        if (yield != lastYield) {
            update.set("#CrusherYield.Text", "Yield: " + yield + "x");
            lastYield = yield;
            return true;
        }
        return false;
    }

    private boolean updateControls(UICommandBuilder update, MachineComponent machine) {
        boolean enabled = machine == null || machine.isEnabled();
        if (lastEnabled == null || enabled != lastEnabled) {
            update.set("#CrusherStatus.Text", enabled ? "Status: ON" : "Status: OFF");
            update.set("#CrusherToggleButton.Text", enabled ? DISABLE_LABEL : ENABLE_LABEL);
            lastEnabled = enabled;
            return true;
        }
        return false;
    }

    private boolean updateSlots(UICommandBuilder update) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return false;
        }
        Store<EntityStore> store = playerEntityRef.getStore();
        World world = getWorld(store);
        Vector3i pos = resolveBlockPosition(world);
        if (world == null || pos == null) {
            return false;
        }
        ItemContainer container = MachineItemAccess.getContainer(world, pos.getX(), pos.getY(), pos.getZ());
        if (container == null) {
            return updateEmptySlots(update);
        }

        ItemStack input = container.getCapacity() > INPUT_SLOT
                ? container.getItemStack(INPUT_SLOT)
                : null;
        ItemStack output = container.getCapacity() > OUTPUT_SLOT
                ? container.getItemStack(OUTPUT_SLOT)
                : null;

        String inputKey = buildSlotKey(input);
        String outputKey = buildSlotKey(output);
        boolean changed = false;

        if (!inputKey.equals(lastInputKey)) {
            update.set("#InputSlot.ItemId", UiItemIds.safeItemId(getItemId(input)));
            update.set("#InputQty.Text", getQtyText(input));
            lastInputKey = inputKey;
            changed = true;
        }
        if (!outputKey.equals(lastOutputKey)) {
            update.set("#OutputSlot.ItemId", UiItemIds.safeItemId(getItemId(output)));
            update.set("#OutputQty.Text", getQtyText(output));
            lastOutputKey = outputKey;
            changed = true;
        }

        return changed;
    }

    private boolean updateEmptySlots(UICommandBuilder update) {
        boolean changed = false;
        if (!"empty".equals(lastInputKey)) {
            update.set("#InputSlot.ItemId", "");
            update.set("#InputQty.Text", "");
            lastInputKey = "empty";
            changed = true;
        }
        if (!"empty".equals(lastOutputKey)) {
            update.set("#OutputSlot.ItemId", "");
            update.set("#OutputQty.Text", "");
            lastOutputKey = "empty";
            changed = true;
        }
        return changed;
    }

    private String updateUpgradePanel(UICommandBuilder update, MachineComponent machine, ItemContainer inventory) {
        int tier = machine == null ? OreCrusherConfig.MIN_TIER : OreCrusherConfig.clampTier(machine.getTier());
        if (tier != lastTier) {
            update.set("#CrusherTier.Text", "Tier: " + OreCrusherConfig.getTierName(tier));
            lastTier = tier;
        }

        boolean hasNextTier = OreCrusherConfig.hasNextTier(tier);
        String nextText = hasNextTier
                ? "Next: " + OreCrusherConfig.getTierName(tier + 1)
                : "Next: Max";
        update.set("#CrusherNextTier.Text", nextText);

        String statsText = hasNextTier
                ? "Capacity: " + EnergyUnits.formatJoules(OreCrusherConfig.getCapacityForTier(tier + 1))
                        + " | Consumption: "
                        + EnergyUnits.formatJoulesPerSecond(OreCrusherConfig.getConsumptionPerSecond(tier + 1))
                        + " | Speed: " + String.format(
                                Locale.US,
                                "%.2fs",
                                OreCrusherConfig.getProcessingSecondsForTier(tier + 1))
                        + " | Yield: " + OreCrusherConfig.getOutputMultiplierForTier(tier + 1) + "x"
                : "Max tier reached.";
        update.set("#CrusherUpgradeStats.Text", statsText);

        OreCrusherConfig.Requirement[] requirements =
                hasNextTier ? OreCrusherConfig.getUpgradeRequirements(tier)
                        : new OreCrusherConfig.Requirement[0];
        int[] owned = new int[requirements.length];
        boolean canUpgrade = hasNextTier && inventory != null;
        StringBuilder reqKey = new StringBuilder();
        for (int i = 0; i < requirements.length; i++) {
            OreCrusherConfig.Requirement requirement = requirements[i];
            owned[i] = inventory == null ? 0 : countItem(inventory, requirement.getItemId());
            if (owned[i] < requirement.getQuantity()) {
                canUpgrade = false;
            }
            reqKey.append(requirement.getItemId())
                    .append('=')
                    .append(owned[i])
                    .append('/');
        }

        String buttonText = hasNextTier
                ? (canUpgrade ? "Upgrade" : "Missing Items")
                : "Max Tier";
        update.set("#UpgradeButton.Text", buttonText);
        if (!buttonText.equals(lastButtonText)) {
            lastButtonText = buttonText;
        }

        update.set("#UpgradeReqEmpty.Text", "No further upgrades.");
        update.set("#UpgradeReqEmpty.Visible", !hasNextTier);
        for (int i = 0; i < UPGRADE_ROW_IDS.length; i++) {
            boolean visible = hasNextTier && i < requirements.length;
            update.set(UPGRADE_ROW_IDS[i] + ".Visible", visible);
            if (visible) {
                OreCrusherConfig.Requirement requirement = requirements[i];
                update.set(UPGRADE_SLOT_IDS[i] + ".ItemId", UiItemIds.safeItemId(requirement.getItemId()));
                update.set(UPGRADE_NAME_IDS[i] + ".Text", formatRequirementName(requirement.getItemId()));
                update.set(UPGRADE_QTY_IDS[i] + ".Text", owned[i] + "/" + requirement.getQuantity());
            } else {
                update.set(UPGRADE_SLOT_IDS[i] + ".ItemId", "");
                update.set(UPGRADE_NAME_IDS[i] + ".Text", "");
                update.set(UPGRADE_QTY_IDS[i] + ".Text", "");
            }
        }

        reqKey.append("tier=").append(tier).append("|button=").append(buttonText);
        return reqKey.toString();
    }

    private boolean handleUpgrade(
            Ref<EntityStore> playerRef,
            Store<EntityStore> store,
            World world,
            MachineComponent machine,
            EnergyNodeComponent node) {
        if (world == null || machine == null || node == null) {
            return false;
        }
        int currentTier = OreCrusherConfig.clampTier(machine.getTier());
        if (!OreCrusherConfig.hasNextTier(currentTier)) {
            sendPlayerMessage(playerRef, store, "Ore crusher is already at max tier.");
            return false;
        }

        ItemContainer inventory = getPlayerInventory(store);
        if (inventory == null) {
            return false;
        }

        OreCrusherConfig.Requirement[] requirements =
                OreCrusherConfig.getUpgradeRequirements(currentTier);
        List<ItemStack> stacks = new ArrayList<>(requirements.length);
        for (OreCrusherConfig.Requirement requirement : requirements) {
            stacks.add(new ItemStack(requirement.getItemId(), requirement.getQuantity()));
        }

        if (!stacks.isEmpty() && !inventory.canRemoveItemStacks(stacks)) {
            sendPlayerMessage(playerRef, store, "Missing upgrade materials.");
            return false;
        }

        if (!stacks.isEmpty()) {
            ListTransaction<ItemStackTransaction> transaction = inventory.removeItemStacks(stacks);
            if (transaction == null || !transaction.succeeded()) {
                sendPlayerMessage(playerRef, store, "Upgrade failed.");
                return false;
            }
        }

        int nextTier = currentTier + 1;
        machine.setTier(nextTier);
        applyTier(machine, node, nextTier);

        storeMachine(world, machine);
        storeNode(world, node);

        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            String upgradedId = TieredIdUtil.buildTieredId(MachinariumIds.BLOCK_ORE_CRUSHER, nextTier);
            BlockType blockType = world.getBlockType(pos.getX(), pos.getY(), pos.getZ());
            String blockId = blockType == null ? null : blockType.getId();
            upgradedId = TieredIdUtil.applyNamespace(blockId, MachinariumIds.BLOCK_ORE_CRUSHER, upgradedId);
            UpgradePersistence.queueBlockSwapWithContainer(world, pos, upgradedId, node);
        }

        sendPlayerMessage(
                playerRef,
                store,
                "Upgraded ore crusher to " + OreCrusherConfig.getTierName(nextTier) + ".");
        update(node, machine, true);
        return true;
    }

    private void applyTier(MachineComponent machine, EnergyNodeComponent node, int tier) {
        int capacity = OreCrusherConfig.getCapacityForTier(tier);
        node.setCapacity(capacity);
        if (node.getEnergy() > capacity) {
            node.setEnergy(capacity);
        }
        node.setConsumption(OreCrusherConfig.getConsumptionPerSecond(tier));
        node.setMaxTransfer(OreCrusherConfig.getMaxTransferForTier(tier));

        int progressMax = OreCrusherConfig.getProcessingDelayTicks(tier);
        machine.setProgressMax(progressMax);
        if (machine.getProgress() > progressMax) {
            machine.setProgress(progressMax);
        }
    }

    private ItemContainer getPlayerInventory(Store<EntityStore> store) {
        if (store == null) {
            return null;
        }
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
        String ingotPrefix = "Ingredient_Bar_";
        if (name.startsWith(ingotPrefix)) {
            name = name.substring(ingotPrefix.length()) + " Ingot";
        }
        String hidePrefix = "Ingredient_Hide_";
        if (name.startsWith(hidePrefix)) {
            name = name.substring(hidePrefix.length()) + " Hide";
        }
        String leatherPrefix = "Ingredient_Leather_";
        if (name.startsWith(leatherPrefix)) {
            name = name.substring(leatherPrefix.length()) + " Leather";
        }
        String ingredientPrefix = "Ingredient_";
        if (name.startsWith(ingredientPrefix)) {
            name = name.substring(ingredientPrefix.length());
        }
        return name.replace('_', ' ');
    }

    private String buildSlotKey(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return "";
        }
        return stack.getItemId() + ":" + stack.getQuantity();
    }

    private String getItemId(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return "";
        }
        return stack.getItemId();
    }

    private String getQtyText(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return "";
        }
        int qty = stack.getQuantity();
        return qty > 1 ? Integer.toString(qty) : "";
    }

    Ref<ChunkStore> resolveBlockRef(World world) {
        if (world == null) {
            return blockRef;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos == null) {
            return blockRef;
        }
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
        BlockComponentChunk blockComponents =
                world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
        if (blockComponents == null) {
            return blockRef;
        }
        int localX = ChunkUtil.localCoordinate((long) pos.getX());
        int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
        int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
        Ref<ChunkStore> ref = blockComponents.getEntityReferences().get(blockIndex);
        if (ref != null && (blockRef == null || ref.getIndex() != blockRef.getIndex())) {
            blockRef = ref;
        }
        return blockRef;
    }

    private EnergyNodeComponent resolveNode(World world) {
        if (world == null) {
            return null;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk blockComponents =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (blockComponents != null) {
                int localX = ChunkUtil.localCoordinate((long) pos.getX());
                int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
                int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
                EnergyNodeComponent node = blockComponents.getComponent(blockIndex, energyType);
                if (node != null) {
                    return node;
                }
            }
        }
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> resolvedRef = resolveBlockRef(world);
        try {
            return chunkStore.getComponent(resolvedRef, energyType);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private MachineComponent resolveMachine(World world) {
        if (world == null) {
            return null;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk blockComponents =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (blockComponents != null) {
                int localX = ChunkUtil.localCoordinate((long) pos.getX());
                int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
                int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
                MachineComponent machine = blockComponents.getComponent(blockIndex, machineType);
                if (machine != null) {
                    return machine;
                }
            }
        }
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> resolvedRef = resolveBlockRef(world);
        try {
            return chunkStore.getComponent(resolvedRef, machineType);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private void storeNode(World world, EnergyNodeComponent node) {
        if (world == null || node == null) {
            return;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk blockComponents =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (blockComponents != null) {
                int localX = ChunkUtil.localCoordinate((long) pos.getX());
                int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
                int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
                Holder<ChunkStore> holder = blockComponents.getEntityHolder(blockIndex);
                if (holder == null) {
                    holder = ChunkStore.REGISTRY.newHolder();
                    holder.putComponent(energyType, node);
                    blockComponents.storeEntityHolder(blockIndex, holder);
                } else {
                    holder.putComponent(energyType, node);
                }
                blockComponents.markNeedsSaving();
                return;
            }
        }
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> resolvedRef = resolveBlockRef(world);
        try {
            chunkStore.putComponent(resolvedRef, energyType, node);
        } catch (IllegalStateException ignored) {
            // Ref may be stale right after a block swap.
        }
    }

    private void storeMachine(World world, MachineComponent machine) {
        if (world == null || machine == null) {
            return;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockComponentChunk blockComponents =
                    world.getChunkStore().getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (blockComponents != null) {
                int localX = ChunkUtil.localCoordinate((long) pos.getX());
                int localZ = ChunkUtil.localCoordinate((long) pos.getZ());
                int blockIndex = ChunkUtil.indexBlockInColumn(localX, pos.getY(), localZ);
                Holder<ChunkStore> holder = blockComponents.getEntityHolder(blockIndex);
                if (holder == null) {
                    holder = ChunkStore.REGISTRY.newHolder();
                    holder.putComponent(machineType, machine);
                    blockComponents.storeEntityHolder(blockIndex, holder);
                } else {
                    holder.putComponent(machineType, machine);
                }
                blockComponents.markNeedsSaving();
                return;
            }
        }
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> resolvedRef = resolveBlockRef(world);
        try {
            chunkStore.putComponent(resolvedRef, machineType, machine);
        } catch (IllegalStateException ignored) {
            // Ref may be stale right after a block swap.
        }
    }

    private void sendPlayerMessage(Ref<EntityStore> playerRef, Store<EntityStore> store, String text) {
        if (store == null || playerRef == null) {
            return;
        }
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.sendMessage(Message.raw(text));
    }

    Vector3i resolveBlockPosition(World world) {
        if (world == null) {
            return null;
        }
        if (blockPosition != null) {
            return blockPosition;
        }

        ChunkStore chunkStoreResource = world.getChunkStore();
        for (long chunkIndex : chunkStoreResource.getChunkIndexes()) {
            BlockComponentChunk blockComponents =
                    chunkStoreResource.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
            if (blockComponents == null) {
                continue;
            }

            int blockIndex = findBlockIndex(blockComponents);
            if (blockIndex == Integer.MIN_VALUE) {
                continue;
            }

            int chunkX = ChunkUtil.xOfChunkIndex(chunkIndex);
            int chunkZ = ChunkUtil.zOfChunkIndex(chunkIndex);
            int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
            int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
            int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);

            int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
            int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);
            blockPosition = new Vector3i(worldX, localY, worldZ);
            return blockPosition;
        }

        return null;
    }

    private int findBlockIndex(BlockComponentChunk blockComponents) {
        int targetIndex = blockRef.getIndex();
        if (targetIndex == Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        for (Int2ObjectMap.Entry<Ref<ChunkStore>> entry
                : blockComponents.getEntityReferences().int2ObjectEntrySet()) {
            Ref<ChunkStore> entryRef = entry.getValue();
            if (entryRef != null && entryRef.getIndex() == targetIndex) {
                return entry.getIntKey();
            }
        }
        return Integer.MIN_VALUE;
    }

    private World getWorld(Store<EntityStore> store) {
        EntityStore entityStore = store == null ? null : store.getExternalData();
        return entityStore == null ? null : entityStore.getWorld();
    }

    private void bindButtons(UIEventBuilder uiEventBuilder) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#UpgradeButton",
                EventData.of("Action", ACTION_UPGRADE));
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CrusherToggleButton",
                EventData.of("Action", ACTION_TOGGLE));
    }
}
