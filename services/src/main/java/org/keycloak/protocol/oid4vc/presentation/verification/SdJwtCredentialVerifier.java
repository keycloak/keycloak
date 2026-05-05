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

import java.util.List;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.VerifiedSdJwt;
import org.keycloak.sdjwt.consumer.SdJwtPresentationConsumer;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.util.KeyWrapperUtil;

public class SdJwtCredentialVerifier implements CredentialVerifier {

    private final KeycloakSession session;
    private final SdJwtPresentationConsumer presentationConsumer = new SdJwtPresentationConsumer();
    private final List<TrustedSdJwtIssuer> trustedIssuers;

    public SdJwtCredentialVerifier(KeycloakSession session) {
        this(session, null);
    }

    public SdJwtCredentialVerifier(KeycloakSession session, List<TrustedSdJwtIssuer> trustedIssuers) {
        this.session = session;
        this.trustedIssuers = trustedIssuers;
    }

    @Override
    public String getSupportedFormat() {
        return VCFormat.SD_JWT_VC;
    }

    @Override
    public boolean supports(String credential) {
        return credential != null && credential.contains(OID4VCConstants.SDJWT_DELIMITER);
    }

    @Override
    public CredentialVerificationResult verify(CredentialVerificationRequest request) throws CredentialVerificationException {
        if (request == null || !supports(request.getCredential())) {
            throw new CredentialVerificationException("Unsupported SD-JWT credential");
        }

        try {
            SdJwtVP sdJwtVP = SdJwtVP.of(request.getCredential());

            IssuerSignedJwtVerificationOpts issuerOpts = IssuerSignedJwtVerificationOpts.builder()
                    .withClockSkew(OID4VCConstants.SD_JWT_DEFAULT_CLOCK_SKEW_SECONDS)
                    // Issuer-signed SD-JWT credentials can be long-lived. The short iat
                    // freshness window applies to the KB-JWT presentation proof, not the credential.
                    .withIatCheck(null)
                    .withExpCheck(true)
                    .withNbfCheck(true)
                    .build();

            boolean requireKeyBinding = request.getExpectedAudience() != null && request.getExpectedNonce() != null;
            KeyBindingJwtVerificationOpts.Builder kbOptsBuilder = KeyBindingJwtVerificationOpts.builder()
                    .withKeyBindingRequired(requireKeyBinding)
                    .withClockSkew(OID4VCConstants.SD_JWT_DEFAULT_CLOCK_SKEW_SECONDS)
                    .withIatCheck(OID4VCConstants.SD_JWT_KEY_BINDING_DEFAULT_ALLOWED_MAX_AGE)
                    .withNbfCheck(true);
            if (requireKeyBinding) {
                kbOptsBuilder.withAudCheck(request.getExpectedAudience()).withNonceCheck(request.getExpectedNonce());
            }

            VerifiedSdJwt verifiedSdJwt = presentationConsumer.verifySdJwtPresentation(
                    sdJwtVP,
                    null,
                    getTrustedIssuers(),
                    issuerOpts,
                    kbOptsBuilder.build());

            return toResult(verifiedSdJwt);
        } catch (Exception e) {
            throw new CredentialVerificationException("SD-JWT verification failed: " + e.getMessage(), e);
        }
    }

    private CredentialVerificationResult toResult(VerifiedSdJwt verifiedSdJwt) throws VerificationException {
        if (verifiedSdJwt == null) {
            throw new VerificationException("No disclosed SD-JWT payload available");
        }

        return new CredentialVerificationResult()
                .setFormat(VCFormat.SD_JWT_VC)
                .setClaims(verifiedSdJwt.getClaims())
                .setIssuer(verifiedSdJwt.getStringClaim(OID4VCConstants.CLAIM_NAME_ISSUER).orElse(null))
                .setCredentialType(verifiedSdJwt.getStringClaim("vct").orElse(null));
    }

    private List<TrustedSdJwtIssuer> getTrustedIssuers() {
        return trustedIssuers != null ? trustedIssuers : List.of(new RealmKeysTrustedSdJwtIssuer(session));
    }

    private static class RealmKeysTrustedSdJwtIssuer implements TrustedSdJwtIssuer {

        private final KeycloakSession session;

        private RealmKeysTrustedSdJwtIssuer(KeycloakSession session) {
            this.session = session;
        }

        @Override
        public List<SignatureVerifierContext> resolveIssuerVerifyingKeys(IssuerSignedJWT issuerSignedJWT)
                throws VerificationException {
            RealmModel realm = session.getContext().getRealm();
            JWSHeader header = issuerSignedJWT.getJwsHeader();
            String algorithm = header != null && header.getAlgorithm() != null ? header.getAlgorithm().name() : null;
            if (algorithm == null) {
                throw new VerificationException("Missing SD-JWT issuer signature algorithm");
            }

            String kid = header.getKeyId();
            if (kid != null) {
                KeyWrapper key = session.keys().getKey(realm, kid, KeyUse.SIG, algorithm);
                if (key == null || key.getPublicKey() == null) {
                    throw new VerificationException("No realm signing key found for SD-JWT issuer kid: " + kid);
                }
                return List.of(toVerifierContext(key));
            }

            List<SignatureVerifierContext> verifiers = session.keys().getKeysStream(realm, KeyUse.SIG, algorithm)
                    .filter(key -> key.getPublicKey() != null)
                    .map(RealmKeysTrustedSdJwtIssuer::toVerifierContext)
                    .toList();
            if (verifiers.isEmpty()) {
                throw new VerificationException("No realm signing keys available for SD-JWT issuer algorithm: " + algorithm);
            }

            return verifiers;
        }

        private static SignatureVerifierContext toVerifierContext(KeyWrapper key) {
            return KeyWrapperUtil.createSignatureVerifierContext(key);
        }
    }
}
