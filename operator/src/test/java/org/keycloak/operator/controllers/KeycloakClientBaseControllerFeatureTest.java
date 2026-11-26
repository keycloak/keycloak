package org.keycloak.operator.controllers;

import java.util.List;

import org.keycloak.operator.crds.v2beta1.deployment.Keycloak;
import org.keycloak.operator.crds.v2beta1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2beta1.deployment.spec.FeatureSpec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeycloakClientBaseControllerFeatureTest {

    private Keycloak keycloakWithFeatures(List<String> enabledFeatures) {
        Keycloak keycloak = new Keycloak();
        keycloak.setSpec(new KeycloakSpec());
        if (enabledFeatures != null) {
            FeatureSpec featureSpec = new FeatureSpec();
            featureSpec.setEnabledFeatures(enabledFeatures);
            keycloak.getSpec().setFeatureSpec(featureSpec);
        }
        return keycloak;
    }

    @Test
    public void testNoFeatureSpec() {
        assertFalse(KeycloakClientBaseController.hasFeatureEnabled(keycloakWithFeatures(null)));
    }

    @Test
    public void testEmptyEnabledFeatures() {
        assertFalse(KeycloakClientBaseController.hasFeatureEnabled(keycloakWithFeatures(List.of())));
    }

    @Test
    public void testClientAdminApiV2Enabled() {
        assertTrue(KeycloakClientBaseController.hasFeatureEnabled(
                keycloakWithFeatures(List.of(KeycloakClientBaseController.CLIENT_ADMIN_API_V2))));
    }

    @Test
    public void testClientAdminApiV2EnabledAmongOthers() {
        assertTrue(KeycloakClientBaseController.hasFeatureEnabled(
                keycloakWithFeatures(List.of("docker", KeycloakClientBaseController.CLIENT_ADMIN_API_V2))));
    }

    @Test
    public void testPreviewAlone() {
        assertTrue(KeycloakClientBaseController.hasFeatureEnabled(keycloakWithFeatures(List.of("preview"))));
    }

    @Test
    public void testPreviewWithOtherFeaturesNotEnough() {
        // "preview" only counts when it is the sole enabled feature
        assertFalse(KeycloakClientBaseController.hasFeatureEnabled(keycloakWithFeatures(List.of("preview", "docker"))));
    }

    @Test
    public void testUnrelatedFeature() {
        assertFalse(KeycloakClientBaseController.hasFeatureEnabled(keycloakWithFeatures(List.of("docker"))));
    }
}
