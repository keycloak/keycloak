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
import org.keycloak.protocol.oidc.utils.JWKSServerUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

/**
 * Shared trusted-key loader for attestation-aware proof validators.
 */
public final class TrustedAttestationKeysLoader {

    private static final Logger logger = Logger.getLogger(TrustedAttestationKeysLoader.class);

    private TrustedAttestationKeysLoader() {
    }

    /**
     * Merges trusted keys from realm JWKS, {@code oid4vc.attestation.trusted_keys} JSON, and {@code trusted_key_ids}.
     */
    public static Map<String, JWK> loadTrustedKeysFromRealm(KeycloakSession session) {
        RealmModel contextRealm = session.getContext().getRealm();
        if (contextRealm == null) {
            logger.debugf("No realm available, returning empty trusted keys map");
            return Map.of();
        }
        // Prefer RealmProvider resolution for JWKS; fall back to context realm so we never skip attribute-based keys
        // when getRealm(id) is unavailable in the current session.
        RealmModel realm = session.realms().getRealm(contextRealm.getId());
        if (realm == null) {
            realm = contextRealm;
        }

        // Load keys from session as default/fallback (lowest priority)
        Map<String, JWK> sessionKeys = loadKeysFromSession(session, realm);

        // oid4vc.attestation.* strings are read from the context realm so in-memory updates on that instance
        // (tests, same-request mutations) are visible even when getRealm(id) returns a separately cached copy.
        Map<String, JWK> attributeKeys = loadKeysFromRealmAttribute(contextRealm);

        // Load keys by ID from realm attribute (highest priority, can include disabled keys)
        Map<String, JWK> keyIdsKeys = loadKeysByKeyIds(session, contextRealm);

        // Merge with priority: keyIdsKeys > attributeKeys > sessionKeys
        Map<String, JWK> mergedKeys = new HashMap<>(sessionKeys);
        mergedKeys.putAll(attributeKeys);
        mergedKeys.putAll(keyIdsKeys);

        if (mergedKeys.isEmpty()) {
            logger.debugf("No trusted keys found for attestation-aware proof validation");
        } else {
            logger.debugf("Loaded %d trusted keys for attestation-aware proof validation (%d from session, %d from realm attribute JSON, %d from realm attribute key IDs)",
                    mergedKeys.size(), sessionKeys.size(), attributeKeys.size(), keyIdsKeys.size());
        }

        return Collections.unmodifiableMap(mergedKeys);
    }

    private static Map<String, JWK> loadKeysFromSession(KeycloakSession session, RealmModel realm) {
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
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            logger.warnf(e, "Failed to load keys from session for realm '%s'", realm.getName());
            return Map.of();
        }
    }

    private static Map<String, JWK> loadKeysFromRealmAttribute(RealmModel realm) {
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

    private static Map<String, JWK> loadKeysByKeyIds(KeycloakSession session, RealmModel realm) {
        String trustedKeyIds = realm.getAttribute(OID4VCIConstants.TRUSTED_KEY_IDS_REALM_ATTR);
        if (trustedKeyIds == null || trustedKeyIds.trim().isEmpty()) {
            return Map.of();
        }

        Set<String> keyIds = Arrays.stream(trustedKeyIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());

        if (keyIds.isEmpty()) {
            return Map.of();
        }

        Map<String, JWK> keyMap = new HashMap<>();

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

    private static Map<String, JWK> parseTrustedKeys(String json) {
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
