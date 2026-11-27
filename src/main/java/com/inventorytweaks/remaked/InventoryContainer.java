package com.inventorytweaks.remaked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for containers that need special treatment, such as crafting inputs or alternate player inventory positions,
 * but do not have a chest-like component.
 * <p>
 * Does not enable the Inventory Tweaks sorting buttons for this container.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InventoryContainer {
    boolean showOptions() default true;
}
