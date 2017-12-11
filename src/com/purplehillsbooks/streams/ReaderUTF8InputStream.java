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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * ReaderUTF8InputStream
 *
 * Imagine that you have a Reader object, and you want to call a method
 * that requires an InputStream.  You don't always have access to the underlying
 * InputStream that the Reader is reading from.  This class allows you to
 * "down-convert" from a character oriented Reader back to a byte oriented
 * InputStream with correct UTF-8 encoding characters into the bytes.
 *
 * This is a *proper* conversion from UTF-16 string to UTF-8 byte stream. A
 * string with full Unicode characters can be streamed out as UTF-8 bytes. It
 * performs the conversion on the fly, without needing any large byte buffer
 * internally.
 *
 * Supports UTF-8, the ONLY encoding that guarantees that all characters in the UTF-16
 * can be expressed without loss of characters.
 *
 * In spite of that logic, some might still insist that this class is more
 * general if it had a parameters to specify the encoding to use. This would
 * greatly slow down the operation of the class, because knowledge of UTF-8
 * encoding is used to keep the amount of memory low, and to make the conversion
 * very fast.
 *
 * Mark is not supported because you might try to mark in the middle of a
 * multi-byte sequence, and the sequence would have to be stored away. Have not
 * worked through all the implications of this, so for now not supported.
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class ReaderUTF8InputStream extends InputStream
{

    private Reader in;
    private int[] partial = new int[4];
    private int partialLeft = 0;

    /**
     * Composes an InputStream stream from any other Reader you may have around.
     *
     * @param source
     *          The Reader object to read from. Must not be
     *          <code>null</code>.
     */
    public ReaderUTF8InputStream(Reader source)
    {
        if (in==null)
        {
            throw new RuntimeException("Program Logic Error: ReaderUTF8InputStream "
                +"can not accept a null for the construction parameter.");
        }
        in = source;
    }

    /**
     * Composes a stream from a String.  This is a convenience constructor
     * that constructs a StringReader, and then wraps that Reader.
     *
     * @param source
     *          The string to read from. Must not be <code>null</code>.
     */
    public ReaderUTF8InputStream(String source)
    {
        if (source==null)
        {
            throw new RuntimeException("Program Logic Error: ReaderUTF8InputStream "
                +"can not accept a null for the construction parameter.");
        }
        in = new StringReader(source);
    }

    /**
     * Reads from the Stringreader. If the value is less than 128 it is returned
     * directly, because UTF-8 encoding is this way. If more than 127 the
     * character is broken into a multibyte sequence which will be returned over
     * 2 or more subsequent reads.
     *
     * @return the value of the next byte of the UTF-8 stream, or -1 if there is
     *         nothing left in the original string.
     *
     * @exception IOException
     *         if the original StringReader fails to be read
     */
    public int read() throws IOException
    {
        if (partialLeft > 0)
        {
            partialLeft--;
            return partial[partialLeft];
        }

        int ch = in.read();
        if (ch < 128)
        {
            return ch;
        }
        if (ch < 2048)
        {
            partialLeft = 1;
            partial[0] = 128 + (ch % 64);
            return 192 + ((ch >> 6) % 32);
        }

        // exclude the 'surrogate' characters
        if (ch < 0xD800 || ch > 0xDFFF)
        {
            partialLeft = 2;
            partial[0] = 128 + (ch % 64);
            partial[1] = 128 + ((ch >> 6) % 64);
            return 224 + ((ch >> 12) % 16);
        }

        // oh boy, we are in the surrogate-character world, read next character
        // and calculate full value before converting to UTF-8

        ch = 0x10000 + ((ch % 1024) << 10) + (in.read() % 1024);

        partialLeft = 3;
        partial[0] = 128 + (ch % 64);
        partial[1] = 128 + ((ch >> 6) % 64);
        partial[2] = 128 + ((ch >> 12) % 64);
        return 140 + ((ch >> 18) % 8);
    }

    /**
     * Closes the Stringreader.
     *
     * @exception IOException
     *         if the original Reader fails to be closed
     */
    public void close() throws IOException
    {
        in.close();
    }

    /**
     * Marks the read limit of the StringReader.
     * throws an UnsupportedOperationException.
     *
     * This method is not supported in this class.
     * It is difficult to figure how to translate the mark in bytes
     * to a mark in characters, and to return to it correctly.
     * So simply not supported in this version of the class.
     */
    public synchronized void mark(final int limit)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Resets to the beginning by resetting the wrapped Reader.
     *
     * @exception IOException
     *        if the StringReader fails to be reset
     */
    public synchronized void reset() throws IOException
    {
        in.reset();
        partialLeft = 0;
    }

    /**
     * @see InputStream#markSupported
     */
    public boolean markSupported()
    {
        return false;
    }
}
