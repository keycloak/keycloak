package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.annotations.TestClient;
import org.keycloak.test.framework.annotations.TestRealm;
import org.keycloak.test.framework.annotations.TestUser;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.DefaultClientConfig;
import org.keycloak.test.framework.realm.DefaultRealmConfig;
import org.keycloak.test.framework.realm.DefaultUserConfig;

@KeycloakIntegrationTest
public class GlobalManagedResourcesTest {

    @TestRealm(lifecycle = LifeCycle.GLOBAL)
    RealmResource realmResource;

    @TestClient(lifecycle = LifeCycle.GLOBAL)
    ClientResource clientResource;

    @TestUser(lifecycle = LifeCycle.GLOBAL)
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
