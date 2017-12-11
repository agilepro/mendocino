package com.purplehillsbooks.testcase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.purplehillsbooks.streams.CSVHelper;
import com.purplehillsbooks.streams.MemFile;
import com.purplehillsbooks.testframe.TestRecorder;
import com.purplehillsbooks.testframe.TestRecorderText;
import com.purplehillsbooks.testframe.TestSet;

/*
 *
 * Author: Keith Swenson
 * Copyright: Keith Swenson, all rights reserved
 * License: This code is made available under the GNU Lesser GPL license.
 */
public class Test2 implements TestSet {

    TestRecorder tr;

    public Test2() {
    }

    public void runTests(TestRecorder newTr) throws Exception {
        tr = newTr;
        regularCases();
        randomizedCases();
    }


    private void regularCases() throws Exception {

        StringBuffer tc1 = new StringBuffer();
        tc1.append("a,b,c,d\n");
        tc1.append("\"a\",\"b\",\"c\",\"d\"\n");
        tc1.append("\"a\",b,\"c\",d");

        String sbx = tc1.toString();
        byte[] buf = sbx.getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(buf);

        Reader fr = new InputStreamReader(is, "UTF-8");
        LineNumberReader lnr = new LineNumberReader(fr);

        List<String> vals = CSVHelper.parseLine(lnr);
        int count = 0;
        while (vals!=null) {
            testVal(vals.size(), 4, "Expected to get four values back");
            testVal(vals.get(0), "a", "First value should be a letter a");
            testVal(vals.get(1), "b", "First value should be a letter b");
            testVal(vals.get(2), "c", "First value should be a letter c");
            testVal(vals.get(3), "d", "First value should be a letter d");
            count++;
            vals = CSVHelper.parseLine(lnr);
        }
        testVal(count, 3, "Expected to get three rows back");
    }


    private void randomizedCases() throws Exception {

        Vector<String> sourceVals = new Vector<String>();
        sourceVals.add("a");              //one letter
        sourceVals.add("bbbbbbbbbbbbb");  //bunch of letters
        sourceVals.add("c c c c c");      //letters and spaces
        sourceVals.add("     ");          //five spaceas
        sourceVals.add("a \"big\" exam"); //quoted word in middle
        sourceVals.add("\"");             //just a quote
        sourceVals.add("\"a,b\"");        //reported example bug case
        sourceVals.add("x,y,z");          //value with commas
        sourceVals.add("z,\"y\",z");      //commas and quoted word
        sourceVals.add(" space ");        //spaces at beginning and end
        sourceVals.add(",");              //just a comma
        sourceVals.add(",oo,");           //comma at beginning and end
        sourceVals.add("two\nlines");     //value with a newline in it
        sourceVals.add("x,,,z");          //multiple commas
        sourceVals.add("three\n\nlines"); //multiple newlines in it
        sourceVals.add("two\"\n\"lines"); //quoted newline case
        sourceVals.add("http://test/case"); //slashes
        sourceVals.add("");               //null string
        sourceVals.add("\n");             //just a return
        sourceVals.add("\nword");         //return at start
        sourceVals.add("word\n");         //return at end
        sourceVals.add("\nword\n");       //return at beginning and end
        sourceVals.add(" ,");             //space and a comma
        sourceVals.add(", ");             //comma and a space
        sourceVals.add(" , ");            //spaces surrounding comma
        sourceVals.add(", ,");            //commas surrounding space
        sourceVals.add("\n,");            //newline comma
        sourceVals.add(",\n");            //comma newline
        sourceVals.add(",\n,");           //newline surrounded by commas
        sourceVals.add("\n,\n");          //comma surrounded by newlines

        testRandomMatrix(5,5,5,5, sourceVals, "5x5 matrix ");
        testRandomMatrix(1,1,1,1, sourceVals, "1x1 matrix ");
        testRandomMatrix(10,20,1,10, sourceVals, "random-1 ");
        testRandomMatrix(10,20,1,10, sourceVals, "random-2 ");
        testRandomMatrix(10,20,1,10, sourceVals, "random-3 ");
    }

    private void testRandomMatrix(int rowMin, int rowMax, int colMin, int colMax,  Vector<String> sourceVals, String desc) throws Exception {


        Vector< Vector <String>> matrix = generateMatrix(rowMin, rowMax, colMin, colMax,  sourceVals);

        MemFile mf = new MemFile();
        Writer w = mf.getWriter();

        for (Vector<String> row : matrix) {
            CSVHelper.writeLine(w, row);
        }
        w.flush();

        Reader r = mf.getReader();
        LineNumberReader lnr = new LineNumberReader(r);
        int count = 0;
        for(Vector<String> row : matrix) {

            List<String> parsedRow = CSVHelper.parseLine(lnr);
            if (parsedRow==null) {
                throw new Exception(desc+"- Fatal error, the generated CSV file does not have a row "+count);
            }
            testVal(parsedRow.size(), row.size(), desc+"- output row and input row should both be "+row.size());

            for (int i=0; i<row.size(); i++) {
                String source = row.get(i);
                String result = parsedRow.get(i);
                testVal(result, source, desc+"- matrix ("+count+","+i+") result should be the same as source");
            }

            count++;
        }

        List<String> extraRow = CSVHelper.parseLine(lnr);
        testNull(extraRow, desc+"- null means no additional line in file");

    }

    private Vector<Vector<String>> generateMatrix(int rowMin, int rowMax, int colMin, int colMax,  Vector<String> sourceVals) {
        Vector< Vector <String>> matrix = new Vector< Vector <String>>();
        Random rand = new Random();
        int rows = rowMin;
        if (rowMax>rowMin) {
            rows += rand.nextInt(rowMax-rowMin);
        }
        for (int i=0; i<rows; i++) {

            int cols = colMin;
            if (colMax>colMin) {
                cols += rand.nextInt(colMax-colMin);
            }
            Vector<String> oneRow = new Vector<String>();
            for (int j=0; j<cols; j++) {
                oneRow.add( sourceVals.get( rand.nextInt(sourceVals.size()) ));
            }
            matrix.add(oneRow);
        }
        return matrix;
    }




    private void testNull(Object value, String description) throws Exception {
        if (value == null) {
            tr.markPassed("Is Null: " + description);
        }
        else {
            tr.markFailed("Is Null: " + description,
                    "Test failure, expected a null but did not get one for the situation: "
                            + description);
        }
    }

    private void testVal(int value, int expectedValue, String description) throws Exception {
        if (value == expectedValue) {
            tr.markPassed("Value: " + description);
        }
        else {
            tr.markFailed("Value: " + description, "Test failure, expected the value '"
                    + expectedValue + "' but instead got the value '" + value
                    + "' for the situation: " + description);
        }
    }
    private void testVal(String value, String expectedValue, String description) throws Exception {
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
    private void writeLiteralValue(String varname, String value) {
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



    public static void main(String args[]) {
        Test2 thisTest = new Test2();
        TestRecorderText.parseArgsRunTests(args, thisTest);
    }

}
