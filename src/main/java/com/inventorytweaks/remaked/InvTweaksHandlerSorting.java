package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Core of the sorting behaviour. Allows to move items in a container (inventory or chest) with respect to the mod's
 * configuration.
 *
 * @author Jimeo Wan
 */
public class InvTweaksHandlerSorting extends InvTweaksPlatform {
    private static final int MAX_CONTAINER_SIZE = 999;
    private static int[] DEFAULT_LOCK_PRIORITIES = null;
    private static boolean[] DEFAULT_FROZEN_SLOTS = null;
    private final ContainerSectionManager containerMgr;
    private final SortingMethod algorithm;
    private final int size;
    private final boolean sortArmorParts;

    private final InvTweaksItemTree tree;
    private List<InvTweaksConfigSortingRule> rules;
    private final int[] rulePriority;
    private final int[] keywordOrder;
    private final int[] lockPriorities;
    private final boolean[] frozenSlots;

    public InvTweaksHandlerSorting(Minecraft mc_, InvTweaksConfig config, ContainerSection section, SortingMethod algorithm_, int rowSize) throws Exception {
        super(mc_);

        // Init constants
        if (DEFAULT_LOCK_PRIORITIES == null) {
            DEFAULT_LOCK_PRIORITIES = new int[MAX_CONTAINER_SIZE];
        }
        if (DEFAULT_FROZEN_SLOTS == null) {
            DEFAULT_FROZEN_SLOTS = new boolean[MAX_CONTAINER_SIZE];
        }

        // Init attributes
        containerMgr = new ContainerSectionManager(section);
        size = containerMgr.getSize();
        sortArmorParts = config.getProperty(InvTweaksConfig.PROP_ENABLE_AUTO_EQUIP_ARMOR).equals(InvTweaksConfig.VALUE_TRUE)
                && !isGuiInventoryCreative(getCurrentScreen());

        rules = config.getRules();
        tree = config.getTree();
        if (section == ContainerSection.INVENTORY) {
            lockPriorities = config.getLockPriorities();
            frozenSlots = config.getFrozenSlots();
            algorithm = SortingMethod.INVENTORY;
        } else {
            lockPriorities = DEFAULT_LOCK_PRIORITIES;
            frozenSlots = DEFAULT_FROZEN_SLOTS;
            algorithm = algorithm_;
            if (algorithm != SortingMethod.DEFAULT) {
                computeLineSortingRules(rowSize, algorithm == SortingMethod.HORIZONTAL);
            }
        }

        rulePriority = new int[size];
        keywordOrder = new int[size];
        for (int i = 0; i < size; i++) {
            rulePriority[i] = -1;
            ItemStack stack = containerMgr.getItemStack(i);
            if (!stack.isEmpty()) {
                keywordOrder[i] = getItemOrder(stack);
            } else {
                keywordOrder[i] = -1;
            }
        }

        // Initialize rule priority for currently matching items
        rules.stream().filter(rule -> (rule.getContainerSize() == size && rule.getPreferredSlots() != null)).forEach(rule -> {
            int priority = rule.getPriority();
            int[] preferredSlots = rule.getPreferredSlots();
            if (preferredSlots == null) {
                return;
            }
            for (int slot : preferredSlots) {
                if (slot < 0 || slot >= size) {
                    continue;
                }
                ItemStack stack = containerMgr.getItemStack(slot);
                if (!stack.isEmpty()) {
                    List<IItemTreeItem> items = tree.getItems(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue());
                    if (rulePriority[slot] < priority && tree.matches(items, rule.getKeyword())) {
                        rulePriority[slot] = priority;
                    }
                }
            }
        });
    }

