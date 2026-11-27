package com.inventorytweaks.remaked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Adds the Inventory Tweaks buttons to container screens.
 * <p>
 * In 1.12 the buttons were GuiButton instances injected into the container's buttonList; 1.20.1 screens own their
 * widgets, so they are added during {@code ScreenEvent.Init.Post}. The buttons themselves are drawn by
 * {@link InvTweaksButton}, which reproduces the 1.12 look.
 * <p>
 * Layout: the sorting/settings row sits on the top-right title row; the two transfer arrows sit side by side at the
 * right end of the "Inventory" label row (up = into the container, down = into the player inventory).
 */
public final class InvTweaksGuiButtons {

    private static final int SPACING = 12;
    private static final int BUTTON = InvTweaksButton.SIZE;

    /**
     * Buttons of other mods are often drawn by the mod itself (not registered as screen widgets), so they cannot be
     * detected at runtime. Inventory Profiles Next ships per-screen offsets for exactly this reason; these are the
     * values from its ModIntegrationHintsNG.json, applied as "move our row this many pixels further left".
     */
    private static final Map<String, Integer> GUI_OFFSETS = Map.of(
            "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen", -42,
            "net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen", -40);

    /** The two transfer arrows are positioned independently, so they can sit further right than the sort row. */
    private static final Map<String, Integer> TRANSFER_OFFSETS = Map.of(
            "net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen", -18,
            "net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen", -18);

    private InvTweaksGuiButtons() {
    }

    private static final class Spec {
        final String symbol;
        final String tooltipKey;
        final Runnable action;

        Spec(String symbol, String tooltipKey, Runnable action) {
            this.symbol = symbol;
            this.tooltipKey = tooltipKey;
            this.action = action;
        }
    }

