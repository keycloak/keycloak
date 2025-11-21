/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authentication.authenticators.client;

import java.util.List;

import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.representations.JsonWebToken;

import org.jboss.logging.Logger;

/**
 * Base validator for JWT authorization grant and JWT client validators.
 *
 * @author rmartinc
 */
public abstract class AbstractBaseJWTValidator {

    private static final Logger logger = Logger.getLogger(AbstractBaseJWTValidator.class);

    protected final ClientAssertionState clientAssertionState;
    protected final KeycloakSession session;
    protected final int currentTime;

    public AbstractBaseJWTValidator(KeycloakSession session, ClientAssertionState clientAssertionState) {
        this.session = session;
        this.clientAssertionState = clientAssertionState;
        this.currentTime = Time.currentTime();
    }

    public ClientAssertionState getState() {
        return clientAssertionState;
    }

    public String getClientAssertion() {
        return clientAssertionState.getClientAssertion();
    }

    public JWSInput getJws() {
        return clientAssertionState.getJws();
    }

    public boolean validateTokenActive(int allowedClockSkew, int maxExp, boolean reusePermitted) {
        JsonWebToken token = clientAssertionState.getToken();
        long lifespan;

        if (token.getExp() == null) {
            return failure("Token exp claim is required");
        }

        if (!token.isActive(allowedClockSkew)) {
            return failure("Token is not active");
        }

        lifespan = token.getExp() - currentTime;

        if (token.getIat() == null) {
            if (lifespan > maxExp) {
                return failure("Token expiration is too far in the future and iat claim not present in token");
            }
        } else {
            if (token.getIat() - allowedClockSkew > currentTime) {
                return failure("Token was issued in the future");
            }
            lifespan = Math.min(lifespan, maxExp);
            if (lifespan <= 0) {
                return failure("Token is not active");
            }
            if (currentTime > token.getIat() + maxExp) {
                return failure("Token was issued too far in the past to be used now");
            }
        }

        if (!reusePermitted) {
            if (token.getId() == null) {
                return failure("Token jti claim is required");
            }

            if (!validateTokenReuse(lifespan)) {
                return false;
            }
        }

        return true;
    }

    protected boolean validateTokenReuse(long lifespanInSecs) {
        final JsonWebToken token = clientAssertionState.getToken();
        final String tokenId = token.getId();
        SingleUseObjectProvider singleUseCache = session.singleUseObjects();
        if (singleUseCache.putIfAbsent(tokenId, lifespanInSecs)) {
            logger.tracef("Added token '%s' to single-use cache. Lifespan: %d seconds, issuedFor: %s", tokenId, lifespanInSecs, token.getIssuedFor());
        } else {
            logger.warnf("Token '%s' already used when for issuedFor '%s'.", tokenId, token.getIssuedFor());
            return failure("Token reuse detected");
        }
        return true;
    }

    public boolean validateTokenAudience(List<String> expectedAudiences, boolean multipleAudienceAllowed) {
        JsonWebToken token = clientAssertionState.getToken();
        if (!token.hasAnyAudience(expectedAudiences)) {
            return failure("Invalid token audience");
        }

        if (!multipleAudienceAllowed && token.getAudience().length > 1) {
            return failure("Multiple audiences not allowed");
        }

        return true;
    }

    public boolean validateSignatureAlgorithm(String expectedSignatureAlg) {
        JWSInput jws = clientAssertionState.getJws();

        if (jws.getHeader().getAlgorithm() == null) {
            return failure("Invalid signature algorithm");
        }

        if (expectedSignatureAlg != null) {
            if (!expectedSignatureAlg.equals(jws.getHeader().getAlgorithm().name())) {
                return failure("Invalid signature algorithm");
            }
        }

        return true;
    }

    private boolean failure(String errorDescription) {
        failureCallback(errorDescription);
        return false;
    }

    protected abstract void failureCallback(String errorDescription);
}
