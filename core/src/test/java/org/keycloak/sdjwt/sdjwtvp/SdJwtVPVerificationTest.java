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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.TestSettings;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NBF;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public abstract class SdJwtVPVerificationTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    // This testsuite relies on a range of test vectors (`sdjwt/s20.*-sdjwt+kb*.txt`)
    // manually crafted to fit different cases. External tools were typically used,
    // including mkjwk.org for generating keys, jwt.io for creating signatures, and
    // base64.guru for manipulating the Base64-encoded disclosures.

    static ObjectMapper mapper = new ObjectMapper();
    static TestSettings testSettings = TestSettings.getInstance();

    @Test
    public void testVerif_s20_1_sdjwt_with_kb() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s20.1-sdjwt+kb.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

        sdJwtVP.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build(),
                defaultKeyBindingJwtVerificationOpts().build()
        );
    }

    @Test
    public void testVerif_s20_8_sdjwt_with_kb__AltCnfCurves() throws VerificationException {
        List<String> entries = Arrays.asList(
                "sdjwt/s20.8-sdjwt+kb--es384.txt",
                "sdjwt/s20.8-sdjwt+kb--es512.txt"
        );

        for (String entry : entries) {
            String sdJwtVPString = TestUtils.readFileAsString(getClass(), entry);
            SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

            sdJwtVP.verify(
                    defaultIssuerVerifyingKeys(),
                    defaultIssuerSignedJwtVerificationOpts().build(),
                    defaultKeyBindingJwtVerificationOpts().build()
            );
        }
    }

    @Test
    public void testVerif_s20_8_sdjwt_with_kb__CnfRSA() throws VerificationException {
        List<String> entries = Arrays.asList(
                "sdjwt/s20.8-sdjwt+kb--cnf-rsa-rs256.txt",
                "sdjwt/s20.8-sdjwt+kb--cnf-rsa-ps256.txt",
                "sdjwt/s20.8-sdjwt+kb--cnf-rsa-ps384.txt",
                "sdjwt/s20.8-sdjwt+kb--cnf-rsa-ps512.txt"
        );

        for (String entry : entries) {
            String sdJwtVPString = TestUtils.readFileAsString(getClass(), entry);
            SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

            sdJwtVP.verify(
                    defaultIssuerVerifyingKeys(),
                    defaultIssuerSignedJwtVerificationOpts().build(),
                    defaultKeyBindingJwtVerificationOpts().build()
            );
        }
    }

    @Test
    public void testVerifKeyBindingNotRequired() throws VerificationException {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s6.2-presented-sdjwtvp.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

        sdJwtVP.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build(),
                defaultKeyBindingJwtVerificationOpts()
                        .withKeyBindingRequired(false)
                        .build()
        );
    }

    @Test
    public void testShouldFail_IfExtraDisclosureWithNoDigest() {
        testShouldFailGeneric(
                // One disclosure has no digest throughout Issuer-signed JWT
                "sdjwt/s20.6-sdjwt+kb--disclosure-with-no-digest.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "At least one disclosure is not protected by digest",
                null
        );
    }

    @Test
    public void testShouldFail_IfFieldDisclosureLengthIncorrect() {
        testShouldFailGeneric(
                // One field disclosure has only two elements
                "sdjwt/s20.7-sdjwt+kb--invalid-field-disclosure.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "A field disclosure must contain exactly three elements",
                null
        );
    }

    @Test
    public void testShouldFail_IfArrayElementDisclosureLengthIncorrect() {
        testShouldFailGeneric(
                // One array element disclosure has more than two elements
                "sdjwt/s20.7-sdjwt+kb--invalid-array-elt-disclosure.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "An array element disclosure must contain exactly two elements",
                null
        );
    }

    @Test
    public void testShouldFail_IfKeyBindingRequiredAndMissing() {
        testShouldFailGeneric(
                // This sd-jwt has no key binding jwt
                "sdjwt/s6.2-presented-sdjwtvp.txt",
                defaultKeyBindingJwtVerificationOpts()
                        .withKeyBindingRequired(true)
                        .build(),
                "Missing Key Binding JWT",
                null
        );
    }

    @Test
    public void testShouldFail_IfKeyBindingJwtSignatureInvalid() {
        testShouldFailGeneric(
                // Messed up with the kb signature
                "sdjwt/s20.1-sdjwt+kb--wrong-kb-signature.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT invalid",
                "VerificationException: Invalid jws signature"
        );
    }

    @Test
    public void testShouldFail_IfNoCnfClaim() {
        testShouldFailGeneric(
                // This test vector has no cnf claim in Issuer-signed JWT
                "sdjwt/s20.2-sdjwt+kb--no-cnf-claim.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "No cnf claim in Issuer-signed JWT for key binding",
                null
        );
    }

    @Test
    public void testShouldFail_IfWrongKbTyp() {
        testShouldFailGeneric(
                // Key Binding JWT's header: {"kid": "holder", "typ": "unexpected",  "alg": "ES256"}
                "sdjwt/s20.3-sdjwt+kb--wrong-kb-typ.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key Binding JWT is not of declared typ kb+jwt",
                null
        );
    }

    @Test
    public void testShouldFail_IfReplayChecksFail_Nonce() {
        testShouldFailGeneric(
                "sdjwt/s20.1-sdjwt+kb.txt",
                defaultKeyBindingJwtVerificationOpts()
                        .withNonce("abcd") // kb's nonce is "1234567890"
                        .build(),
                "Key binding JWT: Unexpected `nonce` value",
                null
        );
    }

    @Test
    public void testShouldFail_IfReplayChecksFail_Aud() {
        testShouldFailGeneric(
                "sdjwt/s20.1-sdjwt+kb.txt",
                defaultKeyBindingJwtVerificationOpts()
                        .withAud("abcd") // kb's aud is "https://verifier.example.org"
                        .build(),
                "Key binding JWT: Unexpected `aud` value",
                null
        );
    }

    @Test
    public void testShouldFail_IfKbSdHashWrongFormat() {
        ObjectNode kbPayload = exampleKbPayload();

        // This hash is not a string
        kbPayload.set(OID4VCConstants.SD_HASH, mapper.valueToTree(1234));

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT: Claim `sd_hash` missing or not a string",
                null
        );
    }

    @Test
    public void testShouldFail_IfKbSdHashInvalid() {
        ObjectNode kbPayload = exampleKbPayload();

        // This hash makes no sense
        kbPayload.put(OID4VCConstants.SD_HASH, "c3FmZHFmZGZlZXNkZmZi");

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT: Invalid `sd_hash` digest",
                null
        );
    }

    @Test
    public void testShouldFail_IfKbIssuedInFuture() {
        long now = Instant.now().getEpochSecond();

        ObjectNode kbPayload = exampleKbPayload();
        kbPayload.set(CLAIM_NAME_IAT, mapper.valueToTree(now + 1000));

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT: Invalid `iat` claim",
                "JWT was issued in the future"
        );
    }

    @Test
    public void testShouldTolerateKbIssuedInTheFutureWithinClockSkew() throws VerificationException {
        long now = Instant.now().getEpochSecond();

        ObjectNode kbPayload = exampleKbPayload();
        // Issued just 5 seconds in the future. Should pass with a clock skew of 10 seconds.
        kbPayload.set(CLAIM_NAME_IAT, mapper.valueToTree(now + 5));
        SdJwtVP sdJwtVP = exampleSdJwtWithCustomKbPayload(kbPayload);

        sdJwtVP.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build(),
                defaultKeyBindingJwtVerificationOpts()
                        .withAllowedClockSkew(10)
                        .build()
        );
    }

    @Test
    public void testShouldFail_IfKbTooOld() {
        long issuerSignedJwtIat = 1683000000; // same value in test vector

        ObjectNode kbPayload = exampleKbPayload();
        // This KB-JWT is then issued more than 60s ago
        kbPayload.set(CLAIM_NAME_IAT, mapper.valueToTree(issuerSignedJwtIat - 120));

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts()
                        .withAllowedMaxAge(60)
                        .build(),
                "Key binding JWT is too old",
                null
        );
    }

    @Test
    public void testShouldFail_IfKbExpired() {
        long now = Instant.now().getEpochSecond();

        ObjectNode kbPayload = exampleKbPayload();
        kbPayload.set(CLAIM_NAME_EXP, mapper.valueToTree(now - 1000));

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT: Invalid `exp` claim",
                "JWT has expired"
        );
    }

    @Test
    public void testShouldTolerateExpiredKbWithinClockSkew() throws VerificationException {
        long now = Instant.now().getEpochSecond();

        ObjectNode kbPayload = exampleKbPayload();
        // Expires just 5 seconds ago. Should pass with a clock skew of 10 seconds.
        kbPayload.set(CLAIM_NAME_EXP, mapper.valueToTree(now - 5));
        SdJwtVP sdJwtVP = exampleSdJwtWithCustomKbPayload(kbPayload);

        sdJwtVP.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build(),
                defaultKeyBindingJwtVerificationOpts()
                        .withRequireExpirationClaim(true)
                        .withAllowedClockSkew(10)
                        .build()
        );
    }

    @Test
    public void testShouldFail_IfKbNotBeforeTimeYet() {
        long now = Instant.now().getEpochSecond();

        ObjectNode kbPayload = exampleKbPayload();
        kbPayload.set(CLAIM_NAME_NBF, mapper.valueToTree(now + 1000));

        testShouldFailGeneric2(
                kbPayload,
                defaultKeyBindingJwtVerificationOpts().build(),
                "Key binding JWT: Invalid `nbf` claim",
                "JWT is not yet valid"
        );
    }

    @Test
    public void testShouldFail_IfCnfNotJwk() {
        // The cnf claim is not of type jwk
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s20.8-sdjwt+kb--cnf-is-not-jwk.txt");
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> sdJwtVP.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build(),
                        defaultKeyBindingJwtVerificationOpts().build()
                )
        );

        assertEquals("Only cnf/jwk claim supported", exception.getMessage());
    }

    @Test
    public void testShouldFail_IfCnfJwkCantBeParsed() {
        testShouldFailGeneric(
                // The cnf/jwk object has an unrecognized key type
                "sdjwt/s20.8-sdjwt+kb--cnf-jwk-is-malformed.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "Could not process cnf/jwk",
                "Unsupported or invalid JWK"
        );
    }

    @Test
    public void testShouldFail_IfCnfJwkCantBeParsed2() {
        testShouldFailGeneric(
                // HMAC cnf/jwk parsing is not supported
                "sdjwt/s20.8-sdjwt+kb--cnf-hmac.txt",
                defaultKeyBindingJwtVerificationOpts().build(),
                "Could not process cnf/jwk",
                "Unsupported or invalid JWK"
        );
    }

    private void testShouldFailGeneric(
            String testFilePath,
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts,
            String exceptionMessage,
            String exceptionCauseMessage
    ) {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), testFilePath);
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtVPString);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwtVP.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build(),
                        keyBindingJwtVerificationOpts
                )
        );

        assertEquals(exceptionMessage, exception.getMessage());
        if (exceptionCauseMessage != null) {
            assertThat(exception.getCause().getMessage(), containsString(exceptionCauseMessage));
        }
    }

    /**
     * This test helper allows replacing the key binding JWT of base
     * sample `sdjwt/s20.1-sdjwt+kb.txt` to cover different scenarios.
     */
    private void testShouldFailGeneric2(
            JsonNode kbPayloadSubstitute,
            KeyBindingJwtVerificationOpts keyBindingJwtVerificationOpts,
            String exceptionMessage,
            String exceptionCauseMessage
    ) {
        SdJwtVP sdJwtVP = exampleSdJwtWithCustomKbPayload(kbPayloadSubstitute);

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> sdJwtVP.verify(
                        defaultIssuerVerifyingKeys(),
                        defaultIssuerSignedJwtVerificationOpts().build(),
                        keyBindingJwtVerificationOpts
                )
        );

        assertEquals(exceptionMessage, exception.getMessage());
        if (exceptionCauseMessage != null) {
            assertEquals(exceptionCauseMessage, exception.getCause().getMessage());
        }
    }

    private List<SignatureVerifierContext> defaultIssuerVerifyingKeys() {
        return Collections.singletonList(testSettings.issuerVerifierContext);
    }

    private IssuerSignedJwtVerificationOpts.Builder defaultIssuerSignedJwtVerificationOpts() {
        return IssuerSignedJwtVerificationOpts.builder()
                .withRequireIssuedAtClaim(false)
                .withRequireNotBeforeClaim(false);
    }

    private KeyBindingJwtVerificationOpts.Builder defaultKeyBindingJwtVerificationOpts() {
        return KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withAllowedMaxAge(Integer.MAX_VALUE)
                .withNonce("1234567890")
                .withAud("https://verifier.example.org")
                .withRequireExpirationClaim(false)
                .withRequireNotBeforeClaim(false);
    }

    private ObjectNode exampleKbPayload() {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("nonce", "1234567890");
        payload.put("aud", "https://verifier.example.org");
        payload.put(OID4VCConstants.SD_HASH, "X9RrrfWt_70gHzOcovGSIt4Fms9Tf2g2hjlWVI_cxZg");
        payload.set(CLAIM_NAME_IAT, mapper.valueToTree(1702315679));

        return payload;
    }

    private SdJwtVP exampleSdJwtWithCustomKbPayload(JsonNode kbPayloadSubstitute) {
        KeyBindingJWT keyBindingJWT = KeyBindingJWT.from(
                kbPayloadSubstitute,
                testSettings.holderSigContext,
                KeyBindingJWT.TYP
        );

        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s20.1-sdjwt+kb.txt");
        String sdJwtWithoutKb = sdJwtVPString.substring(0, sdJwtVPString.lastIndexOf(OID4VCConstants.SDJWT_DELIMITER) + 1);

        return SdJwtVP.of(sdJwtWithoutKb + keyBindingJWT.toJws());
    }
}
