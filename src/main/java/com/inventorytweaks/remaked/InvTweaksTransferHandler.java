package com.inventorytweaks.remaked;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The "move items" buttons, modelled on Inventory Profiles Next's Move All / Move Matching feature.
 * <p>
 * Everything is done with vanilla {@link ClickType#QUICK_MOVE} clicks (the same click shift+click sends), so the server
 * performs the transfer: it merges into existing stacks, fills empty slots and never desyncs. Modded containers work
 * too, because their own {@code quickMoveStack} decides where the stack lands.
 */
public final class InvTweaksTransferHandler {

    public enum Mode {
        MATCHING,
        WITHOUT_HOTBAR,
        ALL
    }

    private InvTweaksTransferHandler() {
    }

    public static void transfer(ContainerSection from, ContainerSection to, Mode mode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        IContainerManager manager = InvTweaks.getCurrentContainerManager();
        if (manager == null) {
            return;
        }

        List<Slot> source = manager.getSlots(from);
        List<Slot> destination = manager.getSlots(to);
        if (source == null || destination == null || source.isEmpty() || destination.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = manager.getContainer();
        int hotbarStart = source.size() - InvTweaksConst.INVENTORY_HOTBAR_SIZE;

        for (int i = 0; i < source.size(); i++) {
            Slot slot = source.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (from == ContainerSection.INVENTORY && mode == Mode.WITHOUT_HOTBAR && i >= hotbarStart) {
                continue;
            }
            if (mode == Mode.MATCHING && !destinationContains(destination, stack)) {
                continue;
            }
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, mc.player);
        }
    }

    private static boolean destinationContains(List<Slot> destination, ItemStack stack) {
        for (Slot slot : destination) {
            ItemStack candidate = slot.getItem();
            if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(candidate, stack)) {
                return true;
            }
        }
        return false;
    }
}
