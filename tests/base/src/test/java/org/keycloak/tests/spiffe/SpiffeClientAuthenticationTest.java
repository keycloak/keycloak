package org.keycloak.tests.spiffe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.authenticators.client.SpiffeClientAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

@KeycloakIntegrationTest(config = SpiffeClientAuthenticationTest.SpiffeServerConfig.class)
public class SpiffeClientAuthenticationTest {

    @InjectClient(config = SpiffeClientConfig.class)
    protected ManagedClient client;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    private static final DummySpiffeProvider SPIFFE = new DummySpiffeProvider();

    private static final String SPIFFE_ID = "spiffe://example.org/myclient";

    @Test
    public void testValidToken() {
        String token = SPIFFE.generateToken(SPIFFE_ID, oAuthClient.getEndpoints().getIssuer());

        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest()
                .clientJwt(token).send();

        Assertions.assertTrue(response.isSuccess());
    }

    @Test
    public void testInvalidSubject() {
        String token = SPIFFE.generateToken("spiffe://example.org/invalid", oAuthClient.getEndpoints().getIssuer());

        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest()
                .clientJwt(token).send();

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals("invalid_client", response.getError());
        Assertions.assertEquals("Invalid client or Invalid client credentials", response.getErrorDescription());
    }

    @Test
    public void testInvalidTrustDomain() {
        client.updateWithCleanup(c -> c.attribute(SpiffeClientAuthenticator.TRUST_DOMAIN_KEY, "another.org"));

        String token = SPIFFE.generateToken(SPIFFE_ID, oAuthClient.getEndpoints().getIssuer());

        AccessTokenResponse response = oAuthClient.clientCredentialsGrantRequest()
                .clientJwt(token).send();

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals("invalid_client", response.getError());
        Assertions.assertEquals("Client authentication with signed JWT failed: Subject is not a SPIFFE ID, or not associated with the correct domain", response.getErrorDescription());
    }

    public static class SpiffeServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.SPIFFE_JWT);
        }
    }

    public static class SpiffeClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.clientId(SPIFFE_ID)
                    .serviceAccountsEnabled(true)
                    .authenticatorType(SpiffeClientAuthenticator.PROVIDER_ID)
                    .attribute(SpiffeClientAuthenticator.TRUST_DOMAIN_KEY, "example.org")
                    .attribute(OIDCConfigAttributes.USE_JWKS_STRING, "true")
                    .attribute(OIDCConfigAttributes.JWKS_STRING, SPIFFE.getJwksString());
        }
    }

}
