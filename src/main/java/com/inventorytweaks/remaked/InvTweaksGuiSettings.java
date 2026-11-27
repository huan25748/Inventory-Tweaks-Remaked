package com.inventorytweaks.remaked;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;

import java.io.File;

/**
 * The main options screen of the mod (the "..." button of the inventory and chests).
 */
public class InvTweaksGuiSettings extends Screen {

    protected final Screen parentScreen;
    protected InvTweaksConfig config;
    private Button middleClickButton;
    private Button chestButtonsButton;
    private Button shortcutsButton;
    private Button autoRefillButton;
    private Button beforeBreakButton;

    public InvTweaksGuiSettings(Screen parentScreen_) {
        super(InvTweaksText.text("invtweaks.settings.title"));
        parentScreen = parentScreen_;
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        config = (manager == null) ? null : manager.getConfig();
    }

    @Override
    protected void init() {
        if (config == null) {
            onClose();
            return;
        }
        int x = width / 2 - 155;
        int y = height / 4 + 8;

        middleClickButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_MIDDLE_CLICK)).bounds(x, y, 150, 20).build());
        chestButtonsButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_SHOW_CHEST_BUTTONS)).bounds(x + 160, y, 150, 20).build());
        shortcutsButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_SHORTCUTS)).bounds(x, y + 24, 150, 20).build());
        autoRefillButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_ENABLE_AUTO_REFILL)).bounds(x + 160, y + 24, 150, 20).build());
        beforeBreakButton = addRenderableWidget(Button.builder(Component.empty(), b -> toggle(InvTweaksConfig.PROP_AUTO_REFILL_BEFORE_BREAK)).bounds(x, y + 48, 150, 20).build());

        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.moreoptions"), b -> minecraft.setScreen(new InvTweaksGuiSettingsAdvanced(this)))
                .bounds(x + 160, y + 48, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.rulesfile"), b -> openFile(InvTweaksConst.CONFIG_RULES_FILE, "invtweaks.settings.rulesfile.error"))
                .bounds(x, y + 84, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.treefile"), b -> openFile(InvTweaksConst.CONFIG_TREE_FILE, "invtweaks.settings.treefile.error"))
                .bounds(x + 160, y + 84, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.onlinehelp"), b -> openUrl(InvTweaksConst.HELP_URL))
                .bounds(x, y + 108, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.help.shortcuts.title"), b -> minecraft.setScreen(new InvTweaksGuiShortcutsHelp(this)))
                .bounds(x + 160, y + 108, 150, 20).build());
        addRenderableWidget(Button.builder(InvTweaksText.text("invtweaks.settings.exit"), b -> onClose())
                .bounds(width / 2 - 100, height / 4 + 168, 200, 20).build());

        refreshLabels();
    }

    protected void refreshLabels() {
        if (config == null) {
            return;
        }
        // The advanced screen inherits from this one but builds its own widgets, so every field can be null here.
        if (middleClickButton != null) {
            middleClickButton.setMessage(toggleLabel("invtweaks.settings.middleclick", InvTweaksConfig.PROP_ENABLE_MIDDLE_CLICK));
        }
        if (chestButtonsButton != null) {
            chestButtonsButton.setMessage(toggleLabel("invtweaks.settings.chestbuttons", InvTweaksConfig.PROP_SHOW_CHEST_BUTTONS));
        }
        if (shortcutsButton != null) {
            shortcutsButton.setMessage(toggleLabel("invtweaks.settings.shortcuts", InvTweaksConfig.PROP_ENABLE_SHORTCUTS));
        }
        if (autoRefillButton != null) {
            autoRefillButton.setMessage(toggleLabel("invtweaks.settings.autorefill", InvTweaksConfig.PROP_ENABLE_AUTO_REFILL));
        }
        if (beforeBreakButton != null) {
            beforeBreakButton.setMessage(toggleLabel("invtweaks.settings.beforebreak", InvTweaksConfig.PROP_AUTO_REFILL_BEFORE_BREAK));
        }
    }

    protected Component toggleLabel(String key, String property) {
        boolean enabled = config != null && !config.getProperty(property).equals(InvTweaksConfig.VALUE_FALSE);
        String state = enabled ? "invtweaks.settings.on" : "invtweaks.settings.off";
        return InvTweaksText.text(key).append(": ").append(InvTweaksText.text(state));
    }

    protected void toggle(String property) {
        if (config == null) {
            return;
        }
        boolean enabled = !config.getProperty(property).equals(InvTweaksConfig.VALUE_FALSE);
        config.setProperty(property, enabled ? InvTweaksConfig.VALUE_FALSE : InvTweaksConfig.VALUE_TRUE);
        refreshLabels();
    }

    protected void openFile(File file, String errorKey) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            Util.getPlatform().openFile(file);
        } catch (Exception e) {
            InvTweaks.logInGameErrorStatic(errorKey, e);
        }
    }

    protected void openUrl(String url) {
        try {
            Util.getPlatform().openUri(url);
        } catch (Exception e) {
            InvTweaks.logInGameErrorStatic("invtweaks.settings.onlinehelp.error", e);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        if (config != null) {
            String sortKey = InvTweaksText.get("invtweaks.settings.key") + " "
                    + Keys.getName(config.getSortKeyCode());
            guiGraphics.drawCenteredString(font, sortKey, width / 2, height / 4 - 12, 0xAAAAAA);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
