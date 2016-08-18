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

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class PathMatcher {

    private static final String ANY_RESOURCE_PATTERN = "/*";

    PathConfig matches(final String requestedUri, List<PathConfig> paths) {
        PathConfig actualConfig = null;

        for (PathConfig entry : paths) {
            String protectedUri = entry.getPath();
            String selectedUri = null;

            if (protectedUri.equals(ANY_RESOURCE_PATTERN) && actualConfig == null) {
                selectedUri = protectedUri;
            }

            int suffixIndex = protectedUri.indexOf(ANY_RESOURCE_PATTERN + ".");

            if (suffixIndex != -1) {
                String protectedSuffix = protectedUri.substring(suffixIndex + ANY_RESOURCE_PATTERN.length());

                if (requestedUri.endsWith(protectedSuffix)) {
                    selectedUri = protectedUri;
                }
            }

            if (protectedUri.equals(requestedUri)) {
                selectedUri = protectedUri;
            }

            if (protectedUri.endsWith(ANY_RESOURCE_PATTERN)) {
                String formattedPattern = removeWildCardsFromUri(protectedUri);

                if (!formattedPattern.equals("/") && requestedUri.startsWith(formattedPattern)) {
                    selectedUri = protectedUri;
                }

                if (!formattedPattern.equals("/") && formattedPattern.endsWith("/") && formattedPattern.substring(0, formattedPattern.length() - 1).equals(requestedUri)) {
                    selectedUri = protectedUri;
                }
            }

            int startRegex = protectedUri.indexOf('{');

            if (startRegex != -1) {
                String prefix = protectedUri.substring(0, startRegex);

                if (requestedUri.startsWith(prefix)) {
                    selectedUri = protectedUri;
                }
            }

            if (selectedUri != null) {
                selectedUri = protectedUri;
            }

            if (selectedUri != null) {
                if (actualConfig == null) {
                    actualConfig = entry;
                } else {
                    if (actualConfig.equals(ANY_RESOURCE_PATTERN)) {
                        actualConfig = entry;
                    }

                    if (protectedUri.startsWith(removeWildCardsFromUri(actualConfig.getPath()))) {
                        actualConfig = entry;
                    }
                }
            }
        }

        return actualConfig;
    }

    private String removeWildCardsFromUri(String protectedUri) {
        return protectedUri.replaceAll("/[*]", "/");
    }
}
