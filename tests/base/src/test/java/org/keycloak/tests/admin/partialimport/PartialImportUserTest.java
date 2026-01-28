package org.keycloak.tests.admin.partialimport;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.admin.OperationType;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = AbstractPartialImportTest.PartialImportServerConfig.class)
public class PartialImportUserTest extends AbstractPartialImportTest {

    @Test
    public void testAddUsers() {
        adminEvents.clear();

        setFail();
        addUsers();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        // Need to do this way as admin events from partial import are unsorted
        Set<String> userIds = new HashSet<>();
        for (int i=0 ; i<NUM_ENTITIES ; i++) {
            AdminEventRepresentation adminEvent = adminEvents.poll();
            Assertions.assertEquals(managedRealm.getId(), adminEvent.getRealmId());
            Assertions.assertEquals(OperationType.CREATE.name(), adminEvent.getOperationType());
            Assertions.assertTrue(adminEvent.getResourcePath().startsWith("users/"));
            assertThat(adminEvent.getResourceType(), equalTo(org.keycloak.events.admin.ResourceType.REALM.name()));
            String userId = adminEvent.getResourcePath().substring(6);
            userIds.add(userId);
        }

        Assertions.assertNull(adminEvents.poll());


        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = managedRealm.admin().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            assertThat(user.getUsername(), startsWith(USER_PREFIX));
            assertThat(userIds, hasItem(id));
        }
    }

    @Test
    public void testAddUsersWithIds() {
        adminEvents.clear();

        setFail();
        addUsersWithIds();

        Set<String> userRepIds = new HashSet<>();
        for (UserRepresentation userRep : piRep.getUsers()) {
            userRepIds.add(userRep.getId());
        }

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        // Need to do this way as admin events from partial import are unsorted
        Set<String> userIds = new HashSet<>();
        for (int i=0 ; i<NUM_ENTITIES ; i++) {
            AdminEventRepresentation adminEvent = adminEvents.poll();
            Assertions.assertEquals(managedRealm.getId(), adminEvent.getRealmId());
            Assertions.assertEquals(OperationType.CREATE.name(), adminEvent.getOperationType());
            Assertions.assertTrue(adminEvent.getResourcePath().startsWith("users/"));
            assertThat(adminEvent.getResourceType(), equalTo(org.keycloak.events.admin.ResourceType.REALM.name()));
            String userId = adminEvent.getResourcePath().substring(6);
            userIds.add(userId);
            assertThat(userRepIds, hasItem(userId));
        }

        Assertions.assertNull(adminEvents.poll());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = managedRealm.admin().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            assertThat(user.getUsername(), startsWith(USER_PREFIX));
            assertThat(userIds, hasItem(id));
            assertThat(userRepIds, hasItem(id));
        }
    }

    @Test
    public void testAddUsersWithDuplicateEmailsForbidden() {
        adminEvents.clear();

        setFail();
        addUsers();

        UserRepresentation user = UserConfigBuilder.create().username(USER_PREFIX + 999).email(USER_PREFIX + 1 + "@foo.com").name("foo", "bar").build();
        piRep.getUsers().add(user);

        try (Response response = managedRealm.admin().partialImport(piRep)) {
            assertEquals(409, response.getStatus());
        }
    }

    @Test
    public void testAddUsersWithDuplicateEmailsAllowed() {

        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        realmRep.setDuplicateEmailsAllowed(true);
        managedRealm.admin().update(realmRep);

        adminEvents.clear();

        setFail();
        addUsers();
        doImport();

        UserRepresentation user = UserConfigBuilder.create().username(USER_PREFIX + 999).email(USER_PREFIX + 1 + "@foo.com").name("foo", "bar").build();
        piRep.setUsers(List.of(user));

        PartialImportResults results = doImport();
        assertEquals(1, results.getAdded());
    }

    @Test
    public void testAddUsersWithTermsAndConditions() {
        adminEvents.clear();

        setFail();
        addUsersWithTermsAndConditions();

        PartialImportResults results = doImport();
        assertEquals(NUM_ENTITIES, results.getAdded());

        // Need to do this way as admin events from partial import are unsorted
        Set<String> userIds = new HashSet<>();
        for (int i=0 ; i<NUM_ENTITIES ; i++) {
            AdminEventRepresentation adminEvent = adminEvents.poll();
            Assertions.assertEquals(managedRealm.getId(), adminEvent.getRealmId());
            Assertions.assertEquals(OperationType.CREATE.name(), adminEvent.getOperationType());
            Assertions.assertTrue(adminEvent.getResourcePath().startsWith("users/"));
            String userId = adminEvent.getResourcePath().substring(6);
            userIds.add(userId);
        }

        Assertions.assertNull(adminEvents.poll());

        for (PartialImportResult result : results.getResults()) {
            String id = result.getId();
            UserResource userRsc = managedRealm.admin().users().get(id);
            UserRepresentation user = userRsc.toRepresentation();
            assertTrue(user.getUsername().startsWith(USER_PREFIX));
            Assertions.assertTrue(userIds.contains(id));
            assertThat(user.getRequiredActions(), contains(TermsAndConditions.PROVIDER_ID));
        }
    }

    @Test
    public void testAddUsersFail() {
        addUsers();
        testFail();
    }

    @Test
    public void testAddUsersSkip() {
        addUsers();
        testSkip();
    }

    @Test
    public void testAddUsersOverwrite() {
        addUsers();
        testOverwrite();
    }
}
