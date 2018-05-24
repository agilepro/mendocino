package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONException;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.json.LockableJSONFile;
import com.purplehillsbooks.xml.Mel;

/**
 * Simple test of file locking
 */
public class FileLockThread extends Thread {
    
    JSONObject config;
    File testFile;
    boolean running = true;
    long lastSetValue = 0;
    int totalTries = 0;
    int exceptionCount = 0;
    String threadName;
    long lockHoldMillis = 20;
    Random rand;
    JSONArray stats = new JSONArray();
    public Exception lastException = null;
    
    public FileLockThread(String name, JSONObject _config) throws Exception {
        config = _config;
        threadName = name;
        testFile = new File("./ConFileTest.json");
        if (config.has("testFile")) {
            testFile = new File(config.getString("testFile"));
        }
        if (config.has("lockHoldMillis")) {
            lockHoldMillis = config.getLong("lockHoldMillis");
        }
        LockableJSONFile ljf = LockableJSONFile.getSurrogate(testFile);
        rand = new Random(System.currentTimeMillis());

        synchronized(ljf) {
            try {
                ljf.lock();
                if (!ljf.exists()) {
                    JSONObject jo = new JSONObject();
                    jo.put("testVal1", "1");
                    jo.put("testVal2", "1");
                    jo.put("testVal3", "1");
                    jo.put("initialized", System.currentTimeMillis());
                    jo.put("updated", System.currentTimeMillis());
                    jo.put("thread", threadName);
                    jo.put("longString", "?");
                    ljf.writeTarget(jo);
                    System.out.println("Thread "+threadName+" initialized the file: "+testFile);
                }
                else {
                    System.out.println("Thread "+threadName+" did NOT initialized the file: "+testFile);                    
                }
                if (!ljf.exists()) {
                    //this should never happen, but bomb out if it does.
                    throw new Exception("Newly initialized file ("+testFile+") does not exist after creating it!");
                }
            }
            finally {
                ljf.unlock();
            }
        }
    }
    
    public void die() {
        running = false;
    }

    public void run() {
        System.out.println("Running Thread "+threadName);
        //report every 10 seconds;
        long reportTime = System.currentTimeMillis() + 10000;
        while (running) {
            long thisTime = System.currentTimeMillis();
            if (thisTime > reportTime) {
                System.out.println("\nThread "+threadName+" completed "+totalTries+", value is "+lastSetValue+", exceptions: "+exceptionCount);
                while (thisTime > reportTime) {
                    //usually this goes through the loop once, but occasionally someone will put a machine to sleep
                    //and when it wakes up, don't report for all the 10 second intervals that it was asleep
                    reportTime = reportTime + 10000;
                }
            }
            totalTries++;
            try {
                //we are in a fast loop doing this as fast as possible 
                if (rand.nextInt(20)!=2) {
                    incrementLocalJSON();
                }
                else {
                    checkFileDoesNotChange();
                }
            }
            catch (Exception e) {
                exceptionCount++;
                lastException = e;
                JSONException.traceException(System.out, e, "Thread "+threadName+" FAILURE on try #"+totalTries);
            }
        }
        System.out.println("Stopping Thread "+threadName);
    }   
    
    public void incrementLocalJSON() throws Exception {
        JSONObject stat = new JSONObject();
        stat.put("thread", threadName);
        long dur = 0;
        try {
            LockableJSONFile ljf = LockableJSONFile.getSurrogate(testFile);
            
            synchronized(ljf) {
                long startTime = 0;
                try {
                    ljf.lock();
                    startTime = System.currentTimeMillis();
                    if (!ljf.exists()) {
                        //need to sleep AFTER the test, but before the throw or anything else 
                        Thread.sleep(lockHoldMillis);
                        throw new Exception("Test file NOT FOUND: "+testFile);
                    }
                    Thread.sleep(lockHoldMillis);
                    
                    JSONObject newVersion = ljf.readTarget();
                    //now update them
                    lastSetValue = incrementOneValue(newVersion, "testVal1");
                    incrementOneValue(newVersion, "testVal2");
                    incrementOneValue(newVersion, "testVal3");
                    newVersion.put("updated", System.currentTimeMillis());
                    newVersion.put("thread", threadName);
                    //add one character each time to make the file longer and longer over time.
                    newVersion.put("longString", newVersion.getString("longString")+((char)(65+rand.nextInt(26))));
                    
                    ljf.writeTarget(newVersion);
                    System.out.print(".");
                }
                finally {
                    ljf.unlock();
                }
                dur = System.currentTimeMillis() - startTime;
                if (dur > 500) {
                     System.out.println("\nThread "+threadName+" slow file access held lock "+dur+"ms!");
                }
                stat.put("duration", dur);
                stats.put(stat);
            }

        } catch (Exception e) {
            stat.put("error", e.toString());
            stats.put(stat);
            throw new Exception("Thread "+threadName+" failed to process file: "+testFile,e);
        }
    }
    
    public int incrementOneValue(JSONObject output, String name) throws Exception {
        if (!output.has(name)) {
            throw new Exception("Failure incrementing value "+name+", has object been initialized properly?");
        }
        int val = (int)Mel.safeConvertLong(output.getString(name)) + 1;
        if (val < lastSetValue) {
            throw new Exception("Problem, expected value >"+lastSetValue+" but got "+val+" instead.");
        }
        output.put(name, Integer.toString(val));
        return val;
    }

    
    public void report(PrintStream out) throws Exception {
        out.println("Thread "+threadName+" encountered "+exceptionCount+" exceptions in "+totalTries+" total tries");
        
        if (lastException!=null) {
            JSONException.traceException(System.out, lastException, "Thread "+threadName+" last exception was:");
        }
    }

    
    /**
     * This test gets a lock and holds it for a long time, reading the file at the beginning and the end and
     * assuring that nothing changed.
     */
    public void checkFileDoesNotChange() throws Exception {
        JSONObject stat = new JSONObject();
        stat.put("thread", threadName);
        try {
            LockableJSONFile ljf = LockableJSONFile.getSurrogate(testFile);
            
            synchronized(ljf) {
                try {
                    ljf.lock();
                    JSONObject firstVersion = ljf.readTarget();
                    int firstValue = (int)Mel.safeConvertLong(firstVersion.getString("testVal1"));
                    
                    Thread.sleep(1000);
                    
                    JSONObject lastVersion = ljf.readTarget();
                    int lastValue = (int)Mel.safeConvertLong(lastVersion.getString("testVal1"));
                    lastSetValue = lastValue;
                    
                    if (firstValue!=lastValue) {
                        throw new Exception("File was updated during lock:  "+firstValue+" was changed to "+lastValue);
                    }
                    System.out.print("*");
                }
                finally {
                    ljf.unlock();
                }
            }

        } catch (Exception e) {
            stat.put("error", e.toString());
            stats.put(stat);
            throw new Exception("Thread "+threadName+" failed to show file is static: "+testFile,e);
        }
    }    
}
