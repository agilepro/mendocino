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
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class represents the base node of a schema for an MDS file. The schema
 * file is itself an MDS file, and so is read in pretty much the same way, but
 * this class provides some extra capabilities for efficiently handling schemas.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class Schema extends Mel {
	Hashtable<String, SchemaDef> allElements;
	boolean hasRoot = false;

	public Schema(Document doc, Element ele) throws Exception {
		super(doc, ele);

		if (allElements == null) {
			allElements = new Hashtable<String, SchemaDef>();
		}

		Vector<SchemaDef> containers = getChildren("container", SchemaDef.class);
		for (SchemaDef sd : containers) {
			String eleName = sd.getAttribute("name");
			if (eleName == null || eleName.length() == 0) {
				throw new Exception(
						"Schema is not usable, there is a SchemaDef object that does not contain a name.");
			}
			if (allElements.get(eleName) != null) {
				throw new Exception(
						"Schema is not usable, there are two different definition items with the same name: "
								+ eleName);
			}
			allElements.put(eleName, sd);
		}
		containers = getChildren("data", SchemaDef.class);
		for (SchemaDef sd : containers) {
			allElements.put(sd.getAttribute("name"), sd);
		}
	}

	/**
	 * Given a File object (that points to a real existing MDS schema) This
	 * global static method will read the file and return a Schema for the base
	 * of the tree of the file.
	 */
	public static Schema readFile(File inFile) throws Exception {
		return readInputStream(new FileInputStream(inFile));
	}

	/**
	 * Given a byte stream (that points to a real existing MDS file) this global
	 * static method will read the stream and return a Mel for the base of the
	 * tree of the file. Encoding is always UTF-8.
	 */
	public static Schema readInputStream(InputStream is) throws Exception {
		return Mel.readInputStream(is, Schema.class);
	}

	public SchemaDef lookUpDefinition(String defName) {
		SchemaDef sd = allElements.get(defName);
		if (sd == null) {
			// should this throw an exception? If a program is expected to
			// know the schema, then asking for an def that does not exist would
			// be an error. On the other hand, is it possible that asking to see
			// if a def exists will be useful?
			return null;
		}
		return sd;
	}

	public SchemaDef addContainer(String defName) throws Exception {
		SchemaDef sd = addChild("container", SchemaDef.class);
		sd.setAttribute("name", defName);
		allElements.put(defName, sd);
		return sd;
	}

	public SchemaDef addData(String defName) throws Exception {
		SchemaDef sd = addChild("data", SchemaDef.class);
		sd.setAttribute("name", defName);
		allElements.put(defName, sd);
		return sd;
	}

	/**
	 * Can only set one root. Root can be set once, no way to remove it. The
	 * warning works only if you are building a schema and never removing things
	 * from the schema. Clearly if you remove the root the boolean will not be
	 * cleared. If you wish to deconstruct a schema, you can use the Mel methods
	 * directly.
	 */
	public void addRoot(String rootName) throws Exception {
		if (hasRoot) {
			throw new Exception("This schema already has a root element.  Only one is allowed.");
		}
		setScalar("root", rootName);
		hasRoot = true;
	}
}
