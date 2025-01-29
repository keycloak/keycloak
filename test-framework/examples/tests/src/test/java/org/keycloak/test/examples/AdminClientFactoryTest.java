package org.keycloak.test.examples;

import jakarta.ws.rs.ProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.testframework.admin.KeycloakAdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

@KeycloakIntegrationTest
public class AdminClientFactoryTest {

    private final String REALM1 = "realm1";
    private final String CLIENT1 = "client1";
    private final String USER1 = "user1";

    @InjectAdminClientFactory
    KeycloakAdminClientFactory adminClientFactory;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm masterRealm;

    @InjectRealm(ref = REALM1)
    ManagedRealm realm1;

    @InjectClient(ref = CLIENT1, realmRef = REALM1)
    ManagedClient client1;

    @InjectUser(ref = USER1, realmRef = REALM1)
    ManagedUser user1;

    @Test
    public void test() {
        Keycloak adminClientRealm1 = adminClientFactory.create(REALM1, client1.getClientId());
        Assertions.assertThrows(ProcessingException.class, () -> adminClientRealm1.realm(REALM1).toRepresentation());

        Keycloak adminClientMasterRealm = adminClientFactory.create("master", Config.getAdminClientId());
        RealmResource findRealm1 = adminClientMasterRealm.realm(REALM1);
        Assertions.assertEquals(realm1.getName(), findRealm1.toRepresentation().getRealm());
        Assertions.assertFalse(
                findRealm1.clients().findByClientId(client1.getClientId()).isEmpty()
        );
        Assertions.assertFalse(
                findRealm1.users().search(user1.getUsername()).isEmpty()
        );
    }
}
