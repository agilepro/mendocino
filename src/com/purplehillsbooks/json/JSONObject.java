package com.purplehillsbooks.json;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>A JSONObject is an collection of name/value pairs.</p>
 *
 * <p> This com.purplehillsbooks.json version of the classes enforces alphabetical order
 * on the object members when being serialized.  This is done to put the output
 * into a cononical, reproducible form.  So that identical JSON objects will
 * produce identical output, and so that if you compare two JSON files, the
 * difference will be more meaningful.   Otherwise, the order of the members does
 * not matter.</p>
 *
 * <p> This com.purplehillsbooks.json version also provides human readible, indented output
 * when you use the <code>write(stream, indent, offset)</code> method.</p>
 *
 * <p>The JSONObject serialized
 * form is a string wrapped in curly braces with colons between the names and
 * values, and commas between the values and names. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing the
 * values by name, and <code>put</code> methods for adding or replacing values
 * by name. The values can be any of these types: <code>Boolean</code>,
 * <code>JSONArray</code>, <code>JSONObject</code>, <code>Number</code>,
 * <code>String</code>, or the <code>JSONObject.NULL</code> object. A JSONObject
 * constructor can be used to convert an external form JSON text into an
 * internal form whose values can be retrieved with the <code>get</code> and
 * <code>opt</code> methods, or to convert values into a JSON text using the
 * <code>put</code> and <code>toString</code> methods. A <code>get</code> method
 * returns a value if one can be found, and throws an exception if one cannot be
 * found. An <code>opt</code> method returns a default value instead of throwing
 * an exception, and so is useful for obtaining optional values.</p>
 *
 * <p>The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * casting for you. The opt methods differ from the get methods in that they do
 * not throw. Instead, they return a specified default value, such as null or zero.</p>
 *
 * <p>The <code>put</code> methods add or replace values in an object. For example,</p>
 *
 * <pre>
 * JSONObject myObj = new JSONObject();
 * myObj.put(&quot;JSON&quot;, &quot;Hello, World!&quot;);
 * myObj.write( myWriter );
 * </pre>
 *
 * <p>produces the string <code>{"JSON": "Hello, World"}</code>.</p>
 *
 * <p>The texts produced by the <code>write</code> methods strictly conform to
 * the JSON syntax rules. The constructors are more forgiving in the texts they
 * will accept:</p>
 *
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * <li>Keys can be followed by <code>=</code> or <code>=></code> as well as by
 * <code>:</code>.</li>
 * <li>Values can be followed by <code>;</code> <small>(semicolon)</small> as
 * well as by <code>,</code> <small>(comma)</small>.</li>
 * </ul>
 *
 * <h1>Reading JSON Format</h1>
 *
 * <p>To read from a stream, reader, or string, use the following:</p>
 *
 * <pre>
 * JSONObject jo = new JSONObject( new JSONTokener( input ) );
 * </pre>
 *
 * <p>To read from a file, it is most convenient:</p>
 *
 * <pre>
 * JSONObject jo = JSONObject.readFromFile( file );
 * </pre>
 *
 * <h1>Writing JSON Format</h1>
 *
 * <p>If you want to write to a stream, use the write methods on JSON object or array:</p>
 *
 * <pre>
 * jo.write( output );
 * -or-
 * jo.write( output, 2, 0 );
 * </pre>
 *
 * <p>To write to a file.  This is more than just a convenience, this will write to a
 * temporary file, and then only when it is fully written, will delete the existing file,
 * and rename the temp to the new name:</p>
 *
 * <pre>
 * jo.writeToFile( file );
 * </pre>
 *
 * <p>Thus, no matter if the server crashes in the
 * middle of writing, the file being pointed to will always contain valid JSON format contents.
 * If the server crashes in the middle of writing, this may leave "temp" files in
 * the file system.</p>
 *
 * <p>If you have multiple programs running which will be updating a JSON file, please look
 * into LockableJSONFile for proper file system level locking of JSON files.</p>
 *
 * @see com.purplehillsbooks.json.LockableJSONFile
 *
 * <hr/>
 *
 * </p>Originally written by JSON.org released 2012-10-26 but heavily modified
 * by Keith Swenson in the years after that to make the object classes
 * conform to style guidelines discussed in http://agiletribe.wordpress.com/</p>
 */
public class JSONObject {

    /**
     * The map where the JSONObject's properties are kept.
     */
    private final Map<String, Object> map;


    /**
     * It is sometimes more convenient and less ambiguous to have a
     * <code>NULL</code> object than to use Java's <code>null</code> value.
     * <code>JSONObject.NULL.equals(null)</code> returns <code>true</code>.
     * <code>JSONObject.NULL.toString()</code> returns <code>"null"</code>.
     */
    public static final Object NULL = new Null();


    /**
     * Construct an empty JSONObject.
     */
    public JSONObject() {
        this.map = new HashMap<String,Object>();
    }


    /**
     * Open the file if exists, read the contents, and return the
     * JSONObject tree that the file represents.
     */
    public static JSONObject readFromFile(File inFile) throws Exception {
        try {
            //System.out.println("JSONObject.readFromFile: "+inFile);
            FileInputStream fis = new FileInputStream(inFile);
            JSONTokener jt = new JSONTokener(fis);
            JSONObject jo = new JSONObject(jt);
            fis.close();
            return jo;
        }
        catch (Exception e) {
            //System.out.println("JSONObject.readFromFile FAILURE: "+inFile);
            //e.printStackTrace();
            throw new Exception("Unable to read JSON objects from file: "+inFile, e);
        }
    }


