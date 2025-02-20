package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.behavior.BlockBehaviorFire;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import static org.cloudburstmc.api.block.BlockTypes.OBSIDIAN;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ItemFlintSteelBehavior extends ItemToolBehavior {
    /**
     * The maximum possible size of the outside of a nether portal
     * 23x23 in vanilla
     */
    private static final int MAX_PORTAL_SIZE = 23;

    public ItemFlintSteelBehavior() {
        super(null, null);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public ItemStack onActivate(ItemStack itemStack, Player player, Block block, Block target, Direction face, Vector3f clickPos, Level level) {
        var targetState = target.getState();
        if (block.getState() == BlockStates.AIR && targetState.getBehavior().isSolid(targetState) || targetState.getType() == BlockTypes.LEAVES) {
            PORTAL:
            if (targetState.getType() == OBSIDIAN && ((CloudLevel) player.getLevel()).getDimension() != CloudLevel.DIMENSION_THE_END) {
                final Vector3i pos = clickPos.toInt();
                final int targX = pos.getX();
                final int targY = pos.getY();
                final int targZ = pos.getZ();
                //check if there's air above (at least 3 blocks)
                for (int i = 1; i < 4; i++) {
                    if (level.getBlockState(targX, targY + i, targZ) != BlockStates.AIR) {
                        break PORTAL;
                    }
                }
                int sizePosX = 0;
                int sizeNegX = 0;
                int sizePosZ = 0;
                int sizeNegZ = 0;
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockState(targX + i, targY, targZ).getType() == OBSIDIAN) {
                        sizePosX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockState(targX - i, targY, targZ).getType() == OBSIDIAN) {
                        sizeNegX++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockState(targX, targY, targZ + i).getType() == OBSIDIAN) {
                        sizePosZ++;
                    } else {
                        break;
                    }
                }
                for (int i = 1; i < MAX_PORTAL_SIZE; i++) {
                    if (level.getBlockState(targX, targY, targZ - i).getType() == OBSIDIAN) {
                        sizeNegZ++;
                    } else {
                        break;
                    }
                }
                //plus one for target block
                int sizeX = sizePosX + sizeNegX + 1;
                int sizeZ = sizePosZ + sizeNegZ + 1;
                if (sizeX >= 2 && sizeX <= MAX_PORTAL_SIZE) {
                    //start scan from 1 block above base
                    //find pillar or end of portal to start scan
                    int scanX = targX;
                    int scanY = targY + 1;
                    int scanZ = targZ;
                    for (int i = 0; i < sizePosX + 1; i++) {
                        //this must be air
                        if (level.getBlockState(scanX + i, scanY, scanZ) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (level.getBlockState(scanX + i + 1, scanY, scanZ).getType() == OBSIDIAN) {
                            scanX += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (level.getBlockState(scanX + 1, scanY, scanZ).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockState(scanX - i, scanY, scanZ);
                        if (state == BlockStates.AIR) {
                            innerWidth++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    int innerHeight = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockState(scanX, scanY + i, scanZ);
                        if (state == BlockStates.AIR) {
                            innerHeight++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    if (!(innerWidth <= MAX_PORTAL_SIZE - 2
                            && innerWidth >= 2
                            && innerHeight <= MAX_PORTAL_SIZE - 2
                            && innerHeight >= 3)) {
                        break PORTAL;
                    }

                    for (int height = 0; height < innerHeight + 1; height++) {
                        if (height == innerHeight) {
                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockState(scanX - width, scanY + height, scanZ).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (level.getBlockState(scanX + 1, scanY + height, scanZ).getType() != OBSIDIAN
                                    || level.getBlockState(scanX - innerWidth, scanY + height, scanZ).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockState(scanX - width, scanY + height, scanZ) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            level.setBlockState(Vector3i.from(scanX - width, scanY + height, scanZ), CloudBlockRegistry.get().getBlock(BlockTypes.PORTAL));
                        }
                    }

                    ((CloudLevel) level).addLevelSoundEvent(clickPos, SoundEvent.IGNITE);
                    return null;
                } else if (sizeZ >= 2 && sizeZ <= MAX_PORTAL_SIZE) {
                    //start scan from 1 block above base
                    //find pillar or end of portal to start scan
                    int scanX = targX;
                    int scanY = targY + 1;
                    int scanZ = targZ;
                    for (int i = 0; i < sizePosZ + 1; i++) {
                        //this must be air
                        if (level.getBlockState(scanX, scanY, scanZ + i) != BlockStates.AIR) {
                            break PORTAL;
                        }
                        if (level.getBlockState(scanX, scanY, scanZ + i + 1).getType() == OBSIDIAN) {
                            scanZ += i;
                            break;
                        }
                    }
                    //make sure that the above loop finished
                    if (level.getBlockState(scanX, scanY, scanZ + 1).getType() != OBSIDIAN) {
                        break PORTAL;
                    }

                    int innerWidth = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockState(scanX, scanY, scanZ - i);
                        if (state == BlockStates.AIR) {
                            innerWidth++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    int innerHeight = 0;
                    for (int i = 0; i < MAX_PORTAL_SIZE - 2; i++) {
                        BlockState state = level.getBlockState(scanX, scanY + i, scanZ);
                        if (state == BlockStates.AIR) {
                            innerHeight++;
                        } else if (state.getType() == OBSIDIAN) {
                            break;
                        } else {
                            break PORTAL;
                        }
                    }
                    if (!(innerWidth <= MAX_PORTAL_SIZE - 2
                            && innerWidth >= 2
                            && innerHeight <= MAX_PORTAL_SIZE - 2
                            && innerHeight >= 3)) {
                        break PORTAL;
                    }

                    for (int height = 0; height < innerHeight + 1; height++) {
                        if (height == innerHeight) {
                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockState(scanX, scanY + height, scanZ - width).getType() != OBSIDIAN) {
                                    break PORTAL;
                                }
                            }
                        } else {
                            if (level.getBlockState(scanX, scanY + height, scanZ + 1).getType() != OBSIDIAN
                                    || level.getBlockState(scanX, scanY + height, scanZ - innerWidth).getType() != OBSIDIAN) {
                                break PORTAL;
                            }

                            for (int width = 0; width < innerWidth; width++) {
                                if (level.getBlockState(scanX, scanY + height, scanZ - width) != BlockStates.AIR) {
                                    break PORTAL;
                                }
                            }
                        }
                    }

                    for (int height = 0; height < innerHeight; height++) {
                        for (int width = 0; width < innerWidth; width++) {
                            level.setBlockState(Vector3i.from(scanX, scanY + height, scanZ - width), CloudBlockRegistry.get().getBlock(BlockTypes.PORTAL));
                        }
                    }

                    ((CloudLevel) level).addLevelSoundEvent(clickPos, SoundEvent.IGNITE);
                    return null;
                }
            }
            BlockState fire = CloudBlockRegistry.get().getBlock(BlockTypes.FIRE);
            BlockBehaviorFire fireBehavior = (BlockBehaviorFire) CloudBlockRegistry.get().getBehavior(BlockTypes.FIRE);

 /*           if (BlockBehaviorFire.isBlockTopFacingSurfaceSolid(block.downState()) || BlockBehaviorFire.canNeighborBurn(block)) {
                BlockIgniteEvent e = new BlockIgniteEvent(block, null, player, BlockIgniteEvent.BlockIgniteCause.FLINT_AND_STEEL);
                block.getLevel().getServer().getEventManager().fire(e);

                if (!e.isCancelled()) {
                    level.setBlock(block.getPosition(), fire, true);
                    level.addLevelSoundEvent(block.getPosition(), SoundEvent.IGNITE);
                    block = block.getLevel().getBlock(block.getPosition());
                    level.scheduleUpdate(block, fireBehavior.tickRate() + ThreadLocalRandom.current().nextInt(10));
                }
                return null;
            }*/ // TODO

            if (player.getGamemode().isSurvival()) {
                return this.useOn(itemStack, block.getState());
            }
        }

        return null;
    }

    @Override
    public int getMaxDurability() {
        return ItemToolBehavior.DURABILITY_FLINT_STEEL;
    }
}
