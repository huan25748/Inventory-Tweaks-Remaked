package com.inventorytweaks.remaked;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jimeo Wan
 */
public class InvTweaksShortcutMapping {

    private final List<Integer> keysToHold = new ArrayList<>();

    public InvTweaksShortcutMapping(int keyCode) {
        keysToHold.add(keyCode);
    }

    public InvTweaksShortcutMapping(String... keyNames) {
        for (String keyName : keyNames) {
            keysToHold.add(Keys.getCode(keyName));
        }
    }

    public boolean isTriggered(Map<Integer, Boolean> pressedKeys) {
        for (Integer keyToHold : keysToHold) {
            if (keyToHold != Keys.getCode("LCONTROL")) {
                Boolean pressed = pressedKeys.get(keyToHold);
                if (pressed == null || !pressed) {
                    return false;
                }
            }
            // AltGr also activates LCtrl, make sure the real LCtrl has been pressed
            else if (pressedKeys.get(keyToHold) == null || !pressedKeys.get(keyToHold) || Keys.isDown(Keys.getCode("RALT"))) {
                return false;
            }
        }
        return true;
    }

    public List<Integer> getKeyCodes() {
        return this.keysToHold;
    }
}
