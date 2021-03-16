package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.player.CloudPlayer;

public class BlockBehaviorHayBale extends BlockBehaviorSolid {


    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, CloudPlayer player) {
        return placeBlock(block, item.getBehavior().getBlock(item).withTrait(BlockTraits.AXIS, face.getAxis()));
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.YELLOW_BLOCK_COLOR;
    }
}
