/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.utils;

import java.util.Collection;

public class StringUtil {

    /**
     * Returns true if string is null or blank
     */
    public static boolean isBlank(String str) {
        return !(isNotBlank(str));
    }

    /**
     * Returns true if string is not null and not blank
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }

    /**
     * Returns true if string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Calling:
     * <pre>joinValuesWithLogicalCondition("or", Arrays.asList("foo", "bar", "baz", "caz" ))</pre>
     * will return "foo, bar, baz or caz"
     *
     * @param conditionText condition
     * @param values values to be joined with the condition at the end
     * @return see the example above
     */
    public static String joinValuesWithLogicalCondition(String conditionText, Collection<String> values) {
        StringBuilder options = new StringBuilder();
        int i = 1;
        for (String o : values) {
            if (i == values.size()) {
                options.append(" " + conditionText + " ");
            } else if (i > 1) {
                options.append(", ");
            }
            options.append(o);
            i++;
        }
        return options.toString();
    }

    /**
     * Utility method that substitutes any isWhitespace char to common space ' ' or character 20.
     * The idea is removing any weird space character in the string like \t, \n, \r.
     * If quotes character is passed the quotes char is escaped to mark is not the end
     * of the value (for example escaped \" if quotes char " is found in the string).
     *
     * @param str The string to normalize
     * @param quotes The quotes to escape (for example " or '). It can be null.
     * @return The string without weird whitespaces and quotes escaped
     */
    public static String sanitizeSpacesAndQuotes(String str, Character quotes) {
        // idea taken from commons-lang StringUtils.normalizeSpace
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = null;
        for (int i = 0; i < str.length(); i++) {
            final char actualChar = str.charAt(i);
            if ((Character.isWhitespace(actualChar) && actualChar != ' ') || actualChar == 160) {
                if (sb == null) {
                    sb = new StringBuilder(str.length() + 10).append(str.substring(0, i));
                }
                sb.append(' ');
            } else if (quotes != null && actualChar == quotes) {
                if (sb == null) {
                    sb = new StringBuilder(str.length() + 10).append(str.substring(0, i));
                }
                sb.append('\\').append(actualChar);
            } else if (sb != null) {
                sb.append(actualChar);
            }
        }
        return sb == null? str : sb.toString();
    }

    public static String removeSuffix(String str, String suffix) {
        int index = str.lastIndexOf(suffix);
        if (str.endsWith(suffix) && index > 0) {
            str = str.substring(0, index);
        }
        return str;
    }
}
