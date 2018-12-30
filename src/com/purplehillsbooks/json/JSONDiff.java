package com.purplehillsbooks.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONObject;
import com.purplehillsbooks.streams.CSVHelper;


/**
 * The purpose of this class is to produce a "DIFF" comparison of two JSON
 * files.   This is useful for JSON files which are holding internationalized
 * string values.  Two JSON objects are compared.  We assume the keys are relatively
 * unique, and that arrays are not common.   The value from one object is compared
 * to the value of the same key from the other.   Nested objects use paths which
 * are again assumed to be unique.  If there is an array, then the elements in the
 * array will be compared in exactly the order they are found, but this might or might
 * not be useful.  No attempt is made to find a canonical order for the array elements.
 *
 * The output is a three column CSV file.   The first column is the key, the second
 * is the value from the first json object, the third column is the value from the
 * second json object:
 *
 * JSON File 1:
 *
 * {
 *     "title": "title",
 *     "pgNumTot": "number of pages",
 *     "print": "print",
 *     "menu": {
 *         "open": "Open"
 *     }
 *     "missing": "missing"
 * }
 *
 * JSON File 2:
 *
 * {
 *     "title": "titre",
 *     "pgNumTot": "nombre de pages",
 *     "print": "imprimer"
 *     "menu": {
 *         "open": "Ouvre"
 *     },
 *     "extra": "superfluous"
 * }
 *
 * The output would then be:
 *
 * "title", "title", "titre"
 * "pgNumTot", "number of pages", "nombre de pages"
 * "print", "print", "imprimer"
 * "menu.open", "Open", "Ouvre"
 * "missing", "missing", "~null~"
 *
 * Note that the first file is assumed to be a stronger definition of
 * the structure.  That is, all elements in the first file will be considered
 * and followed even if they are missing from the second file.  If the key
 * points to an object in the first file and a non-object in the second file,
 * then the value will be considered to be an object and sub-elements will be
 * followed.   However a non-object in the first and an object in the second
 * will be treated like a simple value.
 * The second object is subservient to this:  extra keys in the second object
 * will be ignored in some cases.  If the key points to a string value in the
 * first object, but an object value in the second object, then
 *
 */
public class JSONDiff {

    boolean includeAll = true;

    public JSONDiff(boolean reportAll) {
        includeAll = reportAll;
    }

    /*
     * Creates a list, each element of the list is a triplet of Strings
     */
    public List<List<String>> createDiff(JSONObject ob1, JSONObject ob2) throws Exception {
        List<List<String>> table = new ArrayList<List<String>>();
        addRecursive(table, "", ob1, ob2);
        return table;
    }


    /**
     * JSONDiff <file1> <file2> [-a]
     */
    public static void main(String[] args) {
        try {
            String fileName1 = null;
            String fileName2 = null;
            boolean doAllRows = false;
            if (args.length>0) {
                fileName1 = args[0];
            }
            if (args.length>1) {
                fileName2 = args[1];
            }
            if (args.length>2) {
                if ("-a".equals(args[2])) {
                    doAllRows = true;
                }
            }
            File file1 = new File(fileName1);
            if (!file1.exists()) {
                throw new Exception("Can't file first file: "+file1.getCanonicalPath());
            }
            JSONObject obj1 = JSONObject.readFromFile(file1);
            JSONObject obj2 = new JSONObject();
            File file2 = new File(fileName2);
            if (file2.exists()) {
                obj2 = JSONObject.readFromFile(file2);
            }
            JSONDiff jdiff = new JSONDiff(doAllRows);
            List<List<String>> table = new ArrayList<List<String>>();
            jdiff.addRow(table, "DIFF", fileName1, fileName2);
            jdiff.addRecursive(table, "", obj1, obj2);

            File fileOut = new File(fileName2+"diff.csv");
            if (fileOut.exists()) {
                fileOut.delete();
            }
            FileOutputStream fos = new FileOutputStream(fileOut);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            CSVHelper.writeTable(osw, table);
            osw.flush();
            osw.close();

            //now write out the extended object
            File newSecondObject = new File(file2.getParentFile(), file2.getName()+".augment.json");
            obj2.writeToFile(newSecondObject);
        }
        catch (Exception e) {
            System.out.println("##### FATAL ENDING OF JSONDiff #####");
            e.printStackTrace();
        }
    }



    private static String smartValue(Object o) throws Exception {
        if (o==null) {
            return "~null~";
        }
        if (o instanceof String) {
            return (String)o;
        }
        if (o instanceof JSONObject) {
            return "~object~";
        }
        if (o instanceof JSONArray) {
            return "~array~";
        }
        return o.toString();
    }


