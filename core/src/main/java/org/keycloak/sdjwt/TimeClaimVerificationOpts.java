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

/**
 * Options for validating common time claims during SD-JWT verification.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class TimeClaimVerificationOpts {

    /**
     * Tolerance window to account for clock skew when checking time claims
     */
    public static final int DEFAULT_LEEWAY_SECONDS = 10;

    private final boolean validateIssuedAtClaim;
    private final boolean validateExpirationClaim;
    private final boolean validateNotBeforeClaim;
    private final int leewaySeconds;

    protected TimeClaimVerificationOpts(
            boolean validateIssuedAtClaim,
            boolean validateExpirationClaim,
            boolean validateNotBeforeClaim,
            int leewaySeconds
    ) {
        this.validateIssuedAtClaim = validateIssuedAtClaim;
        this.validateExpirationClaim = validateExpirationClaim;
        this.validateNotBeforeClaim = validateNotBeforeClaim;
        this.leewaySeconds = leewaySeconds;
    }

    public boolean mustValidateIssuedAtClaim() {
        return validateIssuedAtClaim;
    }

    public boolean mustValidateExpirationClaim() {
        return validateExpirationClaim;
    }

    public boolean mustValidateNotBeforeClaim() {
        return validateNotBeforeClaim;
    }

    public int getLeewaySeconds() {
        return leewaySeconds;
    }

    protected static class Builder<T extends Builder<T>> {

        protected boolean validateIssuedAtClaim = false;
        protected boolean validateExpirationClaim = true;
        protected boolean validateNotBeforeClaim = true;
        protected int leewaySeconds = DEFAULT_LEEWAY_SECONDS;

        @SuppressWarnings("unchecked")
        public T withValidateIssuedAtClaim(boolean validateIssuedAtClaim) {
            this.validateIssuedAtClaim = validateIssuedAtClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withValidateExpirationClaim(boolean validateExpirationClaim) {
            this.validateExpirationClaim = validateExpirationClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withValidateNotBeforeClaim(boolean validateNotBeforeClaim) {
            this.validateNotBeforeClaim = validateNotBeforeClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withLeewaySeconds(int leewaySeconds) {
            this.leewaySeconds = leewaySeconds;
            return (T) this;
        }
    }
}
