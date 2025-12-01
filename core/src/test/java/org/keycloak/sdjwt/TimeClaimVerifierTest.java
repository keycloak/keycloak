/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.function.Function;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NBF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerifierTest {

    private static final int TIMESTAMP2021 = 1609459200; // Fixed timestamp: 2021-01-01 00:00:00 UTC
    private int CURRENT_TIMESTAMP;
    private static final int DEFAULT_CLOCK_SKEW_SECONDS = 20;

    private final ClaimVerifier timeClaimVerifier = new FixedTimeClaimVerifier(DEFAULT_CLOCK_SKEW_SECONDS, false);

    static class FixedTimeClaimVerifier extends ClaimVerifier {

        public FixedTimeClaimVerifier(int clockSkew, boolean requireClaims) {
            super(new ArrayList<>(),
                  createOptsWithClockSkew(clockSkew, requireClaims)
                      .getContentVerifiers());
        }
    }

    @Before
    public void updateTimeOffset() {
        int currentTime = Time.currentTime();
        Time.setOffset(TIMESTAMP2021 - currentTime); // Move time to 2021-01-01 00:00:00 UTC
        this.CURRENT_TIMESTAMP = Time.currentTime();
    }

    @After
    public void revertTimeOffset() {
        Time.setOffset(0);
    }

    @Test
    public void testVerifyIatClaimInTheFuture() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP + 100); // 100 seconds in the future

        VerificationException exception = assertThrows(VerificationException.class,
                                                       () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                   exception.getMessage().matches("Token was issued in the future: now: '\\d+', iat: '\\d+'"));
    }

    @Test
    public void testVerifyIatClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 5); // Issued 5 seconds ago, in the past

        timeClaimVerifier.verifyBodyClaims(payload);
    }

    @Test
    public void testVerifyIatClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT,
                    CURRENT_TIMESTAMP + 15); // Issued 15 seconds in the future, within the 20 second clock skew

        timeClaimVerifier.verifyBodyClaims(payload);

        payload.put(CLAIM_NAME_IAT,
                CURRENT_TIMESTAMP + 25); // Issued 25 seconds in the future, which is not within the 20 second clock skew

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                exception.getMessage().matches("Token was issued in the future: now: '\\d+', iat: '\\d+'"));
    }

    @Test
    public void testVerifyExpClaimExpired() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP - 100); // Expired 100 seconds ago

        VerificationException exception = assertThrows(VerificationException.class,
                                                       () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                   exception.getMessage().matches("Token has expired by exp: now: '\\d+', exp: '\\d+'"));
    }

    @Test
    public void testVerifyExpClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP + 100); // Expires 100 seconds in the future

        timeClaimVerifier.verifyBodyClaims(payload);
    }

    @Test
    public void testVerifyExpClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP - 15); // 15 seconds ago, within the 20 second clock skew

        // No exception expected for JWT expiring within clock skew
        timeClaimVerifier.verifyBodyClaims(payload);
    }

    @Test
    public void testVerifyNotBeforeClaimNotYetValid() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP + 100); // Not valid for another 100 seconds

        VerificationException exception = assertThrows(VerificationException.class,
                                                       () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                   exception.getMessage().matches("Token is not yet valid: now: '\\d+', nbf: '\\d+'"));
    }

    @Test
    public void testVerifyNotBeforeClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP - 100); // Valid since 100 seconds ago

        timeClaimVerifier.verifyBodyClaims(payload);
    }

    // Test for verifyNotBeforeClaim (edge case: valid exactly at current time with clock skew)
    @Test
    public void testVerifyNotBeforeClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP + 15); // 15 seconds in the future, within the 20 second clock skew

        // No exception expected for JWT becoming valid within clock skew
        timeClaimVerifier.verifyBodyClaims(payload);

        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP + 25); // 25 seconds in the future, not anymore within the 20 second clock skew

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                exception.getMessage().matches("Token is not yet valid: now: '\\d+', nbf: '\\d+'"));
    }

    @Test
    public void testVerifyAgeJwtTooOld() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 365); // 365 seconds old

        VerificationException exception = assertThrows(VerificationException.class,
                                                       () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                   exception.getMessage().matches("Token has expired by iat: now: '\\d+', expired at:"
                                                                    + " '\\d+', iat: '\\d+', maxLifetime: '300'"));
    }

    @Test
    public void testVerifyAgeValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 100); // Only 100 seconds old

        timeClaimVerifier.verifyBodyClaims(payload);
    }

    @Test
    public void testVerifyAgeValidEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 315); // 315 seconds old, within the 20 second clock skew

        timeClaimVerifier.verifyBodyClaims(payload);

        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 325); // 325 seconds old. not anymore within valid clock skew

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyBodyClaims(payload));

        assertTrue(String.format("Expected message '%s' does not match regex", exception.getMessage()),
                exception.getMessage().matches("Token has expired by iat: now: '\\d+', expired at:"
                        + " '\\d+', iat: '\\d+', maxLifetime: '300'"));
    }

    @Test
    public void testUseClockSkewZeroIfSetToNegative() {
        ClaimVerifier claimVerifier = createOptsWithClockSkew(-1, false);
        claimVerifier.getContentVerifiers()
                     .stream()
                     .filter(verifier -> verifier instanceof ClaimVerifier.TimeCheck)
                     .forEach(verifier -> {
                         assertEquals(0, ((ClaimVerifier.TimeCheck) verifier).getClockSkewSeconds());
                     });
    }

    @Test
    public void testPermissiveVerifierMissingClaims() throws VerificationException {
        // No time claims added
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();

        // No exception expected as claims are not required
        timeClaimVerifier.verifyBodyClaims(payload);
    }

    @Test
    public void testStrictVerifierMissingClaims() {
        // No time claims added
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();

        Function<ClaimVerifier, VerificationException> strictVerifier = verifier -> {
            try{
                verifier.verifyBodyClaims(payload);
                fail("Verification should have failed");
                return null;
            } catch(VerificationException e){
                return e;
            }
        };

        VerificationException exceptionIat = strictVerifier.apply(ClaimVerifier.builder()
                                                                               .withIatCheck(false)
                                                                               .withNbfCheck(true)
                                                                               .withExpCheck(true)
                                                                               .build());
        assertEquals("Missing required claim 'iat'", exceptionIat.getMessage());

        VerificationException exceptionExp = strictVerifier.apply(ClaimVerifier.builder()
                                                                               .withExpCheck(false)
                                                                               .withIatCheck(true)
                                                                               .withNbfCheck(true)
                                                                               .build());
        assertEquals("Missing required claim 'exp'", exceptionExp.getMessage());

        VerificationException exceptionNbf = strictVerifier.apply(ClaimVerifier.builder()
                                                                               .withNbfCheck(false)
                                                                               .withIatCheck(true)
                                                                               .withExpCheck(true)
                                                                               .build());
        assertEquals("Missing required claim 'nbf'", exceptionNbf.getMessage());
    }

    private static ClaimVerifier createOptsWithClockSkew(int clockSkew, boolean requireClaims) {
        final int defaultMaxLifeTime = 300;
        final boolean isOptionalCheck = !requireClaims;

        return ClaimVerifier.builder()
                            .withClockSkew(clockSkew)
                            .withIatCheck(defaultMaxLifeTime, isOptionalCheck)
                            .withExpCheck(isOptionalCheck)
                            .withNbfCheck(isOptionalCheck)
                            .build();
    }
}
