package com.example.plugin.changelog;

import com.example.plugin.ui.ChangelogPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class ChangelogManager {
    public static final String CHANGELOG_VERSION = "1.3.1";
    public static final String CHANGELOG_TEXT = """
## Changelog

### [1.3.1] (from 1.3.0)
- Added macOS support for **modlist** and **config**.
- Added automatic **config file generation** inside the `mods` folder.
- Completely reworked the **Quarry** feature due to bugs that (with border enabled) changed surrounding blocks and sometimes returned `null`.
- Updated Quarry setup:
  - Click **Give Border Torch** and place **2 torches**.
  - Torch #1 should be placed at the same height/width/length level, ideally right next to the Quarry.
  - Torch #2 should be placed at the opposite corner depending on the desired Quarry size.
  - Maximum Quarry size: **50x50**.

# 📌 Changelog BIG UPDATE!

## Version 1.3.1 *(from 1.2.8 and 1.3.0)*

IMPORTANT! IF THERE ARE MISSING/ERROR BLOCKS, THEY WILL NEED TO BE REBUILT. THERE HAVE BEEN CHANGES TO UI AND FUNCTIONALITY

---

## 🔓 Unlocked Recipes from Bronze

- **Weapons**
- **Armor**

---

## ✨ New Content

- Added the **Alloy Smelter** for advanced alloy production.
- Added the **Ore Crusher** for ore doubling through powder processing.
- Introduced **Ore Powders** as a new intermediate refining material.
- Added a **Bronze Ingot** crafting recipe.
- Implemented the **Wind Turbine** as a new renewable power source.

---

## ⛏️ New Ores Added

The following new ores are now available in world generation and processing:

- **Bauxite**
- **Cassiterite**
- **Chromite**
- **Ilmenite**
- **Manganese**
- **Pentlandite**
- **Quartzite**
- **Scheelite**
- **Spodumene**
- **Uraninite**
- **Vanadinite**

---

## 🔩 New Materials & Ingots

New ingots and processed materials obtained from ores:

- Bauxite → `Machinarium_Aluminum_Ingot`
- Cassiterite → `Machinarium_Tin_Ingot`
- Chromite → `Machinarium_Chromium_Ingot`
- Ilmenite → `Machinarium_Titanium_Ingot`
- Manganese → `Machinarium_Manganese_Ingot`
- Pentlandite → `Machinarium_Nickel_Ingot`
- Quartzite → `Machinarium_Silicon` *(raw silicon material, not an ingot)*
- Scheelite → `Machinarium_Tungsten_Ingot`
- Spodumene → `Machinarium_Lithium_Ingot`
- Uraninite → `Machinarium_Uranium_Ingot`
- Vanadinite → `Machinarium_Vanadium_Ingot`

---

## 🏭 New Alloys & Advanced Blends

Added multiple new alloy types for mid- and late-game progression:

- **Electrum Alloy**
- **Steel Alloy**
- **Invar Alloy**
- **Constantan Alloy**
- **High-Tier Alloy Blend**
- **Composite Alloy Ingot**
- **Superalloy Blend**
- **Titanium-Vanadium Alloy**

---

## ⚙️ New Core Crafting Components

Introduced additional fundamental crafting parts:

- Iron Gear
- Reinforced Gear
- Iron Plate
- Reinforced Plate
- Titanium Plate
- Tungsten Plate
- Iron Rod
- Reinforced Rod

---

## 🧱 New Items Added

New essential machine-building components:

- **Machine Frame**
- **Reinforced Machine Frame**
- **Circuit Board**
- **Spring**

---

## 🔋 New Machines & Storage

- Added the **Large Battery** for improved energy storage.
- Added the **Metal Press** machine for advanced component crafting.

---

## 🎨 Visual & UI Improvements

- Updated the **Electric Furnace** model.
- Improved general UI layout and usability.
- Enhanced **item filter UI** for better inventory management.
- Updated textures for:
  - Energy cables
  - Item transport cables

---

## 🐛 Bug Fixes

- Fixed an issue where **Solar Panels generated power even when covered/blocked**.

---

## 🧩 Components & Future Expansion

- Added a large set of new items and crafting components *(currently placeholders, to be expanded in upcoming updates)*.
- Early preparation work for upcoming:
  - **Nuclear Science**
  - **Heat Science**
""";
    public static final String CHANGELOG_ID =
            CHANGELOG_VERSION + ":" + Integer.toHexString(CHANGELOG_TEXT.hashCode());

    private final JavaPlugin plugin;
    private final Config<ChangelogConfig> config;
    private final Path configPath;
    private ChangelogConfig state = new ChangelogConfig();
    private boolean loaded;

    public ChangelogManager(JavaPlugin plugin, Config<ChangelogConfig> config, Path configPath) {
        this.plugin = plugin;
        this.config = config;
        this.configPath = configPath;
    }

    public void load() {
        if (loaded) {
            return;
        }
        boolean exists = configPath != null && Files.exists(configPath);
        if (config == null) {
            loaded = true;
            return;
        }
        try {
            ChangelogConfig loadedConfig = config.load().join();
            if (loadedConfig != null) {
                state = loadedConfig;
            }
        } catch (Exception ex) {
            if (plugin != null) {
                plugin.getLogger().atWarning().log(
                        "[HyProTech] Failed to load changelog state: %s", ex.getMessage());
            }
            state = new ChangelogConfig();
        }
        loaded = true;
        if (!exists) {
            save();
        }
    }

    public boolean shouldShow(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        ensureLoaded();
        String lastSeen = state.getLastSeen(uuid);
        return !CHANGELOG_ID.equalsIgnoreCase(lastSeen);
    }

    public void markSeen(UUID uuid) {
        if (uuid == null) {
            return;
        }
        ensureLoaded();
        state.markSeen(uuid, CHANGELOG_ID);
        save();
    }

    public boolean openForPlayer(Player player, PlayerRef playerRef, boolean force) {
        if (player == null || playerRef == null) {
            return false;
        }
        if (!force && !shouldShow(playerRef.getUuid())) {
            return false;
        }
        PageManager pageManager = player.getPageManager();
        if (pageManager == null) {
            return false;
        }
        if (pageManager.getCustomPage() != null) {
            return false;
        }
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null) {
            return false;
        }
        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return false;
        }
        pageManager.openCustomPage(entityRef, store, new ChangelogPage(playerRef, this));
        return true;
    }

    private void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private void save() {
        if (config == null) {
            return;
        }
        config.save().exceptionally(ex -> {
            if (plugin != null) {
                plugin.getLogger().atWarning().log(
                        "[HyProTech] Failed to save changelog state: %s", ex.getMessage());
            }
            return null;
        });
    }
}
