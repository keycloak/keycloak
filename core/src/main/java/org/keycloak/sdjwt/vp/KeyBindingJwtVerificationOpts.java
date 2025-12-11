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

import java.util.List;
import java.util.Optional;

import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.ClaimVerifier;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Options for Key Binding JWT verification.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class KeyBindingJwtVerificationOpts extends IssuerSignedJwtVerificationOpts {

    /**
     * Specifies the Verifier's policy whether to check Key Binding
     */
    private final boolean keyBindingRequired;

    /**
     * Specifies the maximum age (in seconds) of an issued Key Binding
     */
    private final int allowedMaxAge;

    public KeyBindingJwtVerificationOpts(boolean keyBindingRequired,
                                         int allowedMaxAge,
                                         List<ClaimVerifier.Predicate<ObjectNode>> headerVerifiers,
                                         List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
        super(headerVerifiers, contentVerifiers);
        this.keyBindingRequired = keyBindingRequired;
        this.allowedMaxAge = allowedMaxAge;
    }

    public boolean isKeyBindingRequired() {
        return keyBindingRequired;
    }

    public int getAllowedMaxAge() {
        return allowedMaxAge;
    }

    public static KeyBindingJwtVerificationOpts.Builder builder() {
        return new KeyBindingJwtVerificationOpts.Builder();
    }

    public static KeyBindingJwtVerificationOpts.Builder builder(Integer clockSkew) {
        return new KeyBindingJwtVerificationOpts.Builder(clockSkew);
    }

    public static class Builder extends IssuerSignedJwtVerificationOpts.Builder {
        private boolean keyBindingRequired = true;

        public Builder() {
            super();
        }

        public Builder(Integer clockSkew) {
            super(clockSkew);
        }

        public Builder withKeyBindingRequired(boolean keyBindingRequired) {
            this.keyBindingRequired = keyBindingRequired;
            return this;
        }

        public KeyBindingJwtVerificationOpts.Builder withNonceCheck(String expectedNonce) {
            return withClaimCheck(IDToken.NONCE, expectedNonce, true);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withAudCheck(String expectedAud) {
            return (Builder) super.withAudCheck(expectedAud);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withIatCheck(Integer allowedMaxAge) {
            return (Builder) super.withIatCheck(allowedMaxAge);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withIatCheck(boolean isCheckOptional) {
            return (Builder) super.withIatCheck(isCheckOptional);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withIatCheck(Integer allowedMaxAge, boolean isCheckOptional) {
            return (Builder) super.withIatCheck(allowedMaxAge, isCheckOptional);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withNbfCheck() {
            return (Builder) super.withNbfCheck();
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withNbfCheck(boolean isCheckOptional) {
            return (Builder) super.withNbfCheck(isCheckOptional);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withExpCheck() {
            return (Builder) super.withExpCheck();
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withExpCheck(boolean isCheckOptional) {
            return (Builder) super.withExpCheck(isCheckOptional);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withClockSkew(int clockSkew) {
            return (Builder) super.withClockSkew(clockSkew);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withClaimCheck(String claimName, String expectedValue) {
            return (Builder) super.withClaimCheck(claimName, expectedValue);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withClaimCheck(String claimName, String expectedValue, boolean isOptionalCheck) {
            return (Builder) super.withClaimCheck(claimName, expectedValue, isOptionalCheck);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder withContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            return (Builder) super.withContentVerifiers(contentVerifiers);
        }

        @Override
        public KeyBindingJwtVerificationOpts.Builder addContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            return (Builder) super.addContentVerifiers(contentVerifiers);
        }

        @Override
        public KeyBindingJwtVerificationOpts build() {
            boolean isAudCheckPresent = contentVerifiers.stream().anyMatch(verifier -> {
                return verifier instanceof AudienceCheck ||
                    (verifier instanceof ClaimCheck && ((ClaimCheck) verifier).getClaimName().equals(JsonWebToken.AUD));
            });
            boolean isNonceCheckPresent = contentVerifiers.stream().anyMatch(verifier -> {
                return verifier instanceof ClaimCheck && ((ClaimCheck) verifier).getClaimName().equals(IDToken.NONCE)
                    && Optional.ofNullable(((ClaimCheck) verifier).getExpectedClaimValue()).map(s -> !s.isEmpty())
                               .orElse(false);
            });
            if (keyBindingRequired && (!isAudCheckPresent || !isNonceCheckPresent)) {
                throw new IllegalArgumentException("Missing `nonce` and `aud` claims for replay protection");
            }

            return new KeyBindingJwtVerificationOpts(keyBindingRequired,
                                                     allowedMaxAge,
                                                     headerVerifiers,
                                                     contentVerifiers);
        }
    }
}
