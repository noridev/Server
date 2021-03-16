package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;

public class BlockBehaviorStairsWood extends BlockBehaviorStairs {

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WOOD_BLOCK_COLOR;
    }
}
