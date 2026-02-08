package org.keycloak.tests.scim.tck;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class GroupTest {

    @InjectScimClient
    ScimClient client;

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testCreate() {
        Group group = new Group();

        group.setDisplayName("MyGroup");

        group = client.groups().create(group);
        assertNotNull(group);

        group = client.groups().get(group.getId());
        assertNotNull(group);

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("Stefan Group");
        realm.admin().groups().add(rep).close();
        GroupRepresentation stefanGroup = realm.admin().groups().groups("Stefan Group", -1, -1).get(0);

        group = client.groups().get(stefanGroup.getId());
        assertNotNull(group);
    }
}
