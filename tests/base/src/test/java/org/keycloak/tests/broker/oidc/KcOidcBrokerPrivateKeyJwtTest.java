package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
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
import org.keycloak.tests.broker.OidcBrokerConfigSupport;

@KeycloakIntegrationTest
public class KcOidcBrokerPrivateKeyJwtTest implements BrokerLoginTest, OidcBrokerConfigSupport {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = PrivateKeyJwtProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = PrivateKeyJwtConsumerRealmConfig.class)
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

    static class PrivateKeyJwtProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return OidcBrokerConfigSupport.configureProviderRealm(realm,
                    OidcBrokerConfigSupport.createDefaultProviderClient()
                            .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.USE_JWKS_URL, "true")
                            .attribute(OIDCConfigAttributes.JWKS_URL,
                                    "http://localhost:8080/realms/" + CONSUMER_REALM + "/protocol/openid-connect/certs"));
        }
    }

    static class PrivateKeyJwtConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return OidcBrokerConfigSupport.configureConsumerRealm(realm,
                    OidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute("clientSecret", "")
                            .attribute("clientAuthMethod", OIDCLoginProtocol.PRIVATE_KEY_JWT));
        }
    }
}
