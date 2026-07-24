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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        ObjectNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-claims.json");

        // Merge both
        holderClaimSet.setAll((ObjectNode) issuerClaimSet);

        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder().withClaims(holderClaimSet, disclosureSpec).build();
        SdJwt sdJwt = SdJwt.builder()
                .withIssuerSignedJwt(issuerSignedJWT)
                .withIssuerSigningContext(TestSettings.getInstance().getIssuerSignerContext())
                .withUseDefaultDecoys(false)
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
        ObjectNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        String presentation = sdJwtVP.present(null, true, keyBindingClaims,
                TestSettings.getInstance().getHolderSignerContext());

        SdJwtVP presenteSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presenteSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from settings
        presenteSdJwtVP.getKeyBindingJWT().get().verifySignature(TestSettings.getInstance().getHolderVerifierContext());

        // Verify with public key from cnf claim
        presenteSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presenteSdJwtVP.getCnfClaim(), Algorithm.ES256));

        assertExpectedClaims(presenteSdJwtVP, Arrays.asList("address", "given_name", "family_name"));
    }

    @Test(expected = VerificationException.class)
    public void testS6_2_PresentationNegative() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        ObjectNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        String presentation = sdJwtVP.present(null, true, keyBindingClaims,
                TestSettings.getInstance().getHolderSignerContext());

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
        ObjectNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");
        // disclose only the given_name
        String presentation = sdJwtVP.present(Arrays.asList("jsu9yVulwQQlhFlM_3JlzMaSFzglhQG0DpfayQwLUK4"), false,
                keyBindingClaims, TestSettings.getInstance().getHolderSignerContext());

        SdJwtVP presenteSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presenteSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from cnf claim
        presenteSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presenteSdJwtVP.getCnfClaim(), Algorithm.ES256));

        assertExpectedClaims(presenteSdJwtVP, Collections.singletonList("given_name"));
    }

    @Test
    public void testPresentationWithoutDisclosures() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        ObjectNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");

        // Presentation without any disclosed claims
        String presentation = sdJwtVP.present(Collections.emptyList(), false,
                keyBindingClaims, TestSettings.getInstance().getHolderSignerContext());

        SdJwtVP presentedSdJwtVP = SdJwtVP.of(presentation);
        assertTrue(presentedSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from cnf claim
        presentedSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presentedSdJwtVP.getCnfClaim(), Algorithm.ES256));

        // Assert no claims disclosed
        assertExpectedClaims(presentedSdJwtVP, Collections.emptyList());
    }

    @Test
    public void testPresentationOfSpecifiedClaims() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);
        ObjectNode keyBindingClaims = TestUtils.readClaimSet(getClass(), "sdjwt/s6.2-key-binding-claims.json");

        // Disclosures of family_name and given_name
        String presentation = sdJwtVP.present(Arrays.asList("TGf4oLbgwd5JQaHyKVQZU9UdGE0w5rtDsrZzfUaomLo", "jsu9yVulwQQlhFlM_3JlzMaSFzglhQG0DpfayQwLUK4"),
                false, null, null);

        // Creating presentation with directly specifying claims I want to disclose
        String presentation2 = sdJwtVP.presentWithSpecifiedClaims(Arrays.asList("given_name", "family_name"), false,
                null, null);

        Assert.assertEquals(presentation, presentation2);

        // Specifying not-existent claims works as well. Non-existent claim is ignored
        String presentation3 = sdJwtVP.presentWithSpecifiedClaims(Arrays.asList("given_name", "family_name", "non-existent"), false,
                null, null);
        Assert.assertEquals(presentation, presentation3);

        // Test with key-binding not present
        SdJwtVP presentedSdJwtVP = SdJwtVP.of(presentation);
        assertFalse(presentedSdJwtVP.getKeyBindingJWT().isPresent());

        // Test with key-binding present
        String presentation4 = sdJwtVP.presentWithSpecifiedClaims(Arrays.asList("given_name", "family_name"), false,
                keyBindingClaims, TestSettings.getInstance().getHolderSignerContext());

        presentedSdJwtVP = SdJwtVP.of(presentation4);
        assertTrue(presentedSdJwtVP.getKeyBindingJWT().isPresent());

        // Verify with public key from cnf claim
        presentedSdJwtVP.getKeyBindingJWT().get()
                .verifySignature(TestSettings.verifierContextFrom(presentedSdJwtVP.getCnfClaim(), Algorithm.ES256));

        // Assert only given_name and family_name claims disclosed in the new presentation
        assertExpectedClaims(presentedSdJwtVP, Arrays.asList("given_name", "family_name"));
    }

    private void assertExpectedClaims(SdJwtVP presentedSdJwtVP, List<String> expectedClaims) {
        Set<String> availableClaims = presentedSdJwtVP.getClaims().values()
                .stream()
                .filter(arrayNode -> arrayNode.size() == 3) // Filter array claims
                .map(arrayNode -> arrayNode.get(1).asText())
                .collect(Collectors.toSet());

        Assert.assertEquals(availableClaims, new HashSet<>(expectedClaims));
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
