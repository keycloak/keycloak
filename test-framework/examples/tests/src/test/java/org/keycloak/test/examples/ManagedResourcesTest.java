package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;

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
        Assertions.assertEquals("default", realm.getName());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("default", client.getClientId());
        Assertions.assertEquals("default", realm.admin().clients().get(client.getId()).toRepresentation().getClientId());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals("default", user.getUsername());
        Assertions.assertEquals("default", realm.admin().users().get(user.getId()).toRepresentation().getUsername());
    }

}
