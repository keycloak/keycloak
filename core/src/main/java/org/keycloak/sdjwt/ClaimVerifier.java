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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Module for checking the validity of JWT time claims. All checks account for a leeway window to accommodate clock
 * skew.
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class ClaimVerifier {

    public static final int DEFAULT_CLOCK_SKEW = 10;
    public static final int DEFAULT_ALLOWED_MAX_AGE = 5 * 60; // 5 minutes

    public static final String CLAIM_NAME_IAT = "iat";
    public static final String CLAIM_NAME_EXP = "exp";
    public static final String CLAIM_NAME_NBF = "nbf";

    private final List<Predicate<ObjectNode>> verifiers;

    public ClaimVerifier(List<Predicate<ObjectNode>> verifiers) {
        this.verifiers = verifiers;
    }

    public void verifyClaims(ObjectNode jsonNode) throws VerificationException {
        for (Predicate<ObjectNode> verifier : verifiers) {
            verifier.test(jsonNode);
        }
    }

    public List<ClaimVerifier.Predicate<ObjectNode>> getVerifiers() {
        return verifiers;
    }

    public static ClaimVerifier.Builder builder() {
        return new ClaimVerifier.Builder();
    }

    /**
     * Functional interface of checks that verify some part of a JWT.
     *
     * @param <T> Type of the token handled by this predicate.
     */
    public interface Predicate<T> {
        /**
         * Performs a single check on the given token verifier.
         *
         * @param t Token, guaranteed to be non-null.
         * @return
         * @throws VerificationException
         */
        boolean test(T t) throws VerificationException;

        default Instant getCurrentTimestamp() {
            return Instant.ofEpochSecond(Time.currentTime());
        }
    }

    public static abstract class TimeCheck {

        protected int clockSkewSeconds;

        public TimeCheck(int clockSkewSeconds) {
            this.clockSkewSeconds = Math.max(0, clockSkewSeconds);
        }

        public int getClockSkewSeconds() {
            return clockSkewSeconds;
        }

        public void setClockSkewSeconds(int clockSkewSeconds) {
            this.clockSkewSeconds = clockSkewSeconds;
        }
    }

    public static class ClaimCheck implements Predicate<ObjectNode> {

        private final String claimName;

        private final String expectedClaimValue;

        private final boolean isOptional;

        public ClaimCheck(String claimName, String expectedClaimValue) {
            this(claimName, expectedClaimValue, false);
        }

        public ClaimCheck(String claimName, String expectedClaimValue, boolean isOptional) {
            this.claimName = claimName;
            this.expectedClaimValue = expectedClaimValue;
            this.isOptional = isOptional;
        }

        @Override
        public boolean test(ObjectNode t) throws VerificationException {
            if (expectedClaimValue == null) {
                throw new VerificationException(String.format("Missing expected value for claim '%s'", claimName));
            }

            String claimValue = Optional.ofNullable(t.get(claimName)).map(JsonNode::asText).map(String::valueOf)
                                        .orElse(null);
            if (claimValue == null && !isOptional) {
                throw new VerificationException(String.format("Missing claim '%s' in token", claimName));
            }

            if (!expectedClaimValue.equals(claimValue)) {
                throw new VerificationException(String.format("Expected value '%s' in token for claim '%s' does " +
                                                                  "not match actual value '%s'",
                                                              expectedClaimValue,
                                                              claimName,
                                                              claimValue));
            }

            return true;
        }

        public String getClaimName() {
            return claimName;
        }
    }

    public static class IatLifetimeCheck extends TimeCheck implements Predicate<ObjectNode> {

        private final long maxLifetime;

        private boolean isOptional;

        public IatLifetimeCheck(int clockSkewSeconds, long maxLifetime) {
            this(clockSkewSeconds, maxLifetime, false);
        }

        public IatLifetimeCheck(int clockSkewSeconds, long maxLifetime, boolean isOptional) {
            super(Math.max(0, clockSkewSeconds));
            this.maxLifetime = Math.max(0, maxLifetime);
            this.isOptional = isOptional;
        }

        @Override
        public boolean test(ObjectNode jsonWebToken) throws VerificationException {
            Long iat = Optional.ofNullable(jsonWebToken.get(ClaimVerifier.CLAIM_NAME_IAT))
                               .filter(node -> !node.isNull())
                               .map(JsonNode::asLong)
                               .orElse(null);
            if (iat == null) {
                if (isOptional) {
                    return true;
                }
                else {
                    throw new VerificationException("Missing required claim 'iat'");
                }
            }

            long now = getCurrentTimestamp().getEpochSecond();

            if (now + clockSkewSeconds < iat) {
                throw new VerificationException(String.format("Token was issued in the future: now: '%s', iat: '%s'",
                                                              now,
                                                              iat));
            }

            long expiration = iat + maxLifetime;

            if (expiration < now - clockSkewSeconds) {
                throw new VerificationException(String.format("Token has expired by iat: now: '%s', expired at: '%s', "
                                                                  + "iat: '%s', maxLifetime: '%s'",
                                                              now,
                                                              expiration,
                                                              iat,
                                                              maxLifetime));
            }
            return true;
        }
    }

    public static class NbfCheck extends TimeCheck implements Predicate<ObjectNode> {

        private boolean isOptional;

        public NbfCheck(int clockSkewSeconds) {
            this(clockSkewSeconds, false);
        }

        public NbfCheck(int clockSkewSeconds, boolean isOptional) {
            super(Math.max(0, clockSkewSeconds));
            this.isOptional = isOptional;
        }

        @Override
        public boolean test(ObjectNode jsonWebToken) throws VerificationException {
            Long notBefore = Optional.ofNullable(jsonWebToken.get(ClaimVerifier.CLAIM_NAME_NBF))
                                     .filter(node -> !node.isNull())
                                     .map(JsonNode::asLong)
                                     .orElse(null);
            if (notBefore == null) {
                if (isOptional) {
                    return true;
                }
                else {
                    throw new VerificationException("Missing required claim 'nbf'");
                }
            }
            long now = getCurrentTimestamp().getEpochSecond();

            if (notBefore > now + clockSkewSeconds) {
                throw new VerificationException(String.format("Token is not yet valid: now: '%s', nbf: '%s'",
                                                              now,
                                                              notBefore));
            }
            return true;
        }
    }

    public static class ExpCheck extends TimeCheck implements Predicate<ObjectNode> {

        private boolean isOptional;

        public ExpCheck(int clockSkewSeconds) {
            this(clockSkewSeconds, false);
        }

        public ExpCheck(int clockSkewSeconds, boolean isOptional) {
            super(Math.max(0, clockSkewSeconds));
            this.isOptional = isOptional;
        }

        @Override
        public boolean test(ObjectNode jsonWebToken) throws VerificationException {
            Long expiration = Optional.ofNullable(jsonWebToken.get(ClaimVerifier.CLAIM_NAME_EXP))
                                      .filter(node -> !node.isNull())
                                      .map(JsonNode::asLong)
                                      .orElse(null);
            if (expiration == null) {
                if (isOptional) {
                    return true;
                }
                else {
                    throw new VerificationException("Missing required claim 'exp'");
                }
            }
            long now = getCurrentTimestamp().getEpochSecond();

            if (expiration < now - clockSkewSeconds) {
                throw new VerificationException(String.format("Token has expired by exp: now: '%s', exp: '%s'",
                                                              now,
                                                              expiration));
            }
            return true;
        }
    }

    public static class AudienceCheck implements Predicate<ObjectNode> {

        private final String expectedAudience;

        public AudienceCheck(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public boolean test(ObjectNode t) throws VerificationException {
            if (expectedAudience == null) {
                throw new VerificationException("Missing expected audience");
            }

            JsonNode audienceArray = t.get("aud");
            if (audienceArray == null) {
                throw new VerificationException("No audience in the token");
            }

            Set<String> audiences = new HashSet<>();
            if (audienceArray.isArray()) {
                for (JsonNode audienceNode : audienceArray) {
                    audiences.add(audienceNode.textValue());
                }
            }
            else {
                audiences.add(audienceArray.textValue());
            }

            if (audiences.contains(expectedAudience)) {
                return true;
            }

            throw new VerificationException(String.format("Expected audience '%s' not available in the token. " +
                                                              "Present values are '%s'",
                                                          expectedAudience, audiences));
        }
    }

    public static class Builder {

        protected Integer clockSkew = DEFAULT_CLOCK_SKEW;
        protected Integer allowedMaxAge = DEFAULT_ALLOWED_MAX_AGE;
        protected List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers = new ArrayList<>();

        public Builder() {
            this(DEFAULT_CLOCK_SKEW);
        }

        public Builder(Integer clockSkew) {
            this.withClockSkew(Optional.ofNullable(clockSkew).orElse(DEFAULT_CLOCK_SKEW));
            Set<Class> timeVerifierTypes = contentVerifiers.stream()
                                                           .filter(verifier -> {
                                                               return verifier instanceof TimeCheck;
                                                           }).map(Object::getClass)
                                                           .collect(Collectors.toSet());
            if (!timeVerifierTypes.contains(IatLifetimeCheck.class)) {
                this.withIatCheck(allowedMaxAge, false);
            }
            if (!timeVerifierTypes.contains(ExpCheck.class)) {
                this.withExpCheck(false);
            }
            if (!timeVerifierTypes.contains(NbfCheck.class)) {
                this.withNbfCheck(false);
            }
        }

        public Builder withClockSkew(int clockSkew) {
            this.clockSkew = Math.max(0, clockSkew);
            contentVerifiers.stream()
                            .filter(verifier -> verifier instanceof TimeCheck)
                            .forEach(timeCheckVerifier -> {
                                ((TimeCheck) timeCheckVerifier).setClockSkewSeconds(clockSkew);
                            });
            return this;
        }

        public Builder withIatCheck(Integer allowedMaxAge) {
            return withIatCheck(allowedMaxAge, false);
        }

        public Builder withIatCheck(boolean isCheckOptional) {
            return withIatCheck(allowedMaxAge, isCheckOptional);
        }

        public Builder withIatCheck(Integer allowedMaxAge, boolean isCheckOptional) {
            this.allowedMaxAge = Optional.ofNullable(allowedMaxAge).orElse(0);
            contentVerifiers.removeIf(verifier -> verifier instanceof ClaimVerifier.IatLifetimeCheck);
            if (allowedMaxAge != null) {
                contentVerifiers.add(new ClaimVerifier.IatLifetimeCheck(Optional.ofNullable(clockSkew).orElse(0),
                                                                        allowedMaxAge,
                                                                        isCheckOptional));
            }
            return this;
        }

        public Builder withNbfCheck() {
            withNbfCheck(false);
            return this;
        }

        public Builder withNbfCheck(boolean isCheckOptional) {
            contentVerifiers.removeIf(verifier -> verifier instanceof ClaimVerifier.NbfCheck);
            if (clockSkew != null) {
                contentVerifiers.add(new ClaimVerifier.NbfCheck(clockSkew, isCheckOptional));
            }
            return this;
        }

        public Builder withExpCheck() {
            withExpCheck(false);
            return this;
        }

        public Builder withExpCheck(boolean isCheckOptional) {
            contentVerifiers.removeIf(verifier -> verifier instanceof ClaimVerifier.ExpCheck);
            if (clockSkew != null) {
                contentVerifiers.add(new ClaimVerifier.ExpCheck(clockSkew, isCheckOptional));
            }
            return this;
        }

        public Builder withAudCheck(String expectedAud) {
            contentVerifiers.removeIf(verifier -> verifier instanceof ClaimVerifier.AudienceCheck);
            if (expectedAud != null) {
                contentVerifiers.add(new ClaimVerifier.AudienceCheck(expectedAud));
            }
            return this;
        }

        public Builder withClaimCheck(String claimName, String expectedValue) {
            return withClaimCheck(claimName, expectedValue, false);
        }

        public Builder withClaimCheck(String claimName, String expectedValue, boolean isOptionalCheck) {
            contentVerifiers.removeIf(verifier -> {
                return verifier instanceof ClaimVerifier.ClaimCheck &&
                    ((ClaimVerifier.ClaimCheck) verifier).getClaimName().equals(claimName);
            });
            if (expectedValue != null) {
                contentVerifiers.add(new ClaimVerifier.ClaimCheck(claimName, expectedValue, isOptionalCheck));
            }
            return this;
        }

        public Builder withContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            this.contentVerifiers = contentVerifiers;
            return this;
        }

        public Builder addContentVerifiers(List<ClaimVerifier.Predicate<ObjectNode>> contentVerifiers) {
            this.contentVerifiers = Optional.ofNullable(this.contentVerifiers).orElseGet(ArrayList::new);
            this.contentVerifiers.addAll(contentVerifiers);
            return this;
        }

        public ClaimVerifier build() {

            return new ClaimVerifier(contentVerifiers);
        }
    }
}
