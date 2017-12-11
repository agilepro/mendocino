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

package com.purplehillsbooks.xml;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.CDATASection;

/**
 * This class contains code to attempt to generate a schema using an example
 * file. It does this simplistically, by walking the sample file, and adding
 * schema elements as children of a node are found. It should be obvious, of
 * course, it can only add schema elements for the actual elements that appear
 * in the file, so if the example file is not complete enough the schema will
 * not be complete. It makes a guess at plurality as well, assuming that only a
 * single elements is allowed until it sees a particular element with multiple
 * children, then it will mark it as plural.
 *
 * Like all MDF files, the tag name is assumed unique, and assumed to be the
 * same across the file, regardless of conttext.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class SchemaGen {
	public Vector<String> ambiguousCases = new Vector<String>();

	private SchemaGen() {

	}

	public static Schema generateFor(Mel me) throws Exception {
		SchemaGen sg = new SchemaGen();
		Schema schema = Mel.createEmpty("schema", Schema.class);

		String rootName = me.getName();
		schema.addContainer(rootName);
		schema.addRoot(rootName);

		sg.generateForChildren(schema, me.getElement());
		sg.finishAmbiguousCases(schema);

		return schema;
	}

	public void generateForChildren(Schema schema, Element parent) throws Exception {
		// dive down to the children first
		NodeList childNdList = parent.getChildNodes();
		Vector<Element> children = new Vector<Element>();

		// first scan to see if there are child Elements, or just text
		boolean hasChildElements = false;
		boolean hasNonWhiteChars = false;
		boolean hasAttributes = false;
		for (int i = 0; i < childNdList.getLength(); i++) {
			Node n = childNdList.item(i);
			if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				hasChildElements = true;
				children.add((Element) n);
				generateForChildren(schema, (Element) n);
			}
			else if (n.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
				String val = n.getNodeValue();
				for (int j = 0; j < val.length(); j++) {
					char ch = val.charAt(j);
					if (ch > ' ') {
						hasNonWhiteChars = true;
						break;
					}
				}
			}
			else if (n.getNodeType() == org.w3c.dom.Node.CDATA_SECTION_NODE) {
				CDATASection cdc = (CDATASection) n;
				String val = cdc.getNodeValue();
				for (int j = 0; j < val.length(); j++) {
					char ch = val.charAt(j);
					if (ch > ' ') {
						hasNonWhiteChars = true;
						break;
					}
				}
			}

		}

		NamedNodeMap nnm = parent.getAttributes();
		if (nnm != null) {
			int len = nnm.getLength();
			hasAttributes = (len > 0);
		}

		String myName = parent.getNodeName();
		if (hasChildElements && hasNonWhiteChars) {
			// the problem here is that we have see child element and we have
			// also seen a non-white character. If we classify this as a
			// container then those non-white characters will be lost in a
			// reformat, but it should be a container.
			//
			// For now, give up by throwing an exception. We should never hit
			// this situation on a properly constructed MDS file, so this
			// should not be a problem very often.
			throw new Exception(
					"Schema generator has see a tag '"
							+ myName
							+ "' which has both text and child elements, and the generator can not decide whether to make this a container or a data element.  Correct the data file in order to generate the schema.");
		}
		SchemaDef sd = schema.lookUpDefinition(myName);
		if (hasNonWhiteChars) {
			// pretty clear this is a text data element
			if (sd == null) {
				// first time ... go ahead and create it
				sd = schema.addData(myName);
			}
			else if (sd.isContainer()) {
				// uh oh, we have a problem. someplace else the code
				// has assumed that this is text because of non-white characters
				// directly within this. But here it seems to be a container
				// For now, give up by throwing an exception so that we do not
				// create an incompatible schema
				throw new Exception(
						"Schema generator has see a tag '"
								+ myName
								+ "' which was defined earlier as a container node but later found to have non-white text directly within it.  The generator can not decide whether to make this a container or a data element.  Make sure that the tag is used consistently in the sample document in order to generate the schema.");
			}
		}
		else if (hasChildElements || hasAttributes) {
			// now we know it is a container
			if (sd == null) {
				// first time ... go ahead and create it
				sd = schema.addContainer(myName);
			}
			else if (!sd.isContainer()) {
				// uh oh, we have a problem. someplace else the code
				// has assumed that this is text because of non-white characters
				// directly within this. But here it seems to be a container
				// For now, give up by throwing an exception so that we do not
				// create an incompatible schema
				throw new Exception(
						"Schema generator has see a tag '"
								+ myName
								+ "' which was defined earlier as a data node but later found to have child elements.  The generator can not decide whether to make this a container or a data element.  Make sure that the tag is used consistently in the sample document in order to generate the schema.");
			}

			if (nnm != null) {
				int len = nnm.getLength();
				for (int i = 0; i < len; i++) {
					hasAttributes = true;
					Node n = nnm.item(i);
					String attrName = n.getNodeName();
					sd.declareAttribute(attrName);
				}
			}

			Hashtable<String, String> alreadySeen = new Hashtable<String, String>();
			Enumeration<Element> e = children.elements();
			while (e.hasMoreElements()) {
				Element child = e.nextElement();
				String childName = child.getNodeName();
				boolean haveAlreadySeen = (alreadySeen.get(childName) != null);
				alreadySeen.put(childName, childName);
				if (haveAlreadySeen) {
					sd.declareChild(childName, true);
				}
				else {
					sd.declareChild(childName);
				}
			}
		}
		else {
			// neither child element nor non-white text.
			// put this in the ambiguous file ... maybe it will be defined later
			// in the file
			ambiguousCases.add(myName);
		}

	}

	public void finishAmbiguousCases(Schema schema) throws Exception {
		Enumeration<String> e = ambiguousCases.elements();
		while (e.hasMoreElements()) {
			String ambigName = e.nextElement();
			SchemaDef sd = schema.lookUpDefinition(ambigName);
			if (sd == null) {
				sd = schema.addData(ambigName);
				sd.setAttribute("ambiguous", "true");
			}
		}
	}
}
