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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

/**
 * Provider for token-exchange subject impersonation where subject of the token is changed.
 *
 * This is Keycloak proprietary and it is not related to standard token-exchange impersonation described in
 * the specification https://datatracker.ietf.org/doc/html/rfc8693 where the subject in the tokens are not changed. That one is covered by {@link StandardTokenExchangeProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SubjectImpersonationTokenExchangeProvider extends AbstractTokenExchangeProvider {

    @Override
    public boolean supports(TokenExchangeContext context) {
        String requestedSubject = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        return requestedSubject != null;
    }

    @Override
    protected Response tokenExchange() {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        ClientConnection clientConnection = context.getClientConnection();
        Cors cors = context.getCors();
        ClientModel client = context.getClient();
        EventBuilder event = context.getEvent();

        UserModel tokenUser = null;
        AccessToken token = null;

        String subjectToken = context.getParams().getSubjectToken();
        if (subjectToken != null) {
            String subjectTokenType = context.getParams().getSubjectTokenType();

            if (subjectTokenType != null && !subjectTokenType.equals(OAuth2Constants.ACCESS_TOKEN_TYPE)) {
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

            tokenUser = authResult.getUser();
            token = authResult.getToken();
        }

        String requestedSubject = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_SUBJECT);
        boolean disallowOnHolderOfTokenMismatch = true;

        event.detail(Details.REQUESTED_SUBJECT, requestedSubject);
        UserModel requestedUser = session.users().getUserByUsername(realm, requestedSubject);
        if (requestedUser == null) {
            requestedUser = session.users().getUserById(realm, requestedSubject);
        }

        if (requestedUser == null) {
            // We always returned access denied to avoid username fishing
            event.detail(Details.REASON, "requested_subject not found");
            event.error(Errors.NOT_ALLOWED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

        }

        if (token != null) {
            event.detail(Details.IMPERSONATOR, tokenUser.getUsername());
            // for this case, the user represented by the token, must have permission to impersonate.
            AdminAuth auth = new AdminAuth(realm, token, tokenUser, client);
            if (!AdminPermissions.evaluator(session, realm, auth).users().canImpersonate(requestedUser, client)) {
                event.detail(Details.REASON, "subject not allowed to impersonate");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
            }
        } else {
            // no token is being exchanged, this is a direct exchange.  Client must be authenticated, not public, and must be allowed
            // to impersonate
            if (client.isPublicClient()) {
                event.detail(Details.REASON, "public clients not allowed");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);

            }
            if (!AdminPermissions.management(session, realm).users().canClientImpersonate(client, requestedUser)) {
                event.detail(Details.REASON, "client not allowed to impersonate");
                event.error(Errors.NOT_ALLOWED);
                throw new CorsErrorResponseException(cors, OAuthErrorException.ACCESS_DENIED, "Client not allowed to exchange", Response.Status.FORBIDDEN);
            }

            // see https://issues.redhat.com/browse/KEYCLOAK-5492
            disallowOnHolderOfTokenMismatch = false;
        }

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, requestedUser, requestedUser.getUsername(), clientConnection.getRemoteAddr(), "impersonate", false, null, null);
        if (tokenUser != null) {
            userSession.setNote(IMPERSONATOR_ID.toString(), tokenUser.getId());
            userSession.setNote(IMPERSONATOR_USERNAME.toString(), tokenUser.getUsername());
        }

        tokenUser = requestedUser;

        String requestedIssuer = context.getFormParams().getFirst(OAuth2Constants.REQUESTED_ISSUER);
        if (requestedIssuer == null) {
            return exchangeClientToClient(tokenUser, userSession, token, disallowOnHolderOfTokenMismatch);
        } else {
            try {
                // Subject impersonation supports "internal to external" exchange as well for now
                return exchangeToIdentityProvider(tokenUser, userSession, requestedIssuer);
            } finally {
                if (subjectToken == null) { // we are naked! So need to clean up user session
                    try {
                        session.sessions().removeUserSession(realm, userSession);
                    } catch (Exception ignore) {

                    }
                }
            }
        }
    }
}
