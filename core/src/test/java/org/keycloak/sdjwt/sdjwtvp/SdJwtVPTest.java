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
package org.keycloak.sdjwt.sdjwtvp;

import java.util.Arrays;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.TestSettings;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.SdJwtVP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.SD_JWT_VC_FORMAT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public abstract class SdJwtVPTest {
    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();
    // Additional tests can be written to cover edge cases, error conditions,
    // and any other functionality specific to the SdJwt class.
    @Test
    public void testIssuerSignedJWTWithUndiclosedClaims3_3() {
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("family_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("email", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("phone_number", "eI8ZWm9QnKPpNPeNenHdhQ")
                .withUndisclosedClaim("address", "Qg_O64zqAxe412a108iroA")
                .withUndisclosedClaim("birthdate", "AJx-095VPrpTtN4QMOqROA")
                .withUndisclosedClaim("is_over_18", "Pc33JM2LchcU_lHggv_ufQ")
                .withUndisclosedClaim("is_over_21", "G02NSrQfjFXQ7Io09syajA")
                .withUndisclosedClaim("is_over_65", "lklxF5jMYlGTPUovMNIvCA")
                .build();

        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-claims.json");

        // Merge both
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        SdJwt sdJwt = SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(holderClaimSet)
                .withSigner(TestSettings.getInstance().getIssuerSignerContext())
                .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());

        String sdJwtString = sdJwt.toSdJwtString();

        SdJwtVP actualSdJwt = SdJwtVP.of(sdJwtString);

        String expectedString = TestUtils.readFileAsString(getClass(), "sdjwt/s3.3-unsecured-sd-jwt.txt");
        SdJwtVP expecteSdJwt = SdJwtVP.of(expectedString);

        TestCompareSdJwt.compare(expecteSdJwt, actualSdJwt);

    }

    @Test
    public void testIssuerSignedJWTWithUndiclosedClaims6_1() {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.1-issued-payload.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        // System.out.println(sdJwtVP.verbose());
        assertEquals(0, sdJwtVP.getRecursiveDigests().size());
        assertEquals(0, sdJwtVP.getGhostDigests().size());
    }

    @Test
    public void testA1_Example2_with_nested_disclosure_and_decoy_claims() {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/a1.example2-sdjwt.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        // System.out.println(sdJwtVP.verbose());
        assertEquals(10, sdJwtVP.getDisclosures().size());
        assertEquals(0, sdJwtVP.getRecursiveDigests().size());
        assertEquals(0, sdJwtVP.getGhostDigests().size());
    }

    @Test
    public void testS7_3_RecursiveDisclosureOfStructuredSdJwt() {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s7.3-sdjwt.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        // System.out.println(sdJwtVP.verbose());
        assertEquals(5, sdJwtVP.getDisclosures().size());
        assertEquals(4, sdJwtVP.getRecursiveDigests().size());
        assertEquals(0, sdJwtVP.getGhostDigests().size());
    }

    @Test
    public void testS7_3_GhostDisclosures() {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s7.3-sdjwt+ghost.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        // System.out.println(sdJwtVP.verbose());
        assertEquals(8, sdJwtVP.getDisclosures().size());
        assertEquals(4, sdJwtVP.getRecursiveDigests().size());
        assertEquals(3, sdJwtVP.getGhostDigests().size());
    }

    @Test
    public void testS7_3_VerifyIssuerSignaturePositive() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s7.3-sdjwt.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        sdJwtVP.getIssuerSignedJWT().verifySignature(TestSettings.getInstance().getIssuerVerifierContext());
    }

    @Test(expected = VerificationException.class)
    public void testS7_3_VerifyIssuerSignatureNegative() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s7.3-sdjwt.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        sdJwtVP.getIssuerSignedJWT().verifySignature(TestSettings.getInstance().getHolderVerifierContext());
    }

    @Test
    public void testS6_2_PresentationPositive() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        JsonNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        String presentation = sdJwtVP.present(null, keyBindingClaims,
                TestSettings.getInstance().getHolderSignerContext(), SD_JWT_VC_FORMAT);

        SdJwtVP presenteSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presenteSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from settings
        presenteSdJwtVP.getKeyBindingJWT().get().verifySignature(TestSettings.getInstance().getHolderVerifierContext());

        // Verify with public key from cnf claim
        presenteSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presenteSdJwtVP.getCnfClaim(), Algorithm.ES256));
    }

    @Test(expected = VerificationException.class)
    public void testS6_2_PresentationNegative() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        JsonNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        String presentation = sdJwtVP.present(null, keyBindingClaims,
                TestSettings.getInstance().getHolderSignerContext(), SD_JWT_VC_FORMAT);

        SdJwtVP presenteSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presenteSdJwtVP.getKeyBindingJWT().isPresent());
        // Verify with public key from cnf claim
        presenteSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presenteSdJwtVP.getCnfClaim(), Algorithm.ES256));

        // Verify with wrong public key from settings (iisuer)
        presenteSdJwtVP.getKeyBindingJWT().get().verifySignature(TestSettings.getInstance().getIssuerVerifierContext());
    }

    @Test
    public void testS6_2_PresentationPartialDisclosure() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        JsonNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        // disclose only the given_name
        String presentation = sdJwtVP.present(Arrays.asList("jsu9yVulwQQlhFlM_3JlzMaSFzglhQG0DpfayQwLUK4"),
                keyBindingClaims, TestSettings.getInstance().getHolderSignerContext(), SD_JWT_VC_FORMAT);

        SdJwtVP presenteSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presenteSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from cnf claim
        presenteSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presenteSdJwtVP.getCnfClaim(), Algorithm.ES256));
    }


    @Test
    public void testOf_validInput() {
        String sdJwtString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtString);

        assertNotNull(sdJwtVP);
        assertEquals(4, sdJwtVP.getDisclosures().size());
    }

    @Test
    public void testOf_MalformedSdJwt_ThrowsIllegalArgumentException() {
        // Given
        String malformedSdJwt = "issuer-signed-jwt";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> SdJwtVP.of(malformedSdJwt));
        assertEquals("SD-JWT is malformed, expected to contain a '~'", exception.getMessage());
    }

}
