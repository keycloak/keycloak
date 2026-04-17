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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

public final class EventHookTargetEventFilter {

    public static final String SETTINGS_KEY = "events";

    private static final String WILDCARD = "*";
    private static final String USER_WILDCARD = "USER:*";
    private static final String ADMIN_WILDCARD = "ADMIN:*";
    private static final String USER_PREFIX = "USER:";
    private static final String ADMIN_PREFIX = "ADMIN:";
    private static final String STRINGIFIED_DELIMITER = "##";

    private EventHookTargetEventFilter() {
    }

    public static boolean matchesUserEvent(EventHookTargetModel target, Event event) {
        return matches(normalizeFilters(target == null ? null : target.getSettings().get(SETTINGS_KEY)), USER_PREFIX,
                event == null || event.getType() == null ? null : event.getType().name());
    }

    public static boolean matchesAdminEvent(EventHookTargetModel target, AdminEvent event) {
        return matches(normalizeFilters(target == null ? null : target.getSettings().get(SETTINGS_KEY)), ADMIN_PREFIX,
                event == null || event.getOperationType() == null ? null : event.getOperationType().name());
    }

    public static void validateSettings(Object configuredFilters) {
        normalizeFilters(configuredFilters);
    }

    private static boolean matches(List<String> filters, String prefix, String eventName) {
        if (filters.contains(WILDCARD)) {
            return true;
        }

        if (USER_PREFIX.equals(prefix) && filters.contains(USER_WILDCARD)) {
            return true;
        }

        if (ADMIN_PREFIX.equals(prefix) && filters.contains(ADMIN_WILDCARD)) {
            return true;
        }

        if (eventName == null) {
            return false;
        }

        return filters.contains(eventName) || filters.contains(prefix + eventName);
    }

    private static List<String> normalizeFilters(Object configuredFilters) {
        List<String> filters = rawValues(configuredFilters).stream()
                .map(value -> value == null ? null : value.toString().trim().toUpperCase(Locale.ROOT))
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (filters.isEmpty()) {
            return List.of(WILDCARD);
        }

        filters.forEach(EventHookTargetEventFilter::validateFilter);
        return filters;
    }

    private static List<Object> rawValues(Object configuredFilters) {
        if (configuredFilters == null) {
            return List.of();
        }

        if (configuredFilters instanceof String stringValue) {
            return Arrays.stream(stringValue.split(STRINGIFIED_DELIMITER)).map(value -> (Object) value).toList();
        }

        if (configuredFilters instanceof Collection<?> collectionValue) {
            return collectionValue.stream().map(value -> (Object) value).toList();
        }

        if (configuredFilters.getClass().isArray()) {
            int length = Array.getLength(configuredFilters);
            return java.util.stream.IntStream.range(0, length)
                    .mapToObj(index -> Array.get(configuredFilters, index))
                    .toList();
        }

        return List.of(configuredFilters);
    }

    private static void validateFilter(String filter) {
        if (WILDCARD.equals(filter) || USER_WILDCARD.equals(filter) || ADMIN_WILDCARD.equals(filter)) {
            return;
        }

        if (filter.startsWith(USER_PREFIX)) {
            validateUserEvent(filter.substring(USER_PREFIX.length()), filter);
            return;
        }

        if (filter.startsWith(ADMIN_PREFIX)) {
            validateAdminEvent(filter.substring(ADMIN_PREFIX.length()), filter);
            return;
        }

        if (isUserEvent(filter) || isAdminEvent(filter)) {
            return;
        }

        throw new IllegalArgumentException("Unsupported event filter: " + filter);
    }

    private static void validateUserEvent(String eventName, String originalFilter) {
        if (!isUserEvent(eventName)) {
            throw new IllegalArgumentException("Unsupported event filter: " + originalFilter);
        }
    }

    private static void validateAdminEvent(String operationName, String originalFilter) {
        if (!isAdminEvent(operationName)) {
            throw new IllegalArgumentException("Unsupported event filter: " + originalFilter);
        }
    }

    private static boolean isUserEvent(String eventName) {
        try {
            EventType.valueOf(eventName);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isAdminEvent(String operationName) {
        try {
            OperationType.valueOf(operationName);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
