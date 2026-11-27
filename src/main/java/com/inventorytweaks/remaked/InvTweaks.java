package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Main class for Inventory Tweaks, which maintains various hooks and dispatches the events to the correct handlers.
 *
 * @author Jimeo Wan
 * <p>
 * Contact: jimeo.wan (at) gmail (dot) com Website: https://inventory-tweaks.readthedocs.org/
 * Source code: https://github.com/kobata/inventory-tweaks
 * License: MIT
 */
public class InvTweaks extends InvTweaksPlatform {
    public static final Logger log = LoggerFactory.getLogger("InventoryTweaks");

    private static InvTweaks instance;
    private final ItemStack[] hotbarClone = new ItemStack[InvTweaksConst.INVENTORY_HOTBAR_SIZE];
    private final List<String> queuedMessages = new ArrayList<>();
    private final Deque<Runnable> scheduledTasks = new ArrayDeque<>();

    /**
     * The configuration loader.
     */
    private final InvTweaksConfigManager cfgManager;
    /**
     * Attributes to remember the status of chest sorting while using middle clicks.
     */
    private SortingMethod chestAlgorithm = SortingMethod.DEFAULT;
    private long chestAlgorithmClickTimestamp = 0;
    private boolean chestAlgorithmButtonDown = false;
    /**
     * Various information concerning the context, stored on each tick to allow for certain features (auto-refill,
     * sorting on pick up...)
     */
    private ItemStack storedStack = ItemStack.EMPTY;
    private String storedStackId = null;
    private int storedStackDamage = InvTweaksConst.DAMAGE_WILDCARD;
    private int storedFocusedSlot = -1;
    private boolean hadFocus = true, mouseWasDown = false;
    private boolean wasInGUI = false;
    /**
     * Allows to trigger some logic only every Const.POLLING_DELAY.
     */
    private int tickNumber = 0, lastPollingTickNumber = -InvTweaksConst.POLLING_DELAY;
    /**
     * Stores when the sorting key was last pressed (allows to detect long key holding)
     */
    private long sortingKeyPressedDate = 0;
    private boolean sortKeyDown = false;
    private boolean sortKeyEnabled = true;
    private boolean textboxMode = false;
    private boolean itemPickupPending = false;
    private int itemPickupTimeout = 0;
    /**
     * Debug tools:
     */
    private String mostRecentComparison = "";
    private boolean debugTree = false;

    /**
     * Creates an instance of the mod, and loads the configuration from the files, creating them if necessary.
     */
    public InvTweaks(Minecraft mc_) {
        super(mc_);

        for (int i = 0; i < hotbarClone.length; ++i) {
            hotbarClone[i] = ItemStack.EMPTY;
        }

        instance = this;

        // Load config files
        cfgManager = new InvTweaksConfigManager(mc);
        if (cfgManager.makeSureConfigurationIsLoaded()) {
            log.info("Mod initialized");
        } else {
            log.error("Mod failed to initialize!");
        }
    }

    public static void logInGameStatic(String message) {
        InvTweaks it = getInstance();
        if (it != null) {
            it.logInGame(message);
        } else {
            log.info(message);
        }
    }

    public static void logInGameErrorStatic(String message, Exception e) {
        InvTweaks it = getInstance();
        if (it != null) {
            it.logInGameError(message, e == null ? new Exception(message) : e);
        } else {
            log.error(message, e);
        }
    }

    public static InvTweaks getInstance() {
        return instance;
    }

    public static InvTweaksConfigManager getConfigManager() {
        if (instance == null) {
            return null;
        }
        return instance.cfgManager;
    }

    public static IContainerManager getContainerManager(AbstractContainerMenu container) {
        return new DirectContainerManager(container);
    }

    public static IContainerManager getCurrentContainerManager() {
        AbstractContainerMenu menu = getCurrentContainer();
        if (menu == null) {
            throw new IllegalStateException("No container is currently open");
        }
        return getContainerManager(menu);
    }

    private static String buildLogString(String message, Exception e) {
        if (e != null) {
            StackTraceElement[] trace = e.getStackTrace();
            if (trace.length == 0) {
                return buildLogString(message) + ": " + e.getMessage();
            }
            StackTraceElement exceptionLine = trace[0];
            if (exceptionLine != null && exceptionLine.getFileName() != null) {
                return buildLogString(message) + ": " + e.getMessage() + " (l" + exceptionLine.getLineNumber() + " in "
                        + exceptionLine.getFileName().replace("InvTweaks", "") + ")";
            }
            return buildLogString(message) + ": " + e.getMessage();
        }
        return buildLogString(message);
    }

