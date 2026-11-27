package com.inventorytweaks.remaked;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Loads the item tree by parsing the XML file.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTreeLoader extends DefaultHandler {

    public static final String ATTR_ID = "id";
    public static final String ATTR_DAMAGE = "damage";
    public static final String ATTR_RANGE_DMIN = "dmin";
    public static final String ATTR_RANGE_DMAX = "dmax";
    public static final String ATTR_OREDICT_NAME = "oreDictName";
    public static final String ATTR_DATA = "data";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_LAST_ORDER = "mergePrevious";
    public static final String ATTR_MERGE_CHILDREN = "mergeChildren";
    public static final String ATTR_TREE_VERSION = "treeVersion";
    public static final String ATTR_TREE_ORDER = "treeOrder";

    private static final List<IItemTreeListener> onLoadListeners = new ArrayList<>();
    private static InvTweaksItemTree tree;
    private static String treeVersion;
    private static int itemOrder;
    private static int mergeChildren;
    private static LinkedList<String> categoryStack;
    private static LinkedList<Boolean> mergeStack;
    private static boolean treeLoaded = false;

    private static void init() {
        treeVersion = null;
        tree = new InvTweaksItemTree();
        itemOrder = 0;
        mergeChildren = 0;
        categoryStack = new LinkedList<>();
        mergeStack = new LinkedList<>();
    }

    public static synchronized InvTweaksItemTree load(File file) throws Exception {
        init();

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(false);
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(file, new InvTweaksItemTreeLoader());

        synchronized (onLoadListeners) {
            treeLoaded = true;
            for (IItemTreeListener onLoadListener : onLoadListeners) {
                onLoadListener.onTreeLoaded(tree);
            }
        }

        return tree;
    }

    public static synchronized boolean isValidVersion(File file) throws Exception {
        init();

        if (file.exists()) {
            treeVersion = null;
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(false);
            SAXParser parser = parserFactory.newSAXParser();

            VersionLoader loader = new VersionLoader();
            parser.parse(file, loader);
            return InvTweaksConst.TREE_VERSION.equals(loader.version);
        } else {
            return false;
        }
    }

    public static synchronized void addOnLoadListener(IItemTreeListener listener) {
        onLoadListeners.add(listener);
        if (treeLoaded) {
            listener.onTreeLoaded(tree);
        }
    }

    public static synchronized boolean removeOnLoadListener(IItemTreeListener listener) {
        return onLoadListeners.remove(listener);
    }

    private int getNextItemOrder(boolean lastOrder) {
        if ((lastOrder || mergeChildren > 0) && itemOrder > 0) {
            return itemOrder - 1;
        }
        return itemOrder++;
    }

    @Override
    public synchronized void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        String rangeDMinAttr = attributes.getValue(ATTR_RANGE_DMIN);
        String newTreeVersion = attributes.getValue(ATTR_TREE_VERSION);
        String id = attributes.getValue(ATTR_ID);
        String className = attributes.getValue(ATTR_CLASS);
        String lastOrderValue = attributes.getValue(ATTR_LAST_ORDER);
        String mergeChildrenValue = attributes.getValue(ATTR_MERGE_CHILDREN);
        lastOrderValue = lastOrderValue == null ? "" : lastOrderValue.toLowerCase();
        boolean lastOrder = (lastOrderValue.equals("1") || lastOrderValue.equals("true") || lastOrderValue.equals("yes")
                || lastOrderValue.equals("t") || lastOrderValue.equals("y"));
        mergeChildrenValue = mergeChildrenValue == null ? "" : mergeChildrenValue.toLowerCase();
        boolean willMergeChildren = (mergeChildrenValue.equals("1") || mergeChildrenValue.equals("true") || mergeChildrenValue.equals("yes")
                || mergeChildrenValue.equals("t") || mergeChildrenValue.equals("y"));
        mergeStack.add(willMergeChildren);

        if (treeVersion == null) {
            treeVersion = newTreeVersion;
        }

        String parent = categoryStack.isEmpty() ? null : categoryStack.getLast();
        String path = String.join("\\", categoryStack) + "\\" + name;

        if (id != null) {
            int damage = InvTweaksConst.DAMAGE_WILDCARD;
            if (attributes.getValue(ATTR_DAMAGE) != null) {
                damage = Integer.parseInt(attributes.getValue(ATTR_DAMAGE));
            }
            tree.addItem(parent, new InvTweaksItemTreeItem(name, id, damage, getNextItemOrder(lastOrder), path));
        } else if (attributes.getValue(ATTR_OREDICT_NAME) != null) {
            // Ore dictionary is gone in 1.21; entries are resolved through item tags if present.
        } else if (className != null) {
            Map<String, String> extraData = parseData(attributes.getValue(ATTR_DATA));
            tree.registerClass(parent, name, className.toLowerCase(), extraData, getNextItemOrder(lastOrder), path);
        } else {
            // Category
            if (categoryStack.isEmpty()) {
                tree.setRootCategory(new InvTweaksItemTreeCategory(name));
            } else {
                tree.addCategory(parent, new InvTweaksItemTreeCategory(name));
            }

            if (rangeDMinAttr != null) {
                int rangeDMin = Integer.parseInt(rangeDMinAttr);
                int rangeDMax = Integer.parseInt(attributes.getValue(ATTR_RANGE_DMAX));
                for (int damage = rangeDMin; damage <= rangeDMax; damage++) {
                    tree.addItem(name, new InvTweaksItemTreeItem(name + id + "-" + damage, id, damage, getNextItemOrder(lastOrder), path));
                }
            } else if (willMergeChildren) {
                getNextItemOrder(lastOrder);
            }
            categoryStack.add(name);
        }

        if (willMergeChildren) {
            mergeChildren++;
        }
    }

    /**
     * Parses a legacy "{key : "value"}" data attribute into a simple map. Keys and values are lowercased, as the
     * 1.12 loader lowercased the whole JSON blob before parsing it.
     */
    public static Map<String, String> parseData(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String data = raw.toLowerCase();
        int open = data.indexOf('{');
        int close = data.lastIndexOf('}');
        if (open >= 0 && close > open) {
            data = data.substring(open + 1, close);
        }
        for (String pair : data.split(",")) {
            int colon = pair.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = pair.substring(0, colon).trim().replace("\"", "").replace("'", "");
            String value = pair.substring(colon + 1).trim().replace("\"", "").replace("'", "");
            if (!key.isEmpty()) {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public synchronized void endElement(String uri, String localName, String name) throws SAXException {
        if (!categoryStack.isEmpty() && name.equals(categoryStack.getLast())) {
            categoryStack.removeLast();
        }
        if (!mergeStack.isEmpty()) {
            if (mergeStack.getLast() && mergeChildren > 0) {
                mergeChildren--;
            }
            mergeStack.removeLast();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        tree.endFileRead();
    }

    @Override
    public void warning(SAXParseException e) {
        InvTweaks.log.warn("Tree XML Warning: " + e.getMessage());
    }

    @Override
    public void error(SAXParseException e) {
        InvTweaks.log.error("Tree XML Error: " + e.getMessage());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        InvTweaks.log.error("Tree XML Fatal Error: " + e.getMessage());
        throw e;
    }

    private static class VersionLoader extends DefaultHandler {
        String version;

        @Override
        public synchronized void startElement(String uri, String localName, String name, Attributes attributes) {
            if (version == null) {
                version = attributes.getValue(ATTR_TREE_VERSION);
            }
        }
    }
}
