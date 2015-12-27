package org.workcast.streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Writer;

/**
 * <p>
 * Streams a template file to a Writer object while searching for tokens,
 * and streaming token values in place of those token.  The best way to
 * explain this is by example. Consider the following template:
 * </p>
 * <pre>
 * &lt;html&gt;
 * &lt;body&gt;
 * Dear {{customer name}},
 * Your account has a balance of {{account balance}}.
 * Sincerely,
 * The Bank
 * &lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 *
 * <p>
 * In this example, there are two tokens.  The tokens start with "{{" and
 * and with "}}" and everything between these is called the token name.
 * The names of the tokens in this example are
 * "customer name" and "account balance".  The TemplateStreamer will stream
 * out everything in the template, but inplace of the tokens, it will instead
 * stream the appropriate associated values.
 * A supplied TemplateTokenRetriever object locates and streams the associated values.
 *</p>
 * <p>
 * The TemplateStreamer class does not know what values need to be substituted
 * for the tokens. When the TemplateStreamer instance is constructed, the host
 * program must supply a TemplateTokenRetriever object that will provide values
 * for each of the tokens.  The TemplateStreamer will parse the template, and will
 * stream all the non-token parts, but it will call the TemplateTokenRetriever
 * for each of the tokens that it find, passing the name of the token to be output.
 * The TemplateTokenRetriever then looks up and finds the value using whatever
 * means necessary, and streams the value to the Writer, and returns.
 * </p>
 * <p>
 * We have called this "QuickForms" in the past. It is a lightweight mechanism
 * that allows you to create user interface screens in HTML, and then substitute
 * values into those screens as the screen is being served. Much lighter weight
 * than JSP file, there is no compiler needed.  Templates are parsed and streamed
 * in a single action, which allows template files to be changed at any time, and
 * there is no need to re-compile the templates.  The parsing and streaming is
 * very efficient and fast, incurring only an inperceptibly small overhead over
 * simply streaming the file directly without parsing.  The token values are
 * streamed directly to the output, which eliminates any extra manipulation
 * of string values normaly found in approaches that concatenate strings
 * before producing output.
 * </p><p>
 * The file on disk is called a "template" It is a text file of any type,
 * usually HTML. There are tokens that start with two curley braces "{{" and end
 * with two curley braces "}}".
 *
 * </p><p>
 * A single brace alone will be ignored. Everything that is not between the
 * curley brace delimiter will be streamed out without change. When a token has
 * been found, the content (that is the text between the braces) will be passed
 * to the calling object in a "callback" method. The result of the callback is
 * the value to place into the template (if any).
 *
 * </p><p>
 * As a template design, you decide what token values are valuable for your
 * situation, the TemplateStreamer does not care what the tokens are. The string
 * value between the double curley braces (a.k.a. the token name) is passed unchanged
 * to the TemplateTokenRetriever.  The token name might have multiple parameters
 * or even an expression of any syntax.  The only restriction that exists on the
 * token name is that it must not contain "}}" (the ending double curley brace.)
 *
 * </p><p>
 * All the methods are static.  There is no need to create an instance of this class.
 *
 * </p><p>
 * How does this compare to JSP? Well, there are no looping constructs or
 * branching constructs. It is really designed for flat files that simply need
 * some run-time values placed into them.  This is greate for simple web pages,
 * and particularly for email templates -- basically anytime you have something
 * that looks like HTML, but just has a few values substituted in.
 * </p>
 */
public class TemplateStreamer {

    /**
     * streamRawFile simply reads the File passed in, and streams it to output
     * byte for byte, WITHOUT any modification. This is a convenience function.
     * Exception if the file passed in does not exist. Exception if the file has
     * zero bytes length on assumption this must be a mistake.
     */
    public static void streamRawFile(OutputStream out, File resourceFile) throws Exception {

        if (!resourceFile.exists()) {
            throw new Exception("The file (" + resourceFile.toString()
                    + ") does not exist and can not be streamed as a template.");
        }

        InputStream is = new FileInputStream(resourceFile);
        byte[] buf = new byte[800];
        int amt = is.read(buf);
        int count = 0;
        while (amt >= 0) {
            out.write(buf, 0, amt);
            count += amt;
            amt = is.read(buf);
        }

        is.close();
        if (count == 0) {
            throw new Exception("Hey, the resource (" + resourceFile + ") had zero bytes in it!");
        }

        out.flush();
    }

