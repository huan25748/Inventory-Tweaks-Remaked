package com.inventorytweaks.remaked;

import com.mojang.logging.LogUtils;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod entry point. Inventory Tweaks is a client-side mod: no blocks, items or packets are registered.
 */
@Mod(InvTweaksMod.MODID)
public class InvTweaksMod {

    public static final String MODID = "inventorytweaksremaked";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InvTweaksMod() {
        // Forge 47 (1.20.1) instantiates the mod class through its no-arg constructor only, so the deprecated
        // FMLJavaModLoadingContext accessor is still the supported way to reach the mod event bus here.
        FMLJavaModLoadingContext.get().getModEventBus().addListener(InvTweaksMod::registerKeyMappings);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(InvTweaksKeys.SORT);
    }
}
