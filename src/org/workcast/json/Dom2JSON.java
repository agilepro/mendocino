package org.workcast.json;

import java.util.ArrayList;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.w3c.dom.NodeList;


/**
 * This class contains routines to convert a standard XML DOM
 * into a JSON structure.  Useful for the kind of XML is that which has
 * been used to store DATA.  What I mean by that is that data is always
 * name-value pair, can be stored in an attribute OR can be stored in
 * scalar form (one element with a name and contents in the value).
 * If you have a set of values then you use a set of elements all
 * the same name.
 *
 * ALGORITHM EXPLANATION
 *

 * An element can come in two forms:  scalar value and object
 * The Object has members, and two types of member: scalar or vector
 *
 * A: <elementA>value</elementA>
 *
 * The simple scalar form can ONLY have text within it, nothing else.
 * No attributes, no sub elements.  This then gets translated
 * to the following JSON:
 *
 * {
 *     "elementA": value
 * }
 *
 *
 * B: <elementB attrib="aval"><sub>xxx</sub></elemenetB>
 *
 * In case B, we we have an object, and must structure this as a
 * JSON object.  Each of the attributes becomes a single named
 * string element.  Each sub-element is converted depending upon
 * whether it is simple or complex, and whether there is one
 * or multiple.   In this first case, we have one attribute and
 * one simple sub-element
 *
 * {
 *     "elementB": {
 *         "attrib": "aval",
 *         "sub":  "xxx"
 *     }
 * }
 *
 * Attributes always map to simple values, and are treated the
 * same as simple subelements. The order is assumed to be unimportant.
 *
 * The next case to consider is multiple simple subelements.
 * If multiple tags with the same name appear
 * in the child, then the output need to map to an array value.
 *
 * C:  <elementC><sub>xxx</sub><sub>yyy</sub><sub>zzz</sub></elemenetC>
 *
 * Then the result needs to be:
 *
 * {
 *     "elementC": {
 *         "sub":  ["xxx", "yyy", "zzz"]
 *     }
 * }
 *
 * The next case to consider is a single complex sub element:
 *
 * D: <elementD><sub x="23" y="48"/></elemenetD>
 *
 * Because the sub element is complex, the result must be an
 * object notation:
 *
 * {
 *     "elementD": {
 *         "sub":  {
 *             x: 23,
 *             y: 48
 *         }
 *     }
 * }
 *
 * The final case to consider is multiple complex sub elements
 * and you get an array of json objects:
 *
 * E: <elementE><sub x="1" y="2"/><sub x="3" y="4"/></elemenetE>
 *
 * {
 *     "elementD": {
 *         "sub":  [
 *             {
 *                 x: 23,
 *                 y: 48
 *             },
 *             {
 *                 x: 23,
 *                 y: 48
 *             }
 *         ]
 *     }
 * }
 *
 * LIMITATIONS
 *
 * IF an element is supposed to be an array, but the source document
 * has only a single item, then there is no way for the introspection
 * approach to determine that is it supposed to be an array or not.
 * If there is only one (or zero) element, it can not know that it
 * is supposed to be an array.
 *
 * So Hints is a map from element name to an integer
 *
 *   0 means it is a simple value, just get the string value out
 *   1 means array of simple values, that is, this element defines
 *     a simple value and it should be one of multiple such values
 *   2 means it is an object -- even if no attributes or sub-elements appear
 *     still always treat this as an object
 *   3 means multiple objects -- this should be put in an array and
 *     treated as an object regardless of whether it looks like one
 *
 * In this example:
 *
 * <books><book/></books>
 *
 * The hint of 3 is placed on "book" so that book will be interpreted
 * as an object, and also to say that there can be multiple books potentially.
 * Unfortunately, if the XML has zero book elements, there is no way to
 * know about the hint, and then no empty array can be generated.  To solve
 * this we would need a more complete schema definition of all the things
 * that could be within any given element.
 *
 *
 * STRIPPING AND IGNORING TEXT
 *
 * An important thing to remember is that when an element is determined
 * to be non-simple, then all of the text within it is ignored.  THat is,
 * all the text between the start and end tag that is NOT within a
 * sub element.   For data usage in XML, the text around the sub elements
 * is usually text for indenting the tags for display.  This transformation
 * strips that out.  (There is no way in XML to distinguish content text from
 * text that is used for layout, and there is a weak mechanism to consider
 * all white space collapsable into a single character -- or even no characters.
 * This is a weakness of XML and why you should generally prefer JSON for data.)
 *
 * In the example below, all of the 'x' will be ignored.  IN a real XML this
 * might be white space, or it might be other characters, but in this conversion
 * they will always be ignored.
 *
 * <elementF>xx
 * xxxxx<sub>Hello</sub>xx
 * xxxxx<dub>
 * xxxxxxxx<dubdub>World</dubdub>xxx
 * xxxxx</dub>xxx
 * </elementF>
 *
 * The implication of this is that this is NOT useful for converting HTML
 * to JSON.  In HTML, you can have text, and within that text a word might
 * be marked BOLD or something.  In this conversion, all the text except
 * for the small block marked bold would be ignored.  Another way of thinking
 * about this is that text for data can only exist as leaves at the end of
 * the tree.  Text that is not at a leaf will be ignored.
 */

