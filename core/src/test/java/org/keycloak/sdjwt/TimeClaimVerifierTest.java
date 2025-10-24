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
import static org.keycloak.sdjwt.TimeClaimVerifier.CLAIM_NAME_EXP;
import static org.keycloak.sdjwt.TimeClaimVerifier.CLAIM_NAME_IAT;
import static org.keycloak.sdjwt.TimeClaimVerifier.CLAIM_NAME_NBF;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerifierTest {

    private final TimeClaimVerifier timeClaimVerifier = new FixedTimeClaimVerifier(20); // 20 seconds of leeway

    private static final long CURRENT_TIMESTAMP = 1609459200L; // Fixed timestamp: 2021-01-01 00:00:00 UTC

    static class FixedTimeClaimVerifier extends TimeClaimVerifier {

        public FixedTimeClaimVerifier(int leewaySeconds) {
            super(leewaySeconds);
        }

        @Override
        public long currentTimestamp() {
            return CURRENT_TIMESTAMP;
        }
    }

    @Test
    public void testVerifyIatClaimInTheFuture() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP + 100); // 100 seconds in the future

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyIssuedAtClaim(payload));

        assertEquals("JWT was issued in the future", exception.getMessage());
    }

    @Test
    public void testVerifyIatClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 1); // Issued 1 second ago, in the past

        timeClaimVerifier.verifyIssuedAtClaim(payload);
    }

    @Test
    public void testVerifyIatClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP + 19); // Issued 19 seconds in the future, within the 20 second leeway

        timeClaimVerifier.verifyIssuedAtClaim(payload);
    }

    @Test
    public void testVerifyExpClaimExpired() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP - 100); // Expired 100 seconds ago

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyExpirationClaim(payload));

        assertEquals("JWT has expired", exception.getMessage());
    }

    @Test
    public void testVerifyExpClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP + 100); // Expires 100 seconds in the future

        timeClaimVerifier.verifyExpirationClaim(payload);
    }

    @Test
    public void testVerifyExpClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_EXP, CURRENT_TIMESTAMP - 19); // 19 seconds ago, within the 20 second leeway

        // No exception expected for JWT expiring within leeway
        timeClaimVerifier.verifyExpirationClaim(payload);
    }

    @Test
    public void testVerifyNotBeforeClaimNotYetValid() {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP + 100); // Not valid for another 100 seconds

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyNotBeforeClaim(payload));

        assertEquals("JWT is not yet valid", exception.getMessage());
    }

    @Test
    public void testVerifyNotBeforeClaimValid() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP - 100); // Valid since 100 seconds ago

        timeClaimVerifier.verifyNotBeforeClaim(payload);
    }

    // Test for verifyNotBeforeClaim (edge case: valid exactly at current time with leeway)
    @Test
    public void testVerifyNotBeforeClaimEdge() throws VerificationException {
        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_NBF, CURRENT_TIMESTAMP + 19); // 19 seconds in the future, within the 20 second leeway

        // No exception expected for JWT becoming valid within leeway
        timeClaimVerifier.verifyNotBeforeClaim(payload);
    }

    @Test
    public void testVerifyAgeJwtTooOld() {
        int maxAgeAllowed = 300; // 5 minutes

        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 361); // 361 seconds old

        VerificationException exception = assertThrows(VerificationException.class,
                () -> timeClaimVerifier.verifyAge(payload, maxAgeAllowed));

        assertEquals("JWT is too old", exception.getMessage());
    }

    @Test
    public void testVerifyAgeValid() throws VerificationException {
        int maxAgeAllowed = 300; // 5 minutes

        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 100); // Only 100 seconds old

        timeClaimVerifier.verifyAge(payload, maxAgeAllowed);
    }

    @Test
    public void testVerifyAgeValidEdge() throws VerificationException {
        int maxAgeAllowed = 300; // 5 minutes

        ObjectNode payload = SdJwtUtils.mapper.createObjectNode();
        payload.put(CLAIM_NAME_IAT, CURRENT_TIMESTAMP - 320); // 320 seconds old, within the 20 second leeway

        timeClaimVerifier.verifyAge(payload, maxAgeAllowed);
    }

    @Test
    public void instantiationShouldFailIfLeewayNegative() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TimeClaimVerifier(-1));

        assertEquals("Leeway seconds cannot be negative", exception.getMessage());
    }
}
