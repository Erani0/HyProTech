package com.example.plugin.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class SolarUpgradeEvent {
    public static final BuilderCodec<SolarUpgradeEvent> CODEC =
            BuilderCodec.<SolarUpgradeEvent>builder(SolarUpgradeEvent.class, SolarUpgradeEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .build();

    private String action;

    public String getAction() {
        return action;
    }
}
