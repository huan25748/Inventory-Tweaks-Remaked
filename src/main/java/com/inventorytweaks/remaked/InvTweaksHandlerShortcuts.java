package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Jimeo Wan
 */
public class InvTweaksHandlerShortcuts extends InvTweaksPlatform {
    private static final int DROP_SLOT = -999;
    private final InvTweaksConfig config;
    private IContainerManager container;
    /**
     * Stores all pressed keys (only the ones that are related to shortcuts)
     */
    private final Map<Integer, Boolean> pressedKeys;
    /**
     * Stores the shortcuts mappings
     */
    private final Map<InvTweaksShortcutType, List<InvTweaksShortcutMapping>> shortcuts;

    public InvTweaksHandlerShortcuts(Minecraft mc_, InvTweaksConfig config_) {
        super(mc_);
        config = config_;
        pressedKeys = new HashMap<>();
        shortcuts = new HashMap<>();
    }

    public void loadShortcuts() {
        pressedKeys.clear();
        shortcuts.clear();

        // Register shortcut mappings
        Map<String, String> keys = config.getProperties(InvTweaksConfig.PROP_SHORTCUT_PREFIX);
        for (String key : keys.keySet()) {
            String[] keyMappings = keys.get(key).split("[ ]*,[ ]*");
            InvTweaksShortcutType shortcutType = InvTweaksShortcutType.fromConfigKey(key);
            if (shortcutType != null) {
                for (String keyMapping : keyMappings) {
                    if (keyMapping.trim().isEmpty()) {
                        continue;
                    }
                    String[] keysToHold = keyMapping.split("\\+");
                    registerShortcutMapping(shortcutType, new InvTweaksShortcutMapping(keysToHold));
                }
            }
        }

        // Add Minecraft's Up & Down mappings
        registerShortcutMapping(InvTweaksShortcutType.MOVE_UP, new InvTweaksShortcutMapping(getKeyBindingForwardKeyCode()));
        registerShortcutMapping(InvTweaksShortcutType.MOVE_DOWN, new InvTweaksShortcutMapping(getKeyBindingBackKeyCode()));

        // Add hotbar shortcuts (1-9) mappings
        for (int i = 1; i <= 9; i++) {
            registerShortcutMapping(InvTweaksShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT, new InvTweaksShortcutMapping(Keys.getCode(String.valueOf(i))));
            registerShortcutMapping(InvTweaksShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT, new InvTweaksShortcutMapping(Keys.getCode("NUMPAD" + i)));
        }

        // Register (L/R)SHIFT to allow to filter them
        pressedKeys.put(Keys.getCode("LSHIFT"), false);
        pressedKeys.put(Keys.getCode("RSHIFT"), false);
    }

    private void registerShortcutMapping(InvTweaksShortcutType type, InvTweaksShortcutMapping mapping) {
        shortcuts.computeIfAbsent(type, k -> new LinkedList<>()).add(mapping);
        for (int keyCode : mapping.getKeyCodes()) {
            pressedKeys.put(keyCode, false);
        }
    }

    /**
     * @return true if a shortcut was triggered (and the click should not be handled by the vanilla screen).
     */
    public boolean handleShortcut() {
        // Check the live config here as well: the settings toggle writes the property right away, and this makes
        // sure no call path can run shortcuts while they are disabled.
        InvTweaksConfigManager liveManager = InvTweaks.getConfigManager();
        if (liveManager == null || liveManager.getConfig() == null
                || !liveManager.getConfig().getProperty(InvTweaksConfig.PROP_ENABLE_SHORTCUTS).equals(InvTweaksConfig.VALUE_TRUE)) {
            return false;
        }
        try {
            ShortcutConfig shortcutToTrigger = computeShortcutToTrigger();
            if (shortcutToTrigger != null) {
                runShortcut(shortcutToTrigger);
                return true;
            }
        } catch (Exception e) {
            InvTweaks.logInGameErrorStatic("invtweaks.shortcut.error", e);
        }
        return false;
    }