    private static boolean canMergeStacks(ItemStack from, ItemStack to) {
        if (areItemsStackable(from, to)) {
            // We will not merge from a stack that exceeds its maximum size already.
            if (from.getCount() > from.getMaxStackSize()) {
                return false;
            }
            // If the destination stack has any room left, we can add to it.
            if (to.getCount() < to.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public void sort() {
        long timer = System.nanoTime();
        IContainerManager globalContainer = InvTweaks.getCurrentContainerManager();

        // Put hold item down
        if (!getHeldStack().isEmpty()) {
            int emptySlot = globalContainer.getFirstEmptyIndex(ContainerSection.INVENTORY);
            if (emptySlot != -1) {
                globalContainer.putHoldItemDown(ContainerSection.INVENTORY, emptySlot);
            } else {
                return; // Not enough room to work, abort
            }
        }

        if (algorithm != SortingMethod.DEFAULT) {
            if (algorithm == SortingMethod.EVEN_STACKS) {
                sortEvenStacks();
            } else if (algorithm == SortingMethod.INVENTORY) {
                sortInventory(globalContainer);
            }
            sortWithRules();
        }

        //// Sort remaining
        defaultSorting();

        if (InvTweaks.log.isDebugEnabled()) {
            timer = System.nanoTime() - timer;
            InvTweaks.log.debug("Sorting done in " + timer + "ns");
        }

        //// Put hold item down, just in case
        if (!getHeldStack().isEmpty()) {
            int emptySlot = globalContainer.getFirstEmptyIndex(ContainerSection.INVENTORY);
            if (emptySlot != -1) {
                globalContainer.putHoldItemDown(ContainerSection.INVENTORY, emptySlot);
            }
        }

        globalContainer.applyChanges();
    }

    private void sortWithRules() {
        //// Apply rules
        // Sorts rule by rule, themselves being already sorted by decreasing priority
        for (InvTweaksConfigSortingRule rule : rules) {
            int priority = rule.getPriority();

            // For every item in the inventory
            for (int i = 0; i < size; i++) {
                ItemStack from = containerMgr.getItemStack(i);

                // If the rule is strong enough to move the item and it matches the item, move it
                if (hasToBeMoved(i, priority) && lockPriorities != null && lockPriorities[i] < priority) {
                    List<IItemTreeItem> fromItems = tree.getItems(BuiltInRegistries.ITEM.getKey(from.getItem()).toString(), from.getDamageValue());
                    if (tree.matches(fromItems, rule.getKeyword())) {

                        // Test preferred slots
                        int[] preferredSlots = rule.getPreferredSlots();
                        if (preferredSlots == null) {
                            continue;
                        }
                        int stackToMove = i;
                        for (int k : preferredSlots) {
                            // Move the stack!
                            int moveResult = move(stackToMove, k, priority);
                            if (moveResult != -1) {
                                if (moveResult == k) {
                                    break;
                                } else {
                                    from = containerMgr.getItemStack(moveResult);
                                    fromItems = tree.getItems(BuiltInRegistries.ITEM.getKey(from.getItem()).toString(), from.getDamageValue());
                                    if (tree.matches(fromItems, rule.getKeyword())) {
                                        if (i >= moveResult) {
                                            stackToMove = moveResult;
                                        } else {
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //// Don't move locked stacks
        for (int i = 0; i < size; i++) {
            if (lockPriorities != null && hasToBeMoved(i, 1) && lockPriorities[i] > 0) {
                markAsMoved(i, 1);
            }
        }
    }

    private void sortInventory(IContainerManager globalContainer) {
        //// Move items out of the crafting slots
        if (globalContainer.hasSection(ContainerSection.CRAFTING_IN)) {
            List<Slot> craftingSlots = globalContainer.getSlots(ContainerSection.CRAFTING_IN);
            int emptyIndex = globalContainer.getFirstEmptyIndex(ContainerSection.INVENTORY);
            if (emptyIndex != -1) {
                for (Slot craftingSlot : craftingSlots) {
                    if (craftingSlot.hasItem()) {
                        globalContainer.move(ContainerSection.CRAFTING_IN, globalContainer.getSlotIndex(getSlotNumber(craftingSlot)),
                                ContainerSection.INVENTORY, emptyIndex);
                        emptyIndex = globalContainer.getFirstEmptyIndex(ContainerSection.INVENTORY);
                        if (emptyIndex == -1) {
                            break;
                        }
                    }
                }
            }
        }

        sortMergeArmor(globalContainer);
    }

    private void sortMergeArmor(IContainerManager globalContainer) {
        //// Merge stacks to fill the ones in locked slots
        //// + Move armor parts to the armor slots
        for (int i = size - 1; i >= 0; i--) {
            ItemStack from = containerMgr.getItemStack(i);
            if (!from.isEmpty()) {
                Item fromItem = from.getItem();
                if (from.isDamageableItem()) {
                    moveArmor(globalContainer, i, from, fromItem);
                }
                // Stackable objects are never damageable
                else {
                    mergeItem(i, from);
                }
            }
        }
    }

    private void mergeItem(int i, ItemStack from) {
        int j = 0;
        if (lockPriorities == null) {
            return;
        }
        for (Integer lockPriority : lockPriorities) {
            if (lockPriority > 0) {
                ItemStack to = containerMgr.getItemStack(j);
                if (!to.isEmpty() && areItemsStackable(from, to)) {
                    move(i, j, Integer.MAX_VALUE);
                    markAsNotMoved(j);
                    if (containerMgr.getItemStack(i).isEmpty()) {
                        break;
                    }
                }
            }
            j++;
        }
    }

    private void moveArmor(IContainerManager globalContainer, int i, ItemStack from, Item fromItem) {
        if (!sortArmorParts || !(fromItem instanceof ArmorItem fromArmor)) {
            return;
        }
        if (globalContainer.hasSection(ContainerSection.ARMOR)) {
            List<Slot> armorSlots = globalContainer.getSlots(ContainerSection.ARMOR);
            for (Slot slot : armorSlots) {
                boolean move = false;
                if (!slot.hasItem()) {
                    move = true;
                } else {
                    Item currentArmor = slot.getItem().getItem();
                    if (currentArmor instanceof ArmorItem currentArmorItem) {
                        int armorLevel = currentArmorItem.getDefense();
                        if (armorLevel < fromArmor.getDefense()
                                || (armorLevel == fromArmor.getDefense() && slot.getItem().getDamageValue() < from.getDamageValue())) {
                            move = true;
                        }
                    } else {
                        move = true;
                    }
                }
                if (move && slot.mayPlace(from)) {
                    globalContainer.move(ContainerSection.INVENTORY, i, ContainerSection.ARMOR,
                            globalContainer.getSlotIndex(getSlotNumber(slot)));
                }
            }
        }
    }

    private void sortEvenStacks() {
        // item and slot counts for each unique item
        Map<String, int[]> itemCounts = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = containerMgr.getItemStack(i);
            if (!stack.isEmpty()) {
                String item = itemKey(stack);
                int[] count = itemCounts.get(item);
                if (count == null) {
                    itemCounts.put(item, new int[]{stack.getCount(), 1});
                } else {
                    count[0] += stack.getCount(); // amount of item
                    count[1]++; // slots with item
                }
            }
        }

        // handle each unique item separately
        for (Entry<String, int[]> entry : itemCounts.entrySet()) {
            String item = entry.getKey();
            int[] count = entry.getValue();
            int numPerSlot = count[0] / count[1]; // totalNumber/numberOfSlots

            // skip hacked itemstacks that are larger than their max size
            if (numPerSlot <= maxStackSizeOf(item)) {
                // linked lists to store which stacks have too many/few items
                LinkedList<Integer> smallStacks = new LinkedList<>();
                LinkedList<Integer> largeStacks = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    ItemStack stack = containerMgr.getItemStack(i);
                    if (!stack.isEmpty() && itemKey(stack).equals(item)) {
                        int stackSize = stack.getCount();
                        if (stackSize > numPerSlot) {
                            largeStacks.offer(i);
                        } else if (stackSize < numPerSlot) {
                            smallStacks.offer(i);
                        }
                    }
                }

                // move items from stacks with too many to those with too little
                while (!smallStacks.isEmpty() && !largeStacks.isEmpty()) {
                    int largeIndex = largeStacks.peek();
                    int largeSize = containerMgr.getItemStack(largeIndex).getCount();
                    int smallIndex = smallStacks.peek();
                    int smallSize = containerMgr.getItemStack(smallIndex).getCount();
                    containerMgr.moveSome(largeIndex, smallIndex, Math.min(numPerSlot - smallSize, largeSize - numPerSlot));

                    // update stack lists
                    largeSize = containerMgr.getItemStack(largeIndex).getCount();
                    smallSize = containerMgr.getItemStack(smallIndex).getCount();
                    if (largeSize == numPerSlot) {
                        largeStacks.remove();
                    }
                    if (smallSize == numPerSlot) {
                        smallStacks.remove();
                    }
                }

                // put all leftover into one stack for easy removal
                while (largeStacks.size() > 1) {
                    int largeIndex = largeStacks.poll();
                    int largeSize = containerMgr.getItemStack(largeIndex).getCount();
                    containerMgr.moveSome(largeIndex, largeStacks.peek(), largeSize - numPerSlot);
                }
            }
        }

        // mark all items as moved. (is there a better way?)
        for (int i = 0; i < size; i++) {
            markAsMoved(i, 1);
        }
    }

    private static String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + stack.getDamageValue();
    }

    private static int maxStackSizeOf(String itemKey) {
        int hash = itemKey.lastIndexOf('#');
        String id = hash < 0 ? itemKey : itemKey.substring(0, hash);
        Item item = InvTweaksPlatform.getItem(id);
        return item == null ? 64 : new ItemStack(item).getMaxStackSize();
    }

    private void defaultSorting() {
        ArrayList<Integer> remaining = new ArrayList<>(), nextRemaining = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (hasToBeMoved(i, 1)) {
                remaining.add(i);
                nextRemaining.add(i);
            }
        }

        int iterations = 0;
        while (!remaining.isEmpty() && iterations++ < 50) {
            for (int i : remaining) {
                if (hasToBeMoved(i, 1)) {
                    for (int j = 0; j < size; j++) {
                        if (move(i, j, 1) != -1) {
                            nextRemaining.remove((Integer) j);
                            break;
                        }
                    }
                } else {
                    nextRemaining.remove((Integer) i);
                }
            }
            remaining.clear();
            remaining.addAll(nextRemaining);
        }
        if (iterations == 100) {
            InvTweaks.log.warn("Sorting takes too long, aborting.");
        }
    }

    private boolean canSwapSlots(int i, int j, int priority) {
        return lockPriorities != null && lockPriorities[j] <= priority
                && (rulePriority[j] < priority || (rulePriority[j] == priority && isOrderedBefore(i, j)));
    }

    /**
     * Tries to move a stack from i to j, and swaps them if j is already occupied but i is of greater priority (even if
     * they are of same ID).
     *
     * @return -1 if it failed, j if the stacks were merged into one, n if the j stack has been moved to the n slot.
     */
    private int move(int i, int j, int priority) {
        ItemStack from = containerMgr.getItemStack(i), to = containerMgr.getItemStack(j);

        if (from.isEmpty() || frozenSlots == null || lockPriorities == null || frozenSlots[j] || frozenSlots[i]) {
            return -1;
        }

        if (lockPriorities[i] <= priority) {
            if (i == j) {
                markAsMoved(i, priority);
                return j;
            }

            // Move to empty slot
            if (to.isEmpty() && lockPriorities[j] <= priority && !frozenSlots[j]) {
                rulePriority[i] = -1;
                keywordOrder[i] = -1;
                rulePriority[j] = priority;
                keywordOrder[j] = getItemOrder(from);
                if (containerMgr.move(i, j)) {
                    return j;
                } else {
                    return -1;
                }
            }

            // Try to swap/merge
            else if (!to.isEmpty()) {
                if (canSwapSlots(i, j, priority) || canMergeStacks(from, to)) {
                    keywordOrder[j] = keywordOrder[i];
                    rulePriority[j] = priority;
                    rulePriority[i] = -1;
                    boolean success = containerMgr.move(i, j);

                    if (success) {
                        ItemStack remains = containerMgr.getItemStack(i);

                        if (!remains.isEmpty()) {
                            int dropSlot = i;
                            if (lockPriorities[j] > lockPriorities[i]) {
                                for (int k = 0; k < size; k++) {
                                    if (containerMgr.getItemStack(k).isEmpty() && lockPriorities[k] == 0) {
                                        dropSlot = k;
                                        break;
                                    }
                                }
                            }
                            if (dropSlot != i) {
                                if (!containerMgr.move(i, dropSlot)) {
                                    return -1;
                                }
                            }
                            rulePriority[dropSlot] = -1;
                            keywordOrder[dropSlot] = getItemOrder(remains);
                            return dropSlot;
                        } else {
                            return j;
                        }
                    } else {
                        return -1;
                    }
                }
            }
        }

        return -1;
    }

    private void markAsMoved(int i, int priority) {
        rulePriority[i] = priority;
    }

    private void markAsNotMoved(int i) {
        rulePriority[i] = -1;
    }

    private boolean hasToBeMoved(int slot, int priority) {
        return !containerMgr.getItemStack(slot).isEmpty() && rulePriority[slot] <= priority;
    }

    private boolean isOrderedBefore(int i, int j) {
        ItemStack iStack = containerMgr.getItemStack(i), jStack = containerMgr.getItemStack(j);
        InvTweaks it = InvTweaks.getInstance();
        return it != null && it.compareItems(iStack, jStack, keywordOrder[i], keywordOrder[j]) < 0;
    }

    private int getItemOrder(ItemStack itemStack) {
        List<IItemTreeItem> items = tree.getItems(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), itemStack.getDamageValue());
        return (items.size() > 0) ? items.get(0).getOrder() : Integer.MAX_VALUE;
    }

    private void computeLineSortingRules(int rowSize, boolean horizontal) {
        // Abort if rowSize is too great.
        if (rowSize > 9) {
            return;
        }

        rules = new ArrayList<>();

        Map<IItemTreeItem, Integer> stats = computeContainerStats();
        List<IItemTreeItem> itemOrder = new ArrayList<>();

        int distinctItems = stats.size();
        int columnSize = getContainerColumnSize(rowSize);
        int spaceWidth;
        int spaceHeight;
        int availableSlots = size;
        int remainingStacks = 0;
        for (Integer stacks : stats.values()) {
            remainingStacks += stacks;
        }

        // No need to compute rules for an empty chest
        if (distinctItems == 0) {
            return;
        }

        // (Partially) sort stats by decreasing item stack count
        List<IItemTreeItem> unorderedItems = new ArrayList<>(stats.keySet());
        boolean hasStacksToOrderFirst = true;
        while (hasStacksToOrderFirst) {
            hasStacksToOrderFirst = false;
            for (IItemTreeItem item : unorderedItems) {
                Integer value = stats.get(item);
                if (value > ((horizontal) ? rowSize : columnSize) && !itemOrder.contains(item)) {
                    hasStacksToOrderFirst = true;
                    itemOrder.add(item);
                    unorderedItems.remove(item);
                    break;
                }
            }
        }
        unorderedItems.sort(Collections.reverseOrder());
        itemOrder.addAll(unorderedItems);

        // Define space size used for each item type.
        if (horizontal) {
            spaceHeight = 1;
            spaceWidth = Math.max(1, rowSize / ((distinctItems + columnSize - 1) / columnSize));
        } else {
            spaceWidth = 1;
            spaceHeight = Math.max(1, columnSize / ((distinctItems + rowSize - 1) / rowSize));
        }

        char row = 'a', maxRow = (char) (row - 1 + columnSize);
        char column = '1', maxColumn = (char) (column - 1 + rowSize);

        // Create rules
        for (IItemTreeItem item : itemOrder) {
            // Adapt rule dimensions to fit the amount
            int thisSpaceWidth = spaceWidth, thisSpaceHeight = spaceHeight;
            while (stats.get(item) > thisSpaceHeight * thisSpaceWidth) {
                if (horizontal) {
                    if (column + thisSpaceWidth < maxColumn) {
                        thisSpaceWidth = maxColumn - column + 1;
                    } else if (row + thisSpaceHeight < maxRow) {
                        thisSpaceHeight++;
                    } else {
                        break;
                    }
                } else {
                    if (row + thisSpaceHeight < maxRow) {
                        thisSpaceHeight = maxRow - row + 1;
                    } else if (column + thisSpaceWidth < maxColumn) {
                        thisSpaceWidth++;
                    } else {
                        break;
                    }
                }
            }

            // Adjust line/column ends to fill empty space
            if (horizontal && (column + thisSpaceWidth == maxColumn)) {
                thisSpaceWidth++;
            } else if (!horizontal && row + thisSpaceHeight == maxRow) {
                thisSpaceHeight++;
            }

            // Create rule
            String constraint = String.format("%c%c-%c%c", row, column, (char) (row - 1 + thisSpaceHeight), (char) (column - 1 + thisSpaceWidth));
            if (!horizontal) {
                constraint += 'v';
            }
            rules.add(new InvTweaksConfigSortingRule(tree, constraint, item.getName(), size, rowSize));

            // Check if there's still room for more rules
            availableSlots -= thisSpaceHeight * thisSpaceWidth;
            remainingStacks -= stats.get(item);
            if (availableSlots >= remainingStacks) {
                // Move origin for next rule
                if (horizontal) {
                    if (column + thisSpaceWidth + spaceWidth <= maxColumn + 1) {
                        column += thisSpaceWidth;
                    } else {
                        column = '1';
                        row += thisSpaceHeight;
                    }
                } else {
                    if (row + thisSpaceHeight + spaceHeight <= maxRow + 1) {
                        row += thisSpaceHeight;
                    } else {
                        row = 'a';
                        column += thisSpaceWidth;
                    }
                }
                if (row > maxRow || column > maxColumn) {
                    break;
                }
            } else {
                break;
            }
        }

        String defaultRule;
        if (horizontal) {
            defaultRule = maxRow + "1-a" + maxColumn;
        } else {
            defaultRule = "a" + maxColumn + "-" + maxRow + "1v";
        }
        if (tree.getRootCategory() != null) {
            rules.add(new InvTweaksConfigSortingRule(tree, defaultRule, tree.getRootCategory().getName(), size, rowSize));
        }
    }

    private Map<IItemTreeItem, Integer> computeContainerStats() {
        Map<IItemTreeItem, Integer> stats = new HashMap<>();
        Map<Integer, IItemTreeItem> itemSearch = new HashMap<>();

        for (int i = 0; i < size; i++) {
            ItemStack stack = containerMgr.getItemStack(i);
            if (!stack.isEmpty()) {
                int itemSearchKey = BuiltInRegistries.ITEM.getId(stack.getItem()) * 100000 + ((stack.getMaxStackSize() != 1) ? stack.getDamageValue() : 0);
                IItemTreeItem item = itemSearch.get(itemSearchKey);
                if (item == null) {
                    List<IItemTreeItem> items = tree.getItems(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue());
                    if (items.isEmpty()) {
                        continue;
                    }
                    item = items.get(0);
                    itemSearch.put(itemSearchKey, item);
                    stats.put(item, 1);
                } else {
                    stats.put(item, stats.get(item) + 1);
                }
            }
        }

        return stats;
    }

    private int getContainerColumnSize(int rowSize) {
        return Math.max(1, size / rowSize);
    }
}
