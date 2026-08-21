package org.keycloak.tests.broker;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.testsuite.util.AccountHelper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class KcOidcBrokerPassMaxAgeTest implements OidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PassMaxAgeConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @InjectPage
    ErrorPage errorPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Override
    public ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    public ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oauth;
    }

    @Override
    public ManagedWebDriver getWebDriver() {
        return webDriver;
    }

    @Override
    public LoginPage getLoginPage() {
        return loginPage;
    }

    @Override
    public IdpReviewUserProfilePage getUpdateProfilePage() {
        return updateProfilePage;
    }

    @Test
    void loginWithExistingUser() {
        logInAsUserInIDP();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();

        oauth.openLoginForm();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess(), "Should be logged in");
        logoutFromConsumerRealm();
        AccountHelper.logout(providerRealm.admin(), getUserLogin());
        oauth.openLoginForm();
        loginPage.assertCurrent();

        oauth.openLoginForm();
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

        oauth.openLoginForm();
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
            return OidcBrokerConfigSupport.configureConsumerRealm(realm,
                    OidcBrokerConfigSupport.createOidcIdentityProvider()
                            .providerId(TestKeycloakOidcIdentityProviderFactory.ID)
                            .attribute(IdentityProviderModel.LOGIN_HINT, "false")
                            .attribute(IdentityProviderModel.PASS_MAX_AGE, "true"));
        }
    }
}
