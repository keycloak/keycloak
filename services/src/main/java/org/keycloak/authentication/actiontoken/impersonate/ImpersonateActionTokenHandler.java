/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.actiontoken.impersonate;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

import java.net.URI;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectKeyModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;

public class ImpersonateActionTokenHandler extends AbstractActionTokenHandler<ImpersonateActionToken> {

    public ImpersonateActionTokenHandler() {
        super(ImpersonateActionToken.TOKEN_TYPE, ImpersonateActionToken.class, Messages.IMPERSONATE_ERROR,
                EventType.IMPERSONATE, Errors.INVALID_TOKEN);
    }

    @Override
    public TokenVerifier.Predicate<? super ImpersonateActionToken>[] getVerifiers(
            ActionTokenContext<ImpersonateActionToken> tokenContext) {
        return TokenUtils.predicates();
    }

    @Override
    public Response handleToken(ImpersonateActionToken token, ActionTokenContext<ImpersonateActionToken> tokenContext) {
        KeycloakSession session = tokenContext.getSession();
        RealmModel realm = tokenContext.getRealm();
        UserModel user = session.users().getUserById(realm, token.getUserId());
        ClientConnection clientConnection = tokenContext.getClientConnection();
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        if (user == null) {
            return handleImpersonationError(tokenContext, "User not found", Status.NOT_FOUND);
        }
        if (!user.isEnabled()) {
            return handleImpersonationError(tokenContext, "User is disabled", Status.BAD_REQUEST);
        }
        if (user.getServiceAccountClientLink() != null) {
            return handleImpersonationError(tokenContext, "Service accounts cannot be impersonated", Status.BAD_REQUEST);
        }

        // If the current user is already impersonating another user, we expire the existing session to prevent
        // multiple impersonations at the same time.
        UserSessionModel activeUserSession = session.getContext().getUserSession();
        if (activeUserSession != null && !activeUserSession.getUser().getId().equals(user.getId())) {
            AuthenticationManager.expireIdentityCookie(session);
            AuthenticationManager.expireRememberMeCookie(session);
            AuthenticationManager.expireAuthSessionCookie(session);
            AuthenticationManager.backchannelLogout(session, realm, activeUserSession, session.getContext().getUri(), clientConnection, session.getContext().getRequestHeaders(), true);
        }

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "impersonate", false, null, null);
        userSession.setNote(IMPERSONATOR_ID.toString(), token.getImpersonatorId());
        userSession.setNote(IMPERSONATOR_USERNAME.toString(), token.getImpersonatorUsername());

        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, session.getContext().getUri(), clientConnection);
        URI redirect = URI.create(token.getRedirectUri());
        
        event.event(EventType.IMPERSONATE)
                .session(userSession)
                .user(user)
                .detail(Details.IMPERSONATOR_REALM, token.getImpersonatorRealm())
                .detail(Details.IMPERSONATOR, token.getImpersonatorUsername())
                .success();

        SingleUseObjectKeyModel actionTokenKey = DefaultActionTokenKey.from(token.serializeKey());
        if (actionTokenKey != null) {
            SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
            singleUseObjectProvider.put(actionTokenKey.serializeKey(), actionTokenKey.getExp() - Time.currentTime(), null);
        }

        return Response.status(Response.Status.FOUND)
                .location(redirect)
                .build();
    }

    @Override
    public boolean canUseTokenRepeatedly(ImpersonateActionToken token,
            ActionTokenContext<ImpersonateActionToken> tokenContext) {
        return false;
    }

    @Override
    public boolean checkIsUserValid(ImpersonateActionToken token,
            ActionTokenContext<ImpersonateActionToken> tokenContext) {
        // Impersonations are actually performed as part of a different user session, so we don't 
        // want to check the validity of the user here.
        return false;
    }

    private Response handleImpersonationError(ActionTokenContext<?> tokenContext, String errorMessage, Status status) {
        if (tokenContext != null && tokenContext.getAuthenticationSession() != null) {
            new AuthenticationSessionManager(tokenContext.getSession())
                .removeAuthenticationSession(tokenContext.getRealm(), tokenContext.getAuthenticationSession(), true);
        }

        return ErrorPage.error(tokenContext.getSession(), null, status, errorMessage);
    }
}
