/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.Collection;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class PathMatcher<P> {

    private static final char WILDCARD = '*';

    public P matches(final String targetUri) {
        int patternCount = 0;
        int bracketsPatternCount = 0;
        P matchingPath = null;
        P matchingAnyPath = null;
        P matchingAnySuffixPath = null;

        for (P entry : getPaths()) {
            String expectedUri = getPath(entry);

            if (expectedUri == null) {
                continue;
            }

            String matchingUri = null;

            if (exactMatch(expectedUri, targetUri)) {
                matchingUri = expectedUri;
            }

            if (isTemplate(expectedUri)) {
                String templateUri = buildUriFromTemplate(expectedUri, targetUri, false);

                if (templateUri != null) {
                    int length = expectedUri.split("\\/").length;
                    int bracketsLength = expectedUri.split("\\{").length;

                    if (exactMatch(templateUri, targetUri) && (patternCount == 0 || length > patternCount || bracketsLength < bracketsPatternCount)) {
                        matchingUri = templateUri;
                        P resolved = resolvePathConfig(entry, targetUri);

                        if (resolved != null) {
                            entry = resolved;
                        }

                        patternCount = length;
                        bracketsPatternCount = bracketsLength;
                    }
                }
            }

            if (matchingUri != null) {
                StringBuilder path = new StringBuilder(expectedUri);
                int patternIndex = path.indexOf("/" + WILDCARD);

                if (patternIndex != -1) {
                    path.delete(patternIndex, path.length());
                }

                patternIndex = path.indexOf("{");

                if (patternIndex != -1) {
                    path.delete(patternIndex, path.length());
                }

                String pathString = path.toString();

                if ("".equals(pathString)) {
                    pathString = "/";
                }

                if (matchingUri.equals(targetUri) || pathString.equals(targetUri)) {
                    if (patternCount == 0) {
                        return entry;
                    } else {
                        matchingPath = entry;
                    }
                }

                if (WILDCARD == expectedUri.charAt(expectedUri.length() - 1)) {
                    if (matchingAnyPath == null) {
                        matchingAnyPath = entry;
                    } else {
                        String resourcePath = getPath(matchingAnyPath);

                        if (resourcePath.split("/").length < matchingUri.split("/").length) {
                            matchingAnyPath = entry;
                        }
                    }
                } else {
                    int suffixIndex = expectedUri.indexOf(WILDCARD + ".");

                    if (suffixIndex != -1) {
                        String protectedSuffix = expectedUri.substring(suffixIndex + 1);

                        if (targetUri.endsWith(protectedSuffix)) {
                            matchingAnySuffixPath = entry;
                        }
                    }
                }
            }
        }

        if (matchingPath != null) {
            return matchingPath;
        }

        if (matchingAnySuffixPath != null) {
            return matchingAnySuffixPath;
        }

        return matchingAnyPath;
    }

    protected abstract String getPath(P entry);

    protected abstract Collection<P> getPaths();

    private boolean exactMatch(String expectedUri, String targetUri) {
        if (targetUri.equals(expectedUri)) {
            return true;
        }

        if (endsWithWildcard(expectedUri)) {
            String rootPath = expectedUri.substring(0, expectedUri.length() - 1);

            if (targetUri.startsWith(rootPath)) {
                return true;
            }

            return targetUri.equals(rootPath.substring(0, rootPath.length() - 1));
        }

        String suffix = "/*.";
        int suffixIndex = expectedUri.indexOf(suffix);

        if (suffixIndex != -1) {
            return targetUri.endsWith(expectedUri.substring(suffixIndex + suffix.length() - 1));
        }

        return false;
    }

    protected String buildUriFromTemplate(String template, String targetUri, boolean onlyFirstParam) {
        StringBuilder uri = new StringBuilder(template);
        String expectedUri = uri.toString();
        int patternStartIndex = expectedUri.indexOf("{");

        if (expectedUri.endsWith("/*")) {
            expectedUri = expectedUri.substring(0, expectedUri.length() - 2);
        }

        if (patternStartIndex == -1 || patternStartIndex >= targetUri.length()) {
            return null;
        }

        if (expectedUri.split("/").length > targetUri.split("/").length) {
            return null;
        }

        char[] expectedUriChars = expectedUri.toCharArray();
        char[] matchingUri = Arrays.copyOfRange(expectedUriChars, 0, patternStartIndex);
        int matchingUriLastIndex = matchingUri.length;
        String targetUriParams = targetUri.substring(patternStartIndex);

        if (Arrays.equals(matchingUri, Arrays.copyOf(targetUri.toCharArray(), matchingUri.length))) {
            matchingUri = Arrays.copyOf(matchingUri, targetUri.length());
            int paramIndex = 0;
            int lastPattern = 0;

            for (int i = patternStartIndex; i < expectedUriChars.length; i++) {
                if (matchingUriLastIndex >= matchingUri.length) {
                    break;
                }

                char c = expectedUriChars[i];

                if (c == '{' || c == '*') {
                    String[] params = targetUriParams.split("/");

                    for (int k = paramIndex; k <= (c == '*' ? params.length : paramIndex); k++) {
                        if (k == params.length) {
                            break;
                        }

                        int paramLength = params[k].length();

                        if (matchingUriLastIndex + paramLength > matchingUri.length) {
                            return null;
                        }
                        
                        StringBuilder value = new StringBuilder();

                        for (int j = 0; j < paramLength; j++) {
                            char valueChar = params[k].charAt(j);
                            value.append(valueChar);
                            matchingUri[matchingUriLastIndex++] = valueChar;
                        }

                        if (c == '{') {
                            uri.replace(uri.indexOf("{", lastPattern), uri.indexOf("}", lastPattern) + 1, value.toString());
                        }

                        if (value.charAt(value.length() - 1) == '}') {
                            lastPattern = uri.indexOf(value.toString()) + value.length();
                        }

                        if (c == '*' && matchingUriLastIndex < matchingUri.length) {
                            matchingUri[matchingUriLastIndex++] = '/';
                        }
                    }

                    if (c == '{') {
                        i = expectedUri.indexOf('}', i);
                    }

                    if (i == expectedUri.lastIndexOf('}') && onlyFirstParam) {
                        return String.valueOf(matchingUri).substring(0, matchingUriLastIndex);
                    }
                } else {
                    if (c == '/') {
                        paramIndex++;
                    }
                    matchingUri[matchingUriLastIndex++] = c;
                }
            }

            return uri.toString();
        }

        return null;
    }

    public boolean endsWithWildcard(String expectedUri) {
        int length = expectedUri.length();
        return length > 0 && WILDCARD == expectedUri.charAt(length - 1);
    }

    private boolean isTemplate(String uri) {
        return uri.indexOf("{") != -1;
    }

    protected P resolvePathConfig(P entry, String path) {
        return entry;
    }
}

