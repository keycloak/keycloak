package org.keycloak.test.examples;

import java.util.Optional;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.representations.info.FeatureRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = CustomConfigTest.CustomServerConfig.class)
public class CustomConfigTest {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void testPasskeyFeatureEnabled() {
        Optional<FeatureRepresentation> passKeysFeature = adminClient.serverInfo().getInfo().getFeatures().stream().filter(f -> f.getName().equals(Profile.Feature.PASSKEYS.name())).findFirst();
        Assertions.assertTrue(passKeysFeature.isPresent());
        Assertions.assertTrue(passKeysFeature.get().isEnabled());
    }

    public static class CustomServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PASSKEYS);
        }

    }

}
