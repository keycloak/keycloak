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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Factory for creating AttestationProofValidator instances with configurable trusted keys.
 * Trusted keys are loaded from multiple sources with the following priority (highest to lowest):
 * <ol>
 *   <li>Keys by ID from realm attribute 'oid4vc.attestation.trusted_key_ids': Keys referenced by their keyId
 *       from the realm's key providers (can include disabled keys, not exposed in well-known endpoints)</li>
 *   <li>Keys from realm attribute 'oid4vc.attestation.trusted_keys': Explicit JWK JSON array</li>
 *   <li>Realm session keys (default): All enabled keys from the realm's key providers (exposed in well-known endpoints)</li>
 * </ol>
 * Keys from higher priority sources take precedence when there are conflicts (same kid).
 * This approach allows using realm keys as a default while supporting additional keys via realm attributes,
 * including disabled keys that are not exposed in well-known endpoints.
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
     * Loads trusted keys by merging keys from multiple sources with priority:
     * 1. Keys by ID from realm attribute (highest priority, can include disabled keys)
     * 2. Keys from realm attribute JSON (explicit JWK)
     * 3. Enabled keys from session (lowest priority, exposed in well-known endpoints)
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

        // Load keys from session as default/fallback (lowest priority)
        Map<String, JWK> sessionKeys = loadKeysFromSession(session, realm);

        // Load keys from realm attribute JSON (medium priority)
        Map<String, JWK> attributeKeys = loadKeysFromRealmAttribute(realm);

        // Load keys by ID from realm attribute (highest priority, can include disabled keys)
        Map<String, JWK> keyIdsKeys = loadKeysByKeyIds(session, realm);

        // Merge with priority: keyIdsKeys > attributeKeys > sessionKeys
        Map<String, JWK> mergedKeys = new HashMap<>(sessionKeys);
        mergedKeys.putAll(attributeKeys);
        mergedKeys.putAll(keyIdsKeys);

        if (mergedKeys.isEmpty()) {
            logger.debugf("No trusted keys found for attestation proof validation");
        } else {
            logger.debugf("Loaded %d trusted keys for attestation proof validation (%d from session, %d from realm attribute JSON, %d from realm attribute key IDs)",
                    mergedKeys.size(), sessionKeys.size(), attributeKeys.size(), keyIdsKeys.size());
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
     * Loads trusted keys by key IDs from realm attribute.
     * Keys are looked up from realm's key providers by their keyId, regardless of enabled status.
     * This allows using disabled keys that are not exposed in well-known endpoints.
     *
     * @param session The Keycloak session
     * @param realm   The realm
     * @return Map of trusted keys keyed by kid, or empty map if no key IDs are configured
     */
    private Map<String, JWK> loadKeysByKeyIds(KeycloakSession session, RealmModel realm) {
        String trustedKeyIds = realm.getAttribute(OID4VCIConstants.TRUSTED_KEY_IDS_REALM_ATTR);
        if (trustedKeyIds == null || trustedKeyIds.trim().isEmpty()) {
            return Map.of();
        }

        // Parse comma-separated list of key IDs
        Set<String> keyIds = Arrays.stream(trustedKeyIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());

        if (keyIds.isEmpty()) {
            return Map.of();
        }

        Map<String, JWK> keyMap = new HashMap<>();

        // Get all keys from realm (including disabled ones) and convert to JWK format
        session.keys().getKeysStream(realm)
                .filter(key -> keyIds.contains(key.getKid()) && key.getPublicKey() != null)
                .forEach(key -> {
                    try {
                        JWKBuilder builder = JWKBuilder.create()
                                .kid(key.getKid())
                                .algorithm(key.getAlgorithmOrDefault());
                        List<X509Certificate> certificates = Optional.ofNullable(key.getCertificateChain())
                                .filter(certs -> !certs.isEmpty())
                                .orElseGet(() -> Optional.ofNullable(key.getCertificate())
                                        .map(Collections::singletonList)
                                        .orElseGet(Collections::emptyList));
                        JWK jwk = null;
                        if (Objects.equals(key.getType(), KeyType.RSA)) {
                            jwk = builder.rsa(key.getPublicKey(), certificates, key.getUse());
                        } else if (Objects.equals(key.getType(), KeyType.EC)) {
                            jwk = builder.ec(key.getPublicKey(), certificates, key.getUse());
                        } else if (Objects.equals(key.getType(), KeyType.OKP)) {
                            jwk = builder.okp(key.getPublicKey(), key.getUse());
                        }
                        if (jwk != null) {
                            keyMap.put(key.getKid(), jwk);
                        } else {
                            logger.debugf("Unsupported key type '%s' for key '%s'", key.getType(), key.getKid());
                        }
                    } catch (Exception e) {
                        logger.warnf(e, "Failed to convert key '%s' to JWK format", key.getKid());
                    }
                });

        // Log any key IDs that were not found
        Set<String> foundKeyIds = keyMap.keySet();
        Set<String> missingKeyIds = keyIds.stream()
                .filter(id -> !foundKeyIds.contains(id))
                .collect(Collectors.toSet());
        if (!missingKeyIds.isEmpty()) {
            logger.warnf("The following key IDs from realm attribute '%s' were not found in realm key providers: %s",
                    OID4VCIConstants.TRUSTED_KEY_IDS_REALM_ATTR, missingKeyIds);
        }

        if (!keyMap.isEmpty()) {
            logger.debugf("Loaded %d trusted keys by key ID from realm attribute (including potentially disabled keys)", keyMap.size());
        }

        return Collections.unmodifiableMap(keyMap);
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

            logger.debugf("Loaded %d trusted keys from realm attribute JSON", keyMap.size());
            return Collections.unmodifiableMap(keyMap);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format for trusted keys: " + e.getMessage(), e);
        }
    }
}
