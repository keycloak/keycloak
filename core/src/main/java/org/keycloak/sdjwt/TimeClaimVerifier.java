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

import java.time.Instant;

import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NBF;

/**
 * Module for checking the validity of JWT time claims.
 * All checks account for a small window to accommodate allowed clock skew.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerifier {

    private final TimeClaimVerificationOpts opts;

    public TimeClaimVerifier(TimeClaimVerificationOpts opts) {
        if (opts.getAllowedClockSkewSeconds() < 0) {
            throw new IllegalArgumentException("Allowed clock skew seconds cannot be negative");
        }

        this.opts = opts;
    }

    /**
     * Validates that JWT was not issued in the future
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyIssuedAtClaim(JsonNode jwtPayload) throws VerificationException {
        if (!jwtPayload.hasNonNull(CLAIM_NAME_IAT)) {
            if (opts.mustRequireIssuedAtClaim()) {
                throw new VerificationException("Missing 'iat' claim or null");
            }

            return; // Not required, skipping check
        }

        long iat = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_IAT);

        if ((currentTimestamp() + opts.getAllowedClockSkewSeconds()) < iat) {
            throw new VerificationException("JWT was issued in the future");
        }
    }

    /**
     * Validates that JWT has not expired
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyExpirationClaim(JsonNode jwtPayload) throws VerificationException {
        if (!jwtPayload.hasNonNull(CLAIM_NAME_EXP)) {
            if (opts.mustRequireExpirationClaim()) {
                throw new VerificationException("Missing 'exp' claim or null");
            }

            return; // Not required, skipping check
        }

        long exp = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_EXP);

        if ((currentTimestamp() - opts.getAllowedClockSkewSeconds()) >= exp) {
            throw new VerificationException("JWT has expired");
        }
    }

    /**
     * Validates that JWT can yet be processed
     *
     * @param jwtPayload the JWT's payload
     */
    public void verifyNotBeforeClaim(JsonNode jwtPayload) throws VerificationException {
        if (!jwtPayload.hasNonNull(CLAIM_NAME_NBF)) {
            if (opts.mustRequireNotBeforeClaim()) {
                throw new VerificationException("Missing 'nbf' claim or null");
            }

            return; // Not required, skipping check
        }

        long nbf = SdJwtUtils.readTimeClaim(jwtPayload, CLAIM_NAME_NBF);

        if ((currentTimestamp() + opts.getAllowedClockSkewSeconds()) < nbf) {
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

        if ((currentTimestamp() - iat - opts.getAllowedClockSkewSeconds()) > maxAge) {
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
