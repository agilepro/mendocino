package com.purplehillsbooks.json;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * <p>For use when you have a file being shared across a cluster of servers in order to assure
 * that only one node of the cluster has access to the file at a time.</p>
 * 
 * <p>This file allows you to lock it before you read, and then unlock after you write, offering full
 * file lock concurrency control across read and update.  The stadnard file lock does not work
 * because the JSONObject writes to a temporary file before the old file is deleted.  Only when 
 * the temporary file is fully committed to the disk, the old file deleted, and the temporary file
 * is renamed.  Thus locking the actual json file does not work.</p>
 *
 * <p>Instead, it locks a symbolic name, based on the original file passed in.</p>
 * <pre>
 * Actual JSON file:     c:/a/b/c/file.json
 * Symbol is locked:     c:/a/b/c/file.json#LOCK
 * </pre>
 * 
 * <p>While the file is locked by one node, the other nodes trying to read the file are blocked.
 * When one node unlocks the file (after writing) then any other waiting node is allowed to proceed.</p>
 * 
 * <p>This clearly only locks and protects against other code using this same class.  Use this class in a
 * cluster and it will prevent one node from writing over the changes of another node.  It is kind of
 * heavy handed.  Clearly if you are doing high performance you should use a database, but this approach
 * is suitable when you have files that are read a modest amount and updated infrequently.</p>
 *
 * <h1>USAGE - update existing file</h1>
 * 
 * <pre>
 * File myFile = new File("c:/a/b/c/file.json");
 * ClusterJSONFile cjf = new ClusterJSONFile(myFile);   //declare
 * JSONObject jo = cjf.lockAndRead();                   //read and lock
 * ...                                                  //exclusive actions while locked
 * cjf.writeAndUnlock(jo);                              //write and release lock
 * </pre>
 * 
 * <p>The expectation is that the underlying JSON write mechanism will write to a new file, and then delete
 * the original file and rename the new file to the real name.  Because of this approach you are generally
 * safe reading a file without a lock if you are NOT going to update the file.  But if you plan to
 * write to the file, you must get the lock before you read the file.</p>
 *
 * <h1>USAGE - creating a file</h1>
 * 
 * <pre>
 * File myFile = new File("c:/a/b/c/file.json");
 * ClusterJSONFile cjf = new ClusterJSONFile(myFile);   //declare
 * JSONObject jo = new JSONObject();                    //create JSON contents
 * cjf.initializeFile(jo);                              //create the file
 * </pre>
 * 
 * <p>The file is not locked.  If you want to read and update the new file, use the regular readAndLock
 * method to assure that this process has exclusive access, just like normal.
 * 
 * <h1>USAGE - exceptions</h1>
 * 
 * <pre>
 * File myFile = new File("c:/a/b/c/file.json");
 * ClusterJSONFile cjf = new ClusterJSONFile(myFile);   //declare
 * try {
 *     JSONObject jo = cjf.lockAndRead();               //read and lock
 *     ...                                              //exclusive actions while locked
 *     ...                                              //after all this is done, only then:
 *     cjf.writeAndUnlock(jo);                          //write and release lock
 * }
 * catch (Exception e) {
 *     cjf.unlock();                                    //unlock WITHOUT writing content
 *     throw new Exception("Unable to update "+myFile, e);
 * }
 * </pre>
 * 
 * <p>It is important to assure that if you lock the file, you also unlock it.
 * If you hit an error, you generally don't want to write out the file with an undetermined
 * amount of update to the content.  Unless you know how to 'fix' the problem, you 
 * generally want to leave the file untouched, but you want to clear the file lock
 * so that other threads have access.  The exception might not be the fault of the 
 * file itself, and it may not be a problem next time you read it.</p>
 */
public class ClusterJSONFile {

    File target;
    File lockFile;
    InputStream stream;
    boolean isLocked = false;
    RandomAccessFile lockAccessFile = null;
    FileLock lock = null;

