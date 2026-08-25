package org.keycloak.tests.broker.oidc;

import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.crypto.Algorithm;
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
import org.keycloak.tests.broker.KcOidcBrokerConfigSupport;

@KeycloakIntegrationTest
public class KcOidcBrokerClientSecretJwtCustomSignAlgTest implements BrokerLoginTest, KcOidcBrokerConfigSupport {

    // BCFIPS approved mode requires at least 112 bits (14 characters) for client-secret-jwt
    private static final String CLIENT_SECRET_JWT = "atleast-14chars-password";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = JwtSecretCustomAlgProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = JwtSecretCustomAlgConsumerRealmConfig.class)
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

    static class JwtSecretCustomAlgProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureProviderRealm(realm,
                    KcOidcBrokerConfigSupport.createDefaultProviderClient()
                            .secret(CLIENT_SECRET_JWT)
                            .authenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID)
                            .attribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, Algorithm.HS384));
        }
    }

    static class JwtSecretCustomAlgConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return KcOidcBrokerConfigSupport.configureConsumerRealm(realm,
                    KcOidcBrokerConfigSupport.createOidcIdentityProvider()
                            .attribute("clientSecret", CLIENT_SECRET_JWT)
                            .attribute("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_JWT)
                            .attribute("clientAssertionSigningAlg", Algorithm.HS384));
        }
    }
}
