package org.cloudburstmc.server.inventory;

import com.google.common.collect.ImmutableSet;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.packet.InventoryContentPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.event.entity.EntityArmorChangeEvent;
import org.cloudburstmc.api.inventory.CreatureInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityCreature;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;

public class CloudCreatureInventory extends BaseInventory implements CreatureInventory {

    private int heldItemIndex = 0;
    private Int2ObjectMap<CloudItemStack> armorSlots = new Int2ObjectOpenHashMap<>(4);
    private CloudItemStack offHand;

    public CloudCreatureInventory(EntityCreature entity) {
        super(entity, InventoryType.PLAYER);
    }

    @Override
    public EntityCreature getHolder() {
        return (EntityCreature) super.getHolder();
    }

    @Override
    public int getSize() {
        return super.getSize() - 5;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size + 5);
    }

    @Override
    public int getHeldItemIndex() {
        return this.heldItemIndex;
    }

    public void setHeldItemIndex(int index) {
        if (index >= 0 && index <= this.getSize()) {
            this.heldItemIndex = index;
            this.sendHeldItem(this.getHolder().getViewers());
        }
    }

    @Override
    public void decrementHandCount() {
        this.decrementCount(getHeldItemIndex());
    }

    @Override
    public void incrementHandCount() {
        this.incrementCount(getHeldItemIndex());
    }

    @Override
    public CloudItemStack getItemInHand() {
        return this.getItem(this.getHeldItemIndex());
    }

    @Override
    public boolean setItemInHand(ItemStack item) {
        return this.setItem(this.getHeldItemIndex(), item);
    }

    public void sendHeldItem(Collection<CloudPlayer> players) {
        this.sendHeldItem(players.toArray(new CloudPlayer[0]));
    }

    public void sendHeldItem(CloudPlayer... players) {
        CloudItemStack item = this.getItemInHand();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setItem(item.getNetworkData());
        packet.setInventorySlot(this.getHeldItemIndex());
        packet.setHotbarSlot(this.getHeldItemIndex());

        for (CloudPlayer player : players) {
            packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
            if (player.equals(this.getHolder())) {
                packet.setRuntimeEntityId(player.getRuntimeId());
                this.sendSlot(this.getHeldItemIndex(), player);
            }

            player.sendPacket(packet);
        }
    }

    @Override
    public CloudItemStack getArmorItem(int index) {
        return this.armorSlots.get(index) == null ? CloudItemRegistry.get().AIR : this.armorSlots.get(index);
    }

    @Override
    public boolean setArmorItem(int index, ItemStack item, boolean ignoreArmorEvents) {
        if (index < 0 || index >= 3) {
            return false;
        } else if (item.isNull() || item.getAmount() <= 0) {
            return this.armorSlots.remove(index) != null;
        }

        CloudItemStack oldItem = this.armorSlots.get(index);
        if (!ignoreArmorEvents) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(getHolder(), oldItem, item, index);
            getHolder().getServer().getEventManager().fire(ev);

            if (ev.isCancelled()) {
                this.sendArmorSlot(index, this.getViewers());
                return false;
            }
        }
        this.armorSlots.put(index, ((CloudItemStack) item));
        this.sendArmorSlot(index, this.getViewers());
        return true;
    }

    @Override
    public Set<CloudItemStack> getArmorContents() {
        return ImmutableSet.copyOf(this.armorSlots.values());
    }

    @NonNull
    @Override
    public ItemStack getOffHandItem() {
        return this.offHand == null ? CloudItemRegistry.get().AIR : this.offHand;
    }

    @Override
    public void setOffHandItem(ItemStack offhand) {
        this.offHand = (CloudItemStack) offhand;
    }

    public void sendOffHandContents(Collection<CloudPlayer> players) {
        this.sendOffHandContents(players.toArray(new CloudPlayer[0]));
    }

    public void sendOffHandContents(CloudPlayer player) {
        this.sendOffHandContents(new CloudPlayer[]{player});
    }

    public void sendOffHandContents(CloudPlayer[] players) {
        ItemStack offHand = this.getOffHandItem();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offHand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (CloudPlayer player : players) {
            if (player.equals(this.getHolder())) {
                InventoryContentPacket invPacket = new InventoryContentPacket();
                invPacket.setContainerId(ContainerId.OFFHAND);
                invPacket.setContents(ItemUtils.toNetwork(Collections.singleton(offHand)));
                player.sendPacket(invPacket);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendOffHandSlot(CloudPlayer player) {
        this.sendOffHandSlot(new CloudPlayer[]{player});
    }

    public void sendOffHandSlot(Collection<CloudPlayer> players) {
        this.sendOffHandSlot(players.toArray(new CloudPlayer[0]));
    }

    public void sendOffHandSlot(CloudPlayer[] players) {
        ItemStack offhand = this.getOffHandItem();

        MobEquipmentPacket packet = new MobEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setItem(((CloudItemStack) offhand).getNetworkData());
        packet.setContainerId(ContainerId.OFFHAND);
        packet.setInventorySlot(1);

        for (CloudPlayer player : players) {
            if (player.equals(this.getHolder())) {
                InventorySlotPacket slotPacket = new InventorySlotPacket();
                slotPacket.setContainerId(ContainerId.OFFHAND);
                slotPacket.setItem(((CloudItemStack) offhand).getNetworkData());
                slotPacket.setSlot(0); // Not sure why offhand uses slot 0 in SlotPacket and 1 in MobEq Packet
                player.sendPacket(slotPacket);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendArmorContents(CloudPlayer player) {
        this.sendArmorContents(new CloudPlayer[]{player});
    }

    public void sendArmorContents(CloudPlayer[] players) {
        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(((CloudItemStack) getHelmet()).getNetworkData());
        packet.setChestplate(((CloudItemStack) getChestplate()).getNetworkData());
        packet.setLeggings(((CloudItemStack) getLeggings()).getNetworkData());
        packet.setBoots(((CloudItemStack) getBoots()).getNetworkData());

        for (CloudPlayer player : players) {
            if (player.equals(this.getHolder())) {
                InventoryContentPacket packet2 = new InventoryContentPacket();
                packet2.setContainerId(ContainerId.ARMOR);
                packet2.setContents(ItemUtils.toNetwork(new ArrayList<>(getArmorContents())));
                player.sendPacket(packet2);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void setArmorContents(ItemStack[] items) {
        if (items.length < 4) {
            ItemStack[] newItems = new ItemStack[4];
            System.arraycopy(items, 0, newItems, 0, items.length);
            items = newItems;
        }

        for (int i = 0; i < 4; ++i) {
            if (items[i] == null) {
                items[i] = CloudItemRegistry.get().AIR;
            }

            if (items[i].isNull()) {
                this.armorSlots.remove(i);
            } else {
                this.armorSlots.put(i, (CloudItemStack) items[i]);
            }
        }
    }

    public void sendArmorContents(Collection<CloudPlayer> players) {
        this.sendArmorContents(players.toArray(new CloudPlayer[0]));
    }

    public void sendArmorSlot(int index, CloudPlayer player) {
        this.sendArmorSlot(index, new CloudPlayer[]{player});
    }

    public void sendArmorSlot(int index, CloudPlayer[] players) {
        MobArmorEquipmentPacket packet = new MobArmorEquipmentPacket();
        packet.setRuntimeEntityId(this.getHolder().getRuntimeId());
        packet.setHelmet(((CloudItemStack) getHelmet()).getNetworkData());
        packet.setChestplate(((CloudItemStack) getChestplate()).getNetworkData());
        packet.setLeggings(((CloudItemStack) getLeggings()).getNetworkData());
        packet.setBoots(((CloudItemStack) getBoots()).getNetworkData());

        for (CloudPlayer player : players) {
            if (player.equals(this.getHolder())) {
                InventorySlotPacket packet2 = new InventorySlotPacket();
                packet2.setContainerId(ContainerId.ARMOR);
                packet2.setSlot(index);
                packet2.setItem(this.armorSlots.get(index).getNetworkData());
                player.sendPacket(packet2);
            } else {
                player.sendPacket(packet);
            }
        }
    }

    public void sendArmorSlot(int index, Collection<CloudPlayer> players) {
        this.sendArmorSlot(index, players.toArray(new CloudPlayer[0]));
    }

    @Override
    public void saveInventory(NbtMapBuilder tag) {
        super.saveInventory(tag);
        List<NbtMap> armor = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            // For whatever reason the armor list doesn't store "Slot" info
            armor.add(i, ItemUtils.serializeItem(getArmorItem(i)));
        }

        tag.putList("Armor", NbtType.COMPOUND, armor);
        if (this.offHand != null) {
            tag.putList("Offhand", NbtType.COMPOUND, ItemUtils.serializeItem(this.offHand));
        }
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        if (index >= this.getSize()) {
            this.sendArmorSlot(index - this.getSize(), this.getViewers());
            this.sendArmorSlot(index - this.getSize(), this.getHolder().getViewers());
        } else {
            super.onSlotChange(index, before, send);
        }
    }
}
