package org.keycloak.test.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.test.framework.KeycloakIntegrationTest;
import org.keycloak.test.framework.TestClient;
import org.keycloak.test.framework.TestRealm;
import org.keycloak.test.framework.server.KeycloakTestServerConfig;

import java.util.Map;

@KeycloakIntegrationTest
public class RealmTest {

    @TestRealm
    RealmResource realmResource;

    @TestClient
    ClientResource clientResource;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("RealmTest", realmResource.toRepresentation().getRealm());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("RealmTest", clientResource.toRepresentation().getClientId());
    }

}
