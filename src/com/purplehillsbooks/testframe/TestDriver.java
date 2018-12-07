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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import com.purplehillsbooks.json.JSONException;

/**
 * The Test Driver reads the command line arguments, makes an instance of the
 * test class, and passes a test recorder to it.
 * <p>
 * <b>TestFrame</b> overview. Compared to other test frameworks, TestFrame may
 * seem very simple. Do not confuse this with lack of capability.
 * TestFrame was designed to provide precisely what is needed to implement a set
 * of component tests, without a lot of extra fluff. Everything that is provided
 * is needed, and everything that is provided has a well defined meaning. Other
 * frameworks provide a large number of methods and capabilities that are not
 * well defined, often resulting in multiple ways to do things. While having
 * multiple redundant ways to do things may seem initially as a good thing, it
 * leads to different developers setting things up in different ways, with the
 * result that the entire test set is harder to manage. The goal with
 * "TestFrame" is to provide a single clear way to write the test so that it is
 * easier for developers to write tests that are consistent, and as a result
 * easier to maintain.
 * <p>
 * You need to understand two classes and one interface in order to write a
 * component test.
 * <ul>
 * <li>TestSet - this is the interface that you must implement. It has a single
 * method: "runTests". Simple enough. Whatever your test does, you must put the
 * code in the body of this method.
 * <li>TestRecorder - the method "runTests" is passed a single parameter which
 * is a TestRecorder. This object is the resource that your test uses to (1) get
 * configuration information and parameters, and (2) communicate results of
 * tests. There will be no writing to Standard.out! See the class documentation
 * for details.
 * <li>TestDriver - this is the program that creates a TestRecorder, and invokes
 * a test by passing the recorder to it. The only thing a component test writer
 * needs to know about TestDriver is the command line parameters to control the
 * testing. See below.
 * </ul>
 * <p>
 * Usage of TestDrive is very easy. THere are two methods. The first passes the
 * name of a single test class to invoke that class. The second is the name of a
 * text file. This text file contains a list of names of testclasses to be
 * invoked.
 * <ul>
 * <li>TestDriver myTestList.txt param param param
 * </ul>
 * The first parameter ends with ".txt" so it is assumed to be a text file, and
 * is read line by line. The rest of the parameters are stored in the
 * TestRecorder so that the test itself can access them. Each line contains a
 * fully qualified class name, and then a list of parameters. The parameters are
 * also stored in the TestRecorder so that the test can access them. A test file
 * lime look something like:
 * <ul>
 * <li>myTest.class param param param
 * <li>myTest.class param param param
 * </ul>
 * The second way to invoke TestDriver is to pass the name of a test class
 * itself followed by a number of parameters. Again, the parameters are not
 * significant to the TestDriver itself, but are stored in TestRecorder for use
 * by the test.
 * <ul>
 * <li>TestDriver myTest.class param param param
 * </ul>
 * <p>
 * That is all there is to it. Define a test which implements TestSet. That test
 * can make use of any routines, any support libraries. It can go read files
 * (although be careful not to hard code the path). It can run any number of
 * individual tests. It can even, if it chooses, call other classes that
 * implement TestSet. Essentually you have the full power of Java at your
 * disposal, so use it.
 *
 * @see TestRecorder
 * @see TestSet
 *
 *      Author: Keith Swenson
 */
public class TestDriver {

    private TestRecorderText recorder;
    private String[] excludeList;

    /**
     * This is used internally only
     */
    private TestDriver(TestRecorderText tr) {
        recorder = tr;
    }

    @SuppressWarnings("resource")
    static String[] readTestListFile(String fileName) throws Exception {
        Reader reader = null;
        File testListFile = new File(fileName);
        if (testListFile.exists()) {
            reader = new FileReader(testListFile);
        }
        else {
            // try to get the list from classpath!!
            InputStream is = ClassLoader.getSystemResourceAsStream(fileName);
            InputStreamReader isr = new InputStreamReader(is);
            reader = isr;
        }
        ArrayList<String> v = new ArrayList<String>();
        BufferedReader in = new BufferedReader(reader);
        String inputLine = null;
        while ((inputLine = in.readLine()) != null) {
            v.add(inputLine);
        }
        reader.close();
        return v.toArray(new String[0]);
    }

    private void runTests(String fileName) throws Exception {
        int lineCount = 0;
        String[] mainLineArgs = recorder.getCommandLineArgs();
        try {
            String[] testClassNames = readTestListFile(fileName);
            for (int i = 0; i < testClassNames.length; i++) {
                String name = testClassNames[i];
                if (name.length() < 5) {
                    continue;
                }
                if (name.charAt(0) == '#') {
                    continue;
                }
                lineCount = i;

                int startPos = 0;
                ArrayList<String> newArgs = new ArrayList<String>();
                int pos = name.indexOf(" ");
                while (pos >= 0 && startPos < name.length()) {
                    String token = name.substring(startPos, pos);
                    newArgs.add(token);
                    startPos = pos + 1;
                    pos = name.indexOf(" ", startPos);
                }
                if (startPos < name.length()) {
                    newArgs.add(name.substring(startPos));
                }

                // append whatever args were on the command line (except the
                // first)
                for (int k = 1; k < mainLineArgs.length; k++) {
                    newArgs.add(name.substring(startPos));
                }

                // convert to string array
                String[] newArgArray = new String[newArgs.size()];
                for (int j = 0; j < newArgs.size(); j++) {
                    newArgArray[j] = newArgs.get(j);
                }

                runOneTest(newArgArray);
            }
        }
        catch (Exception e) {
            throw new Exception("Line '" + lineCount + "' of file '" + fileName
                    + "' contains a entry that caused an exception", e);
        }
    }

