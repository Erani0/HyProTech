package HyProTechTeam.interaction;

import HyProTechTeam.MachinariumComponents;
import HyProTechTeam.energy.EnergyNodeComponent;
import HyProTechTeam.energy.EnergySide;
import HyProTechTeam.energy.EnergySideMode;
import HyProTechTeam.item.ItemMode;
import HyProTechTeam.item.ItemNodeComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockFace;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CableSideToolInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<CableSideToolInteraction> CODEC =
            BuilderCodec.builder(
                            CableSideToolInteraction.class,
                            CableSideToolInteraction::new,
                            SimpleBlockInteraction.CODEC)
                    .documentation("Toggles Machinarium cable side between input and output.")
                    .build();
    private static final boolean DEBUG = true;

    public CableSideToolInteraction() {
        super();
    }

    @Override
    protected void interactWithBlock(
            World world,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionType interactionType,
            InteractionContext context,
            ItemStack itemStack,
            Vector3i blockPos,
            CooldownHandler cooldownHandler) {
        if (world == null || context == null || blockPos == null) {
            debug(commandBuffer, context, "CableTool: invalid context.");
            return;
        }

        EnergySide side = resolveSide(context);
        if (side == null) {
            debug(commandBuffer, context, "CableTool: missing side (no face/normal).");
            return;
        }

        int y = blockPos.getY();
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.HEIGHT) {
            debug(commandBuffer, context, "CableTool: Y out of bounds.");
            return;
        }

        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPos.getX(), blockPos.getZ());
        BlockAccessor accessor = world.getChunkIfLoaded(chunkIndex);
        if (accessor == null) {
            debug(commandBuffer, context, "CableTool: chunk not loaded.");
            return;
        }

        BlockType blockType = accessor.getBlockType(blockPos.getX(), y, blockPos.getZ());
        String blockId = blockType == null ? "unknown" : blockType.getId();

        if (MachinariumComponents.ENERGY == null || MachinariumComponents.ITEM == null) {
            debug(commandBuffer, context, "CableTool: component types not initialized.");
            return;
        }

        ChunkStore chunkStore = world.getChunkStore();
        BlockComponentChunk blockComponents =
                chunkStore.getChunkComponent(chunkIndex, BlockComponentChunk.getComponentType());
        if (blockComponents == null) {
            debug(commandBuffer, context, "CableTool: missing BlockComponentChunk.");
            return;
        }

        int localX = ChunkUtil.localCoordinate((long) blockPos.getX());
        int localZ = ChunkUtil.localCoordinate((long) blockPos.getZ());
        int blockIndex = ChunkUtil.indexBlockInColumn(localX, y, localZ);

        boolean updated = false;
        ItemNodeComponent itemNode = blockComponents.getComponent(blockIndex, MachinariumComponents.ITEM);
        if (itemNode != null) {
            toggleItemSide(itemNode, side);
            updated = true;
        } else {
            EnergyNodeComponent energyNode = blockComponents.getComponent(blockIndex, MachinariumComponents.ENERGY);
            if (energyNode != null && energyNode.getNodeType() == EnergyNodeComponent.NodeType.CABLE) {
                toggleEnergySide(energyNode, side);
                updated = true;
            }
        }

        if (updated) {
            blockComponents.markNeedsSaving();
            debug(commandBuffer, context, "CableTool: toggled " + side.label() + " on " + blockId + ".");
        } else {
            debug(commandBuffer, context, "CableTool: not a Machinarium cable (" + blockId + ").");
        }
    }

    @Override
    protected void simulateInteractWithBlock(
            InteractionType interactionType,
            InteractionContext context,
            ItemStack itemStack,
            World world,
            Vector3i blockPos) {
        // Server-side only.
    }

    private EnergySide resolveSide(InteractionContext context) {
        EnergySide side = resolveSideFromState(context.getState());
        if (side != null) {
            return side;
        }
        return resolveSideFromState(context.getClientState());
    }

    private EnergySide resolveSideFromState(InteractionSyncData state) {
        if (state == null) {
            return null;
        }
        BlockFace face = state.blockFace;
        EnergySide fromFace = resolveSideFromFace(face);
        if (fromFace != null) {
            return fromFace;
        }
        return resolveSideFromNormal(state.raycastNormal);
    }

    private EnergySide resolveSideFromFace(BlockFace face) {
        if (face == null || face == BlockFace.None) {
            return null;
        }

        switch (face) {
            case East:
                return EnergySide.EAST;
            case West:
                return EnergySide.WEST;
            case Up:
                return EnergySide.UP;
            case Down:
                return EnergySide.DOWN;
            case South:
                return EnergySide.SOUTH;
            case North:
                return EnergySide.NORTH;
            default:
                return null;
        }
    }

    private EnergySide resolveSideFromNormal(Vector3f normal) {
        if (normal == null) {
            return null;
        }

        float ax = Math.abs(normal.x);
        float ay = Math.abs(normal.y);
        float az = Math.abs(normal.z);

        if (ax >= ay && ax >= az) {
            return normal.x >= 0f ? EnergySide.EAST : EnergySide.WEST;
        }
        if (ay >= ax && ay >= az) {
            return normal.y >= 0f ? EnergySide.UP : EnergySide.DOWN;
        }
        return normal.z >= 0f ? EnergySide.SOUTH : EnergySide.NORTH;
    }

    private void debug(CommandBuffer<EntityStore> commandBuffer, InteractionContext context, String message) {
        if (!DEBUG || commandBuffer == null || context == null) {
            return;
        }
        if (context.getEntity() == null) {
            return;
        }
        Player player = commandBuffer.getComponent(context.getEntity(), Player.getComponentType());
        if (player == null) {
            return;
        }
        player.sendMessage(Message.raw(message));
    }

    private void toggleEnergySide(EnergyNodeComponent node, EnergySide side) {
        EnergySideMode mode = node.getSideMode(side);
        if (mode == EnergySideMode.OUTPUT) {
            node.setSideMode(side, EnergySideMode.INPUT);
        } else {
            node.setSideMode(side, EnergySideMode.OUTPUT);
        }
    }

    private void toggleItemSide(ItemNodeComponent node, EnergySide side) {
        ItemMode mode = node.getSideMode(side);
        if (mode == ItemMode.PUT) {
            node.setSideMode(side, ItemMode.TAKE);
        } else {
            node.setSideMode(side, ItemMode.PUT);
        }
    }
}
