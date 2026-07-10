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

package org.keycloak.singleobject.jpa;

import java.io.IOException;
import java.util.Map;

import org.keycloak.models.ModelException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Helper for serializing and deserializing {@link SingleUseObjectEntity} notes field
 * to and from its JSON text column representation.
 */
final class SingleUseObjectSerialization {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    public static Map<String, String> getNotes(SingleUseObjectEntity entity) {
        return read(entity.getNotes(), entity.getId(), "notes");
    }

    public static Map<String, String> parseNotes(String id, String json) {
        return read(json, id, "notes");
    }

    public static String notesToString(String id, Map<String, String> notes) {
        return write(notes, id, "notes");
    }

    private static Map<String, String> read(String json, String id, String field) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JsonSerialization.readValue(json, STRING_MAP);
        } catch (IOException e) {
            throw new ModelException("Error reading single-use object field: id='%s', field='%s'".formatted(id, field), e);
        }
    }

    private static String write(Object value, String id, String field) {
        try {
            return JsonSerialization.writeValueAsString(value);
        } catch (IOException e) {
            throw new ModelException("Error writing single-use object field: id='%s', field='%s'".formatted(id, field), e);
        }
    }
}
