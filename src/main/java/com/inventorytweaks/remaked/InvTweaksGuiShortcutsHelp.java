package com.inventorytweaks.remaked;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Detailed shortcut help, replacing the terse 1.12 two-column list.
 */
public class InvTweaksGuiShortcutsHelp extends Screen {

    private final Screen parentScreen;
    private static final int LINE = 9;
    private static final int HEADER_GAP = 3;
    private static final int KEY_WIDTH = 64;

    private static final String[][] ROWS = {
            {"", "invtweaks.help.shortcuts.sort"},
            {"R", "invtweaks.help.shortcuts.sort.1"},
            {"", "invtweaks.help.shortcuts.sort.2"},
            {"invtweaks.help.shortcuts.key.middle", "invtweaks.help.shortcuts.middle"},
            {"R + 1~9", "invtweaks.help.shortcuts.scheme"},
            {"invtweaks.help.shortcuts.key.holdr", "invtweaks.help.shortcuts.holdr"},
            {"", "invtweaks.help.shortcuts.modifier"},
            {"Ctrl", "invtweaks.help.shortcuts.ctrl"},
            {"Ctrl + Shift", "invtweaks.help.shortcuts.ctrlshift"},
            {"Shift", "invtweaks.help.shortcuts.shift"},
            {"invtweaks.help.shortcuts.key.space", "invtweaks.help.shortcuts.space"},
            {"\u2191 / \u2193", "invtweaks.help.shortcuts.arrows"},
            {"1~9", "invtweaks.help.shortcuts.hotbar"},
            {"Alt", "invtweaks.help.shortcuts.alt"},
            {"", "invtweaks.help.shortcuts.note"}
    };

    public InvTweaksGuiShortcutsHelp(Screen parentScreen_) {
        super(Component.translatable("invtweaks.help.shortcuts.title"));
        parentScreen = parentScreen_;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(width / 2 - 60, height - 24, 120, 18).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String tr(String value) {
        return value.startsWith("invtweaks.") ? Component.translatable(value).getString() : value;
    }

    private void drawHelp(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        int x = width / 2 - 165;
        int y = 26;
        for (String[] row : ROWS) {
            if (row[0].isEmpty()) {
                graphics.drawString(font, tr(row[1]), x, y, 0xFFD700);
                y += HEADER_GAP + LINE;
                continue;
            }
            graphics.drawString(font, tr(row[0]), x, y, 0x88DDFF);
            graphics.drawString(font, tr(row[1]), x + KEY_WIDTH, y, 0xFFFFFF);
            y += LINE;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        drawHelp(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
