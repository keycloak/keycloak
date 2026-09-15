package org.keycloak.tests.authz.services;

import java.util.List;

import org.keycloak.representations.idm.authorization.PolicyProviderRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.authz.services.config.DeployedScriptPolicyServerConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = DeployedScriptPolicyServerConfig.class)
public class DeployedScriptPolicyTest {

    private static final String DEPLOYED_SCRIPT_TYPE = "script-scripts/default-policy.js";
    private static final String EXPECTED_DESCRIPTION = "A policy that grants access only for users within this realm";

    @InjectClient(config = DefaultResourceServerConfig.class)
    ManagedClient client;

    @Test
    public void testDescriptionInPolicyProviders() {
        List<PolicyProviderRepresentation> providers = client.admin().authorization().policies().policyProviders();

        PolicyProviderRepresentation scriptProvider = providers.stream()
                .filter(p -> DEPLOYED_SCRIPT_TYPE.equals(p.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(scriptProvider, "Deployed script policy provider should be listed");
        assertEquals(EXPECTED_DESCRIPTION, scriptProvider.getDescription());
    }

    @Test
    public void testDescriptionOnCreatedPolicy() {
        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName("Test Default Policy");
        policy.setType(DEPLOYED_SCRIPT_TYPE);

        client.admin().authorization().policies().create(policy).close();

        PolicyRepresentation created = client.admin().authorization().policies()
                .findByName("Test Default Policy");

        assertNotNull(created, "Created policy should be found by name");
        assertEquals(EXPECTED_DESCRIPTION, created.getDescription());
    }
}
