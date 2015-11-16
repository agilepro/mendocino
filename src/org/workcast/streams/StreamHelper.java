package org.workcast.streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Quite simply: there are a number of patterns that get written over and over
 * when using streams, and so these methods are just a common colection of shortcuts
 *
 * Author: Keith Swenson Copyright: Keith Swenson, all rights reserved License:
 * This code is made available under the GNU Lesser GPL license.
 */
public class StreamHelper {

    public static void copyReaderToWriter(Reader r, Writer w) throws Exception {
        char[] buf = new char[1000];
        int amt = r.read(buf, 0, 1000);
        while (amt>0) {
            w.write(buf, 0, amt);
            amt = r.read(buf, 0, 1000);
        }
        w.flush();
    }

    public static void copyInputToOutput(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[1000];
        int amt = is.read(buf, 0, 1000);
        while (amt>0) {
            os.write(buf, 0, amt);
            amt = is.read(buf, 0, 1000);
        }
        os.flush();
    }

    public static void copyFileToWriter(File file, Writer w,
            String fileEncoding) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, fileEncoding);

        copyReaderToWriter(isr, w);
        isr.close();
        fis.close();
    }

    public static void copyUTF8FileToWriter(File file, Writer w) throws Exception {
        copyFileToWriter(file, w, "UTF-8");
    }

    public static void streamFileToOutput(File file, OutputStream os) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        copyInputToOutput(fis, os);
        fis.close();
    }

    public static void copyStreamToFile(InputStream is, File file) throws Exception {

        File folder = file.getParentFile();
        File tempFile = new File(folder, "~"+file.getName()+".tmp~");

        if (tempFile.exists()) {
            tempFile.delete();
        }

        FileOutputStream fos = new FileOutputStream(tempFile);

        copyInputToOutput(is, fos);
        fos.close();

        if (file.exists()) {
            file.delete();
        }
        tempFile.renameTo(file);
    }

    public static void copyReaderToUTF8File(Reader r, File file) throws Exception {
        copyReaderToFile(r,file,"UTF-8");
    }

    public static void copyReaderToFile(Reader r, File file, String encoding) throws Exception {

        File folder = file.getParentFile();
        File tempFile = new File(folder, "~"+file.getName()+".tmp~");

        if (tempFile.exists()) {
            tempFile.delete();
        }

        FileOutputStream fos = new FileOutputStream(tempFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);

        copyReaderToWriter(r, osw);
        fos.close();

        if (file.exists()) {
            file.delete();
        }
        tempFile.renameTo(file);
    }



}
