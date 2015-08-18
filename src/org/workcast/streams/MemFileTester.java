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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import org.workcast.testframe.TestRecorder;
import org.workcast.testframe.TestRecorderText;

/**
 * Test class for the Mem File object
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class MemFileTester {

	TestRecorder tr;

	public MemFileTester() {
	}

	public void runTests(TestRecorder newTr) throws Exception {
		tr = newTr;

		testMemFiles("basic", "abc 123");
		testMemFiles("upper ascii", "Sämplé\nStrîñg\t");
		testMemFiles("double byte", "\u1234\u1235\u1236");
		// testMemFiles("surrogate pairs", "\u1234\u1235\u1236");

		testJSEncode("nothing special", "abc123", "abc123");
		testJSEncode("punctuation", "the \"big\" and\\or", "the \\\"big\\\" and\\\\or");
		testJSEncode("upper ascii", "Sämplé\nStrîñg\t", "S\\xE4mpl\\xE9\\nStr\\xEE\\xF1g\\t");
		testJSEncode("just a quote", "\"", "\\\"");
		testJSEncode("two quotes", "\"\"", "\\\"\\\"");
		testJSEncode("one backslash", "\\", "\\\\");
		testJSEncode("ascii punctuation", ",.<>/?;:[{]}=+-_)(*&^%$#@!",
				",.<>/?;:[{]}=+-_)(*&^%$#@!");

		testWriteHTML("nothing special", "abc123", "abc123");
		testWriteHTML("quote and angle", "the \"big\" and<or>case",
				"the &quot;big&quot; and&lt;or&gt;case");
		testWriteHTML("upper ascii", "Sämplé\nStrîñg\t", "Sämplé\nStrîñg\t");
	}

	private void testMemFiles(String caseDescription, String testCase) throws Exception {
		// first, lets get an encoded copy of the test case
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		@SuppressWarnings("resource")
		Writer osw = new OutputStreamWriter(baos, "UTF-8");
		osw.write(testCase);
		osw.flush();
		osw.close();
		byte[] encodedCase = baos.toByteArray();
		int bytesInCase = encodedCase.length;

		MemFile mf = new MemFile();

		// write the same value 1000 items
		osw = mf.getWriter();
		for (int i = 0; i < 1000; i++) {
			osw.write(testCase);
		}
		osw.flush();
		osw.close();
		tr.markPassed(caseDescription + " - Wrote 1000 iterations");

		tr.testInt(caseDescription + " - Num bytes for 1000 copies", mf.totalBytes(),
				1000 * bytesInCase);
		tr.testInt(caseDescription + " - Num chars for 1000 copies", mf.totalChars(),
				1000 * testCase.length());

		testReadChar(caseDescription + " - initial ", tr, mf, testCase);
		testReadByte(caseDescription + " - initial ", tr, mf, encodedCase);

		// there are four ways to copy a mem file to another, and this exercises
		// all 8 of the streaming methods.
		// IS: file1 creates InputStream, file2 reads from it
		// OS: file1 writes to an OutputStream created by file 2
		// R: file1 creates Reader, file2 reads from it
		// W: file1 writes to a Writer created by file 2

		{
			// IS: file1 creates InputStream, file2 reads from it
			MemFile mf2 = new MemFile();
			InputStream is2 = mf.getInputStream();
			mf2.fillWithInputStream(is2);

			testReadChar(caseDescription + " - IS copy ", tr, mf2, testCase);
			testReadByte(caseDescription + " - IS copy ", tr, mf2, encodedCase);
		}

		{
			// OS: file1 writes to an OutputStream created by file 2
			MemFile mf3 = new MemFile();
			OutputStream os3 = mf3.getOutputStream();
			mf.outToOutputStream(os3);
			os3.flush();
			os3.close();

			testReadChar(caseDescription + " - OS copy ", tr, mf3, testCase);
			testReadByte(caseDescription + " - OS copy ", tr, mf3, encodedCase);
		}

		{
			// R: file1 creates Reader, file2 reads from it
			MemFile mf4 = new MemFile();
			Reader r4 = mf.getReader();
			mf4.fillWithReader(r4);

			testReadChar(caseDescription + " - R copy  ", tr, mf4, testCase);
			testReadByte(caseDescription + " - R copy  ", tr, mf4, encodedCase);
		}

		{
			// W: file1 writes to a Writer created by file 2
			MemFile mf5 = new MemFile();
			Writer w5 = mf5.getWriter();
			mf.outToWriter(w5);
			w5.flush();
			w5.close();

			testReadChar(caseDescription + " - W copy  ", tr, mf5, testCase);
			testReadByte(caseDescription + " - W copy  ", tr, mf5, encodedCase);
		}

		mf.clear();
		tr.testInt(caseDescription + " - Num bytes after clear", mf.totalBytes(), 0);
		tr.testInt(caseDescription + " - Num chars after clear", mf.totalChars(), 0);
	}

	private void testReadChar(String caseDescription, TestRecorder tr, MemFile mf, String testCase)
			throws Exception {
		// read to see if the exact thing can be read out of the input stream
		String thisTestId = caseDescription + " - Read 1000 iterations";
		Reader isr = mf.getReader();
		for (int i = 0; i < 1000; i++) {
			int last = testCase.length();
			for (int j = 0; j < last; j++) {
				char testChar = testCase.charAt(j);
				char readChar = (char) isr.read();
				if (testChar != readChar) {
					tr.markFailed(thisTestId, "At position " + j + " of iteration " + i
							+ " expected character " + testChar + " but got " + readChar
							+ " instead");
					return;
				}
			}
		}
		if (-1 == isr.read()) {
			tr.markPassed(thisTestId);
		}
		else {
			tr.markFailed(thisTestId, "Failed to get -1 when reading off the end of buffer");
		}
	}

	private void testReadByte(String caseDescription, TestRecorder tr, MemFile mf,
			byte[] encodedCase) throws Exception {
		// read to see if the exact thing can be read out of the input stream
		String thisTestId = caseDescription + " - Stream 1000 iterations";
		InputStream is = mf.getInputStream();
		for (int i = 0; i < 1000; i++) {
			int last = encodedCase.length;
			for (int j = 0; j < last; j++) {
				byte testByte = encodedCase[j];
				int readByte = is.read();
				if (readByte != testByte) {
					tr.markFailed(thisTestId, "At position " + j + " of iteration " + i
							+ " expected byte value " + testByte + " but got " + readByte
							+ " instead");
					return;
				}
			}
		}
		if (-1 == is.read()) {
			tr.markPassed(thisTestId);
		}
		else {
			tr.markFailed(thisTestId, "Failed to get -1 when reading off the end of buffer");
		}
	}

	char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	@SuppressWarnings("unused")
	private String prettify(byte[] input) {
		StringBuffer res = new StringBuffer();
		for (byte b : input) {
			// bytes are signed, so 128-255 are negative!
			int i = b;
			if (i < 0) {
				i = i + 256;
			}
			// now i is always positive, like a character
			if (i > 31 && i < 128) {
				res.append((char) i);
			}
			else {
				res.append('[');
				res.append(hex[i / 16]);
				res.append(hex[i % 16]);
				res.append(']');
			}
		}
		return res.toString();
	}

	private void testJSEncode(String description, String raw, String encoded) throws Exception {
		String testCase = "JS method - " + description;
		StringWriter sbw = new StringWriter();
		JavaScriptWriter.encode(sbw, raw);
		String result1 = sbw.toString();
		if (result1.equals(encoded)) {
			tr.markPassed(testCase);
		}
		else {
			tr.markFailed(testCase, "Failed to convert: \nexpected: '" + encoded
					+ "' \nbut got:  '" + result1 + "' instead. " + findPosDiff(encoded, result1));
		}

		testCase = "JS stream - " + description;
		sbw = new StringWriter();
		Writer jsw = new JavaScriptWriter(sbw);
		jsw.write(raw);
		jsw.close();
		result1 = sbw.toString();
		if (result1.equals(encoded)) {
			tr.markPassed(testCase);
		}
		else {
			tr.markFailed(testCase, "Failed to convert: \nexpected: '" + encoded
					+ "' \nbut got:  '" + result1 + "' instead. " + findPosDiff(encoded, result1));
		}
	}

	private void testWriteHTML(String description, String raw, String encoded) throws Exception {
		String testCase = "HTML method - " + description;
		StringWriter sbw = new StringWriter();
		HTMLWriter.writeHtml(sbw, raw);
		String result1 = sbw.toString();
		if (result1.equals(encoded)) {
			tr.markPassed(testCase);
		}
		else {
			tr.markFailed(testCase, "Failed to convert: \nexpected: '" + encoded
					+ "' \nbut got:  '" + result1 + "' instead. " + findPosDiff(encoded, result1));
		}

		testCase = "HTML stream - " + description;
		sbw = new StringWriter();
		Writer hw = new HTMLWriter(sbw);
		hw.write(raw);
		hw.close();
		result1 = sbw.toString();
		if (result1.equals(encoded)) {
			tr.markPassed(testCase);
		}
		else {
			tr.markFailed(testCase, "Failed to convert: \nexpected: '" + encoded
					+ "' \nbut got:  '" + result1 + "' instead. " + findPosDiff(encoded, result1));
		}
	}

	private String findPosDiff(String str1, String str2) {
		int len1 = str1.length();
		int len2 = str2.length();
		if (len1 != len2) {
			return "They are different lengths, " + len1 + " and " + len2 + " respectively.";
		}

		for (int i = 0; i < len1; i++) {
			char ch1 = str1.charAt(i);
			char ch2 = str2.charAt(i);
			if (ch1 != ch2) {
				return "They differ at character " + i + " which is '" + ch1 + "' and '" + ch2
						+ "' respectively.";
			}
		}
		return "Can't find any difference in these strings!";
	}

	public static void main(String args[]) {
		try {
			// if (args.length<2)
			// {
			// throw new Exception("USAGE: MemFileTester");
			// }

			TestRecorderText tr = new TestRecorderText(new OutputStreamWriter(System.out, "UTF-8"),
					true, new String[0], ".", new Properties());
			MemFileTester t1 = new MemFileTester();
			t1.runTests(tr);
		}
		catch (Exception e) {
			System.out.print("\n\n\n====================================================");
			System.out.print("\nEXCEPTION CAUGHT AT MAIN LEVEL:\n");
			e.printStackTrace(System.out);
		}
	}

}
