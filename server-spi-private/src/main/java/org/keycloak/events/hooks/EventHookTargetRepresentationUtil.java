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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.EventHookTargetRepresentation;

public final class EventHookTargetRepresentationUtil {

    private EventHookTargetRepresentationUtil() {
    }

    public static EventHookTargetRepresentation toRepresentation(KeycloakSession session, EventHookTargetModel model, boolean redactSecrets) {
        EventHookTargetProviderFactory providerFactory = findProviderFactory(session, model.getType());
        if (model.getRealmName() == null && model.getRealmId() != null) {
            RealmModel realm = session.realms().getRealm(model.getRealmId());
            model.setRealmName(realm == null ? null : realm.getName());
        }
        EventHookTargetRepresentation representation = new EventHookTargetRepresentation();
        representation.setId(model.getId());
        representation.setName(model.getName());
        representation.setType(model.getType());
        representation.setEnabled(model.isEnabled());
        representation.setCreatedAt(model.getCreatedAt());
        representation.setUpdatedAt(model.getUpdatedAt());
        representation.setAutoDisabled(model.getAutoDisabledUntil() != null
            && !model.isEnabled()
            && model.getAutoDisabledUntil().longValue() > System.currentTimeMillis());
        representation.setAutoDisabledUntil(model.getAutoDisabledUntil());
        representation.setAutoDisabledReason(model.getAutoDisabledReason());
        representation.setSettings(providerFactory == null
                ? safeSettings(model.getSettings())
                : redactSecrets ? providerFactory.redactConfig(model.getSettings()) : safeSettings(model.getSettings()));
        representation.setDisplayInfo(providerFactory == null ? null : providerFactory.getDisplayInfo(model));
        return representation;
    }

    public static EventHookTargetRepresentation redactRepresentation(KeycloakSession session, EventHookTargetRepresentation representation) {
        EventHookTargetProviderFactory providerFactory = findProviderFactory(session, representation.getType());
        if (providerFactory != null) {
            representation.setSettings(providerFactory.redactConfig(representation.getSettings()));
        }
        return representation;
    }

    public static EventHookTargetModel toModel(KeycloakSession session, RealmModel realm, EventHookTargetRepresentation representation,
            EventHookTargetModel existing, long now, boolean preserveRepresentationMetadata) {
        return toModel(session, realm, representation, existing, now, preserveRepresentationMetadata, false);
    }

    public static EventHookTargetModel toModel(KeycloakSession session, RealmModel realm, EventHookTargetRepresentation representation,
            EventHookTargetModel existing, long now, boolean preserveRepresentationMetadata, boolean allowUnknownType) {
        validateRepresentation(representation);

        EventHookTargetProviderFactory providerFactory = findProviderFactory(session, representation.getType());
        if (providerFactory == null && !allowUnknownType) {
            throw new IllegalArgumentException("Unknown event hook target type: " + representation.getType());
        }

        Map<String, Object> normalizedSettings;
        if (providerFactory == null) {
            normalizedSettings = safeSettings(representation.getSettings());
        } else {
            Map<String, Object> existingSettings = existing == null ? Collections.emptyMap() : safeSettings(existing.getSettings());
            normalizedSettings = providerFactory.normalizeConfig(existingSettings, safeSettings(representation.getSettings()));
            EventHookTargetConfigValidator.validate(session, providerFactory, normalizedSettings);
        }

        EventHookTargetModel target = existing == null ? new EventHookTargetModel() : existing;
        target.setId(resolveId(representation, existing, preserveRepresentationMetadata));
        target.setRealmId(realm.getId());
        target.setRealmName(realm.getName());
        target.setName(representation.getName().trim());
        target.setType(representation.getType());
        target.setEnabled(Boolean.TRUE.equals(representation.getEnabled()));
        target.setCreatedAt(resolveCreatedAt(representation, existing, now, preserveRepresentationMetadata));
        target.setUpdatedAt(resolveUpdatedAt(representation, now, preserveRepresentationMetadata));
        if (target.isEnabled()) {
            target.setAutoDisabledUntil(null);
            target.setAutoDisabledReason(null);
            target.setConsecutive429Count(null);
        }
        target.setSettings(normalizedSettings);
        return target;
    }

    private static void validateRepresentation(EventHookTargetRepresentation representation) {
        if (representation == null) {
            throw new IllegalArgumentException("Target representation is required");
        }
        if (representation.getName() == null || representation.getName().isBlank()) {
            throw new IllegalArgumentException("Target name is required");
        }
        if (representation.getType() == null || representation.getType().isBlank()) {
            throw new IllegalArgumentException("Target type is required");
        }
    }

    private static EventHookTargetProviderFactory findProviderFactory(KeycloakSession session, String type) {
        return (EventHookTargetProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(EventHookTargetProvider.class, type);
    }

    private static Map<String, Object> safeSettings(Map<String, Object> settings) {
        return settings == null ? Collections.emptyMap() : settings;
    }

    private static String resolveId(EventHookTargetRepresentation representation, EventHookTargetModel existing,
            boolean preserveRepresentationMetadata) {
        if (existing != null) {
            return existing.getId();
        }
        if (preserveRepresentationMetadata && representation.getId() != null && !representation.getId().isBlank()) {
            return representation.getId();
        }
        return UUID.randomUUID().toString();
    }

    private static long resolveCreatedAt(EventHookTargetRepresentation representation, EventHookTargetModel existing, long now,
            boolean preserveRepresentationMetadata) {
        if (existing != null) {
            return existing.getCreatedAt();
        }
        if (preserveRepresentationMetadata && representation.getCreatedAt() != null) {
            return representation.getCreatedAt();
        }
        return now;
    }

    private static long resolveUpdatedAt(EventHookTargetRepresentation representation, long now,
            boolean preserveRepresentationMetadata) {
        if (preserveRepresentationMetadata && representation.getUpdatedAt() != null) {
            return representation.getUpdatedAt();
        }
        return now;
    }
}
