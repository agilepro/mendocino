package com.purplehillsbooks.json;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * @deprecated use LockableJSONFile instead
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
     * @deprecated
     */
    public boolean exists() {
        //A JSON file has to have at least two characters in it:  {}
        //Sometimes empty files are created that cause parsing errors
        //so it is simple enough to test the file length here.  If it is 
        //empty it is the same as not existing.
        return target.exists() && target.length()>=2;
    }

    /**
     * @deprecated
     */
    public void initializeFile(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
        if (!exists()) {
            throw new Exception("ClusterJSONFile.initializeFile tried to create file, but it does not exist: "+target);
        }
    }

    /**
     * @deprecated
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
     * @deprecated
     */
    public JSONObject readWithoutLock() throws Exception {
        if (!target.exists()) {
            throw new Exception("File does not exist.  File must be initialized before reading: "+target);
        }
        return JSONObject.readFromFile(target);
    }

    /**
     * @deprecated
     */
    public boolean isLocked() {
        return (lock!=null && lock.isValid());
    }

    /**
     * @deprecated
     */
    public void writeWithoutUnlock(JSONObject newContent) throws Exception {
        newContent.writeToFile(target);
    }

    /**
     * @deprecated
     */
    public void writeAndUnlock(JSONObject newContent) throws Exception {
        if (lock == null || lockAccessFile == null) {
            throw new Exception("Attempt to unlock a file that was not locked or already unlocked."+target);
        }
        newContent.writeToFile(target);
        unlock();
    }

    /**
     * @deprecated
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
