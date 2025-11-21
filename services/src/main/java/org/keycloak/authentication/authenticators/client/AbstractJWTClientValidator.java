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

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.JsonWebToken;

import org.jboss.logging.Logger;

/**
 * Common validation for JWT client authentication with private_key_jwt or with client_secret
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractJWTClientValidator extends AbstractBaseJWTValidator {

    private static final Logger logger = Logger.getLogger(AbstractJWTClientValidator.class);

    protected final ClientAuthenticationFlowContext context;
    protected final RealmModel realm;
    protected final SignatureValidator signatureValidator;
    protected final String clientAuthenticatorProviderId;
    protected String expectedClientAssertionType = OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT;

    public AbstractJWTClientValidator(ClientAuthenticationFlowContext context, SignatureValidator signatureValidator, String clientAuthenticatorProviderId) throws Exception {
        super(context.getSession(), context.getState(ClientAssertionState.class, ClientAssertionState.supplier()));
        this.context = context;
        this.realm = context.getRealm();
        this.signatureValidator = signatureValidator;
        this.clientAuthenticatorProviderId = clientAuthenticatorProviderId;
    }

    public ClientAuthenticationFlowContext getContext() {
        return context;
    }

    public ClientModel getClient() {
        return clientAssertionState.getClient();
    }

    public boolean validate() {
        return validateClientAssertionParameters() &&
                validateClient() &&
                validateSignatureAlgorithm(getExpectedSignatureAlgorithm()) &&
                validateSignature() &&
                validateTokenAudience(getExpectedAudiences(), isMultipleAudienceAllowed()) &&
                validateTokenActive(getAllowedClockSkew(), getMaximumExpirationTime(), isReusePermitted());
    }

    private boolean validateClientAssertionParameters() {
        String clientAssertionType = clientAssertionState.getClientAssertionType();
        String clientAssertion = clientAssertionState.getClientAssertion();

        if (clientAssertionType == null) {
            return failure("Parameter client_assertion_type is missing");
        }

        if (!expectedClientAssertionType.equals(clientAssertionType)) {
            return failure("Parameter client_assertion_type has value '"
                    + clientAssertionType + "' but expected is '" + expectedClientAssertionType + "'");
        }

        if (clientAssertion == null) {
            return failure("client_assertion parameter missing");
        }

        return true;
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

        ClientModel client = clientAssertionState.getClient();

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

    private boolean validateSignature() {
        return signatureValidator.verifySignature(this);
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

    @Override
    protected void failureCallback(String errorDescription) {
        failure(errorDescription);
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
