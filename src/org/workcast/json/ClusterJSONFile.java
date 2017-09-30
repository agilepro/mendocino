package org.workcast.json;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.workcast.json.JSONObject;

/**
 * For use when you have a file being shared across a cluster of servers in order to assure
 * that only one node of the cluster has access to the file at a time.
 * 
 * This file allows you to lock it before you read, and then unlock after you write, offering full
 * file lock concurrency control across read and update.  The stadnard file lock does not work
 * because the JSONObject writes to a temporary file before the old file is deleted.  Only when 
 * the temporary file is fully committed to the disk, the old file deleted, and the temporary file
 * is renamed.  Thus locking the actual json file does not work.
 *
 * Instead, it locks a symbolic name, based on the original file passed in.
 * Actual JSON file:     c:/a/b/c/file.json
 * Symbol is locked:     c:/a/b/c/file.json#LOCK
 *
 * While the file is locked by one node, the other nodes trying to read the file are blocked.
 * When one node unlocks the file (after writing) then any other waiting node is allowed to proceed.
 * 
 * This clearly only locks and protects against other code using this same class.  Use this class in a
 * cluster and it will prevent one node from writing over the changes of another node.  It is kind of
 * heavy handed.  Clearly if you are doing high performance you should use a database, but this approach
 * is suitable when you have files that are read a modest amount and updated infrequently.
 *
 * USAGE:
 * 
 * File myFile = new File("c:/a/b/c/file.json");
 * ClusterJSONFile cjf = new ClusterJSONFile(myFile);   //declare
 * JSONObject jo = cjf.lockAndRead();                   //read and lock
 * ...                                                  //exclusive actions while locked
 * cjf.writeAndUnlock(jo);                              //write and release lock
 *
 * The expectation is that the underlying JSON write mechanism will write to a new file, and then delete
 * the original file and rename the new file to the real name.  Because of this approach you are generally
 * safe reading a file without a lock if you are NOT going to update the file.  But if you plan to
 * write to the file, you must get the lock before you read the file.
 *
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
     * Test to see if the target file exists.  Generally, you need to assure that the file
     * exists, and to initialize with an empty JSON structure if it does not exist.
     * So use this to test whether you need to call initializeFile.
     */
    public boolean exists() {
        return target.exists();
    }

    /**
     * Pass a JSON object to write to the file
     */
    public void initializeFile(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
        if (!exists()) {
            //TODO: this is debug code
            throw new Exception("Something wrong ... just created file, but it exists not."+target);
        }
    }

    /**
     * Use this method to read a file, if you are planning to update the file.
     * If you do, be sure to unlock the file using writeAndUnlock.
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

    public JSONObject readWithoutLock() throws Exception {
        if (!target.exists()) {
            throw new Exception("File does not exist.  File must be initialized before reading: "+target);
        }
        return JSONObject.readFromFile(target);
    }

    public boolean isLocked() {
        return (lock!=null && lock.isValid());
    }

    public void writeWithoutUnlock(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
    }

    public void writeAndUnlock(JSONObject newContent) throws Exception {
        if (lock == null || lockAccessFile == null) {
            throw new Exception("Attempt to unlock a file that was not locked or already unlocked."+target);
        }
        newContent.writeToFile(target);
        unlock();
    }

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
