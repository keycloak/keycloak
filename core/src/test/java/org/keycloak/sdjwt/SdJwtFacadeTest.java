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

import java.util.Collections;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.rule.CryptoInitRule;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for SdJwtFacade.
 *
 * @author <a href="mailto:rodrick.awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public abstract class SdJwtFacadeTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    private static final String HASH_ALGORITHM = "sha-256";
    private static final String JWS_TYPE = "JWS_TYPE";

    private SdJwtFacade sdJwtFacade;

    private JsonNode claimSet;
    private DisclosureSpec disclosureSpec;

    @Before
    public void setUp() {
        SignatureSignerContext signer = TestSettings.getInstance().getIssuerSignerContext();

        sdJwtFacade = new SdJwtFacade(signer, HASH_ALGORITHM, JWS_TYPE);

        claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-holder-claims.json");
        disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("sub", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                .build();
    }

    @Test
    public void shouldCreateSdJwtSuccessfully() {
        SdJwt createdSdJwt = sdJwtFacade.createSdJwt(claimSet, disclosureSpec);

        assertNotNull(createdSdJwt);
    }

    @Test
    public void shouldVerifySdJwtSuccessfullyWithValidKeys() {
        claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-issuer-claims.json");

        SdJwt sdJwt = sdJwtFacade.createSdJwt(claimSet, disclosureSpec);

        List<SignatureVerifierContext> verifyingKeys = Collections.singletonList(
                createSignatureVerifierContext("doc-signer-05-25-2022", "ES256", true)
        );
        IssuerSignedJwtVerificationOpts verificationOpts = createVerificationOptions();

        try {
            sdJwtFacade.verifySdJwt(sdJwt, verifyingKeys, verificationOpts);
        } catch (VerificationException e) {
            fail("Verification failed: " + e.getMessage());
        }
    }

    @Test
    public void shouldReturnSdJwtString() {
        SdJwt sdJwt = sdJwtFacade.createSdJwt(claimSet, disclosureSpec);

        String sdJwtString = sdJwtFacade.getSdJwtString(sdJwt);

        assertNotNull(sdJwtString);
        assertEquals(sdJwt.toString(), sdJwtString);
    }

    @Test
    public void shouldFailVerificationWithInvalidKeys() {
        claimSet = TestUtils.readClaimSet(getClass(), "sdjwt/a1.example2-issuer-claims.json");
        SdJwt sdJwt = sdJwtFacade.createSdJwt(claimSet, disclosureSpec);

        List<SignatureVerifierContext> invalidKeys = Collections.singletonList(
                createSignatureVerifierContext("invalid-key-id", "invalid-algorithm", false)
        );
        IssuerSignedJwtVerificationOpts verificationOpts = createVerificationOptions();

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwtFacade.verifySdJwt(sdJwt, invalidKeys, verificationOpts)
        );

        assertTrue(exception.getMessage().contains("Signature could not be verified"));
    }

    private SignatureVerifierContext createSignatureVerifierContext(String kid, String algorithm, boolean verificationResult) {
        return new SignatureVerifierContext() {
            @Override
            public String getKid() {
                return kid;
            }

            @Override
            public String getAlgorithm() {
                return algorithm;
            }

            @Override
            public boolean verify(byte[] data, byte[] signature) {
                return verificationResult;
            }
        };
    }

    private IssuerSignedJwtVerificationOpts createVerificationOptions() {
        return IssuerSignedJwtVerificationOpts.builder()
                .withRequireIssuedAtClaim(false)
                .withRequireExpirationClaim(false)
                .withRequireNotBeforeClaim(false)
                .build();
    }
}