    private void runOneTest(String[] args) throws Exception {
        recorder.setCommandLineArgs(args);
        if (args == null) {
            recorder.log("runOneTest null parameter -- this is a programming error within the TestDriver!");
            return;
        }
        if (args.length == 0) {
            recorder.log("runOneTest empty string array -- this is a programming error within the TestDriver!");
            return;
        }
        String className = args[0];
        if (className == null) {
            recorder.log("runOneTest received a null class name to run -- this is a programming error within the TestDriver!");
            return;
        }
        StringBuffer fullCommandBuff = new StringBuffer();
        for (int k = 0; k < args.length; k++) {
            fullCommandBuff.append(args[k]);
            fullCommandBuff.append(" ");
        }
        String fullCommand = fullCommandBuff.toString();

        if (!verifyTestName(className)) {
            recorder.log("**************************************************************");
            recorder.log("     IGNORING THIS TEST     ");
            recorder.log(fullCommand);
            recorder.log("**************************************************************");
            return;
        }
        try {
            recorder.log("**************************************************************");
            recorder.log(fullCommand);
            recorder.log("**************************************************************");

            // load the class by name
            Class<?> c = Class.forName(className);
            if (c == null) {
                throw new Exception("Class not found for name:" + className);
            }
            // make an instance of the class
            Constructor<?> ct = null;
            Object o = null;

            try {
                ct = c.getConstructor(new Class[] { TestRecorder.class });
            }
            catch (Exception e) {
                // thrown an exception if not found
            }
            if (ct != null) {
                o = ct.newInstance(new Object[] { recorder });
            }
            else {
                try {
                    ct = c.getConstructor(new Class[] {});
                }
                catch (Exception e) {
                    // thrown an exception if not found
                }
                if (ct != null) {
                    o = ct.newInstance(new Object[] {});
                }
            }

            if (ct == null) {
                throw new Exception(
                        "No appropriate constructor for class:"
                                + className
                                + "   The class must either have a constructor that takes a TestRecorder as an argument, "
                                + "or it must have a default constructor with no argument.");
            }
            if (o == null) {
                throw new Exception("Object instance not created for class:" + className);
            }
            if (!(o instanceof TestSet)) {
                throw new Exception("Class " + className
                        + " does not appear to be a subclass of TestSet!");
            }
            TestSet ts = (TestSet) o;
            ts.runTests(recorder);
        }
        catch (Exception e) {
            JSONException.traceException(e, "TestSet (" + fullCommand + ") failed");
            recorder.markFatalError(e);
            throw new Exception("TestSet (" + fullCommand + ") failed", e);
        }
    }

    private boolean verifyTestName(String className) {
        // Comment from Keith:
        // I am not convinced this is the right way. Each individual test should
        // be knowledgeable about whether that test can run on a particular
        // version of the product. Thus this knowledge of what runs and doesn't
        // run should be contained within the test program itself.
        // Need to consider whether we can switch to that design.
        if (excludeList == null) {
            try {
                String excludeListFile = null;
                String tType = recorder.getRequiredProperty("TWFTransportType");
                if (tType.equalsIgnoreCase("LE")) {
                    excludeListFile = recorder.getRequiredProperty("LEexcludeTestListFileName");
                }
                else if (tType.equalsIgnoreCase("EE")) {
                    excludeListFile = recorder.getRequiredProperty("EEexcludeTestListFileName");
                }
                else if (tType.equalsIgnoreCase("RMI")) {
                    excludeListFile = recorder.getRequiredProperty("AEexcludeTestListFileName");
                }

                excludeList = readTestListFile(excludeListFile);
            }
            catch (Exception e) {
                excludeList = new String[0];
            }
        }
        if (excludeList.length > 0) {
            for (int i = 0; i < excludeList.length; i++) {
                if (className.equals(excludeList[i])) {
                    return false;
                }
            }
        }

        return true;

    }

    private static void writeJustified(Writer w, int val) throws Exception {
        String s = Integer.toString(val);
        for (int i = s.length(); i < 5; i++) {
            w.write(" ");
        }
        w.write(s);
    }

