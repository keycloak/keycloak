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

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.managers.UserSessionManager;

/**
 * Provider for external-internal token exchange
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExternalToInternalTokenExchangeProvider extends StandardTokenExchangeProvider {

    @Override
    public boolean supports(TokenExchangeContext context) {
        return (isExternalInternalTokenExchangeRequest(context));
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    protected Response tokenExchange() {
        String subjectToken = context.getParams().getSubjectToken();
        String subjectTokenType = context.getParams().getSubjectTokenType();
        String subjectIssuer = getSubjectIssuer(this.context, subjectToken, subjectTokenType);
        return exchangeExternalToken(subjectIssuer, subjectToken);
    }

    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.ID_TOKEN_TYPE);
    }

    @Override
    protected String getRequestedTokenType() {
        String requestedTokenType = params.getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
            return requestedTokenType;
        }
        if (getSupportedOAuthResponseTokenTypes().contains(requestedTokenType)) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    protected Response exchangeExternalToken(String subjectIssuer, String subjectToken) {
        // try to find the IDP whose alias matches the issuer or the subject issuer in the form params.
        ExternalExchangeContext externalExchangeContext = this.locateExchangeExternalTokenByAlias(subjectIssuer);

        if (externalExchangeContext == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }
        BrokeredIdentityContext context = externalExchangeContext.provider().exchangeExternal(this, this.context);
        if (context == null) {
            event.error(Errors.INVALID_ISSUER);
            throw new CorsErrorResponseException(cors, Errors.INVALID_ISSUER, "Invalid " + OAuth2Constants.SUBJECT_ISSUER + " parameter", Response.Status.BAD_REQUEST);
        }

        UserModel user = importUserFromExternalIdentity(context);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "external-exchange", false, null, null);
        externalExchangeContext.provider().exchangeExternalComplete(userSession, context, formParams);

        // this must exist so that we can obtain access token from user session if idp's store tokens is off
        userSession.setNote(UserAuthenticationIdentityProvider.EXTERNAL_IDENTITY_PROVIDER, externalExchangeContext.idpModel().getAlias());
        userSession.setNote(UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN, subjectToken);

        context.addSessionNotesToUserSession(userSession);

        return exchangeClientToClient(user, userSession, null, false);
    }

}
