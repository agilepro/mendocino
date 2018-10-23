package com.purplehillsbooks.json;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

/**
 * <p>For use when you have a file being shared across a cluster of servers in order to assure
 * that only one node of the cluster has access to the file at a time.</p>
 *
 * <p>This is a replacement for another class ClusterJSONFile which worked for locking the file
 * but did NOT allow threads to lock each other out of the file.  This approach is different in
 * that you have one shared object that represents the file, and you can use a synchronized
 * critical section to keep threads from stepping on each other.  The underlying lock protocol
 * is the same and so the classes are compatible on the operational side.</p>
 *
 * <p>The object that represents the file allows you to lock a proxy file it before you read,
 * replace or rename the protected file, and then unlock after you write, offering full
 * file lock concurrency control across read and update.  The standard file lock does not work
 * because the JSONObject writes to a temporary file before the old file is deleted.  Only when
 * the temporary file is fully committed to the disk, the old file deleted, and the temporary file
 * is renamed.</p>
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
 * <p>This clearly only works when all the code uses the same approach.  Use this class in a
 * cluster and it will prevent one node from writing over the changes of another node.  It is kind of
 * heavy handed.  Clearly if you are doing high performance you should use a database, but this approach
 * is suitable when you have files that are read a modest amount and updated infrequently.</p>
 *
 * <h1>USAGE - update existing file</h1>
 *
 * <pre>
 * File myFile = new File("c:/a/b/c/file.json");
 * LockableJSONFile ljf = LockableJSONFile.getSurrogate(myFile);   //retrieve
 * synchronized (ljf) {
 *     try {
 *         ljf.lock();
 *         JSONObject jo = ljf.readTarget();       //read
 *         ...                                     //exclusive actions while locked
 *         ljf.writeTarget(jo);                    //write
 *     }
 *     finally {
 *         ljf.unlock();
 *     }
 * }
 * ljf.free();
 * </pre>
 *
 * <p>Get the lockable file just before the use.   In order to prevent overlapped reads and writes
 * do the file operations within a synchronized block of Java code, using the lockable file as the
 * synchronization object.</p>
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
 * LockableJSONFile ljf = LockableJSONFile.getSurrogate(myFile);   //declare
 * JSONObject jo = new JSONObject();                  //create JSON contents
 * synchronized(ljf) {
 *     try {
 *         ljf.lock();
 *         ljf.writeTarget(jo);                       //create the file
 *     }
 *     finally {
 *         ljf.unlock();
 *     }
 * }
 * </pre>
 *
 * <p>The file is not locked guaranteed to be locked after the call, but if an
 * exception is thrown in the middle, it might be in a locked state, so be sure
 * to call unlock in a finally block.  If you want to read and update the new file, use the regular readAndLock
 * method to assure that this process has exclusive access, just like normal.
 *
 * <h1>USAGE - exceptions</h1>
 *
 * <pre>
 * File myFile = new File("c:/a/b/c/file.json");
 * LockableJSONFile ljf = LockableJSONFile.getSurrogate(myFile);   //declare
 * synchronized (ljf) {
 *     try {
 *         ljf.lock();
 *         JSONObject jo = ljf.readTarget();              //read and lock
 *         ...                                            //exclusive actions while locked
 *         ...                                            //after all this is done, only then:
 *         ljf.writeTarget(jo);                       v   //write and release lock
 *     }
 *     catch (Exception e) {
 *         throw new Exception("Unable to ... (details about goals of this method)", e);
 *     }
 *     finally {
 *         ljf.unlock();                                  //unlock WITHOUT writing content
 *     }
 * }
 * ljf.free();
 * </pre>
 *
 * <p>It is important to assure that if you lock the file, you also unlock it.
 * If you have multi-threading, then do everything in a synchronized block.
 * Exceptions can always occur, so do everything in a try block, and put the
 * unlock in a finally block.
 * You must lock before you do anything, even just checking existence.
 * It is OK to call unlock redundant times, the unnecessary unlocks are ignored,
 * however following the pattern of the finally block virtually eliminates this possibility.
 * If you hit an error, you generally don't want to write out the file with an undetermined
 * amount of update to the content.  Unless you know how to 'fix' the problem, you
 * generally want to leave the file untouched, but you want to clear the file lock
 * so that other threads have access.  The exception might not be the fault of the
 * file itself, and it may not be a problem next time you read it.</p>
 *
 *
 * <p>RQ High Reliability Option</p>
 *
 * <p>We have a problem at a customer where they are using a very unreliable
 * file system.  This is causing errors.  The problem is that when a file is
 * written, and unlocked by one system, the other system gets the lock, but is
 * unable to read the file.   Sometimes because the file is locked and unable
 * to be accessed. Other times the temp file can not be renamed for some reason.</p>
 *
 * <p>The strategy to avoid problem is:</p>
 * <ol>
 * <li>When getting the locked file, also check that the target file exist.  Wait
 *    for it in increments of 20ms for up to 1 second for it to appear.</li>
 *
 * <li>When reading the file, wait for the file to exist before reading it.</li>
 *
 * <li>If there is a failure read the file, wait 50ms and try again, up to 5 times.</li>
 *
 * <li>When writing a file, it there is a failure, wait 50ms and try again, up to 5 times.</li>
 *
 * <li>After writing, wait for the file to exist.</li>
 * </ol>
 */
