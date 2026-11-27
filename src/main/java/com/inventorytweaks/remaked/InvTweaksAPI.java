package com.inventorytweaks.remaked;

import net.minecraft.world.item.ItemStack;

/**
 * Interface to access functions exposed by Inventory Tweaks.
 * The main @Mod instance offers {@code InvTweaksAPI.getInstance()}.
 */
@SuppressWarnings("unused")
public interface InvTweaksAPI {
    void addOnLoadListener(IItemTreeListener listener);

    boolean removeOnLoadListener(IItemTreeListener listener);

    void setSortKeyEnabled(boolean enabled);

    void setTextboxMode(boolean enabled);

    int compareItems(ItemStack i, ItemStack j);

    void sort(ContainerSection section, SortingMethod method);
}
