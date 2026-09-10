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

package org.keycloak.authentication.jpa;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ModelException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.keycloak.sessions.CommonClientSessionModel.ExecutionStatus;

/**
 * Helper for serializing and deserializing {@link AuthenticationSessionEntity} collection fields (notes, execution
 * status, required actions, client scopes) to and from their JSON text column representation.
 */
final class AuthenticationSessionSerialization {

    private static final TypeReference<Set<String>> STRING_SET = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ExecutionStatus>> EXECUTION_STATUS_MAP = new TypeReference<>() {
    };


    public static Set<String> getClientScopes(AuthenticationSessionEntity entity) {
        return read(entity.getClientScopes(), STRING_SET, entity.getTabId(), "clientScopes");
    }

    public static Set<String> getRequiredActions(AuthenticationSessionEntity entity) {
        return read(entity.getRequiredActions(), STRING_SET, entity.getTabId(), "requiredActions");
    }

    public static Map<String, ExecutionStatus> getExecutionStatus(AuthenticationSessionEntity entity) {
        return read(entity.getExecutionStatus(), EXECUTION_STATUS_MAP, entity.getTabId(), "executionStatus");
    }

    public static Map<String, String> getClientNotes(AuthenticationSessionEntity entity) {
        return read(entity.getClientNotes(), STRING_MAP, entity.getTabId(), "clientNotes");
    }

    public static Map<String, String> getAuthNotes(AuthenticationSessionEntity entity) {
        return read(entity.getAuthNotes(), STRING_MAP, entity.getTabId(), "authNotes");
    }

    public static Map<String, String> getUserSessionNotes(AuthenticationSessionEntity entity) {
        return read(entity.getUserSessionNotes(), STRING_MAP, entity.getTabId(), "userSessionNotes");
    }

    public static void setClientScopes(AuthenticationSessionEntity entity, Set<String> values) {
        entity.setClientScopes(write(values, entity.getTabId(), "clientScopes"));
    }

    public static void setRequiredActions(AuthenticationSessionEntity entity, Set<String> values) {
        entity.setRequiredActions(write(values, entity.getTabId(), "requiredActions"));
    }

    public static void setExecutionStatus(AuthenticationSessionEntity entity, Map<String, ExecutionStatus> notes) {
        entity.setExecutionStatus(write(notes, entity.getTabId(), "executionStatus"));
    }

    public static void setClientNotes(AuthenticationSessionEntity entity, Map<String, String> notes) {
        entity.setClientNotes(write(notes, entity.getTabId(), "clientNotes"));
    }

    public static void setAuthNotes(AuthenticationSessionEntity entity, Map<String, String> notes) {
        entity.setAuthNotes(write(notes, entity.getTabId(), "authNotes"));
    }

    public static void setUserSessionNotes(AuthenticationSessionEntity entity, Map<String, String> notes) {
        entity.setUserSessionNotes(write(notes, entity.getTabId(), "userSessionNotes"));
    }

    private static <T> T read(String json, TypeReference<T> typeReference, String id, String field) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JsonSerialization.readValue(json, typeReference);
        } catch (IOException e) {
            throw new ModelException("Error reading authentication session field. id='%s' field='%s'".formatted(id, field), e);
        }
    }

    private static String write(Object value, String id, String field) {
        try {
            return JsonSerialization.writeValueAsString(value);
        } catch (IOException e) {
            throw new ModelException("Error writing authentication session field. id='%s' field='%s'".formatted(id, field), e);
        }
    }
}
