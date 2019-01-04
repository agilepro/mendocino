package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import com.purplehillsbooks.json.JSONDiff;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.streams.CSVHelper;
import com.purplehillsbooks.streams.MemFile;
import com.purplehillsbooks.streams.StreamHelper;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class TestJSONDiff implements TestSet {

    TestRecorder tr;
    JSONDiff jdFull;
    JSONDiff jdLtd;

    public TestJSONDiff() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;
        jdFull = new JSONDiff(true);
        jdLtd  = new JSONDiff(false);
        diffCases();
    }


    private void diffCases() throws Exception {
        JSONObject ob1 = new JSONObject();
        JSONObject ob2 = new JSONObject();

        JSONDiff jdFull = new JSONDiff(true);
        JSONDiff jdLtd  = new JSONDiff(false);

        //test completely empty objects
        doit("JSONDiff-test1", ob1, ob2);

        ob1.put("telephone", "555-444-3322");
        doit("JSONDiff-test2", ob1, ob2);

        ob2.put("telephone", "555-444-3322");
        doit("JSONDiff-test3", ob1, ob2);

        ob2.put("extra", "This is only in the second object");
        doit("JSONDiff-test4", ob1, ob2);

        JSONObject weather = new JSONObject();
        weather.put("temp", "warm");
        weather.put("humidity", "98%");
        weather.put("wind", "light");

        ob1.put("weather", weather);
        doit("JSONDiff-test5", ob1, ob2);

        JSONObject weather2 = new JSONObject();
        weather2.put("temp", "warm");
        ob2.put("weather", weather2);
        doit("JSONDiff-test6", ob1, ob2);


        weather2.put("humidity", "98%");
        doit("JSONDiff-test7", ob1, ob2);


        weather2.put("wind", "light");
        doit("JSONDiff-test8", ob1, ob2);


    }

    private void doit(String rootFile, JSONObject ob1, JSONObject ob2) throws Exception {

        List<List<String>> table = jdFull.createDiff(ob1, ob2);
        compareToFile(rootFile+"a.csv", table);
        table = jdLtd.createDiff(ob1, ob2);
        compareToFile(rootFile+"b.csv", table);
    }

    private void compareToFile(String fileName, List<List<String>> table) throws Exception {
        MemFile mf = new MemFile();
        Writer w = mf.getWriter();
        CSVHelper.writeTable(w, table);
        w.flush();
        w.close();


        File outputFile1 = new File(tr.getProperty("testoutput", null), fileName);
        StreamHelper.copyReaderToFile(mf.getReader(), outputFile1, "UTF-8");

        File sourceFolder = new File(tr.getProperty("source", null), "testdata");
        File inputFile1 = new File(sourceFolder, fileName);
        if (!inputFile1.exists()) {
            tr.markFailed("JSONDiff test"+fileName, "test file has not been created: "+inputFile1);
            return;
        }
        FileInputStream fis = new FileInputStream(inputFile1);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        Reader r2 = mf.getReader();
        int ch = isr.read();
        int charPos = 0;
        while (ch>=0) {
            charPos++;
            int ch2 = r2.read();
            if (ch!=ch2) {
                isr.close();
                tr.markFailed("JSONDiff test"+fileName, "comparison failed on character "+charPos);
                return;
            }
            ch = isr.read();
        }
        tr.markPassed("JSONDiff test"+fileName);
        isr.close();
    }

    public static void main(String args[]) {
        TestJSONDiff thisTest = new TestJSONDiff();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }


}
