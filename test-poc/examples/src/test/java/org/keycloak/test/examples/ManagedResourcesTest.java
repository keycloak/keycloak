package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.annotations.TestClient;
import org.keycloak.test.framework.annotations.TestRealm;
import org.keycloak.test.framework.annotations.TestUser;
import org.keycloak.test.framework.injection.LifeCycle;

import java.util.List;

@KeycloakIntegrationTest
public class ManagedResourcesTest {

    @TestRealm(lifecycle = LifeCycle.CLASS)
    RealmResource realmResource;

    @TestClient
    ClientResource clientResource;

    @TestUser
    UserResource userResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals(ManagedResourcesTest.class.getSimpleName(), realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals(ManagedResourcesTest.class.getSimpleName(), clientResource.toRepresentation().getClientId());

        List<ClientRepresentation> clients = realmResource.clients().findByClientId(ManagedResourcesTest.class.getSimpleName());
        Assertions.assertEquals(1, clients.size());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals(ManagedResourcesTest.class.getSimpleName().toLowerCase(), userResource.toRepresentation().getUsername());
    }

}
