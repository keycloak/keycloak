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

/**
 * Options for Issuer-signed JWT verification.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class IssuerSignedJwtVerificationOpts {
    private final boolean validateIssuedAtClaim;
    private final boolean validateExpirationClaim;
    private final boolean validateNotBeforeClaim;

    public IssuerSignedJwtVerificationOpts(
            boolean validateIssuedAtClaim,
            boolean validateExpirationClaim,
            boolean validateNotBeforeClaim) {
        this.validateIssuedAtClaim = validateIssuedAtClaim;
        this.validateExpirationClaim = validateExpirationClaim;
        this.validateNotBeforeClaim = validateNotBeforeClaim;
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

    public static IssuerSignedJwtVerificationOpts.Builder builder() {
        return new IssuerSignedJwtVerificationOpts.Builder();
    }

    public static class Builder {
        private boolean validateIssuedAtClaim;
        private boolean validateExpirationClaim = true;
        private boolean validateNotBeforeClaim = true;

        public Builder withValidateIssuedAtClaim(boolean validateIssuedAtClaim) {
            this.validateIssuedAtClaim = validateIssuedAtClaim;
            return this;
        }

        public Builder withValidateExpirationClaim(boolean validateExpirationClaim) {
            this.validateExpirationClaim = validateExpirationClaim;
            return this;
        }

        public Builder withValidateNotBeforeClaim(boolean validateNotBeforeClaim) {
            this.validateNotBeforeClaim = validateNotBeforeClaim;
            return this;
        }

        public IssuerSignedJwtVerificationOpts build() {
            return new IssuerSignedJwtVerificationOpts(
                    validateIssuedAtClaim,
                    validateExpirationClaim,
                    validateNotBeforeClaim
            );
        }
    }
}
