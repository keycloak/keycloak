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

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

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
        EventBuilder event = tokenContext.getEvent();
        event.event(EventType.IMPERSONATE)
                .detail(Details.IMPERSONATOR_REALM, token.getImpersonatorRealm())
                .detail(Details.IMPERSONATOR, token.getImpersonatorUsername());

        // Normally, action tokens are invalidated after the intended required action is executed. However, since
        // impersonation doesn't have a required action and is instead executed immediately when the token is handled,
        // we need to invalidate the token here to prevent it from being used multiple times.
        if (!AuthenticationManager.invalidateActionToken(session, token.serializeKey(), 0L)) {
            return handleImpersonationError(tokenContext, Errors.EXPIRED_CODE, Status.BAD_REQUEST);
        }

        if (user == null) {
            return handleImpersonationError(tokenContext, Errors.USER_NOT_FOUND, Status.NOT_FOUND);
        }
        if (!user.isEnabled()) {
            return handleImpersonationError(tokenContext, Errors.USER_DISABLED, Status.BAD_REQUEST);
        }
        if (user.getServiceAccountClientLink() != null) {
            return handleImpersonationError(tokenContext, Errors.NOT_ALLOWED, Status.BAD_REQUEST);
        }

        // When impersonating within the same realm, the administrator's own session is terminated here (at redemption
        // time), because their identity cookie is about to be replaced with the impersonated user's session. The
        // session to terminate is carried in the token instead of being resolved from the request context, as this
        // handler opts out of the identity-cookie authentication performed by LoginActionsServiceChecks#checkIsUserValid.
        if (token.getImpersonatorSessionId() != null) {
            UserSessionModel impersonatorSession = session.sessions().getUserSession(realm, token.getImpersonatorSessionId());
            if (impersonatorSession != null && !impersonatorSession.getUser().getId().equals(user.getId())) {
                AuthenticationManager.expireIdentityCookie(session);
                AuthenticationManager.expireRememberMeCookie(session);
                AuthenticationManager.expireAuthSessionCookie(session);
                AuthenticationManager.backchannelLogout(session, realm, impersonatorSession, session.getContext().getUri(),
                        clientConnection, session.getContext().getRequestHeaders(), true);
            }
        }

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "impersonate", false, null, null);
        userSession.setNote(IMPERSONATOR_ID.toString(), token.getImpersonatorId());
        userSession.setNote(IMPERSONATOR_USERNAME.toString(), token.getImpersonatorUsername());

        AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession, session.getContext().getUri(), clientConnection);
        URI redirect = URI.create(token.getRedirectUri());

        event.session(userSession)
                .user(user)
                .success();

        // The fresh authentication session created for processing this action token has served its purpose and would
        // otherwise linger (together with its browser cookie) until it times out.
        removeAuthenticationSession(tokenContext);

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

    private Response handleImpersonationError(ActionTokenContext<?> tokenContext, String error, Status status) {
        removeAuthenticationSession(tokenContext);

        tokenContext.getEvent().event(EventType.IMPERSONATE).error(error);

        return ErrorPage.error(tokenContext.getSession(), null, status, Messages.IMPERSONATE_ERROR);
    }

    private static void removeAuthenticationSession(ActionTokenContext<?> tokenContext) {
        if (tokenContext.getAuthenticationSession() != null) {
            new AuthenticationSessionManager(tokenContext.getSession())
                .removeAuthenticationSession(tokenContext.getRealm(), tokenContext.getAuthenticationSession(), true);
        }
    }
}
