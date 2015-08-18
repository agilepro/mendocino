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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents the a node in a schema, either a "container" or a
 * "data" node. Use "isContainer" to tell which one.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class SchemaDef extends Mel {
	public SchemaDef(Document doc, Element ele) {
		super(doc, ele);
	}

	public boolean isContainer() {
		return ("container".equals(getName()));
	}

	/**
	 * Declares that this container has an attribute. Pure data values are not
	 * allowed to have attributes.
	 */
	public Mel declareAttribute(String attrName) throws Exception {
		Mel child = findChild("attr", "name", attrName, Mel.class);
		if (child == null) {
			// if it gets here, then it did not exist before
			// so we need to add it
			child = addChild("attr", Mel.class);
			child.setAttribute("name", attrName);
		}
		return child;
	}

	/**
	 * Declares that this container contains something with the specified name
	 * but does not specify whether plural or not. This will add the declaration
	 * if it is not there. If it is there, it will leave the plural setting at
	 * its previous value.
	 */
	public Mel declareChild(String childName) throws Exception {
		Mel child = findChild("contains", "name", childName, Mel.class);
		if (child == null) {
			// if it gets here, then it did not exist before
			// so we need to add it
			child = addChild("contains", Mel.class);
			child.setAttribute("name", childName);
		}
		return child;
	}

	/**
	 * Declares that this container contains something with the specified name
	 * and also specifies the plurality of that entry. If a declaration already
	 * exists, the plurality is set to the specified value.
	 */
	public void declareChild(String childName, boolean allowPlural) throws Exception {
		Mel child = declareChild(childName);
		child.setAttribute("plural", allowPlural ? "true" : "false");
	}

	/**
	 * Tells whether a specific declaration is allowed to be plural or not.
	 */
	public boolean childIsPlural(String childName) throws Exception {
		Mel child = findChild("contains", "name", childName, Mel.class);
		if (child == null) {
			return false;
		}
		return ("true".equals(child.getAttribute("plural")));
	}

}
