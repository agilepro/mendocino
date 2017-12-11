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

package com.purplehillsbooks.streams;

import java.io.Writer;

/**
 * NullWriter is a writer that throws everything away, and never stores anything
 * anywhere. All functions just return without doing anything. Useful when you
 * are required to pass a Writer, but you are not interested the output that is
 * going to the Writer.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class NullWriter extends Writer {
	public NullWriter() {
	}

	public void close() {
	}

	public void flush() {
	}

	public void write(char[] cbuf) {
	}

	public void write(char[] cbuf, int off, int len) {
	}

	public void write(int c) {
	}

	public void write(String str) {
	}

	public void write(String str, int off, int len) {
	}
}
