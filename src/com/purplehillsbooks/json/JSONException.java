package com.purplehillsbooks.json;

import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JSONException is mainly a class that has a few helpful methods for handling
 * exceptions, such as:
 * to get the full message of a chain of exceptions,
 * to test if a particular message is anywhere in a chain,
 * to convert an exception to JSON in a standard way
 * to trace an exception to the output stream in a standard way.
 */
public class JSONException extends Exception {
    private static final long serialVersionUID = 0;
    public  String   template;
    public  Object[] params;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
        params = new Object[0];
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
        params = new Object[0];
    }

    /**
     * Construct the exception with a template by using variable parameters
     * Use a template like this:
     *
     * JSONException("Error when contemplating {0} in context of {1}", value0, value1)
     *
     * Tokens are braces with a single digit numeral between them.
     * The message will ultimately include the main string template with the values
     * substituted, however this gives the option to translate the template before
     * substitution and get a translated message.
     */
    public JSONException(String template, Object ... params) {
        super(formatString(template, params));
        this.template = template;
        this.params = params;
    }
    public JSONException(String template, Throwable cause, Object ... params) {
        super(formatString(template, params), cause);
        this.template = template;
        this.params = params;
    }

    /**
     * @deprecated always specify a message value, never just do the wrap
     */
    public JSONException(Throwable cause) {
        super("Error while processing JSON", cause);
    }


    public static String formatString(String template, Object[] params) {
        StringBuilder sb = new StringBuilder();

        int start = 0;
        int pos = template.indexOf("{");
        while (pos>0)  {
            sb.append(template.substring(start, pos));
            int endPos = template.indexOf("}",pos);
            if (endPos<pos+2) {
                //no end of the token, or malformed token with no contents
                //just append the rest and return it
                sb.append(template.substring(pos));
                return sb.toString();
            }
            int param = safeConvertInt(template.substring(pos+1,endPos));
            if (param>=params.length) {
                sb.append("unknown_param_"+param);
            }
            else {
                Object p = params[param];
                if (p instanceof String) {
                    sb.append(p);
                }
                else {
                    sb.append(p.toString());
                }
            }
            start = endPos+1;
            pos = template.indexOf("{", start);
        }
        sb.append(template.substring(start));
        return sb.toString();
    }

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

    /**
    * Walks through a chain of exception objects, from the first, to each
    * "cause" in turn, creating a single combined string message from all
    * the exception objects in the chain, with newline characters between
    * each exception message.
    */
    public static String getFullMessage(Throwable e)
    {
        StringBuffer retMsg = new StringBuffer();
        while (e != null) {
            retMsg.append(e.toString());
            retMsg.append("\n");
            e = e.getCause();
        }
        return retMsg.toString();
    }

    /**
    * When an exception is caught, you will want to test whether the exception, or any of
    * the causing exceptions contains a particular string fragment.  This routine searches
    * the entire cascading chain of exceptions and return true if the string fragment is
    * found in any of the exception, and false if the fragment is not found anywhere.
    */
    public static boolean containsMessage(Throwable t, String fragment) {
        while (t!=null) {
            if (t.getMessage().contains(fragment)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }


    public String substituteParams() {
        String msg = getMessage();
        if (params.length==0) {
            return msg;
        }
        String result = String.format(msg, (Object[]) params);
        return result;
    }


    /**
     * In any kind of JSON protocol, you need to return an exception back to the caller.
     * How should the Java exception be encoded into JSON?
     * This method offers a convenient way to convert ANY exception
     * into a stardard form proposed by OASIS:
     *
     * http://docs.oasis-open.org/odata/odata-json-format/v4.0/errata02/os/odata-json-format-v4.0-errata02-os-complete.html#_Toc403940655
     *
     * Also see:
     *
     * https://agiletribe.wordpress.com/2015/09/16/json-rest-api-exception-handling/
     *
     * @param context allows you to include some context about the operation that was being performed
     *                when the exception occurred.
     */
    public static JSONObject convertToJSON(Throwable e, String context) throws Exception {
        JSONObject responseBody = new JSONObject();
        JSONObject errorTag = new JSONObject();
        responseBody.put("error", errorTag);

        errorTag.put("code", 400);
        errorTag.put("context", context);

        JSONArray detailList = new JSONArray();
        errorTag.put("details", detailList);

        Throwable nextRunner = e;
        List<ExceptionTracer> traceHolder = new ArrayList<ExceptionTracer>();
        while (nextRunner!=null) {
            //doing this at the top allows 'continues' statements to be safe
            Throwable runner = nextRunner;
            nextRunner = runner.getCause();

            String className =  runner.getClass().getName();
            String msg =  runner.toString();

            //trim the classname off the message since that is usually unimportant to communicating the problem
            if (msg.startsWith(className) && msg.length()>className.length()+5) {
                int skipTo = className.length();
                while (skipTo<msg.length()) {
                    char ch = msg.charAt(skipTo);
                    if (ch != ':' && ch != ' ') {
                        break;
                    }
                    skipTo++;
                }
                msg = msg.substring(skipTo);
            }

            ExceptionTracer et = new ExceptionTracer();
            et.t = runner;
            et.msg = msg;
            et.captureTrace();
            traceHolder.add(et);

            JSONObject detailObj = new JSONObject();
            if (runner instanceof JSONException) {
                JSONException jrun = (JSONException)runner;
                if (jrun.params.length>0) {
                    detailObj.put("template", jrun.template);
                    for (int i=0; i<jrun.params.length; i++) {
                        detailObj.put("param"+i,jrun.params[i]);
                    }
                }
            }
            detailObj.put("message",msg);
            int dotPos = className.lastIndexOf(".");
            if (dotPos>0) {
                className = className.substring(dotPos+1);
            }
            detailObj.put("code",className);
            detailList.put(detailObj);
        }

        JSONObject innerError = new JSONObject();
        errorTag.put("innerError", innerError);

        //now do them in the opposite order for the stack trace.
        JSONArray stackList = new JSONArray();
        for (int i=traceHolder.size()-1;i>=0;i--) {
            ExceptionTracer et = traceHolder.get(i);
            if (i>0) {
                ExceptionTracer lower = traceHolder.get(i-1);
                et.removeTail(lower.trace);
            }
            et.insertIntoArray(stackList);
        }
        errorTag.put("stack", stackList);

        return responseBody;
    }

    /**
     * This grabs a copy of the trace so that later it can be compared with
     * other stack traces, and truncated to eliminate the redundant parts.
     * Records the fact that it was truncated.
     */
    static class ExceptionTracer {
        public Throwable t;
        public String msg;
        public List<String> trace = new ArrayList<String>();
        boolean wasTrimmed = false;

        public ExceptionTracer() {}

        public void captureTrace() {
            for (StackTraceElement ste : t.getStackTrace()) {
                String line = "    "+ste.getFileName() + ": " + ste.getMethodName() + ": " + ste.getLineNumber();
                trace.add(line);
            }
        }
        public void removeTail(List<String> lower) {
            int offUpper = trace.size()-1;
            int offLower = lower.size()-1;
            while (offUpper>0 && offLower>0
                    && trace.get(offUpper).equals(lower.get(offLower))) {
                trace.remove(offUpper);
                offUpper--;
                offLower--;
                wasTrimmed = true;
            }
        }

        public void insertIntoArray(JSONArray ja) {
            ja.put(msg);
            for (String line : trace) {
                ja.put(line);
            }
            if (wasTrimmed) {
                ja.put("    (continued below)");
            }
        }
    }

    /**
     * A standardized way to trace a given exception to the system out.
     */
    public static JSONObject traceException(Throwable e, String context) {
        JSONObject errOB = traceException(System.out, e, context);
        return errOB;
    }

    /**
     * A standardized way to trace a given exception to the system out.
     */
    public static JSONObject traceException(PrintStream out, Throwable e, String context) {
        if (out==null) {
            System.out.println("$$$$$$$$ traceException requires an out parameter");
            return null;
        }
        if (e==null) {
            System.out.println("$$$$$$$$ traceException requires an e parameter");
            return null;
        }
        if (context==null || context.length()==0) {
            System.out.println("$$$$$$$$ traceException requires a context parameter");
            return null;
        }
        try {
            JSONObject errOb = convertToJSON(e, context);
            traceConvertedException(out, errOb);
            return errOb;
        }
        catch (Exception eee) {
            System.out.println("$$$$$$$$ FAILURE TRACING AN EXCEPTION TO JSON");
            eee.printStackTrace();
            return null;
        }
    }

    /**
     * A standardized way to trace a given exception to the system out.
     */
    public static JSONObject traceException(Writer w, Throwable e, String context) {
        if (w==null) {
            System.out.println("$$$$$$$$ traceException requires an w parameter");
            return null;
        }
        if (e==null) {
            System.out.println("$$$$$$$$ traceException requires an e parameter");
            return null;
        }
        if (context==null || context.length()==0) {
            System.out.println("$$$$$$$$ traceException requires a context parameter");
            return null;
        }
        try {
            JSONObject errOb = convertToJSON(e, context);
            traceConvertedException(w, errOb);
            return errOb;
        }
        catch (Exception eee) {
            System.out.println("$$$$$$$$ FAILURE TRACING AN EXCEPTION TO JSON");
            eee.printStackTrace();
            return null;
        }
    }


    /**
     * If you have already converted to a JSONOBject, you can use this method to
     * get a standard trace of that object to the output writer.
     */
    public static void traceConvertedException(PrintStream out, JSONObject errOb) {
        try {
            out.println(getTraceExceptionFormat(errOb));
        }
        catch (Exception eee) {
            System.out.println("$$$$$$$$ FAILURE TRACING A CONVERTED EXCEPTION");
            eee.printStackTrace();
        }
    }

    public static void traceConvertedException(Writer w, JSONObject errOb) {
        try {
            w.write(getTraceExceptionFormat(errOb));
        }
        catch (Exception eee) {
            System.out.println("$$$$$$$$ FAILURE TRACING A CONVERTED EXCEPTION");
            eee.printStackTrace();
        }
    }

    /**
     * If you have already converted to a JSONOBject, you can use this method to
     * get a standard trace of that object to the output writer.
     *
     * This returns a string because it was too difficult to sort out the
     * PrintWriter, Writer, PrintStream differences, and because all of the
     * exceptions within exception need to be handled with output streams.
     * Clearly returning a string is not efficient memory-wise, but exceptions
     * should be rare, so don't worry about it.
     */
    public static String getTraceExceptionFormat(JSONObject errOb) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n~~~~~~~~~~~~~EXCEPTION~~~~~~~~~~~~~~~~ ");
        sb.append(new Date().toString());
        sb.append("\n");
        sb.append(errOb.toString(2));
        sb.append("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ \n");
        return sb.toString();
    }

    /**
     * Given a JSON representation of an exception, this re-constructs the Exception
     * chain (linked cause exceptions) and returns the Exception object.
     * The main purpose of this is when calling a web service, if it returns
     * this standard kind of JSON representation, then this converts it back
     * to exception objects so that can be thrown.  Thus the exception on the server
     * is reproduced to the client.
     *
     * This copies the stack to the final (rootmost) exception object and does not try
     * to allocate stack to each of the individual exception objects in the chain
     *
     **/
    public static Exception convertJSONToException(JSONObject ex) {
        Exception trailer = null;
        try {
            if (ex==null) {
                return new Exception("Failure converting JSONToException: Null parameter to JSONToException");
            }
            if (!ex.has("error")) {
                return new Exception("Failure converting JSONToException: no 'error' member in object. "+ex.toString());
            }
            JSONObject error = ex.getJSONObject("error");
            if (!error.has("details")) {
                return new Exception("Failure converting JSONToException: no 'error.details' member in object. "+ex.toString());
            }
            JSONArray details = error.getJSONArray("details");
            for (int i=details.length()-1; i>=0; i--) {
                JSONObject oneDetail = details.getJSONObject(i);
                String message = oneDetail.getString("message");
                String template = oneDetail.optString("template", null);
                int paramCount = 0;
                while (oneDetail.has("param"+paramCount)) {
                    paramCount++;
                }
                Object[] params = new Object[paramCount];
                for (int ii=0; ii<paramCount; ii++) {
                    params[ii] = oneDetail.getString("param"+ii);
                }
                if (trailer!=null) {
                    if (template!=null && paramCount>0) {
                        trailer = new JSONException(template, trailer, params);
                    }
                    else {
                        trailer = new Exception(message, trailer);
                    }
                }
                else {
                    if (template!=null && paramCount>0) {
                        trailer = new JSONException(template, params);
                    }
                    else {
                        trailer = new Exception(message);
                    }
                }
                //zero out the stack traces on these objects
                trailer.setStackTrace(new StackTraceElement[0]);
            }

            if (trailer==null) {
                return new Exception("Failure converting JSONToException: no details of the error. "+ex.toString());
            }
            if (error.has("stack")) {
                JSONArray stack = error.getJSONArray("stack");
                ArrayList<StackTraceElement> newStack = new  ArrayList<StackTraceElement>();
                for (int i=0; i<stack.length(); i++) {
                    String line = stack.getString(i);
                    //now parse this into part according to the form
                    //   "    "+FileName + ": " + MethodName + ": " + LineNumber
                    String fileName = "";
                    String methodName = "";
                    int lineNumber = 0;
                    if (line.length()<4) {
                        //ignore very short lines
                        continue;
                    }
                    if (!"  ".equals(line.substring(0,2))) {
                        //ignore lines that don't start with two spaces
                        continue;
                    }
                    int pos = line.lastIndexOf(":");
                    if (pos<0) {
                        //ignore lines without any colons in them
                        continue;
                    }
                    lineNumber = safeConvertInt(line.substring(pos+1));
                    line = line.substring(0, pos);
                    pos = line.lastIndexOf(":");
                    if (pos>0) {
                        methodName = line.substring(pos+1).trim();
                        line = line.substring(0, pos);
                    }
                    fileName = line.trim();
                    newStack.add(new StackTraceElement("", methodName, fileName, lineNumber));
                }
                StackTraceElement[] newStackArray = new StackTraceElement[newStack.size()];
                for (int i=0; i<newStack.size(); i++) {
                    newStackArray[i] = newStack.get(i);
                }
                trailer.setStackTrace(newStackArray);
            }
            return trailer;
        }
        catch (Exception xxx) {
            return new Exception("Failure converting JSONToException: "+ex.toString(), xxx);
        }
    }

}
