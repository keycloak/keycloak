/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.adapters.authorization;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class PathMatcher {

    private static final char WILDCARD = '*';
    private final AuthzClient authzClient;
    // TODO: make this configurable
    private PathCache cache = new PathCache(100, 30000);

    public PathMatcher(AuthzClient authzClient) {
        this.authzClient = authzClient;
    }

    public PathConfig matches(final String targetUri, Map<String, PathConfig> paths) {
        PathConfig pathConfig = paths.get(targetUri) == null ? cache.get(targetUri) : paths.get(targetUri);

        if (pathConfig != null) {
            return pathConfig;
        }

        PathConfig matchingAnyPath = null;
        PathConfig matchingAnySuffixPath = null;

        for (PathConfig entry : paths.values()) {
            String expectedUri = entry.getPath();
            String matchingUri = null;

            if (exactMatch(expectedUri, targetUri, expectedUri)) {
                matchingUri = expectedUri;
            }

            if (isTemplate(expectedUri)) {
                String templateUri = buildUriFromTemplate(expectedUri, targetUri);

                if (templateUri != null) {
                    if (exactMatch(expectedUri, targetUri, templateUri)) {
                        matchingUri = templateUri;
                        entry = resolvePathConfig(entry, targetUri);
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

                if (matchingUri.equals(targetUri)) {
                    cache.put(targetUri, entry);
                    return entry;
                }

                if (WILDCARD == expectedUri.charAt(expectedUri.length() - 1)) {
                    matchingAnyPath = entry;
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

        if (matchingAnySuffixPath != null) {
            cache.put(targetUri, matchingAnySuffixPath);
            return matchingAnySuffixPath;
        }

        if (matchingAnyPath != null) {
            cache.put(targetUri, matchingAnyPath);
        }

        return matchingAnyPath;
    }

    private boolean exactMatch(String expectedUri, String targetUri, String value) {
        if (targetUri.equals(value)) {
            return value.equals(targetUri);
        }

        if (endsWithWildcard(expectedUri)) {
            return targetUri.startsWith(expectedUri.substring(0, expectedUri.length() - 2));
        }

        String suffix = "/*.";
        int suffixIndex = expectedUri.indexOf(suffix);

        if (suffixIndex != -1) {
            return targetUri.endsWith(expectedUri.substring(suffixIndex + suffix.length() - 1));
        }

        return false;
    }

    public String buildUriFromTemplate(String expectedUri, String targetUri) {
        int patternStartIndex = expectedUri.indexOf("{");

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

                        for (int j = 0; j < paramLength; j++) {
                            matchingUri[matchingUriLastIndex++] = params[k].charAt(j);
                        }

                        if (c == '*' && matchingUriLastIndex < matchingUri.length) {
                            matchingUri[matchingUriLastIndex++] = '/';
                        }
                    }

                    i = expectedUri.indexOf('}', i);
                } else {
                    if (c == '/') {
                        paramIndex++;
                    }
                    matchingUri[matchingUriLastIndex++] = c;
                }
            }

            if (matchingUri[matchingUri.length - 1] == '\u0000') {
                return null;
            }

            return String.valueOf(matchingUri);
        }

        return null;
    }

    public boolean endsWithWildcard(String expectedUri) {
        return WILDCARD == expectedUri.charAt(expectedUri.length() - 1);
    }

    private boolean isTemplate(String uri) {
        return uri.indexOf("{") != -1;
    }

    private PathConfig resolvePathConfig(PathConfig originalConfig, String path) {
        if (originalConfig.hasPattern()) {
            ProtectedResource resource = this.authzClient.protection().resource();
            Set<String> search = resource.findByFilter("uri=" + path);

            if (!search.isEmpty()) {
                // resource does exist on the server, cache it
                ResourceRepresentation targetResource = resource.findById(search.iterator().next()).getResourceDescription();
                PathConfig config = PolicyEnforcer.createPathConfig(targetResource);

                config.setScopes(originalConfig.getScopes());
                config.setMethods(originalConfig.getMethods());
                config.setParentConfig(originalConfig);
                config.setEnforcementMode(originalConfig.getEnforcementMode());

                return config;
            }
        }

        return originalConfig;
    }
}
