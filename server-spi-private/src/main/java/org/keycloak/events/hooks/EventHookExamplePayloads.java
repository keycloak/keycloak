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
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;

public final class EventHookExamplePayloads {

    private EventHookExamplePayloads() {
    }

    public static Map<String, Object> buildUserEventPayload(Event event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", EventHookSourceType.USER.name());
        payload.put("eventId", event.getId());
        payload.put("realmId", event.getRealmId());
        payload.put("time", event.getTime());
        payload.put("eventType", event.getType() == null ? null : event.getType().name());
        payload.put("clientId", event.getClientId());
        payload.put("userId", event.getUserId());
        payload.put("sessionId", event.getSessionId());
        payload.put("ipAddress", event.getIpAddress());
        payload.put("error", event.getError());
        payload.put("details", event.getDetails());
        return payload;
    }

    public static Map<String, Object> buildAdminEventPayload(AdminEvent event, boolean includeRepresentation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceType", EventHookSourceType.ADMIN.name());
        payload.put("eventId", event.getId());
        payload.put("realmId", event.getRealmId());
        payload.put("time", event.getTime());
        payload.put("operationType", event.getOperationType() == null ? null : event.getOperationType().name());
        payload.put("resourceType", event.getResourceTypeAsString());
        payload.put("resourcePath", event.getResourcePath());
        payload.put("error", event.getError());
        payload.put("details", event.getDetails());
        payload.put("auth", buildAuthPayload(event.getAuthDetails()));
        if (includeRepresentation) {
            payload.put("representation", event.getRepresentation());
        }
        return payload;
    }

    private static Map<String, Object> buildAuthPayload(AuthDetails authDetails) {
        if (authDetails == null) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("realmId", authDetails.getRealmId());
        payload.put("clientId", authDetails.getClientId());
        payload.put("userId", authDetails.getUserId());
        payload.put("ipAddress", authDetails.getIpAddress());
        return payload;
    }
}
