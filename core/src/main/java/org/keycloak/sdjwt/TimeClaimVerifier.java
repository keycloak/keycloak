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

import org.keycloak.common.VerificationException;

import java.time.Instant;

/**
 * Module for checking the validity of JWT time claims.
 * All checks account for a leeway window to accommodate clock skew.
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
     * @param iat Issued-at value. Could be null
     */
    public void verifyIssuedAtClaim(Long iat) throws VerificationException {
        if (iat == null) {
            if (opts.mustRequireIssuedAtClaim()) {
                throw new VerificationException("Missing 'iat' claim");
            }

            return; // Not required, skipping check
        }

        if ((currentTimestamp() + opts.getAllowedClockSkewSeconds()) < iat) {
            throw new VerificationException("JWT was issued in the future");
        }
    }

    /**
     * Validates that JWT has not expired
     *
     * @param exp When the JWT is going to be expired. Could be null
     */
    public void verifyExpirationClaim(Long exp) throws VerificationException {
        if (exp == null) {
            if (opts.mustRequireExpirationClaim()) {
                throw new VerificationException("Missing 'exp' claim");
            }

            return; // Not required, skipping check
        }

        if ((currentTimestamp() - opts.getAllowedClockSkewSeconds()) >= exp) {
            throw new VerificationException("JWT has expired");
        }
    }

    /**
     * Validates that JWT can yet be processed
     *
     * @param nbf When the JWT is going to be expired. Could be null
     */
    public void verifyNotBeforeClaim(Long nbf) throws VerificationException {
        if (nbf == null) {
            if (opts.mustRequireNotBeforeClaim()) {
                throw new VerificationException("Missing 'nbf' claim");
            }

            return; // Not required, skipping check
        }

        if ((currentTimestamp() + opts.getAllowedClockSkewSeconds()) < nbf) {
            throw new VerificationException("JWT is not yet valid");
        }
    }

    /**
     * Validates that JWT is not too old
     *
     * @param iat When JWT was issued. Cannot be null
     * @param maxAge     maximum allowed age in seconds
     */
    public void verifyAge(Long iat, int maxAge) throws VerificationException {
        if (iat == null) {
            throw new VerificationException("Missing 'iat' claim");
        }

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
