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

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.common.VerificationException;

import java.time.Instant;

/**
 * Module for checking the validity of JWT time claims.
 * All checks account for a leeway window to accommodate clock skew.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerifier {

    public static final String CLAIM_NAME_IAT = "iat";
    public static final String CLAIM_NAME_EXP = "exp";
    public static final String CLAIM_NAME_NBF = "nbf";

    /**
     * Tolerance window to account for clock skew
     */
    private final int leewaySeconds;

    public TimeClaimVerifier(int leewaySeconds) {
        if (leewaySeconds < 0) {
            throw new IllegalArgumentException("Leeway seconds cannot be negative");
        }

        this.leewaySeconds = leewaySeconds;
    }

    /**
     * Validates that JWT was not issued in the future
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyIssuedAtClaim(JsonNode jwtPayload) throws VerificationException {
        long iat = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_IAT);

        if ((currentTimestamp() + leewaySeconds) < iat) {
            throw new VerificationException("JWT was issued in the future");
        }
    }

    /**
     * Validates that JWT has not expired
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyExpirationClaim(JsonNode jwtPayload) throws VerificationException {
        long exp = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_EXP);

        if ((currentTimestamp() - leewaySeconds) >= exp) {
            throw new VerificationException("JWT has expired");
        }
    }

    /**
     * Validates that JWT can yet be processed
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyNotBeforeClaim(JsonNode jwtPayload) throws VerificationException {
        long nbf = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_NBF);

        if ((currentTimestamp() + leewaySeconds) < nbf) {
            throw new VerificationException("JWT is not yet valid");
        }
    }

    /**
     * Validates that JWT is not too old
     *
     * @param jwtPayload the JWT's payload
     * @param maxAge     maximum allowed age in seconds
     */
    public void verifyAge(JsonNode jwtPayload, int maxAge) throws VerificationException {
        long iat = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_IAT);

        if ((currentTimestamp() - iat - leewaySeconds) > maxAge) {
            throw new VerificationException("JWT is too old");
        }
    }

    /**
     * Returns current timestamp in seconds.
     */
    public long currentTimestamp() {
        return Instant.now().getEpochSecond();
    }
}
