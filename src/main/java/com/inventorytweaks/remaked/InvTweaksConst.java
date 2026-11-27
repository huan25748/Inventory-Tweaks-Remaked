package com.inventorytweaks.remaked;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

public class InvTweaksConst {

    // Mod version
    public static final String MOD_VERSION = "1.0.0";

    // Mod tree version
    // Change only when the tree evolves significantly enough to need to override all configs
    public static final String TREE_VERSION = "1.20.1";

    // Timing constants
    public static final int RULESET_SWAP_DELAY = 1000;
    public static final int POLLING_DELAY = 3;
    public static final int CHEST_ALGORITHM_SWAP_MAX_INTERVAL = 2000;
    public static final int TOOLTIP_DELAY = 800;

    // File constants
    public static final File MINECRAFT_CONFIG_DIR = FMLPaths.CONFIGDIR.get().toFile();
    public static final File INVTWEAKS_CONFIG_DIR = new File(MINECRAFT_CONFIG_DIR, "InvTweaks");
    public static final File INVTWEAKS_TREES_DIR = new File(INVTWEAKS_CONFIG_DIR, "trees");
    public static final File CONFIG_PROPS_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaks.cfg");
    public static final File CONFIG_RULES_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaksRules.txt");
    public static final File CONFIG_TREE_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaksTree.txt");
    public static final File OLD_CONFIG_TREE_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksTree.txt");
    public static final File OLDER_CONFIG_TREE_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksTree.xml");
    public static final File OLDER_CONFIG_RULES_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksRules.txt");

    public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));
    public static final File MERGED_TREE_FILE = new File(TEMP_DIR, "InvTweaksTree.txt");
    public static final File MERGED_TREE_FILE_ALT = new File(INVTWEAKS_TREES_DIR, "InvTweaksTree.txt");

    public static final String INVTWEAKS_RESOURCE_DOMAIN = "inventorytweaksremaked";
    // Resources live in the jar root (no assets folder needed).
    public static final String DEFAULT_CONFIG_FILE = "/invtweaks_defaultconfig.dat";
    public static final String DEFAULT_CONFIG_TREE_FILE = "/invtweaks_itemtree.xml";
    public static final String TREE_README_FILE = "/invtweaks_tree_readme.txt";

    public static final String HELP_URL = "https://huan25748.github.io/Inventory-Tweaks-Remaked/";
    public static final String TREE_URL = "https://github.com/IMarvinTPA/InventoryTweaksTrees";

    // Global mod constants
    public static final String INGAME_LOG_PREFIX = "InvTweaks: ";
    public static final int JIMEOWAN_ID = 54696386; // Used in GUIs

    // Minecraft constants
    public static final int INVENTORY_SIZE = 36;
    public static final int INVENTORY_ROW_SIZE = 9;
    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_HOTBAR_SIZE = INVENTORY_ROW_SIZE;
    public static final int DAMAGE_WILDCARD = 32767;
}
