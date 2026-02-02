package com.example.plugin.sound;

import com.example.plugin.energy.EnergyNodeComponent;
import com.example.plugin.machine.MachineComponent;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.common.OggVorbisInfoCache;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MachinariumSounds {
    public static final String EVENT_WIND_TURBINE = "Machinarium_Wind_Turbine";
    public static final String EVENT_SOLAR_PANEL = "Machinarium_Solar_Panel";
    public static final String EVENT_ORE_CRUSHER = "Machinarium_Ore_Crusher";
    public static final String EVENT_ELECTRIC_FURNACE = "Machinarium_Electric_Furnace";
    public static final String EVENT_ALLOY_SMELTER = "Machinarium_Alloy_Smelter";

    public static final String FILE_WIND_TURBINE = "Sounds/WindTurbine.ogg";
    public static final String FILE_SOLAR_PANEL = "Sounds/SolarPanel.ogg";
    public static final String FILE_ORE_CRUSHER = "Sounds/OreCrusher.ogg";
    public static final String FILE_ELECTRIC_FURNACE = "Sounds/ElectricFurnace.ogg";
    public static final String FILE_ALLOY_SMELTER = "Sounds/AlloySmelter.ogg";

    public static final long DEFAULT_LOOP_MS = 10000L;

    private static final Map<String, Integer> SOUND_INDEX_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LOOP_MS_CACHE = new ConcurrentHashMap<>();

    private MachinariumSounds() {
    }

    public static void tickLoop(
            World world,
            int x,
            int y,
            int z,
            String eventId,
            String soundFile,
            long fallbackMs,
            boolean working,
            MachineComponent machine) {
        if (machine == null) {
            return;
        }
        long next = machine.getNextSoundMs();
        long updated = tickLoopInternal(world, x, y, z, eventId, soundFile, fallbackMs, working, next);
        if (updated != next) {
            machine.setNextSoundMs(updated);
        }
    }

    public static void tickLoop(
            World world,
            int x,
            int y,
            int z,
            String eventId,
            String soundFile,
            long fallbackMs,
            boolean working,
            EnergyNodeComponent node) {
        if (node == null) {
            return;
        }
        long next = node.getNextSoundMs();
        long updated = tickLoopInternal(world, x, y, z, eventId, soundFile, fallbackMs, working, next);
        if (updated != next) {
            node.setNextSoundMs(updated);
        }
    }

    private static long tickLoopInternal(
            World world,
            int x,
            int y,
            int z,
            String eventId,
            String soundFile,
            long fallbackMs,
            boolean working,
            long nextSoundMs) {
        if (!working) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        if (nextSoundMs > 0L && now < nextSoundMs) {
            return nextSoundMs;
        }
        playSound(world, x, y, z, eventId);
        long loopMs = resolveLoopMs(soundFile, fallbackMs);
        return now + loopMs;
    }

    private static void playSound(World world, int x, int y, int z, String eventId) {
        if (world == null || eventId == null || eventId.isEmpty()) {
            return;
        }
        int index = resolveSoundIndex(eventId);
        if (index <= 0) {
            return;
        }
        if (world.getEntityStore() == null) {
            return;
        }
        SoundUtil.playSoundEvent3d(
                index,
                SoundCategory.SFX,
                x + 0.5,
                y + 0.5,
                z + 0.5,
                world.getEntityStore().getStore());
    }

    private static int resolveSoundIndex(String eventId) {
        Integer cached = SOUND_INDEX_CACHE.get(eventId);
        if (cached != null && cached > 0) {
            return cached;
        }
        int index = SoundEvent.getAssetMap().getIndex(eventId);
        if (index > 0) {
            SOUND_INDEX_CACHE.put(eventId, index);
        }
        return index;
    }

    private static long resolveLoopMs(String soundFile, long fallbackMs) {
        if (soundFile == null || soundFile.isEmpty()) {
            return fallbackMs;
        }
        return LOOP_MS_CACHE.computeIfAbsent(soundFile, file -> {
            OggVorbisInfoCache.OggVorbisInfo info = OggVorbisInfoCache.getNow(file);
            if (info != null && info.duration > 0d) {
                long ms = Math.round(info.duration * 1000d);
                return Math.max(250L, ms);
            }
            return fallbackMs;
        });
    }
}
