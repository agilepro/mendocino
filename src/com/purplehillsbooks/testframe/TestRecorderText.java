/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.purplehillsbooks.testframe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import com.purplehillsbooks.json.JSONException;

/**
 * TestRecorderText implements the TestRecorder interface so that:
 * <ol>
 * <li>(1) Configuration information is read from a text file called
 * 'test.conf'. This file has one name-value pair per line as
 * parameter_name=parameter_value
 *
 * <li>(2) Test results are written to a text file, typically 'FinalLog.log'.
 * </ol>
 *
 * @see TestSet
 * @see TestDriver
 *
 *      Author: Keith Swenson
 */
public class TestRecorderText implements TestRecorder {
    /**
     * This is simply the count of tests that have been recorded as passed. It
     * starts at zero, and when all tests are done the result is reported.
     */
    private int passedTests = 0;
    /**
     * This is simply the count of tests that have been recorded as failing. It
     * starts at zero, and when all tests are done the result is reported.
     */
    private int failedTests = 0;
    /**
     * This is simply the count of tests that have been terminated prematurely.
     * It starts at zero. Generally speaking, this should either be a zero or 1.
     * If an exception is thrown from a test routine, it is a fatal error, and
     * should stop all subsequent testing activity.
     */
    private int fatalTests = 0;

    /**
     * verbose == false means only failures are reported. verbose == true means
     * in addition to the above, all log and passed items are also reported.
     */
    private boolean verbose;

    /**
     * The arguments to the main routine are stored in the string array
     * "commandLineArgs". If the command line pointed to a text file with a list
     * of tests, each line of that file contains the name of a class file, along
     * with parameters, and those parameters will appear in thsi string array.
     * Individual tests can have access to the command line arguments in this
     * way, and can use them for any purpose necessary.
     */
    private String[] commandLineArgs;

    /**
     * A configuration file 'test.conf' is read in order to get all of the
     * configuration properties for the tests. The properties are represented as
     * a Properties object. Standard global configuration parameters should be
     * placed in 'test.conf' and the tests can get access to those configuration
     * settings in this variable.
     *
     * The originalProps (passed in on constructor) are then appended to this,
     * which is where the connection properties come from. The property settings
     * from originalProps will take precidence, that is the passed in properties
     * will override the property values taken from 'test.conf'.
     */
    private Properties props;

    /**
     * This is a properties object passed in which contain configuration details
     * about the current system: e.g. server name, port number, and all the
     * things that might change from installation to installation. These are
     * then appended to the configuration properties that are read from the
     * 'test.conf' config file.
     */
    private Properties originalProps;

    private PrintWriter logWriter;
    private String testDir; // directory to read files from

    // This vector saves up the log when it is being suppressed.
    // If a failure message is reported, then the log that preceeded it
    // will be printed out before the failure message.
    // If a pass message is reported, then the vector is cleared.
    // Every element is a string.
    private ArrayList<String> savedLog;

    // save away the time that the test recorder was created so that time
    // can be recorded as a difference to this.
    private long creationTime;

    // keep the long term records
    public static Hashtable<String, int[]> records = new Hashtable<String, int[]>();

    public static ArrayList<TestResultRecord> resultSet;

    public TestRecorderText(Writer forLog, boolean newVerbose, String[] args, Properties configProps)
            throws Exception {
        this(forLog, newVerbose, args, null, configProps);
    }

    /**
     * @deprecated constructor, use the one below instead
     */
    public TestRecorderText(Writer forLog, boolean newVerbose, String[] args, String configDir)
            throws Exception {
        this(forLog, newVerbose, args, configDir, new Properties());
    }

    /**
     * The test driver initializes this class and gives it to each test case.
     * For a run of the tests, only one recorder is constructed, so that it can
     * collect the results of all the tests.
     * <p>
     * A test class should never be creating a TestRecorder, only the TestDriver
     * should do so. This allows the tests themselves to be reusable in any
     * situation.  You can write your own test driver which calls all your tests,
     * or you can use the generic TestDriver class that reads what it needs from
     * input files in order to call the tests.
     */
    public TestRecorderText(Writer forLog, boolean newVerbose, String[] args, String configDir,
            Properties configProps) throws Exception {
        testDir = configDir;
        logWriter = new PrintWriter(forLog);
        commandLineArgs = args;
        if (configProps != null) {
            originalProps = configProps;
        }
        else {
            originalProps = new Properties();
        }
        props = readConfFile(configDir);

        // now copy the values from passed in properties to the test.conf props
        @SuppressWarnings("unchecked")
        Enumeration<String> ep = (Enumeration<String>) originalProps.propertyNames();
        while (ep.hasMoreElements()) {
            String key = ep.nextElement();
            if (key == null) {
                continue; // in case it is non-string key
            }
            String val = originalProps.getProperty(key);
            if (val == null) {
                continue; // in case it is non-string value
            }
            props.setProperty(key, val);
        }

        // read the verbose setting from the config file.
        String verboseDefault = "false";
        if (newVerbose) {
            verboseDefault = "true";
        }
        verbose = ("true".equals(getProperty("verbose", verboseDefault)));
        savedLog = new ArrayList<String>();

        creationTime = System.currentTimeMillis();

        if (records == null) {
            records = new Hashtable<String, int[]>();
        }
        if (resultSet == null) {
            resultSet = new ArrayList<TestResultRecord>();
        }
    }

