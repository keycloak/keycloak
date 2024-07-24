package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.injection.LifeCycle;

@KeycloakIntegrationTest
public class GlobalManagedResourcesTest {

    @InjectRealm(lifecycle = LifeCycle.GLOBAL)
    RealmResource realmResource;

    @InjectClient(lifecycle = LifeCycle.GLOBAL)
    ClientResource clientResource;

    @InjectUser(lifecycle = LifeCycle.GLOBAL)
    UserResource userResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("default", realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("default", clientResource.toRepresentation().getClientId());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals("default", userResource.toRepresentation().getUsername());
    }

}