public class LockableJSONFile {

    private File target;
    private Path targetPath;
    private File lockFile;
    private long lastUseTime = 0;
    private RandomAccessFile lockAccessFile = null;
    private RandomAccessFile lockAccessFile2 = null;
    private FileLock lock = null;
    private FileLock lock2 = null;

    private static Hashtable<String, LockableJSONFile> surrogates = new Hashtable<String, LockableJSONFile>();

    private LockableJSONFile(File targetFile) throws Exception {
        //private constructor
        target     = targetFile;
        targetPath = Paths.get(targetFile.toString());

        lockFile = new File(target.getParent(), target.getName() + "#LOCK");
        if (!lockFile.exists()) {
            //this will leave these lock file around ... but there is no harm done
            lockFile.createNewFile();
        }
    }

    /**
     * Get a lock file surrogate object, that is an object that represents
     * the file being locked / read / written.
     */
    static public synchronized LockableJSONFile getSurrogate(File targetFile) throws Exception {
        try {
            String canPath = targetFile.getCanonicalPath();

            LockableJSONFile oneSurr = surrogates.get(canPath);
            if (oneSurr==null) {
                oneSurr = new LockableJSONFile(targetFile);
                surrogates.put(canPath, oneSurr);
            }
            oneSurr.touch();
            cleanOutTable();
            return oneSurr;
        }
        catch (Exception e) {
            throw new Exception("Unable to get surrogate file for "+targetFile, e);
        }
    }

    private void touch() {
        lastUseTime = System.currentTimeMillis();
    }

    /**
     * remove old entries from the hash table that have not been touched in the last hour.
     * Call this private static method ONLY with a synchronized static method
     */
    private static void cleanOutTable() {
        Hashtable<String, LockableJSONFile> newTable = new Hashtable<String, LockableJSONFile>();
        long tooOld = System.currentTimeMillis() - 3600000;
        for (String key : surrogates.keySet()) {
            LockableJSONFile ljf = surrogates.get(key);
            if (ljf.lastUseTime>tooOld) {
                newTable.put(key, ljf);
            }
        }
        surrogates = newTable;
    }



