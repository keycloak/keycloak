package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.RealmConfig;

@KeycloakIntegrationTest
public class MultipleInstancesTest {

    @InjectRealm
    ManagedRealm realm1;

    @InjectRealm
    ManagedRealm realm2;

    @InjectRealm(ref = "another", config = CustomRealmConfig.class)
    ManagedRealm realm3;

    @InjectClient(ref = "client1")
    ManagedClient client;

    @InjectClient
    ManagedClient client2;

    @Test
    public void testMultipleInstances() {
        Assertions.assertEquals("default", realm1.getName());
        Assertions.assertEquals("default", realm2.getName());
        Assertions.assertEquals(realm1, realm2);

        Assertions.assertEquals("another", realm3.getName());

        Assertions.assertEquals("client1", client.getClientId());
        Assertions.assertEquals("default", client2.getClientId());
    }


    public static class CustomRealmConfig implements RealmConfig {
        @Override
        public RealmRepresentation getRepresentation() {
            return new RealmRepresentation();
        }
    }

}
