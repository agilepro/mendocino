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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Vector;

/**
 * Holds a stream of bytes in memory. It is a buffer that you can stream to, and
 * stream from, in either bytes or characters. More efficient than a byte array
 * since the bytes are not held in a contiguous array, and the bytes do not need
 * to be copied around in order to keep the byte array contiguous.
 * <p>
 * To write bytes to the memory file, either 1) Get an output stream and write
 * output to it 2) Instruct the memory file to fill itself from an InputStream
 * <p>
 * To read bytes from the memory file, either 3) Get an InputStream and read
 * from it 4) Instruct the memory file to write itself to an OutputStream.
 * <p>
 * For character-oriented reading & writing, only UTF-8 character encoding is
 * supported, because that is the only encoding that can represent the entire
 * Unicode set without loss.
 * <p>
 * For getting characters into a mem file you can: 5) Get a Writer to write
 * characters to the memory file 6) Instruct to read all chars from a Reader
 * into the mem file.
 * <p>
 * for getting characters out of a mem file, you can: 7) Get a Reader to read
 * characters from the memory file 8) Instruct to write all chars to a Writer.
 * <p>
 * Usage and Justification: The main usage is that you need to construct the
 * contents of a file programmatically, and then parse it, or alternately you
 * need to write something out, and the examine the results. In both cases you
 * needed a temporary buffer to hold the output/input. Often in Java a
 * StringBuffer is used for this but that is (1) inefficient/slow, and (2)
 * character oriented when you need bytes. The common practice of storing bytes
 * in characters causes a lot of confusion and bugs. Programmers tend to want to
 * use String for everything since they are the basic building block so of any
 * programs, but constructing a long string programmatically, by building
 * substring and putting them together in a recursive way is very inefficient,
 * and the string is copied many times in the process.
 * <p>
 * To compose something from strings in memory, use this approach:
 *
 * <pre>
 * MemFile mf = new MemFile();
 * Writer w = mf.getWriter();
 * w.write(&quot;This is the first line\n&quot;);
 * w.write(&quot;This is the second line\n&quot;);
 * w.write(&quot;This is the third line\n&quot;);
 * // then use getReader() or getInputStream() to pass to a method that consumes
 * // the file as if it was a stream.
 * </pre>
 *
 * Threading: MemFile should be used and accessed only from a single thread.
 * Program should input everything to the mem file, and then read everything.
 * Helper classes for InputStream and OutputStream will not necessarily return
 * the right values if input is done at the same time as output.
 * <p>
 * <i>Why not use a StringBuffer?</i> Because a StringBuffer is optimized for fast
 * conversion to a string, and to do this it keeps all the characters in a
 * single contiguous array. While you are filling the buffer, if it runs out of room, it
 * allocates a bigger contiguous array, and then copies the characters from the
 * old buffer to the new buffer. This can happen multiple times. MemFile will
 * never do this, because it does not require the bytes to be in a single
 * contiguous array.
 * <p>
 * What if you need a String to pass to a method. You can construct a string in
 * the normal way: Create a StringBuffer, create a StringBufferWriter, and ask
 * the MemFile to write the entire contents to that.  But once you start using
 * streams correctly, the need to convert them to strings is almost elminated.
 * <p>
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class MemFile {

    // holds all the bytes as byte arrays in this vector
    Vector<byte[]> contents;

    public MemFile() throws Exception {
        contents = new Vector<byte[]>();
    }

    /**
     * Gets rid of all stored contents and clears out memory ready to receive
     * new content.
     */
    public void clear() {
        contents.clear();
    }

    /**
     * Reads all bytes from the passed in InputStream and stored the entire
     * contents in memory.
     */
    public void fillWithInputStream(InputStream in) throws Exception {
        byte[] buf = new byte[5000];
        int len = in.read(buf);
        while (len > 0) {
            if (len == 5000) {
                contents.add(buf);
                buf = new byte[5000];
            }
            else {
                addPartial(buf, len);
            }
            len = in.read(buf);
        }
    }

    /**
     * Reads all character from the passed in Reader and stores the entire
     * contents in memory.
     */
    public void fillWithReader(Reader in) throws Exception {
        char[] buf = new char[5000];
        Writer w = getWriter();

        int len = in.read(buf);
        while (len > 0) {
            if (len == 5000) {
                w.write(buf);
            }
            else {
                w.write(buf, 0, len);
            }
            len = in.read(buf);
        }
        w.flush();
        w.close();
    }

    /**
     * Writes the entire contents of the memory file to the OutputStream passed.
     */
    public void outToOutputStream(OutputStream out) throws Exception {
        for (byte[] buf : contents) {
            out.write(buf);
        }
    }

    /**
     * Writes the entire contents of the memory file to the Writer that is
     * passed.
     */
    public void outToWriter(Writer w) throws Exception {
        Reader r = getReader();
        char[] buf = new char[2000];
        int amt = r.read(buf);
        while (amt > -1) {
            w.write(buf, 0, amt);
            amt = r.read(buf);
        }
    }

    /**
     * Returns an input stream which may be read from in order to read the
     * contents of the memory file.
     */
    public InputStream getInputStream() {
        return new MemFileInputStream(this);
    }

    /**
     * Returns a Reader which may be read from in order to read the contents of
     * the memory file, assuming that the file is in UTF-8 encoding.
     */
    public Reader getReader() throws Exception {
        return new InputStreamReader(getInputStream(), "UTF-8");
    }

    /**
     * Returns an output stream which may be written to in order to fill the
     * memory file. Adds to the end of whatever is currently in memory, so use
     * "Clear" if you want to start with an empty memfile. Holds a buffer, so be
     * sure to flush and/or close to get the complete value into the mem file.
     */
    public OutputStream getOutputStream() {
        return new MemFileOutputStream(this);
    }

    /**
     * Returns a Writer which may be written to in order to fill the memory
     * file. Adds to the end of whatever is currently in memory, so use "Clear"
     * if you want to start with an empty memfile. Only supports UTF-8. Holds a
     * buffer, so be sure to flush and/or close to get the complete value into
     * the mem file.
     */
    public Writer getWriter() throws Exception {
        return new OutputStreamWriter(getOutputStream(), "UTF-8");
    }

    /**
     * Takes the byte array and adds it to the file. NOTE: the actual object is
     * retained, so if you modify the contents of this buffer you will modify
     * the file. Do NOT reuse the buffer after passing it to this routine.
     */
    public void adopt(byte[] buf) {
        contents.add(buf);
    }

    /**
     * Returns the number of bytes that the MemFile currently is holding.
     */
    public int totalBytes() {
        int total = 0;
        for (byte[] buf : contents) {
            total += buf.length;
        }
        return total;
    }

    /**
     * Returns the number of characters that the MemFile currently is holding.
     * Caution, the result is accurate, but this requires scanning and deciding
     * all the bytes to determine when multibyte sequences add up to only a
     * single character.
     */
    public int totalChars() {
        int total = 0;
        for (byte[] buf : contents) {
            for (byte ch : buf) {
                // There are three cases:
                // 1) if the byte is below 128, then it is a simple ASCII
                // character where each character takes one byte. Count this.
                // 2) if the byte is between 128 and 192 then it is part of
                // a multibyte sequence. Don't count any of these.
                // since bytes are signed, this is -128 thru -65
                // 3) if the byte is 192 or above, then it is the terminating
                // byte of a multibyte character. Count this.
                // since bytes are signed, this is -64 thru -1
                if (ch >= -64) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * copies the specified number of bytes from the byte array and adds it to
     * the file. It is OK to use the buffer for other purposes after this.
     */
    public void addPartial(byte[] buf, int len) {
        if (len > 0) {
            byte[] buf2 = new byte[len];
            for (int i = 0; i < len; i++) {
                buf2[i] = buf[i];
            }
            contents.add(buf2);
        }
    }

    class MemFileInputStream extends InputStream {
        MemFile mf = null;
        int idx = 0;
        byte[] thisBuf = null;
        int idxInBuf = 0;

        MemFileInputStream(MemFile newmf) {
            mf = newmf;
            if (mf.contents.size() > 0) {
                thisBuf = mf.contents.elementAt(0);
            }
            idx = 1;
            idxInBuf = 0;
        }

        public int read() throws IOException {
            while (thisBuf != null) {
                if (idxInBuf >= thisBuf.length) {
                    if (idx >= mf.contents.size()) {
                        return -1;
                    }
                    thisBuf = mf.contents.elementAt(idx);
                    if (thisBuf == null) {
                        return -1;
                    }
                    idxInBuf = 0;
                    idx++;
                }
                if (idxInBuf < thisBuf.length) {
                    // unsigned value!
                    int res = (thisBuf[idxInBuf]) & 0xFF;
                    idxInBuf++;
                    return res;
                }
            }
            return -1;
        }

        // returns the number of bytes in the current buffer
        public int available() throws IOException {
            if (thisBuf != null) {
                return 0;
            }
            return thisBuf.length - idxInBuf;
        }

    }

    class MemFileOutputStream extends OutputStream {
        MemFile mf = null;
        byte[] thisBuf = null;
        int idxInBuf = 0;

        MemFileOutputStream(MemFile newmf) {
            mf = newmf;
            thisBuf = new byte[5000];
            idxInBuf = 0;
        }

        public void write(int b) throws IOException {
            if (idxInBuf >= 5000) {
                mf.adopt(thisBuf);
                thisBuf = new byte[5000];
                idxInBuf = 0;
            }
            thisBuf[idxInBuf] = (byte) b;
            idxInBuf++;
        }

        public void flush() throws IOException {
            if (idxInBuf > 0) {
                mf.addPartial(thisBuf, idxInBuf);
                idxInBuf = 0;
            }
        }

        public void close() throws IOException {
            if (idxInBuf > 0) {
                mf.addPartial(thisBuf, idxInBuf);
                idxInBuf = 0;
            }
        }
    }
}
