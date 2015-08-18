/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.workcast.mendocino;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Mel = Mendocino Element
 *
 * The purpose of this class is to be a base class to classes which are designed
 * to be interfaced to DOM trees. If you have a DOM tree, and you want to
 * directly reflect that out to the program as structured data, you create a
 * file that has getters and setters for the various members. This base class
 * containes convenient methods for manipulating the DOM tree, and keeping it in
 * sync with the external representation you are presenting.
 *
 * GETTERS: pull values directly from child dom elements. Useful when a
 * particular member is gotten only a few times any reading of the file. If this
 * is a heavily used member, prefectch that to a cache member, and use that.
 * SETTERS: put value directly into child dom elements. If you have decided to
 * use a cache member for fast access, don't forget to update both the cache and
 * the dom tree.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class Mel {
    private Document fDoc;
    private Element fEle;
    private Schema schema;

    @SuppressWarnings("rawtypes")
    private static Class[] constructParams = new Class[] { Document.class, Element.class };

    /**
     * Standard constructor for a Mel on an existing XML tree. If you have a XML
     * document which you know is a MDS file you can use this constructor to
     * make a single Mel that wraps an XML element. Children elements are not
     * created until asked for.
     */
    public Mel(Document doc, Element ele) {
        if (ele == null) {
            throw new RuntimeException("Program logic error: Mel object can not"
                    + " be constructed on a null element parameter.");
        }
        if (doc == null) {
            throw new RuntimeException("Program logic error: Mel object can not"
                    + " be constructed on a null document parameter.");
        }
        fDoc = doc;
        fEle = ele;
    }

    /**
     * Constructs an instance of an extended class. This is used by all the
     * methods that take a class name and return elements of specific
     * subclasses.
     */
    public static <T extends Mel> T construct(Class<T> childClass, Document doc, Element ele)
            throws Exception {
        try {
            Constructor<T> con = childClass.getConstructor(constructParams);
            Object[] inits = new Object[2];
            inits[0] = doc;
            inits[1] = ele;
            return con.newInstance(inits);
        }
        catch (Exception e) {
            throw new Exception("Unable to create an object of class " + childClass.getName(), e);
        }
    }

    /**
     * Constructs an instance of an extended class. This is used by all the
     * methods that take a class name and return elements of specific
     * subclasses.
     */
    public <T extends Mel> T convertClass(Class<T> desiredClass) throws Exception {
        try {
            Constructor<T> con = desiredClass.getConstructor(constructParams);
            Object[] inits = new Object[2];
            inits[0] = fDoc;
            inits[1] = fEle;
            return con.newInstance(inits);
        }
        catch (Exception e) {
            throw new Exception(
                    "Unable to convert to an object of class " + desiredClass.getName(), e);
        }
    }

    /**
     * This internal method is used to create a child of this object, of the
     * specified class, once the DOM element is found. The schema reference is
     * passed on the child.
     */
    private <T extends Mel> T constructRelative(Element ele, Class<T> childClass) throws Exception {
        T me = construct(childClass, fDoc, ele);
        me.setSchema(schema);
        return me;
    }

    /**
     * Given a File object (that points to a real existing MDS file) This global
     * static method will read the file and return a Mel for the base of the
     * tree of the file.
     */
    public static <T extends Mel> T readFile(File inFile, Class<T> rootClass) throws Exception {
        return readInputStream(new FileInputStream(inFile), rootClass);
    }

    /**
     * Given a byte stream (that points to a real existing MDS file) this global
     * static method will read the stream and return a Mel for the base of the
     * tree of the file. Encoding is always UTF-8.
     */
    public static <T extends Mel> T readInputStream(InputStream is, Class<T> rootClass)
            throws Exception {
        Document doc = convertInputStreamToDocument(is);
        Element docElement = doc.getDocumentElement();
        return construct(rootClass, doc, docElement);
    }

    /**
     * Use this to create a brand new base of the tree of the file.
     */
    public static <T extends Mel> T createEmpty(String rootElement, Class<T> rootClass)
            throws Exception {
        Document doc = createDocument(rootElement);
        Element docElement = doc.getDocumentElement();
        return construct(rootClass, doc, docElement);
    }

    /**
     * writeToOutputStream streams the entire XML output that reflects the
     * entire tree to an output stream. Encoding is always UTF-8.
     */
    public void writeToOutputStream(OutputStream out) throws Exception {
        fDoc.setXmlStandalone(true);
        DOMSource docSource = new DOMSource(fDoc);
        Transformer transformer = getXmlTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(docSource, new StreamResult(out));
    }

    /**
     * <p>
     * As you might have guessed from the name, the writeToFile method will
     * write the entire tree out to a file. You must pass in a valid full path
     * to a file name.
     * </p>
     * <p>
     * To assure atomic writing, the file is first output to a temporary file
     * that is the same name, but with a "tmp-###" appended to the end of the
     * name. Only when the entire tree is successfully output to the file, then
     * the file is renamed to the desired name.
     * </p>
     * <p>
     * If there was a file already there with that name, then that file will be
     * deleted, just before the temporary file is renamed to the desired name.
     * </p>
     * <p>
     * If your program crashes for any reason before the entire file is output,
     * the result will be that the previous version of the file will still be
     * there, and there will be a partially written file with a "tmp-###"
     * appended to the end. Since these are most likely fragments of the entire
     * file, they are not valid XML files, and can not be parsed. They should be
     * discarded, and the cause of the crash researched. There are no normal
     * situation that these files should be seen left in the file system.
     * </p>
     * <p>
     * Here is a specific example. If you want to write to a file with the path
     * "c:/data/MyConfig.cfg" the folder "c:/data" must exist. The output
     * routine will choose a random number, for example '137' and will write the
     * contents of the tree to "c:/data/MyConfig.cfg-tmp-137". Once the file is
     * completely output to disk, the old file "MyConfig.cfg" is deleted, and
     * the temp file is renamed to "MyConfig.cfg".
     * </p>
     * <p>
     * With this approach you are most likely to always have a valid file with
     * the desired name. There is an extremely small possibility that the
     * program might crash between the deletion of the old file, and the
     * renaming of the temp file, leaving you with no file at all with the
     * desired name. There is a very small window of time between these two
     * operations where another program or another thread might find the file
     * missing from the file system. This seems to be a limitation that we have
     * to live with until Java offer an atomic rename-and-delete-previous-file
     * operation.
     * </p>
     */
    public void writeToFile(File outFile) throws Exception {
        // write to a temp file with a temp name
        File tempFile = null;
        Random r = new Random();
        do {
            tempFile = new File(outFile.toString() + "-tmp-" + r.nextInt(1000));
        }
        while (!tempFile.createNewFile());
        OutputStream fos = new FileOutputStream(tempFile);
        writeToOutputStream(fos);
        fos.flush();
        fos.close();

        // got here without problem, ok, delete the backup file, and rename
        // output file
        // looking for a way to rename a file and delete the current file with
        // that name
        // in a single, atomic, opration.
        if (outFile.exists()) {
            outFile.delete();
        }
        tempFile.renameTo(outFile);
    }

    /********************* SELF ***********************/

    /**
     * The name of the data element that this object represents.
     */
    public String getName() {
        return getElementName(fEle);
    }

    /**
     * This returns the namespace prefix, if there is one. Returns an empty
     * string if not.
     */
    public String getPrefix() {
        String name = fEle.getNodeName();
        if (name == null || name.length() == 0) {
            name = fEle.getLocalName();
        }
        int colonPos = name.lastIndexOf(":");
        if (colonPos >= 0) {
            return name.substring(0, colonPos);
        }
        return "";
    }

    /**
     * Generally a Mel is a container, meaning it can have children which are
     * either more Mel or data values. When retrieving a data value (either
     * scalar or vector), the methods return the value directly. But some
     * methods return all children as a single collection, including both data
     * values and containers. This is convenient for traversing the tree and
     * generating output. In those situations you need to call this method
     * "isContainer" to determine if this particular node contains only text, or
     * whether it can have structured children.
     *
     * Note: there is one situation where it is impossible to know whether it is
     * a container or not, and that is when the tag is completely empty and it
     * has no attributes. In this case it might be a data value without any
     * data, or it might be a container without any contents. Either way it is
     * empty. This function returns false in this ambiguous case, thus an
     * element that is normally a container will return false (not a container)
     * if it is completely empty.
     */
    public boolean isContainer() {
        // data elements can not have attributes, so if there
        // are any attributes, then it is clearly a container.
        NamedNodeMap nnm = fEle.getAttributes();
        if (nnm != null) {
            if (nnm.getLength() > 0) {
                return true;
            }
        }

        // No attributes, so we need to check the child elements
        // if there are any. It can either have child elements,
        // or it can have non-white characters in the text.
        // As soon as we detect one or the other we will know.
        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            Node n = childNdList.item(i);
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                return true;
            }
            if (n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                String val = n.getNodeValue();
                for (int j = 0; j < val.length(); j++) {
                    char ch = val.charAt(j);
                    if (ch > ' ') {
                        return false;
                    }
                }
            }
            else if (n.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                return false;
            }
        }

        // this is the ambiguous case, no attributes, no children, and no
        // non-white chars
        // so we treat it like a data case
        return false;
    }

    /**
     * If you call "getAllChildren" you will get Mels that represent containers,
     * as well as ones that represent data. If the "isContainer" method returns
     * false, then it represents a data element, and this method will return the
     * string value for that data element.
     */
    public String getDataValue() {
        return textValueOf(fEle);
    }

    /**
     * If the MDS file has a corresponding schema, this method can be used to
     * set the schema on the MDS file, and consequently allow interactions to be
     * validated by the schema.
     */
    public void setSchema(Schema desiredSchema) {
        schema = desiredSchema;
    }

    /**
     * Pass in an empty vector, and it will be filled with any schema violations
     * that are discovered in this element and all children.
     */
    public void validate(Vector<Exception> results) throws Exception {
        // first check to see if the three is constructed properly in order to
        // validate.
        // there must be a schema.
        String myName = getName();
        if (schema == null) {
            results.add(new Exception(
                    "Unable to validate because there is no schema defined for element (" + myName
                            + ")"));
            return;
        }

        // Now check if the schema includes an entry for this object
        SchemaDef sd = schema.lookUpDefinition(myName);
        if (sd == null) {
            results.add(new Exception(
                    "Unable to validate schema, unable to find a schema definition for element ("
                            + myName + ")"));
            return;
        }

        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                // we are not really interested in any of the text nodes at this
                // point
                // no validation necessary at this time
                continue;
            }

            Element ne = (Element) n;
            String childName = getElementName(ne);

            if (childName == null) {
                results.add(new Exception(
                        "strange, there exists an element node in the DOM tree that does not have a name...this should never happen.  Some sort of programming logic mistake with the DOM API"));
                return;
            }

            // only element tags. See what the schema says
            SchemaDef sd2 = schema.lookUpDefinition(childName);

            if (sd2 == null) {
                results.add(new Exception("Element '" + myName + "' has a child element '"
                        + childName + "' that is not in the schema."));
                continue;
            }

            if (sd2.isContainer()) {
                Mel child = constructRelative(ne, Mel.class);
                child.validate(results);
            }
        }
    }

    /**
     * Pass in an empty ValidationResults object, and it will be filled with any
     * schema violations that are discovered in this element and all children.
     */
    public void validate(ValidationResults results) throws Exception {
        // first check to see if the three is constructed properly in order to
        // validate.
        // there must be a schema.
        String myName = getName();
        if (schema == null) {
            results.addResult("Unable to validate because there is no schema defined for element ("
                    + myName + ")");
            return;
        }

        // Now check if the schema includes an entry for this object
        SchemaDef sd = schema.lookUpDefinition(myName);
        if (sd == null) {
            results.addResult("Unable to validate schema, unable to find a schema definition for element ("
                    + myName + ")");
            return;
        }

        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                // we are not really interested in any of the text nodes at this
                // point
                // no validation necessary at this time
                continue;
            }

            Element ne = (Element) n;
            String childName = getElementName(ne);

            if (childName == null) {
                results.addResult("strange, there exists an element node in the DOM tree that does not have a name...this should never happen.  Some sort of programming logic mistake with the DOM API");
                return;
            }

            // only element tags. See what the schema says
            SchemaDef sd2 = schema.lookUpDefinition(childName);

            if (sd2 == null) {
                results.addResult("Element '" + myName + "' has a child element '" + childName
                        + "' that is not in the schema.");
                continue;
            }

            if (sd2.isContainer()) {
                Mel child = constructRelative(ne, Mel.class);
                child.validate(results);
            }
        }
    }

    /**
     * Returns a vector of strings, each string being the name of a
     */
    public Vector<String> childElementNames() throws Exception {
        if (schema == null) {
            throw new Exception(
                    "Schema is not defined on this tree.  Define the schema if you wish to get a list of the child elements");
        }
        String myName = getName();
        Vector<Mel> definedElements = schema.getChildren("container");
        for (Mel me : definedElements) {
            if (myName.equals(me.getAttribute("name"))) {
                // found my entry
                Vector<String> childNames = me.getVector("contains");
                return childNames;
            }
        }
        throw new Exception("Schema is defined, but it does not have an element for the element '"
                + myName + "'.");
    }

    /********************* ATTRIBUTE ***********************/

    /**
     * If a containing tag has an attribute, getAttribute will return the string
     * value of that attribute.
     *
     * attrName: the name of the attribute holding a data value returns: a
     * string values
     */
    public String getAttribute(String attrName) {
        if (attrName == null) {
            throw new RuntimeException("Program logic error: a null attribute name"
                    + " was passed to getAttribute.");
        }
        return fEle.getAttribute(attrName);
    }

    /**
     * If a containing tag has an attribute, and that attribute contains an
     * integer string value, then getAttribute will return the long value of
     * that attribute.
     *
     * A "forgiving" conversion is used which does not complain if non-numeral
     * characters are included, it simply uses any numerals it finds to return
     * the result as if it was only numerals. If there are no numerals at all,
     * or if the attribute does not have a value, it simply returns zero. The
     * reason for using this is because MDF files are designed for interchange
     * between systems. If the value contains an integer then this conversion
     * will proceed correctly, but if the file generated by some other program
     * has the wrong value in the attribute, the problem is essentially ignored.
     * This approach allows commas (or periods) to group thousands and they will
     * be ignored. It successfully ignores all currency symbols or other unit
     * indicators that might be there. Most importantly, it does not throw
     * exceptions ever.
     *
     * attrName: the name of the attribute holding a data value returns: a long
     * value
     */
    public long getAttributeLong(String attrName) {
        return safeConvertLong(getAttribute(attrName));
    }

    /**
     * Sets an attribute and value on a tag. This will replace any value that
     * already exists there.
     *
     * attrName: the name of the attribute holding a data value value: a string
     * value
     */
    public void setAttribute(String attrName, String value) {
        if (attrName == null) {
            throw new RuntimeException("Program logic error: a null attribute name"
                    + " was passed to setAttribute.");
        }
        if (value == null) {
            fEle.removeAttribute(attrName);
        }
        else {
            fEle.setAttribute(attrName, value);
        }
    }

    /**
     * Sets an attribute and an integer (long) value on a tag. This will replace
     * any value that already exists there.
     *
     * attrName: the name of the attribute holding a data value value: a long
     * value
     */
    public void setAttributeLong(String attrName, long value) {
        setAttribute(attrName, Long.toString(value));
    }

    /**
     * Retrieves an attribute value from a tag, and compares it to a given
     * value, returning true if they are equal. This is convenient when looking
     * through a list of data elements to find one with a particular attribute
     * value. The most common use for this is an "id" attribute. This is
     * convenient for finding a particular data element with a particular id
     * value.
     *
     * attrName: the name of the attribute holding a data value testValue: a
     * string value to test against
     */
    public boolean attributeEquals(String attrName, String testValue) throws Exception {
        if (testValue == null) {
            throw new RuntimeException("Program logic error: a null test value"
                    + " was passed to attributeEquals.");
        }
        String val = getAttribute(attrName);
        if (val == null) {
            return false;
        }
        return testValue.equals(val);
    }

    public Vector<String> getAllAttributeNames() {
        Vector<String> result = new Vector<String>();
        NamedNodeMap nnm = fEle.getAttributes();
        if (nnm == null) {
            return result;
        }
        if (nnm.getLength() == 0) {
            return result;
        }
        int last = nnm.getLength();
        for (int i = 0; i < last; i++) {
            Node n = nnm.item(i);
            String attrName = n.getNodeName();
            result.add(attrName);
        }
        return result;
    }

    /*************** SCALAR DATA VALUES *************************/

    /**
     * If a containing tag has a child value tag getScalar will find that tag,
     * and return the string value.
     *
     * memberName: the name of the tag holding a data value returns: a string
     * value
     */
    public String getScalar(String memberName) {
        if (memberName == null) {
            throw new RuntimeException("Program logic error: a null member name"
                    + " was passed to getScalar.");
        }
        return getChildText(fEle, memberName);
    }

    /**
     * setScalar will create a child value tag of a specified name and value for
     * the containing element. setScalar will also remove the child value tag if
     * you pass a null for the value parameter.
     *
     * memberName: the name of the tag holding a data value value: a string
     * value to put in the tag.
     */
    public void setScalar(String memberName, String value) {
        if (memberName == null) {
            throw new RuntimeException("Program logic error: a null member name"
                    + " was passed to setScalar.");
        }
        setChildValue(fDoc, fEle, memberName, value);
    }

    /*************** VECTOR DATA VALUES *************************/

    /**
     * If a containing tag has multiple child value tags you can access all the
     * values at once, retrieving a vector of string values.
     *
     * Call getVector with the name of the tag(s) that hold the data. Each such
     * tag will be accessed, and the value included in the vector in the same
     * order.
     *
     * memberName: the name of the tag holding a data value returns: a Vector of
     * string values
     */
    public Vector<String> getVector(String memberName) {
        if (memberName == null) {
            throw new RuntimeException("Program logic error: a null member name"
                    + " was passed to getVector.");
        }
        Vector<String> list = new Vector<String>();
        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            if (memberName.equals(getElementName((Element) n))) {
                list.add(textValueOf(n));
            }
        }
        return list;
    }

    /**
     * If a containing tag has multiple child value tags you can access and set
     * all the values at once using a vector of string values.
     *
     * Construct a vector will all of the string value in the correct order.
     * Call setVector, and a child tag and value will be created in the DOM tree
     * for each value in the vector. Any previous values will be removed.
     *
     * memberName: the name of the tag holding a data value values: a Vector of
     * string values
     */
    public void setVector(String memberName, Vector<String> values) {
        if (memberName == null) {
            throw new RuntimeException("Program logic error: a null member name"
                    + " was passed to setVector.");
        }
        removeAllNamedChild(fEle, memberName);
        for (String val : values) {
            createChildElement(fDoc, fEle, memberName, val);
        }
    }

    /**
     * If a containing tag has multiple child value tags you append a value to
     * that set of values.
     *
     * memberName: the name of the tag holding a data value value: a string
     * value to be added
     */
    public void addVectorValue(String memberName, String value) {
        if (memberName == null) {
            throw new RuntimeException("Program logic error: a null member name"
                    + " was passed to addVectorValue.");
        }
        createChildElement(fDoc, fEle, memberName, value);
    }

    /*************** CHILDREN *************************/

    public <T extends Mel> T addChild(String elementName, Class<T> childClass) throws Exception {
        Element ele = createChildElement(fDoc, fEle, elementName);
        return construct(childClass, fDoc, ele);
    }

    public void removeChild(Mel mele) throws Exception {
        Element ele = mele.getElement();
        fEle.removeChild(ele);
    }

    public void removeAllNamedChild(String elementName) throws Exception {
        removeAllNamedChild(fEle, elementName);
    }

    public Mel getChild(String elementName, int index) throws Exception {
        return getChild(elementName, index, Mel.class);
    }

    /**
     * Returns a child object with a specified name. If you pass an index of 0,
     * it will return the This is designed to be used when you know that you
     * have only a single instance of a child with a name, but it can be used in
     * situations where you want only the first occurrance.
     */
    public <T extends Mel> T getChild(String elementName, int index, Class<T> childClass)
            throws Exception {
        if (index < 0) {
            throw new RuntimeException(
                    "The index parameter Mel.getChild must be a positive integer value ... a negative was passed and that simply does not make any sense.");
        }
        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            Element ne = (Element) n;
            if (elementName.equals(getElementName(ne))) {
                index--;
                if (index < 0) {
                    return construct(childClass, fDoc, (Element) n);
                }
            }
        }
        // did not find that index of child, so give up
        return null;
    }

    /**
     * Returns a child object with a specified name. If you pass an index of 0,
     * it will return the This is designed to be used when you know that you
     * have only a single instance of a child with a name, but it can be used in
     * situations where you want only the first occurrance.
     */
    public <T extends Mel> T findChild(String elementName, String attributeName, String keyValue,
            Class<T> childClass) throws Exception {
        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            Element ne = (Element) n;
            if (elementName.equals(getElementName(ne))) {
                String key = ne.getAttribute(attributeName);
                if (key != null && keyValue.equals(key)) {
                    return constructRelative(ne, childClass);
                }
            }
        }
        // did not find child with that attribute value
        return null;
    }

    public Vector<Mel> getChildren(String elementName) throws Exception {
        return getChildren(elementName, Mel.class);
    }

    public <T extends Mel> Vector<T> getChildren(String elementName, Class<T> childClass)
            throws Exception {
        Vector<T> list = new Vector<T>();
        Constructor<T> con = childClass.getConstructor(constructParams);
        Object[] inits = new Object[2];
        inits[0] = fDoc;

        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            Element ne = (Element) n;
            if (elementName.equals(getElementName(ne))) {
                inits[1] = n;
                list.add(con.newInstance(inits));
            }
        }
        return list;
    }

    /**
     * getAllChildren will return a vector of Mels, one for each existing child
     * element, as well as one for each existing data value. This is a rare case
     * that data is returned not as a string, but as a Mel. You must use
     * "isContainer" method to determine whether this element is a data value or
     * not.
     */
    public Vector<Mel> getAllChildren() throws Exception {
        Vector<Mel> list = new Vector<Mel>();
        Constructor<Mel> con = Mel.class.getConstructor(constructParams);
        Object[] inits = new Object[2];
        inits[0] = fDoc;

        NodeList childNdList = fEle.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            inits[1] = n;
            list.add(con.newInstance(inits));
        }
        return list;
    }

    /**
     * Avoid using this, but provided for interfacing to external libraries
     */
    public Element getElement() {
        return fEle;
    }

    public Document getDocument() {
        return fDoc;
    }

    /**
     * designed primarily for returning date long values works only for positive
     * integer (long) values considers all numeral, ignores all letter and
     * punctuation never throws an exception if you give this something that is
     * not a number, you get surprising result. Zero if no numerals at all.
     */
    public static long safeConvertLong(String val) {
        if (val == null) {
            return 0;
        }
        long res = 0;
        int last = val.length();
        for (int i = 0; i < last; i++) {
            char ch = val.charAt(i);
            if (ch >= '0' && ch <= '9') {
                res = res * 10 + ch - '0';
            }
        }
        return res;
    }

    public void reformatXML() throws Exception {
        Element root = fDoc.getDocumentElement();
        indentChildren(root, "\n");
    }

    private void indentChildren(Element parent, String indent) throws Exception {
        NodeList childNdList = parent.getChildNodes();
        String newIndent = indent + "  ";

        // first scan to see if there are child Elements, or just text
        boolean hasChildElements = false;
        for (int i = 0; i < childNdList.getLength(); i++) {
            Node n = childNdList.item(i);
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                hasChildElements = true;
                break;
            }
        }

        // if there are no elements, then don't modify anything ... it is a data
        // value
        if (!hasChildElements) {
            return;
        }

        // Since there are some elements, then we know this should be in this
        // pattern:
        //
        // indentation text node
        // element
        // indentation text node
        // element
        // smaller indentation text node
        //
        // All other text nodes can be deleted. Each indentation node is a
        // CR then n*2 spaces. The smaller indent is (n-1)*2 spaces.
        // To accomplish this, all Elements are removed from the parent and
        // placed
        // in a vector for temporary holding, and all the existing text nodes
        // are destroyed.
        // Once the parent is empty, the correct indenting nodes are added
        // between
        // the Element nodes.

        Vector<Element> elementSet = new Vector<Element>();
        for (int i = 0; i < childNdList.getLength(); i++) {
            Node n = childNdList.item(i);
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                elementSet.add((Element) n);
            }
        }
        {

            Node nx = parent.getFirstChild();
            while (nx != null) {
                parent.removeChild(nx);
                nx = parent.getFirstChild();
            }
        }

        if (parent.hasChildNodes()) {
            throw new Exception("just cleaned out child nodes, but there seems to still be one.");
        }
        childNdList = parent.getChildNodes();
        if (childNdList.getLength() > 0) {
            throw new Exception(
                    "just cleaned out child nodes, but there seems to still be one in childlist.");
        }

        Collections.sort(elementSet, new DOMElementComparator());

        Enumeration<Element> e1 = elementSet.elements();
        while (e1.hasMoreElements()) {
            parent.appendChild(fDoc.createTextNode(newIndent));
            Element ele = e1.nextElement();
            parent.appendChild(ele);
        }

        parent.appendChild(fDoc.createTextNode(indent));

        // recursively indent the children elements now
        e1 = elementSet.elements();
        while (e1.hasMoreElements()) {
            indentChildren(e1.nextElement(), newIndent);
        }

    }

    public void eliminateCData() throws Exception {
        Element root = fDoc.getDocumentElement();
        eliminateCDataRecurse(root);
    }

    private void eliminateCDataRecurse(Element parent) throws Exception {
        NodeList childNdList = parent.getChildNodes();

        for (int i = 0; i < childNdList.getLength(); i++) {
            Node n = childNdList.item(i);
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                eliminateCDataRecurse((Element) n);
            }
            else if (n.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                // might need to pull thsi out to a vector, and then replace
                // later
                // depending on how well behaved the node list is.
                CDATASection cdc = (CDATASection) n;
                String val = cdc.getNodeValue();
                Node newText = fDoc.createTextNode(val);
                parent.replaceChild(newText, n);
            }
        }
    }

    /************** INTERNAL HELPERS *********************/

    /**
     * This method creates a new Document Object. Pass in the name of the root
     * node, since you ALWAYS need a root node, and attaching this to the
     * document is not like other children. Retrieve the root element with the
     * standard getDocumentElement.
     *
     * @return
     * @throws Exception
     */
    protected static Document createDocument(String rootNodeName) throws Exception {
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        dfactory.setValidating(false);
        DocumentBuilder bldr = dfactory.newDocumentBuilder();
        Document doc = bldr.newDocument();
        Element rootEle = doc.createElement(rootNodeName);
        doc.appendChild(rootEle);
        return doc;
    }

    protected static Document convertInputStreamToDocument(InputStream is) throws Exception {
        boolean validate = false;
        boolean isNamespaceAware = false;
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(isNamespaceAware);
        dfactory.setValidating(validate);
        dfactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder bldr = dfactory.newDocumentBuilder();
        bldr.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
                // ignore warnings
            }

            public void error(SAXParseException exception) throws SAXException {
                // ignore parse validation errors
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        Document doc = bldr.parse(new InputSource(is));
        return doc;
    }

    private static String getChildText(Element parent, String name) {
        Element child = getChildElement(parent, name);
        if (child == null) {
            return "";
        }
        return textValueOf(child);
    }

    /**
     * passing a null removed all evidence of a tag by that name removed all
     * duplicate values, and leaves with one child of the name and value
     * specified.
     */
    private static void setChildValue(Document doc, Element parent, String childName,
            String newValue) {
        removeAllNamedChild(parent, childName);
        createChildElement(doc, parent, childName, newValue);
    }

    private static Element getChildElement(Element parent, String name) {
        NodeList childNdList = parent.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            Element ne = (Element) n;
            if (name.equals(getElementName(ne))) {
                return ne;
            }
        }
        return null;
    }

    private static Element createChildElement(Document doc, Element parent, String name) {
        Element newElem = doc.createElement(name);
        parent.appendChild(newElem);
        return newElem;
    }

    private static Element createChildElement(Document doc, Element parent, String name,
            String textValue) {
        // if a null is passed in, then do not create the child element
        // at all. Then when reading, if the element does not exist,
        // the value will be null. This is standard behaviod for
        // optional element.
        if (textValue == null) {
            return null;
        }
        Element newElem = doc.createElement(name);
        newElem.appendChild(doc.createTextNode(textValue));
        parent.appendChild(newElem);
        return newElem;
    }

    private static void removeAllNamedChild(Element parent, String name) {
        NodeList childNdList = parent.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            org.w3c.dom.Node n = childNdList.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                continue;
            }
            Element ne = (Element) n;
            if (name.equals(getElementName(ne))) {
                parent.removeChild(n);
            }
        }
    }

    /**
     * Returns the text of all the chidren of a node as a single string
     *
     * @param node
     *            is the parent of the text
     */
    private static String textValueOf(Node node) {
        // unfold the loop. 99.9% of the time, the XML will have a
        // single text node. Memory is much more efficiently handled
        // if the string from that text node is used directly, instead
        // of being copied into the string buffer. Unfold the loop,
        // and if there is a single child node, simply return that
        // value.
        Node child = skipToNextTextNode(node.getFirstChild());
        if (child == null) {
            return "";
        }
        Node nextChild = skipToNextTextNode(child.getNextSibling());
        if (nextChild == null) {
            return child.getNodeValue();
        }

        // we have more than one, so make a string buffer to
        // concatenate them together.
        StringBuffer text = new StringBuffer();
        text.append(child.getNodeValue());
        child = nextChild;
        while (child != null) {
            text.append(child.getNodeValue());
            child = skipToNextTextNode(child.getNextSibling());
        }
        return text.toString();
    }

    // PRIVATE:
    // if text node passed in, then that is returned.
    // if not, skips nodes that are not text nodes.
    // returns null when gets to the last sibling.
    private static Node skipToNextTextNode(Node child) {
        if (child == null) {
            return null;
        }
        while (child.getNodeType() != Node.CDATA_SECTION_NODE
                && child.getNodeType() != Node.TEXT_NODE) {
            child = child.getNextSibling();
            if (child == null) {
                return null;
            }
        }
        return child;
    }

    private static Transformer getXmlTransformer() throws Exception {
        /*
         * CDATA_SECTION_ELEMENTS | cdata-section-elements = expanded names.
         * DOCTYPE_PUBLIC | doctype-public = string. DOCTYPE_SYSTEM |
         * doctype-system = string. ENCODING | encoding = string. INDENT |
         * indent = "yes" | "no". MEDIA_TYPE | media-type = string. METHOD |
         * method = "xml" | "html" | "text" | expanded name.
         * OMIT_XML_DECLARATION | omit-xml-declaration = "yes" | "no".
         * STANDALONE | standalone = "yes" | "no". VERSION | version = nmtoken.
         */

        initTransformer();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        return transformer;
    }

    private static TransformerFactory transformerFactory = null;
    private static final Long mutex2 = new Long(2);

    private static final void initTransformer() throws TransformerFactoryConfigurationError,
            TransformerConfigurationException {
        if (transformerFactory == null) {
            synchronized (mutex2) {
                transformerFactory = TransformerFactory.newInstance();
            }
        }
    }

    public String getRawDOM() {
        StringBuffer sb = new StringBuffer();
        getRawChildren(fEle, sb, "");
        return sb.toString();
    }

    public static void getRawChildren(Element parent, StringBuffer sb, String place) {
        NodeList childNdList = parent.getChildNodes();
        for (int i = 0; i < childNdList.getLength(); i++) {
            String thisPlace = place + i + ".";
            sb.append(thisPlace);
            Node n = childNdList.item(i);
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                sb.append("ELE:");
                writeShortLiteralValue(sb, n.getNodeName());
            }
            else if (n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                sb.append("TXT:");
                writeShortLiteralValue(sb, n.getNodeName());
            }
            else {
                sb.append("ELE:");
            }
            sb.append("\n");
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                getRawChildren((Element) n, sb, thisPlace);
            }
        }
    }

    public static void writeShortLiteralValue(StringBuffer sb, String value) {
        for (int i = 0; i < value.length() && i < 20; i++) {
            char ch = value.charAt(i);
            if (ch == '"') {
                sb.append("\\\"");
            }
            else if (ch == '\\') {
                sb.append("\\\\");
            }
            else if (ch == '\n') {
                sb.append("\\n");
            }
            else if (ch == (char) 13) {
                // do output anything ... ignore these
            }
            else if (ch < 32 || ch > 128) {
                sb.append("\\u");
                addHex(sb, (ch / 16 / 16 / 16) % 16);
                addHex(sb, (ch / 16 / 16) % 16);
                addHex(sb, (ch / 16) % 16);
                addHex(sb, ch % 16);
            }
            else {
                sb.append(ch);
            }
        }
    }

    private static void addHex(StringBuffer sb, int val) {
        if (val >= 0 && val < 10) {
            sb.append((char) (val + '0'));
        }
        else if (val >= 0 && val < 16) {
            sb.append((char) (val + 'A' - 10));
        }
        else {
            sb.append('?');
        }
    }

    private static String getElementName(Element e) {
        String name = e.getNodeName();
        if (name == null || name.length() == 0) {
            name = e.getLocalName();
        }
        int colonPos = name.lastIndexOf(":");
        if (colonPos >= 0) {
            name = name.substring(colonPos + 1);
        }
        return name;
    }

    /**
     * use DOMElementComparator to sort a vector of elements into alphabetical
     * order according to their name.
     */
    static class DOMElementComparator implements Comparator<Element> {
        public DOMElementComparator() {
        }

        public int compare(Element o1, Element o2) {
            String name1 = o1.getNodeName();
            String name2 = o2.getNodeName();
            if (name1 == null || name2 == null) {
                return 0;
            }
            return name1.compareToIgnoreCase(name2);
        }
    }

}
