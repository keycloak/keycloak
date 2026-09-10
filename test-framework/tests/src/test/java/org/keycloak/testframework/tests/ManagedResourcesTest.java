package org.keycloak.testframework.tests;

import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ManagedResourcesTest {

    @InjectRealm(lifecycle = LifeCycle.CLASS)
    ManagedRealm realm;

    @InjectClient
    ManagedClient client;

    @InjectUser
    ManagedUser user;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("http://localhost:8080/realms/default", realm.getBaseUrl());
        Assertions.assertEquals(realm.admin().toRepresentation().getId(), realm.getId());
        Assertions.assertEquals("default", realm.getName());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("default", client.getClientId());
        Assertions.assertEquals("default", realm.admin().clients().get(client.getId()).toRepresentation().getClientId());

        List<ClientRepresentation> clients = realm.admin().clients().findByClientId("default");
        Assertions.assertEquals(1, clients.size());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals("default", user.getUsername());
        Assertions.assertEquals("default", realm.admin().users().get(user.getId()).toRepresentation().getUsername());
    }

}