    private static String buildLogString(String message) {
        return InvTweaksConst.INGAME_LOG_PREFIX + message;
    }

    private static int compareMaxDamage(ItemStack i, ItemStack j) {
        // Use durability to sort, favoring more durable items.
        int maxDamage1 = i.getMaxDamage() <= 0 ? Integer.MAX_VALUE : i.getMaxDamage();
        int maxDamage2 = j.getMaxDamage() <= 0 ? Integer.MAX_VALUE : j.getMaxDamage();
        return maxDamage2 - maxDamage1;
    }

    private static int compareCurDamage(ItemStack i, ItemStack j) {
        // Use remaining durability to sort, favoring more damaged.
        int curDamage1 = i.getDamageValue();
        int curDamage2 = j.getDamageValue();
        InvTweaksConfigManager manager = getConfigManager();
        boolean invert = manager == null || manager.getConfig() == null
                || !manager.getConfig().getProperty(InvTweaksConfig.PROP_INVERT_TOOL_DAMAGE).equals(InvTweaksConfig.VALUE_TRUE);
        if (i.isDamageableItem() && invert) {
            return curDamage2 - curDamage1;
        } else {
            return curDamage1 - curDamage2;
        }
    }

    /**
     * Returns the "tool class" of an item, as used by the item tree ("pickaxe", "axe", ...). Swords are not tools.
     */
    public static String getToolClass(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item instanceof SwordItem) {
            return "";
        }
        if (item instanceof PickaxeItem) {
            return "pickaxe";
        }
        if (item instanceof AxeItem) {
            return "axe";
        }
        if (item instanceof ShovelItem) {
            return "shovel";
        }
        if (item instanceof HoeItem) {
            return "hoe";
        }
        if (item instanceof ShearsItem) {
            return "shears";
        }
        if (item instanceof FishingRodItem) {
            return "fishingrod";
        }
        return "";
    }

    private static int toolClassPriority(String toolClass) {
        String[] prefOrder = {"pickaxe", "axe", "shovel", "hoe", "shears", "wrench", "fishingrod"};
        for (int i = 0; i < prefOrder.length; i++) {
            if (prefOrder[i].equals(toolClass)) {
                return i;
            }
        }
        return prefOrder.length;
    }

    private static int tierLevel(Item item) {
        if (item instanceof TieredItem tiered) {
            return tiered.getTier().getLevel();
        }
        return 0;
    }

    public void addScheduledTask(Runnable task) {
        synchronized (scheduledTasks) {
            scheduledTasks.add(task);
        }
    }

    /**
     * Runs the tasks that were scheduled during the previous tick. Called from the client tick handler.
     */
    public void runScheduledTasks() {
        synchronized (scheduledTasks) {
            while (!scheduledTasks.isEmpty()) {
                Runnable task = scheduledTasks.poll();
                try {
                    if (task != null) {
                        task.run();
                    }
                } catch (Exception e) {
                    log.error("Scheduled task failed", e);
                }
            }
        }
    }

    /**
     * To be called on each tick during the game (except when in a menu). Handles the auto-refill.
     */
    public void onTickInGame() {
        synchronized (this) {
            if (!onTick()) {
                return;
            }
            handleAutoRefill();
            if (wasInGUI) {
                wasInGUI = false;
                textboxMode = false;
            }
        }
    }

    /**
     * To be called on each tick when a menu is open. Handles the GUI additions and the middle clicking.
     */
    public void onTickInGUI(Screen guiScreen) {
        if (mc.player == null || mc.player.isSpectator()) {
            onTick();
            return;
        }

        synchronized (this) {
            handleMiddleClick(guiScreen); // Called before the rest to be able to trigger config reload
            if (!onTick()) {
                return;
            }
            if (isTimeForPolling()) {
                unlockKeysIfNecessary();
            }
            if (guiScreen instanceof AbstractContainerScreen<?> containerScreen) {
                InvTweaksGuiButtons.updateButtons(containerScreen);
            }
            if (!wasInGUI) {
                // Right-click is always true on initial open of GUI.
                // Ignore it to prevent erroneous trigger of shortcuts.
                mouseWasDown = true;
            }
            if (guiScreen instanceof AbstractContainerScreen<?> containerScreen) {
                handleShortcuts(containerScreen);
            }

            // Copy some info about current selected stack for auto-refill
            ItemStack currentStack = getFocusedStack();

            storedStackId = (currentStack.isEmpty()) ? null : BuiltInRegistries.ITEM.getKey(currentStack.getItem()).toString();
            storedStackDamage = (currentStack.isEmpty()) ? 0 : currentStack.getDamageValue();
            if (!wasInGUI) {
                wasInGUI = true;
            }
        }
    }

    /**
     * To be called every time the sorting key is pressed. Sorts the inventory.
     */
    public final void onSortingKeyPressed() {
        synchronized (this) {
            // Check config loading success
            InvTweaksConfigManager manager = getConfigManager();
            if (manager == null || !manager.makeSureConfigurationIsLoaded()) {
                return;
            }

            // Check current GUI
            Screen guiScreen = getCurrentScreen();
            if (guiScreen == null || (isGuiContainer(guiScreen) && isSortableContainer((AbstractContainerScreen<?>) guiScreen))) {
                // Sorting!
                handleSorting(guiScreen);
            }
        }
    }

    private static boolean isSortableContainer(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        return isValidChest(menu) || isValidInventory(menu) || showButtons(menu);
    }

    /**
     * To be called every time a stack has been picked up. Moves the picked up item in another slot that matches best the
     * current configuration.
     */
    public void onItemPickup() {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || !manager.makeSureConfigurationIsLoaded() || manager.getConfig() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();
        // Handle option to disable this feature
        if (config.getProperty(InvTweaksConfig.PROP_ENABLE_SORTING_ON_PICKUP).equals("false")) {
            itemPickupPending = false;
            return;
        }

        try {
            ContainerSectionManager containerMgr = new ContainerSectionManager(ContainerSection.INVENTORY);

            // Find stack slot (look in hotbar only).
            // We're looking for a brand new stack in the hotbar
            // (not an existing stack whose amount has been increased)
            int currentSlot = -1;
            for (int i = 0; i < InvTweaksConst.INVENTORY_HOTBAR_SIZE; i++) {
                ItemStack currentHotbarStack = containerMgr.getItemStack(i + 27);
                // Don't move already started stacks
                if (!currentHotbarStack.isEmpty() && currentHotbarStack.getPopTime() > 0 && hotbarClone[i].isEmpty()) {
                    currentSlot = i + 27;
                }
            }

            if (currentSlot != -1) {
                itemPickupPending = false;

                // Find preferred slots
                IItemTree tree = config.getTree();
                ItemStack stack = containerMgr.getItemStack(currentSlot);

                List<IItemTreeItem> items = tree.getItems(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue());

                List<Integer> preferredPositions = config.getRules().stream()
                        .filter(rule -> tree.matches(items, rule.getKeyword()))
                        .flatMapToInt(e -> Arrays.stream(e.getPreferredSlots() == null ? new int[0] : e.getPreferredSlots()))
                        .boxed().collect(Collectors.toList());

                // Find best slot for stack
                boolean hasToBeMoved = true;
                for (int newSlot : preferredPositions) {
                    // Already in the best slot!
                    if (newSlot == currentSlot) {
                        hasToBeMoved = false;
                        break;
                    }
                    // Is the slot available?
                    else if (newSlot >= 0 && newSlot < containerMgr.getSize() && containerMgr.getItemStack(newSlot).isEmpty()) {
                        if (containerMgr.move(currentSlot, newSlot)) {
                            break;
                        }
                    }
                }

                // Else, put the slot anywhere
                if (hasToBeMoved) {
                    for (int i = 0; i < containerMgr.getSize(); i++) {
                        if (containerMgr.getItemStack(i).isEmpty()) {
                            if (containerMgr.move(currentSlot, i)) {
                                break;
                            }
                        }
                    }
                }

                // Sync after pickup movements.
                containerMgr.applyChanges();
            } else {
                if (--itemPickupTimeout == 0) {
                    itemPickupPending = false;
                }
            }
        } catch (Exception e) {
            logInGameError("invtweaks.pickup.error", e);
            itemPickupPending = false;
        }
    }

    public int compareItems(ItemStack i, ItemStack j) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), false);
    }

    public int compareItems(ItemStack i, ItemStack j, boolean onlyTreeSort) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), onlyTreeSort);
    }

    int compareItems(ItemStack i, ItemStack j, int orderI, int orderJ) {
        return compareItems(i, j, orderI, orderJ, false);
    }

    int compareItems(ItemStack i, ItemStack j, int orderI, int orderJ, boolean api) {
        if (i.isEmpty() && j.isEmpty()) {
            if (debugTree) {
                mostRecentComparison = "Both stacks are Empty.";
            }
            return 0;
        } else if (j.isEmpty()) {
            if (debugTree) {
                mostRecentComparison = "J is Empty.";
            }
            return -1;
        } else if (i.isEmpty() || orderI == -1) {
            if (debugTree) {
                mostRecentComparison = "I is Empty or orderI was -1.";
            }
            return 1;
        } else {
            if (debugTree) {
                mostRecentComparison = "";
            }
            InvTweaksConfigManager manager = getConfigManager();
            if (api && manager != null && manager.getConfig() != null && manager.getConfig().getTree() != null) {
                int lastOrder = manager.getConfig().getTree().getLastTreeOrder();
                if (orderI > lastOrder) {
                    orderI = Integer.MAX_VALUE;
                }
                if (orderJ > lastOrder) {
                    orderJ = Integer.MAX_VALUE;
                }
            }

            if (orderI != orderJ) {
                return orderI - orderJ;
            }

            // Allow external sorting systems to take control of unsorted items not handled by the tree.
            if (orderI == Integer.MAX_VALUE && orderJ == Integer.MAX_VALUE && api) {
                return 0;
            }

            Item iItem = i.getItem(), jItem = j.getItem();
            // Sort By Tool type then Harvest Level, (Better first.)
            int cTool = compareTools(i, j);
            if (cTool != 0) {
                return cTool;
            }

            // Sort by main-hand damage capability
            int cSword = compareSword(i, j);
            if (cSword != 0) {
                return cSword;
            }

            // Sort By Armor utility: (More First)
            int cArmor = compareArmor(i, j);
            if (cArmor != 0) {
                return cArmor;
            }

            // Sort by display name
            int cName = compareNames(i, j);
            if (cName != 0) {
                return cName;
            }

            // Sort by enchantments
            int cEnchant = compareEnchantment(i, j);
            if (cEnchant != 0) {
                return cEnchant;
            }

            int maxDamage = compareMaxDamage(i, j);
            if (maxDamage != 0) {
                return maxDamage;
            }

            int curDamage = compareCurDamage(i, j);
            if (curDamage != 0) {
                return curDamage;
            }

            // Use stack size to put bigger stacks first.
            if (j.getCount() != i.getCount()) {
                return j.getCount() - i.getCount();
            }

            // Final catch all
            String iId = BuiltInRegistries.ITEM.getKey(iItem).toString();
            String jId = BuiltInRegistries.ITEM.getKey(jItem).toString();
            return iId.compareTo(jId);
        }
    }

    private int compareTools(ItemStack i, ItemStack j) {
        String toolClass1 = getToolClass(i);
        String toolClass2 = getToolClass(j);
        if (toolClass1.isEmpty() || toolClass2.isEmpty()) {
            // One of the two is not a tool: tools first
            return Boolean.compare(!toolClass2.isEmpty(), !toolClass1.isEmpty());
        }
        if (!toolClass1.equals(toolClass2)) {
            return toolClassPriority(toolClass1) - toolClassPriority(toolClass2);
        }
        return Integer.compare(tierLevel(j.getItem()), tierLevel(i.getItem()));
    }

    private int compareSword(ItemStack itemStack1, ItemStack itemStack2) {
        boolean weapon1 = isWeapon(itemStack1.getItem());
        boolean weapon2 = isWeapon(itemStack2.getItem());
        if (!weapon1 || !weapon2) {
            return Boolean.compare(weapon2, weapon1);
        }
        int damage1 = weaponDamage(itemStack1.getItem());
        int damage2 = weaponDamage(itemStack2.getItem());
        if (damage1 != damage2) {
            return damage2 - damage1;
        }
        return compareMaxDamage(itemStack1, itemStack2);
    }

    private static boolean isWeapon(Item item) {
        return item instanceof SwordItem || item instanceof AxeItem;
    }

    private static int weaponDamage(Item item) {
        if (item instanceof TieredItem tiered) {
            return (int) (tiered.getTier().getAttackDamageBonus() * 10);
        }
        return 0;
    }

    private int compareArmor(ItemStack i, ItemStack j) {
        boolean isArmor1 = i.getItem() instanceof net.minecraft.world.item.ArmorItem;
        boolean isArmor2 = j.getItem() instanceof net.minecraft.world.item.ArmorItem;
        if (!isArmor1 || !isArmor2) {
            return Boolean.compare(isArmor2, isArmor1);
        }
        net.minecraft.world.item.ArmorItem a1 = (net.minecraft.world.item.ArmorItem) i.getItem();
        net.minecraft.world.item.ArmorItem a2 = (net.minecraft.world.item.ArmorItem) j.getItem();
        if (a1.getEquipmentSlot() != a2.getEquipmentSlot()) {
            return a2.getEquipmentSlot().compareTo(a1.getEquipmentSlot());
        }
        int defense1 = a1.getDefense();
        int defense2 = a2.getDefense();
        if (defense1 != defense2) {
            return defense2 - defense1;
        }
        return compareMaxDamage(i, j);
    }

    private int compareNames(ItemStack i, ItemStack j) {
        boolean iHasName = i.hasCustomHoverName();
        boolean jHasName = j.hasCustomHoverName();
        String iDisplayName = i.getHoverName().getString();
        String jDisplayName = j.getHoverName().getString();

        // Custom named items come first.
        if (iHasName || jHasName) {
            if (!iHasName) {
                return -1;
            } else if (!jHasName) {
                return 1;
            }
        }
        if (!iDisplayName.equals(jDisplayName)) {
            return iDisplayName.compareTo(jDisplayName);
        }
        return 0;
    }

    private int compareEnchantment(ItemStack i, ItemStack j) {
        int iEnchs = enchantmentCount(i);
        int jEnchs = enchantmentCount(j);

        // Pick the item with the most enchantments first.
        if (iEnchs != jEnchs) {
            return jEnchs - iEnchs;
        }
        return 0;
    }

    private static int enchantmentCount(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).size();
    }

    public void setItemPickupPending(boolean value) {
        itemPickupPending = value;
        itemPickupTimeout = 5;
    }

    public void setSortKeyEnabled(boolean enabled) {
        sortKeyEnabled = enabled;
    }

    public void setTextboxMode(boolean enabled) {
        textboxMode = enabled;
    }

    public void logInGame(String message) {
        logInGame(message, false);
    }

    public void printQueuedMessages() {
        if (mc.player != null && !queuedMessages.isEmpty()) {
            queuedMessages.forEach(this::addChatMessage);
            queuedMessages.clear();
        }
    }

    public void logInGame(String message, boolean alreadyTranslated) {
        String formattedMsg = buildLogString((alreadyTranslated) ? message : InvTweaksText.get(message));

        if (mc.player == null) {
            queuedMessages.add(formattedMsg);
        } else {
            addChatMessage(formattedMsg);
        }

        log.info(formattedMsg);
    }

    public void logInGameError(String message, Exception e) {
        String formattedMsg = buildLogString(InvTweaksText.get(message), e);
        log.error(formattedMsg, e);

        if (mc.player == null) {
            queuedMessages.add(formattedMsg);
        } else {
            addChatMessage(formattedMsg);
        }
    }

    private boolean onTick() {
        printQueuedMessages();
        runScheduledTasks();

        tickNumber++;

        if (mc.player == null || mc.player.isSpectator()) {
            return false;
        }

        InvTweaksConfigManager manager = getConfigManager();
        InvTweaksConfig config = (manager == null) ? null : manager.getConfig();
        if (config == null) {
            return false;
        }

        // Clone the hotbar to be able to monitor changes on it
        if (itemPickupPending) {
            onItemPickup();
        }
        Screen currentScreen = getCurrentScreen();
        if (currentScreen == null || isGuiInventory(currentScreen)) {
            cloneHotbar();
        }

        // Handle sort key
        if (isSortingShortcutDown()) {
            if (!sortKeyDown) {
                sortKeyDown = true;
                onSortingKeyPressed();
            }
        } else {
            sortKeyDown = false;
        }

        // Handle config switch
        handleConfigSwitch();

        return true;
    }

    private void handleConfigSwitch() {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null || manager.getShortcutsHandler() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();
        Screen currentScreen = getCurrentScreen();

        // Switch between configurations (shortcut)
        manager.getShortcutsHandler().updatePressedKeys();
        InvTweaksShortcutMapping switchMapping = manager.getShortcutsHandler().isShortcutDown(InvTweaksShortcutType.MOVE_TO_SPECIFIC_HOTBAR_SLOT);
        if (isSortingShortcutDown() && switchMapping != null && !switchMapping.getKeyCodes().isEmpty()) {
            String newRuleset = null;
            int pressedKey = switchMapping.getKeyCodes().get(0);
            int firstDigit = Keys.getCode("1");
            int lastDigit = Keys.getCode("9");
            if (pressedKey >= firstDigit && pressedKey <= lastDigit) {
                newRuleset = config.switchConfig(pressedKey - firstDigit);
            } else {
                int numpad1 = Keys.getCode("NUMPAD1");
                if (pressedKey >= numpad1 && pressedKey <= numpad1 + 8) {
                    newRuleset = config.switchConfig(pressedKey - numpad1);
                }
            }

            if (newRuleset != null) {
                logInGame(String.format(InvTweaksText.get("invtweaks.loadconfig.enabled"), newRuleset), true);
                // Hack to prevent 2nd way to switch configs from being enabled
                sortingKeyPressedDate = Integer.MAX_VALUE;
            }
        }

        // Switch between configurations (by holding the sorting key)
        if (isSortingShortcutDown()) {
            long currentTime = System.currentTimeMillis();
            if (sortingKeyPressedDate == 0) {
                sortingKeyPressedDate = currentTime;
            } else if (currentTime - sortingKeyPressedDate > InvTweaksConst.RULESET_SWAP_DELAY && sortingKeyPressedDate != Integer.MAX_VALUE) {
                String previousRuleset = config.getCurrentRulesetName();
                String newRuleset = config.switchConfig();
                // Log only if there is more than 1 ruleset
                if (previousRuleset != null && newRuleset != null && !previousRuleset.equals(newRuleset)) {
                    logInGame(String.format(InvTweaksText.get("invtweaks.loadconfig.enabled"), newRuleset), true);
                    handleSorting(currentScreen);
                }
                sortingKeyPressedDate = currentTime;
            }
        } else {
            sortingKeyPressedDate = 0;
        }
    }

    public void handleSorting(Screen guiScreen) {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null) {
            return;
        }

        // The sort key sorts whatever the mouse points at: chest slots -> the chest, inventory slots -> the inventory.
        ContainerSection mouseSection = null;
        boolean chestMenu = false;
        AbstractContainerMenu menu = null;
        if (guiScreen instanceof AbstractContainerScreen<?> containerScreen) {
            menu = containerScreen.getMenu();
            chestMenu = InvTweaksPlatform.isValidChest(menu);
            if (chestMenu) {
                Slot hovered = InvTweaksPlatform.getSlotAtMousePosition(containerScreen);
                IContainerManager containerMgr = getCurrentContainerManager();
                if (hovered != null && containerMgr != null) {
                    mouseSection = containerMgr.getSlotSection(getSlotNumber(hovered));
                }
            }
        }

        boolean sortChest = chestMenu && mouseSection == ContainerSection.CHEST;
        boolean sortInventory = !chestMenu || mouseSection == ContainerSection.INVENTORY_HOTBAR
                || mouseSection == ContainerSection.INVENTORY_NOT_HOTBAR;
        // Hovering anything else (crafting grid, the GUI background, ...) keeps the old behaviour and sorts everything.
        if (chestMenu && !sortChest && !sortInventory) {
            sortChest = true;
            sortInventory = true;
        }

        if (sortChest && menu != null) {
            try {
                new InvTweaksHandlerSorting(mc, manager.getConfig(), ContainerSection.CHEST, SortingMethod.DEFAULT,
                        SlotMaps.getRowSize(menu)).sort();
            } catch (Exception e) {
                logInGameError("invtweaks.sort.chest.error", e);
            }
        }

        if (sortInventory) {
            try {
                new InvTweaksHandlerSorting(mc, manager.getConfig(), ContainerSection.INVENTORY, SortingMethod.INVENTORY,
                        InvTweaksConst.INVENTORY_ROW_SIZE).sort();
            } catch (Exception e) {
                logInGameError("invtweaks.sort.inventory.error", e);
            }
        }

        playClick();
    }

    private void handleAutoRefill() {
        ItemStack currentStack = getFocusedStack();
        ItemStack offhandStack = getOffhandStack();

        String currentStackId = (currentStack.isEmpty()) ? null : BuiltInRegistries.ITEM.getKey(currentStack.getItem()).toString();

        int currentStackDamage = (currentStack.isEmpty()) ? 0 : currentStack.getDamageValue();
        int focusedSlot = getFocusedSlot() + 27; // Convert to container slots index
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();

        if (storedFocusedSlot != focusedSlot) { // Filter selection change
            storedFocusedSlot = focusedSlot;
        } else if (!ItemStack.isSameItemSameTags(currentStack, storedStack) && storedStackId != null) {
            // Same item but different durability must enter this branch, otherwise "replace before break" never fires.
            if (!storedStack.isEmpty() && !ItemStack.matches(offhandStack, storedStack)) { // Checks not switched to offhand
                if (currentStack.isEmpty() || (currentStack.is(Items.BOWL) && Objects.equals(storedStackId, "minecraft:mushroom_stew"))) {
                    // Handle eaten mushroom soup
                    if (config.isAutoRefillEnabled(storedStackId, storedStackDamage)) {
                        try {
                            manager.getAutoRefillHandler().autoRefillSlot(focusedSlot, storedStackId, storedStackDamage);
                        } catch (Exception e) {
                            logInGameError("invtweaks.sort.autorefill.error", e);
                        }
                    }
                } else {
                    int itemMaxDamage = currentStack.getMaxDamage();
                    int autoRefillThreshhold = config.getIntProperty(InvTweaksConfig.PROP_AUTO_REFILL_DAMAGE_THRESHHOLD);
                    if (canToolBeReplaced(currentStackDamage, itemMaxDamage, autoRefillThreshhold)
                            && config.getProperty(InvTweaksConfig.PROP_AUTO_REFILL_BEFORE_BREAK).equals(InvTweaksConfig.VALUE_TRUE)
                            && config.isAutoRefillEnabled(storedStackId, storedStackDamage)) {
                        // Trigger auto-refill before the tool breaks
                        try {
                            manager.getAutoRefillHandler().autoRefillSlot(focusedSlot, storedStackId, storedStackDamage);
                        } catch (Exception e) {
                            logInGameError("invtweaks.sort.autorefill.error", e);
                        }
                    }
                }
            }
        }

        // Copy some info about current selected stack for auto-refill
        storedStack = currentStack.copy();
        storedStackId = currentStackId;
        storedStackDamage = currentStackDamage;
    }

    private boolean canToolBeReplaced(int currentStackDamage, int itemMaxDamage, int autoRefillThreshhold) {
        return itemMaxDamage != 0 && itemMaxDamage - currentStackDamage < autoRefillThreshhold
                && itemMaxDamage - storedStackDamage >= autoRefillThreshhold;
    }

    private void handleMiddleClick(Screen guiScreen) {
        if (Keys.isMouseDown(2)) {
            InvTweaksConfigManager manager = getConfigManager();
            if (manager == null || !manager.makeSureConfigurationIsLoaded() || manager.getConfig() == null) {
                return;
            }
            InvTweaksConfig config = manager.getConfig();

            // Check that middle click sorting is allowed
            if (config.getProperty(InvTweaksConfig.PROP_ENABLE_MIDDLE_CLICK).equals(InvTweaksConfig.VALUE_TRUE)
                    && guiScreen instanceof AbstractContainerScreen<?> guiContainer) {

                AbstractContainerMenu container = guiContainer.getMenu();

                if (!chestAlgorithmButtonDown) {
                    chestAlgorithmButtonDown = true;

                    IContainerManager containerMgr = getContainerManager(container);
                    net.minecraft.world.inventory.Slot slotAtMousePosition = getSlotAtMousePosition(guiContainer);
                    ContainerSection target = null;
                    if (slotAtMousePosition != null) {
                        target = containerMgr.getSlotSection(getSlotNumber(slotAtMousePosition));
                    }

                    if (isValidChest(container)) {
                        // Check if the middle click target the chest or the inventory
                        if (ContainerSection.CHEST.equals(target)) {
                            // Play click
                            playClick();

                            long timestamp = System.currentTimeMillis();
                            int rowSize = SlotMaps.getRowSize(container);
                            if (timestamp - chestAlgorithmClickTimestamp > InvTweaksConst.CHEST_ALGORITHM_SWAP_MAX_INTERVAL || rowSize > 9) {
                                chestAlgorithm = SortingMethod.DEFAULT;
                            }
                            try {
                                new InvTweaksHandlerSorting(mc, manager.getConfig(), ContainerSection.CHEST, chestAlgorithm, rowSize).sort();
                            } catch (Exception e) {
                                logInGameError("invtweaks.sort.chest.error", e);
                            }
                            chestAlgorithm = SortingMethod.values()[(chestAlgorithm.ordinal() + 1) % 3];
                            chestAlgorithmClickTimestamp = timestamp;

                        } else if (ContainerSection.CRAFTING_IN.equals(target) || ContainerSection.CRAFTING_IN_PERSISTENT.equals(target)) {
                            try {
                                new InvTweaksHandlerSorting(mc, manager.getConfig(), target, SortingMethod.EVEN_STACKS,
                                        (containerMgr.getSize(target) == 9) ? 3 : 2).sort();
                            } catch (Exception e) {
                                logInGameError("invtweaks.sort.crafting.error", e);
                            }

                        } else if (ContainerSection.INVENTORY_HOTBAR.equals(target) || (ContainerSection.INVENTORY_NOT_HOTBAR.equals(target))) {
                            handleSorting(guiScreen);
                        }

                    } else if (isValidInventory(container)) {
                        if (ContainerSection.CRAFTING_IN.equals(target) || ContainerSection.CRAFTING_IN_PERSISTENT.equals(target)) {
                            // Crafting stacks evening
                            try {
                                new InvTweaksHandlerSorting(mc, manager.getConfig(), target, SortingMethod.EVEN_STACKS,
                                        (containerMgr.getSize(target) == 9) ? 3 : 2).sort();
                            } catch (Exception e) {
                                logInGameError("invtweaks.sort.crafting.error", e);
                            }
                        } else {
                            // Sorting
                            handleSorting(guiScreen);
                        }
                    }
                }
            }
        } else {
            chestAlgorithmButtonDown = false;
        }
    }

    private void handleShortcuts(AbstractContainerScreen<?> guiScreen) {
        // Check open GUI
        if (!((isValidChest(guiScreen.getMenu()) || isValidInventory(guiScreen.getMenu())))) {
            return;
        }

        // Configurable shortcuts
        if (Keys.isMouseDown(0) || Keys.isMouseDown(1)) {
            if (!mouseWasDown) {
                mouseWasDown = true;

                // The mouse has just been clicked,
                // trigger a shortcut according to the pressed keys.
                InvTweaksConfigManager manager = getConfigManager();
                if (manager != null && manager.getConfig() != null && manager.getShortcutsHandler() != null
                        && manager.getConfig().getProperty(InvTweaksConfig.PROP_ENABLE_SHORTCUTS).equals("true")) {
                    manager.getShortcutsHandler().handleShortcut();
                }
            }
        } else {
            mouseWasDown = false;
        }
    }

    private int getItemOrder(ItemStack itemStack) {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null || manager.getConfig().getTree() == null) {
            return Integer.MAX_VALUE;
        }
        List<IItemTreeItem> items = manager.getConfig().getTree().getItems(
                BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), itemStack.getDamageValue());
        return (items.size() > 0) ? items.get(0).getOrder() : Integer.MAX_VALUE;
    }

    private boolean isSortingShortcutDown() {
        if (sortKeyEnabled && !textboxMode) {
            int keyCode = getConfigManager() == null || getConfigManager().getConfig() == null
                    ? InvTweaksKeys.getSortKeyCode() : getConfigManager().getConfig().getSortKeyCode();
            if (keyCode > 0 || keyCode < -100) {
                return Keys.isDown(keyCode);
            }
        }
        return false;
    }

    private boolean isTimeForPolling() {
        if (tickNumber - lastPollingTickNumber >= InvTweaksConst.POLLING_DELAY) {
            lastPollingTickNumber = tickNumber;
        }
        return tickNumber - lastPollingTickNumber == 0;
    }

    private void unlockKeysIfNecessary() {
        boolean hasFocus = mc.isWindowActive();
        if (!hadFocus && hasFocus) {
            // Nothing to do with GLFW, but keep the state so that the check stays meaningful.
            log.debug("Game window regained focus");
        }
        hadFocus = hasFocus;
    }

    /**
     * Allows to maintain a clone of the hotbar contents to track changes (especially needed by the "on pickup"
     * features).
     */
    private void cloneHotbar() {
        NonNullList<ItemStack> mainInventory = getMainInventory();
        for (int i = 0; i < 9; i++) {
            hotbarClone[i] = mainInventory.get(i).copy();
        }
    }

    @Override
    public void playClick() {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null
                || !manager.getConfig().getProperty(InvTweaksConfig.PROP_ENABLE_SOUNDS).equals(InvTweaksConfig.VALUE_FALSE)) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /**
     * Plays a small sound, used by the auto-refill.
     */
    public void playRefillSound() {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHICKEN_EGG, 1.0F));
    }

    public boolean isDebugTree() {
        return debugTree;
    }

    public String getMostRecentComparison() {
        return mostRecentComparison;
    }

    /**
     * Used by the tooltip handler to display the tree path of an item.
     */
    public String getTreePath(ItemStack stack) {
        InvTweaksConfigManager manager = getConfigManager();
        if (manager == null || manager.getConfig() == null || manager.getConfig().getTree() == null) {
            return null;
        }
        List<IItemTreeItem> items = manager.getConfig().getTree().getItems(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getDamageValue());
        return items.isEmpty() ? null : items.get(0).getPath();
    }

    /**
     * Release all resources when the client stops.
     */
    public static void shutdown() {
        instance = null;
    }
}
