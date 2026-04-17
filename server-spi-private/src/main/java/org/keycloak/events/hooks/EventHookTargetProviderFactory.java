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

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.util.JsonSerialization;

public interface EventHookTargetProviderFactory extends ProviderFactory<EventHookTargetProvider> {

    String REDACTED_SECRET_VALUE = "********";

    List<ProviderConfigProperty> getConfigMetadata();

    void validateConfig(KeycloakSession session, Map<String, Object> settings);

    default boolean supportsBatch() {
        return false;
    }

    default boolean supportsPush() {
        return true;
    }

    default boolean supportsPull() {
        return false;
    }

    default boolean supportsRetry() {
        return true;
    }

    default boolean supportsAggregation() {
        return false;
    }

    default Object getTargetEndpointResource(KeycloakSession session, RealmModel realm, EventHookTargetModel target, String endpointName) {
        return null;
    }

    default String getDisplayInfo(EventHookTargetModel target) {
        return null;
    }

    default Map<String, Object> normalizeConfig(Map<String, Object> existingSettings, Map<String, Object> submittedSettings) {
        if ((existingSettings == null || existingSettings.isEmpty()) && (submittedSettings == null || submittedSettings.isEmpty())) {
            return Collections.emptyMap();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        if (submittedSettings != null) {
            normalized.putAll(submittedSettings);
        }

        secretProperties().forEach(secretProperty -> {
            Object submittedValue = normalized.get(secretProperty);
            if (REDACTED_SECRET_VALUE.equals(submittedValue)) {
                if (existingSettings != null && existingSettings.containsKey(secretProperty)) {
                    normalized.put(secretProperty, existingSettings.get(secretProperty));
                } else {
                    normalized.remove(secretProperty);
                }
            } else if (!normalized.containsKey(secretProperty)
                    && existingSettings != null
                    && existingSettings.containsKey(secretProperty)) {
                normalized.put(secretProperty, existingSettings.get(secretProperty));
            }
        });

        return normalized.isEmpty() ? Collections.emptyMap() : normalized;
    }

    default Map<String, Object> redactConfig(Map<String, Object> settings) {
        if (settings == null || settings.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> redacted = new LinkedHashMap<>(settings);
        secretProperties().forEach(secretProperty -> {
            if (redacted.containsKey(secretProperty) && redacted.get(secretProperty) != null) {
                redacted.put(secretProperty, REDACTED_SECRET_VALUE);
            }
        });
        return redacted;
    }

    default Set<String> secretProperties() {
        return getConfigMetadata().stream()
                .filter(property -> property.isSecret() || ProviderConfigProperty.PASSWORD.equals(property.getType()))
                .map(ProviderConfigProperty::getName)
                .collect(Collectors.toSet());
    }

    default EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target) {
        long started = System.currentTimeMillis();
        EventHookTargetProvider provider = create(session);

        try {
            List<EventHookMessageModel> messages = createTestMessages(session, realm, target);
            EventHookDeliveryResult result = provider.deliver(target, messages.get(0));
            if (result.getDurationMillis() <= 0) {
                result.setDurationMillis(System.currentTimeMillis() - started);
            }
            return result;
        } catch (Exception exception) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(false);
            result.setRetryable(false);
            result.setStatusCode("TEST_ERROR");
            result.setDetails(truncate(exception.getMessage(), 1024));
            result.setDurationMillis(System.currentTimeMillis() - started);
            return result;
        } finally {
            provider.close();
        }
    }

    default List<EventHookMessageModel> createTestMessages(KeycloakSession session, RealmModel realm, EventHookTargetModel target) throws IOException {
        long now = System.currentTimeMillis();
        String messageId = "test-" + UUID.randomUUID();

        EventHookMessageModel message = new EventHookMessageModel();
        message.setId(messageId);
        message.setRealmId(realm.getId());
        message.setTargetId(target.getId());
        message.setSourceType(EventHookSourceType.USER);
        message.setSourceEventId(messageId);
        message.setStatus(EventHookMessageStatus.PENDING);
        message.setNextAttemptAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        message.setPayload(JsonSerialization.writeValueAsString(testPayload(realm, target, messageId, now)));
        return List.of(message);
    }

    private Map<String, Object> testPayload(RealmModel realm, EventHookTargetModel target, String messageId, long timestamp) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deliveryTest", true);
        payload.put("eventType", "EVENT_HOOK_TEST");
        payload.put("message", "This is a test delivery generated by Keycloak event hooks.");
        payload.put("messageId", messageId);
        payload.put("time", Instant.ofEpochMilli(timestamp).toString());
        payload.put("realm", namedMap(realm.getId(), realm.getName()));
        payload.put("target", targetMap(target));
        return payload;
    }

    private Map<String, Object> targetMap(EventHookTargetModel target) {
        Map<String, Object> targetMap = new LinkedHashMap<>();
        targetMap.put("id", target.getId());
        targetMap.put("name", target.getName());
        targetMap.put("type", target.getType());
        targetMap.put("enabled", target.isEnabled());
        return targetMap;
    }

    private Map<String, Object> namedMap(String id, String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("name", name);
        return value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