    // could pass the file name into the constructor, but I don't
    // see the need for that flexibility right now.
    // Better to keep it simple.
    private Properties readConfFile(String configDir) throws Exception {
        Properties iprop = new Properties();

        if (configDir == null) {
            return iprop;
        }

        File configPath = new File(configDir);
        if (!configPath.exists()) {
            throw new Exception(
                    "Problem reading config file, the path for the config directory does not exist: "
                            + configDir);
        }

        File configFile = new File(configPath, "test.conf");
        if (!configFile.exists()) {
            return iprop;
        }

        try {

            iprop.load(new FileInputStream(configFile));

            return iprop;
        }
        catch (Exception e) {
            throw new Exception("Unable to read the test framework configuration file '"
                    + configDir + "test.conf'", e);
        }
    }

    /**
     * Read a property from the global properties file. If property does not
     * exist, then default is returned.
     */
    public String getProperty(String name, String defaultVal) {
        String val = props.getProperty(name);
        if (val != null) {
            return val;
        }
        return defaultVal;
    }

    /**
     * Read a property from the global properties file. The specified property
     * MUST exist in the properties file. Throws a nice exception if the
     * property is missing. Use this for properties like "serverName" where
     * there is no way to provide a reaonsable default. If you need a particular
     * configuration value, then if it is not configured, then this is a
     * configuration problem that must be addressed before running the tests.
     * Throwing an exception will cause termination of the testing due to a
     * fatal error, and it will display the reason for the fatal, which will
     * encourage the tester to set the configuration up correctly.
     */
    public String getRequiredProperty(String name) throws Exception {
        String val = props.getProperty(name);
        if (val != null) {
            return val;
        }
        throw new Exception("Unable to find the '" + name
                + "' property in the 'test.conf' configuration file.");
    }

    /**
     * Write out the number of seconds since the test started in the format
     * sss.mmm where sss is a three digit number of seconds, and mmm is the
     * three digit milliseconds. First, make sure that duration is less than 1
     * million, then add a million so you have a 7 digit number of milliseconds.
     * Then pick three digits for seconds and three digits for millis.
     */
    private void timeStamp() {
        long dur = System.currentTimeMillis() - creationTime;
        String rep;
        if (dur > 999999) {
            dur = dur % 1000000;
        }
        dur = dur + 1000000;
        rep = Long.toString(dur);
        logWriter.write(rep.substring(1, 4));
        logWriter.write(".");
        logWriter.write(rep.substring(4, 7));
        logWriter.write(" ");
    }

    /**
     * When a simple test passes, for example a simple calculation performed,
     * and the result matched the expected result, then this is called to
     * indicate that the test has succeeded.
     * <p>
     * This will record the fact the that test succeeded and report it in the
     * final results. The id is a simple name or description that can be used to
     * uniquely identify this test. It might consist of the test number in a
     * particular test set.
     * <p>
     * When verbose=false (normal situations) this will not produce any output
     * to the log. The logic is that normally we will have hundreds of tests,
     * and they all pass so we don't really need to know about those.
     * <p>
     * When verbose=true, it will record this pass to the log, which allows you
     * to get a listing of all the tests that were run if you wish.
     */
    public void markPassed(String id) {
        TestResultRecord trr = new TestResultRecord(id, "", true, commandLineArgs);
        resultSet.add(trr);

        passedTests++;
        try {
            if (verbose) {
                logWriter.write("[PASS  ] ");
                timeStamp();
                logWriter.write(id);
                logWriter.write("\n");
                logWriter.flush();
            }
            savedLog.clear();
        }
        catch (Exception e) {
            JSONException.traceException(logWriter, e, "Exception during markPassed");
        }
    }