public class Dom2JSON {

    /**
     * Pass in a DOM Document, and you get a JSON object that represents
     * the entire contents.
     */
    public static JSONObject convertDomToJSON(Document doc) throws Exception {
        Element rootEle = doc.getDocumentElement();
        return convertElementToJSON(rootEle);
    }

    /**
     * Pass in a DOM Document, and you get a JSON object that represents
     * the entire contents.
     */
    public static JSONObject convertDomToJSON(Document doc, Hashtable<String,Integer> hints) throws Exception {
        Element rootEle = doc.getDocumentElement();
        return convertElementToJSON(rootEle, hints);
    }

    /**
     * Pass in an element and make a stand alone JSON structure for this one
     * element.   Please note that this method can not be used if the element
     * has siblings.  It only works for a single, standing alone element like
     * the root element of a document or the root of any tree.
     */
    public static JSONObject convertElementToJSON(Element rootEle) throws Exception {
        return convertElementToJSON(rootEle, new Hashtable<String,Integer>());
    }

    /**
     * Pass in an element and make a stand alone JSON structure for this one
     * element.   Please note that this method can not be used if the element
     * has siblings.  It only works for a single, standing alone element like
     * the root element of a document or the root of any tree.
     */
    public static JSONObject convertElementToJSON(Element rootEle,
            Hashtable<String,Integer> hints) throws Exception {

        JSONObject jo = new JSONObject();
        String tagName = rootEle.getTagName();
        boolean isSimple = elementIsSimple(rootEle, hints.get(tagName));
        if (isSimple) {
            String value = getElementText(rootEle);
            jo.put(tagName, value);
        }
        else {
            JSONObject eleObj = getElementObj(rootEle, hints);
            jo.put(tagName, eleObj);
        }
        return jo;
    }


    /**
     * determines whether this element is simple or not.
     * Simple elements can be converted to a string value.
     * Non-simple elements need to be converted to a JSONObject
     */
    public static boolean elementIsSimple(Element ele, Integer hintValue) {
        if (hintValue!=null) {
            int val = hintValue.intValue();
            return (val==0 || val==1);
        }
        NodeList nl = ele.getChildNodes();
        int last = nl.getLength();
        NamedNodeMap nnm = ele.getAttributes();
        int attributeCount = nnm.getLength();
        int elementCount = 0;
        for (int i=0; i<last; i++) {
            Node node = nl.item(i);
            short type = node.getNodeType();
            if (Node.ELEMENT_NODE==type) {
                elementCount++;
            }
        }
        return (attributeCount==0 && elementCount==0);
    }