    /**
     * Formats an exception message in order to print out a cascaded exception
     * correctly.
     * <p>
     * It converts a string with multiple nested curley braces into a sequence
     * of lines, indented one additional space for every level of nesting.
     * <p>
     * It is not likely that a test writer will need this, because a test should
     * simple <b>throw</b> an exception, and almost never catch them, and in any
     * case should never be printing them out. Instead an exception is reported
     * through the TestRecorder.fatal method.
     */
    public static void deparenthesize(Writer out, String input) throws Exception {
        try {
            // now print it as a set of ul statements
            int pos = 0;
            int last = input.length();
            int level = 1;
            while (pos < last) {
                int spos = input.indexOf('{', pos);
                int epos = input.indexOf('}', pos);
                int jpos = input.indexOf("nested exception is:", pos);
                int kpos = -1; // input.indexOf("because:", pos);
                int npos = last;
                int option = 0;
                int skip = 0;
                if (spos >= 0 && spos < npos) {
                    npos = spos;
                    option = 1;
                    skip = 1;
                }
                if (epos >= 0 && epos < npos) {
                    npos = epos;
                    option = 2;
                    skip = 1;
                }
                if (jpos >= 0 && jpos < npos) {
                    npos = jpos;
                    option = 3;
                    skip = 20;
                }
                if (kpos >= 0 && kpos < npos) {
                    npos = kpos;
                    option = 3;
                    skip = 8;
                }

                // if the character at the position is the searched char,
                // then both pos and npos will be the same
                if (npos > pos) {
                    out.write("\n");
                    for (int j = 0; j < level; j++) {
                        out.write(">");
                    }
                    out.write("  ");
                    out.write(input.substring(pos, npos));
                }
                pos = npos + skip;
                switch (option) {
                case 3:
                    // continue to next block
                case 1:
                    level++;
                    break;
                case 2:
                    level--;
                    break;
                case 0:
                    break;
                }

                // skip any following white space to avoid empty outline entries
                while (pos < last && input.charAt(pos) == ' ') {
                    pos++;
                }
            }
        }
        catch (Exception ee) {
            out.write("<!-- deparenthesize got this error:");
            out.write(ee.toString());
            out.write("-->");
        }
        out.write("\n\n");
    }

    /**
     * DriveTestsWithRecorder - allows an external program to create a test
     * recorder and pass it to this, but otherwise drive the tests in the same
     * manner. Used to integrate EST test framework with TestFrame
     */
    public static void DriveTests(TestRecorderText tr, String[] args) throws Exception {
        TestDriver td = new TestDriver(tr);
        if (args.length<1) {
            throw new Exception("TestDriver needs at least one parameter: the name of a class to test, or the name of a text file that lists all the classes to test");
        }
        String firstParam = args[0];
        if (firstParam.endsWith(".txt")) {
            td.runTests(firstParam);
        }
        else {
            td.runOneTest(args);
        }
    }

    /**
     * This program can be used with these parameters:
     *
     * <ul>
     * <li>TestDriver myTestList.txt param param param
     * <li>TestDriver myTest.class param param param
     * </ul>
     *
     * The parameter is the name of a text file that contains the list of
     * command lines: the first token on the line must be the test class, and
     * the rest of the parameters work like command line parameters, only note:
     * each parameter must be separated by exactly one space, and there is no
     * support for quote symbols to allow spaces within a parameter. Each such
     * class must implement the TestSet interface.
     */
    public static void main(String[] args) {
        int result = 0;
        try {
            Writer w = null;
            String firstParam = "";
            w = new OutputStreamWriter(System.out, "UTF-8");
            TestRecorderText tr = new TestRecorderText(w, true, args, "./");

            // please note, any important exception will happen within this try
            // block
            try {
                DriveTests(tr, args);
                result = tr.failedCount() + tr.fatalCount();
                w.flush();
            }
            catch (Exception e) {
                w.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                w.write("Fatal Error, in " + firstParam + "\n");
                deparenthesize(w, e.toString());
                JSONException.convertToJSON(e, "Fatal Error, in " + firstParam).write(w,2,2);
                result = -1;
                w.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            }

            w.write("\nSummary: ");
            writeJustified(w, tr.passedCount());
            w.write(" passed, ");
            writeJustified(w, tr.failedCount());
            w.write(" failed, ");
            writeJustified(w, tr.fatalCount());
            w.write(" fatal errors in ");
            w.write(firstParam);
            if (args.length > 1) {
                w.write(" ");
                w.write(args[1]);
            }
            w.write("\n");
            w.flush();

            if (tr.fatalCount() > 0) {
                w.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                w.write("Printing configuration since there was a fatal test\n");
                for  (String aName: tr.getProps().stringPropertyNames()) {
                    String val = tr.getProps().getProperty(aName);
                    w.write(aName + "=" + val + "\n");
                }
                w.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            }
            w.flush();
        }
        catch (Exception ee4) {
            // really just need this catch so that "main" does not throw any
            // exceptions
            System.out.println("While this is highly unlikely, constructing or writing");
            System.out.println("the summary to the output stream threw an exception: ");
            System.out.println(ee4.toString());
        }
        // for some reason exit needs to be called in order to assure that all
        // the
        // threads are shut down. Not sure why this is needed. Should
        // investigate.
        System.exit(result);
    }
}
