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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.rule.CryptoInitRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_SD_UNDISCLOSED_ARRAY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public abstract class SdJwtVerificationTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    static ObjectMapper mapper = new ObjectMapper();
    static TestSettings testSettings = TestSettings.getInstance();

    @Test
    public void settingsTest() {
        SignatureSignerContext issuerSignerContext = testSettings.issuerSigContext;
        assertNotNull(issuerSignerContext);
    }

    @Test
    public void testSdJwtVerification_FlatSdJwt() throws VerificationException {
        for (String hashAlg : Arrays.asList("sha-256", "sha-384", "sha-512")) {
            SdJwt sdJwt = exampleFlatSdJwtV1()
                    .withHashAlgorithm(hashAlg)
                    .build();

            sdJwt.verify(
                    defaultIssuerVerifyingKeys(),
                    defaultIssuerSignedJwtVerificationOpts().build()
            );
        }
    }

    @Test
    public void testSdJwtVerification_EnforceIdempotence() throws VerificationException {
        SdJwt sdJwt = exampleFlatSdJwtV1().build();

        sdJwt.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build()
        );

        sdJwt.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build()
        );
    }

    @Test
    public void testSdJwtVerification_SdJwtWithUndisclosedNestedFields() throws VerificationException {
        SdJwt sdJwt = exampleSdJwtWithUndisclosedNestedFieldsV1().build();

        sdJwt.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build()
        );
    }

    @Test
    public void testSdJwtVerification_SdJwtWithUndisclosedArrayElements() throws Exception {
        SdJwt sdJwt = exampleSdJwtWithUndisclosedArrayElementsV1().build();

        sdJwt.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build()
        );
    }

    @Test
    public void testSdJwtVerification_RecursiveSdJwt() throws Exception {
        SdJwt sdJwt = exampleRecursiveSdJwtV1().build();

        sdJwt.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build()
        );
    }

    @Test
    public void sdJwtVerificationShouldFail_OnInsecureHashAlg() {
        SdJwt sdJwt = exampleFlatSdJwtV1()
                .withHashAlgorithm("sha-224") // not deemed secure
                .build();

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwt.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build()
                )
        );

        assertEquals("Unexpected or insecure hash algorithm: sha-224", exception.getMessage());
    }

    @Test
    public void sdJwtVerificationShouldFail_WithWrongVerifier() {
        SdJwt sdJwt = exampleFlatSdJwtV1().build();
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwt.verify(
                        Collections.singletonList(testSettings.holderVerifierContext), // wrong verifier
                        defaultIssuerSignedJwtVerificationOpts().build()
                )
        );

        assertThat(exception.getMessage(), is("Invalid Issuer-Signed JWT: Signature could not be verified"));
    }

    @Test
    public void sdJwtVerificationShouldFail_IfExpired() {
        long now = Instant.now().getEpochSecond();

        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John");
        claimSet.put("exp", now - 1000); // expired 1000 seconds ago

        // Exp claim is plain
        SdJwt sdJwtV1 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder().build()).build();
        // Exp claim is undisclosed
        SdJwt sdJwtV2 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                .withRedListedClaimNames(DisclosureRedList.of(Collections.emptySet()))
                .withUndisclosedClaim("exp", "eluV5Og3gSNII8EYnsxA_A")
                .build()).build();

        for (SdJwt sdJwt : Arrays.asList(sdJwtV1, sdJwtV2)) {
            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> sdJwt.verify(
                            defaultIssuerVerifyingKeys(),
                            defaultIssuerSignedJwtVerificationOpts().build()
                    )
            );

            assertEquals("Issuer-Signed JWT: Invalid `exp` claim", exception.getMessage());
            assertEquals("JWT has expired", exception.getCause().getMessage());
        }
    }

    @Test
    public void sdJwtVerificationShouldFail_IfExpired_CaseExpInvalid() {
        // exp: null
        ObjectNode claimSet1 = mapper.createObjectNode();
        claimSet1.put("given_name", "John");

        // exp: invalid
        ObjectNode claimSet2 = mapper.createObjectNode();
        claimSet1.put("given_name", "John");
        claimSet1.set("exp", null);

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .build();

        SdJwt sdJwtV1 = exampleFlatSdJwtV2(claimSet1, disclosureSpec).build();
        SdJwt sdJwtV2 = exampleFlatSdJwtV2(claimSet2, disclosureSpec).build();

        for (SdJwt sdJwt : Arrays.asList(sdJwtV1, sdJwtV2)) {
            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> sdJwt.verify(
                            defaultIssuerVerifyingKeys(),
                            defaultIssuerSignedJwtVerificationOpts()
                                    .withRequireExpirationClaim(true)
                                    .build()
                    )
            );

            assertEquals("Issuer-Signed JWT: Invalid `exp` claim", exception.getMessage());
            assertEquals("Missing 'exp' claim or null", exception.getCause().getMessage());
        }
    }

    @Test
    public void sdJwtVerificationShouldFail_IfIssuedInTheFuture() {
        long now = Instant.now().getEpochSecond();

        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John");
        claimSet.put("iat", now + 1000); // issued in the future

        // Exp claim is plain
        SdJwt sdJwtV1 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder().build()).build();
        // Exp claim is undisclosed
        SdJwt sdJwtV2 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                .withRedListedClaimNames(DisclosureRedList.of(Collections.emptySet()))
                .withUndisclosedClaim("iat", "eluV5Og3gSNII8EYnsxA_A")
                .build()).build();

        for (SdJwt sdJwt : Arrays.asList(sdJwtV1, sdJwtV2)) {
            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> sdJwt.verify(
                            defaultIssuerVerifyingKeys(),
                            defaultIssuerSignedJwtVerificationOpts().build()
                    )
            );

            assertEquals("Issuer-Signed JWT: Invalid `iat` claim", exception.getMessage());
            assertEquals("JWT was issued in the future", exception.getCause().getMessage());
        }
    }

    @Test
    public void sdJwtVerificationShouldFail_IfNbfInvalid() {
        long now = Instant.now().getEpochSecond();

        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John");
        claimSet.put("nbf", now + 1000); // now will be too soon to accept the jwt

        // Exp claim is plain
        SdJwt sdJwtV1 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder().build()).build();
        // Exp claim is undisclosed
        SdJwt sdJwtV2 = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                .withRedListedClaimNames(DisclosureRedList.of(Collections.emptySet()))
                .withUndisclosedClaim("iat", "eluV5Og3gSNII8EYnsxA_A")
                .build()).build();

        for (SdJwt sdJwt : Arrays.asList(sdJwtV1, sdJwtV2)) {
            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> sdJwt.verify(
                            defaultIssuerVerifyingKeys(),
                            defaultIssuerSignedJwtVerificationOpts().build()
                    )
            );

            assertEquals("Issuer-Signed JWT: Invalid `nbf` claim", exception.getMessage());
            assertEquals("JWT is not yet valid", exception.getCause().getMessage());
        }
    }

    @Test
    public void sdJwtVerificationShouldFail_IfSdArrayElementIsNotString() throws JsonProcessingException {
        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John");
        claimSet.set(CLAIM_NAME_SD, mapper.readTree("[123]"));

        SdJwt sdJwt = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder().build()).build();

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwt.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build()
                )
        );

        assertEquals("Unexpected non-string element inside _sd array: 123", exception.getMessage());
    }

    @Test
    public void sdJwtVerificationShouldFail_IfForbiddenClaimNames() {
        for (String forbiddenClaimName : Arrays.asList(CLAIM_NAME_SD, CLAIM_NAME_SD_UNDISCLOSED_ARRAY)) {
            ObjectNode claimSet = mapper.createObjectNode();
            claimSet.put(forbiddenClaimName, "Value");

            SdJwt sdJwt = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                    .withUndisclosedClaim(forbiddenClaimName, "eluV5Og3gSNII8EYnsxA_A")
                    .build()).build();

            VerificationException exception = assertThrows(
                    VerificationException.class,
                    () -> sdJwt.verify(
                            defaultIssuerVerifyingKeys(),
                            defaultIssuerSignedJwtVerificationOpts().build()
                    )
            );

            assertEquals("Disclosure claim name must not be '_sd' or '...'", exception.getMessage());
        }
    }

    @Test
    public void sdJwtVerificationShouldFail_IfDuplicateDigestValue() {
        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John"); // this same field will also be nested

        SdJwt sdJwt = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withDecoyClaim("G02NSrQfjFXQ7Io09syajA")
                .withDecoyClaim("G02NSrQfjFXQ7Io09syajA")
                .build()).build();

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwt.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build()
                )
        );

        assertTrue(exception.getMessage().startsWith("A digest was encountered more than once:"));
    }

    @Test
    public void sdJwtVerificationShouldFail_IfDuplicateSaltValue() {
        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("given_name", "John");
        claimSet.put("family_name", "Doe");

        String salt = "eluV5Og3gSNII8EYnsxA_A";
        SdJwt sdJwt = exampleFlatSdJwtV2(claimSet, DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", salt)
                // We are reusing the same salt value, and that is the problem
                .withUndisclosedClaim("family_name", salt)
                .build()).build();

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwt.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build()
                )
        );

        assertEquals("A salt value was reused: " + salt, exception.getMessage());
    }

    private List<SignatureVerifierContext> defaultIssuerVerifyingKeys() {
        return Collections.singletonList(testSettings.issuerVerifierContext);
    }

    private IssuerSignedJwtVerificationOpts.Builder defaultIssuerSignedJwtVerificationOpts() {
        return IssuerSignedJwtVerificationOpts.builder()
                .withRequireIssuedAtClaim(false)
                .withRequireExpirationClaim(false)
                .withRequireNotBeforeClaim(false);
    }

    private SdJwt.Builder exampleFlatSdJwtV1() {
        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("sub", "6c5c0a49-b589-431d-bae7-219122a9ec2c");
        claimSet.put("given_name", "John");
        claimSet.put("family_name", "Doe");
        claimSet.put("email", "john.doe@example.com");

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("email", "eI8ZWm9QnKPpNPeNenHdhQ")
                .withDecoyClaim("G02NSrQfjFXQ7Io09syajA")
                .build();

        return SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(claimSet)
                .withSigner(testSettings.issuerSigContext);
    }

    private SdJwt.Builder exampleFlatSdJwtV2(ObjectNode claimSet, DisclosureSpec disclosureSpec) {
        return SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(claimSet)
                .withSigner(testSettings.issuerSigContext);
    }

    private SdJwt exampleAddrSdJwt() {
        ObjectNode addressClaimSet = mapper.createObjectNode();
        addressClaimSet.put("street_address", "Rue des Oliviers");
        addressClaimSet.put("city", "Paris");
        addressClaimSet.put("country", "France");

        DisclosureSpec addrDisclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("street_address", "AJx-095VPrpTtN4QMOqROA")
                .withUndisclosedClaim("city", "G02NSrQfjFXQ7Io09syajA")
                .withDecoyClaim("G02NSrQfjFXQ7Io09syajA")
                .build();

        return SdJwt.builder()
                .withDisclosureSpec(addrDisclosureSpec)
                .withClaimSet(addressClaimSet)
                .build();
    }

    private SdJwt.Builder exampleSdJwtWithUndisclosedNestedFieldsV1() {
        SdJwt addrSdJWT = exampleAddrSdJwt();

        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("sub", "6c5c0a49-b589-431d-bae7-219122a9ec2c");
        claimSet.put("given_name", "John");
        claimSet.put("family_name", "Doe");
        claimSet.put("email", "john.doe@example.com");
        claimSet.set("address", addrSdJWT.asNestedPayload());

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("email", "eI8ZWm9QnKPpNPeNenHdhQ")
                .build();

        return SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(claimSet)
                .withNestedSdJwt(addrSdJWT)
                .withSigner(testSettings.issuerSigContext);
    }

    private SdJwt.Builder exampleSdJwtWithUndisclosedArrayElementsV1() throws JsonProcessingException {
        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("sub", "6c5c0a49-b589-431d-bae7-219122a9ec2c");
        claimSet.put("given_name", "John");
        claimSet.put("family_name", "Doe");
        claimSet.put("email", "john.doe@example.com");
        claimSet.set("nationalities", mapper.readTree("[\"US\", \"DE\"]"));

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("email", "eI8ZWm9QnKPpNPeNenHdhQ")
                .withUndisclosedArrayElt("nationalities", 1, "nPuoQnkRFq3BIeAm7AnXFA")
                .withDecoyArrayElt("nationalities", 2, "G02NSrQfjFXQ7Io09syajA")
                .build();

        return SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(claimSet)
                .withSigner(testSettings.issuerSigContext);
    }

    private SdJwt.Builder exampleRecursiveSdJwtV1() {
        SdJwt addrSdJWT = exampleAddrSdJwt();

        ObjectNode claimSet = mapper.createObjectNode();
        claimSet.put("sub", "6c5c0a49-b589-431d-bae7-219122a9ec2c");
        claimSet.put("given_name", "John");
        claimSet.put("family_name", "Doe");
        claimSet.put("email", "john.doe@example.com");
        claimSet.set("address", addrSdJWT.asNestedPayload());

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("family_name", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("email", "eI8ZWm9QnKPpNPeNenHdhQ")
                // Making the whole address object selectively disclosable makes the process recursive
                .withUndisclosedClaim("address", "BZFzhQsdPfZY1WSL-1GXKg")
                .build();

        return SdJwt.builder()
                .withDisclosureSpec(disclosureSpec)
                .withClaimSet(claimSet)
                .withNestedSdJwt(addrSdJWT)
                .withSigner(testSettings.issuerSigContext);
    }
}
