package com.example.plugin.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class SideToggleEvent {
    public static final BuilderCodec<SideToggleEvent> CODEC =
            BuilderCodec.<SideToggleEvent>builder(SideToggleEvent.class, SideToggleEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .addField(new KeyedCodec<>("Side", new StringCodec()),
                            (event, value) -> event.side = value,
                            event -> event.side)
                    .addField(new KeyedCodec<>("Index", new StringCodec()),
                            (event, value) -> event.index = value,
                            event -> event.index)
                    .addField(new KeyedCodec<>("@SearchQuery", new StringCodec()),
                            (event, value) -> event.searchQuery = value,
                            event -> event.searchQuery)
                    .addField(new KeyedCodec<>("@Value", new StringCodec()),
                            (event, value) -> event.value = value,
                            event -> event.value)
                    .build();

    private String action;
    private String side;
    private String index;
    private String searchQuery;
    private String value;

    public String getAction() {
        return action;
    }

    public String getSide() {
        return side;
    }

    public String getIndex() {
        return index;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getValue() {
        return value;
    }
}
