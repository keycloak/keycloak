package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;

import java.util.List;

@KeycloakIntegrationTest
public class DefaultConfig2Test {

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void testAdminClient() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        Assertions.assertFalse(realms.isEmpty());
    }

}