    /**
     * This is the basic lock command and wait until the target file is there.
     * It will block until the lock on the LOCK file can be gotten.
     * Then it will check if the target file exists, and wait up to 1 second for it to appear.
     * You should only call lock once, before calling unlock.
     *
     * It waits for the file to appear because on some shared file systems, the lock signal will
     * arrive long before the file name change from the last update will propogate.   That means
     * that for a long period of time (sometimes more than 100ms) it will appear like there is no
     * target file in the file system.  To prevent errors we wait for it here.
     * After 1 second it will continue without error assuming that this is the
     * extremely rare initialization case.  That means the very first time you create
     * a file you will encounter a 1 second delay but we assume that is a rare case that is
     * not seen in normal operation.
     */
    public void lock() throws Exception {
        if (lock != null || lockAccessFile != null) {
            throw new Exception("Seem to be locking a second time before unlocking the last time: "+target);
        }
        lockAccessFile = new RandomAccessFile(lockFile, "rw");
        FileChannel lockChannel = lockAccessFile.getChannel();
        lock = lockChannel.lock();

        waitUntilItExists();

        getSecondLock();
    }

    private void getSecondLock() throws Exception {
        try {
            if (lockAccessFile2!=null) {
                throw new Exception("Did second lock get called twice?");
            }
            lockAccessFile2 = new RandomAccessFile(target, "rw");
            FileChannel lockChannel2 = lockAccessFile2.getChannel();
            lock2 = lockChannel2.lock();
            //System.out.println("+++ getSecondLock on "+target);
        }
        catch (Exception e) {
            throw new Exception("Failed to get second lock, the one on : "+target, e);
        }
    }
    private void releaseSecondLock() throws Exception {
        try {
            if (lock2 != null) {
                lock2.release();
                lock2 = null;
            }
            if (lockAccessFile2 != null) {
                lockAccessFile2.close();
                lockAccessFile2 = null;
            }
            //System.out.println("+++ releaseSecondLock on "+target);
        }
        catch (Exception e) {
            throw new Exception("Failed to release second lock, the one on : "+target, e);
        }
    }


    private void waitUntilItExists() throws Exception {
        //wait for the file to appear in the file system from the last write/rename
        int count = 0;
        while (!exists() && ++count<50) {
            //there are some file systems that are slow about letting the programs know about files.
            //we have found this in stress scenarios that a file just written out, the lock can be released
            //somewhat before the file appears to the program.   So wait 100 ms to see if it appears
            //in that time.  If the file really is not there -- e.g. the first time you look for a file and
            //expect to create it, will result in a delay of up to 1 second.  Otherwise give up.
            //For normal files, they should normally exist, so delaying only on the create case should not be a problem.
            //I don't like this.  I don't.   I don't.
            Thread.sleep(20);
        }
        if (count>5 && exists()) {
            System.out.println("SLOW FILE SYSTEM: file appeared "+ (count*20) + "ms after expected: "+target);
        }
    }


    /**
     * Use this to unlock the file when you don't need to update the contents.
     * This method is particularly useful in 'finally' statements, where an
     * error has occurred, and you simply need to make sure that the lock is
     * released, while leaving the file unchanged.
     */
    public void unlock() throws Exception {
        releaseSecondLock();

        if (lock != null) {
            lock.release();
            lock = null;
        }
        if (lockAccessFile != null) {
            lockAccessFile.close();
            lockAccessFile = null;
        }
    }

    /**
     * Tells whether the calling program/thead is holding the lock.  It does not tell you whether
     * any other thread or program is holding the lock at the current moment.
     */
    public boolean isLocked() {
        return (lock!=null && lock.isValid());
    }



    /**
     * <p>Test to see if the target file exists.  Generally, you need to assure that the file
     * exists, and to initialize with an empty JSON structure if it does not exist.
     * So use this to test whether you need to call initializeFile.</p>
     * <p>Note: since the smallest JSON file has two characters (just the open and close brace)
     * this method will return FALSE when an empty file exists at that name.  The file must be
     * 2 byte or longer to be existing according to this routine.</p>
     * <p>Note2: file must be locked BEFORE calling this to be sure that it does not change
     * in the mean time.</p>
     */
    public boolean exists() throws Exception {
        //consistency check
        if (!isLocked()) {
            throw new Exception("File was not locked before checking if it exists: "+target);
        }
        return Files.exists(targetPath) && target.length()>=2;
    }


