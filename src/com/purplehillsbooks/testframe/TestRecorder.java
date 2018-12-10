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

import java.util.Properties;

/**
 * The TestFrame framework provides a TestRecorder interface to each test upon
 * invocation. The only parameter to "TestSet.runTest" is a TestRecorder. Each
 * Test Case uses this class to
 * <ol>
 * <li>(1) get configuration information, and
 * <li>(2) record test results - whether the case passed or failed.
 * </ol>
 *
 * Each implementation of TestRecorder may do the above in its own way.
 *
 * For example, one implementation may read configuration information from a
 * flat text file of name-value pairs, while another may read it from an XML
 * file Also, one implementation may write the test results to a text file,
 * while another may tie into a package like log4j to produce the results.
 *
 * All communications to the outside world that is not part of the test must be
 * through the test recorder.
 *
 * Standard.out must NOT be written to, because there is no guarantee that
 * output will be accessible. You must not open your own log file for recording
 * results. Calling markPassed or markFailed will assure that the result of the
 * test is properly recorded, and entered in all statistics.
 *
 * There is a capability to produce "debug" output while you are developing the
 * test, see the "log" method.
 *
 * TestRecorder implementations should have "verbose" as one of the
 * configuration parameters. When verbose=false (normal situations) output
 * should only be produced for failed and fatal tests. The logic is that
 * normally we will have hundreds of tests, and they all pass so we only need to
 * know about tests which failed or had fatal errors. When verbose=true, output
 * should be produced for passed tests and log information (recorded using the
 * log method), in addition to the output produced when verbose=false. This
 * allows you to get a listing of all the tests that were run if you wish, along
 * with any "debug" information that the tests may have put out.
 *
 * @see TestSet
 * @see TestDriver
 *
 * Author: Keith Swenson
 */
public interface TestRecorder {
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
    public void markPassed(String id);

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
     *                follows the same rules as above (and it goes without
     *                saying that the same id should be used when indicating
     *                success as when indicating failure for a particular test.)
     * @param details
     *                is additional information explaining what was expected and
     *                what was wrong about it. For example to explain what was
     *                being done, what was unique about this test case, which
     *                might give a clue to why the program failed.
     */
    public void markFailed(String id, String details);


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
    public void markFatalError(Exception e);


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
    public void log(String s);

    /**
     * Returns the value of the 'verbose' configuration parameter. Tests will
     * ususally not need to call this method.
     */
    public boolean isVerbose();

    /**
     * A configuration file 'test.conf' is read automatically and is represented
     * in the TestRecorder as a Properties object. Standard global configuration
     * parameters should be placed in 'test.conf' and the tests can get access
     * to those configuration parameters using this method. If the property does
     * not exist, then default is returned.
     */
    public String getProperty(String name, String defaultVal);

    /**
     * A configuration file 'test.conf' is read automatically and is represented
     * in the TestRecorder as a Properties object. Standard global configuration
     * parameters should be placed in 'test.conf' and the tests can get access
     * to those configuration parameters using this method. The specified
     * property MUST exist in the properties file. Throws a nice exception if
     * the property is missing. Use this for properties like "serverName" where
     * there is no way to provide a reaonsable default. If you need a particular
     * configuration value, then if it is not configured, then this is a
     * configuration problem that must be addressed before running the tests.
     * Throwing an exception will cause termination of the testing due to a
     * fatal error, and it will display the reason for the fatal, which will
     * encourage the tester to set the configuration up correctly.
     */
    public String getRequiredProperty(String name) throws Exception;

    /**
     * getTestDir returns the path to the folder that contains all the test
     * files. If a test needs to read such a file, get this path and append the
     * name of the file on to it.
     *
     * You should assume this directory is "read only"
     */
    public String getTestDir();


    public void testInt(String id, int value, int expected);



    //////////////////// Below are under review /////////////////////

    /**
     * Tests generally should not use this method but rather should use the
     * convenient routines getProperty and getRequiredProperty,
     */
    public Properties getProps();

    /**
     * setProps should not be used except in extreme situations where particular
     * properties need to be added or modified for a particular test.
     */
    public void setProps(Properties props);

    /**
     * The command line arguments to the main routine are stored in the
     * TestRecorder. Individual tests can have access to them using this method,
     * and can use them for any purpose necessary. If the command line pointed
     * to a text file with a list of tests, each line of that file contains the
     * name of a class file, along with parameters, and those parameters will
     * appear in the returned string array.
     */
    public String[] getCommandLineArgs();

    /**
     * The TestDriver may call this method if it needs to add/modify the command
     * line arguments. Tests should not call this method.
     */
    public void setCommandLineArgs(String[] args);

}