    public static void addButtons(AbstractContainerScreen<?> screen, ScreenEvent.Init.Post event) {
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        if (manager == null || manager.getConfig() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();
        AbstractContainerMenu menu = screen.getMenu();

        if (!InvTweaksPlatform.showButtons(menu)) {
            return;
        }

        int guiLeft = ScreenAccessor.getGuiLeft(screen);
        int guiTop = ScreenAccessor.getGuiTop(screen);
        int guiWidth = ScreenAccessor.getImageWidth(screen);
        if (InvTweaksPlatform.isGuiInventoryCreative(screen)) {
            // 1.20.1's creative screen keeps leftPos/topPos at 0 and lays the item picker out itself, so the
            // reflected origin is useless there. Recreate the vanilla geometry: the picker panel is 195x136.
            guiLeft = (screen.width - 195) / 2;
            guiTop = (screen.height - 136) / 2;
            guiWidth = 195;
        }
        int rightEdge = guiLeft + guiWidth;
        int titleRowY = guiTop + 5;
        int offset = guiOffset(screen);

        Map<ContainerSection, List<Slot>> slotMap = SlotMaps.getSlotMap(menu);
        boolean isValidChest = InvTweaksPlatform.isValidChest(menu);
        boolean hasChest = slotMap.containsKey(ContainerSection.CHEST);

        // Sorting row (s, v, h, settings), aligned to the right end of the title row.
        List<Spec> row = new ArrayList<>();
        if (isValidChest && !config.getProperty(InvTweaksConfig.PROP_SHOW_CHEST_BUTTONS).equals("false")) {
            int rowSize = SlotMaps.getRowSize(menu);
            row.add(new Spec("s", "invtweaks.button.chest1.tooltip",
                    () -> sortChest(screen, SortingMethod.DEFAULT, rowSize)));
            if (rowSize <= 9) {
                row.add(new Spec("v", "invtweaks.button.chest2.tooltip",
                        () -> sortChest(screen, SortingMethod.VERTICAL, rowSize)));
                row.add(new Spec("h", "invtweaks.button.chest3.tooltip",
                        () -> sortChest(screen, SortingMethod.HORIZONTAL, rowSize)));
            }
        }
        row.add(new Spec("...", "invtweaks.button.settings.tooltip",
                () -> Minecraft.getInstance().setScreen(new InvTweaksGuiSettings(screen))));

        int rowRight = (hasChest ? rightEdge - 18 : rightEdge - 16) + offset;
        int rowX = findFreeX(screen, rowRight - SPACING * (row.size() - 1), rowRight + BUTTON, titleRowY, row.size());
        for (int i = 0; i < row.size(); i++) {
            Spec spec = row.get(i);
            event.addListener(new InvTweaksButton(rowX + i * SPACING, titleRowY,
                    spec.symbol, spec.tooltipKey, spec.action));
        }

        if (!hasChest || !slotMap.containsKey(ContainerSection.INVENTORY)) {
            return;
        }

        // Both transfer arrows sit side by side at the right end of the "Inventory" label row.
        // Slot coordinates are GUI relative, so guiTop has to be added to get screen space.
        int inventoryY = guiTop + firstSlotY(menu, ContainerSection.INVENTORY) - 12;
        if (inventoryY > guiTop) {
            int transferShift = transferOffset(screen);
            int downX = rightEdge - 18 + transferShift;
            int upX = downX - SPACING;
            int pairX = findFreeX(screen, upX, downX + BUTTON, inventoryY, 2);

            event.addListener(new InvTweaksButton(pairX, inventoryY, "transfer_up",
                    "invtweaks.button.transfer_up.tooltip", () -> {
                        boolean shift = Keys.isDown(Keys.getCode("LSHIFT")) || Keys.isDown(Keys.getCode("RSHIFT"));
                        boolean alt = Keys.isDown(Keys.getCode("LALT")) || Keys.isDown(Keys.getCode("RALT"));
                        InvTweaksTransferHandler.Mode mode = (shift && alt)
                                ? InvTweaksTransferHandler.Mode.ALL
                                : (shift ? InvTweaksTransferHandler.Mode.WITHOUT_HOTBAR : InvTweaksTransferHandler.Mode.MATCHING);
                        InvTweaksTransferHandler.transfer(ContainerSection.INVENTORY, ContainerSection.CHEST, mode);
                    }));

            event.addListener(new InvTweaksButton(pairX + SPACING, inventoryY, "transfer_down",
                    "invtweaks.button.transfer_down.tooltip", () -> {
                        boolean shift = Keys.isDown(Keys.getCode("LSHIFT")) || Keys.isDown(Keys.getCode("RSHIFT"));
                        InvTweaksTransferHandler.transfer(ContainerSection.CHEST, ContainerSection.INVENTORY,
                                shift ? InvTweaksTransferHandler.Mode.ALL : InvTweaksTransferHandler.Mode.MATCHING);
                    }));
        }
    }

    private static int guiOffset(Screen screen) {
        return lookupOffset(screen, GUI_OFFSETS);
    }

    private static int transferOffset(Screen screen) {
        return lookupOffset(screen, TRANSFER_OFFSETS);
    }

    private static int lookupOffset(Screen screen, Map<String, Integer> table) {
        for (Class<?> type = screen.getClass(); type != null; type = type.getSuperclass()) {
            Integer offset = table.get(type.getName());
            if (offset != null) {
                return offset;
            }
        }
        return 0;
    }

    private static int firstSlotY(AbstractContainerMenu menu, ContainerSection section) {
        List<Slot> slots = SlotMaps.getSlotMap(menu).get(section);
        if (slots == null || slots.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        return slots.get(0).y;
    }

    /**
     * Slides a button (or a row of buttons) left until it does not overlap widgets that the screen already has - that
     * is what happens on modded GUIs which draw their own buttons in the same corner as widgets.
     */
    private static int findFreeX(AbstractContainerScreen<?> screen, int defaultX, int rowRight, int y, int count) {
        int rowWidth = SPACING * (count - 1) + BUTTON;
        for (int step = 0; step <= 10; step++) {
            int x = defaultX - step * SPACING;
            if (!overlapsWidgets(screen, x, y, x + rowWidth, y + BUTTON)) {
                return x;
            }
        }
        return defaultX;
    }

    private static boolean overlapsWidgets(AbstractContainerScreen<?> screen, int x0, int y0, int x1, int y1) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible) {
                if (x0 < widget.getX() + widget.getWidth() && x1 > widget.getX()
                        && y0 < widget.getY() + widget.getHeight() && y1 > widget.getY()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void sortChest(AbstractContainerScreen<?> screen, SortingMethod method, int rowSize) {
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        if (manager == null || manager.getConfig() == null) {
            return;
        }
        try {
            new InvTweaksHandlerSorting(Minecraft.getInstance(), manager.getConfig(), ContainerSection.CHEST, method, rowSize).sort();
        } catch (Exception e) {
            InvTweaks.logInGameErrorStatic("invtweaks.sort.chest.error", e);
        }
    }

    /**
     * Kept for parity with the 1.12 layout code, which re-created the buttons when NEI/JEI or the recipe book toggled.
     * 1.20.1 screens keep their widgets, so nothing has to be done here.
     */
    public static void updateButtons(AbstractContainerScreen<?> screen) {
        // no-op
    }
}
