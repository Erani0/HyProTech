package com.example.plugin.machine;

import com.example.plugin.MachinariumIds;
import com.example.plugin.TieredIdUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class QuarryAreaManager {
    private static final String BORDER_STATE_LINE = "Border_Line";
    private static final String BORDER_STATE_LINE_BACK = "Border_Line_Back";
    private static final String BORDER_STATE_LINE_BACK_FRONT = "Border_Line_BackFront";
    private static final String BORDER_STATE_LINE_BACK_UP = "Border_Line_BackUp";
    private static final String BORDER_STATE_LINE_SIDE = "Border_Line_Side";
    private static final String BORDER_STATE_LINE_SIDE_LEFT = "Border_Line_Side_Left";
    private static final String BORDER_STATE_LINE_SIDE_UP_LEFT = "Border_Line_Side_Up_Left";
    private static final String BORDER_STATE_LINE_SIDE_UP_RIGHT = "Border_Line_Side_Up_Right";
    private static final String BORDER_STATE_CORNER_LEFT_FRONT = "Border_Corner_LeftFront";
    private static final String BORDER_STATE_CORNER_LEFT_BACK = "Border_Corner_LeftBack";
    private static final String BORDER_STATE_CORNER_RIGHT_FRONT = "Border_Corner_RightFront";
    private static final String BORDER_STATE_CORNER_RIGHT_BACK = "Border_Corner_RightBack";
    private static final String BORDER_STATE_CORNER_MIDDLE_LEFT_FRONT = "Border_Corner_Middle_LeftFront";
    private static final String BORDER_STATE_CORNER_MIDDLE_LEFT_BACK = "Border_Corner_Middle_LeftBack";
    private static final String BORDER_STATE_CORNER_MIDDLE_RIGHT_FRONT = "Border_Corner_Middle_RightFront";
    private static final String BORDER_STATE_CORNER_MIDDLE_RIGHT_BACK = "Border_Corner_Middle_RightBack";
    private static final String BORDER_STATE_CORNER_UP = "Border_Corner_Up";
    private static final String BORDER_STATE_CORNER_UP_LEFT_FRONT = "Border_Corner_Up_LeftFront";
    private static final String BORDER_STATE_CORNER_UP_LEFT_BACK = "Border_Corner_Up_LeftBack";
    private static final String BORDER_STATE_CORNER_UP_RIGHT_FRONT = "Border_Corner_Up_RightFront";
    private static final String BORDER_STATE_CORNER_UP_RIGHT_BACK = "Border_Corner_Up_RightBack";
    private static final int BORDER_HEIGHT_BLOCKS = 3;
    private static final int BORDER_OFFSET_Y = -1;
    private static final int BORDER_OFFSET_Z = -2;
    private static final int BORDER_ROTATION_INDEX = RotationTuple.NONE_INDEX;
    private static final Map<World, Map<String, BlockSnapshot>> STORED_BLOCKS = new WeakHashMap<>();
    private static final Map<World, Map<String, BorderRecord>> BORDER_RECORDS = new WeakHashMap<>();
    private static final Map<World, Map<String, BorderRecord>> BORDER_RECORDS_BY_XZ = new WeakHashMap<>();

    private QuarryAreaManager() {
    }

    public static void showArea(World world, Vector3i origin, int width, int depth) {
        applyArea(world, origin, width, depth, true);
    }

    public static void hideArea(World world, Vector3i origin, int width, int depth) {
        applyArea(world, origin, width, depth, false);
    }

    public static void updateArea(
            World world,
            Vector3i origin,
            int oldWidth,
            int oldDepth,
            int newWidth,
            int newDepth) {
        if (world == null || origin == null) {
            return;
        }
        world.execute(() -> {
            applyAreaInternal(world, origin, oldWidth, oldDepth, false);
            applyAreaInternal(world, origin, newWidth, newDepth, true);
        });
    }

    private static void applyArea(World world, Vector3i origin, int width, int depth, boolean place) {
        if (world == null || origin == null) {
            return;
        }
        world.execute(() -> applyAreaInternal(world, origin, width, depth, place));
    }

    private static void applyAreaInternal(World world, Vector3i origin, int width, int depth, boolean place) {
        int w = Math.max(1, width);
        int d = Math.max(1, depth);
        int baseY = origin.getY() + 1 + BORDER_OFFSET_Y;
        if (baseY < ChunkUtil.MIN_Y || baseY >= ChunkUtil.HEIGHT) {
            return;
        }
        int topY = Math.min(baseY + BORDER_HEIGHT_BLOCKS - 1, ChunkUtil.HEIGHT - 1);

        if (!place) {
            clearBorderFromRecord(world, origin);
            clearBorderArea(world, origin, w, d, baseY, topY);
            return;
        }

        startBorderRecord(world, origin);
        Rotation yaw = getBlockYaw(world, origin);
        Bounds bounds = computeBounds(w, d);
        if (BORDER_OFFSET_Z != 0) {
            bounds = new Bounds(bounds.minX, bounds.maxX, bounds.minZ + BORDER_OFFSET_Z, bounds.maxZ + BORDER_OFFSET_Z);
        }
        Bounds stateBounds = rotateBounds(bounds, yaw);
        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            updateBorderStack(world, origin, stateBounds, x, bounds.minZ, baseY, topY, place, yaw);
            if (bounds.maxZ != bounds.minZ) {
                updateBorderStack(world, origin, stateBounds, x, bounds.maxZ, baseY, topY, place, yaw);
            }
        }
        for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
            if (z == bounds.minZ || z == bounds.maxZ) {
                continue;
            }
            updateBorderStack(world, origin, stateBounds, bounds.minX, z, baseY, topY, place, yaw);
            if (bounds.maxX != bounds.minX) {
                updateBorderStack(world, origin, stateBounds, bounds.maxX, z, baseY, topY, place, yaw);
            }
        }
    }

    private static void updateBorderStack(
            World world,
            Vector3i origin,
            Bounds stateBounds,
            int localX,
            int localZ,
            int baseY,
            int topY,
            boolean place,
            Rotation yaw) {
        Vector3i offset = rotateLocalOffset(localX, localZ, yaw);
        int worldX = origin.getX() + offset.getX();
        int worldZ = origin.getZ() + offset.getZ();
        int stateX = offset.getX();
        int stateZ = offset.getZ();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
        BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
        if (accessor == null) {
            return;
        }
        boolean hasWidth = stateBounds.minX != stateBounds.maxX;
        boolean hasDepth = stateBounds.minZ != stateBounds.maxZ;
        boolean isCorner = hasWidth && hasDepth
                && (stateX == stateBounds.minX || stateX == stateBounds.maxX)
                && (stateZ == stateBounds.minZ || stateZ == stateBounds.maxZ);
        if (place) {
            for (int y = baseY; y <= topY; y++) {
                String stateName;
                if (isCorner) {
                    if (y == baseY) {
                        stateName = cornerState(stateBounds, stateX, stateZ);
                    } else if (y == topY) {
                        stateName = cornerUpState(stateBounds, stateX, stateZ);
                    } else {
                        stateName = cornerMiddleState(stateBounds, stateX, stateZ);
                    }
                } else {
                    if (y != baseY && y != topY) {
                        continue;
                    }
                    boolean isTop = y == topY;
                    stateName = lineState(stateBounds, stateX, stateZ, hasWidth, hasDepth, isTop);
                }
                if (placeBorderBlock(world, accessor, worldX, y, worldZ, stateName)) {
                    recordBorderPosition(world, origin, worldX, y, worldZ);
                }
            }
            return;
        }

        for (int y = baseY; y <= topY; y++) {
            BlockType current = accessor.getBlockType(worldX, y, worldZ);
            if (isBorder(current)) {
                restoreOrClear(world, accessor, worldX, y, worldZ);
            }
        }
    }

    private static boolean placeBorderBlock(
            World world,
            BlockAccessor accessor,
            int x,
            int y,
            int z,
            String stateName) {
        BlockType current = accessor.getBlockType(x, y, z);
        if (isQuarry(current)) {
            return false;
        }
        if (!isBorder(current)) {
            storeSnapshot(world, x, y, z, current);
        }

        accessor.setBlock(x, y, z, MachinariumIds.BLOCK_QUARRY_BORDER, BORDER_ROTATION_INDEX);
        BlockType borderType = accessor.getBlockType(x, y, z);
        if (isBorder(borderType)) {
            accessor.setBlockInteractionState(x, y, z, borderType, stateName, false);
            return true;
        }
        return false;
    }

    private static String lineState(
            Bounds bounds,
            int x,
            int z,
            boolean hasWidth,
            boolean hasDepth,
            boolean isTop) {
        if (!hasWidth && hasDepth) {
            return isTop ? BORDER_STATE_LINE_SIDE_UP_RIGHT : BORDER_STATE_LINE_SIDE;
        }
        if (!hasDepth && hasWidth) {
            return isTop ? BORDER_STATE_LINE_BACK_FRONT : BORDER_STATE_LINE;
        }
        if (!hasWidth && !hasDepth) {
            return BORDER_STATE_LINE;
        }
        if (x == bounds.minX) {
            return isTop ? BORDER_STATE_LINE_SIDE_UP_LEFT : BORDER_STATE_LINE_SIDE_LEFT;
        }
        if (x == bounds.maxX) {
            return isTop ? BORDER_STATE_LINE_SIDE_UP_RIGHT : BORDER_STATE_LINE_SIDE;
        }
        if (z == bounds.maxZ) {
            return isTop ? BORDER_STATE_LINE_BACK_UP : BORDER_STATE_LINE_BACK;
        }
        return isTop ? BORDER_STATE_LINE_BACK_FRONT : BORDER_STATE_LINE;
    }

    private static String cornerState(Bounds bounds, int x, int z) {
        if (x == bounds.minX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_LEFT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_RIGHT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.maxZ) {
            return BORDER_STATE_CORNER_RIGHT_BACK;
        }
        return BORDER_STATE_CORNER_LEFT_BACK;
    }

    private static String cornerUpState(Bounds bounds, int x, int z) {
        if (x == bounds.minX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_UP_LEFT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_UP_RIGHT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.maxZ) {
            return BORDER_STATE_CORNER_UP_RIGHT_BACK;
        }
        return BORDER_STATE_CORNER_UP_LEFT_BACK;
    }

    private static String cornerMiddleState(Bounds bounds, int x, int z) {
        if (x == bounds.minX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_MIDDLE_LEFT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.minZ) {
            return BORDER_STATE_CORNER_MIDDLE_RIGHT_FRONT;
        }
        if (x == bounds.maxX && z == bounds.maxZ) {
            return BORDER_STATE_CORNER_MIDDLE_RIGHT_BACK;
        }
        return BORDER_STATE_CORNER_MIDDLE_LEFT_BACK;
    }

    private static boolean isEmpty(BlockType blockType) {
        if (blockType == null || blockType == BlockType.EMPTY) {
            return true;
        }
        String id = blockType.getId();
        return id == null || id.isEmpty() || BlockType.EMPTY_KEY.equals(id);
    }

    private static boolean isBorder(BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String id = blockType.getId();
        if (id == null) {
            return false;
        }
        String baseId = MachinariumIds.BLOCK_QUARRY_BORDER;
        return id.equalsIgnoreCase(baseId)
                || id.regionMatches(true, 0, baseId, 0, baseId.length())
                || containsIgnoreCase(id, baseId);
    }

    private static boolean isQuarry(BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String id = blockType.getId();
        return id != null && TieredIdUtil.isTieredId(id, MachinariumIds.BLOCK_QUARRY);
    }

    private static Rotation getBlockYaw(World world, Vector3i origin) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(origin.getX(), origin.getZ());
        if (world.getChunkIfLoaded(chunkIndex) == null) {
            return Rotation.None;
        }

        RotationTuple rotation = RotationTuple.get(
                world.getBlockRotationIndex(origin.getX(), origin.getY(), origin.getZ()));
        if (rotation == null || rotation.yaw() == null) {
            return Rotation.None;
        }

        return rotation.yaw();
    }

    private static Vector3i rotateLocalOffset(int localX, int localZ, Rotation yaw) {
        if (yaw == Rotation.Ninety) {
            return new Vector3i(localZ, 0, -localX);
        }
        if (yaw == Rotation.OneEighty) {
            return new Vector3i(-localX, 0, -localZ);
        }
        if (yaw == Rotation.TwoSeventy) {
            return new Vector3i(-localZ, 0, localX);
        }
        return new Vector3i(localX, 0, localZ);
    }

    private static Bounds rotateBounds(Bounds bounds, Rotation yaw) {
        if (yaw == Rotation.Ninety) {
            return new Bounds(
                    bounds.minZ,
                    bounds.maxZ,
                    -bounds.maxX,
                    -bounds.minX);
        }
        if (yaw == Rotation.OneEighty) {
            return new Bounds(
                    -bounds.maxX,
                    -bounds.minX,
                    -bounds.maxZ,
                    -bounds.minZ);
        }
        if (yaw == Rotation.TwoSeventy) {
            return new Bounds(
                    -bounds.maxZ,
                    -bounds.minZ,
                    bounds.minX,
                    bounds.maxX);
        }
        return bounds;
    }

    private static void clearBorderArea(World world, Vector3i origin, int width, int depth, int baseY, int topY) {
        Bounds bounds = computeBounds(width, depth);
        if (BORDER_OFFSET_Z != 0) {
            bounds = new Bounds(bounds.minX, bounds.maxX, bounds.minZ + BORDER_OFFSET_Z, bounds.maxZ + BORDER_OFFSET_Z);
        }
        int maxX = Math.max(Math.abs(bounds.minX), Math.abs(bounds.maxX));
        int maxZ = Math.max(Math.abs(bounds.minZ), Math.abs(bounds.maxZ));
        int radius = Math.max(maxX, maxZ) + Math.abs(BORDER_OFFSET_Z) + 2;
        int minX = origin.getX() - radius;
        int maxXWorld = origin.getX() + radius;
        int minZ = origin.getZ() - radius;
        int maxZWorld = origin.getZ() + radius;
        Map<String, BlockSnapshot> snapshots = getSnapshotMap(world);

        for (int x = minX; x <= maxXWorld; x++) {
            for (int z = minZ; z <= maxZWorld; z++) {
                long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
                if (accessor == null) {
                    continue;
                }
                for (int y = baseY; y <= topY; y++) {
                    String key = blockKey(x, y, z);
                    BlockType current = accessor.getBlockType(x, y, z);
                    if (isBorder(current)) {
                        restoreOrClear(world, accessor, x, y, z);
                    } else if (snapshots.containsKey(key)) {
                        snapshots.remove(key);
                    }
                }
            }
        }
    }

    private static boolean clearBorderFromRecord(World world, Vector3i origin) {
        BorderRecord record = consumeBorderRecord(world, origin);
        if (record == null) {
            return false;
        }
        Map<String, BlockSnapshot> snapshots = getSnapshotMap(world);
        for (Vector3i pos : record.positions.values()) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.getX(), pos.getZ());
            BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
            if (accessor == null) {
                continue;
            }
            BlockType current = accessor.getBlockType(pos.getX(), pos.getY(), pos.getZ());
            if (isBorder(current)) {
                restoreOrClear(world, accessor, pos.getX(), pos.getY(), pos.getZ());
            } else {
                snapshots.remove(blockKey(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        return true;
    }

    private static void storeSnapshot(World world, int x, int y, int z, BlockType current) {
        if (isEmpty(current)) {
            return;
        }
        Map<String, BlockSnapshot> snapshots = getSnapshotMap(world);
        String key = blockKey(x, y, z);
        if (snapshots.containsKey(key)) {
            return;
        }
        int rotationIndex = world.getBlockRotationIndex(x, y, z);
        snapshots.put(key, new BlockSnapshot(current.getId(), rotationIndex));
    }

    private static void restoreOrClear(World world, BlockAccessor accessor, int x, int y, int z) {
        Map<String, BlockSnapshot> snapshots = getSnapshotMap(world);
        String key = blockKey(x, y, z);
        BlockSnapshot snapshot = snapshots.remove(key);
        if (snapshot != null && snapshot.blockId != null && !snapshot.blockId.isEmpty()) {
            setBlockWithRotation(accessor, x, y, z, snapshot.blockId, snapshot.rotationIndex);
        } else {
            accessor.setBlock(x, y, z, BlockType.EMPTY);
        }
    }

    private static Map<String, BlockSnapshot> getSnapshotMap(World world) {
        Map<String, BlockSnapshot> snapshots = STORED_BLOCKS.get(world);
        if (snapshots == null) {
            snapshots = new HashMap<>();
            STORED_BLOCKS.put(world, snapshots);
        }
        return snapshots;
    }

    private static void setBlockWithRotation(
            BlockAccessor accessor,
            int x,
            int y,
            int z,
            String blockId,
            int rotationIndex) {
        if (accessor == null || blockId == null || blockId.isEmpty()) {
            return;
        }
        int blockIndex = BlockType.getAssetMap().getIndex(blockId);
        if (blockIndex == Integer.MIN_VALUE) {
            return;
        }
        BlockType blockType = BlockType.getAssetMap().getAsset(blockIndex);
        if (blockType == null) {
            return;
        }
        accessor.setBlock(x, y, z, blockIndex, blockType, rotationIndex, 0, 0);
    }

    private static void startBorderRecord(World world, Vector3i origin) {
        Map<String, BorderRecord> records = getBorderRecordMap(world);
        BorderRecord record = new BorderRecord();
        String key = originKey(origin);
        records.put(key, record);
        getBorderRecordMapByXZ(world).put(originKeyXZ(origin), record);
    }

    private static void recordBorderPosition(World world, Vector3i origin, int x, int y, int z) {
        BorderRecord record = getBorderRecord(world, origin);
        if (record == null) {
            return;
        }
        record.positions.put(blockKey(x, y, z), new Vector3i(x, y, z));
    }

    private static BorderRecord consumeBorderRecord(World world, Vector3i origin) {
        Map<String, BorderRecord> records = getBorderRecordMap(world);
        BorderRecord record = records.remove(originKey(origin));
        if (record != null) {
            getBorderRecordMapByXZ(world).remove(originKeyXZ(origin));
            return record;
        }
        return consumeBorderRecordByXZ(world, origin);
    }

    private static BorderRecord consumeBorderRecordByXZ(World world, Vector3i origin) {
        Map<String, BorderRecord> records = getBorderRecordMapByXZ(world);
        BorderRecord record = records.remove(originKeyXZ(origin));
        if (record == null) {
            return null;
        }
        return record;
    }

    private static BorderRecord getBorderRecord(World world, Vector3i origin) {
        Map<String, BorderRecord> records = getBorderRecordMap(world);
        return records.get(originKey(origin));
    }

    private static Map<String, BorderRecord> getBorderRecordMap(World world) {
        Map<String, BorderRecord> records = BORDER_RECORDS.get(world);
        if (records == null) {
            records = new HashMap<>();
            BORDER_RECORDS.put(world, records);
        }
        return records;
    }

    private static Map<String, BorderRecord> getBorderRecordMapByXZ(World world) {
        Map<String, BorderRecord> records = BORDER_RECORDS_BY_XZ.get(world);
        if (records == null) {
            records = new HashMap<>();
            BORDER_RECORDS_BY_XZ.put(world, records);
        }
        return records;
    }

    private static String blockKey(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static String originKey(Vector3i origin) {
        return origin.getX() + ":" + origin.getY() + ":" + origin.getZ();
    }

    private static String originKeyXZ(Vector3i origin) {
        return origin.getX() + ":" + origin.getZ();
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        if (value == null || needle == null || needle.isEmpty()) {
            return false;
        }
        int limit = value.length() - needle.length();
        for (int i = 0; i <= limit; i++) {
            if (value.regionMatches(true, i, needle, 0, needle.length())) {
                return true;
            }
        }
        return false;
    }

    private static Bounds computeBounds(int width, int depth) {
        int halfWidth = width / 2;
        int minX = -halfWidth;
        int maxX = width - 1 - halfWidth;
        int maxZ = 0;
        int minZ = maxZ - (Math.max(1, depth) - 1);
        return new Bounds(minX, maxX, minZ, maxZ);
    }

    private static final class Bounds {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private Bounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    private static final class BlockSnapshot {
        private final String blockId;
        private final int rotationIndex;

        private BlockSnapshot(String blockId, int rotationIndex) {
            this.blockId = blockId;
            this.rotationIndex = rotationIndex;
        }
    }

    private static final class BorderRecord {
        private final Map<String, Vector3i> positions = new HashMap<>();
    }
}
