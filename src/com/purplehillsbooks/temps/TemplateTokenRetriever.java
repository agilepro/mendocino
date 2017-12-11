package com.purplehillsbooks.temps;

import java.io.Writer;

/**
* <p>
 * To use the TemplateStreamer, you have to supply a TemplateTokenRetriever
 * to stream token values for the specific tokens.
 * Create a class that implements this interface, and pass an instance into
 * the TemplateStreamer.
 * </p><p>
 * There is only one method to implement, writeTokenValue().  This is given
 * a Writer object to stream the value to, and the token name (the text that
 * was between the double curley braces).
 * </p><p>
 * For example, consider the following template:
 * </p>
 * <pre>
 *    You have a balance of {{amt}} in your account.
 * </pre>
 * <p>
 * This template there is one token.  The name of that token is "amt".
 * The template will stream out all of the text up to the "{{".
 * Then it will call the TemplateTokenRetrieve, passing the Writer object,
 * and the string template name "amt".   Your TemplateTokenRetriever will use the
 * "amt" name to find the real amount value, and stream that to the Writer.
 * After returning from the call, the TemplateStreamer will write the rest of the
 * text of the template, everything after the "}}".
 * </p>
 * <p>
 * That is really all there is to it.  Use the token "name" to find the value, and
 * then stream it to the output. There are no constraints on the name and there
 * is no reason this has to be a simple value.  If you wish, you could have
 * multiple parameters, or even a complex expression of any syntax.  The only
 * constraint is that the name can NOT contain two curley braces in a row.
 * </p><p>
 * User proper encoding: If your template is supposed to be HTML output,
 * then be sure that when you stream the value, that you encode that value properly
 * for HTML.
 */
public interface TemplateTokenRetriever {

    /**
     * The token passed will be all text between the double curly brace
     * delimiter. This method must determine what associated value is, and write
     * it out.
     *
     * The token can be as complicated an expressions as you want, but it can
     * not have any curly brace characters in it anywhere. You might have a
     * token with multiple parameters as long as you parse out the parameters
     * and recognize their values. If your implementation recognizes the token,
     * then write the value, properly encoded, to the output stream.
     *
     * If the template is HTML, then remember to encode the value using HTML
     * encoding, perhaps by using the TemplateStreamer.writeHtml() function. If
     * you are not sure that the value needs encoding, then encode anyway,
     * because it will eliminate many forms of hacking attacks.
     *
     * Note: throwing an exception from this will cause the streaming of the
     * rest of the template to stop.
     */
    public void writeTokenValue(Writer out, String token) throws Exception;

    /**
     * List writeTokenValue, but without the HTML encoding.
     * Used to include preformatted HTML into the output.
     * BUt be careful, this can be dangerous.
     */
    public void writeTokenValueRaw(Writer out, String token) throws Exception;

    
    /**
     * token specifies a date, and this will format the date for 
     * inclusion in the form using the format specified in the last parameter.
     */
    public void writeTokenDate(Writer out, String token, String format) throws Exception;


    /**
     * A loop has an identifier which functions as the iterated data item,
     * and a token which refers to the array that you are iterating over.
     *
     * The id passed was specified by the user, and it will serve as a
     * variable that can access the iterated object.  This id value may
     * be passed as a token in a regular data write token, so the TTR
     * must remember this id, and check first for this.
     *
     * There could be any number of levels of loops, each one must have
     * a unique id.
     */
    public int initLoop(String id, String token) throws Exception;


    /**
     * Before each time through a loop, this will be called to say which
     * iteration of the loop it is in, and therevore which index element of the
     * chosen array (see initLoop) should be accessed
     */
    public void setIteration(String id, int loopCount) throws Exception;


    /**
     * When a loop is finished, this is called so that the id can be freed up
     * and references to this id value will no longer work.
     */
    public void closeLoop(String id) throws Exception;


    /**
     * Looks at the value specified, and returns boolean
     *
     * scalar:
     *     true means for a scalar that there is a value there, and that the
     *          value is not 0, false, or null.
     *     false means it is missing, empty, null, 0, or nullstring.
     *
     * array:
     *     true means there is one or more entries
     *     false means either it is missing, or it has no entries
     *
     */
    public boolean ifValue(String token) throws Exception;
    
    /**
     * Produce an output which represents all of the tokens that can be used
     * in the current template given the specific context.  In other words
     * make a dump of all the data values right now so we know what values can
     * be used in the tokens.
     */
    public void debugDump(Writer out) throws Exception;

}
