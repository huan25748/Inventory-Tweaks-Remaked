package com.inventorytweaks.remaked;

import java.util.Properties;
import java.util.TreeSet;

public class InvTweaksConfigProperties extends Properties {
    private static final long serialVersionUID = 1L;

    /**
     * Sorts the keys so that the produced file stays readable and diff-friendly.
     */
    public void sortKeys() {
        TreeSet<Object> sortedKeys = new TreeSet<>(this.keySet());
        Properties sorted = new Properties();
        for (Object key : sortedKeys) {
            sorted.put(key, get(key));
        }
        clear();
        putAll(sorted);
    }
}
