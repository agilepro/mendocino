package org.workcast.json;

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

}
