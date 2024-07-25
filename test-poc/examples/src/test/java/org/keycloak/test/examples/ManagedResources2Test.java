package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.annotations.TestClient;
import org.keycloak.test.framework.annotations.TestRealm;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ManagedRealm;

import java.util.List;

@KeycloakIntegrationTest
public class ManagedResources2Test {

    @TestRealm(lifecycle = LifeCycle.CLASS)
    ManagedRealm realm;

    @TestClient
    ClientResource clientResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("http://localhost:8080/realms/default", realm.getBaseUrl());
        Assertions.assertEquals("default", realm.getName());
        Assertions.assertEquals("default", realm.admin().toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("default", clientResource.toRepresentation().getClientId());

        List<ClientRepresentation> clients = realm.admin().clients().findByClientId("default");
        Assertions.assertEquals(1, clients.size());
    }

}
