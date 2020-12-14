/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.ExplainedVerificationException;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.ExplainedTokenVerificationException;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenStoreProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel.Action;
import java.util.Objects;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

/**
 *
 * @author hmlnarik
 */
public class LoginActionsServiceChecks {

    private static final Logger LOG = Logger.getLogger(LoginActionsServiceChecks.class.getName());

    /**
     * This check verifies that user ID (subject) from the token matches
     * the one from the authentication session.
     */
    public static class AuthenticationSessionUserIdMatchesOneFromToken implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        public AuthenticationSessionUserIdMatchesOneFromToken(ActionTokenContext<?> context) {
            this.context = context;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();

            if (authSession == null || authSession.getAuthenticatedUser() == null
              || ! Objects.equals(t.getSubject(), authSession.getAuthenticatedUser().getId())) {
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_TOKEN, Messages.INVALID_USER);
            }

            return true;
        }
    }

    /**
     * Verifies that if authentication session exists and any action is required according to it, then it is
     * the expected one.
     *
     * If there is an action required in the session, furthermore it is not the expected one, and the required
     * action is redirection to "required actions", it throws with response performing the redirect to required
     * actions.
     */
    public static class IsActionRequired implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        private final AuthenticationSessionModel.Action expectedAction;

        public IsActionRequired(ActionTokenContext<?> context, Action expectedAction) {
            this.context = context;
            this.expectedAction = expectedAction;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();

            if (authSession != null && ! Objects.equals(authSession.getAction(), this.expectedAction.name())) {
                if (Objects.equals(AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name(), authSession.getAction())) {
                    throw new LoginActionsServiceException(
                      AuthenticationManager.nextActionAfterAuthentication(context.getSession(), authSession,
                        context.getClientConnection(), context.getRequest(), context.getUriInfo(), context.getEvent()));
                }
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_TOKEN, Messages.INVALID_CODE);
            }

            return true;
        }
    }

    /**
     * Verifies that the authentication session has not yet been converted to user session, in other words
     * that the user has not yet completed authentication and logged in.
     */
    public static <T extends JsonWebToken> void checkNotLoggedInYet(ActionTokenContext<T> context, AuthenticationSessionModel authSessionFromCookie, String authSessionId) throws VerificationException {
        if (authSessionId == null) {
            return;
        }

        UserSessionModel userSession = context.getSession().sessions().getUserSession(context.getRealm(), authSessionId);
        boolean hasNoRequiredActions =
          (userSession == null || userSession.getUser().getRequiredActionsStream().count() == 0)
          &&
          (authSessionFromCookie == null || authSessionFromCookie.getRequiredActions() == null || authSessionFromCookie.getRequiredActions().isEmpty());

        if (userSession != null && hasNoRequiredActions) {
            LoginFormsProvider loginForm = context.getSession().getProvider(LoginFormsProvider.class).setAuthenticationSession(context.getAuthenticationSession())
              .setSuccess(Messages.ALREADY_LOGGED_IN);

            if (context.getSession().getContext().getClient() == null) {
                loginForm.setAttribute(Constants.SKIP_LINK, true);
            }

            throw new LoginActionsServiceException(loginForm.createInfoPage());
        }
    }

    /**
     *  Verifies whether the user given by ID both exists in the current realm. If yes,
     *  it optionally also injects the user using the given function (e.g. into session context).
     */
    public static void checkIsUserValid(KeycloakSession session, RealmModel realm, String userId, Consumer<UserModel> userSetter) throws VerificationException {
        UserModel user = userId == null ? null : session.users().getUserById(realm, userId);

        if (user == null) {
            throw new ExplainedVerificationException(Errors.USER_NOT_FOUND, Messages.INVALID_USER);
        }

        if (! user.isEnabled()) {
            throw new ExplainedVerificationException(Errors.USER_DISABLED, Messages.INVALID_USER);
        }

        if (userSetter != null) {
            userSetter.accept(user);
        }
    }

    /**
     *  Verifies whether the user given by ID both exists in the current realm. If yes,
     *  it optionally also injects the user using the given function (e.g. into session context).
     */
    public static <T extends JsonWebToken & ActionTokenKeyModel> void checkIsUserValid(T token, ActionTokenContext<T> context) throws VerificationException {
        try {
            checkIsUserValid(context.getSession(), context.getRealm(), token.getUserId(), context.getAuthenticationSession()::setAuthenticatedUser);
        } catch (ExplainedVerificationException ex) {
            throw new ExplainedTokenVerificationException(token, ex);
        }
    }

    /**
     * Verifies whether the client denoted by client ID in token's {@code iss} ({@code issuedFor})
     * field both exists and is enabled.
     */
    public static void checkIsClientValid(KeycloakSession session, ClientModel client) throws VerificationException {
        if (client == null) {
            throw new ExplainedVerificationException(Errors.CLIENT_NOT_FOUND, Messages.UNKNOWN_LOGIN_REQUESTER);
        }

        if (! client.isEnabled()) {
            throw new ExplainedVerificationException(Errors.CLIENT_NOT_FOUND, Messages.LOGIN_REQUESTER_NOT_ENABLED);
        }
    }

    /**
     * Verifies whether the client denoted by client ID in token's {@code iss} ({@code issuedFor})
     * field both exists and is enabled.
     */
    public static <T extends JsonWebToken> void checkIsClientValid(T token, ActionTokenContext<T> context) throws VerificationException {
        String clientId = token.getIssuedFor();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        ClientModel client = authSession == null ? null : authSession.getClient();

        try {
            checkIsClientValid(context.getSession(), client);

            if (clientId != null && ! Objects.equals(client.getClientId(), clientId)) {
                throw new ExplainedTokenVerificationException(token, Errors.CLIENT_NOT_FOUND, Messages.UNKNOWN_LOGIN_REQUESTER);
            }
        } catch (ExplainedVerificationException ex) {
            throw new ExplainedTokenVerificationException(token, ex);
        }
    }

    /**
     * Verifies whether the given redirect URL, when set, is valid for the given client.
     */
    public static class IsRedirectValid implements Predicate<JsonWebToken> {

        private final ActionTokenContext<?> context;

        private final String redirectUri;

        public IsRedirectValid(ActionTokenContext<?> context, String redirectUri) {
            this.context = context;
            this.redirectUri = redirectUri;
        }

        @Override
        public boolean test(JsonWebToken t) throws VerificationException {
            if (redirectUri == null) {
                return true;
            }

            ClientModel client = context.getAuthenticationSession().getClient();

            if (RedirectUtils.verifyRedirectUri(context.getSession(), redirectUri, client) == null) {
                throw new ExplainedTokenVerificationException(t, Errors.INVALID_REDIRECT_URI, Messages.INVALID_REDIRECT_URI);
            }

            return true;
        }
    }

    /**
     *  This check verifies that current authentication session is consistent with the one specified in token.
     *  Examples:
     *  <ul>
     *      <li>1. Email from administrator with reset e-mail - token does not contain auth session ID</li>
     *      <li>2. Email from "verify e-mail" step within flow - token contains auth session ID.</li>
     *      <li>3. User clicked the link in an e-mail and gets to a new browser - authentication session cookie is not set</li>
     *      <li>4. User clicked the link in an e-mail while having authentication running - authentication session cookie
     *             is already set in the browser</li>
     *  </ul>
     *
     *  <ul>
     *      <li>For combinations 1 and 3, 1 and 4, and 2 and 3: Requests next step</li>
     *      <li>For combination 2 and 4:
     *          <ul>
     *          <li>If the auth session IDs from token and cookie match, pass</li>
     *          <li>Else if the auth session from cookie was forked and its parent auth session ID
     *              matches that of token, replaces current auth session with that of parent and passes</li>
     *          <li>Else requests restart by throwing RestartFlow exception</li>
     *          </ul>
     *      </li>
     *  </ul>
     *
     *  When the check passes, it also sets the authentication session in token context accordingly.
     *
     *  @param <T>
     */
    public static <T extends JsonWebToken> boolean doesAuthenticationSessionFromCookieMatchOneFromToken(
            ActionTokenContext<T> context, AuthenticationSessionModel authSessionFromCookie, String authSessionCompoundIdFromToken) throws VerificationException {
        if (authSessionCompoundIdFromToken == null) {
            return false;
        }


        if (Objects.equals(AuthenticationSessionCompoundId.fromAuthSession(authSessionFromCookie).getEncodedId(), authSessionCompoundIdFromToken)) {
            context.setAuthenticationSession(authSessionFromCookie, false);
            return true;
        }

        // Check if it's forked session. It would have same parent (rootSession) as our browser authenticationSession
        String parentTabId = authSessionFromCookie.getAuthNote(AuthenticationProcessor.FORKED_FROM);
        if (parentTabId == null) {
            return false;
        }


        AuthenticationSessionModel authSessionFromParent = authSessionFromCookie.getParentSession().getAuthenticationSession(authSessionFromCookie.getClient(), parentTabId);
        if (authSessionFromParent == null) {
            return false;
        }

        // It's the correct browser. We won't continue login
        // from the login form (browser flow) but from the token's flow
        // Don't expire KC_RESTART cookie at this point
        LOG.debugf("Switched to forked tab: %s from: %s . Root session: %s", authSessionFromParent.getTabId(), authSessionFromCookie.getTabId(), authSessionFromCookie.getParentSession().getId());

        context.setAuthenticationSession(authSessionFromParent, false);
        context.setExecutionId(authSessionFromParent.getAuthNote(AuthenticationProcessor.LAST_PROCESSED_EXECUTION));

        return true;
    }

    public static <T extends JsonWebToken & ActionTokenKeyModel> void checkTokenWasNotUsedYet(T token, ActionTokenContext<T> context) throws VerificationException {
        ActionTokenStoreProvider actionTokenStore = context.getSession().getProvider(ActionTokenStoreProvider.class);

        if (actionTokenStore.get(token) != null) {
            throw new ExplainedTokenVerificationException(token, Errors.EXPIRED_CODE, Messages.EXPIRED_ACTION);
        }
    }

}
