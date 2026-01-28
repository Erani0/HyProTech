package com.example.plugin.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class CableUpgradeEvent {
    public static final BuilderCodec<CableUpgradeEvent> CODEC =
            BuilderCodec.<CableUpgradeEvent>builder(CableUpgradeEvent.class, CableUpgradeEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .build();

    private String action;

    public String getAction() {
        return action;
    }
}
