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

package org.keycloak.theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThemeResourcesParser {

    private static final String STYLES = "styles";
    private static final String STYLES_COMMON = "stylesCommon";
    private static final String SCRIPTS = "scripts";
    private static final String FAVICONS = "favicons";

    private static final Pattern RESOURCE_KEY = Pattern.compile("^(styles|stylesCommon|scripts|favicons)\\.([^.]+)$");
    private static final Pattern ATTRIBUTE_KEY = Pattern.compile("^(styles|stylesCommon|scripts|favicons)\\.([^.]+)\\.(.+)$");

    private ThemeResourcesParser() {
    }

    public static ThemeResources parse(Properties properties) {
        if (properties == null) {
            return ThemeResources.empty();
        }

        List<ThemeResourceDescriptor> favicons = parseType(properties, FAVICONS, true);
        if (favicons.isEmpty()) {
            favicons = parseLegacyFavicon(properties);
        }

        return new ThemeResources(
                parseType(properties, STYLES, false),
                parseType(properties, STYLES_COMMON, false),
                parseType(properties, SCRIPTS, false),
                favicons
        );
    }

    private static List<ThemeResourceDescriptor> parseType(Properties properties, String type, boolean favicon) {
        List<ThemeResourceDescriptor> result = new ArrayList<>();

        String flat = properties.getProperty(type);
        if (flat != null && !flat.isBlank()) {
            for (String path : flat.trim().split("\\s+")) {
                if (!path.isEmpty()) {
                    result.add(buildDescriptor(path, favicon));
                }
            }
        }

        Map<String, ThemeResourceDescriptor.Builder> builders = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            Matcher matcher = RESOURCE_KEY.matcher(key);
            if (!matcher.matches() || !matcher.group(1).equals(type)) {
                continue;
            }
            String id = matcher.group(2);
            if ("order".equals(id)) {
                continue;
            }
            String path = properties.getProperty(key);
            if (path == null || path.isBlank()) {
                continue;
            }
            builders.put(id, ThemeResourceDescriptor.builder(path));
        }

        for (String key : properties.stringPropertyNames()) {
            Matcher matcher = ATTRIBUTE_KEY.matcher(key);
            if (!matcher.matches() || !matcher.group(1).equals(type)) {
                continue;
            }
            ThemeResourceDescriptor.Builder builder = builders.get(matcher.group(2));
            if (builder != null) {
                builder.attribute(matcher.group(3), properties.getProperty(key));
            }
        }

        List<String> ids = new ArrayList<>(builders.keySet());
        sortIds(ids, properties.getProperty(type + ".order"));

        for (String id : ids) {
            ThemeResourceDescriptor.Builder builder = builders.get(id);
            if (builder != null) {
                result.add(favicon ? builder.buildFavicon() : builder.build());
            }
        }

        return result;
    }

    private static ThemeResourceDescriptor buildDescriptor(String path, boolean favicon) {
        ThemeResourceDescriptor.Builder builder = ThemeResourceDescriptor.builder(path);
        return favicon ? builder.buildFavicon() : builder.build();
    }

    private static List<ThemeResourceDescriptor> parseLegacyFavicon(Properties properties) {
        String favIcon = properties.getProperty("favIcon");
        if (favIcon == null || favIcon.isBlank()) {
            return List.of();
        }

        ThemeResourceDescriptor.Builder builder = ThemeResourceDescriptor.builder(favIcon);
        String favIconType = properties.getProperty("favIconType");
        if (favIconType != null && !favIconType.isBlank()) {
            builder.type(favIconType);
        }
        return List.of(builder.buildFavicon());
    }

    private static void sortIds(List<String> ids, String orderProperty) {
        if (orderProperty != null && !orderProperty.isBlank()) {
            List<String> order = new ArrayList<>();
            for (String id : orderProperty.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) {
                    order.add(trimmed);
                }
            }
            ids.sort(Comparator
                    .comparingInt((String id) -> {
                        int index = order.indexOf(id);
                        return index >= 0 ? index : Integer.MAX_VALUE;
                    })
                    .thenComparing(ThemeResourcesParser::compareNaturalOrder));
            return;
        }
        ids.sort(ThemeResourcesParser::compareNaturalOrder);
    }

    private static int compareNaturalOrder(String left, String right) {
        try {
            int leftNumber = Integer.parseInt(left);
            int rightNumber = Integer.parseInt(right);
            return Integer.compare(leftNumber, rightNumber);
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }
}
