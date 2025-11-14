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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.keycloak.common.VerificationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerifierTest {

    private static final long CURRENT_TIMESTAMP = 1609459200L; // Fixed timestamp: 2021-01-01 00:00:00 UTC
    private static final int DEFAULT_CLOCK_SKEW_SECONDS = 20;

    private final TimeClaimVerifier timeClaimVerifier = new FixedTimeClaimVerifier(DEFAULT_CLOCK_SKEW_SECONDS, false);
    private final TimeClaimVerifier strictTimeClaimVerifier = new FixedTimeClaimVerifier(DEFAULT_CLOCK_SKEW_SECONDS, true);

    static class FixedTimeClaimVerifier extends TimeClaimVerifier {

        public FixedTimeClaimVerifier(int leewaySeconds, boolean requireClaims) {
            super(createOptsWithAllowedClockSkew(leewaySeconds, requireClaims));
        }

        @Override
        public long currentTimestamp() {
            return CURRENT_TIMESTAMP;
        }
    }

    @Test
    public void testVerifyIatClaimInTheFuture() {
        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyIssuedAtClaim(CURRENT_TIMESTAMP + 100)); // 100 seconds in the future

        assertEquals("JWT was issued in the future", exception.getMessage());
    }

    @Test
    public void testVerifyIatClaimValid() throws VerificationException {
        timeClaimVerifier.verifyIssuedAtClaim(CURRENT_TIMESTAMP - 1); // Issued 1 second ago, in the past
    }

    @Test
    public void testVerifyIatClaimEdge() throws VerificationException {
        timeClaimVerifier.verifyIssuedAtClaim(CURRENT_TIMESTAMP + 19); // Issued 19 seconds in the future, within the 20 second leeway);
    }

    @Test
    public void testVerifyExpClaimExpired() {
        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyExpirationClaim(CURRENT_TIMESTAMP - 100)); // Expired 100 seconds ago

        assertEquals("JWT has expired", exception.getMessage());
    }

    @Test
    public void testVerifyExpClaimValid() throws VerificationException {
        timeClaimVerifier.verifyExpirationClaim(CURRENT_TIMESTAMP + 100); // Expires 100 seconds in the future
    }

    @Test
    public void testVerifyExpClaimEdge() throws VerificationException {
        // No exception expected for JWT expiring within clock skew
        timeClaimVerifier.verifyExpirationClaim(CURRENT_TIMESTAMP - 19); // 19 seconds ago, within the 20 second leeway
    }

    @Test
    public void testVerifyNotBeforeClaimNotYetValid() {
        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyNotBeforeClaim(CURRENT_TIMESTAMP + 100)); // Not valid for another 100 seconds

        assertEquals("JWT is not yet valid", exception.getMessage());
    }

    @Test
    public void testVerifyNotBeforeClaimValid() throws VerificationException {
        timeClaimVerifier.verifyNotBeforeClaim(CURRENT_TIMESTAMP - 100); // Valid since 100 seconds ago
    }

    // Test for verifyNotBeforeClaim (edge case: valid exactly at current time with leeway)
    @Test
    public void testVerifyNotBeforeClaimEdge() throws VerificationException {
        // No exception expected for JWT becoming valid within leeway
        timeClaimVerifier.verifyNotBeforeClaim(CURRENT_TIMESTAMP + 19); // 19 seconds in the future, within the 20 second leeway
    }

    @Test
    public void testVerifyAgeJwtTooOld() {
        int maxAgeAllowed = 300; // 5 minutes

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyAge(CURRENT_TIMESTAMP - 361, maxAgeAllowed)); // 361 seconds old

        assertEquals("JWT is too old", exception.getMessage());
    }

    @Test
    public void testVerifyAgeValid() throws VerificationException {
        int maxAgeAllowed = 300; // 5 minutes

        timeClaimVerifier.verifyAge(CURRENT_TIMESTAMP - 100, maxAgeAllowed); // Only 100 seconds old
    }

    @Test
    public void testVerifyAgeValidEdge() throws VerificationException {
        int maxAgeAllowed = 300; // 5 minutes

        timeClaimVerifier.verifyAge(CURRENT_TIMESTAMP - 320, maxAgeAllowed); // 320 seconds old, within the 20 second leeway
    }

    @Test
    public void instantiationShouldFailIfClockSkewNegative() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TimeClaimVerifier(createOptsWithAllowedClockSkew(-1, false)));

        assertEquals("Allowed clock skew seconds cannot be negative", exception.getMessage());
    }

    @Test
    public void testPermissiveVerifierMissingClaims() throws VerificationException {
        // No time claims added
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();

        // No exception expected as claims are not required
        timeClaimVerifier.verifyIssuedAtClaim(null);
        timeClaimVerifier.verifyExpirationClaim(null);
        timeClaimVerifier.verifyNotBeforeClaim(null);
    }

    @Test
    public void testStrictVerifierMissingClaims() {
        // No time claims added
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();

        VerificationException exceptionIat = assertThrows(VerificationException.class,
                () -> strictTimeClaimVerifier.verifyIssuedAtClaim(null));
        assertEquals("Missing 'iat' claim", exceptionIat.getMessage());

        VerificationException exceptionExp = assertThrows(VerificationException.class,
                () -> strictTimeClaimVerifier.verifyExpirationClaim(null));
        assertEquals("Missing 'exp' claim", exceptionExp.getMessage());

        VerificationException exceptionNbf = assertThrows(VerificationException.class,
                () -> strictTimeClaimVerifier.verifyNotBeforeClaim(null));
        assertEquals("Missing 'nbf' claim", exceptionNbf.getMessage());
    }

    private static TimeClaimVerificationOpts createOptsWithAllowedClockSkew(int allowedClockSkewSeconds, boolean requireClaims) {
        return TimeClaimVerificationOpts.builder()
                .withAllowedClockSkew(allowedClockSkewSeconds)
                .withRequireIssuedAtClaim(requireClaims)
                .withRequireExpirationClaim(requireClaims)
                .withRequireNotBeforeClaim(requireClaims)
                .build();
    }
}
