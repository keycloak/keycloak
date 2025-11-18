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

package org.keycloak.sdjwt.vp;

import org.keycloak.sdjwt.TimeClaimVerificationOpts;

import static org.keycloak.OID4VCConstants.SD_JWT_KEY_BINDING_DEFAULT_ALLOWED_MAX_AGE;

/**
 * Options for Key Binding JWT verification.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class KeyBindingJwtVerificationOpts extends TimeClaimVerificationOpts {

    /**
     * Specifies the Verifier's policy whether to check Key Binding
     */
    private final boolean keyBindingRequired;

    /**
     * Specifies the maximum age (in seconds) of an issued Key Binding
     */
    private final int allowedMaxAge;

    private final String nonce;
    private final String aud;

    private KeyBindingJwtVerificationOpts(
            boolean keyBindingRequired,
            int allowedMaxAge,
            String nonce,
            String aud,
            boolean validateExpirationClaim,
            boolean validateNotBeforeClaim,
            int allowClockSkewSeconds
    ) {
        super(true, validateExpirationClaim, validateNotBeforeClaim, allowClockSkewSeconds);
        this.keyBindingRequired = keyBindingRequired;
        this.allowedMaxAge = allowedMaxAge;
        this.nonce = nonce;
        this.aud = aud;
    }

    public boolean isKeyBindingRequired() {
        return keyBindingRequired;
    }

    public int getAllowedMaxAge() {
        return allowedMaxAge;
    }

    public String getNonce() {
        return nonce;
    }

    public String getAud() {
        return aud;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends TimeClaimVerificationOpts.Builder<Builder> {

        private boolean keyBindingRequired = true;
        protected boolean validateIssuedAtClaim = true;
        private int allowedMaxAge = SD_JWT_KEY_BINDING_DEFAULT_ALLOWED_MAX_AGE;
        private String nonce;
        private String aud;

        public Builder withKeyBindingRequired(boolean keyBindingRequired) {
            this.keyBindingRequired = keyBindingRequired;
            return this;
        }

        public Builder withAllowedMaxAge(int allowedMaxAge) {
            this.allowedMaxAge = allowedMaxAge;
            return this;
        }

        public Builder withNonce(String nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder withAud(String aud) {
            this.aud = aud;
            return this;
        }

        @Override
        public KeyBindingJwtVerificationOpts build() {
            if (!validateIssuedAtClaim) {
                throw new IllegalArgumentException(
                        "Validating `iat` claim cannot be disabled for KB-JWT verification because mandated"
                );
            }

            if (keyBindingRequired && (aud == null || nonce == null || nonce.isEmpty())) {
                throw new IllegalArgumentException(
                        "Missing `nonce` and `aud` claims for replay protection"
                );
            }

            return new KeyBindingJwtVerificationOpts(
                    keyBindingRequired,
                    allowedMaxAge,
                    nonce,
                    aud,
                    requireExpirationClaim,
                    requireNotBeforeClaim,
                    allowedClockSkewSeconds
            );
        }
    }
}
