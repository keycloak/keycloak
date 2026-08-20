package org.keycloak.tests.broker;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

@KeycloakIntegrationTest
public class KcOidcBrokerJWEEcdhEsP384A192GcmTest implements EcdhEsJweBrokerConfigSupport, BrokerLoginTest {

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD,
            config = OidcBrokerConfigSupport.OidcConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver webDriver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage updateProfilePage;

    @Override public String getCurve() { return "P-384"; }
    @Override public String getEncAlg() { return JWEConstants.ECDH_ES; }
    @Override public String getEncEnc() { return JWEConstants.A192GCM; }
    @Override public String getSigAlg() { return Algorithm.ES384; }

    @Override public ManagedRealm getProviderRealm() { return providerRealm; }
    @Override public ManagedRealm getConsumerRealm() { return consumerRealm; }
    @Override public OAuthClient getOAuthClient() { return oauth; }
    @Override public ManagedWebDriver getWebDriver() { return webDriver; }
    @Override public LoginPage getLoginPage() { return loginPage; }
    @Override public IdpReviewUserProfilePage getUpdateProfilePage() { return updateProfilePage; }
}
