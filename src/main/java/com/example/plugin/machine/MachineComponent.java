package com.example.plugin.machine;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class MachineComponent implements Component<ChunkStore> {
    private static final IntegerCodec INTEGER_CODEC = new IntegerCodec();
    private static final StringCodec STRING_CODEC = new StringCodec();
    private static final BooleanCodec BOOLEAN_CODEC = new BooleanCodec();
    private static final int DEFAULT_AREA_SIZE = 5;
    private static final int MIN_AREA_SIZE = 2;

    public static final BuilderCodec<MachineComponent> CODEC =
            BuilderCodec.<MachineComponent>builder(MachineComponent.class, MachineComponent::new)
                    .addField(new KeyedCodec<>("MachineId", STRING_CODEC),
                            (component, value) -> component.machineId = value == null ? "" : value,
                            component -> component.machineId)
                    .addField(new KeyedCodec<>("Tier", INTEGER_CODEC),
                            (component, value) -> component.tier = value == null ? 0 : value,
                            component -> component.tier)
                    .addField(new KeyedCodec<>("Progress", INTEGER_CODEC),
                            (component, value) -> component.progress = value == null ? 0 : value,
                            component -> component.progress)
                    .addField(new KeyedCodec<>("ProgressMax", INTEGER_CODEC),
                            (component, value) -> component.progressMax = value == null ? 0 : value,
                            component -> component.progressMax)
                    .addField(new KeyedCodec<>("Enabled", BOOLEAN_CODEC),
                            (component, value) -> component.enabled = value == null || value,
                            component -> component.enabled)
                    .addField(new KeyedCodec<>("AreaWidth", INTEGER_CODEC),
                            (component, value) -> component.areaWidth = clampArea(value),
                            component -> component.areaWidth)
                    .addField(new KeyedCodec<>("AreaDepth", INTEGER_CODEC),
                            (component, value) -> component.areaDepth = clampArea(value),
                            component -> component.areaDepth)
                    .addField(new KeyedCodec<>("AreaVisible", BOOLEAN_CODEC),
                            (component, value) -> component.areaVisible = value != null && value,
                            component -> component.areaVisible)
                    .build();

    private String machineId = "";
    private int tier;
    private int progress;
    private int progressMax;
    private boolean enabled = true;
    private int areaWidth = DEFAULT_AREA_SIZE;
    private int areaDepth = DEFAULT_AREA_SIZE;
    private boolean areaVisible;

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId == null ? "" : machineId;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(0, tier);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
    }

    public int getProgressMax() {
        return progressMax;
    }

    public void setProgressMax(int progressMax) {
        this.progressMax = Math.max(0, progressMax);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAreaWidth() {
        return areaWidth;
    }

    public void setAreaWidth(int areaWidth) {
        this.areaWidth = clampArea(areaWidth);
    }

    public int getAreaDepth() {
        return areaDepth;
    }

    public void setAreaDepth(int areaDepth) {
        this.areaDepth = clampArea(areaDepth);
    }

    public boolean isAreaVisible() {
        return areaVisible;
    }

    public void setAreaVisible(boolean areaVisible) {
        this.areaVisible = areaVisible;
    }

    @Override
    public Component<ChunkStore> clone() {
        MachineComponent copy = new MachineComponent();
        copy.machineId = machineId;
        copy.tier = tier;
        copy.progress = progress;
        copy.progressMax = progressMax;
        copy.enabled = enabled;
        copy.areaWidth = areaWidth;
        copy.areaDepth = areaDepth;
        copy.areaVisible = areaVisible;
        return copy;
    }

    @Override
    public Component<ChunkStore> cloneSerializable() {
        return clone();
    }

    private static int clampArea(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_AREA_SIZE;
        }
        if (value < MIN_AREA_SIZE) {
            return MIN_AREA_SIZE;
        }
        return value;
    }

}
