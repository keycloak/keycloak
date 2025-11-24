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

import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Factory for creating AttestationProofValidator instances with configurable trusted keys.
 * Trusted keys are configured via realm attribute 'oid4vc.attestation.trusted_keys'.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class AttestationProofValidatorFactory implements ProofValidatorFactory {

    private static final Logger logger = Logger.getLogger(AttestationProofValidatorFactory.class);

    public static final String TRUSTED_KEYS_REALM_ATTR = "oid4vc.attestation.trusted_keys";

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
     * Loads trusted keys from realm attributes.
     */
    private Map<String, JWK> loadTrustedKeysFromRealm(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            return Map.of();
        }

        String trustedKeysJson = realm.getAttribute(TRUSTED_KEYS_REALM_ATTR);
        if (trustedKeysJson == null || trustedKeysJson.trim().isEmpty()) {
            return Map.of();
        }

        try {
            return parseTrustedKeys(trustedKeysJson);
        } catch (Exception e) {
            logger.warnf(e, "Failed to parse trusted keys from realm attribute '%s'", TRUSTED_KEYS_REALM_ATTR);
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
