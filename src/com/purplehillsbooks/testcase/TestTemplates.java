package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.temps.TemplateJSONRetriever;
import com.purplehillsbooks.temps.TemplateStreamer;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class TestTemplates implements TestSet {

    TestRecorder tr;

    public TestTemplates() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;
        testGenTemplate("TemplateSimple");
        testGenTemplate("TemplateCompound");
        testGenTemplate("TemplateArray");
        testGenTemplate("TemplateLoop");
        testGenTemplate("TemplateCondition");
    }


    private void testGenTemplate(String namePart) throws Exception {
        File sourceFolder = new File(tr.getProperty("source", null), "testdata");
        File outputFolder = new File(tr.getProperty("testoutput", null));

        File sourceTemplate = new File(sourceFolder, namePart + ".html");
        File outputResult = new File(outputFolder, namePart + ".html");
        Writer out = new OutputStreamWriter(  new FileOutputStream(outputResult), "UTF-8");
        Reader tempIn = new InputStreamReader( new FileInputStream( sourceTemplate ), "UTF-8");
        JSONObject simpleData = JSONObject.readFromFile(new File(sourceFolder, namePart + "Data.json"));

        TemplateJSONRetriever tjr = new TemplateJSONRetriever(simpleData);
        TemplateStreamer.streamTemplate(out, tempIn, tjr);

        tempIn.close();
        out.close();
        tr.markPassed(namePart + " Template Streamed");
    }






    public static void main(String args[]) {
        TestTemplates thisTest = new TestTemplates();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }

}
