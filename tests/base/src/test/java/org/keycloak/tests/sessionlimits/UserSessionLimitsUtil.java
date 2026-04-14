package org.keycloak.tests.sessionlimits;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserSessionLimitsUtil {
    protected static final String ERROR_TO_DISPLAY = "This account has too many sessions";

    protected static void configureSessionLimits(RealmModel realm, AuthenticationFlowModel flow, String behavior, String realmLimit, String clientLimit) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(UserSessionLimitsAuthenticatorFactory.USER_SESSION_LIMITS);
        execution.setPriority(30);
        execution.setAuthenticatorFlow(false);

        AuthenticatorConfigModel configModel = new AuthenticatorConfigModel();
        Map<String, String> sessionAuthenticatorConfig = new HashMap<>();
        sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.BEHAVIOR, behavior);
        sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, realmLimit);
        sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, clientLimit);
        sessionAuthenticatorConfig.put(UserSessionLimitsAuthenticatorFactory.ERROR_MESSAGE, ERROR_TO_DISPLAY);
        configModel.setConfig(sessionAuthenticatorConfig);
        configModel.setAlias("user-session-limits-" + flow.getId());
        configModel = realm.addAuthenticatorConfig(configModel);
        execution.setAuthenticatorConfig(configModel.getId());
        realm.addAuthenticatorExecution(execution);
    }

    static RunOnServer assertClientSessionCount(String realmName, String username, String clientId, int count) {
        return (session) -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, username);
            assertEquals(count, session.sessions()
                    .readOnlyStreamUserSessions(realm, realm.getClientByClientId(clientId), -1, -1)
                    .filter(userSessionModel -> userSessionModel.getUser().getId().equals(user.getId()))
                    .count());
        };
    }

    static RunOnServer assertSessionCount(String realmName, String username, int count) {
        return (session) -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, username);
            assertEquals(count, session.sessions().getUserSessionsStream(realm, user).count());
        };
    }


    /**
     * Delete all cookies for a specific realm.
     * This method follows the WebDriver best practice of navigating to a page within the realm
     * before deleting cookies to ensure all realm-scoped cookies are accessible for deletion.
     *
     * @param driver The WebDriver instance
     * @param keycloakUrls The Keycloak URLs helper
     * @param realmName The name of the realm
     */
    public static void deleteAllCookiesForRealm(ManagedWebDriver driver, KeycloakUrls keycloakUrls, String realmName) {
        // Navigate to a blank page in the realm to ensure cookies are properly scoped
        String realmUrl = keycloakUrls.getBase() + "/realms/" + realmName;
        driver.driver().navigate().to(realmUrl + "/testing/blank");
        driver.cookies().deleteAll();
    }
}
