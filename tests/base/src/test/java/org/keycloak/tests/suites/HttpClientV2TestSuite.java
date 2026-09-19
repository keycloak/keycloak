package org.keycloak.tests.suites;

import org.keycloak.common.Profile;
import org.keycloak.testframework.injection.SuiteSupport;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.admin.tracing.TracingTest;
import org.keycloak.tests.broker.IdentityProviderStoreTokenV1Test;
import org.keycloak.tests.broker.IdpCreateUserIfUniqueAuthenticatorTest;
import org.keycloak.tests.broker.SamlIdentityProviderStoreTokenV1Test;
import org.keycloak.tests.broker.oidc.KcOidcBrokerClientSecretBasicAuthTest;
import org.keycloak.tests.broker.oidc.KcOidcBrokerClientSecretJwtTest;
import org.keycloak.tests.broker.oidc.KcOidcBrokerPkceTest;
import org.keycloak.tests.broker.oidc.KcOidcBrokerPrivateKeyJwtTest;
import org.keycloak.tests.broker.oidc.KcOidcBrokerTest;
import org.keycloak.tests.broker.saml.KcSamlBrokerTest;
import org.keycloak.tests.httpclient.VertxHttpClientTest;

import org.junit.platform.suite.api.AfterSuite;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        // OIDC broker — exercises getString (JWKS, discovery), token exchange POST, userinfo GET
        KcOidcBrokerTest.class,
        KcOidcBrokerClientSecretBasicAuthTest.class,
        KcOidcBrokerClientSecretJwtTest.class,
        KcOidcBrokerPrivateKeyJwtTest.class,
        KcOidcBrokerPkceTest.class,
        // SAML broker — exercises SAML artifact resolution, metadata fetch
        KcSamlBrokerTest.class,
        // Token store — exercises token exchange, userinfo via outgoing HTTP
        IdentityProviderStoreTokenV1Test.class,
        SamlIdentityProviderStoreTokenV1Test.class,
        // IdP creation — exercises OIDC discovery download
        IdpCreateUserIfUniqueAuthenticatorTest.class,
        // Tracing — verifies OTel factory variant
        TracingTest.class,
        // Wiring — verifies v2 provider is active
        VertxHttpClientTest.class,
        // TODO add x509 revocation tests (CRL via getInputStream, OCSP via postBinary) after #50770
})
public class HttpClientV2TestSuite {

    @BeforeSuite
    public static void beforeSuite() {
        SuiteSupport.startSuite()
                .registerServerConfig(HttpClientV2ServerConfig.class);
    }

    @AfterSuite
    public static void afterSuite() {
        SuiteSupport.stopSuite();
    }

    public static class HttpClientV2ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.HTTP_CLIENT_V2);
        }
    }
}
