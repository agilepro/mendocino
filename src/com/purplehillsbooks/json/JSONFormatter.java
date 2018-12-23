package com.purplehillsbooks.json;

import java.io.File;

public class JSONFormatter {

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
