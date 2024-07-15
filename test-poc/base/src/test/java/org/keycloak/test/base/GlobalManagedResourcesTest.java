package org.keycloak.test.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestClient;
import org.keycloak.test.framework.TestRealm;
import org.keycloak.test.framework.TestUser;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.DefaultClientConfig;
import org.keycloak.test.framework.realm.DefaultRealmConfig;
import org.keycloak.test.framework.realm.DefaultUserConfig;

import java.util.List;

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
        Assertions.assertEquals(DefaultRealmConfig.class.getSimpleName(), realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals(DefaultClientConfig.class.getSimpleName(), clientResource.toRepresentation().getClientId());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals(DefaultUserConfig.class.getSimpleName().toLowerCase(), userResource.toRepresentation().getUsername());
    }

}
