package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to perform various operations on the inventory and/or containers. Works in both single and multiplayer.
 *
 * @author Jimeo Wan
 */
public class DirectContainerManager implements IContainerManager {

    private final AbstractContainerMenu container;
    private Map<ContainerSection, List<Slot>> slotRefs = new HashMap<>();

    public DirectContainerManager(AbstractContainerMenu cont) {
        container = cont;
        initSlots();
    }

    private void initSlots() {
        Map<ContainerSection, List<Slot>> refs = InvTweaksPlatform.getContainerSlotMap(container);
        if (refs == null) {
            slotRefs = new HashMap<>();
        } else {
            slotRefs = refs;
        }
    }

    @Override
    public boolean move(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex) {
        ItemStack srcStack = getItemStack(srcSection, srcIndex);
        ItemStack destStack = (destIndex == DROP_SLOT) ? ItemStack.EMPTY : getItemStack(destSection, destIndex);

        if (srcStack.isEmpty() && destIndex != DROP_SLOT) {
            return false;
        } else if (srcSection == destSection && srcIndex == destIndex) {
            return true;
        }

        // Mod support -- some mods play tricks with slots to display an item but not let it be interacted with.
        if (destIndex != DROP_SLOT) {
            Slot destSlot = getSlot(destSection, destIndex);
            if (destSlot != null && !destSlot.mayPlace(srcStack)) {
                return false;
            }
        }

        // Put hold item down
        if (!InvTweaks.getInstance().getHeldStack().isEmpty()) {
            int firstEmptyIndex = getFirstEmptyIndex(ContainerSection.INVENTORY);
            if (firstEmptyIndex != -1) {
                leftClick(ContainerSection.INVENTORY, firstEmptyIndex);
            } else {
                return false;
            }
        }

        // Use intermediate slot if we have to swap tools, maps, etc.
        assert !srcStack.isEmpty();
        if (!destStack.isEmpty() && !InvTweaksPlatform.areItemsStackable(srcStack, destStack)) {
            int intermediateSlot = getFirstEmptyUsableSlotNumber();
            ContainerSection intermediateSection = (intermediateSlot == -1) ? null : getSlotSection(intermediateSlot);
            int intermediateIndex = (intermediateSlot == -1) ? -1 : getSlotIndex(intermediateSlot);
            if (intermediateIndex != -1 && intermediateSection != null) {
                Slot interSlot = getSlot(intermediateSection, intermediateIndex);
                if (interSlot != null && !interSlot.mayPlace(destStack)) {
                    return false;
                }
                Slot srcSlot = getSlot(srcSection, srcIndex);
                if (srcSlot != null && !srcSlot.mayPlace(destStack)) {
                    return false;
                }
                // Step 1/3: Dest > Int
                leftClick(destSection, destIndex);
                leftClick(intermediateSection, intermediateIndex);
                // Step 2/3: Src > Dest
                leftClick(srcSection, srcIndex);
                leftClick(destSection, destIndex);
                // Step 3/3: Int > Src
                leftClick(intermediateSection, intermediateIndex);
                leftClick(srcSection, srcIndex);
            } else {
                return false;
            }
        }

        // Normal move
        else {
            leftClick(srcSection, srcIndex);
            leftClick(destSection, destIndex);
            if (!InvTweaks.getInstance().getHeldStack().isEmpty()) {
                // Only return to original slot if it can be placed in that slot.
                // (Ex. crafting/furnace outputs)
                Slot srcSlot = getSlot(srcSection, srcIndex);
                if (srcSlot != null && srcSlot.mayPlace(InvTweaks.getInstance().getHeldStack())) {
                    leftClick(srcSection, srcIndex);
                } else {
                    // If the item cannot be placed in its original slot, move to an empty slot.
                    int firstEmptyIndex = getFirstEmptyIndex(ContainerSection.INVENTORY);
                    if (firstEmptyIndex != -1) {
                        leftClick(ContainerSection.INVENTORY, firstEmptyIndex);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean moveSome(ContainerSection srcSection, int srcIndex, ContainerSection destSection, int destIndex, int amount) {
        ItemStack source = getItemStack(srcSection, srcIndex);
        if (source.isEmpty() || (srcSection == destSection && srcIndex == destIndex)) {
            return true;
        }

        ItemStack destination = getItemStack(destSection, destIndex);
        int sourceSize = source.getCount();
        int movedAmount = Math.min(amount, sourceSize);

        if (destination.isEmpty() || InvTweaksPlatform.areItemStacksEqual(source, destination)) {
            leftClick(srcSection, srcIndex);
            for (int i = 0; i < movedAmount; i++) {
                rightClick(destSection, destIndex);
            }
            if (movedAmount < sourceSize) {
                leftClick(srcSection, srcIndex);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean putHoldItemDown(ContainerSection destSection, int destIndex) {
        ItemStack heldStack = InvTweaks.getInstance().getHeldStack();
        if (!heldStack.isEmpty()) {
            if (getItemStack(destSection, destIndex).isEmpty()) {
                click(destSection, destIndex, false);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void click(ContainerSection section, int index, boolean rightClick) {
        int slot = indexToSlot(section, index);
        if (slot != -1) {
            int button = (rightClick) ? 1 : 0;
            if (InvTweaks.getInstance() != null && InvTweaks.getInstance().getThePlayer() != null
                    && Minecraft.getInstance().gameMode != null) {
                Minecraft.getInstance().gameMode.handleInventoryMouseClick(container.containerId, slot, button,
                        ClickType.PICKUP, InvTweaks.getInstance().getThePlayer());
            }
        }
    }

    @Override
    public boolean hasSection(ContainerSection section) {
        return slotRefs.containsKey(section);
    }

    @Override
    public List<Slot> getSlots(ContainerSection section) {
        return slotRefs.get(section);
    }

    @Override
    public int getSize() {
        int result = 0;
        for (List<Slot> slots : slotRefs.values()) {
            result += slots.size();
        }
        return result;
    }

    @Override
    public int getSize(ContainerSection section) {
        if (hasSection(section)) {
            return slotRefs.get(section).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getFirstEmptyIndex(ContainerSection section) {
        if (!hasSection(section)) {
            return -1;
        }
        int i = 0;
        for (Slot slot : slotRefs.get(section)) {
            if (!slot.hasItem()) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public boolean isSlotEmpty(ContainerSection section, int slot) {
        return hasSection(section) && getItemStack(section, slot).isEmpty();
    }

    @Override
    public Slot getSlot(ContainerSection section, int index) {
        List<Slot> slots = slotRefs.get(section);
        if (slots != null && index >= 0 && index < slots.size()) {
            return slots.get(index);
        } else {
            return null;
        }
    }

    @Override
    public int getSlotIndex(int slotNumber, boolean preferInventory) {
        for (ContainerSection section : slotRefs.keySet()) {
            if (!preferInventory && section != ContainerSection.INVENTORY
                    || (preferInventory && section != ContainerSection.INVENTORY_NOT_HOTBAR && section != ContainerSection.INVENTORY_HOTBAR)) {
                int i = 0;
                for (Slot slot : slotRefs.get(section)) {
                    if (InvTweaksPlatform.getSlotNumber(slot) == slotNumber) {
                        return i;
                    }
                    i++;
                }
            }
        }
        return -1;
    }

    @Override
    public ContainerSection getSlotSection(int slotNumber) {
        for (ContainerSection section : slotRefs.keySet()) {
            if (section != ContainerSection.INVENTORY) {
                for (Slot slot : slotRefs.get(section)) {
                    if (InvTweaksPlatform.getSlotNumber(slot) == slotNumber) {
                        return section;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ItemStack getItemStack(ContainerSection section, int index) {
        int slot = indexToSlot(section, index);
        if (slot >= 0 && slot < container.slots.size()) {
            return InvTweaksPlatform.getSlotStack(container, slot);
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public AbstractContainerMenu getContainer() {
        return container;
    }

    private int getFirstEmptyUsableSlotNumber() {
        for (ContainerSection section : slotRefs.keySet()) {
            for (Slot slot : slotRefs.get(section)) {
                // Use only standard slot (to make sure we can freely put and remove items there)
                if (InvTweaksPlatform.isBasicSlot(slot) && !slot.hasItem()) {
                    return InvTweaksPlatform.getSlotNumber(slot);
                }
            }
        }
        return -1;
    }

    /**
     * Converts section/index values to slot ID.
     *
     * @return -1 if not found
     */
    private int indexToSlot(ContainerSection section, int index) {
        if (index == DROP_SLOT) {
            return DROP_SLOT;
        } else if (index < 0) {
            return -1;
        } else if (hasSection(section)) {
            List<Slot> slots = slotRefs.get(section);
            if (slots != null && index < slots.size()) {
                Slot slot = slots.get(index);
                if (slot != null) {
                    return InvTweaksPlatform.getSlotNumber(slot);
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public void applyChanges() {
        // The 1.12 implementation sent a packet asking the server to re-synchronize the container.
        // Clicking through gameMode already keeps both sides in sync in 1.21, so a local resync is enough.
        try {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.containerMenu.broadcastChanges();
            }
        } catch (Exception ignored) {
        }
    }
}
