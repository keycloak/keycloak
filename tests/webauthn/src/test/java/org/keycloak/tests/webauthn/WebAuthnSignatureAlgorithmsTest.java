package org.keycloak.tests.webauthn;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = WebAuthnSignatureAlgorithmsTest.ServerConfig.class)
public class WebAuthnSignatureAlgorithmsTest {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void availableSignatureAlgorithms() {
        assertEquals(expectedAlgorithms(), adminClient.serverInfo().getInfo().getCryptoInfo().getWebAuthnSignatureAlgorithms());
    }

    protected List<String> expectedAlgorithms() {
        return List.of("ES256", "ES384", "ES512", "RS256", "RS384", "RS512", "Ed25519", "RS1");
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.WEBAUTHN_ML_DSA);
        }
    }
}
