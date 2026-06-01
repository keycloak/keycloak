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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.models.KeycloakSession;

public final class EventHookTargetConfigValidator {

    private EventHookTargetConfigValidator() {
    }

    public static void validate(KeycloakSession session, EventHookTargetProviderFactory providerFactory, Map<String, Object> settings) {
        EventHookTargetEventFilter.validateSettings(settings == null ? null : settings.get(EventHookTargetEventFilter.SETTINGS_KEY));
        validateDeliverySettings(providerFactory, settings);
        providerFactory.validateConfig(session, settings);
    }

    private static void validateDeliverySettings(EventHookTargetProviderFactory providerFactory, Map<String, Object> settings) {
        String deliveryMode = stringSetting(settings, "deliveryMode", "SINGLE").toUpperCase(Locale.ROOT);
        if (!List.of("SINGLE", "BULK").contains(deliveryMode)) {
            throw new IllegalArgumentException("Unsupported delivery mode: " + deliveryMode);
        }
        if ("BULK".equals(deliveryMode) && !providerFactory.supportsBatch()) {
            throw new IllegalArgumentException("Target provider does not support bulk delivery: " + providerFactory.getId());
        }

        Integer maxEventsPerBatch = integerSetting(settings, "maxEventsPerBatch");
        if (maxEventsPerBatch != null && maxEventsPerBatch <= 0) {
            throw new IllegalArgumentException("Setting must be greater than zero: maxEventsPerBatch");
        }

        Integer aggregationTimeoutMs = integerSetting(settings, "aggregationTimeoutMs");
        if (!providerFactory.supportsAggregation()) {
            if (settings != null && settings.containsKey("aggregationTimeoutMs")) {
                throw new IllegalArgumentException("Aggregation settings are not supported for target provider: " + providerFactory.getId());
            }
        } else if (aggregationTimeoutMs != null) {
            if (!"BULK".equals(deliveryMode)) {
                throw new IllegalArgumentException("Aggregation timeout requires BULK delivery mode");
            }
            if (aggregationTimeoutMs < 0) {
                throw new IllegalArgumentException("Setting must not be negative: aggregationTimeoutMs");
            }
        }

        boolean retryEnabled = booleanSetting(settings, "retryEnabled", true);
        if (!providerFactory.supportsRetry()) {
            if (settings != null && settings.containsKey("retryEnabled")) {
                throw new IllegalArgumentException("Target provider does not support retries: " + providerFactory.getId());
            }
            if (settings != null && (settings.containsKey("maxAttempts") || settings.containsKey("retryDelayMs"))) {
                throw new IllegalArgumentException("Retry settings are not supported for target provider: " + providerFactory.getId());
            }
            return;
        }

        Integer maxAttempts = integerSetting(settings, "maxAttempts");
        if (retryEnabled && maxAttempts != null && maxAttempts <= 0) {
            throw new IllegalArgumentException("Setting must be greater than zero: maxAttempts");
        }

        Integer retryDelayMs = integerSetting(settings, "retryDelayMs");
        if (retryEnabled && retryDelayMs != null && retryDelayMs < 0) {
            throw new IllegalArgumentException("Setting must not be negative: retryDelayMs");
        }
    }

    private static String stringSetting(Map<String, Object> settings, String key, String defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static Integer integerSetting(Map<String, Object> settings, String key) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private static boolean booleanSetting(Map<String, Object> settings, String key, boolean defaultValue) {
        Object value = settings == null ? null : settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
