package org.workcast.json;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JSONException is nothing special, and one should really use Exception instead.
 * Never never NEVER catch a JSONException, always catch 'Exception' instead.
 * It is just a favorite pattern that programmers like to have their own exception class
 * and now that the API is distributd we are stuck with maintaining this class.
 * But there is no value, and it would be unsafe to treat JSONException as if it indicated
 * any thing different from any other exception.
 * Nothing to see here, just keep on moving .....
 */
public class JSONException extends Exception {
    private static final long serialVersionUID = 0;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
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
    public static JSONObject convertToJSON(Exception e, String context) throws Exception {
        JSONObject responseBody = new JSONObject();
        JSONObject errorTag = new JSONObject();
        responseBody.put("error", errorTag);

        errorTag.put("code", 400);
        errorTag.put("context", context);

        JSONArray detailList = new JSONArray();
        errorTag.put("details", detailList);

        String lastMessage = "";
        Throwable runner = e;
        while (runner!=null) {
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

            runner = runner.getCause();
            if (lastMessage.equals(msg)) {
                //model api has an incredibly stupid pattern of catching an exception, and then throwing a
                //new exception with the exact same message.  This ends up in three or four duplicate messages.
                //Check here for that problem, and eliminate duplicate messages by skipping rest of loop.
                continue;
            }
            lastMessage = msg;

            JSONObject detailObj = new JSONObject();
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

        JSONArray stackList = new JSONArray();
        runner = e;
        while (runner != null) {
            for (StackTraceElement ste : runner.getStackTrace()) {
                String line = ste.getFileName() + ":" + ste.getMethodName() + ":" + ste.getLineNumber();
                stackList.put(line);
            }
            stackList.put("----------------");
            runner = runner.getCause();
        }
        errorTag.put("stack", stackList);

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        List<String> nicerStack = prettifyStack(sw.toString());
        for (String onePart : nicerStack) {
            stackList.put(onePart);
        }
        return responseBody;
    }
    
    private static List<String> prettifyStack(String input) {
        ArrayList<String> res = new ArrayList<String>();
        int start = 0;
        int pos = input.indexOf('\r');
        while (pos>0) {
            String line = input.substring(start, pos).trim();
            int parenPos = line.indexOf('(');
            if (parenPos>0 && parenPos<line.length()) {
                String fullMethod = line.substring(0,parenPos);
                int methodPoint = fullMethod.lastIndexOf('.');
                if (methodPoint>0 && methodPoint<fullMethod.length()) {
                    res.add(fullMethod.substring(methodPoint+1)+line.substring(parenPos));
                }
                else {
                    res.add(line);
                }
            }
            else {
                res.add(line);
            }
            start = pos+1;
            pos = input.indexOf('\r', start);
        }
        return res;
    }
    
    
    /**
     * A standardized way to trace a given exception to the system out.
     */
    public static JSONObject traceException(Exception e, String context) {
        JSONObject errOB = traceException(System.out, e, context);
        return errOB;
    }
    
    /**
     * A standardized way to trace a given exception to the system out.
     */
    public static JSONObject traceException(PrintStream out, Exception e, String context) {
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
