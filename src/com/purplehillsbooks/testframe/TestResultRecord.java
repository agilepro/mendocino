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
}
