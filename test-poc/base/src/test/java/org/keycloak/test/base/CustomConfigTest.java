package org.keycloak.test.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.representations.info.FeatureRepresentation;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestAdminClient;
import org.keycloak.test.framework.server.smallrye_config.CustomServerConfigSource;

import java.util.Optional;

@KeycloakIntegrationTest(config = CustomServerConfigSource.class)
public class CustomConfigTest {

    @TestAdminClient
    Keycloak adminClient;

    @Test
    public void testUpdateEmailFeatureEnabled() {
        Optional<FeatureRepresentation> updateEmailFeature = adminClient.serverInfo().getInfo().getFeatures().stream().filter(f -> f.getName().equals(Profile.Feature.UPDATE_EMAIL.name())).findFirst();
        Assertions.assertTrue(updateEmailFeature.isPresent());
        Assertions.assertTrue(updateEmailFeature.get().isEnabled());
    }

}
