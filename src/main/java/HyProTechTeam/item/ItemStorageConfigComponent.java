package HyProTechTeam.item;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ItemStorageConfigComponent implements Component<ChunkStore> {
    private static final IntegerCodec INTEGER_CODEC = new IntegerCodec();
    private static final StringCodec STRING_CODEC = new StringCodec();

    public static final BuilderCodec<ItemStorageConfigComponent> CODEC =
            BuilderCodec.<ItemStorageConfigComponent>builder(
                            ItemStorageConfigComponent.class,
                            ItemStorageConfigComponent::new)
                    .addField(new KeyedCodec<>("Name", STRING_CODEC),
                            (component, value) -> component.name = normalizeName(value),
                            component -> component.name)
                    .addField(new KeyedCodec<>("Priority", INTEGER_CODEC),
                            (component, value) -> component.priority = ItemNodeComponent.clampPriority(
                                    value == null ? ItemNodeComponent.DEFAULT_PRIORITY : value),
                            component -> component.priority)
                    .build();

    private String name = "";
    private int priority = ItemNodeComponent.DEFAULT_PRIORITY;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeName(name);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = ItemNodeComponent.clampPriority(priority);
    }

    @Override
    public Component<ChunkStore> clone() {
        ItemStorageConfigComponent copy = new ItemStorageConfigComponent();
        copy.name = name;
        copy.priority = priority;
        return copy;
    }

    @Override
    public Component<ChunkStore> cloneSerializable() {
        return clone();
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
