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

package org.workcast.streams;

import java.io.Writer;
import java.io.IOException;

/**
 * This class is for encoding values in order to make make text appear in an
 * HTML page. This is not for creating HTML markeup. Instead you write text to
 * this writer, and it properly encodes everything so that what is seen on the
 * page is exactly what was written to this class.
 *
 * The example usage would be for a "comment" on a web page. A user writes a
 * comment using any variety of characters, including quotes and angle-brackets
 * which are characters that have a special meaning in HTML. What you want to do
 * is to have the comment displayed on the page exactly as the user input it.
 * But to do so, quote characters, and angle bracket characters must be properly
 * converted so that they are not mistaken for HTML markup by the browser.
 *
 * There are two ways: a writer and a static encoding method. Both ways assume
 * you are constructing your HTML by writing them to a Writer object. This is
 * more efficient than concatenating strings together.
 *
 * 1. A Writer Instance. If you want to use a method that is prepared to write a
 * raw value to a Writer, and you want that raw value to be HTML encoded, then
 * you can construct a HTMLWriter on your existing writer, and pass HTMLWriter
 * to the routine that will write the raw value. Every (raw) value written will
 * be converted as it is passed on to the wrapped Writer. An example of this use
 * is if you have a DOM tree (which can stream itself to a Writer) but you want
 * the serialized DOM to be displayed on a HTML web page (and not interpreted as
 * HTML by the browser). Passing the HTMLWriter to the DOM for serialization
 * will mean that all the XML output will be properly encoded as HTML markup so
 * that the resulting XML output will be displayed on the HTML page.
 *
 * 2. A Static Method. If the value you want to convert is a string (as is often
 * the case) you don't need to construct a new writer object for this. Just call
 * the static writeHtml method, passing a Writer and the string you want
 * converted. The string will be converted as it is written to the Writer.
 *
 * the Java code to to create a HTML page with a sample XML within it:
 *
 * <pre>
 * Writer w; // given as the place the page is being written
 * String val = &quot;&lt;a&gt;&lt;b&gt;value of b&lt;/b&gt;&lt;c&gt;value of c&lt;/c&gt;&lt;/a&gt;&quot;;
 * w.write(&quot;&lt;p&gt;The sample XML expression is: &quot;);
 * HTMLWriter.writeHtml(w, val);
 * w.write(&quot;&lt;/p&gt;&lt;/n&gt;&quot;);
 * </pre>
 *
 * The val can be ANY legal String value, e.g. something a user typed in. The
 * output stream of characters will accurately converted to HTML so that the
 * value is displayed to the user.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class HTMLWriter extends Writer {
	private Writer wrapped;

	public HTMLWriter(Writer _wrapped) {
		wrapped = _wrapped;
	}

	public void write(int c) throws IOException {
		writeHtmlChar(wrapped, c);
	}

	public void write(char[] chs, int start, int len) throws IOException {
		if (start < 0) {
			throw new RuntimeException(
					"negative start position passed to HTMLWriter.write(char[], int, int)");
		}
		if (len < 0) {
			throw new RuntimeException("negative len passed to HTMLWriter.write(char[], int, int)");
		}
		int last = start + len;
		if (last > chs.length) {
			throw new RuntimeException("start + len (" + last
					+ ") is longer than char array size (" + chs.length
					+ ") passed to HTMLWriter.write(char[], int, int)");
		}
		for (int i = start; i < last; i++) {
			writeHtmlChar(wrapped, chs[i]);
		}
	}

	public void close() throws IOException {
		wrapped.close();
	}

	public void flush() throws IOException {
		wrapped.flush();
	}

	private static void writeHtmlChar(Writer w, int ch) throws IOException {
		switch (ch) {
		case '&':
			w.write("&amp;");
			return;
		case '<':
			w.write("&lt;");
			return;
		case '>':
			w.write("&gt;");
			return;
		case '"':
			w.write("&quot;");
			return;
		default:
			w.write(ch);
			return;
		}
	}

	/**
	 * Encodes a single <code>String</code> value to a HTML markup so that the
	 * HTML page will display the original passed in value exactly..
	 *
	 * <p>
	 * If you are constructing an HTML page, and you have a String value that
	 * you want to be displayed on the page, and you do not want anything within
	 * that string value to be accidentally misinterpreted as markup, you must
	 * use this method to scan the String and convert any embedded problematic
	 * characters into their escaped equivalents. The result of the conversion
	 * is written into the stream that you pass in.
	 * </p>
	 *
	 *
	 * @param w
	 *            The Writer object to which the encoded String value is added.
	 * @param val
	 *            The <code>String</code> value to encode.
	 * @return Nothing. The encoded version is written to the Writer.
	 */
	public static void writeHtml(Writer w, String val) throws IOException {
		if (w == null) {
			throw new RuntimeException(
					"Program Logic Error: JavaScriptWriter.encode requires a non-null Writer to be passed.");
		}
		// passing a null in results a no output, no quotes, nothing
		if ((val == null)) {
			return;
		}
		int len = val.length();
		for (int i = 0; i < len; i++) {
			char ch = val.charAt(i);
			writeHtmlChar(w, ch);
		}
	}

	/**
	 * writeHtmlWithLines is a special case routine for certain situations. In
	 * HTML a newline character is exactly equivalent to a space or tab
	 * character. This has the unfortunate effect of making the display of a
	 * normal text file to be wrapped into a single long, unreadable, paragraph.
	 * One solution is to use "PRE" tag to preserve the effect of newlines, but
	 * that also causes a fixed width font which is not always desirable.
	 *
	 * This method will convert raw text to equivalent HTML, so that the
	 * original text is displayed but with one embellishment: newline characters
	 * will be converted to a "BR" break tag, forcing the following text will
	 * start at the beginning of the next line. It is not a perfect solution,
	 * because not all text is formatted in this way that a newline always means
	 * the same as a break character, but for normal, casual blocks of text,
	 * like comments and short messages, this can work suitably.
	 */
	public static void writeHtmlWithLines(Writer w, String val) throws IOException {
		if (w == null) {
			throw new RuntimeException(
					"Program Logic Error: JavaScriptWriter.encode requires a non-null Writer to be passed.");
		}
		// passing a null in results a no output, no quotes, nothing
		if ((val == null)) {
			return;
		}
		int len = val.length();
		for (int i = 0; i < len; i++) {
			char ch = val.charAt(i);
			if (ch == '\n') {
				w.write("<br/>\n");
			}
			else {
				writeHtmlChar(w, ch);
			}
		}
	}

}