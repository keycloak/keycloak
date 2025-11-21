package org.keycloak.authentication.authenticators.sessionlimits;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

public class UserSessionLimitsAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(UserSessionLimitsAuthenticator.class);
    public static final String SESSION_LIMIT_EXCEEDED = "sessionLimitExceeded";
    protected final KeycloakSession session;

    String behavior;

    public UserSessionLimitsAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            throw new AuthenticationFlowException("No configuration found of 'User Session Count Limiter' authenticator. Please make sure to configure this authenticator in your authentication flow in the realm '" + context.getRealm().getName() + "'!"
                    , AuthenticationFlowError.INTERNAL_ERROR);
        }
        Map<String, String> config = authenticatorConfig.getConfig();

        // get the current client being authenticated
        ClientModel currentClient = context.getAuthenticationSession().getClient();
        logger.debugf("session-limiter's current keycloak clientId: %s", currentClient.getClientId());

        // check if new user and client session are needed
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(context.getSession(), context.getRealm(), true);
        final boolean newUserSession = authResult == null || authResult.session() == null;
        final boolean newClientSession = authResult == null || authResult.session() == null
                || authResult.session().getAuthenticatedClientSessionByClient(currentClient.getId()) == null;

        // Get the configuration for this authenticator
        behavior = config.get(UserSessionLimitsAuthenticatorFactory.BEHAVIOR);
        int userRealmLimit = getIntConfigProperty(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, config);
        int userClientLimit = getIntConfigProperty(UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, config);

        if (context.getRealm() != null && context.getUser() != null) {

            // Get the session count in this realm for this specific user
            List<UserSessionModel> userSessionsForRealm = session.sessions()
                    .getUserSessionsStream(context.getRealm(), context.getUser())
                    .collect(Collectors.toList());
            int userSessionCountForRealm = userSessionsForRealm.size();

            // Get the session count related to the current client for this user
            List<UserSessionModel> userSessionsForClient = getUserSessionsForClientIfEnabled(userSessionsForRealm, currentClient, userClientLimit);
            int userSessionCountForClient = userSessionsForClient.size();
            logger.debugf("session-limiter's configured realm session limit: %s", userRealmLimit);
            logger.debugf("session-limiter's configured client session limit: %s", userClientLimit);
            logger.debugf("session-limiter's count of total user sessions for the entire realm (could be apps other than web apps): %s", userSessionCountForRealm);
            logger.debugf("session-limiter's count of total user sessions for this keycloak client: %s", userSessionCountForClient);

            // First check if the user has too many sessions in this realm
            if (newUserSession && exceedsLimit(userSessionCountForRealm, userRealmLimit)) {
                logger.infof("Too many session in this realm for the current user. Session count: %s", userSessionCountForRealm);
                String eventDetails = String.format("Realm session limit exceeded. Realm: %s, Realm limit: %s. Session count: %s, User id: %s",
                        context.getRealm().getName(), userRealmLimit, userSessionCountForRealm, context.getUser().getId());

                var removedClientSessions = handleLimitExceeded(context, userSessionsForClient, eventDetails, userClientLimit);
                if (exceedsLimit(userSessionCountForRealm - removedClientSessions.size(), userRealmLimit))
                {
                    List<UserSessionModel> remainingSessionsToBeRemoved = userSessionsForRealm
                            .stream()
                            .filter(userSessionModel -> !removedClientSessions.contains(userSessionModel))
                            .collect(Collectors.toList());
                    handleLimitExceeded(context, remainingSessionsToBeRemoved, eventDetails, userRealmLimit);
                }
            } // otherwise if the user is still allowed to create a new session in the realm, check if this applies for this specific client as well.
            else if (newClientSession && exceedsLimit(userSessionCountForClient, userClientLimit)) {
                logger.infof("Too many sessions related to the current client for this user. Session count: %s", userSessionCountForClient);
                String eventDetails = String.format("Client session limit exceeded. Realm: %s, Client limit: %s. Session count: %s, User id: %s",
                        context.getRealm().getName(), userClientLimit, userSessionCountForClient, context.getUser().getId());
                handleLimitExceeded(context, userSessionsForClient, eventDetails, userClientLimit);
            } else {
                context.success();
            }
        } else {
            context.success();
        }
    }

    private boolean exceedsLimit(long count, long limit) {
        if (limit <= 0) { // if limit is zero or negative, consider the limit disabled
            return false;
        }
        return getNumberOfSessionsThatNeedToBeLoggedOut(count, limit) > 0;
    }

    private long getNumberOfSessionsThatNeedToBeLoggedOut(long count, long limit) {
        return count - (limit - 1);
    }

    private int getIntConfigProperty(String key, Map<String, String> config) {
        String value = config.get(key);
        if (StringUtil.isBlank(value)) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    private List<UserSessionModel> getUserSessionsForClientIfEnabled(List<UserSessionModel> userSessionsForRealm, ClientModel currentClient, int userClientLimit) {
        // Only count this users sessions for this client only in case a limit is configured, otherwise skip this costly operation.
        if (userClientLimit <= 0) {
            return Collections.emptyList();
        }
        logger.debugf("total user sessions for this keycloak client will not be counted. Will be logged as 0 (zero)");
        List<UserSessionModel> userSessionsForClient = userSessionsForRealm.stream().filter(session -> session.getAuthenticatedClientSessionByClient(currentClient.getId()) != null).collect(Collectors.toList());
        return userSessionsForClient;
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }

    /**
     * @return A list of logged-out user sessions, if any.
     */
    private List<UserSessionModel> handleLimitExceeded(AuthenticationFlowContext context, List<UserSessionModel> userSessions, String eventDetails, long limit) {
        switch (behavior) {
            case UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION:
                logger.info("Denying new session");
                String errorMessage = Optional.ofNullable(context.getAuthenticatorConfig())
                        .map(AuthenticatorConfigModel::getConfig)
                        .map(f -> f.get(UserSessionLimitsAuthenticatorFactory.ERROR_MESSAGE))
                        .orElse(SESSION_LIMIT_EXCEEDED);

                context.getEvent().error(Errors.GENERIC_AUTHENTICATION_ERROR);
                Response challenge = context.form().setError(errorMessage).createErrorPage(Response.Status.FORBIDDEN);
                context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, challenge, eventDetails, errorMessage);
                return Collections.emptyList();

            case UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION:
                logger.info("Terminating oldest session");
                var removedSessions = logoutOldestSessions(userSessions, limit);
                context.success();
                return removedSessions;
        }

        return Collections.emptyList();
    }

    /**
     * @return A list of logged-out user sessions, if any.
     */
    private List<UserSessionModel> logoutOldestSessions(List<UserSessionModel> userSessions, long limit) {
        long numberOfSessionsThatNeedToBeLoggedOut = getNumberOfSessionsThatNeedToBeLoggedOut(userSessions.size(), limit);
        if (numberOfSessionsThatNeedToBeLoggedOut == 1) {
            logger.info("Logging out oldest session");
        } else {
            logger.infof("Logging out oldest %s sessions", numberOfSessionsThatNeedToBeLoggedOut);
        }

        List<UserSessionModel> userSessionsToBeRemoved = userSessions
            .stream()
            .sorted(Comparator.comparingInt(UserSessionModel::getLastSessionRefresh))
            .limit(numberOfSessionsThatNeedToBeLoggedOut)
            .toList();

        for (UserSessionModel userSession : userSessionsToBeRemoved) {
            AuthenticationManager.backchannelLogout(session, userSession, true);
        }

        return userSessionsToBeRemoved;
    }
}
