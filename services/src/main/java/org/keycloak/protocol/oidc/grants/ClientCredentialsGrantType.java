/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ServiceAccountTokenRequestContext;
import org.keycloak.services.clientpolicy.context.ServiceAccountTokenResponseContext;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import org.jboss.logging.Logger;

import static org.keycloak.OAuth2Constants.AUTHORIZATION_DETAILS_PARAM;

/**
 * OAuth 2.0 Client Credentials Grant
 * https://datatracker.ietf.org/doc/html/rfc6749#section-4.4
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class ClientCredentialsGrantType extends OAuth2GrantTypeBase {

    private static final Logger logger = Logger.getLogger(ClientCredentialsGrantType.class);

    @Override
    public Response process(Context context) {
        setContext(context);

        if (client.isBearerOnly()) {
            event.detail(Details.REASON, "Bearer-only client not allowed to retrieve service account");
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Bearer-only client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (client.isPublicClient()) {
            event.detail(Details.REASON, "Public client not allowed to retrieve service account");
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Public client not allowed to retrieve service account", Response.Status.UNAUTHORIZED);
        }
        if (!client.isServiceAccountsEnabled()) {
            event.detail(Details.REASON, "Client not enabled to retrieve service account");
            event.error(Errors.INVALID_CLIENT);
            throw new CorsErrorResponseException(cors, OAuthErrorException.UNAUTHORIZED_CLIENT, "Client not enabled to retrieve service account", Response.Status.UNAUTHORIZED);
        }

        UserModel clientUser = session.users().getServiceAccount(client);
        if (clientUser == null) {
            event.detail(Details.REASON, "The associated service account for the client does not exist");
            event.error(Errors.USER_NOT_FOUND);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                    "The associated service account for the client does not exist", Response.Status.UNAUTHORIZED);
        }

        String clientUsername = clientUser.getUsername();
        event.detail(Details.USERNAME, clientUsername);
        event.user(clientUser);

        if (!clientUser.isEnabled()) {
            event.detail(Details.REASON, "User '" + clientUsername + "' disabled");
            event.error(Errors.USER_DISABLED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "User '" + clientUsername + "' disabled", Response.Status.UNAUTHORIZED);
        }

        String scope = getRequestedScopes();

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAuthenticatedUser(clientUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        setAuthorizationDetailsNoteIfIncluded(authSession);

        // persisting of userSession by default
        UserSessionModel.SessionPersistenceState sessionPersistenceState = UserSessionModel.SessionPersistenceState.PERSISTENT;

        if (!useRefreshToken()) {
            // we don't want to store a session hence we mark it as transient, see KEYCLOAK-9551
            sessionPersistenceState = UserSessionModel.SessionPersistenceState.TRANSIENT;
        }

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(authSession.getParentSession().getId(), realm, clientUser, clientUsername,
                clientConnection.getRemoteHost(), ServiceAccountConstants.CLIENT_AUTH, false, null, null, sessionPersistenceState);
        event.session(userSession);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(session, userSession, authSession);
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, context.getGrantType());

        // Notes about client details
        userSession.setNote(ServiceAccountConstants.CLIENT_ID_SESSION_NOTE, client.getClientId()); // This is for backwards compatibility
        userSession.setNote(ServiceAccountConstants.CLIENT_ID, client.getClientId());
        userSession.setNote(ServiceAccountConstants.CLIENT_HOST, clientConnection.getRemoteHost());
        userSession.setNote(ServiceAccountConstants.CLIENT_ADDRESS, clientConnection.getRemoteHost());

        try {
            session.clientPolicy().triggerOnEvent(new ServiceAccountTokenRequestContext(formParams, clientSessionCtx.getClientSession()));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        updateUserSessionFromClientAuth(userSession);

        // client credentials grant always removes the online session
        clientSessionCtx.getClientSession().setNote(AuthenticationProcessor.FIRST_OFFLINE_ACCESS, Boolean.TRUE.toString());
        return createTokenResponse(clientUser, userSession, clientSessionCtx, scope, true,
                responseBuilder -> new ServiceAccountTokenResponseContext(formParams, clientSessionCtx.getClientSession(), responseBuilder));
    }

    @Override
    public EventType getEventType() {
        return EventType.CLIENT_LOGIN;
    }

    @Override
    protected boolean useRefreshToken() {
        return clientConfig.isUseRefreshTokenForClientCredentialsGrant();
    }

    /**
     * Setting a client note with authorization_details to support custom protocol mappers using RAR (Rich Authorization Request)
     * until RAR is fully implemented.
     */
    private void setAuthorizationDetailsNoteIfIncluded(AuthenticationSessionModel authSession) {
        String authorizationDetails = formParams.getFirst(AUTHORIZATION_DETAILS_PARAM);
        if (authorizationDetails != null) {
            authSession.setClientNote(AUTHORIZATION_DETAILS_PARAM, authorizationDetails);
        }
    }
}
