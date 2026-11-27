package com.inventorytweaks.remaked;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * The 10x10 icon button of Inventory Tweaks.
 * <p>
 * The 1.12 mod drew its buttons by sampling the four corners of the vanilla button sprite
 * (textures/gui/widgets.png) and then painted the symbol with 1px rectangles. The background pixels below are the
 * exact result of that sampling for the three button states (taken from the 1.12.2 texture), and the move icons are
 * the ones Inventory Profiles Next uses.
 */
public class InvTweaksButton extends AbstractWidget {

    public static final int SIZE = 10;

    private static final int TEXT_NORMAL = 0xFFE0E0E0;
    private static final int TEXT_HOVER = 0xFFFFFFA0;
    private static final int TEXT_DISABLED = 0xFFA0A0A0;

    /** Vanilla button, state 1 (enabled). */
    private static final int[][] BG_NORMAL = {
            { 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFFAAAAAA, 0xFF6F6F6F },
            { 0xFFAAAAAA, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF707070, 0xFF757575, 0xFF6C6C6C, 0xFF686868, 0xFF525252 },
            { 0xFFAAAAAA, 0xFF6E6E6E, 0xFF6B6B6B, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6C6C6C, 0xFF555555 },
            { 0xFFA8A8A8, 0xFF6F6F6F, 0xFF707070, 0xFF6C6C6C, 0xFF6E6E6E, 0xFF717171, 0xFF757575, 0xFF6D6D6D, 0xFF696969, 0xFF545454 },
            { 0xFFAAAAAA, 0xFF6F6F6F, 0xFF707070, 0xFF717171, 0xFF6B6B6B, 0xFF6E6E6E, 0xFF6B6B6B, 0xFF6D6D6D, 0xFF6E6E6E, 0xFF595959 },
            { 0xFFAAAAAA, 0xFF737373, 0xFF757575, 0xFF707070, 0xFF6F6F6F, 0xFF707070, 0xFF777777, 0xFF717171, 0xFF717171, 0xFF565656 },
            { 0xFFAAAAAA, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF717171, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF707070, 0xFF747474, 0xFF575757 },
            { 0xFFA8A8A8, 0xFF6F6F6F, 0xFF6D6D6D, 0xFF6D6D6D, 0xFF6F6F6F, 0xFF6F6F6F, 0xFF707070, 0xFF757575, 0xFF717171, 0xFF595959 },
            { 0xFF737373, 0xFF565656, 0xFF565656, 0xFF545454, 0xFF505050, 0xFF565656, 0xFF565656, 0xFF565656, 0xFF565656, 0xFF565656 },
            { 0xFF6D6D6D, 0xFF565656, 0xFF565656, 0xFF5A5A5A, 0xFF5E5E5E, 0xFF565656, 0xFF565656, 0xFF585858, 0xFF565656, 0xFF525252 }
    };

