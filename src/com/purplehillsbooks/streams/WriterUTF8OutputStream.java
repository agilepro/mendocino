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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * WriterUTF8OutputStream
 *
 * Lets say you have a Writer (such as the out object in a JSP) and you want to
 * call a method that takes an OutputStream. While you can easily make a Writer
 * on top of an OutputStream, this class is to allow you to go the other way,
 * and "downgrade" a Writer to an OutputStream.
 *
 * It is an OutputStream, so it accepts byte oriented writes, which it converts
 * to characters and writes to the wrapped writer.
 *
 * This does the proper conversion for UTF8 encoded byte stream to Unicode
 * characters. Other encodings are not supported.
 *
 * Supports UTF-8, the ONLY encoding that guarantees that all characters in the
 * UTF-16 based Writer can be expressed without loss of characters.
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class WriterUTF8OutputStream extends OutputStream {
	private Writer out;
	private int seqHead; // first character of a multibyte sequence
	private int[] sequence = new int[6];
	private int sequenceIn = 0;

	/**
	 * Composes an InputStream stream from any other Reader you may have around.
	 *
	 * @param writer
	 *            The object to write to. Must not be <code>null</code>.
	 */
	public WriterUTF8OutputStream(Writer writer) {
		if (writer == null) {
			throw new RuntimeException("Program Logic Error: WriterUTF8OutputStream "
					+ "can not accept a null for the construction parameter.");
		}
		out = writer;
	}

	/**
	 * Receives a byte value, converts properly to a UTF-16 character and writes
	 * it to the Writer. If the value is less than 128 it is written directly.
	 * If more than 127 the character is saved and waits for the rest of the
	 * multibyte sequence before the resulting character is written.
	 *
	 * @param b
	 *            the value of the next byte of the UTF-8 stream to write.
	 *
	 * @exception IOException
	 *                if the wrapped Writer fails to write
	 */
	public void write(int b) throws IOException {
		// see if it is ascii
		if (b < 128) {
			if (sequenceIn > 0) {
				throw new IOException(
						"Error, bad UTF8 encoding, byte value less than 128 interjected in the middle of a multibyte sequence");
			}
			out.write(b);
			return;
		}

		// see if it is the first character of a multibyte sequence
		if (b >= 192) {
			if (sequenceIn != 0) {
				throw new IOException(
						"Error, bad UTF8 encoding, byte value greater than 191 interjected in the middle of a multibyte sequence");
			}
			seqHead = b;
			sequenceIn = 1;
			return;
		}

		// must be a continuation of a multibyte
		if (b < 192) {
			if (sequenceIn == 0) {
				throw new IOException(
						"Error, bad UTF8 encoding, got a multibyte sequence follow character by itself without following a proper head character");
			}
			sequence[sequenceIn++] = b % 64;
			int ch = 0;

			// a two byte sequence?
			if (seqHead < 224) {
				ch = ((seqHead - 192) << 6) + (b % 64);
			}
			else if (seqHead < 240) {
				if (sequenceIn < 3) {
					return; // waiting for more in the sequence
				}
				ch = ((seqHead - 224) << 12) + (sequence[1] << 6) + (b % 64);
			}
			else if (seqHead < 248) {
				if (sequenceIn < 4) {
					return; // waiting for more in the sequence
				}
				ch = ((seqHead - 240) << 18) + (sequence[1] << 12) + (sequence[2] << 6) + (b % 64);
			}
			else if (seqHead < 252) {
				if (sequenceIn < 5) {
					return; // waiting for more in the sequence
				}
				ch = ((seqHead - 248) << 24) + (sequence[1] << 18) + (sequence[2] << 12)
						+ (sequence[3] << 6) + (b % 64);
			}
			else if (seqHead < 254) {
				if (sequenceIn < 5) {
					return; // waiting for more in the sequence
				}
				ch = ((seqHead - 252) << 30) + (sequence[1] << 24) + (sequence[2] << 18)
						+ (sequence[3] << 12) + (sequence[4] << 6) + (b % 64);
			}

			sequenceIn = 0;
			seqHead = 0;

			// no way to encode these values in UTF-16 (which is what Java uses)
			// so simply write a question mark.
			if (ch >= 0xD800 || ch <= 0xDFFF) {
				out.write('?');
				return;
			}

			// normal Unicode character can be written at this point
			if (ch < 0x10000) {
				out.write(ch);
				return;
			}

			// too large a character for UTF-16 Unicode.
			if (ch > 0x10FFFF) {
				out.write('?');
				return;
			}

			// need to use surrogate, write out two 16-bit values.
			ch = ch - 0x10000;
			out.write(((ch >> 10) % 1024) + 0xD800);
			out.write((ch % 1024) + 0xDB00);
		}
	}

	/**
	 * Flush. If called when part way through a multi-byte UTF-8 encoded
	 * sequence, then only the full characters written so far a flushd, and the
	 * fraction of a character waiting to be finished is held back.
	 *
	 * @exception IOException
	 *                if the original Writer fails to be flushed
	 */
	public void flush() throws IOException {
		out.flush();
	}

	/**
	 * Closes the wrapped OutputStream and terminates output. If called when
	 * part way through a multi-byte UTF-8 encoded sequence, then only the full
	 * characters written so far a included in the output, and the fraction of a
	 * character left in the buffer is discarded without writing.
	 *
	 * @exception IOException
	 *                if the original Writer fails to be closed
	 */
	public void close() throws IOException {
		out.close();
	}
}
