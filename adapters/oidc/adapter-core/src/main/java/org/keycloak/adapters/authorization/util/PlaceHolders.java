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
package org.keycloak.adapters.authorization.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PlaceHolders {

    private static Map<String, PlaceHolderResolver> resolvers = new HashMap<>();

    static {
        resolvers.put(RequestPlaceHolderResolver.NAME, new RequestPlaceHolderResolver());
        resolvers.put(KeycloakSecurityContextPlaceHolderResolver.NAME, new KeycloakSecurityContextPlaceHolderResolver());
    }

    private static Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(.+?)\\}");
    private static Pattern PLACEHOLDER_PARAM_PATTERN = Pattern.compile("\\[(.+?)\\]");

    public static List<String> resolve(String value, HttpFacade httpFacade) {
        Map<String, List<String>> placeHolders = parsePlaceHolders(value, httpFacade);

        if (!placeHolders.isEmpty()) {
            value = formatPlaceHolder(value);

            for (Entry<String, List<String>> entry : placeHolders.entrySet()) {
                List<String> values = entry.getValue();

                if (values.isEmpty() || values.size() > 1) {
                    return values;
                }

                value = value.replaceAll(entry.getKey(), values.get(0)).trim();
            }
        }

        return Arrays.asList(value);
    }

    static String getParameter(String source, String messageIfNotFound) {
        Matcher matcher = PLACEHOLDER_PARAM_PATTERN.matcher(source);

        while (matcher.find()) {
            return matcher.group(1).replaceAll("'", "");
        }

        if (messageIfNotFound != null) {
            throw new RuntimeException(messageIfNotFound);
        }

        return null;
    }

    private static Map<String, List<String>> parsePlaceHolders(String value, HttpFacade httpFacade) {
        Map<String, List<String>> placeHolders = new HashMap<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

        while (matcher.find()) {
            String placeHolder = matcher.group(1);
            int resolverNameIdx = placeHolder.indexOf('.');

            if (resolverNameIdx == -1) {
                throw new RuntimeException("Invalid placeholder [" + value + "]. Could not find resolver name.");
            }

            PlaceHolderResolver resolver = resolvers.get(placeHolder.substring(0, resolverNameIdx));

            if (resolver != null) {
                List<String> resolved = resolver.resolve(placeHolder, httpFacade);

                if (resolved != null) {
                    placeHolders.put(formatPlaceHolder(placeHolder), resolved);
                }
            }
        }

        return placeHolders;
    }

    private static String formatPlaceHolder(String placeHolder) {
        return placeHolder.replaceAll("\\{", "").replace("}", "").replace("[", "").replace("]", "").replace("[", "").replace("]", "");
    }
}
