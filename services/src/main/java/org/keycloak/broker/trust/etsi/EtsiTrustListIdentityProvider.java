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

package org.keycloak.broker.trust.etsi;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.broker.provider.TrustMaterialIdentityProvider;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.broker.provider.X509TrustMaterial;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.PublicKeyStorageUtils;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * Exposes the CA certificates of an ETSI TS 119 602 List of Trusted Entities as X.509 trust anchors.
 * HAIP requires the signing certificate in x5c and keeps the trust anchor out of it, so an artifact
 * needs a chain to a listed CA. No key can be selected by kid and listed end entity certificates are
 * ignored.
 */
public class EtsiTrustListIdentityProvider implements TrustMaterialIdentityProvider<EtsiTrustListIdentityProviderConfig> {

    private static final Logger logger = Logger.getLogger(EtsiTrustListIdentityProvider.class);

    // Index of the keyCertSign bit in the key usage extension, see RFC 5280 clause 4.2.1.3
    private static final int KEY_CERT_SIGN = 5;

    private final KeycloakSession session;
    private final EtsiTrustListIdentityProviderConfig config;

    public EtsiTrustListIdentityProvider(KeycloakSession session, EtsiTrustListIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public EtsiTrustListIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public Stream<JWK> resolveKeys(TrustMaterialRequest request) {
        return Stream.empty();
    }

    @Override
    public Stream<X509TrustMaterial> resolveX509Trust(TrustMaterialRequest request) {
        Set<X509Certificate> trustAnchors = new LinkedHashSet<>(listedCertificates().stream()
                .filter(EtsiTrustListIdentityProvider::isCaCertificate)
                .filter(EtsiTrustListIdentityProvider::isWithinValidityPeriod)
                .toList());
        if (trustAnchors.isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(new X509TrustMaterial(trustAnchors, List.of()));
    }

    @Override
    public void close() {
    }

    private List<X509Certificate> listedCertificates() {
        String modelKey = PublicKeyStorageUtils.getIdpModelCacheKey(session.getContext().getRealm().getId(), config.getInternalId());
        List<KeyWrapper> keys;
        try {
            keys = session.getProvider(PublicKeyStorageProvider.class)
                    .getKeys(modelKey, new EtsiTrustListPublicKeyLoader(session, config));
        } catch (RuntimeException e) {
            // Other trust providers of the client still work while this list is unavailable
            logger.warnf(e, "Failed to load the trust list of identity provider '%s'", config.getAlias());
            return List.of();
        }
        return keys.stream()
                .map(KeyWrapper::getCertificate)
                .filter(Objects::nonNull)
                .toList();
    }

    private static boolean isCaCertificate(X509Certificate certificate) {
        boolean[] keyUsage = certificate.getKeyUsage();
        boolean ca = certificate.getBasicConstraints() >= 0 && (keyUsage == null || (keyUsage.length > KEY_CERT_SIGN && keyUsage[KEY_CERT_SIGN]));
        if (!ca) {
            logger.debugf("Ignoring listed certificate '%s', it is not a CA certificate", certificate.getSubjectX500Principal());
        }
        return ca;
    }

    private static boolean isWithinValidityPeriod(X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return true;
        } catch (CertificateException e) {
            return false;
        }
    }
}
