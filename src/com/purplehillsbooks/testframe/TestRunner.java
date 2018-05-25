/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.purplehillsbooks.testframe;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

import com.purplehillsbooks.json.JSONException;

/**
 *
 * Author: Keith Swenson
 */

public class TestRunner {

    /**
     * @param args
     */
    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {

        try {

            if (args.length < 4) {
                System.out.println("usage: com.fujitsu.iflowqa.testframe.TestRunner <userid> "
                            +"<password> <NamingProvider> <NamingProviderURL> "
                            +"[test case or testcase file list like]");
                System.out.println("e.g.: com.fujitsu.iflowqa.testframe.TestRunner "
                            +"ibpm_server1 Infosys123 weblogic.jndi.WLInitialContextFactory "
                            +"t3://localhost:7001");
                return;
            }

            String className = "normalList.txt";

            Properties prop = new Properties();
            prop.put("NamingProvider", args[2]);
            prop.put("NamingProviderURL", args[3]);

            prop.put("JmsNamingProvider", args[2]);
            prop.put("JmsNamingProviderURL", args[3]);
            prop.put("JmsFactory", "iflow.TopicConnectionFactory");
            prop.put("UserAgentServiceName", "iflow.UserAgentService");
            prop.put("JmsNotificationTopic", "iflow.NotificationTopic");
            prop.put("JmsSQNotificationTopic", "iflow.SQNotificationTopic");
            prop.put("JmsEmailNotificationTopic", "iflow.EmailNotificationTopic");
            prop.put("JmsCommandTopic", "iflow.CommandTopic");
            prop.put("JmsResponseTopic", "iflow.ResponseTopic");
            prop.put("JmsFilterEnabled", "false");
            prop.put("NumberOfDispatcherThread", "1");
            prop.put("TWFTransportType", "EE");

            PrintStream outStream = System.out;
            PrintWriter out = new PrintWriter(outStream);

            String[] testArgs = null;
            if (args.length > 4) {
                testArgs = new String[args.length - 4];
                for (int i = 4; i < args.length; i++) {
                    testArgs[i - 4] = args[i];
                }
            }
            else {
                testArgs = new String[] { className };
            }

            TestRecorderText tr = new TestRecorderText(out, true, testArgs, prop);
            try {
                TestDriver.DriveTests(tr, testArgs);
                out.flush();
            }
            catch (Exception e) {
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                out.write("Fatal Error, in " + className + "\n");
                TestDriver.deparenthesize(out, e.toString());
                JSONException.traceException(out, e, "Fatal Error, in " + className);
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            }

            out.write("\nSummary: " + tr.passedCount());
            out.write(" passed, " + tr.failedCount());
            out.write(" failed, " + tr.fatalCount());
            out.write(" fatal errors in ");
            out.write(className);
            out.flush();
            if (args.length > 1) {
                out.write(" ");
                out.write(args[1]);
                out.flush();
            }
            out.write("\n");
            out.flush();

            // since this test recorded was created above, there are no
            // "previous"
            // values to pass in, just pass 0 and 0.

            if (tr.fatalCount() > 0) {
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
                out.write("Printing configuration since there was a fatal test\n");
                @SuppressWarnings("unchecked")
                Enumeration<String> enumeration = (Enumeration<String>) tr.getProps()
                        .propertyNames();
                while (enumeration.hasMoreElements()) {
                    String aName = enumeration.nextElement();
                    String val = tr.getProps().getProperty(aName);
                    out.write(aName + "=" + val + "\n");
                }
                out.write("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

            }
            out.flush();
        }
        catch (Exception e) {
            JSONException.traceException(e, "TestRunner.run");
        }
    }
}
