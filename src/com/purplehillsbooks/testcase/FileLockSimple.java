package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import com.purplehillsbooks.json.JSONException;

public class FileLockSimple {
    
    
	/**
	 * Application Main
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
        File testFile = new File("./LockFileTest.random");
        File lockFile = new File("./LockFileTest.random.LOCK");
        Random r = new Random(System.currentTimeMillis());
        
        //initialize
        try {
            if (!testFile.exists()) {
                OutputStream os = new FileOutputStream(testFile);
                for (int i=0; i<1000; i++) {
                    os.write( (char)(65+r.nextInt(26)));
                }
                os.close();
            }
        }
        catch (Exception e) {
            JSONException.traceException(e, "FileLockSimple.main");
        }
        
		while (true) {
	        File tempFile = new File("./LockFileTest.random~TMP."+System.currentTimeMillis());
		    try {
                RandomAccessFile lockAccessFile = new RandomAccessFile(lockFile, "rw");
                FileChannel lockChannel = lockAccessFile.getChannel();
                FileLock lock = lockChannel.lock();
                try {
                    if (!testFile.exists()) {
                        throw new Exception("Test file NOT FOUND: "+testFile);
                    }
                    //this needed to assure you have other programs waiting on the lock before release
                    Thread.sleep(20);
                    
                    //read the file
                    InputStream is = new FileInputStream(testFile);
                    int ch = is.read();
                    while (ch>=0) {
                        ch = is.read();
                    }
                    is.close();
                    
                    //write the file
                    OutputStream os = new FileOutputStream(tempFile);
                    for (int i=0; i<1000; i++) {
                        os.write( (char)(65+r.nextInt(26)));
                    }
                    os.close();
                    
                    testFile.delete();
                    
                    Path sourcePath      = Paths.get(tempFile.toString());
                    Path destinationPath = Paths.get(testFile.toString());
                    Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                finally {
                    lock.release();
                    lockAccessFile.close();
                }
                
                if (!testFile.exists()) {
                    throw new Exception("Test file was not written/moved correctly: "+testFile);
                }
                System.out.print(".");
		    } 
		    catch (Exception e) {
		        JSONException.traceException(e, "FileLockSimple main routine");
    		}
		}
	}
	

}