    /**
     * This will update the contents of the file on disk, without changing
     * the lock state.
     *
     * The file must be locked before calling this.
     */
    public void writeTarget(JSONObject newContent) throws Exception {
        //consistency check
        if (!isLocked()) {
            throw new Exception("File was not locked before calling writeTarget: "+target);
        }
        releaseSecondLock();

        int retryCount = 0;
        while (retryCount++ < 5) {
            try {
                newContent.writeToFile(target);
                waitUntilItExists();
                return;
            }
            catch (Exception e) {
                System.out.println();
                JSONException.traceException(e, "LockableJSONFile.writeTarget:  Try #"+retryCount+" writing file got an exception.");
                Thread.sleep(50);
            }
        }
        //check and make sure it exists!
        if (!exists()) {
            throw new Exception("LockableJSONFile.initializeFile tried to create file, but it did not get created: "+target);
        }
        getSecondLock();
    }

    /**
     * Read and return the contents of the file.
     * You must lock the file before calling this.
     */
    public JSONObject readTarget() throws Exception {
        //consistency check
        if (!isLocked()) {
            throw new Exception("File was not locked before calling readTarget: "+target);
        }
        releaseSecondLock();

        int retryCount = 0;
        Exception lastException = null;
        while (retryCount++ < 5) {
            try {
                waitUntilItExists();
                JSONObject result = JSONObject.readFromFile(target);
                getSecondLock();
                return result;
            }
            catch (Exception e) {
                lastException = e;
                System.out.println();
                JSONException.traceException(e, "LockableJSONFile.writeTarget:  Try #"+retryCount+" writing file got an exception.");
                Thread.sleep(50);
            }
        }
        getSecondLock();
        throw new Exception("LockableJSONFile.writeTarget:  Failed "+retryCount+" times to write file "+target, lastException);
    }

    /**
     * Read and return the contents of the file if it exists.
     * If it does not exist, the file will be created with an empty JS object.
     * You must lock the file before calling this.
     *
     * Note: this method will first wait up to 1 second to see
     * if the file exists, and then it will initialize the file
     * to an empty object if not found, so that next time
     * it will see and read the file quickly.
     * This method should only be used on files that almost always exist
     * and should be initialized if they don't exist.
     */
    public JSONObject readTargetIfExists() throws Exception {
        //consistency check
        if (!isLocked()) {
            throw new Exception("File was not locked before calling readTargetIfExists: "+target);
        }
        releaseSecondLock();
        try {
            waitUntilItExists();
            if (!exists()) {
                //actually initialize the file here so that next time we can avoid
                //the 1 second delay waiting for it to appear.
                //This should be rare, so print a trace statement about it
                writeTarget(new JSONObject());
                //System.out.println("LockableJSONFile: initialized file to empty JSON object: "+target);
            }
            return JSONObject.readFromFile(target);
        }
        catch (Exception e) {
            JSONException.traceException(e, "LockableJSONFile.readTargetIfExists FAILURE: "+target);
            throw new Exception("readTargetIfExists failed for "+target, e);
        }
        finally {
            getSecondLock();
        }
    }


    /**
     * The easiest way to safely read a file.
     * Use this to read the file if you are NOT going to update it.
     * It locks the file briefly, reads it, and guarantees that the
     * file is unlocked at the end.
     *
     * It is synchronized so you don't need to do a synchronize on the object.
     *
     * This is the simplest way to safely read a shared file.
     */
    public synchronized JSONObject lockReadUnlock() throws Exception {
        try {
            lock();
            return readTarget();
        }
        finally {
            unlock();
        }
    }

}
