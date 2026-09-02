/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility methods for validating and matching regular expressions.
 */
public class RegexUtils {

    /**
     * Default maximum regex length — limits complexity to mitigate ReDoS attacks.
     */
    public static final int DEFAULT_MAX_LENGTH = 512;

    /**
     * Validates whether the given string is a syntactically valid regular expression
     * with a maximum length of {@link #DEFAULT_MAX_LENGTH} and groups allowed.
     *
     * @param regexp the regular expression to validate
     * @return {@code true} if the expression is valid, {@code false} otherwise
     */
    public static boolean isValidRegex(String regexp) {
        return isValidRegex(regexp, DEFAULT_MAX_LENGTH, true);
    }

    /**
     * Validates whether the given string is a syntactically valid regular expression.
     *
     * @param regexp      the regular expression to validate
     * @param maxLength   the maximum allowed length of the expression
     * @param allowGroups whether capturing and non-capturing groups (parentheses) are permitted
     * @return {@code true} if the expression is valid, {@code false} if it is {@code null},
     *         exceeds {@code maxLength}, contains groups when disallowed, or has invalid syntax
     */
    public static boolean isValidRegex(String regexp, int maxLength, boolean allowGroups) {
        if (regexp == null || regexp.length() > maxLength) {
            return false;
        }
        if (!allowGroups && (regexp.contains("(") || regexp.contains(")"))) {
            return false;
        }
        try {
            Pattern.compile(regexp);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public static boolean valueMatchesRegex(String regex, Object value) {
        if (value instanceof List) {
            List list = (List) value;
            for (Object val : list) {
                if (valueMatchesRegex(regex, val)) {
                    return true;
                }
            }
        } else {
            if (value != null) {
                String stringValue = value.toString();
                return stringValue != null && stringValue.matches(regex);
            }
        }
        return false;
    }
}