    /**
     * Open the file if exists, read the contents, and return the
     * JSONObject tree that the file represents.
     * If the file does not exist, then it return an empty JSONObject
     * representing the content of the file that does not exist.
     * This is useful for files that lazily created and that start
     * as an empty JSONObject.  This makes a missing file acts as if
     * the file had been initialized with an empty JSONObject, but it
     * does not actually update the file system with a new file.
     */
    public static JSONObject readFileIfExists(File inFile) throws Exception {
        try {
            //System.out.println("JSONObject.readFileIfExists: "+inFile);
            if (!inFile.exists()) {
                return new JSONObject();
            }
            FileInputStream fis = new FileInputStream(inFile);
            JSONTokener jt = new JSONTokener(fis);
            JSONObject jo = new JSONObject(jt);
            fis.close();
            return jo;
        }
        catch (Exception e) {
            //System.out.println("JSONObject.readFileIfExists FAILURE: "+inFile);
            //e.printStackTrace();
            throw new Exception("Unable to read JSON objects from file: "+inFile, e);
        }
    }

    /**
     * Write the entire contents of the JSONObject tree to the specified
     * file using JSON format.  This is written out properly and safely
     * by first writing to a temporary file with the symbol ~tmp~ on the end,
     * as well as a time stamp to ensure uniqueness.
     * Then, once the temp file exists, the old
     * file (if there was one) is deleted, and the temp file is renamed
     * to be the desired output file name.  This guarantees that you will
     * Always have a complete file on the disk (along with a vanishingly
     * small possibility that there will be no file).  No matter if the
     * host program crashes, or the power goes out, you will never have
     * a half-written corrupted file on disk.
     * @param outFile
     * @throws Exception
     */
    public void writeToFile(File outFile) throws Exception {
        try {
            File folder = outFile.getParentFile();
            File tempFile = new File(folder, "~"+outFile.getName()+"~tmp~"+System.currentTimeMillis());
            if (tempFile.exists()) {
                tempFile.delete();
                if (tempFile.exists()) {
                    throw new Exception("Before writing JSON tmp file, unable to delete the old tmp file: "+tempFile);
                }
            }
            FileOutputStream fos = new FileOutputStream(tempFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            try {
                this.write(osw,2,0);
            }
            finally {
                osw.close();
            }

            Path sourcePath      = Paths.get(tempFile.toString());
            Path destinationPath = Paths.get(outFile.toString());

            boolean failedOnce = false;
            if (Files.exists(destinationPath)) {
                try {
                    Files.delete(destinationPath);
                }
                catch (Exception e) {
                    System.out.println("RETRYING-FAILURE deleting file ("+tempFile+") because: "+e.toString());
                    failedOnce = true;
                }
            }
            if (failedOnce) {
                //This is really gross, but if the delete fails, try it again, if needed.
                if (Files.exists(destinationPath)) {
                    Files.delete(destinationPath);
                }
            }


            failedOnce = false;
            try {
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (Exception e) {
                System.out.println("RETRYING-FAILURE renaming file ("+tempFile+") because: "+e.toString());
                failedOnce = true;
            }

            //This is really gross, but it seems that this call will fail about 1% of the time,  we
            //suspect it is due to virus-scan or something else.   It does not fail every time, and it
            //is timing dependent, so hard to prove either way.   This causes a slight slow down in
            //maybe 1% of the cases, but there is no real way around it.
            if (failedOnce) {
                Thread.sleep(10);
                //after waiting just a moment, if this fails, then it fails for good
                Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }

            //on some shared file systems, this step was failing but there was no indication at the time of the fail.
            //So the following two steps check for that, and produce an error when it is detected.
            if (!outFile.exists()) {
                throw new Exception("Unable to rename the JSON tmp file ("+tempFile+") to the actual file name("+outFile+")");
            }
            if (!Files.exists(destinationPath)) {
                throw new Exception("Unable to rename the JSON tmp file ("+tempFile+") to the actual file name("+outFile+")");
            }
            if (tempFile.exists()) {
                throw new Exception("Very strange, the JSON tmp file ("+tempFile+") remains in the folder after renaming to the actual file.");
            }
        }
        catch (Exception e) {
            throw new Exception("Unable to write JSON objects to the file: "+outFile, e);
        }
    }

    /**
     * Construct a JSONObject from a subset of another JSONObject.
     * An array of strings is used to identify the keys that should be copied.
     * Missing keys are ignored.
     * @param jo A JSONObject.
     * @param names An array of strings.
     * @throws JSONException
     * @exception JSONException If a value is a non-finite number or if a name is duplicated.
     */
    public JSONObject(JSONObject jo, String[] names) throws Exception {
        this();
        for (String name : names) {
            Object val = jo.opt(name);
            if (val!=null) {
                this.putOnce(name, val);
            }
        }
    }


    /**
     * Construct a JSONObject from a JSONTokener.
     * This SHOULD be the most common constructor because it consumes a
     * stream directly and builds the object in the most efficient manner.
     * <p>
     * For a UTF-8 encoded stream, use:
     * <br/>
     * <code> new JSONObject( new JSONTokener( inputStream ) );</code>
     * <br/>
     * For a character based reader, including inputStreams of other encodings
     * wrapped in the appropriate InputStreamReader, use:
     * <br/>
     * <code> new JSONObject( new JSONTokener( reader ) );</code>
     * <br/>
     * <p>
     * @param x A JSONTokener object containing the source string.
     * @throws JSONException If there is a syntax error in the source string
     *  or a duplicated key.
     */
    public JSONObject(JSONTokener x) throws JSONException {
        this();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'");
        }
        for (;;) {
            c = x.nextClean();
            switch (c) {
            case 0:
                throw x.syntaxError("A JSONObject text must end with '}'");
            case '}':
                return;
            default:
                x.back();
                key = x.nextValue().toString();
            }

// The key is followed by ':'. We will also tolerate '=' or '=>'.

            c = x.nextClean();
            if (c == '=') {
                if (x.next() != '>') {
                    x.back();
                }
            } else if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            this.putOnce(key, x.nextValue());

// Pairs are separated by ','. We will also tolerate ';'.

            switch (x.nextClean()) {
            case ';':
            case ',':
                if (x.nextClean() == '}') {
                    return;
                }
                x.back();
                break;
            case '}':
                return;
            default:
                throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }



    /**
     * Construct a JSONObject from an Object using bean getters.
     * It reflects on all of the public methods of the object.
     * For each of the methods with no parameters and a name starting
     * with <code>"get"</code> or <code>"is"</code> followed by an uppercase letter,
     * the method is invoked, and a key and the value returned from the getter method
     * are put into the new JSONObject.
     *
     * The key is formed by removing the <code>"get"</code> or <code>"is"</code> prefix.
     * If the second remaining character is not upper case, then the first
     * character is converted to lower case.
     *
     * For example, if an object has a method named <code>"getName"</code>, and
     * if the result of calling <code>object.getName()</code> is <code>"Larry Fine"</code>,
     * then the JSONObject will contain <code>"name": "Larry Fine"</code>.
     *
     * @param bean An object that has getter methods that should be used
     * to make a JSONObject.
     */
    public JSONObject(Object bean) {
        this();
        this.populateMap(bean);
    }


    /**
     * Construct a JSONObject from a source JSON text string.
     *
     * @deprecated don't use this, use the JSONTokener
     * instead.  The JSON stream should never be  a string, but
     * instead should always be a Stream.  It is always more
     * efficient to stream the JSON representation in and out.
     * Never use a string for this.
     */
    public JSONObject(String source) throws JSONException {
        this(new JSONTokener(source));
    }


    /**
     * Accumulate values under a key. It is similar to the put method except
     * that if there is already an object stored under the key then a
     * JSONArray is stored under the key to hold all of the accumulated values.
     * If there is already a JSONArray, then the new value is appended to it.
     * In contrast, the put method replaces the previous value.
     *
     * If only one value is accumulated that is not a JSONArray, then the
     * result will be the same as using put. But if multiple values are
     * accumulated, then the result will be like append.
     * @param key   A key string.
     * @param value An object to be accumulated under the key.
     * @return this.
     * @throws JSONException If the value is an invalid number
     *  or if the key is null.
     */
    public JSONObject accumulate( String key, Object value ) throws JSONException {
        testValidity(value);
        Object object = this.opt(key);
        if (object == null) {
            this.put(key, value instanceof JSONArray
                    ? new JSONArray().put(value)
                    : value);
        } else if (object instanceof JSONArray) {
            ((JSONArray)object).put(value);
        } else {
            this.put(key, new JSONArray().put(object).put(value));
        }
        return this;
    }


    /**
     * Append values to the array under a key. If the key does not exist in the
     * JSONObject, then the key is put in the JSONObject with its value being a
     * JSONArray containing the value parameter. If the key was already
     * associated with a JSONArray, then the value parameter is appended to it.
     * @param key   A key string.
     * @param value An object to be accumulated under the key.
     * @return this.
     * @throws JSONException If the key is null or if the current value
     *  associated with the key is not a JSONArray.
     */
    public JSONObject append(String key, Object value) throws JSONException {
        testValidity(value);
        Object object = this.opt(key);
        if (object == null) {
            this.put(key, new JSONArray().put(value));
        } else if (object instanceof JSONArray) {
            this.put(key, ((JSONArray)object).put(value));
        } else {
            throw new JSONException("JSONObject[" + key +
                    "] is not a JSONArray.");
        }
        return this;
    }


    /**
     * Produce a string from a double. The string "null" will be returned if
     * the number is not finite.
     * @param  d A double.
     * @return A String.
     */
    public static String doubleToString(double d) {
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "null";
        }

// Shave off trailing zeros and decimal point, if possible.

        String string = Double.toString(d);
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0 &&
                string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }


    /**
     * Get the value object associated with a key.
     *
     * @param key   A key string.
     * @return      The object associated with the key.
     * @throws      JSONException if the key is not found.
     */
    public Object get(String key) throws JSONException {
        if (key == null) {
            throw new JSONException("Null key.");
        }
        Object object = this.opt(key);
        if (object == null) {
            throw new JSONException("JSONObject[" + quote(key) +
                    "] not found.");
        }
        return object;
    }


    /**
     * Get the boolean value associated with a key.
     *
     * @param key   A key string.
     * @return      The truth.
     * @throws      JSONException
     *  if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) throws JSONException {
        Object object = this.get(key);
        if (object.equals(Boolean.FALSE) ||
                (object instanceof String &&
                ((String)object).equalsIgnoreCase("false"))) {
            return false;
        } else if (object.equals(Boolean.TRUE) ||
                (object instanceof String &&
                ((String)object).equalsIgnoreCase("true"))) {
            return true;
        }
        throw new JSONException("JSONObject[" + quote(key) +
                "] is not a Boolean.");
    }


    /**
     * Get the double value associated with a key.
     * @param key   A key string.
     * @return      The numeric value.
     * @throws JSONException if the key is not found or
     *  if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(String key) throws JSONException {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number)object).doubleValue()
                : Double.parseDouble((String)object);
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(key) +
                "] is not a number.");
        }
    }


    /**
     * Get the int value associated with a key.
     *
     * @param key   A key string.
     * @return      The integer value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to an integer.
     */
    public int getInt(String key) throws JSONException {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number)object).intValue()
                : Integer.parseInt((String)object);
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(key) +
                "] is not an int.");
        }
    }


    /**
     * Get the JSONArray value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     * @throws      JSONException if the key is not found or
     *  if the value is not a JSONArray.
     */
    public JSONArray getJSONArray(String key) throws JSONException {
        Object object = this.get(key);
        if (object instanceof JSONArray) {
            return (JSONArray)object;
        }
        throw new JSONException("JSONObject[" + quote(key) +
                "] is not a JSONArray.");
    }


    /**
     * Get the JSONArray value associated with a key
     * and create an empty array with that key if it does not yet exist.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     * @throws      JSONException if the key exists but value is not a JSONArray.
     */
    public JSONArray requireJSONArray(String key) throws JSONException {
        Object object = this.opt(key);
        if (null == object) {
            JSONArray newVal = new JSONArray();
            this.put(key, newVal);
            return newVal;
        }
        if (object instanceof JSONArray) {
            return (JSONArray)object;
        }
        throw new JSONException("JSONObject[" + quote(key) +
                "] is not a JSONArray.");
    }


    /**
     * Get the JSONObject value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     * @throws      JSONException if the key is not found or
     *  if the value is not a JSONObject.
     */
    public JSONObject getJSONObject(String key) throws JSONException {
        Object object = this.get(key);
        if (object instanceof JSONObject) {
            return (JSONObject)object;
        }
        throw new JSONException("JSONObject[" + quote(key) +
                "] is not a JSONObject.");
    }

    /**
     * Get the JSONObject value associated with a key, and create one
     * if it does not exist yet.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     * @throws      JSONException if the key exists but is not a JSONObject.
     */
    public JSONObject requireJSONObject(String key) throws JSONException {
        Object object = this.get(key);
        if (null == object) {
            JSONObject newVal = new JSONObject();
            this.put(key, newVal);
            return newVal;
        }
        if (object instanceof JSONObject) {
            return (JSONObject)object;
        }
        throw new JSONException("JSONObject[" + quote(key) +
                "] is not a JSONObject.");
    }


    /**
     * Get the long value associated with a key.
     *
     * @param key   A key string.
     * @return      The long value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to a long.
     */
    public long getLong(String key) throws JSONException {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number)object).longValue()
                : Long.parseLong((String)object);
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(key) +
                "] is not a long.");
        }
    }


    /**
     * Get an array of field names from a JSONObject.
     *
     * @return An array of field names.  Never returns null.
     *    if there are no fields, returns an empty array.
     */
    public static String[] getNames(JSONObject jo) {
        int length = jo.length();
        String[] names = new String[length];
        int i = 0;
        for (String key : jo.keySet()) {
            names[i] = key;
            i++;
        }
        return names;
    }


    /**
     * Get an array of field names from an Object.
     *
     * @return An array of field names.  Never returns null.
     */
    public static String[] getNames(Object object) {
        if (object == null) {
            return new String[0];
        }
        Class<? extends Object> klass = object.getClass();
        Field[] fields = klass.getFields();
        int length = fields.length;
        if (length == 0) {
            return null;
        }
        String[] names = new String[length];
        for (int i = 0; i < length; i += 1) {
            names[i] = fields[i].getName();
        }
        return names;
    }


    /**
     * Get the string associated with a key.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     * @throws   JSONException if there is no string value for the key.
     */
    public String getString(String key) throws JSONException {
        Object object = this.get(key);
        if (object instanceof String) {
            return (String)object;
        }
        throw new JSONException("JSONObject[" + quote(key) +
            "] not a string.");
    }


    /**
     * Determine if the JSONObject contains a value at a specific key.
     * This is exactly the same as !isNull(key)
     *
     * @param key   A key string.
     * @return      true if the key exists in the JSONObject
     *              AND points to a non-null value.
     */
    public boolean has(String key) {
        return (this.map.containsKey(key) &&
                !JSONObject.NULL.equals(this.opt(key)));
    }


    /**
     * Increment a property of a JSONObject. If there is no such property,
     * create one with a value of 1. If there is such a property, and if
     * it is an Integer, Long, Double, or Float, then add one to it.
     * @param key  A key string.
     * @return this.
     * @throws JSONException If there is already a property with this name
     * that is not an Integer, Long, Double, or Float.
     */
    public JSONObject increment(String key) throws JSONException {
        Object value = this.opt(key);
        if (value == null) {
            this.put(key, 1);
        } else if (value instanceof Integer) {
            this.put(key, ((Integer)value).intValue() + 1);
        } else if (value instanceof Long) {
            this.put(key, ((Long)value).longValue() + 1);
        } else if (value instanceof Double) {
            this.put(key, ((Double)value).doubleValue() + 1);
        } else if (value instanceof Float) {
            this.put(key, ((Float)value).floatValue() + 1);
        } else {
            throw new JSONException("Unable to increment [" + quote(key) + "].");
        }
        return this;
    }


    /**
     * Determine if the value associated with the key is null or if there is
     *  no value.  This is exactly the same as !has(key)
     *
     * @param key   A key string.
     * @return      true if there is no value associated with the key or if
     *  the value is the JSONObject.NULL object.
     */
    public boolean isNull(String key) {
        return JSONObject.NULL.equals(this.opt(key));
    }



    /**
     * @return Return a set of keys of the JSONObject
     */
    public Set<String> keySet() {
        return this.map.keySet();
    }

    /**
     * @return Return a list of keys of the JSONObject in sorted order
     */
    public List<String> sortedKeySet() {
        List<String> keyList = new ArrayList<String>();
        for (String key : keySet()) {
            keyList.add(key);
        }
        Collections.sort(keyList);
        return keyList;
    }

    /**
     * Don't use this, because it is an iterator.  Deprecated.
     * Use sortedKeySet instead which gives you the same functionality
     * but as a List object which allows for the normal Java language
     * capability to iterate automatically over it.
     * @deprecated use sortedKeySet instead because iterators are old fashioned
     */
    public Iterator<String> sortedKeys() {
        return sortedKeySet().iterator();
    }

    /**
     * Don't use this, because it is an iterator.  Deprecated.
     * Use keySet instead which gives you the same functionality
     * but as a List object which allows for the normal Java language
     * capability to iterate automatically over it.
     * Also, consider using sortedKeySet if you are outputting
     * this to an external destination because getting the keys in a
     * predictable order can help in some other ways.
     * @deprecated use keySet instead because iterators are old fashioned
     */
    public Iterator<String> keys() {
        return this.map.keySet().iterator();
    }


    /**
     * Get the number of keys stored in the JSONObject.
     *
     * @return The number of keys in the JSONObject.
     */
    public int length() {
        return this.map.size();
    }


    /**
     * Produce a JSONArray containing the names of the elements of this
     * JSONObject.  Never returns a null.  If there are no keys, it returns
     * an empty array.
     *
     * @return A JSONArray containing the key strings, or null if the JSONObject
     * is empty.
     */
    public JSONArray names() {
        JSONArray ja = new JSONArray();
        for (String key : keySet()) {
            ja.put(key);
        }
        return ja;
    }

    /**
     * Produce a string from a Number.
     * @param  number A Number
     * @return A String.
     * @throws JSONException If n is a non-finite number.
     */
    public static String numberToString(Number number)
            throws JSONException {
        if (number == null) {
            throw new JSONException("Null pointer");
        }
        testValidity(number);

// Shave off trailing zeros and decimal point, if possible.

        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0 &&
                string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }


    /**
     * Get an optional value associated with a key.
     * Returns a null if you pass in a null, or if there is no value
     * at the specified key.
     *
     * @param key   A key string.
     * @return      An object which is the value, or null if there is no value.
     */
    public Object opt(String key) {
        return key == null ? null : this.map.get(key);
    }


    /**
     * Get an optional boolean associated with a key.
     * It returns false if there is no such key, or if the value is not
     * Boolean.TRUE or the String "true".
     *
     * @param key   A key string.
     * @return      The truth.
     */
    public boolean optBoolean(String key) {
        return this.optBoolean(key, false);
    }


    /**
     * Get an optional boolean associated with a key.
     * It returns the defaultValue if there is no such key, or if it is not
     * a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key              A key string.
     * @param defaultValue     The default.
     * @return      The truth.
     */
    public boolean optBoolean(String key, boolean defaultValue) {
        try {
            return this.getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional double associated with a key,
     * or NaN if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A string which is the key.
     * @return      An object which is the value.
     */
    public double optDouble(String key) {
        return this.optDouble(key, Double.NaN);
    }


    /**
     * Get an optional double associated with a key, or the
     * defaultValue if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public double optDouble(String key, double defaultValue) {
        try {
            return this.getDouble(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional int value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public int optInt(String key) {
        return this.optInt(key, 0);
    }


    /**
     * Get an optional int value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public int optInt(String key, int defaultValue) {
        try {
            return this.getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional JSONArray associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONArray.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     */
    public JSONArray optJSONArray(String key) {
        Object o = this.opt(key);
        return o instanceof JSONArray ? (JSONArray)o : null;
    }


    /**
     * Get an optional JSONObject associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONObject.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     */
    public JSONObject optJSONObject(String key) {
        Object object = this.opt(key);
        return object instanceof JSONObject ? (JSONObject)object : null;
    }


    /**
     * Get an optional long value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public long optLong(String key) {
        return this.optLong(key, 0);
    }


    /**
     * Get an optional long value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return             An object which is the value.
     */
    public long optLong(String key, long defaultValue) {
        try {
            return this.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional string associated with a key.
     * It returns an empty string if there is no such key. If the value is not
     * a string and is not null, then it is converted to a string.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     */
    public String optString(String key) {
        return this.optString(key, "");
    }


    /**
     * Get an optional string associated with a key.
     * It returns the defaultValue if there is no such key.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      A string which is the value.
     */
    public String optString(String key, String defaultValue) {
        Object object = this.opt(key);
        return NULL.equals(object) ? defaultValue : object.toString();
    }


    private void populateMap(Object bean) {
        Class<? extends Object> klass = bean.getClass();

// If klass is a System class then set includeSuperClass to false.

        boolean includeSuperClass = klass.getClassLoader() != null;

        Method[] methods = includeSuperClass
                ? klass.getMethods()
                : klass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i += 1) {
            try {
                Method method = methods[i];
                if (Modifier.isPublic(method.getModifiers())) {
                    String name = method.getName();
                    String key = "";
                    if (name.startsWith("get")) {
                        if ("getClass".equals(name) ||
                                "getDeclaringClass".equals(name)) {
                            key = "";
                        } else {
                            key = name.substring(3);
                        }
                    } else if (name.startsWith("is")) {
                        key = name.substring(2);
                    }
                    if (key.length() > 0 &&
                            Character.isUpperCase(key.charAt(0)) &&
                            method.getParameterTypes().length == 0) {
                        if (key.length() == 1) {
                            key = key.toLowerCase();
                        } else if (!Character.isUpperCase(key.charAt(1))) {
                            key = key.substring(0, 1).toLowerCase() +
                                key.substring(1);
                        }

                        Object result = method.invoke(bean, (Object[])null);
                        if (result != null) {
                            this.map.put(key, wrap(result));
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }


    /**
     * Put a key/boolean pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A boolean which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, boolean value) throws JSONException {
        this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject, where the value will be a
     * JSONArray which is produced from a Collection.
     * @param key   A key string.
     * @param value A Collection value.
     * @return      this.
     * @throws JSONException
     */
    public JSONObject put(String key, Collection<?> value) throws JSONException {
        this.put(key, new JSONArray(value));
        return this;
    }


    /**
     * Put a key/double pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A double which is the value.
     * @return this.
     * @throws JSONException If the key is null or if the number is invalid.
     */
    public JSONObject put(String key, double value) throws JSONException {
        this.put(key, new Double(value));
        return this;
    }


    /**
     * Put a key/int pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value An int which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, int value) throws JSONException {
        this.put(key, new Integer(value));
        return this;
    }


    /**
     * Put a key/long pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A long which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, long value) throws JSONException {
        this.put(key, new Long(value));
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject, where the value will be a
     * JSONObject which is produced from a Map.
     * @param key   A key string.
     * @param value A Map value.
     * @return      this.
     * @throws JSONException
     */
    public JSONObject put(String key, Map<String, ?> value) throws JSONException {
        this.put(key, new JSONObject(value));
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject. If the value is null,
     * then the key will be removed from the JSONObject if it is present.
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *  or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is non-finite number
     *  or if the key is null.
     */
    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) {
            throw new JSONException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.map.put(key, value);
        } else {
            this.remove(key);
        }
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject, but only if the key and the
     * value are both non-null, and only if there is not already a member
     * with that name.
     * @param key
     * @param value
     * @return his.
     * @throws JSONException if the key is a duplicate
     */
    public JSONObject putOnce(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            if (this.opt(key) != null) {
                throw new JSONException("Duplicate key \"" + key + "\"");
            }
            this.put(key, value);
        }
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject, but only if the
     * key and the value are both non-null.  If either the key
     * or the value is null, then simply do nothing.
     *
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *  or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is a non-finite number.
     */
    public JSONObject putOpt(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            this.put(key, value);
        }
        return this;
    }


    /**
     * Produce a string in double quotes with all the characters that need
     * it encoded properly.  See <code>quote</code>.  This method uses that
     * stream-based quote method.  Consider using that if you are simply going
     * to write the string out someplace.
     *
     * @param string the string value you want to be encoded.
     * @return  That value correctly encoded for insertion in a JSON text.
     */
    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException ignored) {
                // will never happen - we are writing to a string writer
                return "";
            }
        }
    }

    /**
    * Writes a quoted (encoded) version of the string to the Writer, and
    * returns the same writer that was passed in.
    * That is, it writes double quote characters before and after
    * the value, and then scans the value, putting a backslash
    * before each double quote, and before each backslash.
    * It also converts newlines, returns, tabs, linefeed, and
    * backspace characters to their properly encoded symbolic equivalent.
    */
    public static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                w.write('\\');
                w.write(c);
                break;
            case '/':
                if (b == '<') {
                    w.write('\\');
                }
                w.write(c);
                break;
            case '\b':
                w.write("\\b");
                break;
            case '\t':
                w.write("\\t");
                break;
            case '\n':
                w.write("\\n");
                break;
            case '\f':
                w.write("\\f");
                break;
            case '\r':
                w.write("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                        || (c >= '\u2000' && c < '\u2100')) {
                    w.write("\\u");
                    hhhh = Integer.toHexString(c);
                    w.write("0000", 0, 4 - hhhh.length());
                    w.write(hhhh);
                } else {
                    w.write(c);
                }
            }
        }
        w.write('"');
        return w;
    }

    /**
     * Remove a name and its value, if present.
     * @param key The name to be removed.
     * @return The value that was associated with the name,
     * or null if there was no value.
     */
    public Object remove(String key) {
        return this.map.remove(key);
    }

    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     * @param string A String.
     * @return A simple JSON value.
     */
    public static Object stringToValue(String string) {
        Double d;
        if (string.equals("")) {
            return string;
        }
        if (string.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (string.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (string.equalsIgnoreCase("null")) {
            return JSONObject.NULL;
        }

        /*
         * If it might be a number, try converting it.
         * If a number cannot be produced, then the value will just
         * be a string. Note that the plus and implied string
         * conventions are non-standard. A JSON parser may accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */

        char b = string.charAt(0);
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
            try {
                if (string.indexOf('.') > -1 ||
                        string.indexOf('e') > -1 || string.indexOf('E') > -1) {
                    d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = new Long(string);
                    if (myLong.longValue() == myLong.intValue()) {
                        return new Integer(myLong.intValue());
                    } else {
                        return myLong;
                    }
                }
            }  catch (Exception ignore) {
            }
        }
        return string;
    }


    /**
     * Throw an exception if the object is a NaN or infinite number.
     * @param o The object to test.
     * @throws JSONException If o is a non-finite number.
     */
    public static void testValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new JSONException(
                        "JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new JSONException(
                        "JSON does not allow non-finite numbers.");
                }
            }
        }
    }


    /**
     * Produce a JSONArray containing the values of the members of this
     * JSONObject.
     * @param names A JSONArray containing a list of key strings. This
     * determines the sequence of the values in the result.
     * @return A JSONArray of values.
     * @throws JSONException If any of the values are non-finite numbers.
     */
    public JSONArray toJSONArray(JSONArray names) throws JSONException {
        if (names == null || names.length() == 0) {
            return null;
        }
        JSONArray ja = new JSONArray();
        for (int i = 0; i < names.length(); i += 1) {
            ja.put(this.opt(names.getString(i)));
        }
        return ja;
    }

    /**
     * Make a JSON text of this JSONObject. For compactness, no whitespace
     * is added. If this would not result in a syntactically correct JSON text,
     * or if there is an exception for any other reason,
     * then null will be returned instead.
     * </p><p>
     * Think carefully before using this method!  Do you really need
     * a String in memory?  Since JSON is used as a data transport format
     * normally you are going to write the String out to some destination.
     * It is far more efficient to use the <code>write</code> operation
     * on this object directly.  Think about it: you have a tree of JSON
     * objects in memory.  This method will make a copy of all that data
     * into a single string -- a second copy of the data in memory.
     * If all you are going to do is to write that string out to a file,
     * then use the write method to stream it directly to the file.
     * If you are going to send the data from a server to client browser,
     * the write directly to the output stream.  This reduces memory usage.
     * Sometimes you really do need a String, so the method is provided,
     * but use it sparingly.
     * </p><p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Make a JSON text String representation of this JSONObject.
     * Think carefully before using this method!  Do you really need
     * a String in memory?  Since JSON is used as a data transport format
     * normally you are going to write the String out to some destination.
     * It is far more efficient to use the <code>write</code> operation
     * on this object directly.  Think about it: you have a tree of JSON
     * objects in memory.  This method will make a copy of all that data
     * into a single string -- a second copy of the data in memory.
     * If all you are going to do is to write that string out to a file,
     * then use the write method to stream it directly to the file.
     * If you are going to send the data from a server to client browser,
     * the write directly to the output stream.  This reduces memory usage.
     * Sometimes you really do need a String, so the method is provided,
     * but use it sparingly.
     * </p><p>
     * The JSON is produced indented by an indentFactor amount specified.
     * If you specify zero indent, the output will be all on a single line.
     * The elements are alphabetized only if an indent is specified.
     * </p><p>
     * Warning: This method assumes that the data structure is acyclical.
     * An exception will be thrown if you have a cycle.
     *
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String toString(int indentFactor) {
        try {
            StringWriter w = new StringWriter();
            synchronized (w.getBuffer()) {
                return this.write(w, indentFactor, 0).toString();
            }
        }
        catch (Exception e) {
            //there is no conceivable exception that can come out of this, but throw something
            //just in case.   Want the signature to not have exception in it.
            throw new RuntimeException("Can not serialize JSONObject????", e);
        }
    }

    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce
     * the JSON text. The method is required to produce a strictly
     * conforming text. If the object does not contain a toJSONString
     * method (which is the most common case), then a text will be
     * produced by other means. If the value is an array or Collection,
     * then a JSONArray will be made from it and its toJSONString method
     * will be called. If the value is a MAP, then a JSONObject will be made
     * from it and its toJSONString method will be called. Otherwise, the
     * value's toString method will be called, and the result will be quoted.
     *
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the value is or contains an invalid number.
     */
    public static String valueToString(Object value) throws JSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof JSONString) {
            Object object;
            try {
                object = ((JSONString)value).toJSONString();
            } catch (Exception e) {
                throw new JSONException("Error converting JSONString to a string", e);
            }
            if (object instanceof String) {
                return (String)object;
            }
            throw new JSONException("Bad value from toJSONString: " + object);
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean || value instanceof JSONObject ||
                value instanceof JSONArray) {
            return value.toString();
        }
        if (value instanceof Map) {
            return new JSONObject(value).toString();
        }
        if (value instanceof Collection<?>) {
            return new JSONArray(value).toString();
        }
        if (value.getClass().isArray()) {
            return new JSONArray(value).toString();
        }
        return quote(value.toString());
    }

     /**
      * Wrap an object, if necessary. If the object is null, return the NULL
      * object. If it is an array or collection, wrap it in a JSONArray. If
      * it is a map, wrap it in a JSONObject. If it is a standard property
      * (Double, String, et al) then it is already wrapped. Otherwise, if it
      * comes from one of the java packages, turn it into a string. And if
      * it doesn't, try to wrap it in a JSONObject. If the wrapping fails,
      * then null is returned.
      *
      * @param object The object to wrap
      * @return The wrapped value
      */
     public static Object wrap(Object object) {
         try {
             if (object == null) {
                 return NULL;
             }
             if (object instanceof JSONObject || object instanceof JSONArray  ||
                     NULL.equals(object)      || object instanceof JSONString ||
                     object instanceof Byte   || object instanceof Character  ||
                     object instanceof Short  || object instanceof Integer    ||
                     object instanceof Long   || object instanceof Boolean    ||
                     object instanceof Float  || object instanceof Double     ||
                     object instanceof String) {
                 return object;
             }

             if (object instanceof Collection<?>) {
                 return new JSONArray(object);
             }
             if (object.getClass().isArray()) {
                 return new JSONArray(object);
             }
             if (object instanceof Map) {
                 return new JSONObject(object);
             }
             Package objectPackage = object.getClass().getPackage();
             String objectPackageName = objectPackage != null
                 ? objectPackage.getName()
                 : "";
             if (
                 objectPackageName.startsWith("java.") ||
                 objectPackageName.startsWith("javax.") ||
                 object.getClass().getClassLoader() == null
             ) {
                 return object.toString();
             }
             return new JSONObject(object);
         } catch(Exception exception) {
             return null;
         }
     }


     /**
      * Write the contents of the JSONObject as JSON text.
      * No indentation.
      * <p>
      * Warning: This method assumes that the data structure is acyclical.
      *
      * @return The writer.
      * @throws JSONException
      */
     public Writer write(Writer writer) throws JSONException {
        return this.write(writer, 0, 0);
    }


    static final Writer writeValue(Writer writer, Object value,
            int indentFactor, int indent) throws JSONException, IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof JSONObject) {
            ((JSONObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof JSONArray) {
            ((JSONArray) value).write(writer, indentFactor, indent);
        } else if (value instanceof Map) {
            new JSONObject(value).write(writer, indentFactor, indent);
        } else if (value instanceof Collection<?>) {
            new JSONArray(value).write(writer, indentFactor,
                    indent);
        } else if (value.getClass().isArray()) {
            new JSONArray(value).write(writer, indentFactor, indent);
        } else if (value instanceof Number) {
            writer.write(numberToString((Number) value));
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof JSONString) {
            Object o;
            try {
                o = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                throw new JSONException("Error while converting a JSONString to a string", e);
            }
            writer.write(o != null ? o.toString() : quote(value.toString()));
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Write the contents of the JSONObject as JSON text to a writer.
     * values are put with name/value on a line.
     * Collections are indented to show the level of nesting.
     * Keys are written in alphabetical order so that the
     * same document is produced with a given set of values
     * regardless of what order it was constructed.
     * The order of arrays are preserved.
     * <p>
     * Standard usage: <code>jobj.write( writer, 2, 0 );</code>
     * <p>
     * Warning: This method will abort if the indent level exceeds 100
     *     because that probably indicates that the JSON is linked in a loop.
     *
     * @param writer where the output goes to
     * @param indentFactor specify the number of spaces for each level of nesting.
     *        A value of 2 will indent two spaces for each level of indent.
     * @param indent should be set to zero for the root most level when you
     *        start the writing of a JSON document.  This method is called recursively
     *        and this parameter indicates the level of recursion.
     * @return The writer that was passed in
     * @throws JSONException
     */
    public Writer write(Writer writer, int indentFactor, int indent)
            throws JSONException {
        if (indent > 100) {
            //it is useful to abort attempts to iterate a looped JSON tree
            //rather than run forever and getting a stack overflow.
            throw new JSONException("Too many levels of indent.  This JSON tree is probably linked in a loop, which causes an infinite recursion.  Aborting output.");
        }
        try {
            boolean commanate = false;
            final int length = this.length();
            List<String> keys = this.sortedKeySet();
            writer.write('{');

            if (length == 1) {
                String key = keys.get(0);
                writer.write(quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeValue(writer, this.map.get(key), indentFactor, indent);
            }
            else if (length > 1) {
                final int newindent = indent + indentFactor;
                for (String key : keys) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    writer.write(quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    writeValue(writer, this.map.get(key), indentFactor,
                            newindent);
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new JSONException("Unable to write object JSONObject indent level: "+indent, exception);
        }
     }

    /**
     * JSONObject.NULL is equivalent to the value that JavaScript calls null,
     * whilst Java's null is equivalent to the value that JavaScript calls
     * undefined.
     */
     private static final class Null {

        /**
         * There is only intended to be a single instance of the NULL object,
         * so the clone method returns itself.
         * @return     NULL.
         */
        protected final Object clone() {
            return this;
        }

        /**
         * A Null object is equal to the null value and to itself.
         * @param object    An object to test for nullness.
         * @return true if the object parameter is the JSONObject.NULL object
         *  or null.
         */
        public boolean equals(Object object) {
            return object == null || object == this;
        }

        /**
         * Get the "null" string value.
         * @return The string "null".
         */
        public String toString() {
            return "null";
        }
    }
}
