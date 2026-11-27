package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the slots of a container onto the sections used by Inventory Tweaks.
 * <p>
 * The 1.12 mod patched every Container class with ASM in order to get this information. Loading transformers into
 * Minecraft classes is not possible on Forge 1.20.1 either, so the mapping is deduced from the vanilla menu classes,
 * from the player's own inventory (which is part of nearly every menu) and, as a last resort, from the slot positions.
 */
public final class SlotMaps {

    private SlotMaps() {
    }

    public static Map<ContainerSection, List<Slot>> getSlotMap(AbstractContainerMenu menu) {
        if (menu.getClass().isAnnotationPresent(IgnoreContainer.class)) {
            return new HashMap<>();
        }

        // Annotation based (modded) containers
        ChestContainer chestAnnotation = menu.getClass().getAnnotation(ChestContainer.class);
        if (chestAnnotation != null) {
            return chestSlots(menu.slots);
        }

        if (isCreativeMenu(menu)) {
            return playerInventorySlots(menu.slots);
        }

        if (menu instanceof InventoryMenu) {
            return containerPlayerSlots(menu.slots);
        }
        if (menu instanceof CraftingMenu) {
            return containerWorkbenchSlots(menu.slots);
        }
        if (menu instanceof FurnaceMenu) {
            return containerFurnaceSlots(menu.slots);
        }
        if (menu instanceof BrewingStandMenu) {
            return containerBrewingSlots(menu.slots);
        }
        if (menu instanceof EnchantmentMenu) {
            return containerEnchantmentSlots(menu.slots);
        }
        if (menu instanceof AnvilMenu) {
            return containerRepairSlots(menu.slots);
        }
        if (menu instanceof GrindstoneMenu) {
            return containerGrindstoneSlots(menu.slots);
        }
        if (menu instanceof SmithingMenu) {
            return containerSmithingSlots(menu.slots);
        }
        if (menu instanceof LoomMenu) {
            return containerLoomSlots(menu.slots);
        }
        if (menu instanceof StonecutterMenu) {
            return containerStonecutterSlots(menu.slots);
        }
        if (menu instanceof CartographyTableMenu) {
            return containerCartographySlots(menu.slots);
        }
        if (menu instanceof BeaconMenu) {
            return containerBeaconSlots(menu.slots);
        }
        if (menu instanceof MerchantMenu) {
            return containerMerchantSlots(menu.slots);
        }
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof DispenserMenu
                || menu instanceof HopperMenu) {
            return chestSlots(menu.slots);
        }

