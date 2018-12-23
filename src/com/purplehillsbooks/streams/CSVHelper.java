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

package com.purplehillsbooks.streams;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps to read and write a CSV file, all methods are static writeLine:
 * Converts list of String values into a line of a CSV file parseLine: read a
 * line from a LineNumberReader and return the list of Strings
 *
 * That should be all you need. Create or open the file & streams yourself from
 * whatever source you need to read from.. Everything in this class works on
 * characters, and not bytes.
 */
public class CSVHelper {

    /**
     * Just a convenience method that iterates teh rows of a table and outputs
     * to a writer which is presumably a CSV file.
     */
    public static void writeTable(Writer w, List<List<String>> table) throws Exception {
        for (int i=0; i<table.size(); i++) {
            List<String> row = table.get(i);
            writeLine(w, row);
        }
    }

    /**
     * Write a single row of a CSV table, all values are quoted
     */
    public static void writeLine(Writer w, List<String> values) throws Exception {
        boolean firstVal = true;
        for (String val : values) {
            if (!firstVal) {
                w.write(",");
            }
            w.write("\"");
            for (int i = 0; i < val.length(); i++) {
                char ch = val.charAt(i);
                if (ch == '\"') {
                    w.write("\""); // extra quote
                }
                w.write(ch);
            }
            w.write("\"");
            firstVal = false;
        }
        w.write("\n");
    }


    /**
    * returns a row of values as a list
    * returns null if you are past the end of the line
    */
    public static List<String> parseLine(Reader r) throws Exception {
        int ch = r.read();
        while (ch == '\r') {
            //ignore linefeed characters wherever they are, particularly just before end of file
            ch = r.read();
        }
        if (ch<0) {
            return null;
        }
        ArrayList<String> store = new ArrayList<String>();
        StringBuilder curVal = new StringBuilder();
        boolean inquotes = false;
        boolean started = false;
        while (ch>=0) {
            if (inquotes) {
                started=true;
                if (ch == '\"') {
                    inquotes = false;
                }
                else {
                    curVal.append((char)ch);
                }
            }
            else {
                if (ch == '\"') {
                    inquotes = true;
                    if (started) {
                        // if this is the second quote in a value, add a quote
                        // this is for the double quote in the middle of a value
                        curVal.append('\"');
                    }
                }
                else if (ch == ',') {
                    store.add(curVal.toString());
                    curVal = new StringBuilder();
                    started = false;
                }
                else if (ch == '\r') {
                    //ignore LF characters
                }
                else if (ch == '\n') {
                    //end of a line, break out
                    break;
                }
                else {
                    curVal.append((char)ch);
                }
            }
            ch = r.read();
        }
        store.add(curVal.toString());
        return store;
    }
}
