package com.inventorytweaks.remaked;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stores a sorting rule, as a target plus a keyword. The target is provided as an array of preferred slots (ex: target
 * "1", i.e. first column, is stored as [0, 9, 18, 27])
 *
 * @author Jimeo Wan
 */
public class InvTweaksConfigSortingRule implements Comparable<InvTweaksConfigSortingRule> {
    private final String constraint;
    private final int[] preferredPositions;
    private final String keyword;
    private final InvTweaksConfigSortingRuleType type;
    private final int priority;
    private final int containerSize;
    private final int containerRowSize;

    public InvTweaksConfigSortingRule(InvTweaksItemTree tree, String constraint_, String keyword_, int containerSize_, int containerRowSize_) {
        keyword = keyword_;
        constraint = constraint_;
        containerSize = containerSize_;
        containerRowSize = containerRowSize_;
        type = getRuleType(constraint, containerRowSize);
        preferredPositions = getRulePreferredPositions(constraint);

        // Compute priority
        // 1st criteria : the rule type
        // 2nd criteria : the keyword category depth
        // 3rd criteria : the item order in a same category
        priority = type.getLowestPriority() + 100000 + tree.getKeywordDepth(keyword) * 1000 - tree.getKeywordOrder(keyword);
    }

    public static int[] getRulePreferredPositions(String constraint, int containerSize, int containerRowSize) {
        int[] result = null;
        int containerColumnSize = containerSize / containerRowSize;

        // Rectangle rules
        if (constraint.length() >= 5) {
            boolean vertical = false;
            if (constraint.toLowerCase().contains("v")) {
                vertical = true;
                constraint = constraint.replace("v", "").replace("V", "");
            }
            String[] elements = constraint.split("-");
            if (elements.length == 2) {
                int[] slots1 = getRulePreferredPositions(elements[0], containerSize, containerRowSize);
                int[] slots2 = getRulePreferredPositions(elements[1], containerSize, containerRowSize);
                if (slots1 != null && slots2 != null && slots1.length == 1 && slots2.length == 1) {
                    int slot1 = slots1[0], slot2 = slots2[0];
                    int point1x = slot1 % containerRowSize, point1y = slot1 / containerRowSize;
                    int point2x = slot2 % containerRowSize, point2y = slot2 / containerRowSize;

                    result = new int[(Math.abs(point2y - point1y) + 1) * (Math.abs(point2x - point1x) + 1)];
                    int resultIndex = 0;

                    // Swap coordinates for vertical ordering
                    if (vertical) {
                        int buffer = point1x;
                        point1x = point1y;
                        point1y = buffer;
                        buffer = point2x;
                        point2x = point2y;
                        point2y = buffer;
                    }

                    int y = point1y;
                    while ((point1y < point2y) ? y <= point2y : y >= point2y) {
                        int x = point1x;
                        while ((point1x < point2x) ? x <= point2x : x >= point2x) {
                            result[resultIndex++] = (vertical) ? index(containerRowSize, x, y) : index(containerRowSize, y, x);
                            x += (point1x < point2x) ? 1 : -1;
                        }
                        y += (point1y < point2y) ? 1 : -1;
                    }

                    if (constraint.toLowerCase().contains("r")) {
                        reverseArray(result);
                    }
                }
            }
        } else {
            // Default values
            int column = -1, row = -1;
            boolean reverse = false;

            // Extract chars
            for (int i = 0; i < constraint.length(); i++) {
                char c = constraint.charAt(i);
                int digitValue = Character.digit(c, 36); // radix-36 maps 0-9 to 0-9, and [a-zA-Z] to 10-36
                if (digitValue >= 1 && digitValue <= containerRowSize && digitValue < 10) {
                    // 1 column = 0, 9 column = 8
                    column = digitValue - 1;
                } else if (digitValue >= 10 && (digitValue - 10) <= containerColumnSize) {
                    // A row = 0, D row = 3, H row = 7
                    row = digitValue - 10;
                } else if (charEqualsIgnoreCase(c, 'r')) {
                    reverse = true;
                }
            }

            // Tile case
            if (column != -1 && row != -1) {
                result = new int[]{index(containerRowSize, row, column)};
            }
            // Row case
            else if (row != -1) {
                result = new int[containerRowSize];
                for (int i = 0; i < containerRowSize; i++) {
                    result[i] = index(containerRowSize, row, reverse ? containerRowSize - 1 - i : i);
                }
            }
            // Column case
            else if (column != -1) {
                result = new int[containerColumnSize];
                for (int i = 0; i < containerColumnSize; i++) {
                    result[i] = index(containerRowSize, reverse ? i : containerColumnSize - 1 - i, column);
                }
            }
        }

        if (result == null) {
            InvTweaks.logInGameStatic("InvTweaks Config: Rule Constraint \"" + constraint + "\" was unable to be correctly determined.");
        }
        return result;
    }

    public static InvTweaksConfigSortingRuleType getRuleType(String constraint, int rowSize) {
        InvTweaksConfigSortingRuleType result = InvTweaksConfigSortingRuleType.SLOT;

        if (constraint.length() == 1 || (constraint.length() == 2 && constraint.toLowerCase().contains("r"))) {
            constraint = constraint.replace("r", "").replace("R", "");
            // Column rule
            int digitValue = Character.digit(constraint.charAt(0), 10);
            if (digitValue >= 1 && digitValue <= rowSize) {
                result = InvTweaksConfigSortingRuleType.COLUMN;
            }
            // Row rule
            else {
                result = InvTweaksConfigSortingRuleType.ROW;
            }
        }
        // Rectangle rule
        else if (constraint.length() > 4) {
            // Special case: rectangle rule on a single column
            if (charEqualsIgnoreCase(constraint.charAt(1), constraint.charAt(4))) {
                result = InvTweaksConfigSortingRuleType.COLUMN;
            }
            // Special case: rectangle rule on a single row
            else if (charEqualsIgnoreCase(constraint.charAt(0), constraint.charAt(3))) {
                result = InvTweaksConfigSortingRuleType.ROW;
            }
            // Usual case
            else {
                result = InvTweaksConfigSortingRuleType.RECTANGLE;
            }
        }

        return result;
    }

    private static boolean charEqualsIgnoreCase(char a, char b) {
        char aU = Character.toUpperCase(a), bU = Character.toUpperCase(b);
        return aU == bU || Character.toLowerCase(aU) == Character.toLowerCase(bU);
    }

    private static int index(int rowSize, int row, int column) {
        return row * rowSize + column;
    }

    private static void reverseArray(int[] data) {
        int left = 0;
        int right = data.length - 1;
        while (left < right) {
            int temp = data[left];
            data[left] = data[right];
            data[right] = temp;
            left++;
            right--;
        }
    }

    public InvTweaksConfigSortingRuleType getType() {
        return type;
    }

    /**
     * @return An array of preferred positions (from the most to the less preferred).
     */
    public int[] getPreferredSlots() {
        return preferredPositions;
    }

    public String getKeyword() {
        return keyword;
    }

    public int getPriority() {
        return priority;
    }

    public int getContainerSize() {
        return containerSize;
    }

    @Override
    public int compareTo(InvTweaksConfigSortingRule o) {
        return priority - o.priority;
    }

    public int[] getRulePreferredPositions(String constraint) {
        return InvTweaksConfigSortingRule.getRulePreferredPositions(constraint, containerSize, containerRowSize);
    }

    @Override
    public String toString() {
        return constraint + " " + keyword;
    }
}
