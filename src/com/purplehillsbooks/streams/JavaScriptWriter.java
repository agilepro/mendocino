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
import java.io.IOException;

/**
 * This class is for encoding values in order to make Java or JavaScript literal
 * expressions. If you are onstructing a web page that will have a JS script in
 * it, and you want to make a simple assignment statement like this:
 *
 * <pre>
 * var t = &quot;properly formatted javascript literal expression&quot;;
 * </pre>
 *
 * But if you want that value to be variable, then given that you have the value
 * as a string in memory, you need to properly encode the value. If there are
 * any quote characters, they need to be escaped with a backslash. If there are
 * any newline characters, they need to be replaced with the appropriate
 * backslash expression. If there are Unicode characters >128, they need to be
 * encoded into hex value expressions. That is what this class will do.
 *
 * There are two ways: a writer and an encoding method. Both methods assume you
 * are constructing your java script expressions by writing them to a Writer
 * object. This is more efficient than concatenating strings together.
 *
 * 1. A Writer Instance. If you want to use a method that is prepared to write a
 * raw value to a Writer, and you want that raw value to be JavaScript encoded,
 * then you can construct a JAvaScriptWriter on your existing writer, and pass
 * JavaScriptWriter to the routine that will write the raw value. Every (raw)
 * value written will be converted as it is passed on to the wrapped Writer. An
 * example of this use is if you have a DOM tree (which can stream itself to a
 * Writer) but you want the serialized DOM to be placed into a JS literal form
 * on a web page. Passing the JAvaScriptWriter to the DOM for serialization will
 * mean that all the XML output will be properly escaped as a JavaScript
 * expression. The code that is writing the value does not know that the value
 * is going into a JavaScript literal expression ... it just writes.
 *
 * 2. A Static Method. If the value you want to convert is a string (as is often
 * the case) you don't need to construct a new object for this. Just call the
 * static encode method, passing a Writer and the string you want converted. The
 * string will be converted as it is written to the Writer.
 *
 * the Java to ccreate the above expression above might be:
 *
 * <pre>
 * Writer w; // given as the place the page is being written
 * String val = &quot;properly formatted javascript literal expression&quot;;
 * w.write(&quot;var t = \&quot;&quot;);
 * JavaScriptWriter.encode(w, val);
 * w.write(&quot;\&quot;;\n&quot;);
 * </pre>
 *
 * The val can be ANY legal String value, e.g. something a user typed in. The
 * output stream of characters will accurately represent the literal expression
 * for val. Note also that this does NOT do encoding for HTML in order to avoid
 * problems with HTML when putting JS within a web page. Consider using an
 * HTMLWriter inside this to take care of angle brackets and such.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class JavaScriptWriter extends Writer {
	private Writer wrapped;

	public JavaScriptWriter(Writer _wrapped) {
		wrapped = _wrapped;
	}

	public void write(int c) throws IOException {
		encodeChar(wrapped, c);
	}

	public void write(char[] chs, int start, int len) throws IOException {
		if (start < 0) {
			throw new RuntimeException(
					"negative start position passed to JavaScriptWriter.write(char[], int, int)");
		}
		if (len < 0) {
			throw new RuntimeException(
					"negative len passed to JavaScriptWriter.write(char[], int, int)");
		}
		int last = start + len;
		if (last > chs.length) {
			throw new RuntimeException("start + len (" + last
					+ ") is longer than char array size (" + chs.length
					+ ") passed to JavaScriptWriter.write(char[], int, int)");
		}
		for (int i = start; i < last; i++) {
			encodeChar(wrapped, chs[i]);
		}
	}

	public void close() throws IOException {
		wrapped.close();
	}

	public void flush() throws IOException {
		wrapped.flush();
	}

	private static char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
			'C', 'D', 'E', 'F' };
	private static int[] hexvalue = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * Encodes a single <code>String</code> value to a JavaScript literal
	 * expression.
	 *
	 * <p>
	 * If you are constructing a JavaScript expression and you have a String
	 * value that you want to be expressed as a String literal in the
	 * JavaScript, you must use this method to scan the String and convert any
	 * embedded problematic characters into their escaped equivalents. The
	 * result of the conversion is written into the stream that you pass in.
	 * </p>
	 *
	 * <p>
	 * <b>Do NOT simply paste quotes before and after the string!</b>
	 * </p>
	 *
	 * @param w
	 *            The Writer object to which the encoded String value is added.
	 * @param val
	 *            The <code>String</code> value to encode.
	 */
	public static void encode(Writer w, String val) throws IOException {
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
			encodeChar(w, ch);
		}
	}

	/**
	 * Writes out a single UTF-16 encoded character. Normal Java strings are
	 * UTF-16 encoded, and have surrogate values in them, and will be fine. With
	 * UTF-16 no value is greater than 2^16.
	 *
	 * Don't call this with un-encoded Unicode Character values which you might
	 * get by decoding UTF-8 or some other encoding, without re-encoding into
	 * UTF-16, otherwise these extended characters >64K will cause an error.
	 */
	private static void encodeChar(Writer w, int ch) throws IOException {
		switch (ch) {
		case '\"':
			w.write("\\\"");
			return;
		case '\\':
			w.write("\\\\");
			return;
		case '\'':
			w.write("\\\'");
			return;
		case '\n':
			w.write("\\n");
			return;
		case '\t':
			w.write("\\t");
			return;
		case '\r':
			w.write("\\r");
			return;
		case '\f':
			w.write("\\f");
			return;
		case '\b':
			w.write("\\b");
			return;
		default:
			if (ch < 128) {
				w.write(ch);
			}
			else if (ch < 256) {
				w.write("\\x");
				w.write(hexchars[(ch / 16) % 16]);
				w.write(hexchars[ch % 16]);
			}
			else if (ch < 65536) {
				w.write("\\u");
				w.write(hexchars[(ch / 4096) % 16]);
				w.write(hexchars[(ch / 256) % 16]);
				w.write(hexchars[(ch / 16) % 16]);
				w.write(hexchars[ch % 16]);
			}
			else {
				throw new RuntimeException("Program Logic Error: encodeChar was called "
						+ "to write a character (" + Integer.toString(ch) + "), which is greater "
						+ "than 64K which can not happen if the sequence of characters is "
						+ "UTF-16 encoded.");
			}
		}
	}

	/**
	 * Takes a single JavaScript literal and converts it back to a String value.
	 * The literal values is what occurs BETWEEN two quote characters. That
	 * value is scanned, and any backslash escaped values are converted
	 * appropriately.
	 *
	 * <p>
	 * Note: Quotes in the middle of the string that are not allowed without
	 * being escaped by a backslash. Newline characters are not allowed either.
	 * </p>
	 *
	 * @param res
	 *            The <code>StringBuffer</code> object to which the converted
	 *            literalString is added.
	 * @param literalString
	 *            The JavaScript literal to be converted. Must NOT have quotes
	 *            around it, only the stuff inside the quotes.
	 * @exception Exception
	 *                Thrown if either parameter is null.
	 */
	public static void decode(StringBuffer res, String literalString) throws Exception {
		if ((res == null) || (literalString == null)) {
			throw new Exception(
					"Program Logic Error: null parameter passed to JavaScriptStream.decode");
		}

		int pos = 0;
		int last = literalString.length();
		while (pos < last) {
			char ch = literalString.charAt(pos++);
			switch (ch) {
			case '\"':
				throw new Exception("JavaScriptStream.decode received a string which is not "
						+ "properly JS encoded, a double quote character appeared without a slash "
						+ "before it at position " + (pos - 1));
			case '\'':
				throw new Exception("JavaScriptStream.decode received a string which is not "
						+ "properly JS encoded, a single quote character appeared without a slash "
						+ "before it at position " + (pos - 1));
			case '\n':
				throw new Exception("JavaScriptStream.decode received a string which is not "
						+ "properly JS encoded, a newline character appeared at position "
						+ (pos - 1));
			case '\\':
				// do nothing for slash, flow continues to bottom of loop
				break;
			default:
				res.append(ch);
				continue;
			}

			// now handle what follows the slash
			if (pos >= last) {
				// there is no character after it, so complain
				throw new Exception(
						"JavaScriptStream.decode received a string which is not "
								+ "properly JS encoded, a slash appears at the end without anything after it.");
			}
			ch = literalString.charAt(pos++);
			switch (ch) {
			case 'n':
				res.append('\n');
				continue;
			case 't':
				res.append('\t');
				continue;
			case 'r':
				res.append('\r');
				continue;
			case 'f':
				res.append('\f');
				continue;
			case 'b':
				res.append('\b');
				continue;
			case 'x':
				if (pos > last - 2) {
					// there are not enough character after it, so complain
					throw new Exception(
							"JavaScriptStream.decode received a string which is not "
									+ "properly JS encoded, a slash x appeared without two hex digits after it.");
				}
				int i1 = hexvalue[literalString.charAt(pos++)];
				int i2 = hexvalue[literalString.charAt(pos++)];
				res.append((char) (i1 * 16 + i2));
				continue;
			case 'u':
				if (pos > last - 4) {
					// there are not enough character after it, so complain
					throw new Exception(
							"JavaScriptStream.decode received a string which is not "
									+ "properly JS encoded, a slash u appeared without four hex digits after it.");
				}
				int u1 = hexvalue[literalString.charAt(pos++)];
				int u2 = hexvalue[literalString.charAt(pos++)];
				int u3 = hexvalue[literalString.charAt(pos++)];
				int u4 = hexvalue[literalString.charAt(pos++)];
				res.append((char) (u1 * 4096 + u2 * 256 + u3 * 16 + u4));
				continue;
			default:
				// this is the case that we see something un-special after
				// the slash. Just ignore the slash and put the following char
				// into the output stream. Necessary for slash quote and slash
				// slash.
				res.append(ch);
			}
		}
	}

}