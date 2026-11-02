/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.DefaultActionToken;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultRequiredActions;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.services.managers.AuthenticationManager.FORCED_REAUTHENTICATION;
import static org.keycloak.services.managers.AuthenticationManager.PASSWORD_VALIDATED;
import static org.keycloak.services.managers.AuthenticationManager.SSO_AUTH;

public class AuthenticatorUtil {

    private static final Logger logger = Logger.getLogger(AuthenticatorUtil.class);

    // It is used for identification of note included in authentication session for storing callback provider factories
    public static String CALLBACKS_FACTORY_IDS_NOTE = "callbacksFactoryProviderIds";


    public static boolean isSSOAuthentication(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(SSO_AUTH));
    }

    public static boolean isForcedReauthentication(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(FORCED_REAUTHENTICATION));
    }

    public static boolean isPasswordValidated(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(PASSWORD_VALIDATED));
    }

    public static boolean isForkedFlow(AuthenticationSessionModel authSession) {
        return authSession.getAuthNote(AuthenticationProcessor.FORKED_FROM) != null;
    }

    /**
     * Set authentication session note for callbacks defined for {@link AuthenticationFlowCallbackFactory) factories
     *
     * @param authSession   authentication session
     * @param authFactoryId authentication factory ID which should be added to the authentication session note
     */
    public static void setAuthCallbacksFactoryIds(AuthenticationSessionModel authSession, String authFactoryId) {
        if (authSession == null || StringUtil.isBlank(authFactoryId)) return;

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            boolean containsProviderId = callbacksFactories.equals(authFactoryId) ||
                    callbacksFactories.contains(Constants.CFG_DELIMITER + authFactoryId) ||
                    callbacksFactories.contains(authFactoryId + Constants.CFG_DELIMITER);

            if (!containsProviderId) {
                authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, callbacksFactories + Constants.CFG_DELIMITER + authFactoryId);
            }
        } else {
            authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, authFactoryId);
        }
    }

    /**
     * Get set of Authentication factories IDs defined in authentication session as CALLBACKS_FACTORY_IDS_NOTE
     *
     * @param authSession authentication session
     * @return set of factories IDs
     */
    public static Set<String> getAuthCallbacksFactoryIds(AuthenticationSessionModel authSession) {
        if (authSession == null) return Collections.emptySet();

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            String[] split = callbacksFactories.split(Constants.CFG_DELIMITER);
            Set<String> set = new HashSet<>(split.length);
            for (String s : split) {
                set.add(s);
            }
            return Collections.unmodifiableSet(set);
        } else {
            return Collections.emptySet();
        }
    }


    /**
     * @param realm
     * @param flowId
     * @param providerId
     * @return all executions of given "provider_id" type. This is deep (recursive) obtain of executions of the particular flow
     */
    public static List<AuthenticationExecutionModel> getExecutionsByType(RealmModel realm, String flowId, String providerId) {
        List<AuthenticationExecutionModel> executions = new LinkedList<>();
        realm.getAuthenticationExecutionsStream(flowId).forEach(authExecution -> {
            if (providerId.equals(authExecution.getAuthenticator())) {
                executions.add(authExecution);
            } else if (authExecution.isAuthenticatorFlow() && authExecution.getFlowId() != null) {
                executions.addAll(getExecutionsByType(realm, authExecution.getFlowId(), providerId));
            }
        });
        return executions;
    }

    /**
     * Useful if we need to find top-level flow from executionModel
     *
     * @param realm
     * @param executionModel
     * @return Top parent flow corresponding to given executionModel.
     */
    public static AuthenticationFlowModel getTopParentFlow(RealmModel realm, AuthenticationExecutionModel executionModel) {
        if (executionModel.getParentFlow() != null) {
            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(executionModel.getParentFlow());
            if (flow == null) throw new IllegalStateException("Flow '" + executionModel.getParentFlow() + "' referenced from execution '" + executionModel.getId() + "' not found in realm " + realm.getName());
            if (flow.isTopLevel()) return flow;

            AuthenticationExecutionModel execution = realm.getAuthenticationExecutionByFlowId(flow.getId());
            if (execution == null) throw new IllegalStateException("Not found execution referenced by flow '" + flow.getId() + "' in realm " + realm.getName());
            return getTopParentFlow(realm, execution);
        } else {
            throw new IllegalStateException("Execution '" + executionModel.getId() + "' does not have parent flow in realm " + realm.getName());
        }
    }



    /**
     * Logouts all sessions that are different to the current authentication session
     * managed in the action context.
     *
     * @param context The required action context
     */
    public static void logoutOtherSessions(RequiredActionContext context) {
        EventBuilder event = context.getEvent().clone()
                .detail(Details.LOGOUT_TRIGGERED_BY_REQUIRED_ACTION, context.getAction());
        logoutOtherSessions(context.getSession(), context.getRealm(), context.getUser(),
                context.getAuthenticationSession(), context.getConnection(), context.getHttpRequest(), event);
    }

    /**
     * Logouts all sessions that are different to the current authentication session
     * managed in the action token context.
     *
     * @param token The action token
     * @param context The required action token context
     */
    public static void logoutOtherSessions(DefaultActionToken token, ActionTokenContext<? extends DefaultActionToken> context) {
        EventBuilder event = context.getEvent().clone()
                .detail(Details.LOGOUT_TRIGGERED_BY_ACTION_TOKEN, token.getActionId());
        logoutOtherSessions(context.getSession(), context.getRealm(), context.getAuthenticationSession().getAuthenticatedUser(),
                context.getAuthenticationSession(), context.getClientConnection(), context.getRequest(), event);
    }

    private static void logoutOtherSessions(KeycloakSession session, RealmModel realm, UserModel user,
            AuthenticationSessionModel authSession, ClientConnection conn, HttpRequest req, EventBuilder event) {
        session.sessions().getUserSessionsStream(realm, user)
                .filter(s -> !Objects.equals(s.getId(), authSession.getParentSession().getId()))
                .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                .forEach(s -> {
                    backchannelLogout(session, realm, conn, req, event, s);
                });

        if (!Profile.isFeatureEnabled(Profile.Feature.LOGOUT_ALL_SESSIONS_V1)) {
            session.sessions().getOfflineUserSessionsStream(realm, user)
                    .filter(s -> !Objects.equals(s.getId(), authSession.getParentSession().getId()))
                    .collect(Collectors.toList()) // collect to avoid concurrent modification as backchannelLogout removes the user sessions.
                    .forEach(s -> {
                        backchannelLogout(session, realm, conn, req, event, s);
                    });
        }

        session.authenticationSessions().removeRootAuthenticationSessionsByAuthenticatedUser(realm, user,
                authSession.getParentSession().getId());
    }

    private static void backchannelLogout(KeycloakSession session, RealmModel realm, ClientConnection conn, HttpRequest req, EventBuilder event, UserSessionModel s) {
        AuthenticationManager.backchannelLogout(session, realm, s, session.getContext().getUri(),
                conn, req.getHttpHeaders(), true);

        event.clone().event(EventType.LOGOUT)
                .session(s)
                .user(s.getUser())
                .success();
    }

    /**
     * @param session
     * @return all credential providers available
     */
    public static Stream<CredentialProvider> getCredentialProviders(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(CredentialProvider.class)
                .filter(f -> Types.supports(CredentialProvider.class, f, CredentialProviderFactory.class))
                .map(f -> session.getProvider(CredentialProvider.class, f.getId()));
    }

    /**
     * Get the list of credentials used in the authentication.
     * @param authSession The authentication session
     * @return The immutable list of credentials (empty returned if none)
     */
    public static List<String> getAuthnCredentials(AuthenticationSessionModel authSession) {
        final String authnCredentials = authSession.getAuthNote(AuthenticationProcessor.AUTHN_CREDENTIALS);
        if (authnCredentials != null) {
            try {
                return Arrays.asList(JsonSerialization.readValue(authnCredentials, String[].class));
            } catch (IOException e) {
                logger.warn("Invalid array stored as authn.credentials: " + authnCredentials);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Adds the credentials to the credentials used in the authentication session.
     * @param authSession The authentication session
     * @param credential The credential to add
     */
    public static void addAuthCredential(AuthenticationSessionModel authSession, String credential) {
        List<String> authnCredentials = new LinkedList<>(getAuthnCredentials(authSession));
        authnCredentials.add(credential);
        try {
            authSession.setAuthNote(AuthenticationProcessor.AUTHN_CREDENTIALS, JsonSerialization.writeValueAsString(authnCredentials));
        } catch (IOException e) {
            // not expected
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves the user session whose auth time is used by the {@code max_auth_age} check of an application-initiated action.
     * <p>
     * The user session is looked up by the ID of the root authentication session first. When the root authentication session was created with
     * its own ID (e.g. restart of the login or an action token with an existing SSO session), no user session with that ID exists. In that case
     * this falls back to the session of the identity cookie, which is the session {@code CookieAuthenticator} used when it authenticated the
     * user via SSO. Without the fallback the auth time would be missing and the re-authentication check would fire on every submit.
     *
     * @param session the Keycloak session
     * @param authSession the current authentication session
     * @return the user session, or {@code null} if none can be resolved
     */
    public static UserSessionModel getUserSessionForKcAction(KeycloakSession session, AuthenticationSessionModel authSession) {
        UserSessionModel userSession = new AuthenticationSessionManager(session).getUserSession(authSession);
        if (userSession != null) {
            return userSession;
        }
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, authSession.getRealm(), true);
        if (authResult == null || authResult.session() == null) {
            return null;
        }
        UserModel authenticatedUser = authSession.getAuthenticatedUser();
        if (authenticatedUser != null && !authenticatedUser.getId().equals(authResult.user().getId())) {
            return null;
        }
        return authResult.session();
    }

    /**
     * Checks if re-authentication is required for an application-initiated action (kc_action).
     * @param session the Keycloak session
     * @param userSession the current user session, or {@code null} if it does not exist (e.g. expired or removed). In that case only the
     *                    auth-session timestamp is considered and a missing timestamp results in re-authentication being required
     * @param authSession the current authentication session, or {@code null}. It must be the authentication session of
     *                    {@code session.getContext()}: the default {@link RequiredActionProvider#getMaxAuthAge(KeycloakSession)}
     *                    resolves the max auth age configuration of the action from the context's authentication session
     * @param provider the required action provider instance, or {@code null} to resolve it from {@code kc_action}
     * @param actionId the expected action ID to match against {@code kc_action}, or {@code null} to check any action
     * @return {@code true} if re-authentication is required because the user's auth time exceeds the action's max auth age, {@code false} otherwise
     */
    public static boolean isReAuthRequiredForKcAction(KeycloakSession session, UserSessionModel userSession, AuthenticationSessionModel authSession, RequiredActionProvider provider, String actionId) {
        if (authSession == null) {
            return false;
        }
        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction == null) {
            return false;
        }

        if (actionId != null) {
            String normalizedKcAction = DefaultRequiredActions.getDefaultRequiredActionCaseInsensitively(kcAction);
            String normalizedActionId = DefaultRequiredActions.getDefaultRequiredActionCaseInsensitively(actionId);

            if (!normalizedKcAction.equalsIgnoreCase(normalizedActionId)) {
                return false;
            }
        }

        if (provider == null) {
            String providerId = null;
            String resolvedAction = DefaultRequiredActions.getDefaultRequiredActionCaseInsensitively(kcAction);
            if (session.getContext() != null && session.getContext().getRealm() != null) {
                RequiredActionProviderModel model = session.getContext().getRealm().getRequiredActionProviderByAlias(resolvedAction);
                if (model != null) {
                    providerId = model.getProviderId();
                }
            }
            if (providerId == null) {
                providerId = resolvedAction;
            }
            provider = session.getProvider(RequiredActionProvider.class, providerId);
            if (provider == null) {
                return false;
            }
        }
        int maxAuthAge = provider.getMaxAuthAge(session);

        String authTimeStr = authSession.getAuthNote(AuthenticationManager.AUTH_TIME);
        boolean isAuthSessionNote = true;
        if (authTimeStr == null) {
            authTimeStr = userSession == null ? null : userSession.getNote(AuthenticationManager.AUTH_TIME);
            isAuthSessionNote = false;
        }

        if (maxAuthAge == 0) {
            return !isAuthSessionNote;
        } else if (maxAuthAge < 0) {
            maxAuthAge = Constants.KC_ACTION_MAX_AGE;
        }

        long authTime = authTimeStr == null ? 0 : Long.parseLong(authTimeStr);
        return authTime + maxAuthAge < Time.currentTime();
    }
}
