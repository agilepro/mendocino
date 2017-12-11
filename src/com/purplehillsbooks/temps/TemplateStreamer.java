package com.purplehillsbooks.temps;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

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

        ArrayList<TemplateChunk> chunks = parseChunks(template);

        int positionCounter = 0;
        while (positionCounter < chunks.size()) {
            TemplateChunk chunk = chunks.get(positionCounter);

            try {
                if (!chunk.isToken) {
                    out.write(chunk.value);
                }
                else if (chunk.value.startsWith("!LOOP")) {
                    positionCounter = handleLoop(out, positionCounter, chunks, true, ttr);
                }
                else if (chunk.value.startsWith("!IF")) {
                    positionCounter = handleIf(out, positionCounter, chunks, true, ttr);
                }
                else if (chunk.value.startsWith("!RAW")) {
                    ArrayList<String> subparts = splitSpaces(chunk.value);
                    ttr.writeTokenValueRaw(out, subparts.get(1));
                }
                else if (chunk.value.startsWith("!DATE")) {
                    ArrayList<String> subparts = splitSpaces(chunk.value);
                    if (subparts.size()<3) {
                        throw new Exception("!DATE command must have two parameters, one for token and one for date format.");
                    }
                    ttr.writeTokenDate(out, subparts.get(1), subparts.get(2));
                }
                else if (chunk.value.startsWith("!DEBUG")) {
                    ttr.debugDump(out);
                }
                else {
                    ttr.writeTokenValue(out, chunk.value);
                }
                positionCounter++;
            }
            catch (Exception e) {
                throw new Exception("Problem on line "+chunk.lineNumber+" of template file.",e);
            }
        }

    }

    /**
     * Pass in the position of the LOOP begin, and this will return
     * the position of the LOOPEND so that process can continue AFTER that
     */
    private static int handleLoop(Writer out, int startPosition, ArrayList<TemplateChunk> chunks,
            boolean showOutput, TemplateTokenRetriever ttr) throws Exception {

        TemplateChunk loopStart = chunks.get(startPosition);
        ArrayList<String> parts = splitSpaces(loopStart.value);
        String command = parts.get(0);
        if (!"!LOOP".equals(command)) {
            throw new Exception("Don't understand the command "+command+" from template line "+loopStart.lineNumber);
        }

        if (parts.size()!=3) {
            throw new Exception("A LOOP command should have 3 parts, but this one has "+parts.size()
                    +" from template line "+loopStart.lineNumber);
        }
        String identifier = parts.get(1);
        String dataPath = parts.get(2);
        int loopCount = ttr.initLoop(identifier, dataPath);

        int pass = 0;
        int endPosition = -1;

        while (pass<loopCount) {
            int positionCounter = startPosition+1;
            ttr.setIteration(identifier, pass);
            while (positionCounter<chunks.size()) {
                TemplateChunk chunk = chunks.get(positionCounter);

                if (chunk.value.startsWith("!ENDLOOP")) {
                    endPosition = positionCounter;
                    break;
                }

                if (!chunk.isToken) {
                    if (showOutput) {
                        out.write(chunk.value);
                    }
                }
                else if (chunk.value.startsWith("!LOOP")) {
                    positionCounter = handleLoop(out, positionCounter, chunks, showOutput, ttr);
                }
                else if (chunk.value.startsWith("!IF")) {
                    positionCounter = handleIf(out, positionCounter, chunks, showOutput, ttr);
                }
                else if (chunk.value.startsWith("!RAW")) {
                    if (showOutput) {
                        ArrayList<String> subparts = splitSpaces(chunk.value);
                        ttr.writeTokenValueRaw(out, subparts.get(1));
                    }
                }
                else if (chunk.value.startsWith("!DATE")) {
                    if (showOutput) {
                        ArrayList<String> subparts = splitSpaces(chunk.value);
                        if (subparts.size()<3) {
                            throw new Exception("!DATE command must have two parameters, one for token and one for date format.");
                        }
                        ttr.writeTokenDate(out, subparts.get(1), subparts.get(2));
                    }
                }
                else if (chunk.value.startsWith("!DEBUG")) {
                    ttr.debugDump(out);
                }
                else {
                    if (showOutput) {
                        ttr.writeTokenValue(out, chunk.value);
                    }
                }

                positionCounter++;
            }
            pass++;
        }

        ttr.closeLoop(identifier);

        if (endPosition > 0 && endPosition < chunks.size()) {
            return endPosition;
        }

        //if we never found the end, then just report the last chunk in the set
        return chunks.size()-1;
    }



    /**
     * Pass in the position of the LOOP begin, and this will return
     * the position of the LOOPEND so that process can continue AFTER that
     */
    private static int handleIf(Writer out, int startPosition, ArrayList<TemplateChunk> chunks,
            boolean showOutput, TemplateTokenRetriever ttr) throws Exception {

        TemplateChunk loopStart = chunks.get(startPosition);
        ArrayList<String> parts = splitSpaces(loopStart.value);
        String command = parts.get(0);
        if (!"!IF".equals(command)) {
            throw new Exception("Don't understand the command "+command+" from template line "+loopStart.lineNumber);
        }

        if (parts.size()!=2) {
            throw new Exception("A IF command should have 2 parts, but this one has "+parts.size()
                    +" from template line "+loopStart.lineNumber);
        }
        String dataPath = parts.get(1);
        boolean hasValue = ttr.ifValue(dataPath);

        int endPosition = -1;

        int positionCounter = startPosition+1;
        while (positionCounter<chunks.size()) {
            TemplateChunk chunk = chunks.get(positionCounter);

            if (chunk.value.startsWith("!ENDIF")) {
                endPosition = positionCounter;
                break;
            }

            if (!chunk.isToken) {
                if (showOutput && hasValue) {
                    out.write(chunk.value);
                }
            }
            else if (chunk.value.startsWith("!LOOP")) {
                positionCounter = handleLoop(out, positionCounter, chunks, showOutput && hasValue, ttr);
            }
            else if (chunk.value.startsWith("!IF")) {
                positionCounter = handleIf(out, positionCounter, chunks, showOutput && hasValue, ttr);
            }
            else if (chunk.value.startsWith("!ELSE")) {
                //only thing we do is switch the logic of writing to non-writing and vice versa
                hasValue = !hasValue;
            }
            else if (chunk.value.startsWith("!RAW")) {
                if (showOutput) {
                    ArrayList<String> subparts = splitSpaces(chunk.value);
                    ttr.writeTokenValueRaw(out, subparts.get(1));
                }
            }
            else if (chunk.value.startsWith("!DATE")) {
                if (showOutput) {
                    ArrayList<String> subparts = splitSpaces(chunk.value);
                    if (subparts.size()<3) {
                        throw new Exception("!DATE command must have two parameters, one for token and one for date format.");
                    }
                    ttr.writeTokenDate(out, subparts.get(1), subparts.get(2));
                }
            }
            else if (chunk.value.startsWith("!DEBUG")) {
                ttr.debugDump(out);
            }
            else {
                if (showOutput && hasValue) {
                    ttr.writeTokenValue(out, chunk.value);
                }
            }

            positionCounter++;
        }

        if (endPosition > 0 && endPosition < chunks.size()) {
            return endPosition;
        }

        //if we never found the end, then just report the last chunk in the set
        return chunks.size()-1;
    }



    private static ArrayList<TemplateChunk> parseChunks(Reader template) throws Exception {
        LineNumberReader lnr = new LineNumberReader(template);
        ArrayList<TemplateChunk> chunks = new ArrayList<TemplateChunk>();


        StringBuffer sb = new StringBuffer();
        while (true) {
            int spanLineStart = lnr.getLineNumber();

            int ch = lnr.read();
            if (ch < 0) {
                chunks.add(new TemplateChunk(false,sb.toString(), spanLineStart));
                return chunks;
            }

            if (ch != '{') {
                sb.append((char)ch);
                continue;
            }

            ch = lnr.read();
            if (ch < 0) {
                chunks.add(new TemplateChunk(false,sb.toString(), spanLineStart));
                return chunks;
            }

            if (ch != '{') {
                sb.append('{');
                sb.append((char)ch);
                continue;
            }

            chunks.add(new TemplateChunk(false,sb.toString(), spanLineStart));
            sb = new StringBuffer();

            // now we definitely have a token

            int tokenLineStart = lnr.getLineNumber();
            try {
                StringBuffer tokenVal = new StringBuffer();
                ch = lnr.read();
                if (ch < 0) {
                    throw new Exception(
                        "Source stream ended before finding a closing brace character");
                }

                while (ch != '}') {
                    tokenVal.append((char) ch);
                    ch = lnr.read();
                    if (ch < 0) {
                        throw new Exception(
                            "Source stream ended before finding a closing brace character");
                    }
                }

                // now we have see the closing brace
                chunks.add(new TemplateChunk(true,tokenVal.toString(), tokenLineStart));


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
                        + tokenLineStart, e);
            }
        }
    }

    private static class TemplateChunk {
        boolean isToken;
        int lineNumber;
        String value;

        TemplateChunk(boolean m, String s, int line) {
            isToken = m;
            value = s;
            lineNumber = line;
        }
    }


    /**
     * Breaks a string into a list of strings using spaces
     * as separators of the token, and trimming each token of
     * extra spaces if there are any.
     */
    private static ArrayList<String> splitSpaces(String val) {
        ArrayList<String> ret = new ArrayList<String>();

        if (val==null) {
            return ret;
        }

        int pos = 0;
        int spacePos = val.indexOf(" ");
        while (spacePos >= pos) {
            if (spacePos > pos) {
                ret.add(val.substring(pos,spacePos));
            }
            pos = spacePos + 1;

            //skip any extra spaces
            while (pos<val.length() && val.charAt(pos)==' ') {
                pos++;
            }

            if (pos >= val.length()) {
                break;
            }
            spacePos = val.indexOf(" ", pos);
        }
        if (pos<val.length()) {
            ret.add(val.substring(pos).trim());
        }
        return ret;
    }

}
