package com.example.plugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;
import org.bson.BsonString;
import java.util.Arrays;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class MachinariumCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> actionArg =
            withRequiredArg("action", "Akce: give", ArgTypes.STRING);
    private final OptionalArg<String> itemArg =
            withOptionalArg(
                    "item",
                    "ID itemu (solar/battery/cable/energy_cable/item_cable/thin_cable_black/"
                            + "thin_cable_brown/thin_cable_blue/thin_cable_green/furnace/workbench/ore_crusher)",
                    ArgTypes.STRING);
    private final OptionalArg<Integer> countArg =
            withOptionalArg("count", "Mnozstvi (default 1)", ArgTypes.INTEGER);

    public MachinariumCommand() {
        super("hyprotech", "HyProTech prikazy", false);
        addAliases("hpt");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        String action = actionArg.get(commandContext);
        if (!"give".equalsIgnoreCase(action)) {
            player.sendMessage(Message.raw(
                    "Pouziti: /hyprotech give [item] [count] nebo /hpt give --item=... --count=..."));
            return;
        }

        int count = 1;
        if (countArg.provided(commandContext)) {
            count = Math.max(1, countArg.get(commandContext));
        }

        Inventory inventory = player.getInventory();
        ItemContainer container = inventory.getStorage();

        String itemId = null;
        if (itemArg.provided(commandContext)) {
            itemId = resolveItemId(itemArg.get(commandContext));
        } else {
            String[] positional = getPositionalArgs(commandContext, action);
            if (positional.length > 0) {
                itemId = resolveItemId(positional[0]);
            }
            if (positional.length > 1 && !countArg.provided(commandContext)) {
                count = parseCount(positional[1], count);
            }
        }

        if (itemId != null) {
            if (giveItem(container, itemId, count, player)) {
                player.sendMessage(Message.raw("Pridano " + count + "x " + itemId));
            }
            return;
        }

        giveItem(container, MachinariumIds.BLOCK_SOLAR_PANEL, count, player);
        giveItem(container, MachinariumIds.BLOCK_ENERGY_CABLE, count, player);
        giveItem(container, MachinariumIds.BLOCK_ITEM_CABLE, count, player);
        giveItem(container, MachinariumIds.BLOCK_THIN_CABLE_BLACK, count, player);
        giveItem(container, MachinariumIds.BLOCK_THIN_CABLE_BROWN, count, player);
        giveItem(container, MachinariumIds.BLOCK_THIN_CABLE_BLUE, count, player);
        giveItem(container, MachinariumIds.BLOCK_THIN_CABLE_GREEN, count, player);
        giveItem(container, MachinariumIds.BLOCK_BATTERY, count, player);
        giveItem(container, MachinariumIds.BLOCK_ELECTRIC_FURNACE, count, player);
        giveItem(container, MachinariumIds.BLOCK_ELECTRICAL_WORKBENCH, count, player);
        giveItem(container, MachinariumIds.BLOCK_ORE_CRUSHER, count, player);
        player.sendMessage(Message.raw("Pridany HyProTech bloky do inventare."));
    }

    private String resolveItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        String normalized = itemId.trim().toLowerCase();
        switch (normalized) {
            case "solar":
            case "solar_panel":
                return MachinariumIds.BLOCK_SOLAR_PANEL;
            case "battery":
                return MachinariumIds.BLOCK_BATTERY;
            case "cable":
            case "energy_cable":
            case "kabel":
                return MachinariumIds.BLOCK_ENERGY_CABLE;
            case "item_cable":
            case "itemcable":
            case "kabel_itemu":
                return MachinariumIds.BLOCK_ITEM_CABLE;
            case "thin_cable_black":
                return MachinariumIds.BLOCK_THIN_CABLE_BLACK;
            case "thin_cable_brown":
                return MachinariumIds.BLOCK_THIN_CABLE_BROWN;
            case "thin_cable_blue":
                return MachinariumIds.BLOCK_THIN_CABLE_BLUE;
            case "thin_cable_green":
                return MachinariumIds.BLOCK_THIN_CABLE_GREEN;
            case "furnace":
            case "electric_furnace":
                return MachinariumIds.BLOCK_ELECTRIC_FURNACE;
            case "workbench":
            case "electrical_workbench":
                return MachinariumIds.BLOCK_ELECTRICAL_WORKBENCH;
            case "ore_crusher":
            case "orecrusher":
            case "crusher":
                return MachinariumIds.BLOCK_ORE_CRUSHER;
            default:
                return itemId;
        }
    }

    private String[] getPositionalArgs(CommandContext commandContext, String action) {
        String input = commandContext.getInputString();
        if (input == null) {
            return new String[0];
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 0) {
            return new String[0];
        }
        if (tokens[0].startsWith("/")) {
            tokens[0] = tokens[0].substring(1);
        }
        int index = 0;
        if (tokens[0].equalsIgnoreCase(getName()) || getAliases().contains(tokens[0].toLowerCase())) {
            index++;
        }
        if (index < tokens.length && tokens[index].equalsIgnoreCase(action)) {
            index++;
        }
        if (index >= tokens.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(tokens, index, tokens.length);
    }

    private int parseCount(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean giveItem(ItemContainer container, String itemId, int count, Player player) {
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || item == Item.UNKNOWN) {
            player.sendMessage(Message.raw("Neznamy item: " + itemId));
            return false;
        }

        ItemStack stack = new ItemStack(itemId, count);
        if (TieredIdUtil.isTieredId(itemId, MachinariumIds.BLOCK_ELECTRIC_FURNACE)) {
            BsonDocument blockState = new BsonDocument("Type", new BsonString("processingBench"));
            BsonDocument metadata = new BsonDocument("BlockState", blockState);
            stack = stack.withMetadata(metadata);
        }

        container.addItemStack(stack);
        return true;
    }
}
