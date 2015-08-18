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

package org.workcast.mendocinotest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import org.workcast.mendocino.Mel;
import org.workcast.mendocino.Schema;
import org.workcast.mendocino.SchemaGen;
import org.workcast.testframe.TestRecorder;
import org.workcast.testframe.TestRecorderText;
import org.workcast.testframe.TestSet;

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

        writeFileAndCompare(me, "UP_Test001.xml");

        userprofile.setScalar("username", "Christopher Columbus");
        testScalar(userprofile, "username", "Christopher Columbus", "modified user profile object");
        testScalar(userprofile, "lastlogin", "1244512041541", "modified user profile object");

        writeFileAndCompare(me, "UP_Test002.xml");

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

        writeFileAndCompare(generatedSchema, "UP_Test003.xml");
    }

    public void TestEmpty() throws Exception {
        Mel testTree = Mel.createEmpty("library", Mel.class);
        writeFileAndCompare(testTree, "constTest001.xml");

        Mel book1 = testTree.addChild("book", Mel.class);
        writeFileAndCompare(testTree, "constTest002.xml");

        book1.setScalar("title", "The Black Swan");
        writeFileAndCompare(testTree, "constTest003.xml");

        book1.setScalar("author", "Nicholas Taleb");
        writeFileAndCompare(testTree, "constTest004.xml");

        Vector<String> stores = new Vector<String>();
        stores.add("Barnes & Noble");
        stores.add("Amazon");
        stores.add("Hicklebees");
        stores.add("Target");
        book1.setVector("stores", stores);
        writeFileAndCompare(testTree, "constTest005.xml");

        Mel book2 = testTree.addChild("book", Mel.class);
        writeFileAndCompare(testTree, "constTest006.xml");

        book2.setVector("stores", stores);
        writeFileAndCompare(testTree, "constTest007.xml");

        book2.setScalar("author", "L Frank Baum");
        writeFileAndCompare(testTree, "constTest008.xml");

        book2.setScalar("title", "Wizard of Oz");
        writeFileAndCompare(testTree, "constTest009.xml");

        book2.setScalar("author", "L. Frank Baum");
        writeFileAndCompare(testTree, "constTest010.xml");

        book1.setScalar("length", "225");
        book2.setScalar("length", "350");
        writeFileAndCompare(testTree, "constTest011.xml");

        // now test that setting a value to a scalar removes it from the file
        book1.setScalar("author", null);
        writeFileAndCompare(testTree, "constTest012.xml");

        Mel reading1 = book1.addChild("reading", Mel.class);
        Mel reading2 = book1.addChild("reading", Mel.class);
        Mel reading3 = book1.addChild("reading", Mel.class);

        reading1.setAttribute("date", "5/15/2009");
        reading3.setAttribute("date", "7/15/2009");
        writeFileAndCompare(testTree, "constTest013.xml");

        reading1.setScalar("readby", "Mark");
        reading2.setScalar("readby", "Joe");
        reading3.setScalar("readby", "Alex");
        writeFileAndCompare(testTree, "constTest014.xml");

        reading1.setScalar("rating", "A+");
        reading2.setScalar("rating", "B");
        reading3.setScalar("rating", "D-");
        writeFileAndCompare(testTree, "constTest015.xml");

    }

    public void TestReadWrite() throws Exception {
        File sourceFolder = new File(tr.getProperty("source", null), "testdata");

        File sourceFile = new File(sourceFolder, "UserProfiles.xml");
        Mel test = Mel.readFile(sourceFile, Mel.class);
        writeFileAndCompare(test, "dataFile001.xml");

        test = Mel.readFile(new File(sourceFolder, "web.xml"), Mel.class);
        writeFileAndCompare(test, "dataFile002.xml");

        test = Mel.readFile(new File(sourceFolder, "TroubleTicket.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile003.xpdl");

        test = Mel.readFile(new File(sourceFolder, "FujitsuExample1_x2.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile004.xpdl");

        test = Mel.readFile(new File(sourceFolder, "simpleProcess2a_mod.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile005.xpdl");

        test = Mel.readFile(new File(sourceFolder, "simpleProcess2a.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile006.xpdl");

        test = Mel.readFile(new File(sourceFolder, "Loyalty_updated_Mar3.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile007.xpdl");

        test = Mel.readFile(new File(sourceFolder, "simplefuj2new_with2008.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile008.xpdl");

        test = Mel.readFile(new File(sourceFolder, "simplefuj2new_with2008b.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile009.xpdl");

        test = Mel.readFile(new File(sourceFolder, "Loyalty.xpdl"), Mel.class);
        writeFileAndCompare(test, "dataFile010.xpdl");

    }

    public void testGenSchema() throws Exception {
        File sourceFolder = new File(tr.getProperty("source", null), "testdata");
        Mel me = Mel.readFile(new File(sourceFolder, "Loyalty.xpdl"), Mel.class);
        writeFileAndCompare(me, "GEN_Test001.xml");

        Schema generatedSchema = SchemaGen.generateFor(me);
        writeFileAndCompare(generatedSchema, "GEN_Test002.sxs");

        me = Mel.readFile(new File(sourceFolder, "RawFeed1.rss"), Mel.class);
        writeFileAndCompare(me, "GEN_Test003.rss");

        me.eliminateCData();
        writeFileAndCompare(me, "GEN_Test004.rss");

        generatedSchema = SchemaGen.generateFor(me);
        writeFileAndCompare(generatedSchema, "GEN_Test005.sxs");
    }

    public void writeFileAndCompare(Mel me, String fileName) throws Exception {
        me.reformatXML();

        File outputFile = new File(tr.getProperty("testoutput", null), fileName);
        me.writeToFile(outputFile);

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
        int charCount = 0;
        while (b1 >= 0 && b2 >= 0) {
            charCount++;
            if (b1 != b2) {
                tr.markFailed(note, "file are different at character number " + charCount);
                fis1.close();
                fis2.close();
                return;
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

            File outputFile = new File(testout, "output_test1.txt");
            if (outputFile.exists()) {
                outputFile.delete();
            }
            tr = new TestRecorderText(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"),
                    true, new String[0], ".", props);
            Test1 t1 = new Test1();
            t1.runTests(tr);
        }
        catch (Exception e) {
            System.out.print("\n\n\n====================================================");
            System.out.print("\nEXCEPTION CAUGHT AT MAIN LEVEL:\n");
            e.printStackTrace(System.out);
        }
        if (tr!=null) {
            System.out.print("\n\n\n====================================================");
            System.out.print("\n               FINISHED TEST1 RUN");
            System.out.print("\n====================================================");
            System.out.print("\n Number PASSED: "+tr.passedCount());
            System.out.print("\n Number FAILED: "+tr.failedCount());
            System.out.print("\n Number FATAL:  "+tr.fatalCount());
            System.out.print("\n====================================================\n");
        }
    }
}
