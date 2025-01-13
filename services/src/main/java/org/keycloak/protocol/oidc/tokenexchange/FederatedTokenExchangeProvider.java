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
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;

/**
 * Provider for external-internal or internal-external token exchange, where identity providers are involved
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedTokenExchangeProvider extends AbstractTokenExchangeProvider {

    @Override
    public boolean supports(TokenExchangeContext context) {
        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer != null) {
            // Internal-external token exchange
            return true;
        }

        return isExternalInternalTokenExchangeRequest(context);
    }

    @Override
    protected Response tokenExchange() {
        KeycloakSession session = context.getSession();
        EventBuilder event = context.getEvent();

        UserModel tokenUser = null;
        UserSessionModel tokenSession = null;
        AccessToken token = null;

        String subjectToken = context.getParams().getSubjectToken();
        if (subjectToken != null) {
            String subjectTokenType = context.getParams().getSubjectTokenType();
            if (isExternalInternalTokenExchangeRequest(context)) {
                String subjectIssuer = getSubjectIssuer(context, subjectToken, subjectTokenType);
                return exchangeExternalToken(subjectIssuer, subjectToken);
            }

            if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
                event.detail(Details.REASON, "subject_token supports access tokens only");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(context.getCors(), OAuthErrorException.INVALID_REQUEST, "Invalid token type, must be access token", Response.Status.BAD_REQUEST);

            }

            AuthenticationManager.AuthResult authResult = AuthenticationManager.verifyIdentityToken(session, context.getRealm(), session.getContext().getUri(), context.getClientConnection(), true, true, null, false, subjectToken, context.getHeaders());
            if (authResult == null) {
                event.detail(Details.REASON, "subject_token validation failure");
                event.error(Errors.INVALID_TOKEN);
                throw new CorsErrorResponseException(context.getCors(), OAuthErrorException.INVALID_REQUEST, "Invalid token", Response.Status.BAD_REQUEST);
            }

            tokenUser = authResult.getUser();
            tokenSession = authResult.getSession();
        }

        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        return exchangeToIdentityProvider(tokenUser, tokenSession, requestedIssuer);
    }

}
