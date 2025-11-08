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
package org.keycloak.sdjwt;

import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Simplified service for creating and managing SD-JWTs with easy-to-use methods.
 *
 * @author <a href="mailto:rodrick.awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class SdJwtFacade {

    private final SignatureSignerContext signer;
    private final String hashAlgorithm;
    private final String jwsType;

    public SdJwtFacade(SignatureSignerContext signer, String hashAlgorithm, String jwsType) {
        this.signer = signer;
        this.hashAlgorithm = hashAlgorithm;
        this.jwsType = jwsType;
    }

    /**
     * Create a new SD-JWT with the provided claim set and disclosure specification.
     *
     * @param claimSet       The claim set in JSON format.
     * @param disclosureSpec The disclosure specification.
     * @return A new SD-JWT.
     */
    public SdJwt createSdJwt(JsonNode claimSet, DisclosureSpec disclosureSpec) {
        return SdJwt.builder()
                .withClaimSet(claimSet)
                .withDisclosureSpec(disclosureSpec)
                .withSigner(signer)
                .withHashAlgorithm(hashAlgorithm)
                .withJwsType(jwsType)
                .build();
    }

    /**
     * Verify the SD-JWT using the provided signature verification keys.
     *
     * @param sdJwt               The SD-JWT to verify.
     * @param issuerVerifyingKeys List of issuer verifying keys.
     * @param verificationOpts    Options for verification.
     * @throws VerificationException if verification fails.
     */
    public void verifySdJwt(SdJwt sdJwt, List<SignatureVerifierContext> issuerVerifyingKeys,
                            IssuerSignedJwtVerificationOpts verificationOpts
    ) throws VerificationException {
        try {
            sdJwt.verify(issuerVerifyingKeys, verificationOpts);
        } catch (VerificationException e) {
            throw new VerificationException("SD-JWT verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve the SD-JWT as a string representation.
     *
     * @param sdJwt The SD-JWT to convert.
     * @return The string representation of the SD-JWT.
     */
    public String getSdJwtString(SdJwt sdJwt) {
        return sdJwt.toString();
    }
}
