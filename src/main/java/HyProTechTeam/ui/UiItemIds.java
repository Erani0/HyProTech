package HyProTechTeam.ui;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

final class UiItemIds {
    private static final String EMPTY_ITEM_ID = ItemStack.EMPTY.getItemId();

    private UiItemIds() {
    }

    static boolean isEmptyItemId(String itemId) {
        return itemId == null
                || itemId.isEmpty()
                || (EMPTY_ITEM_ID != null && EMPTY_ITEM_ID.equalsIgnoreCase(itemId));
    }

    static String safeItemId(String itemId) {
        return isEmptyItemId(itemId) ? "" : itemId;
    }

    static boolean isKnownItemId(String itemId) {
        if (isEmptyItemId(itemId)) {
            return false;
        }
        Item item = Item.getAssetMap().getAsset(itemId);
        return item != null && item != Item.UNKNOWN && !item.isState();
    }
}
