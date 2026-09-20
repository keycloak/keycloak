/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication.requiredactions;

import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.truststore.TruststoreProvider;

import com.webauthn4j.anchor.KeyStoreException;
import com.webauthn4j.anchor.TrustAnchorRepository;
import com.webauthn4j.data.attestation.authenticator.AAGUID;

/**
 * Looks up {@link TrustAnchor}s from the Keycloak {@link TruststoreProvider} on every call, without caching
 * anything, unlike {@link com.webauthn4j.anchor.KeyStoreTrustAnchorRepository}, which scans the whole
 * {@link KeyStore} once in its constructor and caches the result forever.
 * <p>
 * This repository is only consulted while an actual WebAuthn registration is being verified (see
 * {@link com.webauthn4j.verifier.attestation.trustworthiness.certpath.DefaultCertPathTrustworthinessVerifier#verify}),
 * which happens far less often than a {@link WebAuthnRegisterFactory} is instantiated (once per login, merely
 * to check whether the required action is triggered), so re-scanning the truststore here on each use is cheap
 * enough that no caching is needed.
 */
public class KeycloakTrustAnchorRepository implements TrustAnchorRepository {

    private final KeycloakSession session;

    public KeycloakTrustAnchorRepository(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Set<TrustAnchor> find(AAGUID aaguid) {
        return loadTrustAnchors();
    }

    @Override
    public Set<TrustAnchor> find(byte[] attestationCertificateKeyIdentifier) {
        return loadTrustAnchors();
    }

    private Set<TrustAnchor> loadTrustAnchors() {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        KeyStore keyStore = truststoreProvider == null ? null : truststoreProvider.getTruststore();
        if (keyStore == null) {
            return Collections.emptySet();
        }
        try {
            Set<TrustAnchor> trustAnchors = new HashSet<>();
            for (String alias : Collections.list(keyStore.aliases())) {
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                trustAnchors.add(new TrustAnchor(certificate, null));
            }
            return trustAnchors;
        } catch (java.security.KeyStoreException e) {
            throw new KeyStoreException("Failed to load TrustAnchor from keystore", e);
        }
    }
}
