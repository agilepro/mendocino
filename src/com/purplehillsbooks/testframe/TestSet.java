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

/**
 * The TestSet interface must be implemented by each class implementing tests,
 * so that it can be automatically called by the test framework. There is only
 * one method "runTests" that must be implemented.
 *
 * @see TestRecorder
 * @see TestDriver
 *
 * Author: Keith Swenson
 */
public interface TestSet {

    /**
     * The only method that must be implemented is "runTests". The tests must
     * initialize, run, and cleanup within this single call.
     * <p>
     * This method can include any number of tests. It is expected that you will
     * have to get set up in a particular configuration, and then run many
     * individual test to assure the program is running correctly.
     * <p>
     * A test is allowed to do anything you can do in java. Clearly you can
     * write your own subroutines and use them anyway you need. A single TestSet
     * class can actually be any number of individual tests; you simple call the
     * markPassed or markFailed as apropriate for each test. You should strive
     * to design tests so that the number of tests remain constant. One run of
     * the tests should not have 5 results, and a different run the next day
     * have 31 results. If there are 31 tests, they all should produce a result
     * every time. If some tests run only in certain configurations, then when
     * skipping the test, you should record a result of "passed" and include in
     * the id/description that the test was skipped. Try to avoid tests that
     * read the database and report a success for each row in a table because
     * that might vary in number. Instead, read and check every row, and then
     * record a single result so that the number remains constant.
     *
     * @param tr
     *                Each test can record success or failure with the
     *                TestRecorder. The TestRecorder is the only way that the
     *                test should contact the outside world (except the software
     *                it is testing). All configuration information should be
     *                gotten from the config file support or the command line
     *                parameter support. All output should be to the
     *                TestRecorder which offers the ability to record that a
     *                test passed, that it failed, and all additional debug log
     *                output. Where this output goes, and how it is formatted
     *                depends upon the test framework that this is embedded in.
     *                For example, this test might be run as part of a JUnit
     *                suite, and then the output will go to the normal JUnit
     *                log. Or it might be part of anything else. But the test
     *                must only use the TestRecorder to get config info, and to
     *                write results.
     *
     * @exception Exception
     *                    if an error occurs for which you can not properly
     *                    continue testing, the test set should throw an
     *                    exception. This is the proper way to indicate that
     *                    testing was stopped before all of the tests were run.
     *                    The exception will be recorded as a fatal error by the
     *                    test framework. An example of such a fatal error is
     *                    unable to log into the server ... if this happens
     *                    there is little point in trying further tests, so
     *                    throw an exception.
     */
    public void runTests(TestRecorder tr) throws Exception;

}