    public ShortcutSpecification computeCurrentShortcut() {
        ShortcutSpecification.Action action = ShortcutSpecification.Action.MOVE;
        ShortcutSpecification.Target target = ShortcutSpecification.Target.UNSPECIFIED;
        ShortcutSpecification.Scope scope = ShortcutSpecification.Scope.ONE_STACK;

        updatePressedKeys();

        boolean validAction = false;
        if (isShortcutDown(InvTweaksShortcutType.DROP) != null) {
            action = ShortcutSpecification.Action.DROP;
            validAction = true;
        }

        if (action != ShortcutSpecification.Action.DROP) {
            if (isShortcutDown(InvTweaksShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT) != null) {
                target = ShortcutSpecification.Target.HOTBAR_SLOT;
                validAction = true;
            } else if (isShortcutDown(InvTweaksShortcutType.MOVE_UP) != null) {
                target = ShortcutSpecification.Target.UP;
                validAction = true;
            } else if (isShortcutDown(InvTweaksShortcutType.MOVE_DOWN) != null) {
                target = ShortcutSpecification.Target.DOWN;
                validAction = true;
            }
        }

        if (isShortcutDown(InvTweaksShortcutType.MOVE_ALL_ITEMS) != null) {
            scope = ShortcutSpecification.Scope.ALL_ITEMS;
            validAction = true;
        } else if (isShortcutDown(InvTweaksShortcutType.MOVE_EVERYTHING) != null) {
            scope = ShortcutSpecification.Scope.EVERYTHING;
            validAction = true;
        } else if (isShortcutDown(InvTweaksShortcutType.MOVE_ONE_ITEM) != null) {
            scope = ShortcutSpecification.Scope.ONE_ITEM;
            validAction = true;
        }

        if (validAction) {
            return new ShortcutSpecification(action, target, scope);
        }
        return null;
    }

