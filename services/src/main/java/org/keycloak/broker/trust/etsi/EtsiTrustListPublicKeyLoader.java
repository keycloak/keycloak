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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;

/**
 * Loads the certificates of the configured service types from a signed ETSI TS 119 602 V1.1.1 trust
 * list. The list is accepted only if one of the configured certificates verifies its signature.
 */
public class EtsiTrustListPublicKeyLoader implements PublicKeyLoader {

    private static final Logger logger = Logger.getLogger(EtsiTrustListPublicKeyLoader.class);

    private final KeycloakSession session;
    private final EtsiTrustListIdentityProviderConfig config;

    public EtsiTrustListPublicKeyLoader(KeycloakSession session, EtsiTrustListIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public PublicKeysWrapper loadKeys() throws Exception {
        JWSInput jws = new JWSInput(SimpleHttp.create(session).doGet(config.getTrustListUrl())
                .header(HttpHeaders.ACCEPT, "application/jwt")
                .asString().trim());
        verifySignature(jws);

        ListOfTrustedEntities list = parse(jws);
        if (!config.getLoteType().equals(list.schemeInformation().type())) {
            throw new IllegalStateException("Unexpected LoTE type: " + list.schemeInformation().type());
        }

        long now = Time.currentTimeMillis();
        long refreshAt = now + config.getRefreshInterval() * 1000L;
        // ETSI TS 119 602 V1.1.1 clause 6.3.15 uses a null NextUpdate for a discontinued list
        if (Strings.isEmpty(list.schemeInformation().nextUpdate())) {
            logger.warnf("Trust list of identity provider '%s' has no NextUpdate and is discontinued", config.getAlias());
            return new PublicKeysWrapper(List.of(), refreshAt);
        }

        long nextUpdate = Instant.parse(list.schemeInformation().nextUpdate()).toEpochMilli();
        if (nextUpdate <= now) {
            throw new IllegalStateException("Trust list expired at " + list.schemeInformation().nextUpdate());
        }
        return new PublicKeysWrapper(toKeys(list), Math.min(refreshAt, nextUpdate));
    }

    private void verifySignature(JWSInput jws) throws JWSInputException {
        String algorithm = jws.getHeader().getRawAlgorithm();
        SignatureProvider signatureProvider = Strings.isEmpty(algorithm) ? null : session.getProvider(SignatureProvider.class, algorithm);
        if (signatureProvider == null || !signatureProvider.isAsymmetricAlgorithm()) {
            throw new IllegalStateException("Unsupported trust list signature algorithm: " + algorithm);
        }

        byte[] signedData = jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8);
        for (X509Certificate certificate : config.getSigningCertificates()) {
            if (verifies(signatureProvider, certificate, algorithm, signedData, jws.getSignature())) {
                return;
            }
        }
        throw new IllegalStateException("Trust list signature does not match any configured signing certificate");
    }

    private boolean verifies(SignatureProvider signatureProvider, X509Certificate certificate, String algorithm,
                             byte[] signedData, byte[] signature) {
        try {
            certificate.checkValidity();
            KeyWrapper key = toKey(certificate);
            key.setAlgorithm(algorithm);
            return signatureProvider.verifier(key).verify(signedData, signature);
        } catch (Exception e) {
            logger.debugf("Signing certificate '%s' does not verify the trust list: %s", certificate.getSubjectX500Principal(), e.getMessage());
            return false;
        }
    }

    private ListOfTrustedEntities parse(JWSInput jws) throws IOException {
        ListOfTrustedEntities.Document document = JsonSerialization.readValue(jws.getContent(), ListOfTrustedEntities.Document.class);
        if (document.list() == null || document.list().schemeInformation() == null) {
            throw new IllegalStateException("Trust list payload has no LoTE object");
        }
        return document.list();
    }

    private List<KeyWrapper> toKeys(ListOfTrustedEntities list) {
        List<String> serviceTypes = config.getServiceTypes();
        List<String> serviceStatuses = config.getServiceStatuses();
        List<KeyWrapper> keys = new ArrayList<>();
        list.services()
                .filter(service -> service != null && serviceTypes.contains(service.type()))
                .filter(service -> service.status() == null || serviceStatuses.contains(service.status()))
                .flatMap(ListOfTrustedEntities.ServiceInformation::encodedCertificates)
                .forEach(encoded -> addKey(encoded, keys));
        return List.copyOf(keys);
    }

    private void addKey(String encodedCertificate, List<KeyWrapper> keys) {
        try {
            CertificateFactory factory = CryptoIntegration.getProvider().getX509CertFactory();
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(Base64.getMimeDecoder().decode(encodedCertificate)));
            keys.add(toKey(certificate));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            logger.warnf("Ignoring certificate in trust list of identity provider '%s': %s", config.getAlias(), e.getMessage());
        }
    }

    private static KeyWrapper toKey(X509Certificate certificate) throws CertificateException {
        PublicKey publicKey = certificate.getPublicKey();
        KeyWrapper key = new KeyWrapper();
        key.setUse(KeyUse.SIG);
        key.setPublicKey(publicKey);
        key.setCertificate(certificate);
        if (publicKey instanceof ECPublicKey) {
            key.setType(KeyType.EC);
        } else if (publicKey instanceof RSAPublicKey) {
            key.setType(KeyType.RSA);
        } else {
            throw new CertificateException("Unsupported public key type: " + publicKey.getAlgorithm());
        }
        return key;
    }
}
