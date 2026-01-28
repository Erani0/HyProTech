package com.example.plugin.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface WindowProvider {
    Window[] createWindows(Ref<EntityStore> playerEntityRef, Store<EntityStore> store);
}
