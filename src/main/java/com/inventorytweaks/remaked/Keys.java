package com.inventorytweaks.remaked;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Platform key handling for 1.21.1 (GLFW based, replaces LWJGL2 Keyboard/Mouse of 1.12.2).
 * Keyboard keys use their GLFW key code, mouse buttons use -(100 + button).
 */
public final class Keys {
    /** Mouse buttons are stored as negative key codes, like Minecraft's InputConstants (left button = -100). */
    public static final int MOUSE_OFFSET = 100;
    public static final int MOUSE_BASE = -100;

    private static final Map<String, Integer> NAMES = new HashMap<>();

    static {
        for (int i = 0; i <= 12; i++) {
            NAMES.put("BUTTON" + i, MOUSE_BASE + i);
            NAMES.put("MOUSE" + i, MOUSE_BASE + i);
        }

        NAMES.put("SPACE", GLFW.GLFW_KEY_SPACE);
        NAMES.put("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        NAMES.put("COMMA", GLFW.GLFW_KEY_COMMA);
        NAMES.put("MINUS", GLFW.GLFW_KEY_MINUS);
        NAMES.put("PERIOD", GLFW.GLFW_KEY_PERIOD);
        NAMES.put("SLASH", GLFW.GLFW_KEY_SLASH);
        NAMES.put("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        NAMES.put("EQUALS", GLFW.GLFW_KEY_EQUAL);
        NAMES.put("LBRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        NAMES.put("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
        NAMES.put("RBRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
        NAMES.put("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT);
        NAMES.put("ESCAPE", GLFW.GLFW_KEY_ESCAPE);
        NAMES.put("RETURN", GLFW.GLFW_KEY_ENTER);
        NAMES.put("ENTER", GLFW.GLFW_KEY_ENTER);
        NAMES.put("TAB", GLFW.GLFW_KEY_TAB);
        NAMES.put("BACK", GLFW.GLFW_KEY_BACKSPACE);
        NAMES.put("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        NAMES.put("INSERT", GLFW.GLFW_KEY_INSERT);
        NAMES.put("DELETE", GLFW.GLFW_KEY_DELETE);
        NAMES.put("RIGHT", GLFW.GLFW_KEY_RIGHT);
        NAMES.put("LEFT", GLFW.GLFW_KEY_LEFT);
        NAMES.put("DOWN", GLFW.GLFW_KEY_DOWN);
        NAMES.put("UP", GLFW.GLFW_KEY_UP);
        NAMES.put("PAGEUP", GLFW.GLFW_KEY_PAGE_UP);
        NAMES.put("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        NAMES.put("HOME", GLFW.GLFW_KEY_HOME);
        NAMES.put("END", GLFW.GLFW_KEY_END);
        NAMES.put("CAPITAL", GLFW.GLFW_KEY_CAPS_LOCK);
        NAMES.put("CAPSLOCK", GLFW.GLFW_KEY_CAPS_LOCK);
        NAMES.put("NUMLOCK", GLFW.GLFW_KEY_NUM_LOCK);
        NAMES.put("SCROLL", GLFW.GLFW_KEY_SCROLL_LOCK);
        NAMES.put("PRINTSCREEN", GLFW.GLFW_KEY_PRINT_SCREEN);
        NAMES.put("PAUSE", GLFW.GLFW_KEY_PAUSE);
        NAMES.put("SUBTRACT", GLFW.GLFW_KEY_KP_SUBTRACT);
        NAMES.put("ADD", GLFW.GLFW_KEY_KP_ADD);
        NAMES.put("DECIMAL", GLFW.GLFW_KEY_KP_DECIMAL);
        NAMES.put("DIVIDE", GLFW.GLFW_KEY_KP_DIVIDE);
        NAMES.put("MULTIPLY", GLFW.GLFW_KEY_KP_MULTIPLY);

        NAMES.put("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        NAMES.put("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        NAMES.put("LCONTROL", GLFW.GLFW_KEY_LEFT_CONTROL);
        NAMES.put("RCONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        NAMES.put("LALT", GLFW.GLFW_KEY_LEFT_ALT);
        NAMES.put("RALT", GLFW.GLFW_KEY_RIGHT_ALT);
        NAMES.put("LMENU", GLFW.GLFW_KEY_LEFT_ALT);
        NAMES.put("RMENU", GLFW.GLFW_KEY_RIGHT_ALT);
        NAMES.put("LWIN", GLFW.GLFW_KEY_LEFT_SUPER);
        NAMES.put("RWIN", GLFW.GLFW_KEY_RIGHT_SUPER);

        for (char c = 'A'; c <= 'Z'; c++) {
            NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        }
        for (char c = '0'; c <= '9'; c++) {
            NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_0 + (c - '0'));
            NAMES.put("NUMPAD" + c, GLFW.GLFW_KEY_KP_0 + (c - '0'));
        }
        for (int i = 1; i <= 25; i++) {
            NAMES.put("F" + i, GLFW.GLFW_KEY_F1 + (i - 1));
        }
    }

    private Keys() {
    }

    public static int getCode(String name) {
        if (name == null) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        String key = name.trim().toUpperCase();
        if (key.isEmpty()) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        if (key.startsWith("KEY_")) {
            key = key.substring(4);
        }
        // LWJGL2 named the Alt keys LMENU/RMENU.
        if (key.equals("LMENU")) {
            key = "LALT";
        } else if (key.equals("RMENU")) {
            key = "RALT";
        }
        Integer code = NAMES.get(key);
        if (code != null) {
            return code;
        }
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
    }

    public static String getName(int code) {
        if (code < 0) {
            return "BUTTON" + (code - MOUSE_BASE);
        }
        String name = GLFW.glfwGetKeyName(code, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        for (Map.Entry<String, Integer> entry : NAMES.entrySet()) {
            if (entry.getValue() == code && !entry.getKey().startsWith("BUTTON") && !entry.getKey().startsWith("MOUSE")) {
                return entry.getKey();
            }
        }
        return String.valueOf(code);
    }

    public static boolean isDown(int code) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }
        long window = mc.getWindow().getWindow();
        if (code < 0) {
            return GLFW.glfwGetMouseButton(window, code - MOUSE_BASE) == GLFW.GLFW_PRESS;
        }
        if (code != GLFW.GLFW_KEY_UNKNOWN && code != 0) {
            return InputConstants.isKeyDown(window, code);
        }
        return false;
    }

    public static boolean isMouseDown(int button) {
        return isDown(MOUSE_BASE + button);
    }
}
