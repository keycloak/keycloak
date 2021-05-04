package org.keycloak.authentication.authenticators.limit;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class UserSessionsLimiterAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getAuthenticatorConfig() == null) {
            throw new IllegalStateException("Invalid configuration: No configuration was found.");
        }
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();

        int attrMaxSessions = getRequiredConfig(Integer::parseInt,
                UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_SESSIONS, config);
        UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions attrSort =
                getRequiredConfig(UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions::valueOfLabel,
                        UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT, config);
        boolean attrOfflineSessions = getRequiredConfig(Boolean::parseBoolean,
                UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SESSIONS, config);

        int attrMaxOfflineSessions = -1;
        UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions attrOfflineSort = null;
        if (attrOfflineSessions) {
            attrMaxOfflineSessions = getRequiredConfig(Integer::parseInt,
                    UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_MAX_OFFLINE_SESSIONS, config);
            attrOfflineSort =
                    getRequiredConfig(UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions::valueOfLabel,
                            UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_OFFLINE_SORT, config);
        }

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        deleteOldSessions(context.getSession().sessions().getUserSessionsStream(realm, user),
                getComparator(attrSort), attrMaxSessions, realm, context);

        if (attrOfflineSessions) {
            int maxOfflineSessions = anOfflineSessionWillBeCreated(realm, context) ? attrMaxOfflineSessions
                    : (attrMaxOfflineSessions + 1);

            deleteOldSessions(context.getSession().sessions().getOfflineUserSessionsStream(realm, user),
                    getComparator(attrOfflineSort), maxOfflineSessions, realm, context);
        }
        context.success();
    }

    private <R> R getRequiredConfig(Function<String, R> parseFunction, String configParam, Map<String, String> config) {
        String confString = config.get(configParam);
        if (confString == null) {
            throw new IllegalArgumentException(
                    String.format("Invalid configuration: Attribute %s is null.", configParam));
        }
        try {
            R conf = parseFunction.apply(confString);
            if (conf == null) {
                throw new IllegalArgumentException(
                        String.format("Invalid configuration: Value %s for attribute %s is not parsable.", confString,
                                configParam));
            }
            return conf;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format(
                    "Invalid configuration: Value %s for attribute %s is not parsable: %s: %s", confString,
                    configParam, e.getClass(), e.getLocalizedMessage()), e);
        }
    }

    private Comparator<UserSessionModel> getComparator(
            UserSessionsLimiterAuthenticatorFactory.ConfAttrSortOptions sort) {
        switch (sort) {
            case STARTED:
                return (s1, s2) -> s2.getStarted() - s1.getStarted();
            case LAST_ACCESS:
                return (s1, s2) -> s2.getLastSessionRefresh() - s1.getLastSessionRefresh();
            default:
                throw new IllegalStateException(
                        String.format("Invalid configuration: attribute %s=%s is not supported.",
                                UserSessionsLimiterAuthenticatorFactory.CONF_ATTR_SORT, sort));
        }
    }

    private void deleteOldSessions(Stream<UserSessionModel> sessions, Comparator<UserSessionModel> comparator,
            int attrMaxSessions, RealmModel realm,
            AuthenticationFlowContext context) {

        long skip = attrMaxSessions - 1L;
        skip = skip < 0 ? 0 : skip;

        sessions.sequential().sorted(comparator).skip(skip).forEachOrdered(userSessionModel -> {
            AuthenticationManager.backchannelLogout(
                    context.getSession(),
                    context.getSession().getContext().getRealm(),
                    userSessionModel,
                    context.getSession().getContext().getUri(),
                    context.getSession().getContext().getConnection(),
                    context.getSession().getContext().getRequestHeaders(),
                    true,
                    userSessionModel.isOffline());
            new EventBuilder(realm, context.getSession(), context.getConnection())
                    .event(EventType.LOGOUT)
                    .user(context.getUser())
                    .session(userSessionModel)
                    .detail(Details.REASON, "session_limit_reached")
                    .success();
        });
    }

    private boolean anOfflineSessionWillBeCreated(RealmModel realm, AuthenticationFlowContext context) {
        Optional<ClientScopeModel> offlineAccessScope = realm.getClientScopesStream()
                .filter(scope -> scope.getName().equals(OAuth2Constants.OFFLINE_ACCESS)).findAny();
        if (offlineAccessScope.isPresent()) {
            String offlineScopeId = offlineAccessScope.get().getId();
            if (context.getAuthenticationSession().getClient().getClientScopes(true).values().stream()
                    .anyMatch(scope -> scope.getId().equals(offlineScopeId))) {
                return true;
            }
            String requestedScopesString = context.getAuthenticationSession().getClientNote("scope");
            if (requestedScopesString != null && !requestedScopesString.isEmpty()) {
                return Arrays.asList(requestedScopesString.split(" ")).contains(OAuth2Constants.OFFLINE_ACCESS);
            }
        }
        return false;
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
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
