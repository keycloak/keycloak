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
public class IssuerSignedJwtVerificationOpts extends TimeClaimVerificationOpts {

    private IssuerSignedJwtVerificationOpts(
            boolean validateIssuedAtClaim,
            boolean validateExpirationClaim,
            boolean validateNotBeforeClaim,
            int allowedClockSkewSeconds
    ) {
        super(validateIssuedAtClaim, validateExpirationClaim, validateNotBeforeClaim, allowedClockSkewSeconds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends TimeClaimVerificationOpts.Builder<Builder> {

        @Override
        public IssuerSignedJwtVerificationOpts build() {
            return new IssuerSignedJwtVerificationOpts(
                    requireIssuedAtClaim,
                    requireExpirationClaim,
                    requireNotBeforeClaim,
                    allowedClockSkewSeconds
            );
        }
    }
}
