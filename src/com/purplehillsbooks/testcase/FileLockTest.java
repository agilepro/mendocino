package com.purplehillsbooks.testcase;

import java.io.File;
import java.util.*;

import com.purplehillsbooks.json.JSONException;

public class FileLockTest {
    
    
	/**
	 * Application Main
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
            String configFileName = "AA_StressTest.config";
    	    if (args.length>0) {
    	        configFileName = args[0];
    	        System.out.println("CONFIG FILE NAME: "+configFileName);
    	    }
    		FileLockTest pt = new FileLockTest();
			pt.start(configFileName);
		} catch (Exception e) {
			System.out.println("Execution ended with an error.");
			JSONException.traceException(e, "ProcessTest main level");
		}
	}
	
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	// Methods
	// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * Start testing. <br />
	 * Creating processes. Completing a task in every xx milliseconds.
	 */
	private void start(String configFileName) throws Exception {

		// Read from a configuration file
		File file = new File(configFileName);
		if (!file.exists()) {
			throw new Exception(String.format("Cound not find the config file in %s", file.getAbsolutePath()));
		}
		File resultfolder = new File("testResults");
		if (!resultfolder.exists()) {
		    resultfolder.mkdirs();
	        if (!resultfolder.exists()) {
	            throw new Exception("Failed to create the output folder: "+resultfolder);
	        }
		}
		
		int threadCount = 2;
		
        ArrayList<FileLockThread> threadList = new ArrayList<FileLockThread>();
        
        // Start Timer1 : This timer task checks process count periodically
        for (int i = 1; i<=threadCount; i++) {
            FileLockThread flt1 = new FileLockThread("#"+i);
            flt1.start();
            threadList.add(flt1);
        }

		// Keep moving until user types 'quit'
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Enter 'quit' to exit Application Variable Test");
			String str = scanner.nextLine();
			if ("quit".equalsIgnoreCase(str)) {
				System.out.println("Exiting!");
				break;
			}
		}
		scanner.close();

		for (FileLockThread flt : threadList) {
		    flt.die();
		}

		System.out.println("Shutting down in 3 seonds....");
		Thread.sleep(3000);
		
        for (FileLockThread flt : threadList) {
            flt.report(System.out);
        }
	}
	
	

}
