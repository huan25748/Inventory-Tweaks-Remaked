package com.inventorytweaks.remaked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker for containers that have a chest-like persistent storage component. Enables the Inventory Tweaks sorting
 * buttons for this container.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChestContainer {
    boolean showButtons() default true;

    int rowSize() default 9;

    boolean isLargeChest() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface RowSizeCallback {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface IsLargeCallback {
    }
}
