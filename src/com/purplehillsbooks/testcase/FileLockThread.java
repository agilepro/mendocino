package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONException;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.json.LockableJSONFile;

/**
 * Simple test of file locking
 */
public class FileLockThread extends Thread {
    
    boolean running = true;
    long lastSetValue = 0;
    int totalTries = 0;
    int exceptionCount = 0;
    String threadName;
    long lockHoldMillis = 20;
    Random rand;
    JSONArray stats = new JSONArray();
    
    public FileLockThread(String name) throws Exception {
        threadName = name;
        File testFile = new File("./ConFileTest.json");
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
        System.out.println("Running Thread");
        while (running) {
            if (++totalTries % 25 == 0) {
                System.out.println("\nThread "+threadName+" completed "+totalTries+", value is "+lastSetValue+", exceptions: "+exceptionCount);
            }
            try {
                //we are in a fast loop doing this as fast as possible 99 out of 100 times
                incrementLocalJSON();
            
            }
            catch (Exception e) {
                exceptionCount++;
                JSONException.traceException(System.out, e, "Thread "+threadName+" FAILURE on try #"+totalTries);
            }
        }
    }   
    
    public void incrementLocalJSON() throws Exception {
        lastSetValue = 1;
        JSONObject stat = new JSONObject();
        stat.put("thread", threadName);
        long dur = 0;
        File testFile = new File("./ConFileTest.json");
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
        int val = (int)safeConvertLong(output.getString(name)) + 1;
        if (val < lastSetValue) {
            throw new Exception("Problem, expected value >"+lastSetValue+" but got "+val+" instead.");
        }
        output.put(name, Integer.toString(val));
        return val;
    }
    
    public static long safeConvertLong(String val) {
        if (val == null) {
            return 0;
        }
        long res = 0;
        int last = val.length();
        for (int i = 0; i < last; i++) {
            char ch = val.charAt(i);
            if (ch >= '0' && ch <= '9') {
                res = res * 10 + ch - '0';
            }
        }
        return res;
    }
    
    public void report(PrintStream out) throws Exception {
        out.println("Thread "+threadName+" encountered "+exceptionCount+" exceptions in "+totalTries+" total tries");
    }
    
}
