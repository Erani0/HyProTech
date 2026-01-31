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
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ListTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OreCrusherPage extends InteractiveCustomUIPage<OreCrusherUiEvent> implements WindowlessPage {
    private static final String ACTION_UPGRADE = "Upgrade";
    private static final String ACTION_TOGGLE = "ToggleEnabled";
    private static final String ACTION_DRAG_START = "DragStart";
    private static final String ACTION_INPUT_CLICK = "InputClick";
    private static final String ACTION_OUTPUT_CLICK = "OutputClick";
    private static final String ACTION_INVENTORY_CLICK = "InventoryClick";
    private static final String ACTION_INPUT_DROP = "InputDrop";
    private static final String ACTION_OUTPUT_DROP = "OutputDrop";
    private static final String ACTION_INVENTORY_DROP = "InventoryDrop";
    private static final String PAGE_LAYOUT = "Machinarium_OreCrusher_HyUI.ui";
    private static final long UPDATE_INTERVAL_MS = 250L;
    private static final long DRAG_DEDUP_WINDOW_MS = 120L;
    private static final long DRAG_SOURCE_WINDOW_MS = 1500L;
    private static final long INPUT_DROP_SUPPRESS_MS = 250L;
    private static final String ENABLE_LABEL = "TURN ON";
    private static final String DISABLE_LABEL = "TURN OFF";
    private static final short INPUT_SLOT = 0;
    private static final short OUTPUT_SLOT_START = 1;
    private static final int OUTPUT_SLOT_COUNT = OreCrusherConfig.OUTPUT_SLOT_COUNT;
    private static final short OUTPUT_SLOT_END =
            (short) (OUTPUT_SLOT_START + OUTPUT_SLOT_COUNT - 1);
    // ItemGrid index mapping follows UI order in Machinarium_OreCrusher_HyUI.ui
    private static final int GRID_INDEX_INPUT = 0;
    private static final int GRID_INDEX_OUTPUT = 1;
    private static final int GRID_INDEX_PLAYER = 2;
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
    private static final String[] BONUS_ROW_IDS = {
            "#BonusRow1",
            "#BonusRow2",
            "#BonusRow3",
            "#BonusRow4",
            "#BonusRow5"
    };
    private static final String[] BONUS_SLOT_IDS = {
            "#BonusSlot1",
            "#BonusSlot2",
            "#BonusSlot3",
            "#BonusSlot4",
            "#BonusSlot5"
    };
    private static final String[] BONUS_NAME_IDS = {
            "#BonusName1",
            "#BonusName2",
            "#BonusName3",
            "#BonusName4",
            "#BonusName5"
    };
    private static final String[] BONUS_CHANCE_IDS = {
            "#BonusChance1",
            "#BonusChance2",
            "#BonusChance3",
            "#BonusChance4",
            "#BonusChance5"
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
    private String lastInventoryKey = "";
    private String lastBonusKey = "";
    private String lastDragKey = "";
    private Boolean lastEnabled;
    private long lastUpdateMs;
    private long lastDragMs;
    private DragSnapshot lastDragSource;
    private long lastInputDropMs;
    private String lastInputDropItemId;

    public OreCrusherPage(
            PlayerRef playerRef,
            Ref<ChunkStore> blockRef,
            ComponentType<ChunkStore, EnergyNodeComponent> energyType,
            ComponentType<ChunkStore, MachineComponent> machineType) {
        super(playerRef, CustomPageLifetime.CanDismiss, OreCrusherUiEvent.CODEC);
        this.blockRef = blockRef;
        this.energyType = energyType;
        this.machineType = machineType;
    }

    public Ref<ChunkStore> getBlockRef() {
        return blockRef;
    }

    public void setBlockPosition(Vector3i blockPosition) {
        this.blockPosition = blockPosition;
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
        bindItemGrids(uiEventBuilder);

        World world = getWorld(store);
        ItemContainer inventory = getPlayerInventory(store);
        updateForWorld(uiCommandBuilder, world, inventory);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, OreCrusherUiEvent data) {
        if (data == null || data.getAction() == null) {
            return;
        }

        String action = data.getAction();
        if (ACTION_DRAG_START.equalsIgnoreCase(action)) {
            captureDragSource(data);
            return;
        }
        if (ACTION_INPUT_CLICK.equalsIgnoreCase(action)) {
            captureClickSource(GridType.INPUT, data);
            return;
        }
        if (ACTION_OUTPUT_CLICK.equalsIgnoreCase(action)) {
            captureClickSource(GridType.OUTPUT, data);
            return;
        }
        if (ACTION_INVENTORY_CLICK.equalsIgnoreCase(action)) {
            captureClickSource(GridType.PLAYER, data);
            return;
        }
        if (isDragAction(action)) {
            handleDrag(store, data);
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
        changed |= updateBonusPanel(update, machine, resolveMachineContainer(store));

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
        update.set("#InputGrid.Slots", buildEmptySlots(1, true));
        update.set("#OutputGrid.Slots", buildEmptySlots(OUTPUT_SLOT_COUNT, true));
        update.set("#PlayerInventoryGrid.Slots",
                buildEmptySlots(Inventory.DEFAULT_HOTBAR_CAPACITY + Inventory.DEFAULT_STORAGE_CAPACITY, true));
        updateBonusPanel(update, null, null);
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
        ItemContainer container = null;
        Vector3i pos = resolveBlockPosition(world);
        if (pos != null) {
            container = MachineItemAccess.getContainer(world, pos.getX(), pos.getY(), pos.getZ());
        }
        changed |= updateBonusPanel(update, machine, container);
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
            return updatePlayerInventorySlots(update, store);
        }
        ItemContainer container = MachineItemAccess.getContainer(world, pos.getX(), pos.getY(), pos.getZ());
        boolean changed = updateMachineSlots(update, container);
        changed |= updatePlayerInventorySlots(update, store);
        return changed;
    }

    private boolean updateMachineSlots(UICommandBuilder update, ItemContainer container) {
        if (container == null) {
            return updateEmptyMachineSlots(update);
        }

        ItemStack input = container.getCapacity() > INPUT_SLOT
                ? container.getItemStack(INPUT_SLOT)
                : null;

        String inputKey = buildSlotKey(input);
        String outputKey = buildOutputKey(container);
        boolean changed = false;

        if (!inputKey.equals(lastInputKey)) {
            update.set("#InputGrid.Slots", buildSingleSlotList(input));
            lastInputKey = inputKey;
            changed = true;
        }
        if (!outputKey.equals(lastOutputKey)) {
            update.set("#OutputGrid.Slots", buildOutputSlots(container));
            lastOutputKey = outputKey;
            changed = true;
        }

        return changed;
    }

    private boolean updatePlayerInventorySlots(UICommandBuilder update, Store<EntityStore> store) {
        Inventory inventory = getPlayerInventoryFull(store);
        int hotbarCapacity = getHotbarCapacity(inventory);
        int storageCapacity = getStorageCapacity(inventory);
        int totalSlots = hotbarCapacity + storageCapacity;
        String inventoryKey = buildInventoryKey(inventory, hotbarCapacity, storageCapacity);
        if (!inventoryKey.equals(lastInventoryKey)) {
            if (inventory != null) {
                ItemContainer hotbar = inventory.getHotbar();
                ItemContainer storage = inventory.getStorage();
                ItemContainer utility = inventory.getUtility();
                ItemContainer tools = inventory.getTools();
                ItemContainer armor = inventory.getArmor();
                ItemContainer backpack = inventory.getBackpack();
                System.out.println(
                        "[Machinarium] DBG Inv slots hotbar=" + hotbarCapacity
                                + " storage=" + storageCapacity
                                + " total=" + totalSlots
                                + " caps[hotbar=" + (hotbar == null ? "null" : hotbar.getCapacity())
                                + " storage=" + (storage == null ? "null" : storage.getCapacity())
                                + " utility=" + (utility == null ? "null" : utility.getCapacity())
                                + " tools=" + (tools == null ? "null" : tools.getCapacity())
                                + " armor=" + (armor == null ? "null" : armor.getCapacity())
                                + " backpack=" + (backpack == null ? "null" : backpack.getCapacity())
                                + "]");
            }
            update.set(
                    "#PlayerInventoryGrid.Slots",
                    buildPlayerSlots(inventory, hotbarCapacity, storageCapacity));
            lastInventoryKey = inventoryKey;
            return true;
        }
        return false;
    }

    private boolean updateEmptyMachineSlots(UICommandBuilder update) {
        boolean changed = false;
        if (!"empty".equals(lastInputKey)) {
            update.set("#InputGrid.Slots", buildEmptySlots(1, true));
            lastInputKey = "empty";
            changed = true;
        }
        if (!"empty".equals(lastOutputKey)) {
            update.set("#OutputGrid.Slots", buildEmptySlots(OUTPUT_SLOT_COUNT, true));
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

    private boolean updateBonusPanel(UICommandBuilder update, MachineComponent machine, ItemContainer container) {
        int tier = machine == null ? OreCrusherConfig.MIN_TIER : OreCrusherConfig.clampTier(machine.getTier());
        ItemStack input = container == null || container.getCapacity() <= INPUT_SLOT
                ? null
                : container.getItemStack(INPUT_SLOT);
        String oreId = input == null || ItemStack.isEmpty(input) ? null : input.getItemId();
        List<OreCrusherConfig.BonusDrop> drops = OreCrusherConfig.getBonusDropsForOre(oreId);

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(tier).append('|');
        for (OreCrusherConfig.BonusDrop drop : drops) {
            if (drop == null) {
                continue;
            }
            keyBuilder.append(drop.getItemId())
                    .append(':')
                    .append(drop.getQuantity())
                    .append(':')
                    .append(Math.round(drop.getChance(tier) * 100))
                    .append('|');
        }
        String bonusKey = keyBuilder.toString();
        if (bonusKey.equals(lastBonusKey)) {
            return false;
        }
        lastBonusKey = bonusKey;

        update.set("#BonusHeader.Text", "Bonus Drops");
        boolean hasDrops = !drops.isEmpty();
        update.set("#BonusEmpty.Visible", !hasDrops);
        update.set("#BonusEmpty.Text", hasDrops ? "" : "Insert ore to see bonus drops.");

        int count = Math.min(drops.size(), BONUS_ROW_IDS.length);
        for (int i = 0; i < BONUS_ROW_IDS.length; i++) {
            boolean visible = i < count;
            update.set(BONUS_ROW_IDS[i] + ".Visible", visible);
            if (!visible) {
                continue;
            }
            OreCrusherConfig.BonusDrop drop = drops.get(i);
            if (drop == null) {
                update.set(BONUS_SLOT_IDS[i] + ".ItemId", UiItemIds.safeItemId(null));
                update.set(BONUS_NAME_IDS[i] + ".Text", "");
                update.set(BONUS_CHANCE_IDS[i] + ".Text", "");
                continue;
            }
            update.set(BONUS_SLOT_IDS[i] + ".ItemId", UiItemIds.safeItemId(drop.getItemId()));
            update.set(BONUS_NAME_IDS[i] + ".Text", formatBonusName(drop.getItemId()));
            int percent = (int) Math.round(drop.getChance(tier) * 100);
            update.set(BONUS_CHANCE_IDS[i] + ".Text", percent + "%");
        }

        return true;
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

    private Inventory getPlayerInventoryFull(Store<EntityStore> store) {
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
        return player.getInventory();
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

    private String formatBonusName(String itemId) {
        if (itemId == null) {
            return "";
        }
        String name = itemId;
        String machinariumPrefix = "Machinarium_";
        if (name.startsWith(machinariumPrefix)) {
            name = name.substring(machinariumPrefix.length());
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
        String key = stack.getItemId() + ":" + stack.getQuantity();
        if (stack.getMetadata() != null) {
            key += ":" + stack.getMetadata().hashCode();
        }
        return key;
    }

    private String buildOutputKey(ItemContainer container) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            ItemStack stack = getOutputStack(container, i);
            appendStackKey(key, stack);
        }
        return key.toString();
    }

    private List<ItemGridSlot> buildEmptySlots(int count, boolean skipQualityBackground) {
        List<ItemGridSlot> slots = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            slots.add(createSlot(null, skipQualityBackground));
        }
        return slots;
    }

    private List<ItemGridSlot> buildSingleSlotList(ItemStack stack) {
        List<ItemGridSlot> slots = new ArrayList<>(1);
        slots.add(createSlot(stack, true));
        return slots;
    }

    private List<ItemGridSlot> buildOutputSlots(ItemContainer container) {
        List<ItemGridSlot> slots = new ArrayList<>(OUTPUT_SLOT_COUNT);
        for (int i = 0; i < OUTPUT_SLOT_COUNT; i++) {
            slots.add(createSlot(getOutputStack(container, i), true));
        }
        return slots;
    }

    private List<ItemGridSlot> buildPlayerSlots(
            Inventory inventory,
            int hotbarCapacity,
            int storageCapacity) {
        List<ItemGridSlot> slots = new ArrayList<>(hotbarCapacity + storageCapacity);
        ItemContainer hotbar = inventory == null ? null : inventory.getHotbar();
        ItemContainer storage = inventory == null ? null : inventory.getStorage();
        appendSlotsFromContainer(slots, hotbar, hotbarCapacity);
        appendSlotsFromContainer(slots, storage, storageCapacity);
        return slots;
    }

    private void appendSlotsFromContainer(
            List<ItemGridSlot> slots,
            ItemContainer container,
            int capacity) {
        for (int i = 0; i < capacity; i++) {
            ItemStack stack = null;
            if (container != null && i < container.getCapacity()) {
                stack = container.getItemStack((short) i);
            }
            slots.add(createSlot(stack, true));
        }
    }

    private ItemStack getOutputStack(ItemContainer container, int outputIndex) {
        if (container == null || outputIndex < 0 || outputIndex >= OUTPUT_SLOT_COUNT) {
            return null;
        }
        short slot = (short) (OUTPUT_SLOT_START + outputIndex);
        if (slot < 0 || slot >= container.getCapacity()) {
            return null;
        }
        return container.getItemStack(slot);
    }

    private ItemGridSlot createSlot(ItemStack stack, boolean skipQualityBackground) {
        ItemGridSlot slot = new ItemGridSlot();
        boolean hasItem = stack != null && !ItemStack.isEmpty(stack);
        // Allow drops into empty slots; otherwise input grid cannot accept items by drag.
        slot.setActivatable(true);
        slot.setSkipItemQualityBackground(skipQualityBackground);
        if (hasItem) {
            slot.setItemStack(copyStack(stack));
        }
        return slot;
    }

    private ItemStack copyStack(ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            return null;
        }
        return new ItemStack(stack.getItemId(), stack.getQuantity(), stack.getMetadata());
    }

    private int getHotbarCapacity(Inventory inventory) {
        ItemContainer hotbar = inventory == null ? null : inventory.getHotbar();
        return hotbar == null ? Inventory.DEFAULT_HOTBAR_CAPACITY : hotbar.getCapacity();
    }

    private int getStorageCapacity(Inventory inventory) {
        ItemContainer storage = inventory == null ? null : inventory.getStorage();
        return storage == null ? Inventory.DEFAULT_STORAGE_CAPACITY : storage.getCapacity();
    }

    private String buildInventoryKey(Inventory inventory, int hotbarCapacity, int storageCapacity) {
        StringBuilder key = new StringBuilder();
        ItemContainer hotbar = inventory == null ? null : inventory.getHotbar();
        ItemContainer storage = inventory == null ? null : inventory.getStorage();
        appendInventoryKey(key, hotbar, hotbarCapacity);
        appendInventoryKey(key, storage, storageCapacity);
        return key.toString();
    }

    private void appendInventoryKey(StringBuilder key, ItemContainer container, int capacity) {
        for (int i = 0; i < capacity; i++) {
            ItemStack stack = null;
            if (container != null && i < container.getCapacity()) {
                stack = container.getItemStack((short) i);
            }
            appendStackKey(key, stack);
        }
    }

    private void appendStackKey(StringBuilder key, ItemStack stack) {
        if (stack == null || ItemStack.isEmpty(stack)) {
            key.append("|");
            return;
        }
        key.append(stack.getItemId())
                .append(':')
                .append(stack.getQuantity());
        if (stack.getMetadata() != null) {
            key.append(':').append(stack.getMetadata().hashCode());
        }
        key.append('|');
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

    private ItemContainer resolveMachineContainer(Store<EntityStore> store) {
        World world = getWorld(store);
        if (world == null) {
            return null;
        }
        Vector3i pos = resolveBlockPosition(world);
        if (pos == null) {
            return null;
        }
        return MachineItemAccess.getContainer(world, pos.getX(), pos.getY(), pos.getZ());
    }

    private void bindItemGrids(UIEventBuilder uiEventBuilder) {
        bindDrag(uiEventBuilder, "#InputGrid", ACTION_INPUT_DROP);
        bindDrag(uiEventBuilder, "#OutputGrid", ACTION_OUTPUT_DROP);
        bindDrag(uiEventBuilder, "#PlayerInventoryGrid", ACTION_INVENTORY_DROP);
        bindClick(uiEventBuilder, "#InputGrid", ACTION_INPUT_CLICK);
        bindClick(uiEventBuilder, "#OutputGrid", ACTION_OUTPUT_CLICK);
        bindClick(uiEventBuilder, "#PlayerInventoryGrid", ACTION_INVENTORY_CLICK);
    }

    private void bindDrag(UIEventBuilder uiEventBuilder, String selector, String action) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClickPressWhileDragging,
                selector,
                EventData.of("Action", ACTION_DRAG_START));
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Dropped,
                selector,
                EventData.of("Action", action));
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClickReleaseWhileDragging,
                selector,
                EventData.of("Action", action));
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotMouseDragCompleted,
                selector,
                EventData.of("Action", action));
    }

    private void bindClick(UIEventBuilder uiEventBuilder, String selector, String action) {
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.SlotClicking,
                selector,
                EventData.of("Action", action));
    }

    private boolean isDragAction(String action) {
        if (action == null) {
            return false;
        }
        return ACTION_INPUT_DROP.equalsIgnoreCase(action)
                || ACTION_OUTPUT_DROP.equalsIgnoreCase(action)
                || ACTION_INVENTORY_DROP.equalsIgnoreCase(action);
    }

    private void handleDrag(Store<EntityStore> store, OreCrusherUiEvent data) {
        boolean updateSent = false;
        try {
            if (store == null || data == null) {
                return;
            }
            // Debug: surface drag payload in chat to trace slot/index issues.
            debugDragEvent(store, data);
            GridType targetGrid = resolveTargetGrid(data.getAction());
            if (targetGrid == null) {
                return;
            }
            World world = getWorld(store);
            if (world == null) {
                return;
            }
            Vector3i pos = resolveBlockPosition(world);
            ItemContainer machineContainer = pos == null
                    ? null
                    : MachineItemAccess.getContainer(world, pos.getX(), pos.getY(), pos.getZ());
            Inventory inventory = getPlayerInventoryFull(store);

            String sourceSectionId = firstNonEmpty(
                    data.getSourceInventorySectionId(),
                    data.getDragSourceInventorySectionId());
            Integer sourceGridIndex = sourceSectionId == null
                    ? null
                    : firstNonNull(data.getSourceItemGridIndex(), data.getDragSourceItemGridIndex());
            GridType sourceGrid = resolveSourceGrid(sourceSectionId, sourceGridIndex);
            if (sourceGrid == null) {
                sourceGrid = inferSourceGrid(data, targetGrid, inventory, machineContainer);
            }
            Integer sourceSlotId = firstNonNull(data.getSourceSlotId(), data.getDragSourceSlotId());
            DragSnapshot recentSource = getRecentDragSource();
            if (recentSource != null) {
                boolean useRecent = sourceGrid == null || sourceSlotId == null;
                if (!useRecent && sourceGrid == GridType.PLAYER) {
                    SlotRef ref = resolvePlayerSlot(inventory, sourceSlotId);
                    ItemStack stack = ref == null ? null : ref.container.getItemStack(ref.slot);
                    String eventItemId = resolveEventItemId(data);
                    boolean mismatch = stack == null
                            || ItemStack.isEmpty(stack)
                            || (eventItemId != null && !eventItemId.equals(stack.getItemId()));
                    if (mismatch) {
                        useRecent = true;
                    }
                }
                if (useRecent) {
                    sourceGrid = recentSource.grid;
                    sourceSlotId = recentSource.slotId;
                }
            }
            if (sourceGrid == null) {
                String itemId = resolveEventItemId(data);
                if (itemId != null
                        && findMatchingPlayerSlot(inventory, itemId, data.getItemStackQuantity()) != null) {
                    sourceGrid = GridType.PLAYER;
                } else {
                    return;
                }
            }
            String eventItemId = resolveEventItemId(data);
            if (targetGrid == GridType.PLAYER
                    && sourceGrid == GridType.INPUT
                    && eventItemId != null
                    && eventItemId.equals(lastInputDropItemId)
                    && (System.currentTimeMillis() - lastInputDropMs) < INPUT_DROP_SUPPRESS_MS) {
                System.out.println("[Machinarium] DBG Drag suppress inventory drop for itemId=" + eventItemId);
                return;
            }
            sourceGrid = reconcileSourceGrid(sourceGrid, data, inventory, machineContainer);
            if (sourceGrid == GridType.OUTPUT && sourceSlotId != null && sourceSlotId >= OUTPUT_SLOT_COUNT) {
                sourceGrid = GridType.PLAYER;
            }

            Integer targetSlotIndex = data.getSlotIndex();
            if (targetSlotIndex == null && targetGrid == GridType.INPUT) {
                targetSlotIndex = 0;
            }
            if (targetSlotIndex == null) {
                return;
            }
            if (sourceGrid == targetGrid && sourceSlotId != null && sourceSlotId.equals(targetSlotIndex)) {
                return;
            }

            if (targetGrid == GridType.INPUT && sourceGrid == GridType.OUTPUT) {
                ItemStack outputAtIndex = machineContainer == null
                        ? null
                        : getOutputStack(machineContainer, sourceSlotId);
                String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
                if (outputAtIndex == null
                        || ItemStack.isEmpty(outputAtIndex)
                        || (itemId != null && !itemId.equals(outputAtIndex.getItemId()))
                        || itemId == null) {
                    sourceGrid = GridType.PLAYER;
                }
            }

            if (!canMoveBetween(sourceGrid, targetGrid)) {
                return;
            }

            if (targetGrid == GridType.INPUT) {
                // Input drops should always treat the source as player inventory,
                // even if UI index mappings are inconsistent.
                sourceGrid = GridType.PLAYER;
                sourceSlotId = null;
            }
            System.out.println("[Machinarium] DBG Drag v2 state target=" + targetGrid
                    + " source=" + sourceGrid
                    + " sourceSlotId=" + sourceSlotId
                    + " itemId=" + eventItemId);
            PlayerIndexMode playerIndexMode =
                    sourceGrid == GridType.PLAYER
                            ? resolvePlayerIndexMode(inventory, sourceSlotId, eventItemId)
                            : PlayerIndexMode.COMBINED;
            SlotRef sourceRef = null;
            if (sourceGrid == GridType.PLAYER) {
                sourceRef = resolvePlayerSourceRef(inventory, sourceSlotId, data, playerIndexMode);
                if (sourceRef != null && sourceSlotId == null) {
                    sourceSlotId = resolvePlayerGridIndex(inventory, sourceRef);
                }
            } else if (sourceSlotId != null) {
                sourceRef = resolveSlotRef(sourceGrid, sourceSlotId, inventory, machineContainer, PlayerIndexMode.COMBINED);
            }
            SlotRef targetRef = resolveSlotRef(targetGrid, targetSlotIndex, inventory, machineContainer, playerIndexMode);
            if (sourceRef == null || targetRef == null) {
                if (targetGrid == GridType.INPUT) {
                    System.out.println("[Machinarium] DBG Drag input sourceRef/targetRef null"
                            + " sourceRef=" + (sourceRef == null ? "null" : "ok")
                            + " targetRef=" + (targetRef == null ? "null" : "ok")
                            + " itemId=" + eventItemId);
                }
                return;
            }

            ItemStack sourceStack = sourceRef.container.getItemStack(sourceRef.slot);
            if ((sourceStack == null || ItemStack.isEmpty(sourceStack)) && sourceGrid == GridType.OUTPUT) {
                SlotRef playerFallback = resolvePlayerSlot(inventory, sourceSlotId);
                if (playerFallback != null) {
                    ItemStack fallbackStack = playerFallback.container.getItemStack(playerFallback.slot);
                    if (fallbackStack != null && !ItemStack.isEmpty(fallbackStack)) {
                        sourceGrid = GridType.PLAYER;
                        sourceRef = playerFallback;
                        sourceStack = fallbackStack;
                    }
                }
            }
            if (sourceGrid == GridType.PLAYER) {
                boolean missingStack = sourceStack == null || ItemStack.isEmpty(sourceStack);
                boolean mismatch = !missingStack && eventItemId != null
                        && !eventItemId.equals(sourceStack.getItemId());
                if (missingStack || mismatch) {
                    Integer desiredQty = data.getItemStackQuantity();
                    SlotRef fallback = findMatchingPlayerSlot(inventory, eventItemId, desiredQty);
                    if (fallback == null && desiredQty != null) {
                        fallback = findMatchingPlayerSlot(inventory, eventItemId, null);
                    }
                    if (fallback != null) {
                        sourceRef = fallback;
                        sourceStack = fallback.container.getItemStack(fallback.slot);
                    }
                }
            }
            if (sourceStack == null || ItemStack.isEmpty(sourceStack)) {
                if (targetGrid == GridType.INPUT) {
                    System.out.println("[Machinarium] DBG Drag input sourceStack empty itemId=" + eventItemId);
                }
                return;
            }

            int quantity = sourceStack.getQuantity();
            Integer eventQty = data.getItemStackQuantity();
            if (eventQty != null && eventQty > 0 && eventQty < quantity) {
                quantity = eventQty;
            }

            if (sourceSlotId != null
                    && isDuplicateDrag(sourceGrid, sourceSlotId, targetGrid, targetSlotIndex, sourceStack, quantity)) {
                return;
            }

            MoveTransaction<?> move = sourceRef.container.moveItemStackFromSlotToSlot(
                    sourceRef.slot,
                    quantity,
                    targetRef.container,
                    targetRef.slot);
            if (move != null && move.succeeded()) {
                if (targetGrid == GridType.INPUT) {
                    ItemStack targetAfter = targetRef.container.getItemStack(targetRef.slot);
                    boolean ok = stackMatches(targetAfter, eventItemId);
                    System.out.println("[Machinarium] DBG Drag input move succeeded ok=" + ok
                            + " itemId=" + eventItemId);
                    lastInputDropMs = System.currentTimeMillis();
                    lastInputDropItemId = eventItemId;
                    if (!ok) {
                        System.out.println("[Machinarium] DBG Drag input move mismatch, trying manual add itemId="
                                + sourceStack.getItemId());
                        if (tryManualInputTransfer(sourceRef, targetRef, sourceStack, quantity)) {
                            updateSent = refreshSlots(store);
                            return;
                        }
                    }
                }
                updateSent = refreshSlots(store);
                return;
            }
            if (targetGrid == GridType.INPUT && sourceGrid == GridType.PLAYER) {
                System.out.println("[Machinarium] DBG Drag input move failed, trying manual add itemId="
                        + sourceStack.getItemId());
                if (tryManualInputTransfer(sourceRef, targetRef, sourceStack, quantity)) {
                    lastInputDropMs = System.currentTimeMillis();
                    lastInputDropItemId = eventItemId;
                    updateSent = refreshSlots(store);
                    return;
                }
            }

            ItemStack targetStack = targetRef.container.getItemStack(targetRef.slot);
            if (targetStack != null && !ItemStack.isEmpty(targetStack)
                    && canSwapBetween(sourceGrid, targetGrid)) {
                ListTransaction<MoveTransaction<SlotTransaction>> swap =
                        sourceRef.container.swapItems(
                                sourceRef.slot,
                                targetRef.container,
                                targetRef.slot,
                                (short) 1);
                if (swap != null && swap.succeeded()) {
                    updateSent = refreshSlots(store);
                }
            }
        } finally {
            if (!updateSent) {
                sendUpdate(new UICommandBuilder());
            }
        }
    }

    private void debugDragEvent(Store<EntityStore> store, OreCrusherUiEvent data) {
        String msg = "DBG Drag action=" + data.getAction()
                + " target=" + data.getTarget()
                + " slotIndex=" + data.getSlotIndex()
                + " srcSlot=" + data.getSourceSlotId()
                + " srcGridIdx=" + data.getSourceItemGridIndex()
                + " srcSection=" + data.getSourceInventorySectionId()
                + " dragSrcSlot=" + data.getDragSourceSlotId()
                + " dragSrcGridIdx=" + data.getDragSourceItemGridIndex()
                + " dragSrcSection=" + data.getDragSourceInventorySectionId()
                + " itemId=" + data.getItemStackId()
                + " dragItemId=" + data.getDragItemStackId()
                + " qty=" + data.getItemStackQuantity();
        System.out.println("[Machinarium] v2 " + msg);
    }

    private boolean refreshSlots(Store<EntityStore> store) {
        UICommandBuilder update = new UICommandBuilder();
        if (updateSlots(update)) {
            sendUpdate(update);
            return true;
        }
        return false;
    }

    private boolean tryManualInputTransfer(
            SlotRef sourceRef,
            SlotRef targetRef,
            ItemStack sourceStack,
            int quantity) {
        if (sourceRef == null || targetRef == null || sourceStack == null || ItemStack.isEmpty(sourceStack)) {
            return false;
        }
        ItemStack moveStack = new ItemStack(
                sourceStack.getItemId(),
                quantity,
                sourceStack.getMetadata());
        if (!targetRef.container.canAddItemStackToSlot(targetRef.slot, moveStack, false, false)) {
            System.out.println("[Machinarium] DBG Drag input add rejected for itemId=" + moveStack.getItemId());
            return false;
        }
        ItemStackSlotTransaction addTx = targetRef.container.addItemStackToSlot(targetRef.slot, moveStack);
        if (addTx == null || !addTx.succeeded()) {
            System.out.println("[Machinarium] DBG Drag input add failed for itemId=" + moveStack.getItemId());
            return false;
        }
        ItemStackSlotTransaction removeTx = sourceRef.container.removeItemStackFromSlot(sourceRef.slot, quantity);
        if (removeTx == null || !removeTx.succeeded()) {
            System.out.println("[Machinarium] DBG Drag input remove failed for itemId=" + moveStack.getItemId());
            // rollback best-effort
            targetRef.container.removeItemStackFromSlot(targetRef.slot, moveStack, quantity, false, false);
            return false;
        }
        System.out.println("[Machinarium] DBG Drag input manual transfer ok itemId=" + moveStack.getItemId());
        return true;
    }

    private boolean isDuplicateDrag(
            GridType sourceGrid,
            int sourceSlot,
            GridType targetGrid,
            int targetSlot,
            ItemStack stack,
            int quantity) {
        String key = sourceGrid
                + ":"
                + sourceSlot
                + "->"
                + targetGrid
                + ":"
                + targetSlot
                + ":"
                + (stack == null ? "" : stack.getItemId())
                + ":"
                + quantity;
        long now = System.currentTimeMillis();
        if (key.equals(lastDragKey) && now - lastDragMs < DRAG_DEDUP_WINDOW_MS) {
            return true;
        }
        lastDragKey = key;
        lastDragMs = now;
        return false;
    }

    private boolean canMoveBetween(GridType source, GridType target) {
        if (target == GridType.OUTPUT && source != GridType.OUTPUT) {
            return false;
        }
        if (target == GridType.INPUT && source == GridType.OUTPUT) {
            return false;
        }
        return true;
    }

    private boolean canSwapBetween(GridType source, GridType target) {
        if (source == GridType.OUTPUT && target != GridType.OUTPUT) {
            return false;
        }
        if (target == GridType.OUTPUT && source != GridType.OUTPUT) {
            return false;
        }
        return true;
    }

    private GridType resolveTargetGrid(String action) {
        if (action == null) {
            return null;
        }
        if (ACTION_INPUT_DROP.equalsIgnoreCase(action)) {
            return GridType.INPUT;
        }
        if (ACTION_OUTPUT_DROP.equalsIgnoreCase(action)) {
            return GridType.OUTPUT;
        }
        if (ACTION_INVENTORY_DROP.equalsIgnoreCase(action)) {
            return GridType.PLAYER;
        }
        return null;
    }

    private GridType resolveSourceGrid(String sectionId, Integer gridIndex) {
        if (sectionId == null) {
            return null;
        }
        GridType fromIndex = gridIndex == null ? null : resolveGridIndex(gridIndex);
        if (fromIndex != null) {
            return fromIndex;
        }
        String normalized = sectionId.replace("#", "").trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("input")) {
            return GridType.INPUT;
        }
        if (normalized.contains("output")) {
            return GridType.OUTPUT;
        }
        if (normalized.contains("inventory") || normalized.contains("player") || normalized.contains("hotbar")) {
            return GridType.PLAYER;
        }
        return null;
    }

    private GridType reconcileSourceGrid(
            GridType sourceGrid,
            OreCrusherUiEvent data,
            Inventory inventory,
            ItemContainer machineContainer) {
        if (sourceGrid == null || data == null || machineContainer == null) {
            return sourceGrid;
        }
        if (sourceGrid == GridType.PLAYER) {
            return sourceGrid;
        }
        String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
        if (itemId == null) {
            return sourceGrid;
        }

        if (sourceGrid == GridType.OUTPUT) {
            Integer sourceSlotId = firstNonNull(data.getSourceSlotId(), data.getDragSourceSlotId());
            if (sourceSlotId != null && sourceSlotId >= 0 && sourceSlotId < OUTPUT_SLOT_COUNT) {
                ItemStack outputAtSlot = getOutputStack(machineContainer, sourceSlotId);
                if (!stackMatches(outputAtSlot, itemId)) {
                    return GridType.PLAYER;
                }
            } else if (!matchesAnyOutput(machineContainer, itemId)) {
                return GridType.PLAYER;
            }
        }

        ItemStack input = machineContainer.getCapacity() > INPUT_SLOT
                ? machineContainer.getItemStack(INPUT_SLOT)
                : null;
        boolean matchesInput = stackMatches(input, itemId);
        boolean matchesOutput = matchesAnyOutput(machineContainer, itemId);

        if (matchesOutput && !matchesInput) {
            return GridType.OUTPUT;
        }
        if (matchesInput && !matchesOutput) {
            return GridType.INPUT;
        }

        return sourceGrid;
    }

    private boolean matchesAnyOutput(ItemContainer container, String itemId) {
        if (container == null || itemId == null) {
            return false;
        }
        if (container.getCapacity() <= OUTPUT_SLOT_START) {
            return false;
        }
        for (short slot = OUTPUT_SLOT_START; slot <= OUTPUT_SLOT_END && slot < container.getCapacity(); slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stackMatches(stack, itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean stackMatches(ItemStack stack, String itemId) {
        if (stack == null || ItemStack.isEmpty(stack) || itemId == null) {
            return false;
        }
        return itemId.equals(stack.getItemId());
    }

    private GridType resolveGridFromTarget(String target) {
        if (target == null) {
            return null;
        }
        String normalized = target.replace("#", "").trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("inputgrid")) {
            return GridType.INPUT;
        }
        if (normalized.contains("outputgrid")) {
            return GridType.OUTPUT;
        }
        if (normalized.contains("playerinventorygrid") || normalized.contains("inventory")) {
            return GridType.PLAYER;
        }
        return null;
    }

    private void captureDragSource(OreCrusherUiEvent data) {
        if (data == null) {
            return;
        }
        GridType grid = resolveSourceGrid(data.getDragSourceInventorySectionId(), data.getDragSourceItemGridIndex());
        if (grid == null) {
            grid = resolveSourceGrid(data.getSourceInventorySectionId(), data.getSourceItemGridIndex());
        }
        if (grid == null) {
            grid = resolveGridFromTarget(data.getTarget());
        }

        Integer slotId = firstNonNull(
                data.getDragSourceSlotId(),
                data.getSourceSlotId(),
                data.getSlotIndex());
        if (grid == null || slotId == null) {
            return;
        }

        String itemId = firstNonEmpty(data.getDragItemStackId(), data.getItemStackId());
        Integer quantity = firstNonNull(data.getDragItemStackQuantity(), data.getItemStackQuantity());
        lastDragSource = new DragSnapshot(
                grid,
                slotId,
                itemId,
                quantity == null ? 0 : quantity,
                System.currentTimeMillis());
    }

    private void captureClickSource(GridType grid, OreCrusherUiEvent data) {
        if (data == null || grid == null) {
            return;
        }
        Integer slotId = data.getSlotIndex();
        if (slotId == null) {
            return;
        }
        String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
        Integer quantity = firstNonNull(data.getItemStackQuantity(), data.getDragItemStackQuantity());
        lastDragSource = new DragSnapshot(
                grid,
                slotId,
                itemId,
                quantity == null ? 0 : quantity,
                System.currentTimeMillis());
    }

    private DragSnapshot getRecentDragSource() {
        if (lastDragSource == null) {
            return null;
        }
        long age = System.currentTimeMillis() - lastDragSource.timestampMs;
        if (age > DRAG_SOURCE_WINDOW_MS) {
            return null;
        }
        return lastDragSource;
    }

    private boolean dragMatchesSnapshot(OreCrusherUiEvent data, DragSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
        if (itemId == null || snapshot.itemId == null) {
            return true;
        }
        return itemId.equals(snapshot.itemId);
    }

    private static Integer firstNonNull(Integer first, Integer second, Integer third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private static Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private static String firstNonEmpty(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String resolveEventItemId(OreCrusherUiEvent data) {
        if (data == null) {
            return null;
        }
        String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
        if (itemId == null && lastDragSource != null) {
            itemId = lastDragSource.itemId;
        }
        return itemId;
    }

    private SlotRef resolvePlayerSourceRef(
            Inventory inventory,
            Integer sourceSlotId,
            OreCrusherUiEvent data,
            PlayerIndexMode playerIndexMode) {
        String itemId = resolveEventItemId(data);
        Integer desiredQty = data == null ? null : data.getItemStackQuantity();

        if (sourceSlotId != null) {
            SlotRef ref = resolvePlayerSlotByMode(inventory, sourceSlotId, playerIndexMode);
            if (ref != null) {
                ItemStack stack = ref.container.getItemStack(ref.slot);
                if (stack != null && !ItemStack.isEmpty(stack)
                        && (itemId == null || itemId.equals(stack.getItemId()))) {
                    return ref;
                }
            }
            if (itemId != null) {
                SlotRef storageRef = resolvePlayerSlotStorageOnly(inventory, sourceSlotId);
                if (storageRef != null) {
                    ItemStack stack = storageRef.container.getItemStack(storageRef.slot);
                    if (stack != null && !ItemStack.isEmpty(stack)
                            && itemId.equals(stack.getItemId())) {
                        return storageRef;
                    }
                }
                SlotRef hotbarRef = resolvePlayerSlotHotbarOnly(inventory, sourceSlotId);
                if (hotbarRef != null) {
                    ItemStack stack = hotbarRef.container.getItemStack(hotbarRef.slot);
                    if (stack != null && !ItemStack.isEmpty(stack)
                            && itemId.equals(stack.getItemId())) {
                        return hotbarRef;
                    }
                }
            }
        }

        if (lastDragSource != null && lastDragSource.grid == GridType.PLAYER) {
            SlotRef recent = resolvePlayerSlotByMode(inventory, lastDragSource.slotId, playerIndexMode);
            if (recent != null) {
                ItemStack stack = recent.container.getItemStack(recent.slot);
                if (stack != null && !ItemStack.isEmpty(stack)
                        && (itemId == null || itemId.equals(stack.getItemId()))) {
                    return recent;
                }
            }
        }

        if (itemId != null) {
            SlotRef match = findMatchingPlayerSlot(inventory, itemId, desiredQty);
            if (match == null && desiredQty != null) {
                match = findMatchingPlayerSlot(inventory, itemId, null);
            }
            return match;
        }

        return null;
    }

    private SlotRef findMatchingPlayerSlot(Inventory inventory, String itemId, Integer desiredQty) {
        if (inventory == null || itemId == null) {
            return null;
        }
        int qty = desiredQty == null || desiredQty <= 0 ? 1 : desiredQty;
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null) {
            SlotRef ref = findMatchingSlotInContainer(hotbar, itemId, qty);
            if (ref != null) {
                return ref;
            }
        }
        ItemContainer storage = inventory.getStorage();
        if (storage != null) {
            return findMatchingSlotInContainer(storage, itemId, qty);
        }
        return null;
    }

    private SlotRef findMatchingSlotInContainer(ItemContainer container, String itemId, int minQty) {
        if (container == null || itemId == null) {
            return null;
        }
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || ItemStack.isEmpty(stack)) {
                continue;
            }
            if (itemId.equals(stack.getItemId()) && stack.getQuantity() >= minQty) {
                return new SlotRef(container, slot);
            }
        }
        return null;
    }

    private GridType inferSourceGrid(
            OreCrusherUiEvent data,
            GridType targetGrid,
            Inventory inventory,
            ItemContainer machineContainer) {
        if (data == null) {
            return null;
        }
        Integer sourceSlotId = firstNonNull(data.getSourceSlotId(), data.getDragSourceSlotId());
        if (sourceSlotId == null) {
            return null;
        }

        if (targetGrid != GridType.PLAYER) {
            return GridType.PLAYER;
        }

        String itemId = firstNonEmpty(data.getItemStackId(), data.getDragItemStackId());
        if (itemId != null && inventory != null) {
            SlotRef playerSlot = resolvePlayerSlot(inventory, sourceSlotId);
            if (playerSlot != null) {
                ItemStack playerStack = playerSlot.container.getItemStack(playerSlot.slot);
                if (playerStack != null && !ItemStack.isEmpty(playerStack)
                        && itemId.equals(playerStack.getItemId())) {
                    return GridType.PLAYER;
                }
            }
        }

        if (itemId != null && machineContainer != null) {
            ItemStack input = machineContainer.getItemStack(INPUT_SLOT);
            if (input != null && !ItemStack.isEmpty(input) && itemId.equals(input.getItemId())) {
                return GridType.INPUT;
            }
            if (matchesAnyOutput(machineContainer, itemId)) {
                return GridType.OUTPUT;
            }
        }

        return GridType.PLAYER;
    }

    private GridType resolveGridIndex(int gridIndex) {
        if (gridIndex == GRID_INDEX_INPUT) {
            return GridType.INPUT;
        }
        if (gridIndex == GRID_INDEX_OUTPUT) {
            return GridType.OUTPUT;
        }
        if (gridIndex == GRID_INDEX_PLAYER) {
            return GridType.PLAYER;
        }
        return null;
    }

    private SlotRef resolveSlotRef(
            GridType gridType,
            int slotIndex,
            Inventory inventory,
            ItemContainer machineContainer,
            PlayerIndexMode playerIndexMode) {
        if (gridType == GridType.INPUT) {
            return machineContainer == null ? null : new SlotRef(machineContainer, INPUT_SLOT);
        }
        if (gridType == GridType.OUTPUT) {
            if (machineContainer == null) {
                return null;
            }
            if (slotIndex < 0 || slotIndex >= OUTPUT_SLOT_COUNT) {
                return null;
            }
            short outputSlot = (short) (OUTPUT_SLOT_START + slotIndex);
            if (outputSlot < OUTPUT_SLOT_START || outputSlot > OUTPUT_SLOT_END) {
                return null;
            }
            return new SlotRef(machineContainer, outputSlot);
        }
        if (gridType == GridType.PLAYER) {
            return resolvePlayerSlotByMode(inventory, slotIndex, playerIndexMode);
        }
        return null;
    }

    private SlotRef resolvePlayerSlot(Inventory inventory, int gridIndex) {
        if (gridIndex < 0) {
            return null;
        }
        if (inventory == null) {
            return null;
        }
        ItemContainer hotbar = inventory.getHotbar();
        ItemContainer storage = inventory.getStorage();
        int hotbarCapacity = hotbar == null ? 0 : hotbar.getCapacity();
        if (hotbar != null && gridIndex < hotbarCapacity) {
            return new SlotRef(hotbar, (short) gridIndex);
        }
        int storageIndex = gridIndex - hotbarCapacity;
        if (storage != null && storageIndex >= 0 && storageIndex < storage.getCapacity()) {
            return new SlotRef(storage, (short) storageIndex);
        }
        return null;
    }

    private SlotRef resolvePlayerSlotHotbarOnly(Inventory inventory, int gridIndex) {
        if (gridIndex < 0 || inventory == null) {
            return null;
        }
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar != null && gridIndex < hotbar.getCapacity()) {
            return new SlotRef(hotbar, (short) gridIndex);
        }
        return null;
    }

    private SlotRef resolvePlayerSlotStorageOnly(Inventory inventory, int gridIndex) {
        if (gridIndex < 0 || inventory == null) {
            return null;
        }
        ItemContainer storage = inventory.getStorage();
        if (storage != null && gridIndex < storage.getCapacity()) {
            return new SlotRef(storage, (short) gridIndex);
        }
        return null;
    }

    private SlotRef resolvePlayerSlotByMode(
            Inventory inventory,
            int gridIndex,
            PlayerIndexMode mode) {
        if (mode == PlayerIndexMode.STORAGE_ONLY) {
            return resolvePlayerSlotStorageOnly(inventory, gridIndex);
        }
        if (mode == PlayerIndexMode.HOTBAR_ONLY) {
            return resolvePlayerSlotHotbarOnly(inventory, gridIndex);
        }
        SlotRef ref = resolvePlayerSlot(inventory, gridIndex);
        if (ref == null) {
            ref = resolvePlayerSlotStorageOnly(inventory, gridIndex);
        }
        if (ref == null) {
            ref = resolvePlayerSlotHotbarOnly(inventory, gridIndex);
        }
        return ref;
    }

    private PlayerIndexMode resolvePlayerIndexMode(
            Inventory inventory,
            Integer slotIndex,
            String itemId) {
        if (inventory == null || slotIndex == null || itemId == null) {
            return PlayerIndexMode.COMBINED;
        }
        SlotRef combined = resolvePlayerSlot(inventory, slotIndex);
        if (stackMatches(slotRefStack(combined), itemId)) {
            return PlayerIndexMode.COMBINED;
        }
        SlotRef storage = resolvePlayerSlotStorageOnly(inventory, slotIndex);
        if (stackMatches(slotRefStack(storage), itemId)) {
            return PlayerIndexMode.STORAGE_ONLY;
        }
        SlotRef hotbar = resolvePlayerSlotHotbarOnly(inventory, slotIndex);
        if (stackMatches(slotRefStack(hotbar), itemId)) {
            return PlayerIndexMode.HOTBAR_ONLY;
        }
        return PlayerIndexMode.COMBINED;
    }

    private ItemStack slotRefStack(SlotRef ref) {
        if (ref == null || ref.container == null) {
            return null;
        }
        return ref.container.getItemStack(ref.slot);
    }

    private Integer resolvePlayerGridIndex(Inventory inventory, SlotRef slotRef) {
        if (inventory == null || slotRef == null || slotRef.container == null) {
            return null;
        }
        ItemContainer hotbar = inventory.getHotbar();
        ItemContainer storage = inventory.getStorage();
        if (hotbar != null && slotRef.container == hotbar) {
            return (int) slotRef.slot;
        }
        int hotbarCapacity = hotbar == null ? 0 : hotbar.getCapacity();
        if (storage != null && slotRef.container == storage) {
            return hotbarCapacity + slotRef.slot;
        }
        return null;
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

    private enum GridType {
        INPUT,
        OUTPUT,
        PLAYER
    }

    private enum PlayerIndexMode {
        COMBINED,
        STORAGE_ONLY,
        HOTBAR_ONLY
    }

    private static final class DragSnapshot {
        private final GridType grid;
        private final int slotId;
        private final String itemId;
        private final int quantity;
        private final long timestampMs;

        private DragSnapshot(GridType grid, int slotId, String itemId, int quantity, long timestampMs) {
            this.grid = grid;
            this.slotId = slotId;
            this.itemId = itemId;
            this.quantity = quantity;
            this.timestampMs = timestampMs;
        }
    }

    private static final class SlotRef {
        private final ItemContainer container;
        private final short slot;

        private SlotRef(ItemContainer container, short slot) {
            this.container = container;
            this.slot = slot;
        }
    }
}
