package org.keycloak.tests.account;

import java.io.IOException;

import org.keycloak.common.Profile;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = AccountConsoleDisabledTest.ServerConfig.class)
public class AccountConsoleDisabledTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @Test
    public void accountConsoleReturns404WhenDisabled() throws IOException {
        HttpGet request = new HttpGet(realm.getBaseUrl() + "/account/");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(404, response.getStatusLine().getStatusCode(),
                    "Account console should return 404 when ACCOUNT_V3 feature is disabled");
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.ACCOUNT_V3);
        }
    }
}
