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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import com.purplehillsbooks.json.Dom2JSON;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.json.JSONTokener;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;
import com.purplehillsbooks.xml.Mel;
import com.purplehillsbooks.xml.Schema;
import com.purplehillsbooks.xml.SchemaGen;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class Test1 implements TestSet {

    TestRecorder tr;

    public Test1() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;

        TestUserProfileFile();
        TestEmpty();
        TestReadWrite();
        testGenSchema();
    }

    public void TestUserProfileFile() throws Exception {
        Mel me = Mel.readInputStream(getData1Stream(), Mel.class);

        Enumeration<Mel> userprofile_enum = me.getChildren("userprofile").elements();

        Hashtable<String,Integer> hints = new Hashtable<String,Integer>();
        hints.put("userprofile", new Integer(3));
        hints.put("data", new Integer(3));
        hints.put("idrec", new Integer(3));
        hints.put("container", new Integer(3));
        hints.put("attr", new Integer(3));
        hints.put("contains", new Integer(3));


        Mel userprofile = userprofile_enum.nextElement();
        testNotNull(userprofile, "first user profile object");
        testVal(userprofile.getName(), "userprofile", "first user profile object: name");
        testVal(userprofile.getAttribute("id"), "MOBQNTGYF", "first user profile object: id");
        testVal(userprofile.getScalar(""), "", "first user profile object: lastlogin");
        testScalar(userprofile, "lastlogin", "1244434616875", "first user profile object");
        testScalar(userprofile, "lastupdated", "1241018683687", "first user profile object");
        testScalar(userprofile, "username", "AAA", "first user profile object");
        testScalar(userprofile, "nonexistantkey", "", "first user profile object");

        // get the second user profile element
        userprofile = userprofile_enum.nextElement();
        testNotNull(userprofile, "second user profile object");
        testVal(userprofile.getName(), "userprofile", "second user profile object: name");
        testScalar(userprofile, "username", "BBB", "second user profile object");
        testScalar(userprofile, "nonexistantkey_xyz", "", "first user profile object");

        Mel idrec = userprofile.getChild("idrec", 0);
        testNotNull(idrec, "second user idrec object");
        testVal(idrec.getAttribute("loginid"), "aaa@gmail.com", "second user idrec object login id");

        // get the third user profile element
        userprofile = userprofile_enum.nextElement();
        testNotNull(userprofile, "third user profile object");
        testVal(userprofile.getName(), "userprofile", "third user profile object: name");
        testScalar(userprofile, "username", "CCC", "third user profile object");
        testScalar(userprofile, "lastlogin", "1244512041541", "third user profile object");

        writeBothStylesAndCompare(me, "UP_Test001", hints);

        userprofile.setScalar("username", "Christopher Columbus");
        testScalar(userprofile, "username", "Christopher Columbus", "modified user profile object");
        testScalar(userprofile, "lastlogin", "1244512041541", "modified user profile object");

        writeBothStylesAndCompare(me, "UP_Test002", hints);

        Schema schema1 = Mel.readInputStream(getData2Stream(), Schema.class);
        testVal(schema1.getName(), "schema", "root element of schema file name");

        me.setSchema(schema1);

        testValidation(me, "Validation: initial file", "");

        userprofile.setScalar("username_bogus", "CCC_XXX");

        testValidation(
                me,
                "Validation: after making bogus setting",
                "java.lang.Exception: Element 'userprofile' has a child element 'username_bogus' that is not in the schema."
                        + "\n");

        idrec.setScalar("second_bogus", "on idrec");

        testValidation(
                me,
                "Validation: after making bogus setting",
                "java.lang.Exception: Element 'idrec' has a child element 'second_bogus' that is not in the schema."
                        + "\njava.lang.Exception: Element 'userprofile' has a child element 'username_bogus' that is not in the schema."
                        + "\n");

        Schema generatedSchema = SchemaGen.generateFor(me);

        writeBothStylesAndCompare(generatedSchema, "UP_Test003", hints);
    }

    public void TestEmpty() throws Exception {
        Hashtable<String,Integer> hints = new Hashtable<String,Integer>();
        hints.put("book", new Integer(3));
        hints.put("library", new Integer(3));
        hints.put("stores", new Integer(1));
        hints.put("reading", new Integer(3));

        Mel testTree = Mel.createEmpty("library", Mel.class);
        writeBothStylesAndCompare(testTree, "constTest001", hints);

        Mel book1 = testTree.addChild("book", Mel.class);
        writeBothStylesAndCompare(testTree, "constTest002", hints);

        book1.setScalar("title", "The Black Swan");
        writeBothStylesAndCompare(testTree, "constTest003", hints);

        book1.setScalar("author", "Nicholas Taleb");
        writeBothStylesAndCompare(testTree, "constTest004", hints);

        Vector<String> stores = new Vector<String>();
        stores.add("Barnes & Noble");
        stores.add("Amazon");
        stores.add("Hicklebees");
        stores.add("Target");
        book1.setVector("stores", stores);
        writeBothStylesAndCompare(testTree, "constTest005", hints);

        Mel book2 = testTree.addChild("book", Mel.class);
        writeBothStylesAndCompare(testTree, "constTest006", hints);

        book2.setVector("stores", stores);
        writeBothStylesAndCompare(testTree, "constTest007", hints);

        book2.setScalar("author", "L Frank Baum");
        writeBothStylesAndCompare(testTree, "constTest008", hints);

        book2.setScalar("title", "Wizard of Oz");
        writeBothStylesAndCompare(testTree, "constTest009", hints);

        book2.setScalar("author", "L. Frank Baum");
        writeBothStylesAndCompare(testTree, "constTest010", hints);

        book1.setScalar("length", "225");
        book2.setScalar("length", "350");
        writeBothStylesAndCompare(testTree, "constTest011", hints);

        // now test that setting a value to a scalar removes it from the file
        book1.setScalar("author", null);
        writeBothStylesAndCompare(testTree, "constTest012", hints);

        Mel reading1 = book1.addChild("reading", Mel.class);
        Mel reading2 = book1.addChild("reading", Mel.class);
        Mel reading3 = book1.addChild("reading", Mel.class);

        reading1.setAttribute("date", "5/15/2009");
        reading3.setAttribute("date", "7/15/2009");
        writeBothStylesAndCompare(testTree, "constTest013", hints);

        reading1.setScalar("readby", "Mark");
        reading2.setScalar("readby", "Joe");
        reading3.setScalar("readby", "Alex");
        writeBothStylesAndCompare(testTree, "constTest014", hints);

        reading1.setScalar("rating", "A+");
        reading2.setScalar("rating", "B");
        reading3.setScalar("rating", "D-");
        writeBothStylesAndCompare(testTree, "constTest015", hints);

    }

    public void TestReadWrite() throws Exception {
        File sourceFolder = new File(tr.getProperty("source", null), "testdata");
        Hashtable<String,Integer> hints = new Hashtable<String,Integer>();
        hints.put("userprofile", new Integer(3));
        hints.put("servlet", new Integer(3));
        hints.put("servlet-mapping", new Integer(3));

        //these are the XPDL hints
        hints.put("Pool", new Integer(3));
        hints.put("NodeGraphicsInfo", new Integer(3));
        hints.put("Lane", new Integer(3));
        hints.put("TransitionRef", new Integer(3));
        hints.put("Activity", new Integer(3));
        hints.put("Transition", new Integer(3));
        hints.put("Coordinates", new Integer(3));
        hints.put("WorkflowProcess", new Integer(3));
        hints.put("TransitionRestriction", new Integer(3));
        hints.put("Transition", new Integer(3));
        hints.put("ExtendedAttribute", new Integer(3));
        hints.put("Participant", new Integer(3));
        hints.put("Connector", new Integer(3));



        File sourceFile = new File(sourceFolder, "UserProfiles.xml");
        Mel test = Mel.readFile(sourceFile, Mel.class);
        writeBothStylesAndCompare(test, "dataFile001", hints);

        test = Mel.readFile(new File(sourceFolder, "web.xml"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile002", hints);

        test = Mel.readFile(new File(sourceFolder, "TroubleTicket.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile003", hints);

        test = Mel.readFile(new File(sourceFolder, "FujitsuExample1_x2.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile004", hints);

        test = Mel.readFile(new File(sourceFolder, "simpleProcess2a_mod.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile005", hints);

        test = Mel.readFile(new File(sourceFolder, "simpleProcess2a.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile006", hints);

        test = Mel.readFile(new File(sourceFolder, "Loyalty_updated_Mar3.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile007", hints);

        test = Mel.readFile(new File(sourceFolder, "simplefuj2new_with2008.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile008", hints);

        test = Mel.readFile(new File(sourceFolder, "simplefuj2new_with2008b.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile009", hints);

        test = Mel.readFile(new File(sourceFolder, "Loyalty.xpdl"), Mel.class);
        writeBothStylesAndCompare(test, "dataFile010", hints);

    }

    public void testGenSchema() throws Exception {
        Hashtable<String,Integer> hints = new Hashtable<String,Integer>();
        hints.put("userprofile", new Integer(3));
        hints.put("servlet", new Integer(3));
        hints.put("servlet-mapping", new Integer(3));

        //these are the XPDL hints
        hints.put("Pool", new Integer(3));
        hints.put("NodeGraphicsInfo", new Integer(3));
        hints.put("Lane", new Integer(3));
        hints.put("TransitionRef", new Integer(3));
        hints.put("Activity", new Integer(3));
        hints.put("Transition", new Integer(3));
        hints.put("Coordinates", new Integer(3));
        hints.put("WorkflowProcess", new Integer(3));
        hints.put("TransitionRestriction", new Integer(3));
        hints.put("Transition", new Integer(3));
        hints.put("ExtendedAttribute", new Integer(3));
        hints.put("Participant", new Integer(3));

        //these are minimal schema elements
        hints.put("container", new Integer(3));
        hints.put("contains", new Integer(3));
        hints.put("attr", new Integer(3));

        //these are minimal rss elements
        hints.put("item", new Integer(3));
        hints.put("content", new Integer(3));
        hints.put("category", new Integer(3));



        File sourceFolder = new File(tr.getProperty("source", null), "testdata");
        Mel me = Mel.readFile(new File(sourceFolder, "Loyalty.xpdl"), Mel.class);
        writeBothStylesAndCompare(me, "GEN_Test001", hints);

        Schema generatedSchema = SchemaGen.generateFor(me);
        writeBothStylesAndCompare(generatedSchema, "GEN_Test002", hints);

        me = Mel.readFile(new File(sourceFolder, "RawFeed1.rss"), Mel.class);
        writeBothStylesAndCompare(me, "GEN_Test003", hints);

        me.eliminateCData();
        writeBothStylesAndCompare(me, "GEN_Test004", hints);

        generatedSchema = SchemaGen.generateFor(me);
        writeBothStylesAndCompare(generatedSchema, "GEN_Test005", hints);
    }


    public void writeBothStylesAndCompare(Mel me, String fileNamePart, Hashtable<String,Integer> hints) throws Exception {
        writeFileAndCompare(me, fileNamePart + ".xml");
        writeJSONAndCompare(me, fileNamePart + ".json", hints);
    }


    public void writeFileAndCompare(Mel me, String fileName) throws Exception {
        me.reformatXML();

        File outputFile = new File(tr.getProperty("testoutput", null), fileName);
        me.writeToFile(outputFile);

        compareFiles(outputFile, fileName);
    }

    static private Random rand = new Random();

    /**
     * Convert the XML to JSON
     * write it to a file
     * read that file
     * write it again and compare to archive
     */
    public void  writeJSONAndCompare(Mel me, String fileName, Hashtable<String,Integer> hints) throws Exception {
        JSONObject jsonRep = Dom2JSON.convertElementToJSON(me.getElement(), hints);

        File outputFile = new File(tr.getProperty("testoutput", null), fileName);
        File intermediateFile = new File(tr.getProperty("testoutput", null), fileName+"$testtmp$");

        //randomly choose one method or the other.  I don't like randomness in tests
        //but there is too much overhead in testing every case both ways.
        //If they both work, they should be exactly equal....
        //we just want to know that they both work.  If this test starts to fail
        //we may need to isolate to one or the other.
        //
        // FIRST, write to intermediate file
        if (rand.nextBoolean()) {
            FileOutputStream fos = new FileOutputStream(intermediateFile);
            Writer w = new OutputStreamWriter(fos, "UTF-8");
            jsonRep.write(w, 2, 0);
            w.close();
            fos.close();
        }
        else {
            jsonRep.writeToFile(intermediateFile);
        }

        //SECOND read from intermediate
        if (rand.nextBoolean()) {
            FileInputStream fis = new FileInputStream(intermediateFile);
            JSONTokener jt = new JSONTokener(fis);
            jsonRep = new JSONObject(jt);
            fis.close();
        }
        else {
            jsonRep = JSONObject.readFromFile(intermediateFile);
        }

        //THIRD, write to final file
        if (rand.nextBoolean()) {
            FileOutputStream fos = new FileOutputStream(outputFile);
            Writer w = new OutputStreamWriter(fos, "UTF-8");
            jsonRep.write(w, 2, 0);
            w.close();
            fos.close();
        }
        else {
            jsonRep.writeToFile(outputFile);
        }

        //FOURTH delete the intermediate so it is not in the archive
        intermediateFile.delete();

        compareFiles(outputFile, fileName);
    }


    public void compareFiles(File outputFile, String fileName) throws Exception {

        String note = "Compare output to " + fileName;
        File compareFolder = new File(tr.getProperty("source", null), "testoutput");
        File compareFile = new File(compareFolder, fileName);
        if (!compareFile.exists()) {
            tr.markFailed(note, "file to compare to is missing from: " + compareFile.toString());
            return;
        }

        FileInputStream fis1 = new FileInputStream(outputFile);
        FileInputStream fis2 = new FileInputStream(compareFile);

        int b1 = fis1.read();
        int b2 = fis2.read();
        int charCount = 1;
        int lineCount = 1;
        while (b1 >= 0 && b2 >= 0) {
            if (b1 != b2) {
                tr.markFailed(note, "file are different at number " + lineCount + " and character "+charCount);
                fis1.close();
                fis2.close();
                return;
            }
            if (b1=='\n') {
                lineCount++;
                charCount = 1;
            }
            else {
                charCount++;
            }
            b1 = fis1.read();
            b2 = fis2.read();
        }

        fis1.close();
        fis2.close();

        if (b1 >= 0) {
            tr.markFailed(note, "new file has more characters in it than the old file");
            return;
        }
        if (b2 >= 0) {
            tr.markFailed(note, "old file has more characters in it than the new file");
            return;
        }

        tr.markPassed(note);
    }

    public void testValidation(Mel me, String note, String expectedVal) throws Exception {
        Vector<Exception> results = new Vector<Exception>();
        me.validate(results);
        Enumeration<Exception> ve = results.elements();
        StringBuffer or = new StringBuffer();
        while (ve.hasMoreElements()) {
            or.append(ve.nextElement().toString());
            or.append("\n");
        }
        String actualVal = or.toString();

        if (compareStringIgnoringCR(expectedVal, actualVal)) {
            tr.markPassed(note);
        }
        else {
            tr.markFailed(note, "values do not match");
            writeLiteralValue("expected", expectedVal);
            writeLiteralValue("actual", actualVal);
        }
    }

    public boolean compareStringIgnoringCR(String s1, String s2) {
        int i1 = 0;
        int i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1++);
            while (c1 == 13 && i1 < s1.length()) {
                c1 = s1.charAt(i1++);
            }
            char c2 = s2.charAt(i2++);
            while (c2 == 13 && i2 < s2.length()) {
                c2 = s2.charAt(i2++);
            }
            if (c1 != c2) {
                return false;
            }
        }
        return true;
    }

    public void testOutput(Mel me, String note, String expectedVal) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        me.writeToOutputStream(baos);
        String actualVal = baos.toString("UTF-8");

        if (expectedVal.equals(actualVal)) {
            tr.markPassed(note);
        }
        else {
            tr.markFailed(note, "values do not match");
            writeLiteralValue("expected", expectedVal);
            writeLiteralValue("actual", actualVal);
        }
    }

    public void testRawXML(Mel me, String note, String expectedVal) throws Exception {
        String actualVal = me.getRawDOM();

        if (expectedVal.equals(actualVal)) {
            tr.markPassed(note);
        }
        else {
            tr.markFailed(note, "values do not match");
            writeLiteralValue("expected", expectedVal);
            writeLiteralValue("actual", actualVal);
        }
    }

    public void testNotNull(Object value, String description) throws Exception {
        if (value != null) {
            tr.markPassed("Not Null: " + description);
        }
        else {
            tr.markFailed("Not Null: " + description,
                    "Test failure, got an unexpected null for the situation: " + description);
        }
    }

    public void testNull(Object value, String description) throws Exception {
        if (value == null) {
            tr.markPassed("Is Null: " + description);
        }
        else {
            tr.markFailed("Is Null: " + description,
                    "Test failure, expected a null but did not get one for the situation: "
                            + description);
        }
    }

    public void testVal(String value, String expectedValue, String description) throws Exception {
        if (value != null && value.equals(expectedValue)) {
            tr.markPassed("Value: " + description);
        }
        else {
            tr.markFailed("Value: " + description, "Test failure, expected the value '"
                    + expectedValue + "' but instead got the value '" + value
                    + "' for the situation: " + description);
            writeLiteralValue("expected", expectedValue);
            writeLiteralValue("actual", value);
        }
    }

    public void testScalar(Mel me, String eName, String expectedValue, String description)
            throws Exception {
        String value = me.getScalar(eName);
        if (value != null && value.equals(expectedValue)) {
            tr.markPassed("testScalar (" + eName + "): " + description);
        }
        else {
            tr.markFailed("testScalar (" + eName + "): " + description,
                    "Test failure, expected the value '" + expectedValue
                            + "' but instead got the value '" + value + "' for the scaler value '"
                            + eName + "' for  " + description);
            writeLiteralValue("expected", expectedValue);
            writeLiteralValue("actual", value);
        }
    }

    public void writeShortLiteralValue(StringBuffer sb, String value) {
        sb.append("\"");
        for (int i = 0; i < value.length() && i < 20; i++) {
            char ch = value.charAt(i);
            if (ch == '"') {
                sb.append("\\\"");
            }
            else if (ch == '\\') {
                sb.append("\\\\");
            }
            else if (ch == '\n') {
                sb.append("\\n");
            }
            else if (ch == (char) 13) {
                // do output anything ... ignore these
            }
            else if (ch < 32 || ch > 128) {
                sb.append("\\u");
                addHex(sb, (ch / 16 / 16 / 16) % 16);
                addHex(sb, (ch / 16 / 16) % 16);
                addHex(sb, (ch / 16) % 16);
                addHex(sb, ch % 16);
            }
            else {
                sb.append(ch);
            }
        }
        sb.append("\"");
    }

    public void writeLiteralValue(String varname, String value) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        sb.append(varname);
        sb.append(" = \"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"') {
                sb.append("\\\"");
            }
            else if (ch == '\\') {
                sb.append("\\\\");
            }
            else if (ch == '\n') {
                sb.append("\"\n     +\"\\n");
            }
            else if (ch == (char) 13) {
                // strange workaround for literal problem
                sb.append("\"+(char)13+\"");
            }
            else if (ch < 32 || ch > 128) {
                sb.append("\\u");
                addHex(sb, (ch / 16 / 16 / 16) % 16);
                addHex(sb, (ch / 16 / 16) % 16);
                addHex(sb, (ch / 16) % 16);
                addHex(sb, ch % 16);
            }
            else {
                sb.append(ch);
            }
        }
        sb.append("\";\n");
        tr.log(sb.toString());
    }

    private void addHex(StringBuffer sb, int val) {
        if (val >= 0 && val < 10) {
            sb.append((char) (val + '0'));
        }
        else if (val >= 0 && val < 16) {
            sb.append((char) (val + 'A' - 10));
        }
        else {
            sb.append('?');
        }
    }

    public static InputStream getData1Stream() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<userprofiles>");
        sb.append("\n  <userprofile id=\"MOBQNTGYF\">");
        sb.append("\n    <homepage>http://web.com/processleaves/p/main/public.htm</homepage>");
        sb.append("\n    <lastlogin>1244434616875</lastlogin>");
        sb.append("\n    <lastupdated>1241018683687</lastupdated>");
        sb.append("\n    <username>AAA</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n  <userprofile id=\"YVXIXTGYF\">");
        sb.append("\n    <idrec loginid=\"aaa@gmail.com\"/>");
        sb.append("\n    <username>BBB</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n  <userprofile id=\"HMSBPXKYF\">");
        sb.append("\n    <idrec loginid=\"jjj@a.com\"/>");
        sb.append("\n    <idrec confirmed=\"true\" loginid=\"ddd@a.com\"/>");
        sb.append("\n    <lastlogin>1244512041541</lastlogin>");
        sb.append("\n    <username>CCC</username>");
        sb.append("\n  </userprofile>");
        sb.append("\n</userprofiles>");
        String sbx = sb.toString();
        byte[] buf = sbx.getBytes("UTF-8");
        return new ByteArrayInputStream(buf);
    }

    public static InputStream getData2Stream() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<schema>");
        sb.append("\n  <root>userprofiles</root>");
        sb.append("\n  <container name=\"userprofiles\">");
        sb.append("\n    <contains plural=\"*\">userprofile</contains>");
        sb.append("\n  </container>");
        sb.append("\n  <container name=\"userprofile\">");
        sb.append("\n    <attr name=\"id\" type=\"String\"/>");
        sb.append("\n    <contains>homepage</contains>");
        sb.append("\n    <contains>lastlogin</contains>");
        sb.append("\n    <contains>lastupdated</contains>");
        sb.append("\n    <contains>username</contains>");
        sb.append("\n    <contains plural=\"*\">idrec</contains>");
        sb.append("\n  </container>");
        sb.append("\n  <container name=\"idrec\">");
        sb.append("\n    <attr name=\"loginid\"   type=\"String\"/>");
        sb.append("\n    <attr name=\"confirmed\" type=\"String\"/>");
        sb.append("\n  </container>");
        sb.append("\n  <data name=\"lastlogin\"   type=\"Integer\"/>");
        sb.append("\n  <data name=\"lastupdated\" type=\"Integer\"/>");
        sb.append("\n  <data name=\"username\"    type=\"String\"/>");
        sb.append("\n  <data name=\"homepage\"    type=\"String\"/>");
        sb.append("\n  <data name=\"username\"    type=\"String\"/>");
        sb.append("\n</schema>");
        String sbx = sb.toString();
        byte[] buf = sbx.getBytes("UTF-8");
        return new ByteArrayInputStream(buf);
    }

    public static void main(String args[]) {
        Test1 thisTest = new Test1();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }


}
