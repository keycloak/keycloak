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

package org.keycloak.protocol.oidc.tokenexchange;

import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * Provider for internal-internal token exchange, which is compliant with the token exchange specification https://datatracker.ietf.org/doc/html/rfc8693
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class StandardTokenExchangeProvider extends AbstractTokenExchangeProvider {

    @Override
    public boolean supports(TokenExchangeContext context) {
        return true;
    }

    @Override
    protected Response tokenExchange() {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        ClientConnection clientConnection = context.getClientConnection();
        Cors cors = context.getCors();
        EventBuilder event = context.getEvent();

        String subjectToken = context.getParams().getSubjectToken();
        if (subjectToken == null) {
            event.detail(Details.REASON, "subject_token parameter not provided");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "subject_token parameter not provided", Response.Status.BAD_REQUEST);
        }
        String subjectTokenType = context.getParams().getSubjectTokenType();
        if (subjectTokenType == null) {
            event.detail(Details.REASON, "subject_token_type parameter not provided");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "subject_token_type parameter not provided", Response.Status.BAD_REQUEST);
        }

        if (!subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
            event.detail(Details.REASON, "subject_token supports access tokens only");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

        }

        AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, realm, session.getContext().getUri(), clientConnection, true, true, null, false, subjectToken, context.getHeaders());
        if (authResult == null) {
            event.detail(Details.REASON, "subject_token validation failure");
            event.error(Errors.INVALID_TOKEN);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Invalid token", Response.Status.BAD_REQUEST);
        }

        UserModel tokenUser = authResult.getUser();
        UserSessionModel tokenSession = authResult.getSession();
        AccessToken token = authResult.getToken();


        String requestedSubject = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        if (requestedSubject != null) {
            event.detail(Details.REASON, "Parameter '" + OAuth2Constants.REQUESTED_SUBJECT + "' not supported for standard token exchange");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Parameter '" + OAuth2Constants.REQUESTED_SUBJECT + "' not supported for standard token exchange", Response.Status.BAD_REQUEST);
        }

        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer != null) {
            event.detail(Details.REASON, "Parameter '" + OAuth2Constants.REQUESTED_ISSUER + "' not supported for standard token exchange");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Parameter '" + OAuth2Constants.REQUESTED_ISSUER + "' not supported for standard token exchange", Response.Status.BAD_REQUEST);
        }

        return exchangeClientToClient(tokenUser, tokenSession, token, true);
    }
}
