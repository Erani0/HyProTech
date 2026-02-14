package HyProTechTeam.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class ItemNetworkPriorityEvent {
    public static final BuilderCodec<ItemNetworkPriorityEvent> CODEC =
            BuilderCodec.<ItemNetworkPriorityEvent>builder(
                            ItemNetworkPriorityEvent.class,
                            ItemNetworkPriorityEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .addField(new KeyedCodec<>("Index", new StringCodec()),
                            (event, value) -> event.index = value,
                            event -> event.index)
                    .addField(new KeyedCodec<>("@Value", new StringCodec()),
                            (event, value) -> event.value = value,
                            event -> event.value)
                    .build();

    private String action;
    private String index;
    private String value;

    public String getAction() {
        return action;
    }

    public String getIndex() {
        return index;
    }

    public String getValue() {
        return value;
    }
}
