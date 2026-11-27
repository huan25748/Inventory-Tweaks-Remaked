package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles the auto-refilling of the hotbar.
 *
 * @author Jimeo Wan
 */
public class InvTweaksHandlerAutoRefill extends InvTweaksPlatform {

    private InvTweaksConfig config;

    public InvTweaksHandlerAutoRefill(Minecraft mc_, InvTweaksConfig config_) {
        super(mc_);
        config = config_;
    }

    public void setConfig(InvTweaksConfig config_) {
        config = config_;
    }

    /**
     * Auto-refill: replaces the item in the given hotbar slot by a matching one from the inventory.
     *
     * @param slot         the section index of the hotbar slot (27..35)
     * @param wantedId     the registry name of the item to replace
     * @param wantedDamage the damage value of the item to replace
     */
    public void autoRefillSlot(int slot, String wantedId, int wantedDamage) throws Exception {
        ContainerSectionManager container = new ContainerSectionManager(ContainerSection.INVENTORY);
        ItemStack candidateStack;
        ItemStack replacementStack = ItemStack.EMPTY;
        int replacementStackSlot = -1;
        boolean refillBeforeBreak = config.getProperty(InvTweaksConfig.PROP_AUTO_REFILL_BEFORE_BREAK).equals(InvTweaksConfig.VALUE_TRUE);
        boolean hasSubtypes = false;

        Item original = InvTweaksPlatform.getItem(wantedId);
        if (original != null) {
            hasSubtypes = false;
        }

        List<InvTweaksConfigSortingRule> matchingRules = new ArrayList<>();
        List<InvTweaksConfigSortingRule> rules = config.getRules();
        InvTweaksItemTree tree = config.getTree();

        // Check that the item is in the tree
        if (!tree.isItemUnknown(wantedId, wantedDamage)) {
            //// Search replacement
            List<IItemTreeItem> items = tree.getItems(wantedId, wantedDamage);

            // Find rules that match the slot
            for (IItemTreeItem item : items) {
                if (item.getDamage() == wantedDamage || (!hasSubtypes && item.getDamage() == InvTweaksConst.DAMAGE_WILDCARD)) {
                    matchingRules.add(new InvTweaksConfigSortingRule(tree, "D" + (slot - 26), item.getName(),
                            InvTweaksConst.INVENTORY_SIZE, InvTweaksConst.INVENTORY_ROW_SIZE));
                }
            }

            // Fallback to wildcard entry for items with subtypes only if no other entries are found
            if (matchingRules.isEmpty()) {
                for (IItemTreeItem item : items) {
                    if (item.getDamage() == InvTweaksConst.DAMAGE_WILDCARD) {
                        matchingRules.add(new InvTweaksConfigSortingRule(tree, "D" + (slot - 26), item.getName(),
                                InvTweaksConst.INVENTORY_SIZE, InvTweaksConst.INVENTORY_ROW_SIZE));
                    }
                }
            }

            for (InvTweaksConfigSortingRule rule : rules) {
                if (rule.getType() == InvTweaksConfigSortingRuleType.SLOT || rule.getType() == InvTweaksConfigSortingRuleType.COLUMN) {
                    int[] preferredSlots = rule.getPreferredSlots();
                    if (preferredSlots == null) {
                        continue;
                    }
                    for (int preferredSlot : preferredSlots) {
                        if (slot == preferredSlot) {
                            matchingRules.add(rule);
                            break;
                        }
                    }
                }
            }

            // Look only for a matching stack
            // First, look for the same item, else one that matches the slot's rules
            for (InvTweaksConfigSortingRule rule : matchingRules) {
                for (int i = 0; i < InvTweaksConst.INVENTORY_SIZE; i++) {
                    candidateStack = container.getItemStack(i);
                    if (!candidateStack.isEmpty()) {
                        List<IItemTreeItem> candidateItems = tree.getItems(
                                BuiltInRegistries.ITEM.getKey(candidateStack.getItem()).toString(), candidateStack.getDamageValue());
                        if (tree.matches(candidateItems, rule.getKeyword())) {
                            // Choose tool of highest damage value
                            if (candidateStack.getMaxStackSize() == 1) {
                                if ((replacementStack.isEmpty() || candidateStack.getDamageValue() > replacementStack.getDamageValue())
                                        && (!refillBeforeBreak || candidateStack.getMaxDamage() - candidateStack.getDamageValue()
                                        > config.getIntProperty(InvTweaksConfig.PROP_AUTO_REFILL_DAMAGE_THRESHHOLD))) {
                                    replacementStack = candidateStack;
                                    replacementStackSlot = i;
                                }
                            }
                            // Choose stack of lowest size
                            else if (replacementStack.isEmpty() || candidateStack.getCount() < replacementStack.getCount()) {
                                replacementStack = candidateStack;
                                replacementStackSlot = i;
                            }
                        }
                    }
                }
            }
        }

        // If item is unknown, look for exact same item
        else {
            for (int i = 0; i < InvTweaksConst.INVENTORY_SIZE; i++) {
                candidateStack = container.getItemStack(i);
                if (!candidateStack.isEmpty()
                        && Objects.equals(BuiltInRegistries.ITEM.getKey(candidateStack.getItem()).toString(), wantedId)
                        && candidateStack.getDamageValue() == wantedDamage) {
                    replacementStack = candidateStack;
                    replacementStackSlot = i;
                    break;
                }
            }
        }

        //// Proceed to replacement
        if (!replacementStack.isEmpty() || (refillBeforeBreak && !container.getSlots().get(slot).getItem().isEmpty())) {
            InvTweaks.log.info("Automatic stack replacement.");

            final int replacementSlot = replacementStackSlot;
            final boolean beforeBreak = refillBeforeBreak;
            InvTweaks it = InvTweaks.getInstance();
            if (it == null) {
                return;
            }
            it.addScheduledTask(() -> {
                try {
                    ContainerSectionManager containerMgr = new ContainerSectionManager(ContainerSection.INVENTORY);
                    int targetedSlot = slot;
                    int sourceIndex;
                    String expectedItemId;
                    if (replacementSlot != -1) {
                        sourceIndex = replacementSlot;
                        expectedItemId = BuiltInRegistries.ITEM.getKey(containerMgr.getItemStack(sourceIndex).getItem()).toString();
                    } else {
                        sourceIndex = containerMgr.getFirstEmptyIndex();
                        expectedItemId = null;
                    }

                    if (sourceIndex < 0 || targetedSlot < 0) {
                        return;
                    }

                    ItemStack stack = containerMgr.getItemStack(sourceIndex);

                    if ((!stack.isEmpty() && Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), expectedItemId)) || beforeBreak) {
                        if (containerMgr.move(targetedSlot, sourceIndex) || containerMgr.move(sourceIndex, targetedSlot)) {
                            if (!config.getProperty(InvTweaksConfig.PROP_ENABLE_SOUNDS).equals(InvTweaksConfig.VALUE_FALSE)) {
                                InvTweaks inst = InvTweaks.getInstance();
                                if (inst != null) {
                                    inst.playRefillSound();
                                }
                            }
                            // If items are swapped (like for mushroom soups),
                            // put the item back in the inventory if it is in the hotbar
                            if (!containerMgr.getItemStack(sourceIndex).isEmpty() && sourceIndex >= 27) {
                                for (int j = 0; j < InvTweaksConst.INVENTORY_SIZE; j++) {
                                    if (containerMgr.getItemStack(j).isEmpty()) {
                                        containerMgr.move(sourceIndex, j);
                                        break;
                                    }
                                }
                            }

                            // Make sure the inventory resyncs
                            containerMgr.applyChanges();
                        } else {
                            InvTweaks.log.warn("Failed to move stack for autoreplace, despite of prior tests.");
                        }
                    }
                } catch (Exception e) {
                    InvTweaks.log.error("Auto-refill failed", e);
                }
            });
        }
    }
}
