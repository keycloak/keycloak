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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
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

    protected void validateAudience(AccessToken token, boolean disallowOnHolderOfTokenMismatch, List<ClientModel> targetAudienceClients) {
        ClientModel tokenHolder = token == null ? null : realm.getClientByClientId(token.getIssuedFor());
        //reject if the requester-client is not in the audience of the subject token
        if (!client.equals(tokenHolder)) {
            forbiddenIfClientIsNotWithinTokenAudience(token, null);
        }
        for (ClientModel targetClient : targetAudienceClients) {
            boolean isClientTheAudience = targetClient.equals(client);
            if (isClientTheAudience) {
                if (client.isPublicClient()) {
                    // public clients can only exchange on to themselves if they are the token holder
                    forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
                } else if (!client.equals(tokenHolder)) {
                    // confidential clients can only exchange to themselves if they are within the token audience
                    forbiddenIfClientIsNotWithinTokenAudience(token, tokenHolder);
                }
            } else {
                if (client.isPublicClient()) {
                    // public clients can not exchange tokens from other client
                    forbiddenIfClientIsNotTokenHolder(disallowOnHolderOfTokenMismatch, tokenHolder);
                }
            }
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

    protected void setClientToContext(List<ClientModel> targetAudienceClients) {
        // The client requesting exchange is set in the context
        session.getContext().setClient(client);
    }

    protected Response exchangeClientToOIDCClient(UserModel targetUser, UserSessionModel targetUserSession, String requestedTokenType,
                                                  List<ClientModel> targetAudienceClients, String scope) {
        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = createSessionModel(targetUserSession, rootAuthSession, targetUser, client, scope);

        if (targetUserSession == null) {
            // if no session is associated with a subject_token, a transient session is created to only allow building a token to the audience
            targetUserSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, targetUser, targetUser.getUsername(),
                    clientConnection.getRemoteAddr(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
        }

        event.session(targetUserSession);

        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, targetUserSession, authSession);

        updateUserSessionFromClientAuth(targetUserSession);

        if (params.getAudience() != null && !targetAudienceClients.isEmpty()) {
            clientSessionCtx.setAttribute(Constants.REQUESTED_AUDIENCE_CLIENT_IDS, targetAudienceClients.stream().map(ClientModel::getId).toArray(String[]::new));
        }

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager.responseBuilder(realm, client, event, this.session, targetUserSession, clientSessionCtx)
                .generateAccessToken();

        updateTokenFromAudienceParameter(responseBuilder);

        if (targetUserSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) {
            responseBuilder.getAccessToken().setSessionId(null);
        }

        String issuedTokenType;
        if (requestedTokenType.equals(OAuth2Constants.REFRESH_TOKEN_TYPE)
                && OIDCAdvancedConfigWrapper.fromClientModel(client).isUseRefreshToken()
                && targetUserSession.getPersistenceState() != UserSessionModel.SessionPersistenceState.TRANSIENT) {
            responseBuilder.generateRefreshToken();
            responseBuilder.getRefreshToken().issuedFor(client.getClientId());
            issuedTokenType = OAuth2Constants.REFRESH_TOKEN_TYPE;
        } else {
            issuedTokenType = OAuth2Constants.ACCESS_TOKEN_TYPE;
        }

        String scopeParam = clientSessionCtx.getClientSession().getNote(OAuth2Constants.SCOPE);
        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        AccessTokenResponse res = responseBuilder.build();
        res.setOtherClaims(OAuth2Constants.ISSUED_TOKEN_TYPE, issuedTokenType);

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

    /**
     * Update token audience. Default implementation removes the audiences, which were not provided in the "audience" parameter (in case "audience" parameter was provided)
     *
     * @param responseBuilder response builder
     */
    protected void updateTokenFromAudienceParameter(TokenManager.AccessTokenResponseBuilder responseBuilder) {
        if (params.getAudience() != null) {
            AccessToken newToken = responseBuilder.getAccessToken();
            List<String> newTokenAudiences = newToken.getAudience() == null ? new ArrayList<>() : new ArrayList<>(List.of(newToken.getAudience()));

            List<String> audiencesToRemove = new ArrayList<>(newTokenAudiences);
            for (String audienceParam : params.getAudience()) {
                boolean removed = audiencesToRemove.remove(audienceParam);
                // TODO: Should we reject the request if some audience requested by the "audience" parameter is not available in the token?
//                if (!removed) {
//                    event.detail(Details.REASON, "Requested audience not available");
//                    event.error(Errors.INVALID_REQUEST);
//                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Requested audience not available", Response.Status.BAD_REQUEST);
//                }
            }

            // Filter audiences from the "aud" claim and from client roles
            for (String audienceToRemove : audiencesToRemove) {
                newToken.getResourceAccess().remove(audienceToRemove);
            }
            newTokenAudiences.removeAll(audiencesToRemove);
            newToken.audience(newTokenAudiences.toArray(new String[] {}));
        }
    }
}