    public ClusterJSONFile(File targetFile) throws Exception {
        target = targetFile;
        lockFile = new File(target.getParent(), target.getName() + "#LOCK");
        if (!lockFile.exists()) {
            //this will leave these lock file around ... but there is no harm done
            lockFile.createNewFile();
        }
    }

    /**
     * <p>Test to see if the target file exists.  Generally, you need to assure that the file
     * exists, and to initialize with an empty JSON structure if it does not exist.
     * So use this to test whether you need to call initializeFile.</p>
     * <p>Note: since the smallest JSON file has two characters (just the open and close brace)
     * this method will return FALSE when an empty file exists at that name.  The file must be 
     * 2 byte or longer to be existing according to this routine.</p>
     */
    public boolean exists() {
        //A JSON file has to have at least two characters in it:  {}
        //Sometimes empty files are created that cause parsing errors
        //so it is simple enough to test the file length here.  If it is 
        //empty it is the same as not existing.
        return target.exists() && target.length()>=2;
    }

    /**
     * Pass a JSON object to create the file.  Does not lock the file.
     */
    public void initializeFile(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
        if (!exists()) {
            throw new Exception("ClusterJSONFile.initializeFile tried to create file, but it does not exist: "+target);
        }
    }

    /**
     * <p>Use this method to read a file, if you are planning to update the file.
     * If you do, be sure to unlock the file using writeAndUnlock.</p>
     * 
     * <p>If during processing you run into an error, and you don't want to write the 
     * file, be sure to unlock with unlock.</p>
     */
    public JSONObject lockAndRead() throws Exception {
        if (!target.exists()) {
            throw new Exception("File does not exist.  File must be initialized before reading: "+target);
        }
        if (lock != null || lockAccessFile != null) {
            throw new Exception("Seem to be locking a second time before unlocking the last time: "+target);
        }
        lockAccessFile = new RandomAccessFile(lockFile, "rw");
        FileChannel lockChannel = lockAccessFile.getChannel();
        lock = lockChannel.lock();
        return readWithoutLock();
    }

    /**
     * Use this to read the file if you are NOT going to update it.
     * Gives you the contents, but does not lock the file.
     */
    public JSONObject readWithoutLock() throws Exception {
        if (!target.exists()) {
            throw new Exception("File does not exist.  File must be initialized before reading: "+target);
        }
        return JSONObject.readFromFile(target);
    }

    /**
     * Tells whether the calling program/thead is holding the lock.  It does not tell you whether
     * any other thread or program is holding the lock at the current moment.
     */
    public boolean isLocked() {
        return (lock!=null && lock.isValid());
    }

    /**
     * This will update the contents of the file on disk, but retains the lock.
     * This would be used when you are in a long running loop, and you want to update the 
     * file to preserve the partial results for every pass through the loop.
     * The host program might be shut down at any time, and you don't want to have
     * to start the loop all over from the beginning, so the partial results are needed.
     * But in that loop you want to hold on to the lock after writing because you are
     * not completely done.
     */
    public void writeWithoutUnlock(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
    }

    /**
     * This is the standard way to update the file when you are done.
     * Will write the contents safely, and then unlock to allow the other
     * processes to have a chance at the file.
     */
    public void writeAndUnlock(JSONObject newContent) throws Exception {
        if (lock == null || lockAccessFile == null) {
            throw new Exception("Attempt to unlock a file that was not locked or already unlocked."+target);
        }
        newContent.writeToFile(target);
        unlock();
    }

    /**
     * Use this to unlock the file when you don't need to update the contents.
     * This method is particularly useful in 'finally' statements, where an 
     * error has occurred, and you simply need to make sure that the lock is 
     * released, while leaving the file unchanged.
     */
    public void unlock() throws Exception {
        if (lock != null) {
            lock.release();
            lock = null;
        }
        if (lockAccessFile != null) {
            lockAccessFile.close();
            lockAccessFile = null;
        }
    }

}
