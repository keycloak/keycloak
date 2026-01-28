package org.keycloak.testsuite.sessionlimits;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testsuite.broker.AbstractInitializedBaseBrokerTest;

import org.junit.Test;

import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractUserSessionLimitsBrokerTest extends AbstractInitializedBaseBrokerTest {
    @Test
    public void testSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        configureFlow(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
    }

    @Test
    public void testSessionCountExceededAndOldestSessionRemovedFirstBrokerLoginFlow() {
        configureFlow(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "0", "1");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedFirstBrokerLoginFlow() {
        configureFlow(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "1", "0");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
    }

    @Test
    public void testRealmSessionCountExceededAndOldestFirstBrokerLoginFlow() {
        configureFlow(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION, "1", "0");
        loginTwiceAndVerifyBehavior(UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
    }

    private void configureFlow(String behavior, String realmLimit, String clientLimit)
    {
        String realmName = bc.consumerRealmName();
        String idpAlias = bc.getIDPAlias();
        testingClient.server().run(session -> {
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
        });
    }

    private void loginTwiceAndVerifyBehavior(String behavior) {
        logInAsUserInIDPForFirstTime();

        deleteAllCookiesForRealm(bc.consumerRealmName());
        deleteAllCookiesForRealm(bc.providerRealmName());

        logInAsUserInIDP();

        if (UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION.equals(behavior)) {
            appPage.assertCurrent();
            testingClient.server(bc.consumerRealmName()).run(assertSessionCount(bc.consumerRealmName(), bc.getUserLogin(), 1));
        }
        else if (UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION.equals(behavior)) {
            errorPage.assertCurrent();
            assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
        }
        else {
            fail("Invalid behavior " + behavior);
        }
    }
}
