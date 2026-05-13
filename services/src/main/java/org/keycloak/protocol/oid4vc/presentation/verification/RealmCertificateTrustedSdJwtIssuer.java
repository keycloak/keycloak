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
package org.keycloak.protocol.oid4vc.presentation.verification;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.services.Urls;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.JsonNode;

class RealmCertificateTrustedSdJwtIssuer implements TrustedSdJwtIssuer {

    private final RealmModel realm;
    private final KeyManager keyManager;
    private final String realmIssuer;
    private final CertificateChainValidator certificateChainValidator;

    RealmCertificateTrustedSdJwtIssuer(KeycloakSession session, boolean allowSelfSigned) {
        this(
                session.getContext().getRealm(),
                session.keys(),
                Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName()),
                allowSelfSigned);
    }

    RealmCertificateTrustedSdJwtIssuer(RealmModel realm, KeyManager keyManager, String realmIssuer, boolean allowSelfSigned) {
        this.realm = realm;
        this.keyManager = keyManager;
        this.realmIssuer = realmIssuer;
        this.certificateChainValidator = new CertificateChainValidator(null, allowSelfSigned);
    }

    @Override
    public List<SignatureVerifierContext> resolveIssuerVerifyingKeys(IssuerSignedJWT issuerSignedJWT)
            throws VerificationException {
        if (!realmIssuer.equals(issuer(issuerSignedJWT))) {
            return List.of();
        }

        String algorithm = algorithm(issuerSignedJWT);
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        String kid = header != null ? header.getKeyId() : null;
        if (kid != null) {
            KeyWrapper key = keyManager.getKey(realm, kid, KeyUse.SIG, algorithm);
            if (key == null || key.getPublicKey() == null) {
                throw new VerificationException("No realm signing key found for SD-JWT issuer kid: " + kid);
            }
            validateRealmCertificateChain(key);
            return List.of(KeyWrapperUtil.createSignatureVerifierContext(key));
        }

        List<KeyWrapper> keys = keyManager.getKeysStream(realm, KeyUse.SIG, algorithm)
                .filter(key -> key.getPublicKey() != null)
                .filter(key -> !certificateChain(key).isEmpty())
                .toList();
        List<SignatureVerifierContext> verifiers = new ArrayList<>();
        for (KeyWrapper key : keys) {
            validateRealmCertificateChain(key);
            verifiers.add(KeyWrapperUtil.createSignatureVerifierContext(key));
        }
        if (verifiers.isEmpty()) {
            throw new VerificationException("No realm signing certificates available for SD-JWT issuer algorithm: " + algorithm);
        }

        return verifiers;
    }

    private void validateRealmCertificateChain(KeyWrapper key) throws VerificationException {
        List<X509Certificate> chain = certificateChain(key);
        if (chain.isEmpty()) {
            throw new VerificationException("Realm signing key does not have a certificate chain: " + key.getKid());
        }
        certificateChainValidator.validateRealmChain(chain);
    }

    private List<X509Certificate> certificateChain(KeyWrapper key) {
        if (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty()) {
            return key.getCertificateChain();
        }
        if (key.getCertificate() != null) {
            return List.of(key.getCertificate());
        }
        return List.of();
    }

    private String issuer(IssuerSignedJWT issuerSignedJWT) throws VerificationException {
        JsonNode issuerClaim = issuerSignedJWT.getPayload().get(OID4VCConstants.CLAIM_NAME_ISSUER);
        String issuer = issuerClaim != null ? issuerClaim.asText() : null;
        if (issuer == null || issuer.isBlank()) {
            throw new VerificationException("Missing SD-JWT issuer claim");
        }
        return issuer;
    }

    private String algorithm(IssuerSignedJWT issuerSignedJWT) throws VerificationException {
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        String algorithm = header != null && header.getAlgorithm() != null ? header.getAlgorithm().name() : null;
        if (algorithm == null) {
            throw new VerificationException("Missing SD-JWT issuer signature algorithm");
        }
        return algorithm;
    }
}
