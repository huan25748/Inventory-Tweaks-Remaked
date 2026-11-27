package com.inventorytweaks.remaked;


import java.util.Objects;

/**
 * Representation of an item in the item tree.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTreeItem implements IItemTreeItem {

    private final String name;
    private final String id;
    private final int damage;
    private final int order;
    private final String path;

    /**
     * @param name_   The item name
     * @param id_     The item ID
     * @param damage_ The item variant or InvTweaksConst.DAMAGE_WILDCARD
     * @param order_  The item order while sorting
     */
    public InvTweaksItemTreeItem(String name_, String id_, int damage_, int order_, String path_) {
        name = name_;
        id = InvTweaksPlatform.getNamespacedID(id_);
        damage = damage_;
        order = order_;
        path = path_;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getPath() {
        return path;
    }

    /**
     * Warning: the item equality is not reflective. They are equal if "o" matches the item constraints (the opposite
     * can be false).
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IItemTreeItem item)) {
            return false;
        }
        return Objects.equals(id, item.getId()) && (damage == InvTweaksConst.DAMAGE_WILDCARD || damage == item.getDamage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, damage);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(IItemTreeItem item) {
        return item.getOrder() - order;
    }
}
