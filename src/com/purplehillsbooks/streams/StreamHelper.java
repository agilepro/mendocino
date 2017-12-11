package com.purplehillsbooks.streams;

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

    /**
    * copyReaderToWriter will read character from the Reader, and send them to the
    * Writer until the Reader is exhausted.
    */
    public static void copyReaderToWriter(Reader r, Writer w) throws Exception {
        char[] buf = new char[1000];
        int amt = r.read(buf, 0, 1000);
        while (amt>0) {
            w.write(buf, 0, amt);
            amt = r.read(buf, 0, 1000);
        }
        w.flush();
    }

    /**
    * copyInputToOutput will read bytes from the input stream, and send them to the
    * output stream until the input stream is exhausted.
    */
    public static void copyInputToOutput(InputStream is, OutputStream os) throws Exception {
        byte[] buf = new byte[1000];
        int amt = is.read(buf, 0, 1000);
        while (amt>0) {
            os.write(buf, 0, amt);
            amt = is.read(buf, 0, 1000);
        }
        os.flush();
    }

    /**
    * Given an output stream, copyFileToOutput will read the contents of a file
    * and stream those contents to the output stream until the entire file is sent.
    */
    public static void copyFileToOutput(File file, OutputStream os) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        copyInputToOutput(fis, os);
        fis.close();
    }

    /**
    * Given an output Writer object, copyFileToWriter will read the contents of a file
    * converting bytes to characters according to specified encoding, and stream those
    * characters to the Reader until the entire file is sent.
    */
    public static void copyFileToWriter(File file, Writer w,
            String fileEncoding) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, fileEncoding);

        copyReaderToWriter(isr, w);
        isr.close();
        fis.close();
    }

    /**
    * Given an output Writer object, copyUTF8FileToWriter will read the contents of a file
    * converting bytes to characters according to UTF-8 encoding, and stream those
    * characters to the Reader until the entire file is sent.
    */
    public static void copyUTF8FileToWriter(File file, Writer w) throws Exception {
        copyFileToWriter(file, w, "UTF-8");
    }

    /**
    * copyStreamToFile will read the input stream to the end, and write the
    * contents to the designated file.  Bytes are read and bytes are written
    * the resulting file is in whatever encoding that the source stream is.
    *
    * The contents are written first to a temporary file, and then that file is
    * renamed to destination file.  If the process is interrupted in the middle
    * of the copy, the temporary file might remain.
    */
    public static void copyStreamToFile(InputStream is, File file) throws Exception {
        try {
            File folder = file.getParentFile();
            File tempFile = new File(folder, "~"+file.getName()+".tmp~");
    
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    throw new Exception("Unable to delete the previously existing temporary file: "+tempFile);
                }
            }
    
            FileOutputStream fos = new FileOutputStream(tempFile);
    
            copyInputToOutput(is, fos);
            fos.close();
    
            if (file.exists()) {
                if (!file.delete()) {
                    throw new Exception("Unable to delete the previous output file: "+file
                            +".  Is the input stream that was used to read the file closed?");
                }
            }
            tempFile.renameTo(file);
        }
        catch (Exception e) {
            throw new Exception("Unable to copy the stream to the file: "+ file, e);
        }
    }

    /**
    * copyReaderToUTF8File will read the input Reader to the end, and write the
    * contents to the designated file using UTF-8 encoding, overwriting any
    * existing file that might be there.
    *
    * The contents are written first to a temporary file, and then that file is
    * renamed to destination file.  If the process is interrupted in the middle
    * of the copy, the temporary file might remain.
    */
    public static void copyReaderToUTF8File(Reader r, File file) throws Exception {
        copyReaderToFile(r,file,"UTF-8");
    }

    /**
    * copyReaderToUTF8File will read the input Reader to the end, and write the
    * contents to the designated file using specified encoding, overwriting any
    * existing file that might be there.
    *
    * The contents are written first to a temporary file, and then that file is
    * renamed to destination file.  If the process is interrupted in the middle
    * of the copy, the temporary file might remain.
    */
    public static void copyReaderToFile(Reader r, File file, String encoding) throws Exception {
        try {
            File folder = file.getParentFile();
            File tempFile = new File(folder, "~"+file.getName()+".tmp~");
    
            if (tempFile.exists()) {
                if (!tempFile.delete()) {
                    throw new Exception("Unable to delete the previously existing temporary file: "+tempFile);
                }
            }
    
            FileOutputStream fos = new FileOutputStream(tempFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
    
            copyReaderToWriter(r, osw);
            fos.close();
    
            if (file.exists()) {
                if (!file.delete()) {
                    throw new Exception("Unable to delete the previous output file: "+file
                            +".  Is the input stream that was used to read the file closed?");
                }
            }
            tempFile.renameTo(file);
        }
        catch (Exception e) {
            throw new Exception("Unable to copy the Reader data to the file: "+file, e);
        }
    }

    /**
    * This is a convenience routine to copy the contents of one file to another file.
    * It does do by reading all the bytes from one, and writing the butes to another.
    * There is no restriction on where those files might exist, just as long as they
    * can be opened and read/writen to.
    *
    * The contents are written first to a temporary file, and then that file is
    * renamed to destination file.  If the process is interrupted in the middle
    * of the copy, the temporary file might remain.
    */
    public static void copyFileToFile(File inFile, File outFile) throws Exception {
        if (!inFile.exists()) {
            throw new Exception("Can not copy a file that does not exist: "+inFile);
        }
        FileInputStream fis = new FileInputStream(inFile);
        copyStreamToFile(fis, outFile);
        fis.close();
    }


}
