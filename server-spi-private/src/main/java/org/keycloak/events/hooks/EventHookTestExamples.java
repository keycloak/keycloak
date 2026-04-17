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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.RealmModel;

public final class EventHookTestExamples {

    private static final String DEFAULT_USER_CLIENT = "security-admin-console";
    private static final String DEFAULT_USER_ID = "test-user";
    private static final String DEFAULT_SESSION_ID = "test-session";
    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";

    private EventHookTestExamples() {
    }

    public static List<EventHookTestExample> all(RealmModel realm, EventHookTargetModel target) {
        List<EventHookTestExample> userExamples = java.util.Arrays.stream(EventType.values())
                .map(eventType -> userExample(realm, target, eventType))
                .toList();
        List<EventHookTestExample> adminExamples = java.util.Arrays.stream(OperationType.values())
                .map(operationType -> adminExample(realm, target, operationType))
                .toList();

        return java.util.stream.Stream.concat(userExamples.stream(), adminExamples.stream()).toList();
    }

    public static EventHookTestExample defaultExample(RealmModel realm, EventHookTargetModel target) {
        return userExample(realm, target, EventType.LOGIN);
    }

    public static EventHookTestExample resolve(RealmModel realm, EventHookTargetModel target, String exampleId) {
        if (exampleId == null || exampleId.isBlank()) {
            return defaultExample(realm, target);
        }

        return all(realm, target).stream()
                .filter(example -> example.getId().equalsIgnoreCase(exampleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown event hook test example: " + exampleId));
    }

    private static EventHookTestExample userExample(RealmModel realm, EventHookTargetModel target, EventType eventType) {
        Event event = new Event();
        event.setId("test-user-" + eventType.name().toLowerCase(java.util.Locale.ROOT) + "-" + UUID.randomUUID());
        event.setTime(System.currentTimeMillis());
        event.setType(eventType);
        event.setRealmId(realmId(realm, target));
        event.setRealmName(realmName(realm));
        event.setClientId(DEFAULT_USER_CLIENT);
        event.setUserId(DEFAULT_USER_ID);
        event.setSessionId(DEFAULT_SESSION_ID);
        event.setIpAddress(DEFAULT_IP_ADDRESS);
        event.setDetails(userDetails(eventType));

        Map<String, Object> payload = markAsTestPayload(EventHookExamplePayloads.buildUserEventPayload(event), id(EventHookSourceType.USER, eventType.name()));
        return new EventHookTestExample(id(EventHookSourceType.USER, eventType.name()), EventHookSourceType.USER, eventType.name(), payload);
    }

    private static EventHookTestExample adminExample(RealmModel realm, EventHookTargetModel target, OperationType operationType) {
        AdminEvent event = new AdminEvent();
        event.setId("test-admin-" + operationType.name().toLowerCase(java.util.Locale.ROOT) + "-" + UUID.randomUUID());
        event.setTime(System.currentTimeMillis());
        event.setRealmId(realmId(realm, target));
        event.setRealmName(realmName(realm));
        event.setOperationType(operationType);
        event.setResourceType(ResourceType.USER);
        event.setResourcePath("users/" + DEFAULT_USER_ID);
        event.setRepresentation("{\"id\":\"" + DEFAULT_USER_ID + "\",\"username\":\"test-user\"}");
        event.setDetails(Map.of("origin", "event-hook-test"));

        AuthDetails authDetails = new AuthDetails();
        authDetails.setRealmId(realmId(realm, target));
        authDetails.setRealmName(realmName(realm));
        authDetails.setClientId(DEFAULT_USER_CLIENT);
        authDetails.setUserId("admin-user");
        authDetails.setIpAddress(DEFAULT_IP_ADDRESS);
        event.setAuthDetails(authDetails);

        Map<String, Object> payload = markAsTestPayload(EventHookExamplePayloads.buildAdminEventPayload(event, true), id(EventHookSourceType.ADMIN, operationType.name()));
        return new EventHookTestExample(id(EventHookSourceType.ADMIN, operationType.name()), EventHookSourceType.ADMIN, operationType.name(), payload);
    }

    private static Map<String, String> userDetails(EventType eventType) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("username", "test-user");
        details.put("redirect_uri", "https://example.org/callback");
        details.put("scope", "openid profile");
        details.put("testEvent", eventType.name());
        return details;
    }

    private static Map<String, Object> markAsTestPayload(Map<String, Object> payload, String exampleId) {
        Map<String, Object> markedPayload = new LinkedHashMap<>(payload);
        markedPayload.put("deliveryTest", true);
        markedPayload.put("testExampleId", exampleId);
        return markedPayload;
    }

    private static String id(EventHookSourceType sourceType, String eventName) {
        return sourceType.name() + ":" + eventName;
    }

    private static String realmId(RealmModel realm, EventHookTargetModel target) {
        if (realm != null && realm.getId() != null) {
            return realm.getId();
        }
        return target == null ? null : target.getRealmId();
    }

    private static String realmName(RealmModel realm) {
        if (realm == null || realm.getName() == null || realm.getName().isBlank()) {
            return "test-realm";
        }
        return realm.getName();
    }
}
