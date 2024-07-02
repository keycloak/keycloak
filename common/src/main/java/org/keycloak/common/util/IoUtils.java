/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.common.util;

import java.io.Console;

public class IoUtils {

    public static String readFromConsole(String kind, String defaultValue, boolean password) {
        Console cons = System.console();
        if (cons == null) {
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new RuntimeException(String.format("Console is not active, but %s is required", kind));
        }
        String prompt = String.format("Enter %s", kind) + (defaultValue != null ? String.format(" [%s]:", defaultValue) : ":");
        if (password) {
	        char[] passwd;
	        if ((passwd = cons.readPassword(prompt)) != null) {
	            return new String(passwd);
	        }
        } else {
        	return cons.readLine(prompt);
        }
        throw new RuntimeException(String.format("No %s provided", kind));
    }

    public static String readPasswordFromConsole(String kind) {
        return readFromConsole(kind, null, true);
    }
    
    public static String readLineFromConsole(String kind, String defaultValue) {
        return readFromConsole(kind, defaultValue, false);
    }

}
