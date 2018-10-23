package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONException;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.json.LockableJSONFile;
import com.purplehillsbooks.streams.CSVHelper;
import com.purplehillsbooks.xml.Mel;

/**
 * Simple test of file locking
 */
public class FileLockThread extends Thread {

    //the millisecond that the thread is started
    long uniqueTimestamp = System.currentTimeMillis();
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

    static public final String OP_LOCK = "lock";
    static public final String OP_UNLOCK = "unlock";
    static public final String OP_READ = "read";
    static public final String OP_WRITE = "write";


    List<OpRecord> timeTable = new ArrayList<OpRecord>();

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
                this.reportStats(System.out, FileLockThread.OP_LOCK);
                this.reportStats(System.out, FileLockThread.OP_READ);
                this.reportStats(System.out, FileLockThread.OP_WRITE);
                this.reportStats(System.out, FileLockThread.OP_UNLOCK);

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
        OpRecord  or = null;
        JSONObject stat = new JSONObject();
        stat.put("thread", threadName);
        long dur = 0;
        try {
            LockableJSONFile ljf = LockableJSONFile.getSurrogate(testFile);

            synchronized(ljf) {
                long startTime = 0;
                try {
                    or = startOpRecord(FileLockThread.OP_LOCK);
                    ljf.lock();
                    finishOpRecord(or, null);

                    startTime = System.currentTimeMillis();
                    if (!ljf.exists()) {
                        //need to sleep AFTER the test, but before the throw or anything else
                        Thread.sleep(lockHoldMillis);
                        throw new Exception("Test file NOT FOUND: "+testFile);
                    }
                    Thread.sleep(lockHoldMillis);

                    or = startOpRecord(FileLockThread.OP_READ);
                    JSONObject newVersion = null;
                    //there are two different read operations.  They are essentially the same
                    //but one complains if the file does not exist.   We know the file exists
                    //so there is no effective difference.  Randomly call one or the other
                    //so that we can test both of them working in this test.
                    if (rand.nextInt(2)>=1) {
                        newVersion = ljf.readTarget();
                    }
                    else {
                        newVersion = ljf.readTargetIfExists();
                    }
                    finishOpRecord(or, null);

                    //now update them
                    lastSetValue = incrementOneValue(newVersion, "testVal1");
                    incrementOneValue(newVersion, "testVal2");
                    incrementOneValue(newVersion, "testVal3");
                    newVersion.put("updated", System.currentTimeMillis());
                    newVersion.put("thread", threadName);
                    //add one character each time to make the file longer and longer over time.
                    newVersion.put("longString", newVersion.getString("longString")+((char)(65+rand.nextInt(26))));

                    or = startOpRecord(FileLockThread.OP_WRITE);
                    ljf.writeTarget(newVersion);
                    finishOpRecord(or, null);
                    System.out.print(".");
                }
                finally {
                    or = startOpRecord(FileLockThread.OP_UNLOCK);
                    ljf.unlock();
                    finishOpRecord(or, null);
                }
                dur = System.currentTimeMillis() - startTime;
                if (dur > 500) {
                     System.out.println("\nThread "+threadName+" slow file access held lock "+dur+"ms!");
                }
                stat.put("duration", dur);
                stats.put(stat);
            }

        } catch (Exception e) {
            if (or!=null) {
                finishOpRecord(or, e);
                or = null;
            }
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

        this.reportStats(out, FileLockThread.OP_LOCK);
        this.reportStats(out, FileLockThread.OP_READ);
        this.reportStats(out, FileLockThread.OP_WRITE);
        this.reportStats(out, FileLockThread.OP_UNLOCK);

        String dumpName = "Run_"+uniqueTimestamp+threadName+Thread.currentThread().getId()+".csv";
        File dumpFile = new File( testFile.getParent(), dumpName);
        FileOutputStream fos = new FileOutputStream(dumpFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        for (OpRecord or : timeTable) {
            List<String> line = new ArrayList<String>();
            line.add(Long.toString(or.timestamp));
            line.add(or.op);
            line.add(Long.toString(or.duration));
            CSVHelper.writeLine(osw, line);
        }
        osw.flush();
        osw.close();
    }


    /**
     * This test gets a lock and holds it for a long time, reading the file at the beginning and the end and
     * assuring that nothing changed.
     */
    public void checkFileDoesNotChange() throws Exception {
        OpRecord  or = null;
        JSONObject stat = new JSONObject();
        stat.put("thread", threadName);
        try {
            LockableJSONFile ljf = LockableJSONFile.getSurrogate(testFile);

            synchronized(ljf) {
                try {
                    or = startOpRecord(FileLockThread.OP_LOCK);
                    ljf.lock();
                    finishOpRecord(or, null);
                    or = startOpRecord(FileLockThread.OP_READ);
                    JSONObject firstVersion = ljf.readTarget();
                    finishOpRecord(or, null);
                    int firstValue = (int)Mel.safeConvertLong(firstVersion.getString("testVal1"));

                    Thread.sleep(1000);

                    or = startOpRecord(FileLockThread.OP_READ);
                    JSONObject lastVersion = ljf.readTarget();
                    finishOpRecord(or, null);
                    int lastValue = (int)Mel.safeConvertLong(lastVersion.getString("testVal1"));
                    lastSetValue = lastValue;

                    if (firstValue!=lastValue) {
                        throw new Exception("File was updated during lock:  "+firstValue+" was changed to "+lastValue);
                    }
                    System.out.print("*");
                }
                finally {
                    or = startOpRecord(FileLockThread.OP_UNLOCK);
                    ljf.unlock();
                    finishOpRecord(or, null);
                    or = null;
                }
            }

        } catch (Exception e) {
            if (or!=null) {
                finishOpRecord(or, e);
                or = null;
            }
            stat.put("error", e.toString());
            stats.put(stat);
            throw new Exception("Thread "+threadName+" failed to show file is static: "+testFile,e);
        }
    }

    public void finishOpRecord(OpRecord or, Exception e) {
        or.duration = (System.nanoTime()/1000) - or.timestamp;
        timeTable.add(or);
    }

    public OpRecord startOpRecord(String oper) {
        OpRecord or = new OpRecord();
        or.timestamp = System.nanoTime()/1000;
        or.op = oper;
        return or;
    }


    class OpRecord {
        public long timestamp;
        public long duration;
        public String op;
    }


    public void reportStats(PrintStream out, String op) {
        long max = 0;
        long sum = 0;
        long count = 0;
        for (OpRecord or : this.timeTable) {
            if (op.equals(or.op)) {
                if (or.duration > max) {
                    max = or.duration;
                }
                sum += or.duration;
                count++;
            }
        }
        long avg = 0;
        if (count>0) {
            avg = sum / count;
        }
        out.println("    "+op+" count="+count+", max="+max+",  avg="+avg+" (microseconds)");
    }
}
