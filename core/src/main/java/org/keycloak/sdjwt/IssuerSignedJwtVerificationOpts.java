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

import java.util.List;

import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Options for Issuer-signed JWT verification.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class IssuerSignedJwtVerificationOpts extends ClaimVerifier {


    public IssuerSignedJwtVerificationOpts(List<ClaimVerifier.Predicate<ObjectNode>> headerVerifiers,
                                           List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
        super(headerVerifiers, contentVerifiers);
    }

    public void verify(JwsToken tokenToVerify) throws VerificationException {
        super.verifyClaims(tokenToVerify.getJwsHeaderAsNode(), tokenToVerify.getPayload());
    }

    public static IssuerSignedJwtVerificationOpts.Builder builder() {
        return new IssuerSignedJwtVerificationOpts.Builder();
    }

    public static class Builder extends ClaimVerifier.Builder {

        public Builder() {
        }

        public Builder(Integer clockSkew) {
            super(clockSkew);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withIatCheck(Integer allowedMaxAge) {
            return (Builder) super.withIatCheck(allowedMaxAge);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withIatCheck(boolean isCheckOptional) {
            return (Builder) super.withIatCheck(isCheckOptional);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withIatCheck(Integer allowedMaxAge, boolean isCheckOptional) {
            return (Builder) super.withIatCheck(allowedMaxAge, isCheckOptional);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withNbfCheck() {
            return (Builder) super.withNbfCheck();
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withNbfCheck(boolean isCheckOptional) {
            return (Builder) super.withNbfCheck(isCheckOptional);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withExpCheck() {
            return (Builder) super.withExpCheck();
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withExpCheck(boolean isCheckOptional) {
            return (Builder) super.withExpCheck(isCheckOptional);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withClockSkew(int clockSkew) {
            return (Builder) super.withClockSkew(clockSkew);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            return (Builder) super.withContentVerifiers(contentVerifiers);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder addContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            return (Builder) super.addContentVerifiers(contentVerifiers);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withAudCheck(String expectedAud) {
            return (Builder) super.withAudCheck(expectedAud);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withClaimCheck(String claimName, String expectedValue) {
            return (Builder) super.withClaimCheck(claimName, expectedValue);
        }

        @Override
        public IssuerSignedJwtVerificationOpts.Builder withClaimCheck(String claimName,
                                                                      String expectedValue,
                                                                      boolean isOptionalCheck) {
            return (Builder) super.withClaimCheck(claimName, expectedValue, isOptionalCheck);
        }

        public IssuerSignedJwtVerificationOpts build() {

            return new IssuerSignedJwtVerificationOpts(headerVerifiers, contentVerifiers);
        }
    }
}
