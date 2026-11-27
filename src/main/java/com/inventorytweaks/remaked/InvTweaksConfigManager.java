package com.inventorytweaks.remaked;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Handles the (re)loading of the configuration, and all that is related to file extraction/moves.
 *
 * @author Jimeo Wan
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class InvTweaksConfigManager {

    private final Minecraft mc;

    /**
     * The mod's configuration.
     */
    private InvTweaksConfig config = null;
    private long storedConfigLastModified = 0;

    private InvTweaksHandlerAutoRefill autoRefillHandler = null;
    private InvTweaksHandlerShortcuts shortcutsHandler = null;

    public InvTweaksConfigManager(Minecraft mc_) {
        mc = mc_;
    }

    private static long computeConfigLastModified() {
        long sum = Long.MIN_VALUE;
        if (InvTweaksConst.INVTWEAKS_TREES_DIR.exists()) {
            File[] treeFiles = InvTweaksConst.INVTWEAKS_TREES_DIR.listFiles();
            if (treeFiles != null) {
                for (File tree : treeFiles) {
                    if (tree.getName().endsWith(".tree")) {
                        sum += tree.lastModified();
                    }
                }
            }
        }
        return sum + InvTweaksConst.CONFIG_RULES_FILE.lastModified() + InvTweaksConst.CONFIG_TREE_FILE.lastModified();
    }

    private static void backupFile(File file) {
        File newFile = new File(file.getParentFile(), file.getName() + ".bak");
        InvTweaks.log.warn("Backing up file: " + file.getAbsolutePath() + " to " + newFile.getAbsolutePath());
        if (newFile.exists()) {
            InvTweaks.log.warn("New file " + newFile.getAbsolutePath() + " already exists, deleting old.");
            newFile.delete();
        }
        file.renameTo(newFile);
    }

    private static void showConfigErrors(InvTweaksConfig config) {
        List<String> invalid = config.getInvalidKeywords();
        if (!invalid.isEmpty()) {
            StringBuilder error = new StringBuilder(InvTweaksText.get("invtweaks.loadconfig.invalidkeywords")).append(": ");
            for (String keyword : config.getInvalidKeywords()) {
                error.append(keyword).append(" ");
            }
            InvTweaks.logInGameStatic(error.toString());
        }
    }

    public boolean makeSureConfigurationIsLoaded() {
        // Load properties
        try {
            if (config != null && config.refreshProperties()) {
                shortcutsHandler = new InvTweaksHandlerShortcuts(mc, config);

                if (config.getProperty(InvTweaksConfig.PROP_ENABLE_CONFIG_LOADED_MESSAGE).equals(InvTweaksConfig.VALUE_TRUE)) {
                    InvTweaks.logInGameStatic("invtweaks.propsfile.loaded");
                }
            }
        } catch (IOException e) {
            InvTweaks.logInGameErrorStatic("invtweaks.loadconfig.refresh.error", e);
        }

        // Load rules + tree files
        long configLastModified = computeConfigLastModified();
        if (config != null) {
            return storedConfigLastModified == configLastModified || loadConfig();
        } else {
            storedConfigLastModified = configLastModified;
            return loadConfig();
        }
    }

    public InvTweaksConfig getConfig() {
        return config;
    }

    public InvTweaksHandlerAutoRefill getAutoRefillHandler() {
        return autoRefillHandler;
    }

    public InvTweaksHandlerShortcuts getShortcutsHandler() {
        return shortcutsHandler;
    }

    /**
     * Tries to load mod configuration from file, with error handling. If it fails, the config attribute will remain
     * null.
     */
    private boolean loadConfig() {
        // Ensure the config folder exists
        File configDir = InvTweaksConst.MINECRAFT_CONFIG_DIR;
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Create the Config file directory.
        if (!InvTweaksConst.INVTWEAKS_CONFIG_DIR.exists()) {
            InvTweaksConst.INVTWEAKS_CONFIG_DIR.mkdirs();
        }

        if (!InvTweaksConst.INVTWEAKS_TREES_DIR.exists()) {
            if (InvTweaksConst.INVTWEAKS_TREES_DIR.mkdirs()) {
                extractFile(InvTweaksConst.TREE_README_FILE,
                        new File(InvTweaksConst.INVTWEAKS_TREES_DIR, "readme.txt"));
            }
        }

        // Compatibility: Tree version check
        try {
            if (!InvTweaksItemTreeLoader.isValidVersion(InvTweaksConst.CONFIG_TREE_FILE)) {
                backupFile(InvTweaksConst.CONFIG_TREE_FILE);
            }
        } catch (Exception e) {
            InvTweaks.log.warn("Failed to check item tree version: " + e.getMessage());
        }

        // Compatibility: File names check
        if (InvTweaksConst.OLD_CONFIG_TREE_FILE.exists()) {
            if (InvTweaksConst.CONFIG_RULES_FILE.exists()) {
                backupFile(InvTweaksConst.CONFIG_TREE_FILE);
            }
            InvTweaksConst.OLD_CONFIG_TREE_FILE.renameTo(InvTweaksConst.CONFIG_TREE_FILE);
        } else if (InvTweaksConst.OLDER_CONFIG_RULES_FILE.exists()) {
            if (InvTweaksConst.CONFIG_RULES_FILE.exists()) {
                backupFile(InvTweaksConst.CONFIG_RULES_FILE);
            }
            InvTweaksConst.OLDER_CONFIG_RULES_FILE.renameTo(InvTweaksConst.CONFIG_RULES_FILE);
        }

        // Create missing files
        if (!InvTweaksConst.CONFIG_RULES_FILE.exists() && extractFile(InvTweaksConst.DEFAULT_CONFIG_FILE, InvTweaksConst.CONFIG_RULES_FILE)) {
            // First run: only log it, players do not need a chat message.
            InvTweaks.log.info("Created default config: {}", InvTweaksConst.CONFIG_RULES_FILE);
        }
        if (!InvTweaksConst.CONFIG_TREE_FILE.exists() && extractFile(InvTweaksConst.DEFAULT_CONFIG_TREE_FILE, InvTweaksConst.CONFIG_TREE_FILE)) {
            InvTweaks.log.info("Created default item tree: {}", InvTweaksConst.CONFIG_TREE_FILE);
        }

        boolean treeBuilt = false;
        if (InvTweaksConst.INVTWEAKS_TREES_DIR.exists()) {
            treeBuilt = InvTweaksItemTreeBuilder.buildNewTree();
        }

        storedConfigLastModified = computeConfigLastModified();

        // Load
        String error = null;
        Exception errorException = null;

        try {
            // Configuration creation
            if (config == null) {
                if (treeBuilt & InvTweaksConst.MERGED_TREE_FILE.exists()) {
                    config = new InvTweaksConfig(InvTweaksConst.CONFIG_RULES_FILE, InvTweaksConst.MERGED_TREE_FILE);
                } else if (treeBuilt & InvTweaksConst.MERGED_TREE_FILE_ALT.exists()) {
                    config = new InvTweaksConfig(InvTweaksConst.CONFIG_RULES_FILE, InvTweaksConst.MERGED_TREE_FILE_ALT);
                } else {
                    config = new InvTweaksConfig(InvTweaksConst.CONFIG_RULES_FILE, InvTweaksConst.CONFIG_TREE_FILE);
                }
                autoRefillHandler = new InvTweaksHandlerAutoRefill(mc, config);
                shortcutsHandler = new InvTweaksHandlerShortcuts(mc, config);
            }

            // Configuration loading
            config.load();
            shortcutsHandler.loadShortcuts();

            if (config.getProperty(InvTweaksConfig.PROP_ENABLE_CONFIG_LOADED_MESSAGE).equals(InvTweaksConfig.VALUE_TRUE)) {
                InvTweaks.logInGameStatic("invtweaks.loadconfig.done");
            }
            showConfigErrors(config);
        } catch (FileNotFoundException e) {
            error = "Config file not found";
            errorException = e;
        } catch (Exception e) {
            error = "Error while loading config";
            errorException = e;
        }

        if (error != null) {
            InvTweaks.log.error(error);
            InvTweaks.logInGameErrorStatic(error, errorException);

            try {
                backupFile(InvTweaksConst.CONFIG_TREE_FILE);
                backupFile(InvTweaksConst.CONFIG_RULES_FILE);
                backupFile(InvTweaksConst.CONFIG_PROPS_FILE);

                extractFile(InvTweaksConst.DEFAULT_CONFIG_FILE, InvTweaksConst.CONFIG_RULES_FILE);
                extractFile(InvTweaksConst.DEFAULT_CONFIG_TREE_FILE, InvTweaksConst.CONFIG_TREE_FILE);

                config = new InvTweaksConfig(InvTweaksConst.CONFIG_RULES_FILE, InvTweaksConst.CONFIG_TREE_FILE);
                autoRefillHandler = new InvTweaksHandlerAutoRefill(mc, config);
                shortcutsHandler = new InvTweaksHandlerShortcuts(mc, config);

                config.load();
                shortcutsHandler.loadShortcuts();
            } catch (Exception e) {
                config = null;
                autoRefillHandler = null;
                shortcutsHandler = null;

                if (e.getCause() == null) {
                    e.initCause(errorException);
                }
                InvTweaks.log.error("InvTweaks config load failed", e);
            }

            return false;
        } else {
            return true;
        }
    }

    private boolean extractFile(String resourcePath, File destination) {
        try (InputStream input = InvTweaksConfigManager.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                InvTweaks.logInGameErrorStatic(String.format(InvTweaksText.get("invtweaks.extract.find.error"), resourcePath), null);
                return false;
            }
            Files.copy(input, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            InvTweaks.logInGameErrorStatic("invtweaks.extract.create.error", e);
            return false;
        }
    }
}
