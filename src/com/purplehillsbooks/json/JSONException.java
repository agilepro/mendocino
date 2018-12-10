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
    private String   template;
    private String[] params;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
        params = new String[0];
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
        params = new String[0];
    }

    public JSONException(String template, String ... params) {
        super(String.format(template, (Object[]) params));
        this.template = template;
        this.params = params;
    }
    public JSONException(String template, Throwable cause, String ... params) {
        super(String.format(template, (Object[]) params), cause);
        this.template = template;
        this.params = params;
    }

    /**
     * @deprecated always specify a message value, never just do the wrap
     */
    public JSONException(Throwable cause) {
        super("Error while processing JSON", cause);
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

        String lastMessage = "";
        
        Throwable nextRunner = e;
        List<ExceptionTracer> traceHolder = new ArrayList<ExceptionTracer>();
        while (nextRunner!=null) {
            //doing this at the top allows 'continues' statements to be safe
            Throwable runner = nextRunner;
            nextRunner = runner.getCause();
            
            String className =  runner.getClass().getName();
            String msg =  runner.toString();

            //iflow has this annoying habit of including all the later causes in the response
            //surrounded by braces.  This strips them off because we are going to iterate down
            //to those causes anyway.
            boolean isIFlow = className.indexOf("iflow")>0;
            if (isIFlow) {
                int bracePos = msg.indexOf('{');
                if (bracePos>0) {
                    msg = msg.substring(0,bracePos);
                }
            }

            if (msg.startsWith(className)) {
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
            
            if (lastMessage.equals(msg)) {
                //model api has an incredibly stupid pattern of catching an exception, and then throwing a
                //new exception with the exact same message.  This ends up in three or four duplicate messages.
                //Check here for that problem, and eliminate duplicate messages by skipping rest of loop.
                continue;
            }
            
            ExceptionTracer et = new ExceptionTracer();
            et.t = runner;
            et.msg = msg;
            et.captureTrace();
            traceHolder.add(et);

            lastMessage = msg;

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
     * This does not copy the stack traces, only the 'stack' of messages.
     */
    public static Exception convertJSONToException(JSONObject ex) {
        Exception trailer = null;
        try {
            if (ex==null) {
                return new Exception("Failure converting JSONToException: Null parameter to JSONToException");
            }
            if (!ex.has("error")) {
                return new Exception("Failure converting JSONToException: no 'error' member in object.");
            }
            JSONObject error = ex.getJSONObject("error");
            if (!error.has("details")) {
                return new Exception("Failure converting JSONToException: no 'error.details' member in object.");
            }
            JSONArray details = error.getJSONArray("details");
            for (int i=details.length()-1; i>=0; i--) {
                JSONObject oneDetail = details.getJSONObject(i);
                if (trailer!=null) {
                    trailer = new Exception(oneDetail.getString("message"), trailer);
                }
                else {
                    trailer = new Exception(oneDetail.getString("message"));
                }
            }
            if (trailer!=null) {
                return trailer;
            }
            return new Exception("Failure converting JSONToException: no details of the error.");
        }
        catch (Exception xxx) {
            if (trailer!=null) {
                return new Exception("Failure converting JSONToException: "+xxx.toString());
            }
            else {
                return new Exception("Failure converting JSONToException: "+xxx.toString(), xxx);
            }
        }
    }
        
}
