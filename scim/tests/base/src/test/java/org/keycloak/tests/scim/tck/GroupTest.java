package org.keycloak.tests.scim.tck;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.scim.client.ResourceFilter;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class GroupTest extends AbstractScimTest {

    @Test
    public void testCreate() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected.setExternalId(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertNotNull(actual);
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getExternalId(), actual.getExternalId());
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
        expected.setExternalId(KeycloakModelUtils.generateId());
        adminEvents.clear();
        client.groups().update(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getExternalId(), actual.getExternalId());
    }

    @Test
    public void testPatch() {
        Group expected = new Group();
        expected.setDisplayName(KeycloakModelUtils.generateId());
        expected = client.groups().create(expected);
        assertNotNull(expected);

        expected = client.groups().get(expected.getId());
        expected.setDisplayName("Updated " + expected.getDisplayName());
        expected.setExternalId(KeycloakModelUtils.generateId());
        adminEvents.clear();
        client.groups().patch(expected.getId(), PatchRequest.create()
                .replace("displayName", expected.getDisplayName())
                .replace("externalId", expected.getExternalId())
                .build());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.GROUP)
                .representation(Map.of("displayName", expected.getDisplayName()));

        Group actual = client.groups().get(expected.getId());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
        assertEquals(expected.getExternalId(), actual.getExternalId());
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
    public void testGetWithAttributes() {
        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        group = client.groups().create(group);

        // Request only displayName
        Group actual = client.groups().get(group.getId(), List.of("displayName"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(group.getDisplayName(), actual.getDisplayName());
    }

    @Test
    public void testGetWithExcludedAttributes() {
        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        group = client.groups().create(group);

        // Exclude displayName
        Group actual = client.groups().get(group.getId(), null, List.of("displayName"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNull(actual.getDisplayName());
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

    @Test
    public void testSearchByExternalId() {
        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        group.setExternalId(KeycloakModelUtils.generateId());
        group = client.groups().create(group);

        Group group2 = new Group();
        group2.setDisplayName(KeycloakModelUtils.generateId());
        group2.setExternalId(KeycloakModelUtils.generateId());
        group2 = client.groups().create(group2);

        String filter = ResourceFilter.filter().eq("externalId", group.getExternalId()).build();
        ListResponse<Group> response = client.groups().getAll(filter);
        assertFalse(response.getResources().isEmpty());
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getExternalId(), is(group.getExternalId()));

        filter = ResourceFilter.filter().eq("externalId", group2.getExternalId()).build();
        response = client.groups().getAll(filter);
        assertFalse(response.getResources().isEmpty());
        assertThat(response.getTotalResults(), is(1));
        assertThat(response.getResources().get(0).getExternalId(), is(group2.getExternalId()));
    }

    @Test
    public void testCreateDuplicate() {
        Group group = new Group();
        group.setDisplayName(KeycloakModelUtils.generateId());
        client.groups().create(group);

        try {
            client.groups().create(group);
            fail("should fail because of duplicate group");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(409, error.getStatusInt());
            assertEquals("uniqueness", error.getScimType());
            assertNotNull(error.getDetail());
        }
    }
}
