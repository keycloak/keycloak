package org.keycloak.tests.broker.oidc;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ProtocolMapperBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.broker.BrokerLoginTest;
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class KcOidcBrokerSubMatchIntrospectionTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = SubOverrideProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = KcOidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
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
    @Test
    public void testLogInAsUserInIDP() {
        getOAuthClient().openLoginForm();
        logInWithBroker();
        logInAsUserInIDPForFirstTime();
        errorPage.assertCurrent();
    }

    @Override
    @Test
    @Disabled("Sub mismatch prevents login flow from completing")
    public void loginWithExistingUser() {
    }

    static class SubOverrideProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(PROVIDER_REALM)
                    .eventsListeners("jboss-logging")
                    .users(UserBuilder.create(USER_LOGIN)
                            .email(USER_EMAIL)
                            .emailVerified(true)
                            .firstName("First")
                            .lastName("Last")
                            .password(USER_PASSWORD)
                            .enabled(true))
                    .clients(ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("http://localhost:8080/realms/" + CONSUMER_REALM
                                    + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*")
                            .protocolMappers(
                                    ProtocolMapperBuilder.create().name("sub-override")
                                            .protocolMapper(HardcodedClaim.PROVIDER_ID)
                                            .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                                            .config(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "sub")
                                            .config("claim.value", "overriden")
                                            .config(OIDCAttributeMapperHelper.JSON_TYPE, "String")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false")
                                            .config(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true")
                                            .build()));
        }
    }
}
