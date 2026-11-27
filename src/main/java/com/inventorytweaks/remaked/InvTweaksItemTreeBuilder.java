package com.inventorytweaks.remaked;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges the ".tree" part files found in the trees/ folder into a single tree file, as the original mod did.
 */
public class InvTweaksItemTreeBuilder {

    private final DocumentBuilder docBuilder;
    private final Document newDoc;

    public InvTweaksItemTreeBuilder(File file) throws ParserConfigurationException, SAXException, IOException {
        docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        newDoc = docBuilder.parse(file);
    }

    public static boolean buildNewTree() {
        if (!InvTweaksConst.INVTWEAKS_TREES_DIR.exists()) {
            return false;
        }

        File base = new File(InvTweaksConst.INVTWEAKS_TREES_DIR, "minecraft.tree");
        if (!base.exists()) {
            base = InvTweaksConst.CONFIG_TREE_FILE;
        }

        if (!base.exists()) {
            return false;
        }

        try {
            InvTweaks.log.info("Merging tree files.");
            InvTweaksItemTreeBuilder newTree = new InvTweaksItemTreeBuilder(base);

            File[] treeFiles = InvTweaksConst.INVTWEAKS_CONFIG_DIR.listFiles();
            if (treeFiles != null) {
                for (File tree : treeFiles) {
                    if (tree.getName().endsWith(".tree")) {
                        try {
                            if (!tree.equals(base)) {
                                newTree.add(tree);
                            }
                        } catch (Exception e) {
                            InvTweaks.log.error("Unable to process partial Tree: " + tree.getName() + " " + e.getMessage());
                        }
                    }
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            File treeFile;
            if (InvTweaksConst.TEMP_DIR.exists()) {
                treeFile = InvTweaksConst.MERGED_TREE_FILE;
                InvTweaks.log.info("Saving merged tree in TEMP folder.");
            } else {
                treeFile = InvTweaksConst.MERGED_TREE_FILE_ALT;
                InvTweaks.log.info("Saving merged tree in 'trees' folder.");
            }
            Result output = new StreamResult(treeFile);
            Source input = new DOMSource(newTree.newDoc);

            transformer.transform(input, output);
            InvTweaks.log.info("Merged tree files.");
            return true;
        } catch (Exception e) {
            InvTweaks.log.error("Fail Building New Tree: " + e.getMessage());
            return false;
        }
    }

    public static List<Element> getChildrenByTagName(Element parent, String name) {
        List<Element> nodeList = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }
        return nodeList;
    }

    private static boolean matchingAttributes(Element i, Element j) {
        if (!i.hasAttributes() && !j.hasAttributes()) {
            return true;
        }

        String iClass = i.getAttribute(InvTweaksItemTreeLoader.ATTR_CLASS);
        String jClass = j.getAttribute(InvTweaksItemTreeLoader.ATTR_CLASS);
        String iId = i.getAttribute(InvTweaksItemTreeLoader.ATTR_ID);
        String jId = j.getAttribute(InvTweaksItemTreeLoader.ATTR_ID);
        String iDamage = i.getAttribute(InvTweaksItemTreeLoader.ATTR_DAMAGE);
        String jDamage = j.getAttribute(InvTweaksItemTreeLoader.ATTR_DAMAGE);
        String iData = i.getAttribute(InvTweaksItemTreeLoader.ATTR_DATA);
        String jData = j.getAttribute(InvTweaksItemTreeLoader.ATTR_DATA);
        String iOre = i.getAttribute(InvTweaksItemTreeLoader.ATTR_OREDICT_NAME);
        String jOre = j.getAttribute(InvTweaksItemTreeLoader.ATTR_OREDICT_NAME);

        if (iClass.equals(jClass) && iId.equals(jId) && iOre.equals(jOre)) {
            if (iClass.isEmpty() && iId.isEmpty()) {
                return true;
            } else if (!iId.isEmpty() && iDamage.equals(jDamage) && iData.equals(jData)) {
                return true;
            } else if (!iClass.isEmpty() && iData.equals(jData)) {
                return true;
            }
        }
        return false;
    }

    public void add(File file) throws ParserConfigurationException, SAXException, IOException {
        Document otherDoc = docBuilder.parse(file);
        Element newElement = newDoc.getDocumentElement();
        Element otherElement = otherDoc.getDocumentElement();
        combine(newElement, otherElement);
    }

    private int getTreeOrder(Element element, int previousOrder) {
        String treeOrder = element.getAttribute(InvTweaksItemTreeLoader.ATTR_TREE_ORDER);
        if (treeOrder.isEmpty()) {
            return previousOrder;
        }
        try {
            return Integer.parseInt(treeOrder);
        } catch (NumberFormatException e) {
            return previousOrder;
        }
    }

    private void combine(Element existing, Element other) {
        int prevOtherChildNodeOrder = Integer.MAX_VALUE;
        for (Node otherChildNode = other.getFirstChild(); otherChildNode != null; otherChildNode = otherChildNode.getNextSibling()) {
            Element newChild = null;
            if (otherChildNode.getNodeType() == Node.ELEMENT_NODE) {
                Element otherChildElement = (Element) otherChildNode;
                int otherChildOrder = getTreeOrder(otherChildElement, prevOtherChildNodeOrder);
                prevOtherChildNodeOrder = otherChildOrder;

                List<Element> matches = getChildrenByTagName(existing, otherChildElement.getTagName());
                for (Element candidate : matches) {
                    if (matchingAttributes(candidate, otherChildElement)) {
                        newChild = candidate;
                        break;
                    }
                }

                if (newChild == null) {
                    newChild = (Element) newDoc.importNode(otherChildElement, true);

                    Node addBeforeNode = null;
                    int prevMainChildOrder = 0;
                    for (Node mainChildNode = existing.getFirstChild(); mainChildNode != null; mainChildNode = mainChildNode.getNextSibling()) {
                        int mainChildOrder = prevMainChildOrder;
                        if (mainChildNode.getNodeType() == Node.ELEMENT_NODE) {
                            mainChildOrder = getTreeOrder((Element) mainChildNode, prevMainChildOrder);
                            prevMainChildOrder = mainChildOrder;
                        }
                        if (mainChildOrder > otherChildOrder) {
                            addBeforeNode = mainChildNode;
                            break;
                        }
                    }
                    existing.insertBefore(newChild, addBeforeNode);
                } else {
                    combine(newChild, otherChildElement);
                }
            }
        }
    }
}
