package com.purplehillsbooks.streams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * You always need a Base64 encoder and decoder, but for some reason Java does
 * not introduce it until version 1.8, and even so there are many incompatible
 * libraries out there. This one does the decoding without any dependence upon
 * other classes, nor on a complex bloated object class hierarchy that is the
 * grandiose scheme of some programmer to impress others with. This
 * implementation is straightforward and simple to use.
 *
 * To encode: simply call "encode" with the array of bytes that you want
 * encoded, and an encoded string is returned.
 *
 * To decode: pass the string into "decode" and a byte array is returned.
 *
 * That is all you need, and I am constantly amazed at how programmers can make
 * something as simple as this so complicated that you have to read a manual to
 * be able to use it.
 */
public class Base64 {

    /**
     * Encodes by byte array. When byte arrays specified for the argument are
     * null or 0 bytes, the character string of the return value becomes empty
     * string.
     *
     * @param bytes a Byte array to be encoded @return String Encoded Character
     * string
     */
    public static String encode(byte[] bytes) {
        if (null == bytes || 0 == bytes.length) {
            return "";
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(
                bytes.length * 150 / 100);

        int max = bytes.length;
        for (int i = 0; i < bytes.length; i += 3) {
            bout.write(create3byte(bytes, i, max), 0, 4);
            // add CRLF at each 76 characters
            if (54 == i % 57) {
                bout.write('\r');
                bout.write('\n');
            }
        }

        try {
            //none of the bytes are greater than 127 and so the use of
            //UTF-8 here makes no difference
            return bout.toString("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /*
     * Converts three bytes into four bytes. Base64 delimits the bit array in
     * three bytes by six bits, and creates four arrays of six bits. Here, 0
     * padding every two head bits of six bits for each is done. In addition,
     * the created each byte is converted into the character-code of Base64. "="
     * is supplemented to an insufficient six bit location when the value of
     * offset+2 exceeds max.
     */
    private static byte[] create3byte(byte[] bytes, int offset, int max) {
        byte rr[] = new byte[4];
        int num = 0x00000000;
        num |= (bytes[offset + 0] << 16) & 0xFF0000;

        if (offset + 1 < max) {
            num |= (bytes[offset + 1] << 8) & 0xFF00;
        }
        else {
            num |= 0;
        }

        if (offset + 2 < max) {
            num |= (bytes[offset + 2] << 0) & 0x00FF;
        }
        else {
            num |= 0;
        }

        rr[0] = map((num >> 18) & 0x3F);
        rr[1] = map((num >> 12) & 0x3F);

        if (offset + 2 < max) {
            rr[2] = map((num >> 6) & 0x3F);
            rr[3] = map((num >> 0) & 0x3F);
        }
        else if (1 == (max % 3)) {
            rr[2] = (byte) '=';
            rr[3] = (byte) '=';
        }
        else if (2 == (max % 3)) {
            rr[2] = map((num >> 6) & 0x3F);
            rr[3] = (byte) '=';
        }
        return rr;
    }

    /*
     * It encodes it to the character of the Base64 form. The correspondence of
     * the numerical value and the character is taken, and ASCII code of the
     * character is set. The character string of the Base64 form is as follows.
     */
    private static byte map(int code) {
        code = code & 0x3F;
        if (code <= 25) {
            return (byte) (code - 0 + 'A');
        }
        else if (code <= 51) {
            return (byte) (code - 26 + 'a');
        }
        else if (code <= 61) {
            return (byte) (code - 52 + '0');
        }
        else if (code == 62) {
            return (byte) '+';
        }
        else {
            return (byte) '/';
        }
    }

    /**
     * Deciphers the passed encoded string. And it return the result in a byte
     * array. If the specified character string can not be deciphered Exception
     * is thrown.
     *
     * @param str Character string to be deciphered @return byte[] Byte array
     * containing the deciphered bytes @exception Exception If failed during the
     * decipherment.
     */

    public static byte[] decodeBuffer(String str) throws IOException {
        byte[] bytes = null;
        try {
            //note, properly encoded Base64 string will not have any characters
            //higher than 127, so the use of UTF-8 makes no difference here.
            bytes = str.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            //inconceivable that UTF-8 is not supported.
            return null;
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);

        byte bb[] = new byte[4];

        int index = 0;
        int bValue;

        while ((bValue = bin.read()) != -1) {
            if (bValue == '\r' || bValue == '\n' || bValue == ' ') {
                continue;
            }

            bb[index++] = (byte) bValue;

            if (index != 4) {
                continue;
            }

            byte rr[] = decode3byte(bb, 0, 4);
            bout.write(rr, 0, rr.length);

            index = 0;
        }

        if (index != 0) {
            byte rr[] = decode3byte(bb, 0, index);
            bout.write(rr, 0, rr.length);
        }
        return bout.toByteArray();
    }

    /*
     * It converts the passed byte array into byte array of three bytes (24
     * bits). Confirm and operate the length of the byte on the operated side.
     */
    private static byte[] decode3byte(byte[] bb, int offset, int max)
            throws IOException {
        int num = 0x00000000;
        int len = 0;
        for (int i = 0; i < 4; i++) {
            if (offset + i >= max || bb[offset + i] == '=') {
                if (i < 2) {
                    throw new IOException(
                            "BASE64Decoder: Incomplete BASE64 character");
                }
                else {
                    break;
                }
            }
            num |= (unmap(bb[offset + i]) << 2) << (24 - 6 * i);
            len++;
        }
        if (len < 3) {
            len = 1;
        }
        else {
            len--;
        }

        byte rr[] = new byte[len];

        for (int i = 0; i < len; i++) {
            rr[i] = (byte) (num >> (24 - 8 * i));
        }
        return rr;
    }

    /*
     * Converts the character of the Base64 form into the numerical value.
     */
    private static byte unmap(int cc) throws IOException {
        if (cc >= 'A' && cc <= 'Z') {
            return (byte) (cc - 'A');
        }
        else if (cc >= 'a' && cc <= 'z') {
            return (byte) (cc - 'a' + 26);
        }
        else if (cc >= '0' && cc <= '9') {
            return (byte) (cc - '0' + 52);
        }
        else if (cc == '+') {
            return 62;
        }
        else if (cc == '/') {
            return 63;
        }
        else {
            throw new IOException("BASE64Decoder: Illegal character:= "
                    + (char) cc);
        }
    }
}
