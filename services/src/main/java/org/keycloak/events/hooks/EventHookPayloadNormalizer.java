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
import java.util.Map;

import org.keycloak.util.JsonSerialization;

public final class EventHookPayloadNormalizer {

    private EventHookPayloadNormalizer() {
    }

    public static Object readPayload(String payload) throws IOException {
        return normalizeRepresentation(JsonSerialization.readValue(payload, Object.class));
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeRepresentation(Object payload) {
        if (!(payload instanceof Map<?, ?> mapPayload)) {
            return payload;
        }

        Object representation = mapPayload.get("representation");
        if (!(representation instanceof String representationValue) || representationValue.isBlank()) {
            return payload;
        }

        try {
            ((Map<String, Object>) mapPayload).put("representation", JsonSerialization.readValue(representationValue, Object.class));
        } catch (IOException ignored) {
        }

        return payload;
    }
}
