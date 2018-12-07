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

import java.util.ArrayList;

import com.purplehillsbooks.json.JSONArray;
import com.purplehillsbooks.json.JSONException;
import com.purplehillsbooks.json.JSONObject;

/**
 *
 * Author: Keith Swenson
 */
public class TestResultRecord {
    public TestResultRecord(String cat, String det, boolean pf, String[] newArgs) {
        category = cat;
        caseDetails = det;
        pass = pf;
        failureMessage = "";
        args = newArgs;
        duration = 0;
    }

    public String category;
    public String caseDetails;
    public boolean pass;
    public String failureMessage;
    public String[] args;
    public int duration; // milliseconds
    public ArrayList<String> savedLog;
    public Exception fatalException;



    public JSONObject getJSON() throws Exception {
        JSONObject jo = new JSONObject();
        jo.put("category", category);

        jo.put("caseDetails", caseDetails);
        jo.put("pass", pass);
        jo.put("failureMessage", failureMessage);
        jo.put("duration", duration);
        if (fatalException!=null) {
            jo.put("fatalException", JSONException.convertToJSON(fatalException, "TestResultRecord"));
        }

        if (args!=null) {
            JSONArray ja = new JSONArray();
            for (String arg : args) {
                ja.put(arg);
            }
            jo.put("args", ja);
        }

        if (savedLog!=null) {
            JSONArray loga = new JSONArray();
            for (String line : savedLog) {
                loga.put(line);
            }
            jo.put("log", loga);
        }

        return jo;
    }
}
