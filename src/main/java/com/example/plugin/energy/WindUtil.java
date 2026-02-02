package com.example.plugin.energy;

import com.hypixel.hytale.server.core.universe.world.World;

public final class WindUtil {
    private static final int SKY_CHECK_OFFSET_BLOCKS = 4;

    private WindUtil() {
    }

    public static double getWindFactor(World world, int worldX, int worldY, int worldZ) {
        if (world == null) {
            return 0.0;
        }
        int checkY = worldY + SKY_CHECK_OFFSET_BLOCKS;
        if (!SunlightUtil.hasSkyAccess(world, worldX, checkY, worldZ)) {
            return 0.0;
        }
        return 1.0;
    }
}
