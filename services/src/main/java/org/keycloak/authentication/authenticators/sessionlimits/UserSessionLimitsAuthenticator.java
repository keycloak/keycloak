package org.keycloak.authentication.authenticators.sessionlimits;

import java.util.Collections;
import org.jboss.logging.Logger;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.events.Errors;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import static org.keycloak.utils.LockObjectsForModification.lockObjectsForModification;

public class UserSessionLimitsAuthenticator implements Authenticator {

    private static Logger logger = Logger.getLogger(UserSessionLimitsAuthenticator.class);
    public static final String SESSION_LIMIT_EXCEEDED = "sessionLimitExceeded";
    private static String realmEventDetailsTemplate = "Realm session limit exceeded. Realm: %s, Realm limit: %s. Session count: %s, User id: %s";
    private static String clientEventDetailsTemplate = "Client session limit exceeded. Realm: %s, Client limit: %s. Session count: %s, User id: %s";
    protected KeycloakSession session;

    String behavior;

    public UserSessionLimitsAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        Map<String, String> config = authenticatorConfig.getConfig();

        // Get the configuration for this authenticator
        behavior = config.get(UserSessionLimitsAuthenticatorFactory.BEHAVIOR);
        int userRealmLimit = getIntConfigProperty(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, config);
        int userClientLimit = getIntConfigProperty(UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, config);

        if (context.getRealm() != null && context.getUser() != null) {

            // Get the session count in this realm for this specific user
            List<UserSessionModel> userSessionsForRealm = lockObjectsForModification(session, () -> session.sessions().getUserSessionsStream(context.getRealm(), context.getUser()).collect(Collectors.toList()));
            int userSessionCountForRealm = userSessionsForRealm.size();

            // Get the session count related to the current client for this user
            ClientModel currentClient = context.getAuthenticationSession().getClient();
            logger.debugf("session-limiter's current keycloak clientId: %s", currentClient.getClientId());

            List<UserSessionModel> userSessionsForClient = getUserSessionsForClientIfEnabled(userSessionsForRealm, currentClient, userClientLimit);
            int userSessionCountForClient = userSessionsForClient.size();
            logger.debugf("session-limiter's configured realm session limit: %s", userRealmLimit);
            logger.debugf("session-limiter's configured client session limit: %s", userClientLimit);
            logger.debugf("session-limiter's count of total user sessions for the entire realm (could be apps other than web apps): %s", userSessionCountForRealm);
            logger.debugf("session-limiter's count of total user sessions for this keycloak client: %s", userSessionCountForClient);

            // First check if the user has too many sessions in this realm
            if (exceedsLimit(userSessionCountForRealm, userRealmLimit)) {
                logger.infof("Too many session in this realm for the current user. Session count: %s", userSessionCountForRealm);
                String eventDetails = String.format(realmEventDetailsTemplate, context.getRealm().getName(), userRealmLimit, userSessionCountForRealm, context.getUser().getId());
                handleLimitExceeded(context, userSessionsForRealm, eventDetails);
            } // otherwise if the user is still allowed to create a new session in the realm, check if this applies for this specific client as well.
            else if (exceedsLimit(userSessionCountForClient, userClientLimit)) {
                logger.infof("Too many sessions related to the current client for this user. Session count: %s", userSessionCountForRealm);
                String eventDetails = String.format(clientEventDetailsTemplate, context.getRealm().getName(), userClientLimit, userSessionCountForClient, context.getUser().getId());
                handleLimitExceeded(context, userSessionsForClient, eventDetails);
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
        return count > limit - 1;
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
            return Collections.EMPTY_LIST;
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

    private void handleLimitExceeded(AuthenticationFlowContext context, List<UserSessionModel> userSessions, String eventDetails) {
        switch (behavior) {
            case UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION:
                logger.info("Denying new session");
                String errorMessage = Optional.ofNullable(context.getAuthenticatorConfig())
                        .map(AuthenticatorConfigModel::getConfig)
                        .map(f -> f.get(UserSessionLimitsAuthenticatorFactory.ERROR_MESSAGE))
                        .orElse(SESSION_LIMIT_EXCEEDED);

                context.getEvent().error(Errors.GENERIC_AUTHENTICATION_ERROR);
                Response challenge = null;
                if(context.getFlowPath() == null) {
                    OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation(Errors.GENERIC_AUTHENTICATION_ERROR, errorMessage);
                    challenge = Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity(errorRep).type(MediaType.APPLICATION_JSON_TYPE).build();
                }
                else {
                    challenge = context.form().setError(errorMessage).createErrorPage(Response.Status.FORBIDDEN);
                }
                context.failure(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, challenge, eventDetails, errorMessage);
                break;

            case UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION:
                logger.info("Terminating oldest session");
                logoutOldestSession(userSessions);
                context.success();
                break;
        }
    }

    private void logoutOldestSession(List<UserSessionModel> userSessions) {
        logger.info("Logging out oldest session");
        Optional<UserSessionModel> oldest = userSessions.stream().sorted(Comparator.comparingInt(UserSessionModel::getLastSessionRefresh)).findFirst();
        oldest.ifPresent(userSession -> AuthenticationManager.backchannelLogout(session, userSession, true));
    }
}
