package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ClientRollbackTest {

    @InjectClient(config = ClientWithSingleAttribute.class)
    ManagedClient client;

    @Test
    public void testAddAttributeWithRollback() {
        client.updateWithCleanup(u -> u.attribute("one", "two").attribute("two", "two"));

        ClientRepresentation rep = client.admin().toRepresentation();
        Assertions.assertEquals("two", rep.getAttributes().get("one"));
        Assertions.assertTrue(rep.getAttributes().containsKey("two"));
    }

    @Test
    public void testAttributeNotSet() {
        ClientRepresentation rep = client.admin().toRepresentation();
        Assertions.assertEquals("one", rep.getAttributes().get("one"));
        Assertions.assertFalse(rep.getAttributes().containsKey("two"));
    }

    public static class ClientWithSingleAttribute implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.attribute("one", "one");
        }

    }
}
