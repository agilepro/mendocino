package com.purplehillsbooks.testcase;

import java.io.File;

import com.purplehillsbooks.json.JSONException;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 * 
 * NOTE: these tests are very sensitive to the line numbers of the test framework.
 * If you add or remove lines in the test framework, you may need to regenerate
 * the test results files.
 */
public class TestExceptions implements TestSet {

    TestRecorder tr;
    File sourceDataFolder;

    public TestExceptions() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;
        sourceDataFolder = new File(tr.getProperty("source", null), "testData");
        if (!sourceDataFolder.exists()) {
            System.out.println("Source data folder does not exist: "+sourceDataFolder);
            return;
        }
        runCourse("11121112333", "ExTest1.json");
        runCourse("3",           "ExTest2.json");
        runCourse("1222",        "ExTest3.json");
        runCourse("2",           "ExTest4.json");
 
        
        compareString(newTr, "testing format 1", "test whether it works", String.format("test %s it works", "whether"));
        compareString(newTr, "testing format 2", "test whether it works two", String.format("test %s it works %s",  "whether", "two"));
        String[] myBundle = new String[] {"whether", "two"};
        compareString(newTr, "testing format 3", "test whether it works two", String.format("test %s it works %s",  (Object []) myBundle));
        

        
        JSONException je = new JSONException("this is a %s good template", "very");
        compareString(newTr, "JSONException one parameter", "this is a very good template", je.getMessage());
        je = new JSONException("this has %s and %s value", "first", "second");
        compareString(newTr, "JSONException two params", "this has first and second value", je.getMessage());
        je = new JSONException("three %s values %s to %s see", "nogood", "inevibatly", "hardly");
        compareString(newTr, "JSONException three params", "three nogood values inevibatly to hardly see", je.getMessage());      
        JSONException je2 = new JSONException("NESTED %s values %s to %s see", je, "nogood", "inevibatly", "hardly");
        compareString(newTr, "JSONException three params NESTED", "NESTED nogood values inevibatly to hardly see", je2.getMessage());      
    }

    public void compareString(TestRecorder tr, String caseDesc, String s1, String s2) {
        if (s1.equals(s2)) {
            tr.markPassed(caseDesc);
        }
        else {
            tr.markFailed(caseDesc, "Expected ("+s1+") but got ("+s2+")");
        }
    }    
    private void sampleMethod1(String course) throws Exception {
        callHigher(course);
    }

    private void sampleMethod2(String course) throws Exception {
        try {
            callHigher(course);
        }
        catch (Exception e) {
            throw new Exception("caught and rethrow from level: "+course, e);
        }
    }

    private void sampleMethod3(String course) throws Exception {
        callHigher(course);
    }
    
    private void callHigher(String course) throws Exception {
        if (course==null || course.length()==0) {
            throw new Exception("top level exception thrown by exception test when course string ran out");
        }
        char ch = course.charAt(0);
        if (ch=='1') {
            sampleMethod1(course.substring(1));
        }
        else if (ch=='2') {
            sampleMethod2(course.substring(1));
        }
        else if (ch=='3') {
            sampleMethod3(course.substring(1));
        }
        else {
            throw new Exception("course string in exception test had invalid character: "+ch);
        }
    }
    

    private void runCourse(String course, String fileName) {
        Exception testException = null;
        File path = new File(sourceDataFolder, fileName);
        if (!path.exists()) {
            tr.markFailed(fileName, "the test file is missing from source folder: "+path);
            return;
        }
        try {
            callHigher(course);
        }
        catch (Exception e) {
            testException = e;
        }
        
        try {
            JSONObject exObj = JSONException.convertToJSON(testException, "Test file "+fileName+" for course "+course);        
            JSONObject answer = JSONObject.readFromFile(path);
            File outputFile2 = new File(tr.getProperty("testoutput", null), fileName);
            exObj.writeToFile(outputFile2);
            String s1 = exObj.toString();
            String s2 = answer.toString();
            if (s1.equals(s2)) {
                tr.markPassed(fileName);
            }
            else {
                tr.markFailed(fileName, "read the file for details: "+path);
            }
        }
        catch (Exception e) {
            System.out.println("FATAL ERROR: "+e);
        }
    }

    public static void main(String args[]) {
        TestExceptions thisTest = new TestExceptions();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }

}
