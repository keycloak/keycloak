/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.representations.JsonWebToken;

import java.util.List;

/**
 * Common validation for JWT client authentication with private_key_jwt or with client_secret
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractJWTClientValidator {

    private static final Logger logger = Logger.getLogger(AbstractJWTClientValidator.class);

    protected final ClientAuthenticationFlowContext context;
    protected final RealmModel realm;
    protected final int currentTime;
    protected final SignatureValidator signatureValidator;
    protected final String clientAuthenticatorProviderId;
    protected String expectedClientAssertionType = OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT;

    protected final ClientAssertionState clientAssertionState;

    protected ClientModel client;

    public AbstractJWTClientValidator(ClientAuthenticationFlowContext context, SignatureValidator signatureValidator, String clientAuthenticatorProviderId) throws Exception {
        this.context = context;
        this.clientAssertionState = context.getState(ClientAssertionState.class, ClientAssertionState.supplier());
        this.realm = context.getRealm();
        this.signatureValidator = signatureValidator;
        this.currentTime = Time.currentTime();
        this.clientAuthenticatorProviderId = clientAuthenticatorProviderId;
    }

    public ClientAuthenticationFlowContext getContext() {
        return context;
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

    public ClientModel getClient() {
        return client;
    }

    public boolean validate() {
        return validateClientAssertionParameters() &&
                validateClient() &&
                validateSignatureAlgorithm() &&
                validateSignature() &&
                validateTokenAudience() &&
                validateTokenActive();
    }

    private boolean validateClientAssertionParameters() {
        return expectedClientAssertionType.equals(clientAssertionState.getClientAssertionType()) &&
            clientAssertionState.getClientAssertion() != null;
    }

    private boolean validateClient() {
        JsonWebToken token = clientAssertionState.getToken();

        String clientId = token.getSubject();
        if (clientId == null) {
            logger.debug("Can't identify client. Subject missing on JWT token");
            return failure("Token sub claim is required");
        }

        String clientIdParam = context.getHttpRequest().getDecodedFormParameters().getFirst(OAuth2Constants.CLIENT_ID);
        if (clientIdParam != null && !clientIdParam.equals(clientId)) {
            logger.debug("client_id parameter does not match JWT subject");
            return failure("client_id parameter does not match sub claim");
        }

        String expectedTokenIssuer = getExpectedTokenIssuer();
        if (expectedTokenIssuer != null && !expectedTokenIssuer.equals(token.getIssuer())) {
            return false;
        }

        client = clientAssertionState.getClient();

        if (client == null) {
            return failure(AuthenticationFlowError.CLIENT_NOT_FOUND);
        } else {
            context.getEvent().client(client.getClientId());
            context.setClient(client);
        }

        if (!client.isEnabled()) {
            return failure(AuthenticationFlowError.CLIENT_DISABLED);
        }

        if (clientAuthenticatorProviderId != null && !clientAuthenticatorProviderId.equals(client.getClientAuthenticatorType())) {
            logger.debug("Not configured authenticator for client, ignoring");
            return false;
        }

        return true;
    }

    private boolean validateSignatureAlgorithm() {
        JWSInput jws = clientAssertionState.getJws();

        if (jws.getHeader().getAlgorithm() == null) {
            return failure("Invalid signature algorithm");
        }

        String expectedSignatureAlg = getExpectedSignatureAlgorithm();
        if (expectedSignatureAlg != null) {
            if (!expectedSignatureAlg.equals(jws.getHeader().getAlgorithm().name())) {
                return failure("Invalid signature algorithm");
            }
        }

        return true;
    }

    private boolean validateSignature() {
        return signatureValidator.verifySignature(this);
    }

    public boolean validateTokenActive() {
        JsonWebToken token = clientAssertionState.getToken();
        int allowedClockSkew = getAllowedClockSkew();
        int maxExp = getMaximumExpirationTime();
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

        if (!isReusePermitted()) {
            if (token.getId() == null) {
                return failure("Token jti claim is required");
            }

            if (!validateTokenReuse(token.getId(), lifespan)) {
                return false;
            }
        }

        return true;
    }

    private boolean validateTokenReuse(String tokenId, long lifespanInSecs) {
        SingleUseObjectProvider singleUseCache = context.getSession().singleUseObjects();
        if (singleUseCache.putIfAbsent(tokenId, lifespanInSecs)) {
            logger.tracef("Added token '%s' to single-use cache. Lifespan: %d seconds, client: %s", tokenId, lifespanInSecs, client.getClientId());
        } else {
            logger.warnf("Token '%s' already used when authenticating client '%s'.", tokenId, client.getClientId());
            return failure(OAuthErrorException.INVALID_CLIENT, "Token reuse detected", Response.Status.BAD_REQUEST.getStatusCode());
        }
        return true;
    }

    private boolean validateTokenAudience() {
        JsonWebToken token = clientAssertionState.getToken();
        List<String> expectedAudiences = getExpectedAudiences();
        if (!token.hasAnyAudience(expectedAudiences)) {
            return failure("Invalid token audience");
        }

        if (!isMultipleAudienceAllowed() && token.getAudience().length > 1) {
            return failure("Multiple audiences not allowed");
        }

        return true;
    }

    public boolean failure(String errorDescription) {
        return failure(errorDescription, Response.Status.BAD_REQUEST.getStatusCode());
    }

    public boolean failure(String errorDescription, int statusCode) {
        return failure("invalid_client", errorDescription, statusCode);
    }

    public boolean failure(String error, String errorDescription, int statusCode) {
        Response challengeResponse = ClientAuthUtil.errorResponse(statusCode, error, errorDescription);
        return failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
    }

    private boolean failure(AuthenticationFlowError error) {
        return failure(error, null);
    }

    private boolean failure(AuthenticationFlowError error, Response response) {
        context.failure(error, response);
        return false;
    }

    protected abstract String getExpectedTokenIssuer();

    protected abstract List<String> getExpectedAudiences();

    protected abstract boolean isMultipleAudienceAllowed();

    protected abstract int getAllowedClockSkew();

    protected abstract int getMaximumExpirationTime();

    protected abstract boolean isReusePermitted();

    protected abstract String getExpectedSignatureAlgorithm();

    public interface SignatureValidator {

        boolean verifySignature(AbstractJWTClientValidator validator);

    }

}
