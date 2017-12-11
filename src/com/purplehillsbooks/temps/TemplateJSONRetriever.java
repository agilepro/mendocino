package com.purplehillsbooks.temps;

import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;

import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONObject;

/**
* <p>
 * This is a TemplateStreamer that gets all the information for the tokens from
 * JSON object tree.  The token values are expressed as path expressions with
 * dots between them.  For example, for a JSON structure like this:
 * </p>
 * <pre>
 * {
 *     cust: [
 *       {
 *         address: {
 *             street: "666 bottom row",
 *             city: "Pittsburg"
 *         },
 *         name: "Jones"
 *       },
 *       {
 *         address: {
 *             street: "1 Elm St.",
 *             city: "Highland Park"
 *         },
 *         name: "Smith"
 *       }
 *     ]
 * }
 *
 *
 *   cust.0.name            == Jones
 *   cust.1.name            == Smith
 *   cust.1.address.street  == 1 Elm St.
 *
 * </pre>
 * <p>
 * Expressions that accurately address leaf nodes will write that string value
 * If the leaf is a number it will write a string representation of that.
 * If the expression ends at an object or an array, it will write nothing.
 * If the expression addresses a member that does not exist, it writes nothing.
 * </p>
 */
public class TemplateJSONRetriever implements TemplateTokenRetriever {
    JSONObject data;
    Hashtable<String,JSONArray> loopArray = new Hashtable<String,JSONArray>();
    Hashtable<String,Object> loopValue = new Hashtable<String,Object>();

    public TemplateJSONRetriever(JSONObject _data) {
        data = _data;
    }


    public void writeTokenValue(Writer out, String token) throws Exception {
        try {
            Object val = getValueFromContext(token);

            if (val!=null) {
                TemplateStreamer.writeHtml(out, val.toString());
            }
        }
        catch (Exception e) {
            throw new Exception("Unable to write value from path "+token, e);
        }
    }

    public void writeTokenValueRaw(Writer out, String token) throws Exception {
        try {
            Object val = getValueFromContext(token);

            if (val!=null) {
                out.write(val.toString());
            }
        }
        catch (Exception e) {
            throw new Exception("Unable to write RAW value from path "+token, e);
        }
    }

    
    public void writeTokenDate(Writer out, String token, String format) throws Exception {
        try {
            Object val = getValueFromContext(token);

            if (val!=null && val instanceof String) {
                long dateVal = safeConvertLong((String)val);
                if (format==null) {
                    format = "yyyy-mm-dd hh:mm:ss z";
                }
                SimpleDateFormat ff = new SimpleDateFormat(format);
                out.write("@@DATE: "+token+"="+val+","+format+"@@");
                out.write(ff.format(dateVal));
            }
        }
        catch (Exception e) {
            throw new Exception("Unable to write Date value from path "+token+", with format "+format, e);
        }
    }
    
    
    
    private Object getValueFromContext(String token) throws Exception {
        ArrayList<String> tokens = splitDots(token);
        if (tokens.size()==0) {
            throw new Exception("Strange, the token value passed yeilded no tokens: "+token);
        }
        String firstToken = tokens.get(0);
        JSONArray itArray = loopArray.get(firstToken);
        Object val = null;

        if (itArray!=null) {
            Object o = loopValue.get(firstToken);
            if (o==null) {
                throw new Exception("Problem that loop have been initiated, but the setIteration has not been called");
            }
            if (tokens.size()<=1) {
                val = o;
            }
            else if (o instanceof JSONArray) {
                val = getValuefromArray(tokens, 1, (JSONArray)o);
            }
            else if (o instanceof JSONObject) {
                val = getValuefromObject(tokens, 1, (JSONObject)o);
            }
            else {
                val = o;
            }
        }
        else {
            val = getValuefromObject(tokens, 0, data);
        }
        return val;
    }


    public int initLoop(String id, String token) throws Exception {
        //find the array that the token refers to
        //and set up to handle as a loop
        ArrayList<String> tokens = splitDots(token);
        Object o = getValuefromObject(tokens, 0, data);

        if (o instanceof JSONArray) {
            loopArray.put(id, (JSONArray)o);
            return ((JSONArray)o).length();
        }
        else {
            throw new Exception("Loop data path parameter must address a JSON Array, but this one does not: "+token);
        }
    }

