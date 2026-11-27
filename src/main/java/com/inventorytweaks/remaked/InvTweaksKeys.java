package com.inventorytweaks.remaked;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's key bindings. The sorting key stays configurable through Minecraft's controls screen, like in 1.12.
 */
public class InvTweaksKeys {

    public static final String CATEGORY = "key.categories.inventory";

    // The description doubles as the display name: Minecraft falls back to the given text when no
    // translation exists, which keeps the controls screen readable without shipping a lang asset.
    public static final KeyMapping SORT = new KeyMapping(InvTweaksText.get("key.inventorytweaks.sort"), InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, CATEGORY);

    private InvTweaksKeys() {
    }

    public static int getSortKeyCode() {
        return SORT.getKey().getValue();
    }
}
