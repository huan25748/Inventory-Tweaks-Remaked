package com.inventorytweaks.remaked;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.lang.reflect.Field;

/**
 * Reads the protected geometry fields of AbstractContainerScreen. Mojang made guiLeft/guiTop private in recent
 * versions, so a small reflective accessor keeps the layout logic of the original mod intact.
 */
public final class ScreenAccessor {

    private static Field LEFT, TOP, WIDTH, HEIGHT, HOVERED;

    static {
        try {
            LEFT = AbstractContainerScreen.class.getDeclaredField("leftPos");
            LEFT.setAccessible(true);
        } catch (Exception e) {
            LEFT = null;
        }
        try {
            TOP = AbstractContainerScreen.class.getDeclaredField("topPos");
            TOP.setAccessible(true);
        } catch (Exception e) {
            TOP = null;
        }
        try {
            WIDTH = AbstractContainerScreen.class.getDeclaredField("imageWidth");
            WIDTH.setAccessible(true);
        } catch (Exception e) {
            WIDTH = null;
        }
        try {
            HEIGHT = AbstractContainerScreen.class.getDeclaredField("imageHeight");
            HEIGHT.setAccessible(true);
        } catch (Exception e) {
            HEIGHT = null;
        }
        try {
            HOVERED = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
            HOVERED.setAccessible(true);
        } catch (Exception e) {
            HOVERED = null;
        }
    }

    private ScreenAccessor() {
    }

    private static int read(Field field, AbstractContainerScreen<?> screen, int fallback) {
        if (field == null) {
            return fallback;
        }
        try {
            return field.getInt(screen);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int getImageWidth(AbstractContainerScreen<?> screen) {
        return read(WIDTH, screen, 176);
    }

    public static int getImageHeight(AbstractContainerScreen<?> screen) {
        return read(HEIGHT, screen, 166);
    }

    public static int getGuiLeft(AbstractContainerScreen<?> screen) {
        return read(LEFT, screen, (screen.width - getImageWidth(screen)) / 2);
    }

    public static int getGuiTop(AbstractContainerScreen<?> screen) {
        return read(TOP, screen, (screen.height - getImageHeight(screen)) / 2);
    }

    public static Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        if (HOVERED == null) {
            return null;
        }
        try {
            return (Slot) HOVERED.get(screen);
        } catch (Exception e) {
            return null;
        }
    }
}
