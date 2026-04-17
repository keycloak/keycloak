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
        return test(session, realm, target, (String) null);
    }

    default EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target, String exampleId) {
        List<EventHookMessageModel> messages;
        try {
            messages = createTestMessages(session, realm, target, exampleId);
        } catch (IOException exception) {
            EventHookDeliveryResult result = new EventHookDeliveryResult();
            result.setSuccess(false);
            result.setRetryable(false);
            result.setStatusCode("TEST_ERROR");
            result.setDetails(truncate(exception.getMessage(), 1024));
            result.setDurationMillis(0);
            return result;
        }

        return test(session, realm, target, messages);
    }

    default EventHookDeliveryResult test(KeycloakSession session, RealmModel realm, EventHookTargetModel target, List<EventHookMessageModel> messages) {
        long started = System.currentTimeMillis();
        EventHookTargetProvider provider = create(session);

        try {
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
        return createTestMessages(session, realm, target, null);
    }

    default List<EventHookMessageModel> createTestMessages(KeycloakSession session, RealmModel realm, EventHookTargetModel target, String exampleId) throws IOException {
        long now = System.currentTimeMillis();
        EventHookTestExample example = EventHookTestExamples.resolve(realm, target, exampleId);
        String messageId = UUID.randomUUID().toString();

        EventHookMessageModel message = new EventHookMessageModel();
        message.setId(messageId);
        message.setRealmId(realm != null && realm.getId() != null ? realm.getId() : target.getRealmId());
        message.setTargetId(target.getId());
        message.setSourceType(example.getSourceType());
        Object sourceEventId = example.getPayload().get("eventId");
        message.setSourceEventId(sourceEventId == null ? messageId : sourceEventId.toString());
        message.setStatus(EventHookMessageStatus.PENDING);
        message.setTest(true);
        message.setNextAttemptAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        message.setPayload(JsonSerialization.writeValueAsString(example.getPayload()));
        return List.of(message);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
