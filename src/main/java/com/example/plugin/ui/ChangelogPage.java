package com.example.plugin.ui;

import com.example.plugin.changelog.ChangelogManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEventType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChangelogPage extends InteractiveCustomUIPage<ChangelogEvent> {
    private static final String PAGE_LAYOUT = "Machinarium_Changelog.ui";
    private static final String ACTION_CLOSE = "Close";

    private final ChangelogManager changelogManager;

    public ChangelogPage(PlayerRef playerRef, ChangelogManager changelogManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, ChangelogEvent.CODEC);
        this.changelogManager = changelogManager;
    }

    @Override
    public void build(
            Ref<EntityStore> playerRef,
            UICommandBuilder uiCommandBuilder,
            UIEventBuilder uiEventBuilder,
            Store<EntityStore> store) {
        uiCommandBuilder.append(PAGE_LAYOUT);
        uiCommandBuilder.set("#ChangelogText.Value", ChangelogManager.CHANGELOG_TEXT);
        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CloseButton",
                EventData.of("Action", ACTION_CLOSE));
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerRef, Store<EntityStore> store, ChangelogEvent data) {
        if (data == null || data.getAction() == null) {
            return;
        }
        if (!ACTION_CLOSE.equalsIgnoreCase(data.getAction())) {
            return;
        }
        dismiss(playerRef, store);
    }

    @Override
    public void onDismiss(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (changelogManager != null) {
            changelogManager.markSeen(getPlayerUuid());
        }
        super.onDismiss(playerRef, store);
    }

    private void dismiss(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (playerRef == null || store == null) {
            return;
        }
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        PageManager pageManager = player.getPageManager();
        if (pageManager == null) {
            return;
        }
        try {
            pageManager.handleEvent(
                    playerRef,
                    store,
                    new CustomPageEvent(CustomPageEventType.Dismiss, ""));
        } catch (Throwable ignored) {
        }
    }

    private java.util.UUID getPlayerUuid() {
        return playerRef == null ? null : playerRef.getUuid();
    }
}
