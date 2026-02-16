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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import java.util.Map;
import java.util.Optional;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

/**
 * Default implementation of {@link CredentialOfferStorage} that uses Keycloak's
 * {@link org.keycloak.models.SingleUseObjectProvider} for storage.
 * 
 * <p>This implementation is cluster-aware and cross-DC aware, as it relies on
 * Infinispan's distributed cache infrastructure through the singleUseObjects API.
 * The storage automatically handles expiration and prevents memory leaks through
 * the underlying cache's expiration mechanisms.
 */
class DefaultCredentialOfferStorage implements CredentialOfferStorage {

    private static final String ENTRY_KEY = "json";

    /**
     * Calculates the lifespan in seconds from the current time to the expiration timestamp.
     * 
     * @param expirationTimestamp Absolute expiration timestamp in seconds
     * @return Lifespan in seconds, or 0 if the entry is already expired
     */
    private long calculateLifespanSeconds(int expirationTimestamp) {
        int currentTime = Time.currentTime();
        long lifespan = expirationTimestamp - currentTime;
        
        // If already expired or about to expire immediately, skip storage
        // This prevents storing entries that won't be usable
        if (lifespan <= 0) {
            return 0;
        }
        
        return lifespan;
    }

    @Override
    public void putOfferState(KeycloakSession session, CredentialOfferState entry) {
        long lifespanSeconds = calculateLifespanSeconds(entry.getExpiration());
        
        // Skip storing if already expired (following pattern from InfinispanSingleUseObjectProviderFactory)
        if (lifespanSeconds <= 0) {
            return;
        }
        
        String entryJson = JsonSerialization.valueAsString(entry);
        session.singleUseObjects().put(entry.getNonce(), lifespanSeconds, Map.of(ENTRY_KEY, entryJson));
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().put(it, lifespanSeconds, Map.of(ENTRY_KEY, entryJson));
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            it.getCredentialIdentifiers().forEach( cid -> {
                session.singleUseObjects().put(cid, lifespanSeconds, Map.of(ENTRY_KEY, entryJson));
            });
        });
    }

    @Override
    public CredentialOfferState findOfferStateByNonce(KeycloakSession session, String nonce) {
        if (session.singleUseObjects().contains(nonce)) {
            String entryJson = session.singleUseObjects().get(nonce).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    @Override
    public CredentialOfferState findOfferStateByCode(KeycloakSession session, String code) {
        if (session.singleUseObjects().contains(code)) {
            String entryJson = session.singleUseObjects().get(code).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    @Override
    public CredentialOfferState findOfferStateByCredentialId(KeycloakSession session, String credId) {
        if (session.singleUseObjects().contains(credId)) {
            String entryJson = session.singleUseObjects().get(credId).get(ENTRY_KEY);
            return JsonSerialization.valueFromString(entryJson, CredentialOfferState.class);
        }
        return null;
    }

    public void replaceOfferState(KeycloakSession session, CredentialOfferState entry) {
        String entryJson = JsonSerialization.valueAsString(entry);
        session.singleUseObjects().replace(entry.getNonce(), Map.of(ENTRY_KEY, entryJson));
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().replace(it, Map.of(ENTRY_KEY, entryJson));
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            long lifespanSeconds = calculateLifespanSeconds(entry.getExpiration());
            it.getCredentialIdentifiers().forEach( cid -> {
                if (session.singleUseObjects().contains(cid)) {
                    session.singleUseObjects().replace(cid, Map.of(ENTRY_KEY, entryJson));
                } else if (lifespanSeconds > 0) {
                    // Only put if not already expired
                    session.singleUseObjects().put(cid, lifespanSeconds, Map.of(ENTRY_KEY, entryJson));
                }
            });
        });
    }

    @Override
    public void removeOfferState(KeycloakSession session, CredentialOfferState entry) {
        session.singleUseObjects().remove(entry.getNonce());
        entry.getPreAuthorizedCode().ifPresent(it -> {
            session.singleUseObjects().remove(it);
        });
        Optional.ofNullable(entry.getAuthorizationDetails()).ifPresent(it -> {
            it.getCredentialIdentifiers().forEach( cid -> {
                session.singleUseObjects().remove(cid);
            });
        });
    }
}
