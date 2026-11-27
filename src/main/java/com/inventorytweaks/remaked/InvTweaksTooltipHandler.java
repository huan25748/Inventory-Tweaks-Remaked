package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;

/**
 * Adds the item tree path to item tooltips when the enableToolTipTreePath option is on.
 */
public final class InvTweaksTooltipHandler {

    private InvTweaksTooltipHandler() {
    }

    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        if (manager == null || manager.getConfig() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();
        if (!config.getProperty(InvTweaksConfig.PROP_TOOLTIP_PATH).equals(InvTweaksConfig.VALUE_TRUE)) {
            return;
        }

        Slot slot = InvTweaksPlatform.getSlotAtPosition(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || !slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();
        InvTweaks it = InvTweaks.getInstance();
        if (it == null) {
            return;
        }
        String path = it.getTreePath(stack);
        if (path == null || path.isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.renderTooltip(Minecraft.getInstance().font, Component.literal(path), event.getMouseX(), event.getMouseY());
    }
}
