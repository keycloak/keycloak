/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBody;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBody;
import org.keycloak.protocol.oid4vc.model.CredentialBuildConfig;

import org.jboss.logging.Logger;

/**
 * {@link CredentialSigner} implementing the SD_JWT_VC format. It returns the signed SD-JWT as a String.
 * <p>
 * {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
 * {@see https://www.ietf.org/archive/id/draft-fett-oauth-selective-disclosure-jwt-02.html}
 */
public class SdJwtCredentialSigner extends AbstractCredentialSigner<String> {

    private static final Logger LOGGER = Logger.getLogger(SdJwtCredentialSigner.class);

    public SdJwtCredentialSigner(KeycloakSession keycloakSession) {
        super(keycloakSession);
    }

    @Override
    public String signCredential(CredentialBody credentialBody, CredentialBuildConfig credentialBuildConfig)
            throws CredentialSignerException {
        if (!(credentialBody instanceof SdJwtCredentialBody sdJwtCredentialBody)) {
            throw new CredentialSignerException("Credential body unexpectedly not of type SdJwtCredentialBody");
        }

        LOGGER.debugf("Sign credentials to sd-jwt format.");

        // Get the signer first to ensure we use the exact same key that will sign the credential
        SignatureSignerContext signer = getSigner(credentialBuildConfig);

        // Add x5c certificate chain to the header if available (required by HAIP-6.1.1)
        // See: https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6.1.1
        addX5cHeader(sdJwtCredentialBody, signer);

        return sdJwtCredentialBody.sign(signer);
    }

    /**
     * Adds x5c certificate chain to the IssuerSignedJWT header if available.
     * This is required by HAIP-6.1.1 for SD-JWT credentials.
     * <p>
     * Uses the certificate chain from the signer to ensure we use the exact same key
     * that will be used for signing, following Keycloak's established pattern.
     * <p>
     * See <a href="https://openid.github.io/OpenID4VC-HAIP/openid4vc-high-assurance-interoperability-profile-wg-draft.html#section-6.1.1">HAIP Section 6.1.1</a>
     * for the requirement on issuer identification and key resolution.
     *
     * @param sdJwtCredentialBody The SD-JWT credential body to add x5c to
     * @param signer              The signer context containing the certificate(s) for the signing key
     */
    private void addX5cHeader(SdJwtCredentialBody sdJwtCredentialBody, SignatureSignerContext signer) throws CredentialSignerException {
        List<X509Certificate> certificateChain = signer.getCertificateChain();

        if (certificateChain != null && !certificateChain.isEmpty()) {
            // Copy and remove any trailing self-signed certificate(s) (trust anchors) to satisfy HAIP-6.1.1,
            // which requires that the trust anchor is NOT included in the x5c chain.
            List<X509Certificate> filteredChain = certificateChain.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Per HAIP-6.1.1: "The X.509 certificate signing the request MUST NOT be self-signed."
            // Check if the first certificate (signing certificate) is self-signed
            if (!filteredChain.isEmpty()) {
                X509Certificate signingCert = filteredChain.get(0);
                if (signingCert.getSubjectX500Principal().equals(signingCert.getIssuerX500Principal())) {
                    throw new CredentialSignerException("HAIP-6.1.1 violation: signing certificate MUST NOT be self-signed.");
                }
            }

            // Remove trailing self-signed certificates (trust anchors) from the chain
            // Per HAIP-6.1.1: "The X.509 certificate of the trust anchor MUST NOT be included in the x5c JOSE header"
            while (!filteredChain.isEmpty()) {
                X509Certificate last = filteredChain.get(filteredChain.size() - 1);
                if (last.getSubjectX500Principal().equals(last.getIssuerX500Principal())) {
                    // Last certificate is self-signed (trust anchor) -> drop it from x5c
                    filteredChain.remove(filteredChain.size() - 1);
                } else {
                    break;
                }
            }

            // If all certificates were self-signed (trust anchors), issuance is not HAIP-compliant
            if (filteredChain.isEmpty()) {
                throw new CredentialSignerException("HAIP-6.1.1 violation: x5c chain is empty after removing trust anchor certificates.");
            }

            List<String> x5cList = filteredChain.stream()
                    .map(cert -> {
                        try {
                            return Base64.getEncoder().encodeToString(cert.getEncoded());
                        } catch (CertificateEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            if (!x5cList.isEmpty()) {
                sdJwtCredentialBody.getIssuerSignedJWT().getJwsHeader().setX5c(x5cList);
            } else {
                throw new CredentialSignerException("HAIP-6.1.1 violation: no valid certificates available for x5c header.");
            }
        } else {
            throw new CredentialSignerException("HAIP-6.1.1 violation: no certificate chain available for SD-JWT x5c header.");
        }
    }
}
