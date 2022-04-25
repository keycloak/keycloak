package org.keycloak.testsuite.sessionlimits;

import org.junit.Test;
import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.broker.AbstractInitializedBaseBrokerTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractUserSessionLimitsBrokerTest extends AbstractInitializedBaseBrokerTest {
    private static final String ERROR_TO_DISPLAY = "This account has too many sessions";

    @Test
    public void testSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() throws Exception {
        configureFlow(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
    }

    @Test
    public void testSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() throws Exception {
        configureFlow(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "0", "1");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() throws Exception {
        configureFlow(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "1", "0");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
    }

    @Test
    public void testRealmSessionCountExceededAndOldestFirstBrokerLoginFlow() throws Exception {
        configureFlow(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "1", "0");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
    }

    private void configureFlow(String behavior, String realmLimit, String clientLimit)
    {
        String realmName = bc.consumerRealmName();
        String idpAlias = bc.getIDPAlias();
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            AuthenticationFlowModel postBrokerFlow = new AuthenticationFlowModel();
            postBrokerFlow.setAlias("post-broker");
            postBrokerFlow.setDescription("post-broker flow with session limits");
            postBrokerFlow.setProviderId("basic-flow");
            postBrokerFlow.setTopLevel(true);
            postBrokerFlow.setBuiltIn(false);
            postBrokerFlow = realm.addAuthenticationFlow(postBrokerFlow);

            AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
            execution.setParentFlow(postBrokerFlow.getId());
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
            configModel.setAlias("user-session-limits-" + postBrokerFlow.getId());
            configModel = realm.addAuthenticatorConfig(configModel);
            execution.setAuthenticatorConfig(configModel.getId());
            realm.addAuthenticatorExecution(execution);

            IdentityProviderModel idp = realm.getIdentityProviderByAlias(idpAlias);
            idp.setPostBrokerLoginFlowId(postBrokerFlow.getId());
            realm.updateIdentityProvider(idp);
        });
    }

    private void loginTwiceAndVerifyBehavior(String behavior) {
        logInAsUserInIDPForFirstTime();
        assertLoggedInAccountManagement();

        deleteAllCookiesForRealm(bc.consumerRealmName());
        deleteAllCookiesForRealm(bc.providerRealmName());

        logInAsUserInIDP();

        if (UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION.equals(behavior)) {
            assertLoggedInAccountManagement();
            assertSessionCount(bc.consumerRealmName(), bc.getUserLogin(), 1);
        }
        else if (UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION.equals(behavior)) {
            errorPage.assertCurrent();
            assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
        }
        else {
            fail("Invalid behavior " + behavior);
        }
    }

    private void assertSessionCount(String realmName, String username, int count) {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, username);
            assertEquals(count, session.sessions().getUserSessionsStream(realm, user).count());
        });
    }
}
