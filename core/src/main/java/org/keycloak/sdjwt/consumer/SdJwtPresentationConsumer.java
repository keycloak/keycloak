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

package org.keycloak.sdjwt.consumer;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;

/**
 * A component for consuming (verifying) SD-JWT presentations.
 *
 * <p>
 * The purpose is to streamline SD-JWT VP verification beyond signature
 * and disclosure checks of {@link org.keycloak.sdjwt.SdJwtVerificationContext}
 * </p>
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SdJwtPresentationConsumer {

    /**
     * Verify SD-JWT presentation against specific requirements.
     *
     * @param sdJwtVP                         the presentation to verify
     * @param presentationRequirements        the requirements on presented claims
     * @param trustedSdJwtIssuers             trusted issuers for the verification
     * @param issuerSignedJwtVerificationOpts policy for Issuer-signed JWT verification
     * @param keyBindingJwtVerificationOpts   policy for Key-binding JWT verification
     * @throws VerificationException if the verification fails for some reason
     */
    public void verifySdJwtPresentation(
            SdJwtVP sdJwtVP,
            PresentationRequirements presentationRequirements,
            List<TrustedSdJwtIssuer> trustedSdJwtIssuers,
            IssuerSignedJwtVerificationOpts issuerSignedJwtVerificationOpts,
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts
    ) throws VerificationException {
        // Retrieve verifying keys for Issuer-signed JWT
        IssuerSignedJWT issuerSignedJWT = sdJwtVP.getIssuerSignedJWT();
        List<SignatureVerifierContext> issuerVerifyingKeys = new ArrayList<>();
        for (TrustedSdJwtIssuer trustedSdJwtIssuer : trustedSdJwtIssuers) {
            List<SignatureVerifierContext> keys = trustedSdJwtIssuer
                    .resolveIssuerVerifyingKeys(issuerSignedJWT);
            issuerVerifyingKeys.addAll(keys);
        }

        // Verify the SD-JWT token cryptographically
        // Pass presentation requirements to enforce that the presented token meets them
        sdJwtVP.getSdJwtVerificationContext()
                .verifyPresentation(
                        issuerVerifyingKeys,
                        issuerSignedJwtVerificationOpts,
                        keyBindingJwtVerificationOpts,
                        presentationRequirements
                );
    }
}
