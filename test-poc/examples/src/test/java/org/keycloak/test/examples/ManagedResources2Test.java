package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.annotations.Client;
import org.keycloak.test.framework.annotations.Realm;
import org.keycloak.test.framework.injection.LifeCycle;

import java.util.List;

@KeycloakIntegrationTest
public class ManagedResources2Test {

    @Realm(lifecycle = LifeCycle.CLASS)
    RealmResource realmResource;

    @Client
    ClientResource clientResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("default", realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("default", clientResource.toRepresentation().getClientId());

        List<ClientRepresentation> clients = realmResource.clients().findByClientId("default");
        Assertions.assertEquals(1, clients.size());
    }

}