    /**
     * When a simple test fails, for example a simple calculation performed, and
     * the result does not matched the expected result, then this is called to
     * indicate that the test failed.
     * <p>
     * A log entry is printed to record the fact with the details regarless of
     * whether verbose==true or verbose==false. The rationale is that you always
     * want to know about failures. Not only is the id and details written out,
     * but also a line that include the parameters that were passed to the test,
     * as well as any log messages that were saved up since the last result
     * message.
     * <p>
     *
     * @param id
     *            follows the same rules as above (and it goes without saying
     *            that the same id should be used when indicating success as
     *            when indicating failure for a particular test.)
     * @param details
     *            is additional information explaining what was expected and
     *            what was wrong about it. For example to explain what was being
     *            done, what was unique about this test case, which might give a
     *            clue to why the program failed.
     */
    public void markFailed(String id, String details) {
        failedTests++;
        try {
            dumpSavedLog();
            logWriter.write("[  FAIL] ");
            timeStamp();
            logWriter.write(id);
            logWriter.write(": ");
            logWriter.write(details);
            logWriter.write("\n");
            printArgs();
            logWriter.flush();

            TestResultRecord trr = new TestResultRecord(id, "", false, commandLineArgs);
            trr.failureMessage = details;
            trr.savedLog = savedLog;
            resultSet.add(trr);
            // create a new one because the old one stays with test result
            savedLog = new ArrayList<String>();
        }
        catch (Exception e) {
            JSONException.traceException(logWriter, e, "Exception during markFailed");
        }
    }

    public void testInt(String id, int value, int expected) {
        if (value == expected) {
            markPassed(id);
        }
        else {
            markFailed(id, "Expected '" + expected + "' but got '" + value + "' instead.");
        }
    }

    /**
     * A "fatal error" is an error that is so bad that subsequent tests were
     * skipped. For example, a particular routine is expecting test 5
     * consistency constraints on the contents of a file, but it finds that the
     * file does not exist. There are two choices: call markFailed 5 times (so
     * that all failures are recorded) or record a single markFatalError to
     * indicate that something is so wrong it is not even worth continuing
     * testing.
     * <p>
     * Recording a fatal error means tests has been skipped in this run, and the
     * resulting statistics are meaningless. We don't have a complete record of
     * all the tests that might have failed. The presence of a single fatal test
     * should tell you that something is terribly broken in the tests, and the
     * results are not valid.
     * <p>
     * Normally, a test case will indicate a fatal error by throwing an
     * exception. The Test Framework will catch the exception, and use this
     * method to record that a test failed to the extent that further testing
     * had to be skipped. Throw a meaningful exception instead of calling this
     * routine.
     */
    public void markFatalError(Exception e) {
        if (e == null) {
            return;
        }

        fatalTests++;
        try {
            dumpSavedLog();
            logWriter.write("[FATAL ] ");
            timeStamp();
            logWriter.write(e.toString());
            logWriter.write("\n");
            printArgs();
            logWriter.flush();

            TestResultRecord trr = new TestResultRecord("Fatal Error", "", false, commandLineArgs);
            trr.failureMessage = e.toString();
            trr.fatalException = e;
            trr.savedLog = savedLog;
            resultSet.add(trr);
            // create a new one because the old one stays with test result
            savedLog = new ArrayList<String>();
        }
        catch (Exception e1) {
            JSONException.traceException(logWriter, e, "Exception during markFatalError");
        }
    }

    /**
     * Writes the specified string to the log file, only when verbose == true.
     * This is used typically for debugging, and calls to this should be removed
     * when the test case is ready for prime time.
     * <p>
     * Do not use the log method to write out whether a test passes or fails! It
     * is remarkable how many time programmers write tests that require a person
     * to read the log a determine whether the test succeeded or not. In normal
     * testing situation, nobody reads the logs! In the case of automated
     * builds, the automated run of a test produces a log, but if no error is
     * indicated using markFailure, then nobody ever looks at the log files. So
     * use it only for debug information while developing the test.
     * <p>
     * Note: there is a special capability to reduce clutter and still get
     * meaningful results. When the log method is called, and verbose==false the
     * line is not written to output, but is is saved up in a collection. If the
     * next test result is "pass" then those lines are thrown away. But if the
     * next result is "fail" then those saved up log items are output just
     * before the failure message. So if you are outputting information using
     * log statements before reporting the results of a test, those statements
     * will be seen even with verbose==false if the test fails.
     */
    public void log(String s) {
        try {
            savedLog.add(s);
            if (verbose) {
                logWriter.write("[      ] ");
                timeStamp();
                logWriter.write(s);
                logWriter.write("\n");
                logWriter.flush();
            }
        }
        catch (Exception e) {
            JSONException.traceException(logWriter, e, "Exception during logging");
        }
    }

    /**
     * See comment on savedLog. This writes out the contents of the saved up
     * log.
     */
    private void dumpSavedLog() {
        // if it was verbose, then it already was written out.
        if (verbose) {
            return;
        }
        int last = savedLog.size();
        for (int i = 0; i < last; i++) {
            logWriter.write("[      ] ???.??? ");
            logWriter.write(savedLog.get(i));
            logWriter.write("\n");
        }
    }

