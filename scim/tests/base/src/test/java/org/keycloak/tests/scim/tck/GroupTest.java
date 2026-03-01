package org.keycloak.tests.scim.tck;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

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

        client.groups().delete(expected.getId());
        expected = client.groups().get(expected.getId());
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
        client.groups().update(expected);
        Group actual = client.groups().get(expected.getId());
        assertEquals(expected.getDisplayName(), actual.getDisplayName());
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
