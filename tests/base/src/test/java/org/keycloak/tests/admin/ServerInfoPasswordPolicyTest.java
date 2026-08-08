package org.keycloak.tests.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.PasswordPolicyTypeRepresentation;
import org.keycloak.representations.info.ServerInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.policy.TestConfiguredPasswordPolicyProviderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class ServerInfoPasswordPolicyTest {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void testPasswordPolicyHelpText() {
        ServerInfoRepresentation info = adminClient.serverInfo().getInfo();
        assertNotNull(info.getPasswordPolicies());

        PasswordPolicyTypeRepresentation configuredPolicy = findPolicy(info, TestConfiguredPasswordPolicyProviderFactory.ID);
        assertEquals(TestConfiguredPasswordPolicyProviderFactory.HELP_TEXT, configuredPolicy.getHelpText());
        assertEquals("Test Configured Policy", configuredPolicy.getDisplayName());

        PasswordPolicyTypeRepresentation lengthPolicy = findPolicy(info, "length");
        Assertions.assertNull(lengthPolicy.getHelpText());
    }

    private PasswordPolicyTypeRepresentation findPolicy(ServerInfoRepresentation info, String id) {
        return info.getPasswordPolicies().stream()
                .filter(policy -> id.equals(policy.getId()))
                .findFirst().orElseThrow(() -> new RuntimeException("Not found password policy with ID '" + id + "'"));
    }
}
