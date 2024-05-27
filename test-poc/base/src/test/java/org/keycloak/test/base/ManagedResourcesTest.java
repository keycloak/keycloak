package org.keycloak.test.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestClient;
import org.keycloak.test.framework.TestRealm;

import java.util.List;

@KeycloakIntegrationTest
public class ManagedResourcesTest {

    @TestRealm
    RealmResource realmResource;

    @TestClient
    ClientResource clientResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("ManagedResourcesTest", realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("ManagedResourcesTest", clientResource.toRepresentation().getClientId());

        List<ClientRepresentation> clients = realmResource.clients().findByClientId("ManagedResourcesTest");
        Assertions.assertEquals(1, clients.size());
    }

}
