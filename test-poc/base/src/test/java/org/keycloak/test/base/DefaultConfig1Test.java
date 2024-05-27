package org.keycloak.test.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestAdminClient;

import java.util.List;

@KeycloakIntegrationTest
public class DefaultConfig1Test {

    @TestAdminClient
    Keycloak adminClient;

    @Test
    public void testAdminClient() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        Assertions.assertFalse(realms.isEmpty());
    }

}
