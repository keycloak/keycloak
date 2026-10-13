package org.keycloak.tests.httpclient;

import org.keycloak.common.Profile;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = VertxHttpClientTest.HttpClientV2Config.class)
public class VertxHttpClientTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testVertxProviderIsActive() {
        runOnServer.run(session -> {
            HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
            Assertions.assertTrue(
                    provider.getClass().getName().contains("Vertx"),
                    "Expected VertxHttpClientProvider but got: " + provider.getClass().getName());
        });
    }

    @Test
    public void testHttpClientV2FeatureEnabled() {
        runOnServer.run(session -> {
            Assertions.assertTrue(Profile.isFeatureEnabled(Profile.Feature.HTTP_CLIENT_V2),
                    "HTTP_CLIENT_V2 should be enabled");
            Assertions.assertFalse(Profile.isFeatureEnabled(Profile.Feature.HTTP_CLIENT),
                    "HTTP_CLIENT (v1) should be disabled when v2 is active");
        });
    }

    static class HttpClientV2Config implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.HTTP_CLIENT_V2);
        }
    }
}
