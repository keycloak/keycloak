/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tkyjovsk
 */
public class Timer {

    private static Long time;

    private static final Map<String, List<Long>> stats = new HashMap<>();

    public static void time() {
        time = new Date().getTime();
    }

    public static void time(String operation) {
        if (time == null) {
            System.out.println(MessageFormat.format("Starting timer for operation {0}", operation));
            time();
        } else {
            long timeOrig = time;
            time();
            logOperation(operation, time - timeOrig);
            System.out.println(MessageFormat.format("Operation {0} took {1} ms", operation, time - timeOrig));
        }
    }

    private static void logOperation(String operation, long delta) {
        if (!stats.containsKey(operation)) {
            stats.put(operation, new ArrayList<Long>());
        }
        stats.get(operation).add(delta);
    }

    public static void printStats() {
        if (!stats.isEmpty()) {
            System.out.println("OPERATION STATS:");
        }
        for (String op : stats.keySet()) {
            long sum = 0;
            for (Long t : stats.get(op)) {
                sum += t;
            }
            System.out.println(MessageFormat.format("Operation {0} average time: {1,number,#} ms", op, sum / stats.get(op).size()));
        }
        stats.clear();
    }

}
