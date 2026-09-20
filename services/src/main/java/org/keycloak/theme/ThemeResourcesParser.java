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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThemeResourcesParser {

    private static final String STYLES = "styles";
    private static final String STYLES_COMMON = "stylesCommon";
    private static final String SCRIPTS = "scripts";
    private static final String FAVICONS = "favicons";

    private static final Pattern RESOURCE_KEY = Pattern.compile("^(styles|stylesCommon|scripts|favicons)\\.([^.]+)$");
    private static final Pattern ATTRIBUTE_KEY = Pattern.compile("^(styles|stylesCommon|scripts|favicons)\\.([^.]+)\\.(.+)$");

    private record ResourceKeyMatch(String type, String id) {}
    private record AttributeKeyMatch(String type, String id, String attribute) {}

    private static final ResourceKeyMatch RESOURCE_NO_MATCH = new ResourceKeyMatch(null, null);
    private static final AttributeKeyMatch ATTRIBUTE_NO_MATCH = new AttributeKeyMatch(null, null, null);

    /**
     * Caches regex match results keyed by property name string, so each key is matched at most once.
     * Avoids allocating a new {@link Matcher} on every call to {@link #parseType}, which otherwise
     * creates ~960 short-lived Matchers per page render (4 types × 2 patterns × ~120 properties),
     * almost all of which don't match. The number of distinct keys is bounded by the set of property
     * names across all installed themes — typically a few hundred at most — so no eviction is needed.
     */
    private static final Map<String, ResourceKeyMatch> RESOURCE_KEY_MATCHES = new ConcurrentHashMap<>();
    private static final Map<String, AttributeKeyMatch> ATTRIBUTE_KEY_MATCHES = new ConcurrentHashMap<>();

    private ThemeResourcesParser() {
    }

    private static ResourceKeyMatch matchResourceKey(String key) {
        return RESOURCE_KEY_MATCHES.computeIfAbsent(key, k -> {
            Matcher m = RESOURCE_KEY.matcher(k);
            if (!m.matches() || "order".equals(m.group(2))) {
                return RESOURCE_NO_MATCH;
            }
            return new ResourceKeyMatch(m.group(1), m.group(2));
        });
    }

    private static AttributeKeyMatch matchAttributeKey(String key) {
        return ATTRIBUTE_KEY_MATCHES.computeIfAbsent(key, k -> {
            Matcher m = ATTRIBUTE_KEY.matcher(k);
            return m.matches() ? new AttributeKeyMatch(m.group(1), m.group(2), m.group(3)) : ATTRIBUTE_NO_MATCH;
        });
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
            ResourceKeyMatch match = matchResourceKey(key);
            if (match == RESOURCE_NO_MATCH || !match.type().equals(type)) {
                continue;
            }
            String path = properties.getProperty(key);
            if (path == null || path.isBlank()) {
                continue;
            }
            builders.put(match.id(), ThemeResourceDescriptor.builder(path));
        }

        for (String key : properties.stringPropertyNames()) {
            AttributeKeyMatch match = matchAttributeKey(key);
            if (match == ATTRIBUTE_NO_MATCH || !match.type().equals(type)) {
                continue;
            }
            ThemeResourceDescriptor.Builder builder = builders.get(match.id());
            if (builder != null) {
                builder.attribute(match.attribute(), properties.getProperty(key));
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
