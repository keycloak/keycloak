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

package org.keycloak.protocol.oidc.grants;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.authenticators.client.AbstractBaseJWTValidator;
import org.keycloak.authentication.authenticators.client.ClientAssertionState;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

/**
 * Validator for JWT Authorization grant that extends AbstractBaseJWTValidator and
 * implements the JWTAuthorizationGrantValidationContext interface.
 *
 * @author rmartinc
 */
public class JWTAuthorizationGrantValidator extends AbstractBaseJWTValidator implements JWTAuthorizationGrantValidationContext {

    public static JWTAuthorizationGrantValidator createValidator(KeycloakSession session, ClientModel client, String assertion) {
        if (assertion == null) {
            throw new RuntimeException("Missing parameter:" + OAuth2Constants.ASSERTION);
        }
        try {
            JWSInput jws = new JWSInput(assertion);
            JsonWebToken jwt = jws.readJsonContent(JsonWebToken.class);
            ClientAssertionState clientAssertionState = new ClientAssertionState(client, OAuth2Constants.JWT_AUTHORIZATION_GRANT, assertion, jws, jwt);
            return new JWTAuthorizationGrantValidator(session, clientAssertionState);
        } catch (JWSInputException e) {
            throw new RuntimeException("The provided assertion is not a valid JWT");
        }
    }

    private JWTAuthorizationGrantValidator(KeycloakSession session, ClientAssertionState clientAssertionState) {
        super(session, clientAssertionState);
    }

    public void validateClient() {
        if (clientAssertionState.getClient().isPublicClient()) {
            failureCallback("Public client not allowed to use authorization grant");
        }

        String val = clientAssertionState.getClient().getAttribute(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED);
        if (!Boolean.parseBoolean(val)) {
            throw new RuntimeException("JWT Authorization Grant is not supported for the requested client");
        }
    }

    public void validateIssuer() {
        if (getJWT().getIssuer() == null) {
            failureCallback("Missing claim: " + OAuth2Constants.ISSUER);
        }
    }

    public void validateSubject() {
        if (getJWT().getSubject() == null) {
            failureCallback("Missing claim: " + IDToken.SUBJECT);
        }
    }

    @Override
    public JsonWebToken getJWT() {
        return clientAssertionState.getToken();
    }

    @Override
    public String getAssertion() {
        return clientAssertionState.getClientAssertion();
    }

    @Override
    protected void failureCallback(String errorDescription) {
        throw new RuntimeException(errorDescription);
    }
}
