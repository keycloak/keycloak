package org.keycloak.tests.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.tests.broker.AbstractKcOidcBrokerTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPassMaxAgeTest extends AbstractKcOidcBrokerTest {

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PassMaxAgeConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectPage
    ErrorPage errorPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    // The standard login flow doesn't apply cleanly to this config (no client-forwarded prompt, custom
    // provider factory) - legacy disabled the same inherited test for this exact configuration and
    // overrode the other with its own max_age-specific logic instead. Same shape here.
    @Override
    @Test
    @Disabled("PassMaxAge config needs its own login flow; see testLoginWithExistingUser() below")
    public void testLogInAsUserInIDP() {
    }

    @Override
    @Test
    public void testLoginWithExistingUser() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());
        oauth.openLoginForm();
        loginPage.assertCurrent();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        timeOffSet.set(2);

        oauth.loginForm().maxAge(1).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/protocol/openid-connect/auth"),
                "Driver should be on the consumer realm page right now");

        logInWithBroker();

        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");
        loginPage.assertCurrent();

        loginPage.fillPassword(getUserPassword());
        loginPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    @Test
    void testEnforceReAuthenticationWhenMaxAgeIsSet() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());
        oauth.openLoginForm();
        loginPage.assertCurrent();

        logInWithBroker();

        loginPage.fillLogin(getUserLogin(), getUserPassword());
        loginPage.submit();

        IdentityProviderRepresentation idpRep = consumerRealm.admin()
                .identityProviders().get(getIdpAlias()).toRepresentation();
        TestKeycloakOidcIdentityProviderFactory.setIgnoreMaxAgeParam(idpRep);
        consumerRealm.admin().identityProviders().get(getIdpAlias()).update(idpRep);

        timeOffSet.set(2);

        oauth.loginForm().maxAge(1).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + CONSUMER_REALM + "/protocol/openid-connect/auth"),
                "Driver should be on the consumer realm page right now");

        logInWithBroker();

        errorPage.assertCurrent();
        Assertions.assertEquals("Unexpected error when authenticating with identity provider",
                errorPage.getError());
    }

    static class PassMaxAgeConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return configureConsumerRealm(realm,
                    createOidcIdentityProvider()
                            .providerId(TestKeycloakOidcIdentityProviderFactory.ID)
                            .attribute(IdentityProviderModel.LOGIN_HINT, "false")
                            .attribute(IdentityProviderModel.PASS_MAX_AGE, "true")
                            // Legacy KcOidcBrokerConfigurationWithPassMaxAge removed the "prompt" attribute so the
                            // broker does not force an interactive login; without this the provider always shows its
                            // login page and the ignore-max-age error path is never exercised.
                            .attribute("prompt", null));
        }
    }
}
