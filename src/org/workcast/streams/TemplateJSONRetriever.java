package org.workcast.streams;

import java.io.Writer;
import java.util.ArrayList;

import org.workcast.json.JSONArray;
import org.workcast.json.JSONObject;

/**
* <p>
 * This is a TemplateStreamer that gets all the information for the tokens from
 * JSON object tree.  The token values are expressed as path expressions with
 * dots between them.  For example, for a JSON structure like this:
 *
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
 *   cust.0.name            --> Jones
 *   cust.1.name            --> Smith
 *   cust.1.address.street  --> 1 Elm St.
 *
 * Expressions that accurately address leaf nodes will write that string value
 * If the leaf is a number it will write a string representation of that.
 * If the expression ends at an object or an array, it will write nothing.
 * If the expression addresses a member that does not exist, it writes nothing.
 *
 */
public class TemplateJSONRetriever implements TemplateTokenRetriever {
    JSONObject data;

    public TemplateJSONRetriever(JSONObject _data) {
        data = _data;
    }


    public void writeTokenValue(Writer out, String token) throws Exception {
        ArrayList<String> tokens = splitDots(token);

        try {
            String val = getValuefromObject(tokens, 0, data);
            out.write(val);
        }
        catch (Exception e) {
            throw new Exception("Unable to get value from path "+token, e);
        }
    }


    private static String getValuefromObject(ArrayList<String> tokens, int index, JSONObject d) throws Exception {
        String thisToken = tokens.get(index);
        if (!d.has(thisToken)) {
            //no member by this name, so return no value, keep it silent
            return "";
        }

        //not at the end, so we need to be somewhat fancier
        Object o = d.get(thisToken);

        if (index == tokens.size() - 1) {
            //in this case we actually need to get a string
            return o.toString();
        }

        if (o instanceof JSONObject) {
            return getValuefromObject(tokens, index+1, (JSONObject)o );
        }

        if (o instanceof JSONArray) {
            return getValuefromArray(tokens, index+1, (JSONArray)o );
        }

        //don't know what else it is, so just return string
        return o.toString();
    }

    private static String getValuefromArray(ArrayList<String> tokens, int index, JSONArray ja) throws Exception {
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
            return ja.getString(intIndex);
        }

        if (o instanceof JSONObject) {
            return getValuefromObject(tokens, index+1, (JSONObject)o );
        }

        if (o instanceof JSONArray) {
            return getValuefromArray(tokens, index+1, (JSONArray)o );
        }

        //anything else, just return the string version of it
        return o.toString();
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

    private ArrayList<String> splitDots(String val) {
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
