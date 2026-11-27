package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side hooks for Forge 1.20.1.
 */
@Mod.EventBusSubscriber(modid = InvTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class InvTweaksClientEvents {

    private InvTweaksClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        InvTweaks invTweaks = InvTweaks.getInstance();
        if (invTweaks == null) {
            invTweaks = new InvTweaks(mc);
        }

        Screen screen = mc.screen;
        if (screen != null) {
            invTweaks.setTextboxMode(screen.getFocused() instanceof EditBox);
            invTweaks.onTickInGUI(screen);
        } else {
            invTweaks.setTextboxMode(false);
            invTweaks.onTickInGame();
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            InvTweaksGuiButtons.addButtons(containerScreen, event);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        InvTweaksTooltipHandler.onRenderScreen(event);
    }

    @SubscribeEvent
    public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }
        InvTweaksConfigManager manager = InvTweaks.getConfigManager();
        if (manager == null || manager.getConfig() == null || manager.getShortcutsHandler() == null) {
            return;
        }
        InvTweaksConfig config = manager.getConfig();
        if (!config.getProperty(InvTweaksConfig.PROP_ENABLE_SHORTCUTS).equals("true")) {
            return;
        }
        if (manager.getShortcutsHandler().handleShortcut()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) {
            return;
        }
        InvTweaks invTweaks = InvTweaks.getInstance();
        if (invTweaks != null) {
            invTweaks.setItemPickupPending(true);
        }
    }
}
