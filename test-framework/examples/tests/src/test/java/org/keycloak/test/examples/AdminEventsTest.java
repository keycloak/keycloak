package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.events.admin.OperationType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;

@KeycloakIntegrationTest
public class AdminEventsTest {

    @InjectAdminEvents
    private AdminEvents adminEvents;

    @InjectAdminClient
    private Keycloak adminClient;

    @InjectRealm
    private ManagedRealm realm;

    @Test
    public void testAdminEventOnUserCreateAndDelete() {

        String userName = "create_user";

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(userName);
        userRep.setEnabled(true);

        adminClient.realm(realm.getName()).users().create(userRep);

        Assertions.assertEquals(OperationType.CREATE, adminEvents.poll().getOperationType());

        String userId = adminClient.realm(realm.getName()).users().search(userName).get(0).getId();

        adminClient.realm(realm.getName()).users().delete(userId);

        Assertions.assertEquals(OperationType.DELETE, adminEvents.poll().getOperationType());
    }

    @Test
    public void testAdminEventOnRealmCreateAndUpdate() {

        String realmName = "create_realm";

        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm(realmName);
        realmRep.setEnabled(true);

        adminClient.realms().create(realmRep);

        Assertions.assertEquals(OperationType.CREATE, adminEvents.poll().getOperationType());

        RealmRepresentation realmRep2 = adminClient.realms().realm(realmName).toRepresentation();
        realmRep2.setEnabled(false);

        adminClient.realms().realm(realmName).update(realmRep2);

        Assertions.assertEquals(OperationType.UPDATE, adminEvents.poll().getOperationType());
    }

}
