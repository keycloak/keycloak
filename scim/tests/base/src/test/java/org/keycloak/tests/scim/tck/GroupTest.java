package org.keycloak.tests.scim.tck;

import java.time.Instant;
import java.util.Map;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class GroupTest extends AbstractScimTest {

    @Test
    public void testCreate() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertNotNull(actual);
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    @Test
    public void testDelete() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);
        String id = expected.getId();
        String displayName = expected.getDisplayName();
        adminEvents.clear();

        client.groups().delete(id);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("id", id, "displayName", displayName));

        expected = client.groups().get(id);
        assertNull(expected);
    }

    @Test
    public void testUpdate() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);

        expected = client.groups().get(expected.getId());
        expected.setDisplayName("Updated " + expected.getDisplayName());
        adminEvents.clear();
        client.groups().update(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    @Test
    public void testPatch() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);

        expected = client.groups().get(expected.getId());
        expected.setDisplayName("Updated " + expected.getDisplayName());
        adminEvents.clear();
        client.groups().patch(expected.getId(), PatchRequest.create()
                .replace("displayName", expected.getDisplayName())
                .build());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    @Test
    public void testMetaTimestamps() {
        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        group = client.groups().create(group);

        Group afterCreate = client.groups().get(group.getId());
        assertNotNull(afterCreate.getMeta());
        assertNotNull(afterCreate.getMeta().getCreated());
        assertNotNull(afterCreate.getMeta().getLastModified());

        Instant createdTimestamp = Instant.parse(afterCreate.getMeta().getCreated());
        Instant lastModifiedAfterCreate = Instant.parse(afterCreate.getMeta().getLastModified());

        // after create, lastModified should be >= created
        assertThat(lastModifiedAfterCreate, greaterThanOrEqualTo(createdTimestamp));

        // update the group
        afterCreate.setDisplayName("Updated " + afterCreate.getDisplayName());
        client.groups().update(afterCreate);
        Group afterUpdate = client.groups().get(afterCreate.getId());

        Instant createdAfterUpdate = Instant.parse(afterUpdate.getMeta().getCreated());
        Instant lastModifiedAfterUpdate = Instant.parse(afterUpdate.getMeta().getLastModified());

        // created should not change after update
        assertEquals(createdTimestamp, createdAfterUpdate);
        // lastModified should be >= created after update
        assertThat(lastModifiedAfterUpdate, greaterThanOrEqualTo(createdAfterUpdate));
        // lastModified should have advanced after update
        assertThat(lastModifiedAfterUpdate, greaterThanOrEqualTo(lastModifiedAfterCreate));
    }


    @Test
    public void testGetExisting() {
        GroupRepresentation rep = new GroupRepresentation();
        rep.setName(KeycloakModelUtils.generateId());
        realm.admin().groups().add(rep).close();
        rep = realm.admin().groups().groups(rep.getName(), -1, -1).get(0);

        Group group = client.groups().get(rep.getId());
        assertNotNull(group);
        assertEquals(rep.getName(), group.getDisplayName());
    }
}
