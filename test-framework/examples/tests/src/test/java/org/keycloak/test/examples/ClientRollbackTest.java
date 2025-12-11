package org.keycloak.test.examples;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;


@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ClientRollbackTest {

    @InjectClient(config = ClientWithSingleAttribute.class)
    ManagedClient client;

    @Test
    public void test1UpdateWithRollback() {
        client.updateWithCleanup(u -> u.attribute("one", "two").attribute("two", "two"));
        client.updateWithCleanup(u -> u.adminUrl("http://something"));
        client.updateWithCleanup(u -> u.redirectUris("http://something"));
        client.updateWithCleanup(u -> u.attribute("three", "three"));
    }

    @Test
    public void test2CheckRollback() {
        ClientRepresentation current = client.admin().toRepresentation();

        Assertions.assertEquals("one", current.getAttributes().get("one"));
        Assertions.assertFalse(current.getAttributes().containsKey("two"));
        Assertions.assertFalse(current.getAttributes().containsKey("three"));
        Assertions.assertNull(current.getAdminUrl());
        Assertions.assertTrue(current.getRedirectUris().isEmpty());
    }

    public static class ClientWithSingleAttribute implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.attribute("one", "one");
        }

    }
}
