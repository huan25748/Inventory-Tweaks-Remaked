package com.inventorytweaks.remaked;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IContainerManager {
    int DROP_SLOT = -999;

    boolean move(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex);

    boolean moveSome(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex, int amount);

    default boolean drop(ContainerSection srcSection, int srcIndex) {
        return move(srcSection, srcIndex, null, DROP_SLOT);
    }

    default boolean dropSome(ContainerSection srcSection, int srcIndex, int amount) {
        return moveSome(srcSection, srcIndex, null, DROP_SLOT, amount);
    }

    boolean putHoldItemDown(ContainerSection destSection, int destIndex);

    default void leftClick(ContainerSection section, int index) {
        click(section, index, false);
    }

    default void rightClick(ContainerSection section, int index) {
        click(section, index, true);
    }

    void click(ContainerSection section, int index, boolean rightClick);

    boolean hasSection(ContainerSection section);

    List<Slot> getSlots(ContainerSection section);

    int getSize();

    int getSize(ContainerSection section);

    int getFirstEmptyIndex(ContainerSection section);

    boolean isSlotEmpty(ContainerSection section, int slot);

    Slot getSlot(ContainerSection section, int index);

    default int getSlotIndex(int slotNumber) {
        return getSlotIndex(slotNumber, false);
    }

    int getSlotIndex(int slotNumber, boolean preferInventory);

    ContainerSection getSlotSection(int slotNumber);

    ItemStack getItemStack(ContainerSection section, int index);

    AbstractContainerMenu getContainer();

    void applyChanges();
}
