package org.keycloak.tests.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class KcOidcBrokerNoLoginHintTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = NoLoginHintConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

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

    @Override
    public void loginUser() {
        getOAuthClient().loginForm().loginHint(USER_EMAIL).open();

        logInWithBroker();

        webDriver.waiting().until(d -> webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"));
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM + "/"),
                "Driver should be on the provider realm page right now");
        Assertions.assertTrue(loginPage.getUsername().isBlank(),
                "User identifier should not be filled");

        logInAsUserInIDPForFirstTime();
        updateAccountInformation();
        assertUserCreatedInConsumerRealm();
    }

    static class NoLoginHintConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureConsumerRealm(realm,
                    KcOidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute(IdentityProviderModel.LOGIN_HINT, "false"));
        }
    }
}
