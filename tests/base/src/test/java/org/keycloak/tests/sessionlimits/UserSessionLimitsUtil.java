package org.keycloak.tests.sessionlimits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserSessionLimitsUtil {

    protected static final String ERROR_TO_DISPLAY = "This account has too many sessions";

    protected static final String PROVIDER_REALM = "provider";
    protected static final String CONSUMER_REALM = "consumer";
    protected static final String USER_LOGIN = "testuser";
    protected static final String USER_PASSWORD = "password";
    protected static final String USER_EMAIL = "user@localhost.com";
    protected static final String CONSUMER_CLIENT_ID = "broker-app";
    protected static final String CONSUMER_CLIENT_SECRET = "broker-app-secret";

    private UserSessionLimitsUtil() {}

    static void cleanupBeforeTest(ManagedWebDriver driver, ManagedRealm consumerRealm,
            ManagedRealm providerRealm, RunOnServerClient runOnServer) {
        deleteAllCookies(driver, consumerRealm);
        deleteAllCookies(driver, providerRealm);

        List<UserRepresentation> users = consumerRealm.admin().users().search(USER_LOGIN, true);
        for (UserRepresentation user : users) {
            consumerRealm.admin().users().get(user.getId()).logout();
            consumerRealm.admin().users().get(user.getId()).remove();
        }

        runOnServer.run(removePostBrokerFlow(CONSUMER_REALM));
    }

    static void deleteAllCookies(ManagedWebDriver driver, ManagedRealm realm) {
        driver.driver().navigate().to(realm.getBaseUrl());
        driver.cookies().deleteAll();
    }

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

    static RunOnServer configurePostBrokerFlow(String realmName, String idpAlias,
            String behavior, String realmLimit, String clientLimit) {
        return session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            AuthenticationFlowModel postBrokerFlow = new AuthenticationFlowModel();
            postBrokerFlow.setAlias("post-broker");
            postBrokerFlow.setDescription("post-broker flow with session limits");
            postBrokerFlow.setProviderId("basic-flow");
            postBrokerFlow.setTopLevel(true);
            postBrokerFlow.setBuiltIn(false);
            postBrokerFlow = realm.addAuthenticationFlow(postBrokerFlow);
            configureSessionLimits(realm, postBrokerFlow, behavior, realmLimit, clientLimit);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);
            idp.setPostBrokerLoginFlowId(postBrokerFlow.getId());
            session.identityProviders().update(idp);
        };
    }

    static RunOnServer removePostBrokerFlow(String realmName) {
        return session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            AuthenticationFlowModel flow = realm.getFlowByAlias("post-broker");
            if (flow != null) {
                session.identityProviders().getAllStream()
                        .filter(idp -> flow.getId().equals(idp.getPostBrokerLoginFlowId()))
                        .forEach(idp -> {
                            idp.setPostBrokerLoginFlowId(null);
                            session.identityProviders().update(idp);
                        });
                realm.getAuthenticationExecutionsStream(flow.getId()).forEach(execution -> {
                    String configId = execution.getAuthenticatorConfig();
                    if (configId != null) {
                        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(configId);
                        if (config != null) {
                            realm.removeAuthenticatorConfig(config);
                        }
                    }
                });
                realm.removeAuthenticationFlow(flow);
            }
        };
    }

    static RunOnServer assertSessionCount(String realmName, String username, int count) {
        return (session) -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, username);
            assertEquals(count, session.sessions().getUserSessionsStream(realm, user).count());
        };
    }
}
