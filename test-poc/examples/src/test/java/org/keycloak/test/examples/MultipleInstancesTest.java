package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
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

    @InjectRealm
    ManagedRealm realm1;

    @InjectRealm
    ManagedRealm realm2;

    @InjectRealm(ref = "another", config = CustomRealmConfig.class)
    ManagedRealm realm3;

    @InjectRealm(ref = "anotherOne")
    ManagedRealm realm4;

    @InjectClient(ref = "client1")
    ManagedClient client;

    @InjectClient
    ManagedClient client2;

    @InjectUser(realmRef = "default")
    ManagedUser user1;

    @InjectUser(realmRef = "another")
    ManagedUser user2;

    @InjectUser(ref = "another", realmRef = "another")
    ManagedUser user3;

    @InjectUser(ref = "anotherOne", realmRef = "anotherOne")
    ManagedUser user4;

    @InjectUser(realmRef = "anotherTwo")
    ManagedUser user5;

    @InjectUser(ref = "anotherTwo", realmRef = "anotherTwo")
    ManagedUser user6;

    @Test
    public void testMultipleInstances() {
        Assertions.assertEquals("default", realm1.getName());
        Assertions.assertEquals("default", realm2.getName());
        Assertions.assertEquals(realm1, realm2);

        Assertions.assertEquals("another", realm3.getName());

        Assertions.assertEquals("client1", client.getClientId());
        Assertions.assertEquals("default", client2.getClientId());

        Assertions.assertEquals("client1", client.getClientId());
        Assertions.assertEquals("default", client2.getClientId());

        Assertions.assertEquals("default", realm1.admin().toRepresentation().user(user1.getUsername()).getUsername());
        Assertions.assertEquals("default", realm3.admin().toRepresentation().user(user2.getUsername()).getUsername());
        Assertions.assertEquals("another", realm3.admin().toRepresentation().user(user3.getUsername()).getUsername());

        Assertions.assertNotNull(user4);
        Assertions.assertNotNull(user5);
        Assertions.assertNotNull(user6);
    }


    public static class CustomRealmConfig implements RealmConfig {
        @Override
        public RealmRepresentation getRepresentation() {
            return new RealmRepresentation();
        }
    }

}