    /**
     * prints out the command line args for reference with failures
     */
    private void printArgs() {
        int last = commandLineArgs.length;
        logWriter.write("[    IN] ");
        for (int i = 0; i < last; i++) {
            logWriter.write(commandLineArgs[i]);
            logWriter.write(" ");
        }
        logWriter.write("\n");
    }

    public int passedCount() {
        return passedTests;
    }

    public int failedCount() {
        return failedTests;
    }

    public int fatalCount() {
        return fatalTests;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public String[] getCommandLineArgs() {
        return commandLineArgs;
    }

    public void setCommandLineArgs(String[] args) {
        commandLineArgs = args;
    }

    public String getTestDir() {
        return testDir;
    }

    /**
     * This records results of a single test int eh records hashtable. But...the
     * test recorder will be keeping a cumulative total of all tests, so pass in
     * the previous values so they can be discounted.
     *
     * This is not the most straightforward way to do this. Perhaps a better way
     * is to have a command to start a new test, which PUSHes a new results
     * structure on a stack, and a command to complete a test which records the
     * result, and add the results of that test to the global results. I have
     * implemented such a mechanism in the past, but it has been deemed
     * overkill. So this simpler approach will work for now, until we need
     * something better.
     */
    public void recordResults(String testName, int prevPass, int prevFail) {
        if (records == null) {
            records = new Hashtable<String, int[]>();
        }
        int[] stats = records.get(testName);
        if (stats == null) {
            stats = new int[3];
            stats[0] = 0;
            stats[1] = 0;
            stats[2] = 0;
            records.put(testName, stats);
        }
        stats[0] += passedCount() - prevPass + failedCount() - prevFail;
        stats[1] += passedCount() - prevPass;
        stats[2] += failedCount() - prevFail;
    }

    public static void clearResultSet() {
        resultSet = new ArrayList<TestResultRecord>();
    }

    public static ArrayList<TestResultRecord> getResults() {
        // will be null if no tests run yet....
        if (resultSet == null) {
            resultSet = new ArrayList<TestResultRecord>();
        }
        else {
            TestResultComparator trc = new TestResultComparator();
            Collections.sort(resultSet, trc);
        }
        return resultSet;
    }

    static class TestResultComparator implements Comparator<TestResultRecord> {
        TestResultComparator() {
        }

        public int compare(TestResultRecord o1, TestResultRecord o2) {
            TestResultRecord trr1 = o1;
            TestResultRecord trr2 = o2;
            if (trr1 == null || trr2 == null) {
                return 0;
            }
            if (trr1.category == null || trr2.category == null) {
                return 0;
            }
            if (!trr1.category.equals(trr2.category)) {
                return trr1.category.compareTo(trr2.category);
            }
            if (trr1.caseDetails == null || trr2.caseDetails == null) {
                return 0;
            }
            return trr1.caseDetails.compareTo(trr2.caseDetails);
        }
    }




    public static void parseArgsRunTests(String args[], TestSet ts) {
        TestRecorderText tr=null;
        try {
            if (args.length < 2) {
                throw new Exception("USAGE: Test1  <source folder>  <test output folder>");
            }
            String sourceFolder = args[0];
            String outputFolder = args[1];
            Properties props = new Properties();
            props.put("source", sourceFolder);
            props.put("testoutput", outputFolder);
            props.put("verbose", "true");

            File testsrc = new File(sourceFolder, "testdata");
            if (!testsrc.isDirectory()) {
                throw new Exception(
                        "Configuration error: first parameter must be the path to the source directory and it must exist.  The following was passed and does not exist: "
                                + sourceFolder);
            }
            File testout = new File(outputFolder);
            if (!testout.isDirectory()) {
                throw new Exception(
                        "Configuration error: second parameter must be the path to the test output directory and it must exist.  The following was passed and does not exist: "
                                + outputFolder);
            }

            File outputFile = new File(testout, "output_"+ts.getClass().getSimpleName()+".txt");
            if (outputFile.exists()) {
                outputFile.delete();
            }
            tr = new TestRecorderText(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"),
                    true, new String[0], ".", props);
            ts.runTests(tr);
        }
        catch (Exception e) {
            System.out.print("\n\n\n====================================================");
            JSONException.traceException(e, "EXCEPTION CAUGHT AT MAIN LEVEL");
        }
        if (tr!=null) {
            System.out.print("\n\n\n====================================================");
            System.out.print("\n               FINISHED RUN for "+ts.getClass().getName());
            System.out.print("\n====================================================");
            System.out.print("\n Number PASSED: "+tr.passedCount());
            System.out.print("\n Number FAILED: "+tr.failedCount());
            System.out.print("\n Number FATAL:  "+tr.fatalCount());
            System.out.print("\n====================================================\n");
        }
    }
}
