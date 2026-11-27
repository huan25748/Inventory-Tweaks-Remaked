package com.inventorytweaks.remaked;

import net.minecraft.world.item.ItemStack;

/**
 * Implementation of the public Inventory Tweaks API (invtweaks.api.InvTweaksAPI), as exposed by the 1.12 mod.
 */
public class InvTweaksApiImpl implements InvTweaksAPI {

    @Override
    public void addOnLoadListener(IItemTreeListener listener) {
        InvTweaksItemTreeLoader.addOnLoadListener(listener);
    }

    @Override
    public boolean removeOnLoadListener(IItemTreeListener listener) {
        return InvTweaksItemTreeLoader.removeOnLoadListener(listener);
    }

    @Override
    public void setSortKeyEnabled(boolean enabled) {
        InvTweaks it = InvTweaks.getInstance();
        if (it != null) {
            it.setSortKeyEnabled(enabled);
        }
    }

    @Override
    public void setTextboxMode(boolean enabled) {
        InvTweaks it = InvTweaks.getInstance();
        if (it != null) {
            it.setTextboxMode(enabled);
        }
    }

    @Override
    public int compareItems(ItemStack i, ItemStack j) {
        InvTweaks it = InvTweaks.getInstance();
        if (it == null) {
            return 0;
        }
        return it.compareItems(i, j, true);
    }

    @Override
    public void sort(ContainerSection section, SortingMethod method) {
        InvTweaks it = InvTweaks.getInstance();
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        if (it == null || manager == null || manager.getConfig() == null) {
            return;
        }
        try {
            new InvTweaksHandlerSorting(it.mc, manager.getConfig(), section, method, InvTweaksConst.INVENTORY_ROW_SIZE).sort();
        } catch (Exception e) {
            InvTweaks.log.error("Failed to sort " + section, e);
        }
    }
}
