package HyProTechTeam.ui;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;

public class WindUpgradeEvent {
    public static final BuilderCodec<WindUpgradeEvent> CODEC =
            BuilderCodec.<WindUpgradeEvent>builder(WindUpgradeEvent.class, WindUpgradeEvent::new)
                    .addField(new KeyedCodec<>("Action", new StringCodec()),
                            (event, value) -> event.action = value,
                            event -> event.action)
                    .build();

    private String action;

    public String getAction() {
        return action;
    }
}
