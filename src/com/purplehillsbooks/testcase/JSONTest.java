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

package com.purplehillsbooks.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.json.JSONTokener;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class JSONTest implements TestSet {

    TestRecorder tr;

    public JSONTest() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;

        testAllWriteAndReadOperations();
    }


    private JSONObject constructCharacterJSON() throws Exception  {
        JSONObject allChars = new JSONObject();
        for (int line=0; line<150; line++) {
            StringBuffer lineBuf = new StringBuffer();
            for (int j=0; j<16; j++) {
                lineBuf.append((char)(33+(line*16)+j));
            }
            allChars.put("Val"+line, lineBuf.toString());
        }
        return allChars;
    }

    private void checkCharacterJSON(JSONObject allChars2) throws Exception {
        for (int line=0; line<150; line++) {
            StringBuffer lineBuf = new StringBuffer();
            for (int j=0; j<16; j++) {
                lineBuf.append((char)(33+(line*16)+j));
            }
            String comp = allChars2.getString("Val"+line);
            String expected = lineBuf.toString();
            if (!comp.equals(expected)) {
                tr.markFailed("character test line Val"+line, "Expected "+expected+" but got "+comp);
            }
            else {
                tr.markPassed("character test line Val"+line);
            }
        }
    }


    public void testAllWriteAndReadOperations() throws Exception {

        JSONObject allChars = constructCharacterJSON();

        File outputFile1 = new File(tr.getProperty("testoutput", null), "characterTest1.json");
        allChars.writeToFile(outputFile1);

        File outputFile2 = new File(tr.getProperty("testoutput", null), "characterTest2.json");
        FileOutputStream fos = new FileOutputStream(outputFile2);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
        allChars.write(osw,2,0);
        osw.close();


        JSONObject allChars2 = JSONObject.readFromFile(outputFile1);
        checkCharacterJSON(allChars2);

        JSONObject allChars3 = JSONObject.readFromFile(outputFile2);
        checkCharacterJSON(allChars3);

        FileInputStream fis = new FileInputStream(outputFile1);
        JSONTokener jt = new JSONTokener(fis);
        JSONObject allChars4 = new JSONObject(jt);
        checkCharacterJSON(allChars4);

        fis = new FileInputStream(outputFile2);
        jt = new JSONTokener(fis);
        JSONObject allChars5 = new JSONObject(jt);
        checkCharacterJSON(allChars5);

        fis = new FileInputStream(outputFile1);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        jt = new JSONTokener(isr);
        JSONObject allChars6 = new JSONObject(jt);
        checkCharacterJSON(allChars6);

        fis = new FileInputStream(outputFile2);
        isr = new InputStreamReader(fis, "UTF-8");
        jt = new JSONTokener(isr);
        JSONObject allChars7 = new JSONObject(jt);
        checkCharacterJSON(allChars7);

    }


    public static void main(String args[]) {
        JSONTest thisTest = new JSONTest();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }


}
