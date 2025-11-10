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

    public static final int DEFAULT_LEEWAY_SECONDS = 10;

    // These options configure whether the respective time claims must be present
    // during validation. They will always be validated if present.

    private final boolean requireIssuedAtClaim;
    private final boolean requireExpirationClaim;
    private final boolean requireNotBeforeClaim;

    /**
     * Tolerance window to account for clock skew when checking time claims
     */
    private final int leewaySeconds;

    protected TimeClaimVerificationOpts(
            boolean requireIssuedAtClaim,
            boolean requireExpirationClaim,
            boolean validateNotBeforeClaim,
            int leewaySeconds
    ) {
        this.requireIssuedAtClaim = requireIssuedAtClaim;
        this.requireExpirationClaim = requireExpirationClaim;
        this.requireNotBeforeClaim = validateNotBeforeClaim;
        this.leewaySeconds = leewaySeconds;
    }

    public boolean mustRequireIssuedAtClaim() {
        return requireIssuedAtClaim;
    }

    public boolean mustRequireExpirationClaim() {
        return requireExpirationClaim;
    }

    public boolean mustRequireNotBeforeClaim() {
        return requireNotBeforeClaim;
    }

    public int getLeewaySeconds() {
        return leewaySeconds;
    }

    public static <T extends Builder<T>> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<T>> {

        protected boolean requireIssuedAtClaim = true;
        protected boolean requireExpirationClaim = true;
        protected boolean requireNotBeforeClaim = true;
        protected int leewaySeconds = DEFAULT_LEEWAY_SECONDS;

        @SuppressWarnings("unchecked")
        public T withRequireIssuedAtClaim(boolean requireIssuedAtClaim) {
            this.requireIssuedAtClaim = requireIssuedAtClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withRequireExpirationClaim(boolean requireExpirationClaim) {
            this.requireExpirationClaim = requireExpirationClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withRequireNotBeforeClaim(boolean requireNotBeforeClaim) {
            this.requireNotBeforeClaim = requireNotBeforeClaim;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withLeewaySeconds(int leewaySeconds) {
            this.leewaySeconds = leewaySeconds;
            return (T) this;
        }

        public TimeClaimVerificationOpts build() {
            return new TimeClaimVerificationOpts(
                    requireIssuedAtClaim,
                    requireExpirationClaim,
                    requireNotBeforeClaim,
                    leewaySeconds
            );
        }
    }
}
