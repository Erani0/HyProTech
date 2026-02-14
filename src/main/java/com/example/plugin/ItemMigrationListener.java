package com.example.plugin;

import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public class ItemMigrationListener {

    private static Map<String, String> replace_map = new HashMap<>();

    // Заполнение данными (обычно делается в статическом блоке)
    static {
        replace_map.put("Machinarium_Metal_Rod", "Machinarium_Rod_Iron");
        replace_map.put("Machinarium_Metal_Plate", "Machinarium_Plate_Iron");
        replace_map.put("Machinarium_Chromium_Ingot", "Machinarium_Ingot_Chromium");
        replace_map.put("Machinarium_Tin_Ingot", "Machinarium_Ingot_Tin");
        replace_map.put("Machinarium_Aluminum_Ingot", "Machinarium_Ingot_Aluminum");
        replace_map.put("Machinarium_Plutonium_Ingot", "Machinarium_Ingot_Plutonium");
        replace_map.put("Machinarium_Tungsten_Ingot", "Machinarium_Ingot_Tungsten");
        replace_map.put("Machinarium_Nickel_Ingot", "Machinarium_Ingot_Nickel");
        replace_map.put("Machinarium_Lithium_Ingot", "Machinarium_Ingot_Lithium");
        replace_map.put("Machinarium_Manganese_Ingot", "Machinarium_Ingot_Manganese");
    }

    
/*

machinarium_adamantite_powder
machinarium_high_tier_alloy_blend
machinarium_gears
machinarium_alloy_constantan
machinarium_alloy_electrum
machinarium_alloy_invar
machinarium_alloy_steel
machinarium_dense_composite_plate
machinarium_composite_alloy_ingot
machinarium_metal_plate_press
machinarium_titanium_plate
machinarium_titanium_ingot
machinarium_uranium_ingot
machinarium_vanadium_ingot
machinarium_reinforced_gears
machinarium_superalloy_blend
machinarium_reinforced_plate
machinarium_reinforced_rod
machinarium_titanium_vanadium_alloy
machinarium_tungsten_plate
machinarium_metal_rod
machinarium_metal_plate */

    public static void onPlayerJoin(PlayerConnectEvent event) {
/*         if (event.isCancelled()) {
            return;
        } */
        // Получаем инвентарь игрока
        ItemContainer inventory = event.getPlayer().getInventory().getStorage();
        
        //String NEW_ID = "";
        // Проходим по всем слотам
        for (short i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItemStack(i);
            
            // Если слот не пуст и ID совпадает со старым
            if (stack != null /* && .equals(OLD_ID) */) {
                String OLD_ID = stack.getItemId();
                if( replace_map.containsKey( OLD_ID ) == true ) {
                    // Создаем новый стак с тем же количеством и метаданными
                    ItemStack newStack = new ItemStack(
                        replace_map.get( OLD_ID ), 
                        stack.getQuantity(), 
                        stack.getMetadata()
                    );
                    
                    // Устанавливаем новый предмет в тот же слот
                    inventory.replaceItemStackInSlot(i, stack, newStack);
                }
            }
        }
    }
}
