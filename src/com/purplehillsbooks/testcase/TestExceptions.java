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
    
    private void compareJSON(JSONObject j1, JSONObject j2, String id) {
        String s1 = j1.toString();
        String s2 = j2.toString();
        if (s1.equals(s2)) {
            tr.markPassed(id);
        }
        else {
            tr.markFailed(id, "read the file for details: "+id);
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
            compareJSON(exObj, answer, fileName);
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