        // Modded or unknown containers: the player inventory is always placed at the end.
        return unknownContainerSlots(menu.slots);
    }

    private static boolean isCreativeMenu(AbstractContainerMenu menu) {
        return menu.getClass().getName().startsWith("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen");
    }

    /**
     * Uses the player's own inventory to locate INVENTORY / HOTBAR / ARMOR slots. Works for every menu that keeps the
     * vanilla player inventory slots.
     */
    public static Map<ContainerSection, List<Slot>> playerInventorySlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        Inventory playerInventory = null;
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            playerInventory = player.getInventory();
        }

        List<Slot> hotbar = new ArrayList<>();
        List<Slot> main = new ArrayList<>();
        List<Slot> armor = new ArrayList<>();

        if (playerInventory != null) {
            for (Slot slot : slots) {
                if (slot.container != playerInventory) {
                    continue;
                }
                int index = slot.getContainerSlot();
                if (index >= 0 && index < 9) {
                    hotbar.add(slot);
                } else if (index >= 9 && index < 36) {
                    main.add(slot);
                } else if (index >= 36 && index < 40) {
                    armor.add(slot);
                }
            }
        }

        if (hotbar.isEmpty() && main.isEmpty()) {
            // Fallback: the inventory always ends the menu.
            return unknownContainerSlots(slots);
        }

        hotbar.sort(Comparator.comparingInt(Slot::getContainerSlot));
        main.sort(Comparator.comparingInt(Slot::getContainerSlot));
        armor.sort(Comparator.comparingInt(Slot::getContainerSlot));

        // The 1.12 section order is: main inventory first, hotbar last (hotbar = indexes 27..35).
        List<Slot> inventory = new ArrayList<>(main);
        inventory.addAll(hotbar);

        slotRefs.put(ContainerSection.INVENTORY, inventory);
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, hotbar);
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, main);
        if (!armor.isEmpty()) {
            slotRefs.put(ContainerSection.ARMOR, armor);
        }
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerPlayerSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(0, 1));
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(1, 5));
        slotRefs.put(ContainerSection.ARMOR, slots.subList(5, 9));
        slotRefs.put(ContainerSection.INVENTORY, slots.subList(9, 45));
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, slots.subList(9, 36));
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, slots.subList(36, 45));
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> chestSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        int size = slots.size();
        if (size < InvTweaksConst.INVENTORY_SIZE) {
            slotRefs.put(ContainerSection.CHEST, new ArrayList<>(slots));
            return slotRefs;
        }
        slotRefs.put(ContainerSection.CHEST, slots.subList(0, size - InvTweaksConst.INVENTORY_SIZE));
        slotRefs.put(ContainerSection.INVENTORY, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size));
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size - InvTweaksConst.HOTBAR_SIZE));
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, slots.subList(size - InvTweaksConst.HOTBAR_SIZE, size));
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerFurnaceSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.FURNACE_IN, slots.subList(0, 1));
        slotRefs.put(ContainerSection.FURNACE_FUEL, slots.subList(1, 2));
        slotRefs.put(ContainerSection.FURNACE_OUT, slots.subList(2, 3));
        addInventory(slotRefs, slots, 3);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerWorkbenchSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(0, 1));
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(1, 10));
        addInventory(slotRefs, slots, 10);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerEnchantmentSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.ENCHANTMENT, slots.subList(0, 1));
        addInventory(slotRefs, slots, 2);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerBrewingSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.BREWING_BOTTLES, slots.subList(0, 3));
        slotRefs.put(ContainerSection.BREWING_INGREDIENT, slots.subList(3, 4));
        addInventory(slotRefs, slots, 4);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerRepairSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 2));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(2, 3));
        addInventory(slotRefs, slots, 3);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerGrindstoneSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 2));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(2, 3));
        addInventory(slotRefs, slots, 3);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerSmithingSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 3));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(3, 4));
        addInventory(slotRefs, slots, 4);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerLoomSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 3));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(3, 4));
        addInventory(slotRefs, slots, 4);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerStonecutterSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 1));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(1, 2));
        addInventory(slotRefs, slots, 2);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerCartographySlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 2));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(2, 3));
        addInventory(slotRefs, slots, 3);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerBeaconSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CHEST, slots.subList(0, 1));
        addInventory(slotRefs, slots, 1);
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> containerMerchantSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        slotRefs.put(ContainerSection.CRAFTING_IN, slots.subList(0, 2));
        slotRefs.put(ContainerSection.CRAFTING_OUT, slots.subList(2, 3));
        addInventory(slotRefs, slots, 3);
        return slotRefs;
    }

    private static void addInventory(Map<ContainerSection, List<Slot>> slotRefs, List<Slot> slots, int start) {
        int size = slots.size();
        if (size - start < InvTweaksConst.INVENTORY_SIZE) {
            if (start < size) {
                slotRefs.put(ContainerSection.CHEST, slots.subList(start, size));
            }
            return;
        }
        slotRefs.put(ContainerSection.INVENTORY, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size));
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size - InvTweaksConst.HOTBAR_SIZE));
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, slots.subList(size - InvTweaksConst.HOTBAR_SIZE, size));
    }

    public static Map<ContainerSection, List<Slot>> unknownContainerSlots(List<Slot> slots) {
        Map<ContainerSection, List<Slot>> slotRefs = new LinkedHashMap<>();
        int size = slots.size();

        if (size >= InvTweaksConst.INVENTORY_SIZE) {
            slotRefs.put(ContainerSection.CHEST, slots.subList(0, size - InvTweaksConst.INVENTORY_SIZE));
            slotRefs.put(ContainerSection.INVENTORY, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size));
            slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, slots.subList(size - InvTweaksConst.INVENTORY_SIZE, size - InvTweaksConst.HOTBAR_SIZE));
            slotRefs.put(ContainerSection.INVENTORY_HOTBAR, slots.subList(size - InvTweaksConst.HOTBAR_SIZE, size));
        } else {
            slotRefs.put(ContainerSection.CHEST, new ArrayList<>(slots));
        }

        return slotRefs;
    }

    /**
     * @return the number of columns of the "chest" part of the container, deduced from the slot positions.
     */
    public static int getRowSize(AbstractContainerMenu menu) {
        if (menu == null) {
            return InvTweaksConst.INVENTORY_ROW_SIZE;
        }
        Map<ContainerSection, List<Slot>> map = getSlotMap(menu);
        List<Slot> chest = map.get(ContainerSection.CHEST);
        if (chest == null || chest.isEmpty()) {
            return InvTweaksConst.INVENTORY_ROW_SIZE;
        }
        int rows = 0;
        int previousX = Integer.MIN_VALUE;
        for (Slot slot : chest) {
            if (slot.x != previousX) {
                rows++;
                previousX = slot.x;
            }
        }
        return Math.max(1, Math.min(InvTweaksConst.INVENTORY_ROW_SIZE, rows));
    }
}