    /**
     * Gets the content of the element as an object that includes members
     * for each of the attributes, and for each of the sub elements.
     * Text within the element and outside of the subelements will be
     * thrown away.
     *
     * This does NOT use the element name, it returns everything except
     * for the name.
     */
    public static JSONObject getElementObj(Element ele, Hashtable<String,Integer> hints) throws Exception {
        JSONObject jo = new JSONObject();
        NodeList nl = ele.getChildNodes();
        int last = nl.getLength();
        Hashtable<String,ArrayList<String>> stringMap = new Hashtable<String,ArrayList<String>>();
        Hashtable<String,ArrayList<JSONObject>> objectMap = new Hashtable<String,ArrayList<JSONObject>>();
        for (int i=0; i<last; i++) {
            Node node = nl.item(i);
            short type = node.getNodeType();
            if (Node.ATTRIBUTE_NODE==type) {
                String attName = node.getNodeName();
                String attValue = node.getNodeValue();
                jo.put(attName, attValue);
                System.out.println("Found an attribute: "+attName+" = "+attValue);
            }
            else if (Node.ELEMENT_NODE==type) {
                Element ele2 = (Element)node;
                String tagName = ele2.getTagName();
                boolean isSimple = elementIsSimple(ele2, hints.get(tagName));
                if (isSimple) {
                    ArrayList<String> stringList = stringMap.get(tagName);
                    if (stringList==null) {
                        stringList = new ArrayList<String>();
                        stringMap.put(tagName, stringList);
                    }
                    stringList.add(getElementText(ele2));
                }
                else {
                    ArrayList<JSONObject> objList = objectMap.get(tagName);
                    if (objList==null) {
                        objList = new ArrayList<JSONObject>();
                        objectMap.put(tagName, objList);
                    }
                    objList.add(getElementObj(ele2, hints));
                }
            }
            else if (Node.TEXT_NODE!=type && Node.CDATA_SECTION_NODE!=type) {
                System.out.println("Found unknown element: "+type+"   Name: "+node.getNodeName());
            }
        }
        for (String key : stringMap.keySet()) {
            ArrayList<String> stringList = stringMap.get(key);
            boolean isArray = stringList.size()>1;
            Integer hintVal = hints.get(key);
            if (hintVal!=null) {
                if (hintVal==1 || hintVal==3) {
                    isArray=true;
                }
            }
            if (!isArray) {
                jo.put(key, stringList.get(0));
            }
            else {
                JSONArray ja = new JSONArray();
                for (String val : stringList) {
                    ja.put(val);
                }
                jo.put(key, ja);
            }
        }
        for (String key : objectMap.keySet()) {
            ArrayList<JSONObject> objList = objectMap.get(key);
            boolean isArray = objList.size()>1;
            Integer hintVal = hints.get(key);
            if (hintVal!=null) {
                if (hintVal==1 || hintVal==3) {
                    isArray=true;
                }
            }
            if (!isArray) {
                jo.put(key, objList.get(0));
            }
            else {
                JSONArray ja = new JSONArray();
                for (JSONObject jj : objList) {
                    ja.put(jj);
                }
                jo.put(key, ja);
            }
        }
        NamedNodeMap nnm = ele.getAttributes();
        last = nnm.getLength();
        for (int i=0; i<last; i++) {
            Node node = nnm.item(i);
            if (Node.ATTRIBUTE_NODE==node.getNodeType()) {
                String attName = node.getNodeName();
                String attValue = node.getNodeValue();
                jo.put(attName, attValue);
            }
            else {
                System.out.println("Attribute list contains a non attribute: "+node.getNodeType()+" = "+node.getNodeName());
            }
        }

        return jo;
    }

    /**
     * Returns the string contents of a simple element.
     * All attributes and sub-elements will be ignored.
     * If there are multiple spans of text, all the text
     * is appended together.
     */
    public static String getElementText(Element ele) throws Exception {
        NodeList nl = ele.getChildNodes();
        StringBuffer sb = new StringBuffer();
        int last = nl.getLength();
        for (int i=0; i<last; i++) {
            Node node = nl.item(i);
            if (Node.TEXT_NODE==node.getNodeType()) {
                sb.append(node.getNodeValue());
            }
            else if (Node.CDATA_SECTION_NODE==node.getNodeType()) {
                sb.append(node.getNodeValue());
            }
        }
        return sb.toString();
    }

}
