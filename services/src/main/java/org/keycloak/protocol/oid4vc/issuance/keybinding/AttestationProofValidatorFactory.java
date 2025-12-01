/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Factory for creating AttestationProofValidator instances with configurable trusted keys.
 * Trusted keys are loaded from two sources:
 * <ul>
 *   <li>Realm session keys (default): All enabled keys from the realm's key providers</li>
 *   <li>Realm attribute keys (override): Keys configured via realm attribute 'oid4vc.attestation.trusted_keys'</li>
 * </ul>
 * Keys from realm attributes take precedence over session keys when there are conflicts (same kid).
 * This approach allows using realm keys as a default while supporting additional keys via realm attributes,
 * which is especially useful since realm attribute fields have limited length (2-3 keys maximum).
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class AttestationProofValidatorFactory implements ProofValidatorFactory {

    private static final Logger logger = Logger.getLogger(AttestationProofValidatorFactory.class);

    @Override
    public String getId() {
        return ProofType.ATTESTATION;
    }

    @Override
    public ProofValidator create(KeycloakSession session) {
        Map<String, JWK> trustedKeys = loadTrustedKeysFromRealm(session);
        AttestationKeyResolver resolver = new StaticAttestationKeyResolver(trustedKeys);
        return new AttestationProofValidator(session, resolver);
    }

    /**
     * Loads trusted keys by merging keys from realm session (default) with keys from realm attributes (override).
     * Realm attribute keys take precedence if there are conflicts (same kid).
     * This approach allows using realm keys as a default while supporting additional keys via realm attributes.
     *
     * @param session The Keycloak session
     * @return Map of trusted keys keyed by kid, or empty map if realm is null
     */
    private Map<String, JWK> loadTrustedKeysFromRealm(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            logger.debugf("No realm available, returning empty trusted keys map");
            return Map.of();
        }

        // Load keys from session as default/fallback
        Map<String, JWK> sessionKeys = loadKeysFromSession(session, realm);

        // Load keys from realm attribute (these take precedence)
        Map<String, JWK> attributeKeys = loadKeysFromRealmAttribute(realm);

        // Merge: attribute keys override session keys for the same kid
        Map<String, JWK> mergedKeys = new HashMap<>(sessionKeys);
        mergedKeys.putAll(attributeKeys);

        if (mergedKeys.isEmpty()) {
            logger.debugf("No trusted keys found for attestation proof validation (neither from session nor realm attribute)");
        } else {
            logger.debugf("Loaded %d trusted keys for attestation proof validation (%d from session, %d from realm attribute)",
                    mergedKeys.size(), sessionKeys.size(), attributeKeys.size());
        }

        return Collections.unmodifiableMap(mergedKeys);
    }

    /**
     * Loads keys from Keycloak session by reusing JWKSServerUtils.getRealmJwks().
     * This provides a default set of trusted keys from the realm's key providers.
     * Converts the result to a Map keyed by kid for easier lookup and merging.
     */
    private Map<String, JWK> loadKeysFromSession(KeycloakSession session, RealmModel realm) {
        try {
            JSONWebKeySet keySet = JWKSServerUtils.getRealmJwks(session, realm);
            if (keySet == null || keySet.getKeys() == null) {
                return Map.of();
            }

            return Stream.of(keySet.getKeys())
                    .filter(jwk -> jwk != null && jwk.getKeyId() != null)
                    .collect(Collectors.toMap(
                            JWK::getKeyId,
                            jwk -> jwk,
                            (existing, replacement) -> existing // Keep first occurrence if duplicate kids
                    ));
        } catch (Exception e) {
            logger.warnf(e, "Failed to load keys from session for realm '%s'", realm.getName());
            return Map.of();
        }
    }

    /**
     * Loads trusted keys from realm attribute.
     * These keys take precedence over session keys when merged.
     */
    private Map<String, JWK> loadKeysFromRealmAttribute(RealmModel realm) {
        String trustedKeysJson = realm.getAttribute(OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
        if (trustedKeysJson == null || trustedKeysJson.trim().isEmpty()) {
            return Map.of();
        }

        try {
            return parseTrustedKeys(trustedKeysJson);
        } catch (Exception e) {
            logger.warnf(e, "Failed to parse trusted keys from realm attribute '%s'", OID4VCIConstants.TRUSTED_KEYS_REALM_ATTR);
            return Map.of();
        }
    }

    /**
     * Parses trusted keys from JSON string.
     * Expected format: JSON array of JWK objects, each with a 'kid' field.
     */
    private Map<String, JWK> parseTrustedKeys(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Map.of();
        }

        try {
            List<JWK> keys = JsonSerialization.mapper.readValue(json, new TypeReference<List<JWK>>() {
            });
            if (keys == null || keys.isEmpty()) {
                return Map.of();
            }

            Map<String, JWK> keyMap = new HashMap<>();
            for (JWK key : keys) {
                String kid = key.getKeyId();
                if (kid == null || kid.trim().isEmpty()) {
                    logger.warnf("Skipping JWK without 'kid' field in trusted keys configuration");
                    continue;
                }
                keyMap.put(kid, key);
            }

            logger.debugf("Loaded %d trusted keys for attestation proof validation", keyMap.size());
            return Collections.unmodifiableMap(keyMap);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format for trusted keys: " + e.getMessage(), e);
        }
    }
}
