package com.inventorytweaks.remaked;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The advanced options screen (the "More options..." button).
 */
public class InvTweaksGuiSettingsAdvanced extends InvTweaksGuiSettings {

    private Button sortOnPickupButton;
    private Button autoEquipButton;
    private Button soundsButton;
    private Button serverAssistButton;

    public InvTweaksGuiSettingsAdvanced(Screen parentScreen_) {
        super(parentScreen_);
    }

    @Override
    protected void init() {
        if (config == null) {
            onClose();
            return;
        }
        int x = width / 2 - 155;
        int y = height / 4 + 8;

        sortOnPickupButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_SORTING_ON_PICKUP))
                .bounds(x, y, 150, 20).build());
        autoEquipButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_AUTO_EQUIP_ARMOR))
                .bounds(x + 160, y, 150, 20).build());
        soundsButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_SOUNDS))
                .bounds(x, y + 24, 150, 20).build());
        serverAssistButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_SERVER_ITEMSWAP))
                .bounds(x + 160, y + 24, 150, 20).build());

        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.advanced.mappingsfile"),
                        b -> openFile(InvTweaksConst.CONFIG_PROPS_FILE, "invtweaks.settings.advanced.mappingsfile.error"))
                .bounds(x, y + 60, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.treefile"),
                        b -> openFile(InvTweaksConst.CONFIG_TREE_FILE, "invtweaks.settings.treefile.error"))
                .bounds(x + 160, y + 60, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.exit"), b -> onClose())
                .bounds(width / 2 - 100, height / 4 + 168, 200, 20).build());

        refreshLabels();
    }

    @Override
    protected void refreshLabels() {
        if (config == null) {
            return;
        }
        super.refreshLabels();
        sortOnPickupButton.setMessage(toggleLabel("invtweaks.settings.advanced.sortonpickup", InvTweaksConfig.PROP_ENABLE_SORTING_ON_PICKUP));
        autoEquipButton.setMessage(toggleLabel("invtweaks.settings.advanced.autoequip", InvTweaksConfig.PROP_ENABLE_AUTO_EQUIP_ARMOR));
        soundsButton.setMessage(toggleLabel("invtweaks.settings.advanced.sounds", InvTweaksConfig.PROP_ENABLE_SOUNDS));
        serverAssistButton.setMessage(toggleLabel("invtweaks.settings.advanced.serverassist", InvTweaksConfig.PROP_ENABLE_SERVER_ITEMSWAP));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }
}
