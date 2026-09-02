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
import java.time.temporal.ChronoUnit;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.rule.CryptoInitRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public abstract class SdJwsTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    static TestSettings testSettings = TestSettings.getInstance();

    private ObjectNode createPayload() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("sub", "test");
        node.put("exp", Instant.ofEpochSecond(Time.currentTime()).plus(1, ChronoUnit.HOURS).getEpochSecond());
        node.put("name", "Test User");
        return node;
    }

    @Test
    public void testVerifySignature_Positive() throws Exception {
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        JwsToken sdJws = new JwsToken(jwsHeader, createPayload(), testSettings.holderSigContext) {
        };
        sdJws.verifySignature(testSettings.holderVerifierContext);
    }

    @Test
    public void testVerifySignature_WrongPublicKey() {
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        JwsToken sdJws = new JwsToken(jwsHeader, createPayload(), testSettings.holderSigContext) {
        };
        assertThrows(VerificationException.class, () -> sdJws.verifySignature(testSettings.issuerVerifierContext));
    }

    @Test
    public void testVerifyExpClaim_ExpiredJWT() {
        ObjectNode payload = createPayload();
        payload.put("exp", Instant.ofEpochSecond(Time.currentTime()).minus(1, ChronoUnit.HOURS).getEpochSecond());
        assertThrows(VerificationException.class, () -> {
            new ClaimVerifier.ExpCheck(0, false).test(payload);
        });
    }

    @Test
    public void testVerifyExpClaim_Positive() throws Exception {
        ObjectNode payload = createPayload();
        payload.put("exp", Instant.ofEpochSecond(Time.currentTime()).plus(1, ChronoUnit.HOURS).getEpochSecond());

        new ClaimVerifier.ExpCheck(0, false).test(payload);
    }

    @Test
    public void testVerifyNotBeforeClaim_Negative() {
        ObjectNode payload = createPayload();
        payload.put("nbf", Instant.ofEpochSecond(Time.currentTime()).plus(1, ChronoUnit.HOURS).getEpochSecond());
        assertThrows(VerificationException.class, () -> {
            new ClaimVerifier.NbfCheck(0, false).test(payload);
        });
    }

    @Test
    public void testVerifyNotBeforeClaim_Positive() throws Exception {
        ObjectNode payload = createPayload();
        payload.put("nbf", Instant.ofEpochSecond(Time.currentTime()).minus(1, ChronoUnit.HOURS).getEpochSecond());

        new ClaimVerifier.NbfCheck(0, false).test(payload);
    }

    @Test
    public void testPayloadJwsConstruction() {
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        JwsToken sdJws = new JwsToken(jwsHeader, createPayload()) {
        };
        assertNotNull(sdJws.getJwsHeader());
        assertNotNull(sdJws.getPayload());
    }

    @Test
    public void testSignedJwsConstruction() {
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        JwsToken sdJws = new JwsToken(jwsHeader, createPayload(), testSettings.holderSigContext) {
        };

        assertNotNull(sdJws.getJws());
    }

    @Test
    public void testVerifyIssClaim_Negative() {
        String allowedIssuer = "issuer1@sdjwt.com";
        ObjectNode payload = createPayload();
        String invalidIssuer = "unknown-issuer@sdjwt.com";
        payload.put("iss", invalidIssuer);
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            new ClaimVerifier.ClaimCheck("iss", allowedIssuer).test(payload);
        });
        assertEquals(String.format("Expected value '%s' in token for claim 'iss' does not match actual value '%s'",
                                   allowedIssuer,
                                   invalidIssuer), exception.getMessage());
    }

    @Test
    public void testVerifyIssClaim_Positive() throws VerificationException {
        String allowedIssuer = "issuer1@sdjwt.com";
        ObjectNode payload = createPayload();
        payload.put("iss", "issuer1@sdjwt.com");
        new ClaimVerifier.ClaimCheck("iss", allowedIssuer).test(payload);
    }

    @Test
    public void testVerifyVctClaim_Negative() {
        ObjectNode payload = createPayload();

        final String claimName = "vct";
        final String actualValue = "IdentityCredential";
        payload.put(claimName, actualValue);

        final String expectedClaimValue = "PassportCredential";
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            new ClaimVerifier.ClaimCheck(claimName, expectedClaimValue).test(payload);
        });

        assertEquals(String.format("Expected value '%s' in token for claim '%s' does not match actual value '%s'",
                                   expectedClaimValue,
                                   claimName,
                                   actualValue), exception.getMessage());
    }

    @Test
    public void testVerifyVctClaim_Positive() throws VerificationException {
        ObjectNode payload = createPayload();

        final String claimName = "vct";
        final String expectedClaimValue = "IdentityCredential";
        payload.put(claimName, expectedClaimValue);

        new ClaimVerifier.ClaimCheck(claimName, expectedClaimValue).test(payload);
    }

    @Test
    public void shouldValidateAgeSinceIssued() throws VerificationException {
        long now = Time.currentTime();
        JwsToken sdJws = exampleSdJws(now);

        new ClaimVerifier.IatLifetimeCheck(0, 180).test(sdJws.getPayload());
    }

    @Test
    public void shouldValidateAgeSinceIssued_IfJwtIsTooOld() {
        long now = Time.currentTime();
        long iat = now - 1000;
        long maxLifetime = 180;
        JwsToken sdJws = exampleSdJws(iat); // that will be too old
        VerificationException exception = assertThrows(VerificationException.class, () -> {
            new ClaimVerifier.IatLifetimeCheck(0, maxLifetime).test(sdJws.getPayload());
        });

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                exception.getMessage().matches("Token has expired by iat: now: '\\d+', expired at:"
                        + " '\\d+', iat: '\\d+', maxLifetime: '180'"));
    }

    private JwsToken exampleSdJws(long iat) {
        ObjectNode payload = new ObjectNode(JsonNodeFactory.instance);
        payload.set("iat", new LongNode(iat));

        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setType("jwt");
        return new JwsToken(jwsHeader, payload) {
        };
    }
}
