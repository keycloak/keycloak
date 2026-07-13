/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events.hooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SqlPreparedStatementTemplate {

    static final String FULL_PAYLOAD_PARAMETER = "$payload";
    static final String FULL_PAYLOAD_PREFIX = FULL_PAYLOAD_PARAMETER + ".";
    private static final String STRINGIFIED_DELIMITER = "##";

    private final String statement;
    private final List<String> parameterMappings;

    private SqlPreparedStatementTemplate(String statement, List<String> parameterMappings) {
        this.statement = statement;
        this.parameterMappings = List.copyOf(parameterMappings);
    }

    String statement() {
        return statement;
    }

    List<String> parameterMappings() {
        return parameterMappings;
    }

    static SqlPreparedStatementTemplate from(String sqlStatement, Object legacyConfiguredMappings) {
        List<String> inlineMappings = new ArrayList<>();
        String preparedStatement = replaceNamedParameters(sqlStatement, inlineMappings);
        if (!inlineMappings.isEmpty()) {
            return new SqlPreparedStatementTemplate(preparedStatement, inlineMappings);
        }

        return new SqlPreparedStatementTemplate(sqlStatement, legacyParameterMappings(legacyConfiguredMappings));
    }

    private static String replaceNamedParameters(String sqlStatement, List<String> parameterMappings) {
        StringBuilder preparedStatement = new StringBuilder(sqlStatement.length());

        for (int index = 0; index < sqlStatement.length(); index++) {
            char current = sqlStatement.charAt(index);

            if (current == '\'' || current == '"') {
                index = appendQuoted(sqlStatement, preparedStatement, index, current);
                continue;
            }

            if (startsWith(sqlStatement, index, "--")) {
                index = appendLineComment(sqlStatement, preparedStatement, index);
                continue;
            }

            if (startsWith(sqlStatement, index, "/*")) {
                index = appendBlockComment(sqlStatement, preparedStatement, index);
                continue;
            }

            if (current == ':' && startsWith(sqlStatement, index, "::")) {
                preparedStatement.append("::");
                index++;
                continue;
            }

            if (current == ':' && index + 1 < sqlStatement.length() && isParameterStart(sqlStatement.charAt(index + 1))) {
                int endIndex = index + 2;
                while (endIndex < sqlStatement.length() && isParameterPart(sqlStatement.charAt(endIndex))) {
                    endIndex++;
                }

                String placeholderName = sqlStatement.substring(index + 1, endIndex);
                parameterMappings.add(normalizePlaceholderName(placeholderName));
                preparedStatement.append('?');
                index = endIndex - 1;
                continue;
            }

            preparedStatement.append(current);
        }

        return preparedStatement.toString();
    }

    private static int appendQuoted(String sqlStatement, StringBuilder preparedStatement, int startIndex, char quoteCharacter) {
        preparedStatement.append(quoteCharacter);
        int index = startIndex + 1;

        while (index < sqlStatement.length()) {
            char current = sqlStatement.charAt(index);
            preparedStatement.append(current);

            if (current == quoteCharacter) {
                if (index + 1 < sqlStatement.length() && sqlStatement.charAt(index + 1) == quoteCharacter) {
                    preparedStatement.append(quoteCharacter);
                    index += 2;
                    continue;
                }
                return index;
            }

            index++;
        }

        return sqlStatement.length() - 1;
    }

    private static int appendLineComment(String sqlStatement, StringBuilder preparedStatement, int startIndex) {
        int index = startIndex;
        while (index < sqlStatement.length()) {
            char current = sqlStatement.charAt(index);
            preparedStatement.append(current);
            if (current == '\n') {
                return index;
            }
            index++;
        }
        return sqlStatement.length() - 1;
    }

    private static int appendBlockComment(String sqlStatement, StringBuilder preparedStatement, int startIndex) {
        preparedStatement.append("/*");
        int index = startIndex + 2;
        while (index < sqlStatement.length()) {
            char current = sqlStatement.charAt(index);
            preparedStatement.append(current);
            if (current == '*' && index + 1 < sqlStatement.length() && sqlStatement.charAt(index + 1) == '/') {
                preparedStatement.append('/');
                return index + 1;
            }
            index++;
        }
        return sqlStatement.length() - 1;
    }

    private static boolean startsWith(String value, int index, String prefix) {
        return value.regionMatches(index, prefix, 0, prefix.length());
    }

    private static boolean isParameterStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '$';
    }

    private static boolean isParameterPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '.' || value == '$';
    }

    private static String normalizePlaceholderName(String placeholderName) {
        if ("payload".equals(placeholderName) || FULL_PAYLOAD_PARAMETER.equals(placeholderName)) {
            return FULL_PAYLOAD_PARAMETER;
        }

        if (placeholderName.startsWith("payload.")) {
            return FULL_PAYLOAD_PREFIX + placeholderName.substring("payload.".length());
        }

        if (placeholderName.startsWith(FULL_PAYLOAD_PREFIX)) {
            return placeholderName;
        }

        return placeholderName;
    }

    private static List<String> legacyParameterMappings(Object configuredValue) {
        if (configuredValue == null) {
            return List.of(FULL_PAYLOAD_PARAMETER);
        }

        if (configuredValue instanceof String stringValue) {
            String trimmed = stringValue.trim();
            return trimmed.isEmpty()
                    ? List.of()
                    : List.of(trimmed.split(STRINGIFIED_DELIMITER)).stream()
                            .map(String::trim)
                            .filter(value -> !value.isBlank())
                            .toList();
        }

        if (configuredValue instanceof Collection<?> values) {
            return values.stream()
                    .map(value -> value == null ? null : value.toString().trim())
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        }

        String trimmed = configuredValue.toString().trim();
        return trimmed.isEmpty() ? List.of() : List.of(trimmed);
    }
}
