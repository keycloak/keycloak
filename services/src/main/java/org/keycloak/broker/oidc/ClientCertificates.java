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

package org.keycloak.broker.oidc;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.ClientCertificate;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

public final class ClientCertificates {

    /**
     * A key that can be used as a client certificate.
     */
    public static final class ClientCertificateKey implements ClientCertificate {

        private final String id;
        private final KeyWrapper key;

        private ClientCertificateKey(String id, KeyWrapper key) {
            this.id = id;
            this.key = key;
        }

        public KeyWrapper getKey() {
            return key;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public PrivateKey getPrivateKey() {
            return (PrivateKey) key.getPrivateKey();
        }

        @Override
        public X509Certificate[] getCertificateChain() {
            return certificateChain(key);
        }

        public X509Certificate getCertificate() {
            return getCertificateChain()[0];
        }
    }

    private ClientCertificates() {
    }

    /**
     * @return the client certificate of the key provider with the given component id
     * @throws IllegalArgumentException if it cannot be used as a client certificate
     */
    public static ClientCertificateKey find(KeycloakSession session, RealmModel realm, String keyProviderId) {
        if (StringUtil.isBlank(keyProviderId)) {
            throw new IllegalArgumentException("The client certificate key provider is required");
        }
        ComponentModel component = realm.getComponent(keyProviderId);
        if (component == null) {
            throw invalid(keyProviderId, "was not found");
        }
        if (!KeyProvider.class.getName().equals(component.getProviderType())) {
            throw invalid(keyProviderId, "is not a key provider");
        }
        List<KeyWrapper> keys = session.keys().getKeysStream(realm)
                .filter(key -> keyProviderId.equals(key.getProviderId()))
                .toList();
        KeyWrapper key = select(keys);
        String reason = unusableReason(key);
        if (reason != null) {
            throw invalid(keyProviderId, reason);
        }
        return of(realm.getId(), key);
    }

    /**
     * Like {@link #find}, but a disabled key provider cannot be used either. It can be saved as the client
     * certificate, as it can be enabled again.
     */
    public static ClientCertificateKey findEnabled(KeycloakSession session, RealmModel realm, String keyProviderId) {
        ClientCertificateKey certificate = find(session, realm, keyProviderId);
        if (!certificate.getKey().getStatus().isEnabled()) {
            throw invalid(keyProviderId, "is disabled");
        }
        return certificate;
    }

    public static Stream<ClientCertificateKey> eligibleCertificates(KeycloakSession session, RealmModel realm) {
        return session.keys().getKeysStream(realm)
                .filter(key -> key.getProviderId() != null)
                .collect(Collectors.groupingBy(KeyWrapper::getProviderId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(ClientCertificates::select)
                .filter(key -> unusableReason(key) == null)
                .map(key -> of(realm.getId(), key));
    }

    /**
     * @return the certificate of the key followed by its chain, or null if there is no certificate
     */
    static X509Certificate[] certificateChain(KeyWrapper key) {
        List<X509Certificate> chain = key.getCertificateChain();
        if (chain != null && !chain.isEmpty()) {
            return chain.toArray(new X509Certificate[0]);
        }
        if (key.getCertificate() != null) {
            return new X509Certificate[] { key.getCertificate() };
        }
        return null;
    }

    /**
     * Prefers a usable key, then an active one over a passive one over a disabled one, then the highest priority.
     *
     * @return the key, or null if there are none
     */
    static KeyWrapper select(List<KeyWrapper> keys) {
        return keys.stream()
                .min(Comparator.comparing((KeyWrapper key) -> unusableReason(key) == null ? 0 : 1)
                        .thenComparing(ClientCertificates::statusRank)
                        .thenComparing(key -> -key.getProviderPriority()))
                .orElse(null);
    }

    /**
     * @return why the key cannot be a client certificate, or null if it can
     */
    static String unusableReason(KeyWrapper key) {
        if (key == null) {
            return "has no key";
        }
        if (!(key.getPrivateKey() instanceof PrivateKey)) {
            return "has no private key";
        }
        if (key.getUse() == KeyUse.ENC) {
            return "is not a signing key";
        }
        if (certificateChain(key) == null) {
            return "has no certificate";
        }
        return null;
    }

    static ClientCertificateKey of(String realmId, KeyWrapper key) {
        // the realm id and key provider id prevent a realm admin from using the private key of another realm
        return new ClientCertificateKey(realmId + "/" + key.getProviderId(), key);
    }

    private static int statusRank(KeyWrapper key) {
        if (key.getStatus() == KeyStatus.ACTIVE) {
            return 0;
        }
        return key.getStatus().isEnabled() ? 1 : 2;
    }

    private static IllegalArgumentException invalid(String keyProviderId, String reason) {
        return new IllegalArgumentException("The client certificate key provider '" + keyProviderId + "' " + reason);
    }
}
