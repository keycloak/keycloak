package org.keycloak.tests.admin.finegrainedadminv1;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.models.Constants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = FineGrainedAdminWithTokenExchangeTest.FineGrainedWithTokenExchangeServerConf.class)
public class FineGrainedAdminWithTokenExchangeTest extends AbstractFineGrainedAdminTest {

    /**
     * KEYCLOAK-7406
     */
    @Test
    public void testWithTokenExchange() {
        String exchanged = checkTokenExchange(true);
        try (Keycloak client = adminClientFactory.create()
                .realm("master").authorization(exchanged).clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            Assertions.assertNotNull(client.realm("master").roles().get("offline_access"));
        }
    }

    public static class FineGrainedWithTokenExchangeServerConf implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            config.features(Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);

            return config;
        }
    }
}