    private ShortcutConfig computeShortcutToTrigger() {
        ShortcutSpecification shortcut = computeCurrentShortcut();
        if (getCurrentScreen() == null) {
            return null;
        }
        if (!(getCurrentScreen() instanceof AbstractContainerScreen<?> screen)) {
            return null;
        }

        ShortcutConfig shortcutConfig = new ShortcutConfig();

        container = InvTweaks.getCurrentContainerManager();
        Slot slot = InvTweaksPlatform.getSlotAtMousePosition(screen);
        // If a valid and not empty slot is clicked
        if (shortcut != null && slot != null && (slot.hasItem() || !getHeldStack().isEmpty())) {
            int slotNumber = getSlotNumber(slot);

            // Set shortcut origin
            shortcutConfig.fromSection = container.getSlotSection(slotNumber);
            shortcutConfig.fromIndex = container.getSlotIndex(slotNumber);
            shortcutConfig.fromStack = slot.hasItem() ? slot.getItem().copy() : getHeldStack().copy();

            // Compute shortcut type
            // Ensure the item currently in the slot can be placed back into it for one-item shortcuts.
            if (!slot.mayPlace(slot.getItem()) && shortcut.getScope() == ShortcutSpecification.Scope.ONE_ITEM) {
                shortcut.setScope(ShortcutSpecification.Scope.ONE_STACK);
            }

            if (shortcutConfig.fromSection != null && shortcutConfig.fromIndex != -1) {
                if (shortcut.getAction() != ShortcutSpecification.Action.DROP) {
                    // Compute shortcut target
                    if (shortcut.getTarget() == ShortcutSpecification.Target.HOTBAR_SLOT) {
                        shortcutConfig.toSection = ContainerSection.INVENTORY_HOTBAR;
                        InvTweaksShortcutMapping hotbarShortcut = isShortcutDown(InvTweaksShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT);
                        if (hotbarShortcut != null && !hotbarShortcut.getKeyCodes().isEmpty()) {
                            shortcutConfig.toIndex = hotbarIndex(hotbarShortcut.getKeyCodes().get(0));
                        }
                    } else {
                        // Compute targetable sections in order
                        List<ContainerSection> orderedSections = new ArrayList<>();

                        // (Top part)
                        if (container.hasSection(ContainerSection.CHEST)) {
                            orderedSections.add(ContainerSection.CHEST);
                        } else if (container.hasSection(ContainerSection.CRAFTING_IN)) {
                            orderedSections.add(ContainerSection.CRAFTING_IN);
                        } else if (container.hasSection(ContainerSection.CRAFTING_IN_PERSISTENT)) {
                            orderedSections.add(ContainerSection.CRAFTING_IN_PERSISTENT);
                        } else if (container.hasSection(ContainerSection.FURNACE_IN)) {
                            orderedSections.add(ContainerSection.FURNACE_IN);
                        } else if (container.hasSection(ContainerSection.BREWING_INGREDIENT)) {
                            if (!shortcutConfig.fromStack.isEmpty()) {
                                if (shortcutConfig.fromStack.is(Items.POTION)) {
                                    orderedSections.add(ContainerSection.BREWING_BOTTLES);
                                } else {
                                    orderedSections.add(ContainerSection.BREWING_INGREDIENT);
                                }
                            }
                        } else if (container.hasSection(ContainerSection.ENCHANTMENT)) {
                            orderedSections.add(ContainerSection.ENCHANTMENT);
                        }

                        // (Inventory part)
                        orderedSections.add(ContainerSection.INVENTORY_NOT_HOTBAR);
                        orderedSections.add(ContainerSection.INVENTORY_HOTBAR);

                        // Choose target section
                        if (shortcut.getTarget() != ShortcutSpecification.Target.UNSPECIFIED) { // Explicit section (up/down shortcuts)
                            int sectionOffset = 0;
                            if (shortcut.getTarget() == ShortcutSpecification.Target.UP) {
                                sectionOffset--;
                            } else if (shortcut.getTarget() == ShortcutSpecification.Target.DOWN) {
                                sectionOffset++;
                            }
                            int fromSectionIndex = orderedSections.indexOf(shortcutConfig.fromSection);
                            if (fromSectionIndex != -1) {
                                shortcutConfig.toSection = orderedSections.get((orderedSections.size() + fromSectionIndex + sectionOffset) % orderedSections.size());
                            } else {
                                shortcutConfig.toSection = ContainerSection.INVENTORY;
                            }
                        } else { // Implicit section
                            switch (shortcutConfig.fromSection) {
                                case CHEST:
                                    shortcutConfig.toSection = ContainerSection.INVENTORY;
                                    break;
                                case INVENTORY_HOTBAR:
                                    if (orderedSections.contains(ContainerSection.CHEST)) {
                                        shortcutConfig.toSection = ContainerSection.CHEST;
                                    } else {
                                        shortcutConfig.toSection = ContainerSection.INVENTORY_NOT_HOTBAR;
                                    }
                                    break;
                                case CRAFTING_IN:
                                case CRAFTING_IN_PERSISTENT:
                                case FURNACE_IN:
                                    shortcutConfig.toSection = ContainerSection.INVENTORY_NOT_HOTBAR;
                                    break;
                                default:
                                    if (orderedSections.contains(ContainerSection.CHEST)) {
                                        shortcutConfig.toSection = ContainerSection.CHEST;
                                    } else {
                                        shortcutConfig.toSection = ContainerSection.INVENTORY_HOTBAR;
                                    }
                            }
                        }
                    }
                }

                // Shortcut modifiers
                shortcutConfig.forceEmptySlot = Keys.isMouseDown(1);
                shortcutConfig.action = shortcut.getAction();
                shortcutConfig.scope = shortcut.getScope();

                return shortcutConfig;
            }
        }

        return null;
    }

    private static int hotbarIndex(int keyCode) {
        for (int i = 1; i <= 9; i++) {
            if (keyCode == Keys.getCode(String.valueOf(i)) || keyCode == Keys.getCode("NUMPAD" + i)) {
                return i - 1;
            }
        }
        return -1;
    }