    public void setIteration(String id, int loopCount) throws Exception {
        JSONArray itArray = loopArray.get(id);
        if (itArray==null) {
            throw new Exception("Loop problem, no loop with id ("+id+") appears to have been initialized.");
        }
        if (loopCount>=itArray.length()) {
            throw new Exception("Loop problem, seem to have iterated off end of array: "
                    +loopCount+" is greater than "+(itArray.length()-1));
        }
        loopValue.put(id, itArray.get(loopCount));
    }

    public void closeLoop(String id) throws Exception {
        loopArray.remove(id);
        loopValue.remove(id);
    }

    public boolean ifValue(String token) throws Exception {
        try {
            Object o = getValueFromContext(token);

            if (o==null) {
                //should never get a null back, but if so it is a false
                return false;
            }
            if (o instanceof JSONObject) {
                JSONObject jo = (JSONObject)o;
                return (jo.length()>0);
            }
            else if (o instanceof JSONArray) {
                JSONArray ja = (JSONArray)o;
                return (ja.length()>0);
            }
            else if (o instanceof String) {
                return ((String)o).length()>0;
            }
            else if (o instanceof Integer) {
                return ((Integer)o).intValue()!=0;
            }
            return false;
        }
        catch (Exception e) {
            throw new Exception("Unable to determine whether value exists: "+token, e);
        }
    }

    public void debugDump(Writer out) throws Exception {
        data.write(out, 2, 0);
    }
    
    
    private static Object getValuefromObject(ArrayList<String> tokens, int index, JSONObject d) throws Exception {
        String thisToken = tokens.get(index);
        if (!d.has(thisToken)) {
            //no member by this name, so return no value, keep it silent
            return "";
        }

        //not at the end, so we need to be somewhat fancier
        Object o = d.get(thisToken);

        if (index == tokens.size() - 1) {
            //in this case we actually need to get a string
            return o;
        }

        if (o instanceof JSONObject) {
            return getValuefromObject(tokens, index+1, (JSONObject)o );
        }

        if (o instanceof JSONArray) {
            return getValuefromArray(tokens, index+1, (JSONArray)o );
        }

        //don't know what else it is, so just return object
        return o;
    }

    private static Object getValuefromArray(ArrayList<String> tokens, int index, JSONArray ja) throws Exception {
        String thisToken = tokens.get(index);
        int intIndex = safeConvertInt(thisToken);
        if (intIndex >= ja.length()) {
            //exceeded the length of the array, so return a empty string
            return "";
        }

        //not at the end, so we need to be somewhat fancier
        Object o = ja.get(intIndex);

        if (index == tokens.size() - 1) {
            //in this case we actually need to get a string
            return o;
        }

        if (o instanceof JSONObject) {
            return getValuefromObject(tokens, index+1, (JSONObject)o );
        }

        if (o instanceof JSONArray) {
            return getValuefromArray(tokens, index+1, (JSONArray)o );
        }

        //anything else, just return the string version of it
        return o;
    }

    /**
     * designed primarily for returning date long values works only for positive
     * integer (long) values considers all numeral, ignores all letter and
     * punctuation never throws an exception if you give this something that is
     * not a number, you get surprising result. Zero if no numerals at all.
     */
    public static int safeConvertInt(String val) {
        if (val == null) {
            return 0;
        }
        int res = 0;
        int last = val.length();
        for (int i = 0; i < last; i++) {
            char ch = val.charAt(i);
            if (ch >= '0' && ch <= '9') {
                res = res * 10 + ch - '0';
            }
        }
        return res;
    }

    public static long safeConvertLong(String val) {
        if (val == null) {
            return 0;
        }
        long res = 0;
        int last = val.length();
        for (int i = 0; i < last; i++) {
            char ch = val.charAt(i);
            if (ch >= '0' && ch <= '9') {
                res = res * 10 + ch - '0';
            }
        }
        return res;
    }
    
    
    /**
     * Breaks a string into a list of strings using dots (periods)
     * as separators of the token, and trimming each token of
     * spaces if there are any.
     */
    public static ArrayList<String> splitDots(String val) {
        ArrayList<String> ret = new ArrayList<String>();

        if (val==null) {
            return ret;
        }

        int pos = 0;
        int dotPos = val.indexOf(".");
        while (dotPos >= pos) {
            if (dotPos > pos) {
                ret.add(val.substring(pos,dotPos).trim());
            }
            pos = dotPos + 1;
            if (pos >= val.length()) {
                break;
            }
            dotPos = val.indexOf(".", pos);
        }
        if (pos<val.length()) {
            ret.add(val.substring(pos).trim());
        }
        return ret;
    }

}