    private void addRecursive(List<List<String>> table, String baseKey, JSONObject ob1, JSONObject ob2) throws Exception {
        List<String> allKeys = new ArrayList<String>();

        //a null is treated the same as an empty object
        if (ob1==null) {
            ob1 = new JSONObject();
        }
        if (ob2==null) {
            ob2 = new JSONObject();
        }

        //find all the keys and sort them
        for (String key : ob1.keySet()) {
            if (!allKeys.contains(key)) {
                allKeys.add(key);
            }
        }
        for (String key : ob2.keySet()) {
            if (!allKeys.contains(key)) {
                allKeys.add(key);
            }
        }
        Collections.sort(allKeys);

        //iterate the keys
        for (String key : allKeys) {
            Object o1 = ob1.opt(key);
            Object o2 = ob2.opt(key);
            String val1 = smartValue(o1);
            String val2 = smartValue(o2);
            if (o1 == null) {
                if (o2 == null) {
                    //there is a silly situation where you put in the JSON the null value
                    //and we want to just ignore those.
                    continue;
                }
                else if (o2 instanceof JSONObject) {
                    addRecursive(table, baseKey + key + ".", null, (JSONObject)o2);
                }
                else if (o2 instanceof JSONArray) {
                    iterateArray(table, baseKey + key + "[", null, (JSONArray)o2);
                }
                else {
                    addRow(table, baseKey + key, val1, val2);
                }
            }
            else if (o1 instanceof JSONObject) {
                if (o2!=null && o2 instanceof JSONObject) {
                    //if they are both objects then drill down
                    addRecursive(table, baseKey + key + ".", (JSONObject)o1, (JSONObject)o2);
                }
                else if (o2==null) {
                    //the object is missing to add it
                    JSONObject replace = new JSONObject();
                    ob2.put(key, replace);
                    addRecursive(table, baseKey + key + ".", (JSONObject)o1, null);
                }
                else {
                    //a conflicting value exists, so treat like null
                    addRecursive(table, baseKey + key + ".", (JSONObject)o1, null);
                }

            }
            else if (o1 instanceof JSONArray) {
                if (o2!=null && o2 instanceof JSONArray) {
                    iterateArray(table, baseKey + key + "[", (JSONArray)o1, (JSONArray)o2);
                }
                else if (o2==null) {
                    //the object is missing to add it
                    JSONArray replace = new JSONArray();
                    ob2.put(key, replace);
                    iterateArray(table, baseKey + key + "[", (JSONArray)o1, replace);
                }
                else {
                    //in all other cases have to consider o2 to be null
                    iterateArray(table, baseKey + key + "[", (JSONArray)o1, null);
                }
            }
            else {
                addRow(table, baseKey + key, val1, val2);
                if (o2==null) {
                    //in this case put a value in the place
                    System.out.println("Added value for "+key);
                    ob2.put(key, "(*)"+val1);
                }
            }
        }
    }

    private void addRow(List<List<String>> table, String v1, String v2, String v3) {
        if (includeAll || !v2.equals(v3)) {
            List<String> row = new ArrayList<String>();
            row.add(v1);
            row.add(v2);
            row.add(v3);
            table.add(row);
        }
    }

    private void iterateArray(List<List<String>> table, String baseKey, JSONArray ja1, JSONArray ja2) throws Exception {
        if (ja2==null) {
            ja2 = new JSONArray();
        }
        int size = ja1.length();
        if (ja2.length()>size) {
            size = ja2.length();
        }

        for (int i=0; i<size; i++) {
            Object o1 = null;
            if (i<ja1.length()) {
                o1 = ja1.get(i);
            }
            Object o2 = null;
            if (i<ja2.length()) {
                o2 = ja1.get(i);
            }
            if (o1==null) {
                if (o2==null) {
                    continue;
                }
                else if (o2 instanceof JSONObject) {
                    addRecursive(table, baseKey+i+"]", null, (JSONObject)o2);
                }
            }
            else if (o1 instanceof JSONObject) {
                if (o2!=null && o2 instanceof JSONObject) {
                    addRecursive(table, baseKey+i+"]", (JSONObject)o1, (JSONObject)o2);
                }
                else {
                    addRecursive(table, baseKey+i+"]", (JSONObject)o1, null);
                }
            }
            else if (o1 instanceof JSONArray) {
                if (o2!=null && o2 instanceof JSONArray) {
                    iterateArray(table, baseKey+i+"][", (JSONArray)o1, (JSONArray)o2);
                }
                else {
                    iterateArray(table, baseKey+i+"][", (JSONArray)o1, null);
                }
            }
            else {
                addRow(table,  baseKey+i+"]", smartValue(o1), smartValue(o2));
            }
        }
    }



}
