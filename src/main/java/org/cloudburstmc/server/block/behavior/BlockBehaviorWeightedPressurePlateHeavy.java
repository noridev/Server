package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.math.NukkitMath;

public class BlockBehaviorWeightedPressurePlateHeavy extends BlockBehaviorPressurePlateBase {

    public BlockBehaviorWeightedPressurePlateHeavy() {
        this.onPitch = 0.90000004f;
        this.offPitch = 0.75f;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.IRON_BLOCK_COLOR;
    }

    @Override
    protected int computeRedstoneStrength(Block block) {
        int count = Math.min(block.getLevel().getCollidingEntities(getCollisionBoxes(block)).size(), this.getMaxWeight());

        if (count > 0) {
            float f = (float) Math.min(this.getMaxWeight(), count) / (float) this.getMaxWeight();
            return Math.max(1, NukkitMath.ceilFloat(f * 15.0F));
        } else {
            return 0;
        }
    }

    public int getMaxWeight() {
        return 150;
    }
}
