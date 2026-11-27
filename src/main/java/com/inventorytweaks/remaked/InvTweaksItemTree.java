package com.inventorytweaks.remaked;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Contains the whole hierarchy of categories and items, as defined in the XML item tree. Is used to recognize keywords
 * and store item orders.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTree implements IItemTree {
    public static final String UNKNOWN_ITEM = "unknown";

    private static List<IItemTreeItem> defaultItems = null;
    /**
     * All categories, stored by name
     */
    private final Map<String, IItemTreeCategory> categories = new HashMap<>();
    /**
     * Items stored by ID. A same ID can hold several names.
     */
    private final Map<String, List<IItemTreeItem>> itemsById = new HashMap<>(500);
    /**
     * Items stored by name. A same name can match several IDs.
     */
    private final Map<String, List<IItemTreeItem>> itemsByName = new HashMap<>(500);

    private String rootCategory;

    private final List<ItemStack> allGameItems = new ArrayList<>();

    private int highestOrder = 0;

    private final int lastTreeOrder = 0;

    public InvTweaksItemTree() {
        reset();
    }

    public void reset() {
        if (defaultItems == null) {
            defaultItems = new ArrayList<>();
            defaultItems.add(new InvTweaksItemTreeItem(UNKNOWN_ITEM, null, InvTweaksConst.DAMAGE_WILDCARD, Integer.MAX_VALUE, UNKNOWN_ITEM));
        }

        categories.clear();
        itemsByName.clear();
        itemsById.clear();
    }

    /**
     * Checks if given item ID matches a given keyword (either the item's name is the keyword, or it is in the keyword
     * category)
     */
    @Override
    public boolean matches(List<IItemTreeItem> items, String keyword) {
        if (items == null) {
            return false;
        }

        // The keyword is an item
        for (IItemTreeItem item : items) {
            if (item.getName() != null && item.getName().equals(keyword)) {
                return true;
            }
        }

        // The keyword is a category
        IItemTreeCategory category = getCategory(keyword);
        if (category != null) {
            for (IItemTreeItem item : items) {
                if (category.contains(item)) {
                    return true;
                }
            }
        }

        // Everything is stuff
        return keyword.equals(rootCategory);
    }

    @Override
    public int getKeywordDepth(String keyword) {
        try {
            return Objects.requireNonNull(getRootCategory()).findKeywordDepth(keyword);
        } catch (NullPointerException e) {
            InvTweaks.log.error("The root category is missing: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int getKeywordOrder(String keyword) {
        List<IItemTreeItem> items = getItems(keyword);
        if (items != null && !items.isEmpty()) {
            return items.get(0).getOrder();
        } else {
            try {
                return Objects.requireNonNull(getRootCategory()).findCategoryOrder(keyword);
            } catch (NullPointerException e) {
                InvTweaks.log.error("The root category is missing: " + e.getMessage());
                return -1;
            }
        }
    }

    /**
     * Checks if the given keyword is valid (i.e. represents either a registered item or a registered category)
     */
    @Override
    public boolean isKeywordValid(String keyword) {
        return containsItem(keyword) || getCategory(keyword) != null;
    }

    @Override
    public Collection<IItemTreeCategory> getAllCategories() {
        return categories.values();
    }

    @Override
    public IItemTreeCategory getRootCategory() {
        return categories.get(rootCategory);
    }

    @Override
    public void setRootCategory(IItemTreeCategory category) {
        rootCategory = category.getName();
        categories.put(rootCategory, category);
    }

    @Override
    public IItemTreeCategory getCategory(String keyword) {
        return categories.get(keyword);
    }

    @Override
    public boolean isItemUnknown(String id, int damage) {
        return itemsById.get(id) == null;
    }

    @Override
    public List<IItemTreeItem> getItems(String id, int damage) {
        if (id == null) {
            return new ArrayList<>();
        }

        List<IItemTreeItem> items = itemsById.get(id);
        List<IItemTreeItem> filteredItems = new ArrayList<>();
        if (items != null) {
            filteredItems.addAll(items);
            // Filter items of same ID, but different damage value
            filteredItems.removeIf(item -> item.getDamage() != InvTweaksConst.DAMAGE_WILDCARD && item.getDamage() != damage);
        }

        // If there's no matching item, create new ones
        if (filteredItems.isEmpty()) {
            IItemTreeCategory root = getRootCategory();
            String rootName = (root == null) ? UNKNOWN_ITEM : root.getName();
            int newItemOrder = highestOrder + 1;
            IItemTreeItem newItemId = new InvTweaksItemTreeItem(String.format("%s-%d", id, damage), id, damage, newItemOrder,
                    rootName + "\\_uncategorized\\" + String.format("%s-%d", id, damage));
            IItemTreeItem newItemDamage = new InvTweaksItemTreeItem(id, id, InvTweaksConst.DAMAGE_WILDCARD, newItemOrder,
                    rootName + "\\_uncategorized\\" + id);
            if (root != null) {
                addItem(rootName, newItemId);
                addItem(rootName, newItemDamage);
            }
            filteredItems.add(newItemId);
            filteredItems.add(newItemDamage);
        }

        filteredItems.removeIf(Objects::isNull);
        return filteredItems;
    }

    @Override
    public List<IItemTreeItem> getItems(String name) {
        return itemsByName.get(name);
    }

    @Override
    public IItemTreeItem getRandomItem(Random r) {
        Object[] values = itemsByName.values().toArray();
        @SuppressWarnings("unchecked")
        List<IItemTreeItem> picked = (List<IItemTreeItem>) values[r.nextInt(values.length)];
        return picked.get(0);
    }

    @Override
    public boolean containsItem(String name) {
        return itemsByName.containsKey(name);
    }

    @Override
    public boolean containsCategory(String name) {
        return categories.containsKey(name);
    }

    @Override
    public IItemTreeCategory addCategory(String parentCategory, String newCategory) throws NullPointerException {
        IItemTreeCategory addedCategory = new InvTweaksItemTreeCategory(newCategory);
        addCategory(parentCategory, addedCategory);
        return addedCategory;
    }

    @Override
    public IItemTreeItem addItem(String parentCategory, String name, String id, int damage, int order) throws NullPointerException {
        IItemTreeCategory parent = getRootCategory();
        String path = (parent == null ? "" : parent.findKeywordPath(parentCategory)) + "\\" + name;
        InvTweaksItemTreeItem addedItem = new InvTweaksItemTreeItem(name, id, damage, order, path);
        addItem(parentCategory, addedItem);
        return addedItem;
    }

    @Override
    public void addCategory(String parentCategory, IItemTreeCategory newCategory) throws NullPointerException {
        IItemTreeCategory parent = categories.get(parentCategory);
        if (parent == null) {
            throw new NullPointerException("Unknown parent category: " + parentCategory);
        }
        parent.addCategory(newCategory);
        categories.put(newCategory.getName(), newCategory);
    }

    @Override
    public void addItem(String parentCategory, IItemTreeItem newItem) throws NullPointerException {
        highestOrder = Math.max(highestOrder, newItem.getOrder());

        IItemTreeCategory parent = categories.get(parentCategory);
        if (parent == null) {
            throw new NullPointerException("Unknown parent category: " + parentCategory);
        }
        parent.addItem(newItem);

        itemsByName.computeIfAbsent(newItem.getName(), k -> new ArrayList<>()).add(newItem);
        String id = newItem.getId();
        if (id != null) {
            itemsById.computeIfAbsent(id, k -> new ArrayList<>()).add(newItem);
        }
    }

    public int getHighestOrder() {
        return highestOrder;
    }

    public int getLastTreeOrder() {
        return lastTreeOrder;
    }

    @Override
    public void registerOre(String category, String name, String oreName, int order, String path) {
        // The 1.12 OreDictionary does not exist anymore. Ore entries are resolved through item tags,
        // which the tree loader performs directly when a tag attribute is present.
    }

    /**
     * Registers every in-game item matching the given legacy class name (and optional data constraints).
     */
    public void registerClass(String category, String name, String className, Map<String, String> extraData, int order, String path) {
        if (allGameItems.isEmpty()) {
            populateGameItems();
        }

        for (ItemStack stack : allGameItems) {
            Item item = stack.getItem();
            if (instanceOfClassNameKind(item, className) && matchesExtraData(stack, item, extraData)) {
                int dmg = stack.isDamageableItem() ? InvTweaksConst.DAMAGE_WILDCARD : stack.getDamageValue();
                addItem(category, new InvTweaksItemTreeItem(name, BuiltInRegistries.ITEM.getKey(item).toString(), dmg, order, path));
            }
        }
    }

    private static boolean matchesExtraData(ItemStack stack, Item item, Map<String, String> extraData) {
        if (extraData == null || extraData.isEmpty()) {
            return true;
        }
        String toolClass = extraData.get("toolclass");
        if (toolClass != null && !toolClass.equals(InvTweaks.getToolClass(stack))) {
            return false;
        }
        String armorType = extraData.get("armortype");
        if (armorType != null) {
            if (!(item instanceof ArmorItem armor)) {
                return false;
            }
            if (!armorType.equals(armor.getEquipmentSlot().getName())) {
                return false;
            }
        }
        String isShield = extraData.get("isshield");
        if (isShield != null && !(item instanceof ShieldItem)) {
            return false;
        }
        return true;
    }

    private static boolean instanceOfClassNameKind(Item item, String className) {
        String name = className.toLowerCase();
        if (name.contains("armor")) {
            return item instanceof ArmorItem;
        }
        if (name.contains("sword")) {
            return item instanceof SwordItem;
        }
        if (name.contains("bow")) {
            return item instanceof BowItem || item instanceof CrossbowItem;
        }
        if (name.contains("hoe")) {
            return item instanceof HoeItem;
        }
        if (name.contains("shears")) {
            return item instanceof ShearsItem;
        }
        if (name.contains("pickaxe")) {
            return item instanceof PickaxeItem;
        }
        if (name.contains("shovel")) {
            return item instanceof ShovelItem;
        }
        if (name.contains("axe")) {
            return item instanceof AxeItem;
        }
        if (name.contains("arrow")) {
            return item instanceof ArrowItem;
        }
        if (name.contains("fishing")) {
            return item instanceof FishingRodItem;
        }
        // "Item" matches everything, as in 1.12.
        return true;
    }

    private void populateGameItems() {
        for (Item item : BuiltInRegistries.ITEM) {
            allGameItems.add(item.getDefaultInstance());
        }
    }

    public void endFileRead() {
        // Nothing to do; kept for parity with the original tree loader.
    }

    /**
     * Debug helper: dump the tree path of an item stack.
     */
    public String getItemPath(ItemStack stack) {
        List<IItemTreeItem> items = getItems(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue());
        return items.isEmpty() ? UNKNOWN_ITEM : items.get(0).getPath();
    }
}
