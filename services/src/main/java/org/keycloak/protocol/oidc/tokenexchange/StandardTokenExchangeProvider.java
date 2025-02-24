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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.grants.TokenExchangeGrantTypeFactory;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

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

        if(!OIDCAdvancedConfigWrapper.fromClientModel(context.getClient()).isStandardTokenExchangeEnabled()) {
            event.detail(Details.REASON, "Standard token exchange is not enabled for the requested client");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Standard token exchange is not enabled for the requested client", Response.Status.BAD_REQUEST);
        }

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

    @Override
    protected void validateAudience(AccessToken token, boolean disallowOnHolderOfTokenMismatch, List<ClientModel> targetAudienceClients) {
        ClientModel tokenHolder = token == null ? null : realm.getClientByClientId(token.getIssuedFor());

        if (client.isPublicClient()) {
            String errorMessage = "Public client is not allowed to exchange token";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, errorMessage, Response.Status.BAD_REQUEST);
        }

        for (ClientModel targetClient : targetAudienceClients) {
            if (!targetClient.isEnabled()) {
                event.detail(Details.REASON, "audience client disabled");
                event.detail(Details.AUDIENCE, targetClient.getClientId());
                event.error(Errors.CLIENT_DISABLED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Client disabled", Response.Status.BAD_REQUEST);
            }
        }

        //reject if the requester-client is not in the audience of the subject token
        if (!client.equals(tokenHolder)) {
            forbiddenIfClientIsNotWithinTokenAudience(token);
        }
    }

    protected void validateConsents(UserModel targetUser, ClientSessionContext clientSessionCtx) {
        if (!TokenManager.verifyConsentStillAvailable(session, targetUser, client, clientSessionCtx.getClientScopesStream())) {
            event.detail(Details.REASON, "Missing consents for Token Exchange in client " + client.getClientId());
            event.error(Errors.CONSENT_DENIED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE,
                    "Missing consents for Token Exchange in client " + client.getClientId(), Response.Status.BAD_REQUEST);
        }
    }

    // For now, include "scope" parameter as is
    @Override
    protected String getRequestedScope(AccessToken token, List<ClientModel> targetAudienceClients) {
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            AuthorizationRequestContext authorizationRequestContext = AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, scope);
            validScopes = TokenManager.isValidScope(session, scope, authorizationRequestContext, client, null);
        } else {
            validScopes = TokenManager.isValidScope(session, scope, client, null);
        }

        if (!validScopes) {
            String errorMessage = "Invalid scopes: " + scope;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        return scope;
    }

    @Override
    protected void setClientToContext(List<ClientModel> targetAudienceClients) {
        // The client requesting exchange is set in the context
        session.getContext().setClient(client);
    }

    @Override
    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope) {
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, client, scope);

        if (targetUserSession == null) {
            // if no session is associated with a subject_token, a new session will be created, only persistent if refresh token type requested
            targetUserSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, targetUser, targetUser.getUsername(),
                    clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null,
                    requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                            ? UserSessionModel.SessionPersistenceState.PERSISTENT
                            : UserSessionModel.SessionPersistenceState.TRANSIENT);
        }

        event.session(targetUserSession);

        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && clientSessionCtx.getClientScopesStream().filter(s -> OAuth2Constants.OFFLINE_ACCESS.equals(s.getName())).findAny().isPresent()) {
            event.detail(Details.REASON, "Scope offline_access not allowed for token exchange");
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    "Scope offline_access not allowed for token exchange", Response.Status.BAD_REQUEST);
        }

        updateUserSessionFromClientAuth(targetUserSession);

        if (params.getAudience() != null && !targetAudienceClients.isEmpty()) {
            clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENTS, targetAudienceClients.toArray(ClientModel[]::new));
        }

        validateConsents(targetUser, clientSessionCtx);
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, this.session, targetUserSession, clientSessionCtx)
                .generateAccessToken();

        checkRequestedAudiences(responseBuilder);

        if (targetUserSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) {
            responseBuilder.getAccessToken().setSessionId(null);
        }

        if (OAuth2Constants.REFRESH_TOKEN_TYPE.equals(requestedTokenType)) {
            responseBuilder.generateRefreshToken();
        }

        AccessTokenResponse res;
        if (OAuth2Constants.ID_TOKEN_TYPE.equals(requestedTokenType)) {
            // Using the id-token inside "access_token" parameter as per description of "access_token" parameter under https://datatracker.ietf.org/doc/html/rfc8693#name-successful-response
            res = responseBuilder.generateIDToken().build();
            res.setToken(res.getIdToken());
            res.setIdToken(null);
            res.setTokenType(TokenUtil.TOKEN_TYPE_NA);
        } else {
            String scopeParam = params.getScope();
            if (TokenUtil.isOIDCRequest(scopeParam)) {
                responseBuilder.generateIDToken().generateAccessTokenHash();
            }
            res = responseBuilder.build();
        }

        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, requestedTokenType);

        if (responseBuilder.getAccessToken().getAudience() != null) {
            StringJoiner joiner = new StringJoiner(" ");
            for (String s : List.of(responseBuilder.getAccessToken().getAudience())) {
                joiner.add(s);
            }
            event.detail(Details.AUDIENCE, joiner.toString());
        }
        event.user(targetUser);
        event.success();

        return cors.add(Response.ok(res, MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    protected Response exchangeClientToSAML2Client(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType, List<ClientModel> targetAudienceClients) {
        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }

    protected void checkRequestedAudiences(TokenManager.AccessTokenResponseBuilder responseBuilder) {
        if (params.getAudience() != null && (responseBuilder.getAccessToken().getAudience() == null ||
                responseBuilder.getAccessToken().getAudience().length < params.getAudience().size())) {
            final Set<String> missingAudience = new HashSet<>(params.getAudience());
            if (responseBuilder.getAccessToken().getAudience() != null) {
                missingAudience.removeAll(Set.of(responseBuilder.getAccessToken().getAudience()));
            }
            if (!missingAudience.isEmpty()) {
                final String missingAudienceString = CollectionUtil.join(missingAudience);
                event.detail(Details.REASON, "Requested audience not available: " + missingAudienceString);
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "Requested audience not available: " + missingAudienceString, Response.Status.BAD_REQUEST);
            }
        }
    }

    @Override
    protected List<String> getSupportedOAuthResponseTokenTypes() {
        return Arrays.asList(OAuth2Constants.ACCESS_TOKEN_TYPE, OAuth2Constants.ID_TOKEN_TYPE, OAuth2Constants.REFRESH_TOKEN_TYPE);
    }

    @Override
    protected String getRequestedTokenType() {
        String requestedTokenType = params.getRequestedTokenType();
        if (requestedTokenType == null) {
            requestedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
            return requestedTokenType;
        }
        if (requestedTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.ID_TOKEN_TYPE)
                || requestedTokenType.equals(OAuth2Constants.SAML2_TOKEN_TYPE)) {
            return requestedTokenType;
        }
        OIDCAdvancedConfigWrapper oidcClient = OIDCAdvancedConfigWrapper.fromClientModel(client);
        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && oidcClient.isUseRefreshToken() && oidcClient.isStandardTokenExchangeRefreshEnabled()) {
            return requestedTokenType;
        }

        event.detail(Details.REASON, "requested_token_type unsupported");
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "requested_token_type unsupported", Response.Status.BAD_REQUEST);
    }
}
