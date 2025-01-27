package org.keycloak.test.examples;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.admin.KeycloakAdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;

@KeycloakIntegrationTest
public class AdminClientFactoryTest {

    private final String REALM1 = "realm1";
    private final String CLIENT1 = "client1";

    @InjectAdminClientFactory
    KeycloakAdminClientFactory adminClientFactory;

    @InjectRealm(ref = REALM1)
    ManagedRealm realm1;

    @InjectClient(ref = CLIENT1, realmRef = REALM1)
    ManagedClient client1;

    @Test
    public void test() {
        Keycloak adminClientMasterRealm = adminClientFactory.createMaster();
        RealmRepresentation findRealm1 = adminClientMasterRealm.realm(REALM1).toRepresentation();
        Assertions.assertEquals(realm1.getName(), findRealm1.getRealm());

        Keycloak adminClientRealm1 = adminClientFactory.create(REALM1, CLIENT1);
        Assertions.assertThrows(ProcessingException.class, () -> adminClientRealm1.realm(REALM1).toRepresentation());
    }
}
