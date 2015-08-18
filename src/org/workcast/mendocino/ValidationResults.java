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
 * This class represents the base node of a schema for an MDS file. The schema
 * file is itself an MDS file, and so is read in pretty much the same way, but
 * this class provides some extra capabilities for efficiently handlign schemas.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class ValidationResults extends Mel {

	public ValidationResults(Document doc, Element ele) throws Exception {
		super(doc, ele);
	}

	public void addResult(String result) {
		addVectorValue("result", result);
	}
}
