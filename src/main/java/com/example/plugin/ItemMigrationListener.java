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

    static {
        // Machines
        replace_map.put("Machinarium_Metal_Plate_Press",                  "Machinarium_Press");
        replace_map.put("Machinarium_Battery",                            "Machinarium_Battery_Rack_S");
        replace_map.put("Machinarium_Large_Battery",                      "Machinarium_Battery_Rack_L");
        // Rods
        replace_map.put("Machinarium_Metal_Rod",                          "Machinarium_Rod_Iron");
        replace_map.put("Machinarium_Reinforced_Rod",                     "Machinarium_Rod_Reinforced_Iron");
        // Plates
        replace_map.put("Machinarium_Metal_Plate",                        "Machinarium_Plate_Iron");
        replace_map.put("Machinarium_Reinforced_Plate",                   "Machinarium_Plate_Reinforced_Iron");
        replace_map.put("Machinarium_Tungsten_Plate",                     "Machinarium_Plate_Tungsten");
        replace_map.put("Machinarium_Titanium_Plate",                     "Machinarium_Plate_Titanium");
        replace_map.put("Machinarium_Dense_Composite_Plate",              "Machinarium_Plate_Dense_Composite");
        // Gears   
        replace_map.put("Machinarium_Gears",                              "Machinarium_Gears_Iron");
        replace_map.put("Machinarium_Reinforced_Gears",                   "Machinarium_Gears_Reinforced_Iron");
        // Ingots   
        replace_map.put("Machinarium_Chromium_Ingot",                     "Machinarium_Ingot_Chromium");
        replace_map.put("Machinarium_Tin_Ingot",                          "Machinarium_Ingot_Tin");
        replace_map.put("Machinarium_Aluminum_Ingot",                     "Machinarium_Ingot_Aluminum");
        replace_map.put("Machinarium_Plutonium_Ingot",                    "Machinarium_Ingot_Plutonium");
        replace_map.put("Machinarium_Tungsten_Ingot",                     "Machinarium_Ingot_Tungsten");
        replace_map.put("Machinarium_Nickel_Ingot",                       "Machinarium_Ingot_Nickel");
        replace_map.put("Machinarium_Lithium_Ingot",                      "Machinarium_Ingot_Lithium");
        replace_map.put("Machinarium_Manganese_Ingot",                    "Machinarium_Ingot_Manganese");
        replace_map.put("Machinarium_Titanium_Ingot",                     "Machinarium_Ingot_Titanium");
        replace_map.put("Machinarium_Uranium_Ingot",                      "Machinarium_Ingot_Uranium");
        replace_map.put("Machinarium_Vanadium_Ingot",                     "Machinarium_Ingot_Vanadium");
        replace_map.put("Machinarium_Bronze_Blend",                       "Machinarium_Ingot_Bronze");
        replace_map.put("Machinarium_Titanium_Vanadium_Alloy",            "Machinarium_Ingot_TiV");
        replace_map.put("Machinarium_Composite_Alloy_Ingot",              "Machinarium_Ingot_Composite_Alloy");
        replace_map.put("Machinarium_Alloy_Steel",                        "Machinarium_Ingot_Steel");
        replace_map.put("Machinarium_Alloy_Invar",                        "Machinarium_Ingot_Invar");
        replace_map.put("Machinarium_Alloy_Electrum",                     "Machinarium_Ingot_Electrum");
        replace_map.put("Machinarium_Alloy_Constantan",                   "Machinarium_Ingot_Constantan");
        replace_map.put("Machinarium_High_Tier_Alloy_Blend",              "Machinarium_Ingot_High_Tier_Alloy");
        replace_map.put("Machinarium_Silicon",                            "Machinarium_Ingot_Silicon");
        replace_map.put("Machinarium_Superalloy_Blend",                   "Machinarium_Ingot_Superalloy");
        // Powders
        replace_map.put("Machinarium_Silicon_Powder",                     "Machinarium_Powder_Silicon");
        replace_map.put("Machinarium_Adamantite_Powder",                  "Machinarium_Powder_Adamantite");
        replace_map.put("Machinarium_Silver_Powder",                      "Machinarium_Powder_Silver");
        replace_map.put("Machinarium_Stone_Dust",                         "Machinarium_Powder_Stone");
        replace_map.put("Machinarium_Iron_Powder",                        "Machinarium_Powder_Iron");
        replace_map.put("Machinarium_Thorium_Powder",                     "Machinarium_Powder_Thorium");
        replace_map.put("Machinarium_Tin_Powder",                         "Machinarium_Powder_Tin");
        replace_map.put("Machinarium_Titanium_Powder",                    "Machinarium_Powder_Titanium");
        replace_map.put("Machinarium_Tungsten_Powder",                    "Machinarium_Powder_Tungsten");
        replace_map.put("Machinarium_Uranium_Powder",                     "Machinarium_Powder_Uranium");
        replace_map.put("Machinarium_Vanadium_Powder",                    "Machinarium_Powder_Vanadium");
        replace_map.put("Machinarium_Nickel_Powder",                      "Machinarium_Powder_Nickel");
        replace_map.put("Machinarium_Gold_Powder",                        "Machinarium_Powder_Gold");
        replace_map.put("Machinarium_Manganese_Powder",                   "Machinarium_Powder_Manganese");
        replace_map.put("Machinarium_Copper_Powder",                      "Machinarium_Powder_Copper");
        replace_map.put("Machinarium_Lithium_Powder",                     "Machinarium_Powder_Lithium");
        replace_map.put("Machinarium_Cobalt_Powder",                      "Machinarium_Powder_Cobalt");
        replace_map.put("Machinarium_Chromium_Powder",                    "Machinarium_Powder_Chromium");
        replace_map.put("Machinarium_Aluminum_Powder",                    "Machinarium_Powder_Aluminum");
        replace_map.put("Machinarium_Alloy_Steel_Powder",                 "Machinarium_Powder_Steel");
        replace_map.put("Machinarium_Alloy_Constantan_Powder",            "Machinarium_Powder_Constantan");
        replace_map.put("Machinarium_Alloy_Electrum_Powder",              "Machinarium_Powder_Electrum");
        replace_map.put("Machinarium_Titanium_Vanadium_Alloy_Powder",     "Machinarium_Powder_TiV");
        replace_map.put("Machinarium_Composite_Alloy_Powder",             "Machinarium_Powder_Composite_Alloy");
        replace_map.put("Machinarium_Ingot_Invar_Powder",                 "Machinarium_Invar_Powder");
        replace_map.put("Machinarium_Superalloy_Blend_Powder",            "Machinarium_Powder_Superalloy");
        replace_map.put("Machinarium_High_Tier_Alloy_Blend_Powder",       "Machinarium_Powder_High_Tier_Alloy");
    }

    public static void onPlayerJoin(PlayerConnectEvent event) {
/*         if (event.isCancelled()) {
            return;
        } */
        // Получаем инвентарь игрока
        ItemContainer inventory = event.getPlayer().getInventory().getStorage();
        
        // Проходим по всем слотам
        for (short i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItemStack(i);
            
            // Если слот не пуст
            if (stack != null) {
                String OLD_ID = stack.getItemId();
                // Проверяем наличие устаревшего ID
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
