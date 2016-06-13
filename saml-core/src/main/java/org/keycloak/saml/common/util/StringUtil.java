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
package org.keycloak.saml.common.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility dealing with Strings
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 21, 2009
 */
public class StringUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Check whether the passed string is null or empty
     *
     * @param str
     *
     * @return
     */
    public static boolean isNotNull(String str) {
        return str != null && !"".equals(str.trim());
    }

    /**
     * Check whether the string is null or empty
     *
     * @param str
     *
     * @return
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * <p>
     * Get the system property value if the string is of the format ${sysproperty}
     * </p>
     * <p>
     * You can insert default value when the system property is not set, by separating it at the beginning with ::
     * </p>
     * <p>
     * <b>Examples:</b>
     * </p>
     *
     * <p>
     * ${idp} should resolve to a value if the system property "idp" is set.
     * </p>
     * <p>
     * ${idp::http://localhost:8080} will resolve to http://localhost:8080 if the system property "idp" is not set.
     * </p>
     *
     * @param str
     *
     * @return
     */
    public static String getSystemPropertyAsString(String str) {
        if (str == null)
            throw logger.nullArgumentError("str");
        if (str.contains("${")) {
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
            Matcher matcher = pattern.matcher(str);

            StringBuffer buffer = new StringBuffer();
            String sysPropertyValue = null;

            while (matcher.find()) {
                String subString = matcher.group(1);
                String defaultValue = "";

                // Look for default value
                if (subString.contains("::")) {
                    int index = subString.indexOf("::");
                    defaultValue = subString.substring(index + 2);
                    subString = subString.substring(0, index);
                }
                sysPropertyValue = SecurityActions.getSystemProperty(subString, defaultValue);
                if (sysPropertyValue.isEmpty()) {
                    throw logger.systemPropertyMissingError(matcher.group(1));
                }else{
                    // sanitize the value before we use append-and-replace
                    sysPropertyValue = Matcher.quoteReplacement(sysPropertyValue);
                }
                matcher.appendReplacement(buffer, sysPropertyValue);
            }

            matcher.appendTail(buffer);
            str = buffer.toString();
        }
        return str;
    }

    /**
     * Match two strings else throw a {@link RuntimeException}
     *
     * @param first
     * @param second
     */
    public static void match(String first, String second) {
        if (!first.equals(second))
            throw logger.notEqualError(first, second);
    }

    /**
     * Given a comma separated string, get the tokens as a {@link List}
     *
     * @param str
     *
     * @return
     */
    public static List<String> tokenize(String str) {
        return tokenize(str, ",");
    }

    /**
     * Given a delimited string, get the tokens as a {@link List}
     *
     * @param str
     * @param delimiter the delimiter
     *
     * @return
     */
    public static List<String> tokenize(String str, String delimiter) {
        List<String> list = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
        while (tokenizer.hasMoreTokens()) {
            list.add(tokenizer.nextToken());
        }
        return list;
    }

    /**
     * Given a string that is comma delimited and contains key-value pairs
     *
     * @param keyValuePairString
     *
     * @return
     */
    public static Map<String, String> tokenizeKeyValuePair(String keyValuePairString) {
        Map<String, String> map = new HashMap<String, String>();

        List<String> tokens = tokenize(keyValuePairString);
        for (String token : tokens) {
            int location = token.indexOf('=');
            map.put(token.substring(0, location), token.substring(location + 1));
        }
        return map;
    }

    public static String[] split(String toSplit, String delimiter) {
        if (delimiter.length() != 1) {
            throw new IllegalArgumentException("Delimiter can only be one character in length");
        }

        int offset = toSplit.indexOf(delimiter);

        if (offset < 0) {
            return null;
        }

        String beforeDelimiter = toSplit.substring(0, offset);
        String afterDelimiter = toSplit.substring(offset + 1);

        return new String[]{beforeDelimiter, afterDelimiter};
    }
}