    public void updatePressedKeys() {
        if (haveControlsChanged()) {
            loadShortcuts(); // Reset mappings
        }
        for (int keyCode : pressedKeys.keySet()) {
            pressedKeys.put(keyCode, Keys.isDown(keyCode));
        }
    }

    /**
     * Checks if the Up/Down controls that are listened are outdated
     */
    private boolean haveControlsChanged() {
        return (!pressedKeys.containsKey(getKeyBindingForwardKeyCode()) || !pressedKeys.containsKey(getKeyBindingBackKeyCode()));
    }

    public InvTweaksShortcutMapping isShortcutDown(InvTweaksShortcutType type) {
        List<InvTweaksShortcutMapping> mappings = shortcuts.get(type);
        if (mappings != null) {
            for (InvTweaksShortcutMapping mapping : mappings) {
                if (mapping.isTriggered(pressedKeys)) {
                    return mapping;
                }
            }
        }
        return null;
    }

    private void runShortcut(ShortcutConfig shortcut) {
        // Try to put held item down
        if (!getHeldStack().isEmpty()) {
            if (getCurrentScreen() instanceof AbstractContainerScreen<?> screen) {
                Slot slot = InvTweaksPlatform.getSlotAtMousePosition(screen);
                if (slot != null) {
                    int slotNumber = getSlotNumber(slot);
                    container.putHoldItemDown(container.getSlotSection(slotNumber), container.getSlotIndex(slotNumber));
                    if (!getHeldStack().isEmpty()) {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        synchronized (this) {
            if (shortcut.toSection == ContainerSection.INVENTORY_HOTBAR && shortcut.toIndex != -1) {
                container.move(shortcut.fromSection, shortcut.fromIndex, shortcut.toSection, shortcut.toIndex);
            } else {
                switch (shortcut.action) {
                    case DROP: {
                        switch (shortcut.scope) {
                            case ONE_ITEM:
                                container.dropSome(shortcut.fromSection, shortcut.fromIndex, 1);
                                break;
                            case ONE_STACK:
                                container.drop(shortcut.fromSection, shortcut.fromIndex);
                                break;
                            case ALL_ITEMS:
                                dropAll(shortcut, shortcut.fromStack);
                                break;
                            case EVERYTHING:
                                dropAll(shortcut, ItemStack.EMPTY);
                                break;
                        }
                        break;
                    }
                    case MOVE: {
                        int toIndex;
                        boolean success;
                        int newIndex;

                        switch (shortcut.scope) {
                            case ONE_STACK: {
                                Slot slot = container.getSlot(shortcut.fromSection, shortcut.fromIndex);
                                if (slot != null && slot.hasItem()) {
                                    toIndex = getNextTargetIndex(shortcut, slot.getItem());
                                    if (shortcut.fromSection != ContainerSection.CRAFTING_OUT && shortcut.toSection != ContainerSection.ENCHANTMENT) {
                                        while (slot.hasItem() && toIndex != -1) {
                                            success = container.move(shortcut.fromSection, shortcut.fromIndex, shortcut.toSection, toIndex);
                                            newIndex = getNextTargetIndex(shortcut, slot.getItem());
                                            toIndex = (success || (shortcut.action == ShortcutSpecification.Action.DROP) || newIndex != toIndex) ? newIndex : -1;
                                        }
                                    } else {
                                        // Move only once, since the crafting output might be refilled
                                        container.move(shortcut.fromSection, shortcut.fromIndex, shortcut.toSection, toIndex);
                                    }
                                }
                                break;
                            }
                            case ONE_ITEM: {
                                Slot slot = container.getSlot(shortcut.fromSection, shortcut.fromIndex);
                                if (slot != null && slot.hasItem()) {
                                    toIndex = getNextTargetIndex(shortcut, slot.getItem());
                                    container.moveSome(shortcut.fromSection, shortcut.fromIndex, shortcut.toSection, toIndex, 1);
                                }
                                break;
                            }
                            case ALL_ITEMS: {
                                moveAll(shortcut, shortcut.fromStack);
                                if (shortcut.fromSection == ContainerSection.INVENTORY_NOT_HOTBAR && shortcut.toSection == ContainerSection.CHEST) {
                                    shortcut.fromSection = ContainerSection.INVENTORY_HOTBAR;
                                    moveAll(shortcut, shortcut.fromStack);
                                }
                                break;
                            }
                            case EVERYTHING: {
                                moveAll(shortcut, ItemStack.EMPTY);
                                if (shortcut.fromSection == ContainerSection.INVENTORY_HOTBAR && shortcut.toSection == ContainerSection.CHEST) {
                                    shortcut.fromSection = ContainerSection.INVENTORY_HOTBAR;
                                    moveAll(shortcut, ItemStack.EMPTY);
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void dropAll(ShortcutConfig shortcut, ItemStack stackToMatch) {
        container.getSlots(shortcut.fromSection).stream()
                .filter(slot -> slot.hasItem() && (stackToMatch.isEmpty() || areSameItemType(stackToMatch, slot.getItem())))
                .forEach(slot -> {
                    int fromIndex = container.getSlotIndex(getSlotNumber(slot));
                    while (slot.hasItem()) {
                        container.drop(shortcut.fromSection, fromIndex);
                    }
                });
    }

    private void moveAll(ShortcutConfig shortcut, ItemStack stackToMatch) {
        int toIndex = Integer.MIN_VALUE;
        int newIndex;
        boolean success;

        for (Slot slot : container.getSlots(shortcut.fromSection)) {
            if (slot.hasItem() && (stackToMatch.isEmpty() || areSameItemType(stackToMatch, slot.getItem()))) {
                int fromIndex = container.getSlotIndex(getSlotNumber(slot));
                toIndex = getNextTargetIndex(shortcut, slot.getItem());

                // Move while current slot has item, and there is a valid target that is not the same slot we're trying
                // to move from.
                while (slot.hasItem() && toIndex != -1 && !(shortcut.fromSection == shortcut.toSection && fromIndex == toIndex)) {
                    success = container.move(shortcut.fromSection, fromIndex, shortcut.toSection, toIndex);
                    newIndex = getNextTargetIndex(shortcut, slot.getItem());

                    if (success && newIndex == toIndex && slot.hasItem()) {
                        throw new RuntimeException("Inventory in invalid state after move");
                    }

                    if (success || (newIndex != toIndex) || (shortcut.action == ShortcutSpecification.Action.DROP)) {
                        toIndex = newIndex;
                    } else {
                        toIndex = -1;
                    }
                }
            }
            if (toIndex == -1) {
                break;
            }
        }
    }

    private int getNextTargetIndex(ShortcutConfig shortcut, ItemStack current) {
        if (shortcut.action == ShortcutSpecification.Action.DROP) {
            return DROP_SLOT;
        }

        int result = -1;

        // Try to merge with existing slot
        if (!shortcut.forceEmptySlot) {
            int i = 0;
            for (Slot slot : container.getSlots(shortcut.toSection)) {
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    if (areItemsStackable(current, stack) && stack.getCount() < stack.getMaxStackSize()) {
                        result = i;
                        break;
                    }
                }
                i++;
            }
        }

        // Else find empty slot
        if (result == -1) {
            result = container.getFirstEmptyIndex(shortcut.toSection);
        }

        // Switch from FURNACE_IN to FURNACE_FUEL if the slot is taken
        if (result == -1 && shortcut.toSection == ContainerSection.FURNACE_IN) {
            shortcut.toSection = ContainerSection.FURNACE_FUEL;
            result = container.getFirstEmptyIndex(shortcut.toSection);
        }

        return result;
    }

    public static class ShortcutConfig {
        public ContainerSection fromSection;
        public int fromIndex;
        public ItemStack fromStack;
        public ContainerSection toSection;
        public int toIndex = -1;
        public boolean forceEmptySlot = false;
        public ShortcutSpecification.Action action = ShortcutSpecification.Action.MOVE;
        public ShortcutSpecification.Scope scope = ShortcutSpecification.Scope.ONE_STACK;
    }
}