    /** Vanilla button, state 2 (hovered). */
    private static final int[][] BG_HOVER = {
            { 0xFFBEC7FF, 0xFFBDC6FF, 0xFFBEC7FF, 0xFFBEC7FF, 0xFFBFC8FF, 0xFFBEC7FF, 0xFFBEC7FF, 0xFFBFC8FF, 0xFFBEC7FF, 0xFF7E88BF },
            { 0xFFBEC7FF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF808AC0, 0xFF8690C3, 0xFF7A84BC, 0xFF737DB9, 0xFF586299 },
            { 0xFFBEC7FF, 0xFF7D87BE, 0xFF7882BC, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7983BC, 0xFF5B659C },
            { 0xFFBCC5FF, 0xFF7E88BF, 0xFF7F89BF, 0xFF7A84BC, 0xFF7D87BE, 0xFF808AC0, 0xFF8690C3, 0xFF7B85BD, 0xFF757FBA, 0xFF5A649B },
            { 0xFFBEC7FF, 0xFF7E88BF, 0xFF7F89BF, 0xFF828CC1, 0xFF7882BC, 0xFF7D87BE, 0xFF7882BC, 0xFF7C86BE, 0xFF7D87BE, 0xFF5F69A0 },
            { 0xFFBEC7FF, 0xFF838DC1, 0xFF8690C3, 0xFF7F89BF, 0xFF7E88BF, 0xFF808AC0, 0xFF8992C4, 0xFF818BC1, 0xFF818BC1, 0xFF5C669D },
            { 0xFFBEC7FF, 0xFF7E88BF, 0xFF7E88BF, 0xFF7E88BF, 0xFF808AC0, 0xFF7E88BF, 0xFF7E88BF, 0xFF7F89BF, 0xFF848EC2, 0xFF5D679E },
            { 0xFFBCC5FF, 0xFF7E88BF, 0xFF7C86BE, 0xFF7B85BD, 0xFF7E88BF, 0xFF7E88BF, 0xFF7F89BF, 0xFF8690C3, 0xFF808AC0, 0xFF606AA1 },
            { 0xFF828CC1, 0xFF5C669D, 0xFF5C669D, 0xFF5A649B, 0xFF576097, 0xFF5C669D, 0xFF5C669D, 0xFF5C669D, 0xFF5C669D, 0xFF5C669D },
            { 0xFF7C86BD, 0xFF5C669D, 0xFF5C669D, 0xFF616BA2, 0xFF646FA6, 0xFF5C669D, 0xFF5C669D, 0xFF5E689F, 0xFF5C669D, 0xFF586299 }
    };

    /** Vanilla button, state 0 (disabled). */
    private static final int[][] BG_DISABLED = {
            { 0xFF2B2B2B, 0xFF2B2B2B, 0xFF292929, 0xFF2C2C2C, 0xFF2D2D2D, 0xFF2B2B2B, 0xFF292929, 0xFF2A2A2A, 0xFF2C2C2C, 0xFF2C2C2C },
            { 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2D2D2D, 0xFF2F2F2F, 0xFF2A2A2A, 0xFF262626, 0xFF262626 },
            { 0xFF2B2B2B, 0xFF2B2B2B, 0xFF292929, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF292929, 0xFF292929 },
            { 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2A2A2A, 0xFF2B2B2B, 0xFF2D2D2D, 0xFF2F2F2F, 0xFF2A2A2A, 0xFF282828, 0xFF282828 },
            { 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2E2E2E, 0xFF292929, 0xFF2B2B2B, 0xFF292929, 0xFF2A2A2A, 0xFF2B2B2B, 0xFF2B2B2B },
            { 0xFF2E2E2E, 0xFF2E2E2E, 0xFF2F2F2F, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2D2D2D, 0xFF313131, 0xFF2D2D2D, 0xFF2D2D2D, 0xFF2D2D2D },
            { 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2D2D2D, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2F2F2F, 0xFF2F2F2F },
            { 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2A2A2A, 0xFF2A2A2A, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2C2C2C, 0xFF2F2F2F, 0xFF2D2D2D, 0xFF2D2D2D },
            { 0xFF2B2B2B, 0xFF2B2B2B, 0xFF292929, 0xFF2C2C2C, 0xFF2D2D2D, 0xFF2B2B2B, 0xFF292929, 0xFF2A2A2A, 0xFF2C2C2C, 0xFF2C2C2C },
            { 0xFF2A2A2A, 0xFF2A2A2A, 0xFF2B2B2B, 0xFF2A2A2A, 0xFF282828, 0xFF2C2C2C, 0xFF282828, 0xFF2A2A2A, 0xFF2C2C2C, 0xFF2C2C2C }
    };

    private final String symbol;
    private final Component tooltip;
    private final Runnable action;
    private long hoverStart = 0L;

