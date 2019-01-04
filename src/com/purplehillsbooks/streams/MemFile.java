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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

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
    private ArrayList<byte[]> contents;

    // this is the new, unfinished buffer must never be NULL!
    private byte[] incomingBytes = null;
    // position in the new buffer
    private int    incomingPos   = 0;


    public MemFile() throws Exception {
        contents = new ArrayList<byte[]>();
        incomingBytes = new byte[5000];
    }

    /**
     * Gets rid of all stored contents and clears out memory ready to receive
     * new content.
     */
    public void clear() {
        contents.clear();
        incomingPos = 0;
    }


    /*
     * This is the CORE routine for adding bytes to the internal buffers
     */
    private void addByte(int b) throws IOException {
        if (incomingPos >= 5000) {
            adopt(incomingBytes);
            incomingBytes = new byte[5000];
            incomingPos = 0;
        }
        incomingBytes[incomingPos] = (byte) b;
        incomingPos++;
    }




    /**
     * Reads all bytes from the passed in InputStream and stored the entire
     * contents in memory.
     */
    public void fillWithInputStream(InputStream in) throws Exception {
        byte[] buf = new byte[5000];
        OutputStream out = getOutputStream();
        int len = in.read(buf);
        while (len > 0) {
            if (len == 5000) {
                out.write(buf);
            }
            else {
                out.write(buf, 0, len);
            }
            len = in.read(buf);
        }
        out.flush();
        out.close();
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
        out.write(incomingBytes, 0, incomingPos);
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
     * Writes the entire contents of the memory file to the file name passed in
     */
    public void outToFile(File file) throws Exception {
        OutputStream os = new FileOutputStream(file);
        this.outToOutputStream(os);
        os.flush();
        os.close();
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
        // Should get this to work some day so that there is no buffer in the middle
        //return new UTF8Writer(getOutputStream());
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
        //now account for the partial buffer
        total += incomingPos;
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

        //account for those in the incoming buffer
        for (int i=0; i<incomingPos; i++) {
            if (incomingBytes[i] >= -64) {
                total++;
            }
        }
        return total;
    }

    /**
     * copies the specified number of bytes from the byte array and adds it to
     * the file. It is OK to use the buffer for other purposes after this.
     */
    public void addPartial(byte[] buf, int pos, int len) {

        // first test and handle the incoming overflow situation
        // there might be many of these if incoming buffer is
        // quite large
        while (len - pos > 5000 - incomingPos) {
            int amtToXFer = 5000 - incomingPos;
            for (int i = 0; i < amtToXFer; i++) {
                incomingBytes[i+incomingPos] = buf[i+pos];
            }
            adopt(incomingBytes);
            incomingBytes=new byte[5000];
            incomingPos=0;
            pos = pos + amtToXFer;
        }

        //now we have less than a full buffer to deal with, transfer the rest
        if (len - pos > 0) {
            int amtToXFer = len - pos;
            for (int i = 0; i < amtToXFer; i++) {
                incomingBytes[i+incomingPos] = buf[i+pos];
            }
            incomingPos = incomingPos + amtToXFer;
        }
    }


    /**
    * Returns the entire contents of the MemFile as a single string.
    * It first figures out how many characters there are, and then
    * allocates a single StringBuffer of the right size,
    * puts the characters into it, and returns the string.
    *
    * It does this copying every time you call it, so be frugal.
    */
    public String toString() {
        try {
            int size = totalChars();
            StringBuffer sb = new StringBuffer(size+10);
            Reader r = getReader();
            char[] buf = new char[1000];
            int amt = r.read(buf, 0, 1000);
            while (amt>0) {
                sb.append(buf,0,amt);
                amt = r.read(buf, 0, 1000);
            }
            String res = sb.toString();
            return res;
        }
        catch (Exception e) {
            throw new RuntimeException("FATAL ERROR while converting a MemFile to a String", e);
        }
    }

    ////////////////////////////////////////////////////////////////////

    class MemFileInputStream extends InputStream {
        MemFile mf = null;
        int idx = 0;
        byte[] currentBuf = null;
        int currentBufAmt;
        int posInBuf = 0;

        MemFileInputStream(MemFile newmf) {
            mf = newmf;
            if (mf.contents.size() > 0) {
                currentBuf = mf.contents.get(0);
                currentBufAmt = currentBuf.length;
            }
            else {
                currentBuf = mf.incomingBytes;
                currentBufAmt = mf.incomingPos;
            }
            idx = 1;
            posInBuf = 0;
        }

        public int read() throws IOException {
            if (posInBuf >= currentBufAmt) {
                if (idx > mf.contents.size()) {
                    return -1;
                }
                else if (idx == mf.contents.size()) {
                    currentBuf = mf.incomingBytes;
                    currentBufAmt = mf.incomingPos;
                }
                else {
                    currentBuf = mf.contents.get(idx);
                    currentBufAmt = currentBuf.length;
                }
                posInBuf = 0;
                idx++;
            }
            // return an unsigned value!
            int res = (currentBuf[posInBuf]) & 0xFF;
            posInBuf++;
            return res;
        }

        // returns the number of bytes in the current buffer
        public int available() throws IOException {
            if (currentBuf == null) {
                return 0;
            }
            return currentBufAmt - posInBuf;
        }

    }

    ////////////////////////////////////////////////////////////////////

    class MemFileOutputStream extends OutputStream {
        MemFile mf = null;

        MemFileOutputStream(MemFile newmf) {
            mf = newmf;
        }

        public void write(int b) throws IOException {
            mf.addByte(b);
        }

        public void flush() throws IOException {
            //there is nothing to do, no flushing required
        }

        public void close() throws IOException {
            //there is nothing to do, no flushing required
        }
    }


    class UTF8Writer extends Writer {
        OutputStream wos = null;
        int  firstSurrogateChar = -1;

        UTF8Writer(OutputStream _wos) {
            wos = _wos;
        }

        public void write(int ch) throws IOException {

            //handle the second half of a surrogate char pair
            if (firstSurrogateChar>0) {
                if (ch < 0xD800 || ch > 0xDFFF) {
                     throw new IOException("UTF-16 encoding error: got the first character of a surrogate pair, but followed by an invalid character: "
                         +Integer.toString(firstSurrogateChar)+"&"+Integer.toString(ch));
                }
                int combined = 0x10000 + ((firstSurrogateChar % 1024) << 10) + (ch % 1024);
                wos.write(140 + ((combined >> 18) % 8));
                wos.write(128 + (combined % 64));
                wos.write(128 + ((combined >> 6) % 64));
                wos.write(128 + ((combined >> 12) % 64));
                firstSurrogateChar=-1;
                return;
            }
            if (ch < 128)  {
                wos.write(ch);
                return;
            }
            if (ch < 2048)  {
                wos.write(192 + ((ch >> 6) % 32));
                wos.write(128 + (ch % 64));
                return;
            }

            // exclude the 'surrogate' characters
            if (ch < 0xD800 || ch > 0xDFFF) {
                wos.write(224 + ((ch >> 12) % 16));
                wos.write(128 + (ch % 64));
                wos.write(128 + ((ch >> 6) % 64));
                return;
            }

            // oh boy, we are in the surrogate-character world, store this and
            // wait for the next character
            firstSurrogateChar = ch;
        }

        @Override
        public void write(char[] buf, int pos, int last) throws IOException {
            for (int i=pos; i<last; i++) {
                write(buf[i]);
            }
        }


        public void flush() throws IOException {
            //there is nothing to do, no flushing required
        }

        public void close() throws IOException {
            //there is nothing to do, no flushing required
        }

    }

}
