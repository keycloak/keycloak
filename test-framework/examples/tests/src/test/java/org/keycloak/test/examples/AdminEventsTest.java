package org.keycloak.test.examples;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class AdminEventsTest {

    @InjectRealm
    private ManagedRealm realm;

    @InjectAdminEvents
    private AdminEvents adminEvents;

    @InjectRealm(ref = "master", attachTo = "master")
    private ManagedRealm master;

    @InjectAdminEvents(ref = "master", realmRef = "master")
    private AdminEvents masterAdminEvents;

    @InjectAdminClient
    private Keycloak adminClient;

    @Test
    public void testAdminEventOnUserCreateAndDelete() {
        String userName = "create_user";

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(userName);
        userRep.setEnabled(true);

        String userId = ApiUtil.getCreatedId(adminClient.realm(realm.getName()).users().create(userRep));

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.USER)
                .resourcePath("users", userId)
                .representation(userRep);

        adminClient.realm(realm.getName()).users().delete(userId);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.USER)
                .resourcePath("users", userId)
                .representation(null);
    }

    @Test
    public void testAdminEventOnRealmCreateAndUpdate() {
        master.updateWithCleanup(r -> r.adminEventsEnabled(true));

        String realmName = "testAdminEventOnRealmCreateAndUpdate";

        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm(realmName);
        realmRep.setEnabled(true);
        realmRep.setAdminEventsEnabled(true);

        try {
            adminClient.realms().create(realmRep);
            Assertions.assertEquals(OperationType.CREATE.name(), masterAdminEvents.poll().getOperationType());
        } finally {
            adminClient.realm(realmName).remove();
        }
    }

    @Test
    public void testAdminEventOnRealmUpdate() {
        realm.updateWithCleanup(r -> r.editUsernameAllowed(true));

        Assertions.assertEquals(OperationType.UPDATE.name(), adminEvents.poll().getOperationType());
    }

    @Test
    public void testSkipEvent() {
        List<String> userIds = createUsers("testSkipEvent", 2);
        adminEvents.skip();
        AdminEventAssertion.assertSuccess(adminEvents.poll()).resourcePath("users", userIds.get(1));
    }

    @Test
    public void testSkipEvents() {
        List<String> userIds = createUsers("testSkipEvents", 4);
        adminEvents.skip(3);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).resourcePath("users", userIds.get(3));
    }

    @Test
    public void testSkipAlLEvents() {
        createUsers("testSkipAlLEventsBefore", 3);
        adminEvents.skipAll();
        List<String> userIds = createUsers("testSkipAlLEventsAfter", 1);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).resourcePath("users", userIds.get(0));
    }

    private List<String> createUsers(String prefix, int n) {
        List<String> userIds = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserConfigBuilder.create().username(prefix + i).build()));
            userIds.add(userId);
        }
        return userIds;
    }

}