    public InvTweaksButton(int x, int y, String symbol, String tooltipKey, Runnable action) {
        super(x, y, SIZE, SIZE, Component.literal(symbol));
        this.symbol = symbol;
        this.tooltip = InvTweaksText.text(tooltipKey);
        this.action = action;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY(), w = this.width, h = this.height;
        int[][] background = !this.active ? BG_DISABLED : (this.isHovered ? BG_HOVER : BG_NORMAL);
        for (int row = 0; row < background.length; row++) {
            int[] colors = background[row];
            for (int col = 0; col < colors.length; col++) {
                guiGraphics.fill(x + col, y + row, x + col + 1, y + row + 1, colors[col]);
            }
        }

        int color = !this.active ? TEXT_DISABLED : (this.isHovered ? TEXT_HOVER : TEXT_NORMAL);
        switch (this.symbol) {
            case "h":
                guiGraphics.fill(x + 3, y + 3, x + w - 3, y + 4, color);
                guiGraphics.fill(x + 3, y + 6, x + w - 3, y + 7, color);
                break;
            case "v":
                guiGraphics.fill(x + 3, y + 3, x + 4, y + h - 3, color);
                guiGraphics.fill(x + 6, y + 3, x + 7, y + h - 3, color);
                break;
            case "transfer_up":
                // Inventory Profiles Next's "move to container" glyph.
                guiGraphics.fill(x + 4, y + 2, x + 5, y + 3, color);
                guiGraphics.fill(x + 3, y + 3, x + 6, y + 4, color);
                guiGraphics.fill(x + 2, y + 4, x + 3, y + 5, color);
                guiGraphics.fill(x + 4, y + 4, x + 5, y + 5, color);
                guiGraphics.fill(x + 6, y + 4, x + 7, y + 5, color);
                guiGraphics.fill(x + 4, y + 5, x + 5, y + 7, color);
                guiGraphics.fill(x + 4, y + 6, x + 8, y + 7, color);
                break;
            case "transfer_down":
                // Inventory Profiles Next's "move to player" glyph.
                guiGraphics.fill(x + 4, y + 3, x + 8, y + 4, color);
                guiGraphics.fill(x + 4, y + 4, x + 5, y + 5, color);
                guiGraphics.fill(x + 2, y + 5, x + 3, y + 6, color);
                guiGraphics.fill(x + 4, y + 5, x + 5, y + 6, color);
                guiGraphics.fill(x + 6, y + 5, x + 7, y + 6, color);
                guiGraphics.fill(x + 3, y + 6, x + 6, y + 7, color);
                guiGraphics.fill(x + 4, y + 7, x + 5, y + 8, color);
                break;
            case "...":
                guiGraphics.drawString(Minecraft.getInstance().font, this.symbol,
                        x + 5 - (Minecraft.getInstance().font.width(this.symbol) / 2), y - 1, color, false);
                break;
            default:
                guiGraphics.fill(x + 3, y + 3, x + w - 3, y + 4, color);
                guiGraphics.fill(x + 5, y + 4, x + 6, y + 5, color);
                guiGraphics.fill(x + 4, y + 5, x + 5, y + 6, color);
                guiGraphics.fill(x + 3, y + 6, x + w - 3, y + 7, color);
                break;
        }

        drawDelayedTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawDelayedTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.isHovered) {
            this.hoverStart = 0L;
            return;
        }
        long now = Util.getMillis();
        if (this.hoverStart == 0L) {
            this.hoverStart = now;
        }
        if (now - this.hoverStart >= InvTweaksConst.TOOLTIP_DELAY) {
            // Drawn by hand (same as 1.12): a translucent black box with one line of shadowed text.
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            String text = this.tooltip.getString();
            int x = mouseX + 12;
            int y = mouseY - 12;
            int width = font.width(text);
            guiGraphics.fill(x - 3, y - 3, x + width + 3, y + 9, 0xC0000000);
            guiGraphics.drawString(font, text, x, y, 0xFFFFFF, true);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
