package com.inventorytweaks.remaked;

public enum InvTweaksConfigSortingRuleType {

    RECTANGLE(1), ROW(2), COLUMN(3), SLOT(4);

    private final int lowestPriority;

    InvTweaksConfigSortingRuleType(int priorityLevel) {
        lowestPriority = priorityLevel * 1000000;
    }

    // Used for computing rule priorities
    public int getLowestPriority() {
        return lowestPriority;
    }
}
