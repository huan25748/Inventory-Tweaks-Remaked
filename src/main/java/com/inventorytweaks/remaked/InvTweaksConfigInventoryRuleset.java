package com.inventorytweaks.remaked;


import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stores a whole configuration defined by rules. Several of them can be stored in the global configuration, as the mod
 * supports several rule configurations.
 *
 * @author Jimeo Wan
 */
public class InvTweaksConfigInventoryRuleset {
    private static final Pattern rulePattern = Pattern.compile("^(?:(?:[a-d1-9]r?)|(?:[a-d][1-9](?:-[a-d][1-9](?:rv?|vr?)?)?)) \\w+$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    private final String name;
    private final int[] lockPriorities;
    private final boolean[] frozenSlots;
    private final List<InvTweaksConfigSortingRule> rules;
    private final List<String> autoReplaceRules;
    private final InvTweaksItemTree tree;

    public InvTweaksConfigInventoryRuleset(InvTweaksItemTree tree_, String name_) {
        tree = tree_;
        name = name_.trim();

        lockPriorities = new int[InvTweaksConst.INVENTORY_SIZE];
        frozenSlots = new boolean[InvTweaksConst.INVENTORY_SIZE];

        rules = new ArrayList<>();
        autoReplaceRules = new ArrayList<>();
    }

    /**
     * @return If not null, returns the invalid keyword found
     * @throws InvalidParameterException when the line is not a valid rule
     */
    public String registerLine(String rawLine) throws InvalidParameterException {
        InvTweaksConfigSortingRule newRule;
        String lineText = rawLine.replaceAll("\\s+", " ");
        String[] words = lineText.split(" ");

        // Parse valid lines only
        if (words.length == 2) {
            // Standard rules format
            if (rulePattern.matcher(lineText).matches()) {
                if (words[1].equalsIgnoreCase(InvTweaksConfig.LOCKED)) {
                    // Locking rule
                    int[] newLockedSlots = InvTweaksConfigSortingRule.getRulePreferredPositions(words[0], InvTweaksConst.INVENTORY_SIZE, InvTweaksConst.INVENTORY_ROW_SIZE);
                    int lockPriority = InvTweaksConfigSortingRule.getRuleType(words[0], InvTweaksConst.INVENTORY_ROW_SIZE).getLowestPriority() - 1;
                    if (newLockedSlots != null) {
                        for (int i : newLockedSlots) {
                            if (i >= 0 && i < lockPriorities.length) {
                                lockPriorities[i] = lockPriority;
                            }
                        }
                    }
                    return null;
                } else if (words[1].equalsIgnoreCase(InvTweaksConfig.FROZEN)) {
                    // Freeze rule
                    int[] newLockedSlots = InvTweaksConfigSortingRule.getRulePreferredPositions(words[0], InvTweaksConst.INVENTORY_SIZE, InvTweaksConst.INVENTORY_ROW_SIZE);
                    if (newLockedSlots != null) {
                        for (int i : newLockedSlots) {
                            if (i >= 0 && i < frozenSlots.length) {
                                frozenSlots[i] = true;
                            }
                        }
                    }
                    return null;
                } else {
                    // Standard rule
                    String keyword = words[1];
                    boolean isValidKeyword = tree.isKeywordValid(keyword);

                    if (isValidKeyword) {
                        newRule = new InvTweaksConfigSortingRule(tree, words[0], keyword, InvTweaksConst.INVENTORY_SIZE, InvTweaksConst.INVENTORY_ROW_SIZE);
                        rules.add(newRule);
                        return null;
                    } else {
                        return keyword;
                    }
                }
            } else if (words[0].equalsIgnoreCase(InvTweaksConfig.AUTOREFILL) || words[0].equalsIgnoreCase("autoreplace")) {
                // Autoreplace rule
                if (tree.isKeywordValid(words[1]) || words[1].equalsIgnoreCase(InvTweaksConfig.AUTOREFILL_NOTHING)) {
                    autoReplaceRules.add(words[1]);
                }
                return null;
            }
        }

        throw new InvalidParameterException();
    }

    public void finalizeRules() {
        // Default Autoreplace behavior
        if (autoReplaceRules.isEmpty()) {
            try {
                if (tree.getRootCategory() == null) {
                    throw new NullPointerException("No root category is defined.");
                }
                autoReplaceRules.add(tree.getRootCategory().getName());
            } catch (NullPointerException e) {
                throw new NullPointerException("No root category is defined.");
            }
        }

        // Sort rules by priority, highest first
        rules.sort(Collections.reverseOrder());
    }

    public String getName() {
        return name;
    }

    public int[] getLockPriorities() {
        return lockPriorities;
    }

    public boolean[] getFrozenSlots() {
        return frozenSlots;
    }

    public List<InvTweaksConfigSortingRule> getRules() {
        return rules;
    }

    public List<String> getAutoReplaceRules() {
        return autoReplaceRules;
    }
}