    /**
     * A convenience routine to properly encode values for use in HTML.
     * Does the proper encoding for a value to be placed in an HTML file,
     * and have it display exactly as the string is in Java.
     * Always HTML encode all user-entered data, even if you think it should
     * not need it, because this will prevent hackers from injecting scripts
     * into the streamed output.
     */
    public static void writeHtml(Writer w, String t) throws Exception {
        if (t == null) {
            return; // treat it like an empty string, don't write "null"
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            switch (c) {
            case '&':
                w.write("&amp;");
                continue;
            case '<':
                w.write("&lt;");
                continue;
            case '>':
                w.write("&gt;");
                continue;
            case '"':
                w.write("&quot;");
                continue;
            default:
                w.write(c);
                continue;
            }
        }
    }

    /**
    * This is the main method used to stream a template file, reading from the file,
    * writing to the supplied Writer, and substituting values for the tokens.
    *
    * @param out the writer that the template will be streamed to
    * @param file the abstract file path that locates the file to read
    * @param charset the character encoding of the template file to be read
    * @param ttr the TemplateTokenRetriever that understands how to find values
    *        for each of the possible token names in the file, and can stream
    *        that value to the Writer object.
    * @exception an exception will be thrown if anything does not perform
    *        correctly: for example if the file is not found, or can not be
    *        opened, or can not be read, or the output Writer can not be written
    *        to, or the system runs out of memory, or if the template has a
    *        syntax error such as a missing end of token double brace.  The exception
    *        will explain the problem that occurred.  The host program should take
    *        care to properly log or otherwise communicate this problem to the
    *        appropriate channel, because the TemplateStreamer does not keep
    *        any log.
    */
    public static void streamTemplate(Writer out, File file, String charset,
            TemplateTokenRetriever ttr) throws Exception {
        try {
            InputStream is = new FileInputStream(file);
            Reader isr = new InputStreamReader(is, charset);
            streamTemplate(out, isr, ttr);
            isr.close();
            out.flush();
        }
        catch (Exception e) {
            throw new Exception("Error with template file (" + file + ").", e);
        }
    }

    /**
     * This is the stream-in/stream-out version of the TemplateStreamer.
     * Reads text from the Reader, and output it to the Writer while
     * searching for and substituting tokens.  Use this form when the
     * template is not stored as a simple file on the disk.
     */
    public static void streamTemplate(Writer out, Reader template, TemplateTokenRetriever ttr)
            throws Exception {
        LineNumberReader lnr = new LineNumberReader(template);

        while (true) {

            int ch = lnr.read();
            if (ch < 0) {
                return;
            }

            if (ch != '{') {
                out.write(ch);
                continue;
            }

            ch = lnr.read();
            if (ch < 0) {
                return;
            }

            if (ch != '{') {
                out.write('{');
                out.write(ch);
                continue;
            }

            // now we definitely have a token
            int tokenLineStart = lnr.getLineNumber();

            try {
                StringBuffer tokenVal = new StringBuffer();
                ch = lnr.read();
                if (ch < 0) {
                    throw new Exception(
                            "Template source stream ended before finding a closing brace character");
                }

                while (ch != '}') {
                    tokenVal.append((char) ch);
                    ch = lnr.read();
                    if (ch < 0) {
                        throw new Exception(
                                "Template source stream ended before finding a closing brace character");
                    }
                }

                // now we have see the closing brace
                ttr.writeTokenValue(out, tokenVal.toString());

                // read one more character, to get rid of the second closing
                // brace.
                ch = lnr.read();
                if (ch != '}') {
                    throw new Exception(
                            "Found one, but did not find the second closing brace character");
                }
            }
            catch (Exception e) {
                throw new Exception("Problem with template token starting on line "
                        + tokenLineStart);
            }
        }
    }

}