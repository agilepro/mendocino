package com.purplehillsbooks.json;

import java.io.File;

/**
* <p>The purpose of this class is to provide a command line that will
* read a JSON file, clean it up, and write it out again in canonical 
* form.   That is, the keys of all the objects will be put in ASCII
* alphabetical order according to unicode code-point and not sorted
* according to any particular language sort order.  
* The objects will then be formattted, and the values will 
* then be properly indented with 2 spaces per indent level.
* The output is to the file name specified in the second paramter.</p>
* 
* <pre>JSONFormatter {input-file-name} {output-file-name}</pre>
* 
* <p>If the file being read is not in valid JSON format, then an error
* will be reported and no output produced.</p>
*/
public class JSONFormatter {

/**
* <p>The purpose of this class is to provide a command line that will
* read a JSON file, clean it up, and write it out again in canonical 
* form.   That is, the keys of all the objects will be put in ASCII
* alphabetical order according to unicode code-point and not sorted
* according to any particular language sort order.  
* The objects will then be formattted, and the values will 
* then be properly indented with 2 spaces per indent level.
* The output is to the file name specified in the second paramter.</p>
* 
* <pre>JSONFormatter {input-file-name} {output-file-name}</pre>
* 
* <p>If the file being read is not in valid JSON format, then an error
* will be reported and no output produced.</p>
*/
    public static void main(String[] args) {
        try {
            if (args.length==0) {
                throw new Exception("Command: JSONFormatter <inputfile> <outputfile>");
            }
            String pathIn = args[0];
            String pathOut = pathIn;

            if (args.length>1) {
                pathOut = args[1];
            }
            File fileIn  = new File(pathIn);
            File fileOut = new File(pathOut);
            if (!fileIn.exists()) {
                throw new Exception("Can't file first file: "+fileIn.getCanonicalPath());
            }
            JSONObject theGreatObject = JSONObject.readFromFile(fileIn);
            theGreatObject.writeToFile(fileOut);

            //that is all it does, read and write the file!
        }
        catch (Exception e) {
            System.out.println("##### Failed to Format File");
            e.printStackTrace();
        }
    }

}
