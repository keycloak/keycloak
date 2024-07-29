package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;
import org.keycloak.test.framework.realm.RealmConfig;

@KeycloakIntegrationTest
public class MultipleInstancesTest {

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRealm
    ManagedRealm realmDef1;

    @InjectRealm
    ManagedRealm realmDef2;

    @InjectRealm(ref = "A", config = CustomRealmConfig.class)
    ManagedRealm realmA;

    @InjectClient(ref = "client1")
    ManagedClient client1;

    @InjectUser(ref = "user1", realmRef = "default")
    ManagedUser user1;

    @InjectUser(ref = "user2", realmRef = "A")
    ManagedUser user2;

    @InjectUser(ref = "user3", realmRef = "A")
    ManagedUser user3;

    @InjectUser(ref = "user4", realmRef = "B")
    ManagedUser user4;

    @Test
    public void testMultipleInstances() {
        Assertions.assertEquals("default", realmDef1.getName());
        Assertions.assertEquals("default", realmDef2.getName());
        Assertions.assertSame(realmDef1, realmDef2);

        Assertions.assertEquals("A", realmA.getName());
    }

    @Test
    public void testRealmRef() {
        Assertions.assertFalse(realmDef1.admin().clients().findByClientId("client1").isEmpty());

        Assertions.assertEquals(1, realmDef1.admin().users().count());
        Assertions.assertEquals(2, realmA.admin().users().count());

        Assertions.assertNotNull(adminClient.realm("B"));
        Assertions.assertNotNull(adminClient.realm("B").users().get("user4"));
    }


    public static class CustomRealmConfig implements RealmConfig {
        @Override
        public RealmRepresentation getRepresentation() {
            return new RealmRepresentation();
        }
    }

}
