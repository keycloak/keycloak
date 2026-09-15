/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.organization.group;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
public class OrganizationGroupRoleMappingTest extends AbstractOrganizationTest {

    @Test
    public void testAddRealmRoleMappingToOrgGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create a realm role
        RoleRepresentation role = new RoleRepresentation("test-org-role", "Test role for org groups", false);
        realm.admin().roles().create(role);
        realm.cleanup().add(r -> r.roles().deleteRole("test-org-role"));
        RoleRepresentation createdRole = realm.admin().roles().get("test-org-role").toRepresentation();

        // Create org group
        String groupId = createOrgGroup(orgResource, "role-test-group");

        // Add realm role mapping
        RoleScopeResource realmRoles = orgResource.groups().group(groupId).roles().realmLevel();
        realmRoles.add(List.of(createdRole));

        // Verify role mapping exists
        List<RoleRepresentation> mappedRoles = realmRoles.listAll();
        assertThat(mappedRoles, hasSize(1));
        assertThat(mappedRoles.get(0).getName(), is("test-org-role"));
    }

    @Test
    public void testAddClientRoleMappingToOrgGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create a client with a role
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-org-client");
        clientRep.setEnabled(true);

        String clientUuid;
        try (Response response = realm.admin().clients().create(clientRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientUuid).remove());

        RoleRepresentation clientRole = new RoleRepresentation("client-org-role", "Test client role", false);
        realm.admin().clients().get(clientUuid).roles().create(clientRole);
        RoleRepresentation createdClientRole = realm.admin().clients().get(clientUuid).roles().get("client-org-role").toRepresentation();

        // Create org group
        String groupId = createOrgGroup(orgResource, "client-role-test-group");

        // Add client role mapping
        RoleScopeResource clientRoles = orgResource.groups().group(groupId).roles().clientLevel(clientUuid);
        clientRoles.add(List.of(createdClientRole));

        // Verify client role mapping exists
        List<RoleRepresentation> mappedRoles = clientRoles.listAll();
        assertThat(mappedRoles, hasSize(1));
        assertThat(mappedRoles.get(0).getName(), is("client-org-role"));
    }

    @Test
    public void testListRoleMappings() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create realm role
        RoleRepresentation role = new RoleRepresentation("list-test-role", "Test role", false);
        realm.admin().roles().create(role);
        realm.cleanup().add(r -> r.roles().deleteRole("list-test-role"));
        RoleRepresentation createdRole = realm.admin().roles().get("list-test-role").toRepresentation();

        // Create org group and add role
        String groupId = createOrgGroup(orgResource, "list-test-group");
        orgResource.groups().group(groupId).roles().realmLevel().add(List.of(createdRole));

        // List all role mappings via getAll()
        RoleMappingResource roleMappings = orgResource.groups().group(groupId).roles();
        MappingsRepresentation allMappings = roleMappings.getAll();
        assertThat(allMappings.getRealmMappings(), hasSize(1));
        assertThat(allMappings.getRealmMappings().get(0).getName(), is("list-test-role"));
    }

    @Test
    public void testListAvailableRealmRoles() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create two realm roles
        realm.admin().roles().create(new RoleRepresentation("avail-role-1", "", false));
        realm.admin().roles().create(new RoleRepresentation("avail-role-2", "", false));
        realm.cleanup().add(r -> r.roles().deleteRole("avail-role-1"));
        realm.cleanup().add(r -> r.roles().deleteRole("avail-role-2"));
        RoleRepresentation role1 = realm.admin().roles().get("avail-role-1").toRepresentation();

        // Create org group and assign only role1
        String groupId = createOrgGroup(orgResource, "avail-test-group");
        orgResource.groups().group(groupId).roles().realmLevel().add(List.of(role1));

        // Available roles should include role2 but not role1
        List<RoleRepresentation> available = orgResource.groups().group(groupId).roles().realmLevel().listAvailable();
        Set<String> availableNames = available.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(availableNames.contains("avail-role-1"), is(false));
        assertThat(availableNames.contains("avail-role-2"), is(true));
    }

    @Test
    public void testListEffectiveRealmRoles() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create a composite role
        realm.admin().roles().create(new RoleRepresentation("child-role", "", false));
        realm.admin().roles().create(new RoleRepresentation("composite-role", "", false));
        realm.cleanup().add(r -> r.roles().deleteRole("child-role"));
        realm.cleanup().add(r -> r.roles().deleteRole("composite-role"));

        RoleRepresentation childRole = realm.admin().roles().get("child-role").toRepresentation();
        realm.admin().roles().get("composite-role").addComposites(List.of(childRole));
        RoleRepresentation compositeRole = realm.admin().roles().get("composite-role").toRepresentation();

        // Create org group and assign composite role
        String groupId = createOrgGroup(orgResource, "effective-test-group");
        orgResource.groups().group(groupId).roles().realmLevel().add(List.of(compositeRole));

        // Effective roles should include both the composite and its child
        List<RoleRepresentation> effective = orgResource.groups().group(groupId).roles().realmLevel().listEffective();
        Set<String> effectiveNames = effective.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
        assertThat(effectiveNames.contains("composite-role"), is(true));
        assertThat(effectiveNames.contains("child-role"), is(true));
    }

    @Test
    public void testRemoveRoleMapping() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create realm role
        realm.admin().roles().create(new RoleRepresentation("remove-test-role", "", false));
        realm.cleanup().add(r -> r.roles().deleteRole("remove-test-role"));
        RoleRepresentation role = realm.admin().roles().get("remove-test-role").toRepresentation();

        // Create org group and add role
        String groupId = createOrgGroup(orgResource, "remove-test-group");
        RoleScopeResource realmRoles = orgResource.groups().group(groupId).roles().realmLevel();
        realmRoles.add(List.of(role));
        assertThat(realmRoles.listAll(), hasSize(1));

        // Remove role mapping
        realmRoles.remove(List.of(role));
        assertThat(realmRoles.listAll(), is(empty()));
    }

    @Test
    public void testRoleMappingIncludedInGroupRepresentation() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        // Create realm role
        realm.admin().roles().create(new RoleRepresentation("rep-test-role", "", false));
        realm.cleanup().add(r -> r.roles().deleteRole("rep-test-role"));
        RoleRepresentation role = realm.admin().roles().get("rep-test-role").toRepresentation();

        // Create client with a role
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("rep-test-client");
        clientRep.setEnabled(true);
        String clientUuid;
        try (Response response = realm.admin().clients().create(clientRep)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        realm.admin().clients().get(clientUuid).roles().create(new RoleRepresentation("rep-client-role", "", false));
        RoleRepresentation clientRole = realm.admin().clients().get(clientUuid).roles().get("rep-client-role").toRepresentation();

        // Create org group and assign both roles
        String groupId = createOrgGroup(orgResource, "rep-test-group");
        orgResource.groups().group(groupId).roles().realmLevel().add(List.of(role));
        orgResource.groups().group(groupId).roles().clientLevel(clientUuid).add(List.of(clientRole));

        // Verify representation includes role mappings
        GroupRepresentation retrieved = orgResource.groups().group(groupId).toRepresentation(false);
        assertThat(retrieved.getRealmRoles(), notNullValue());
        assertThat(retrieved.getRealmRoles(), hasSize(1));
        assertThat(retrieved.getRealmRoles().get(0), is("rep-test-role"));

        assertThat(retrieved.getClientRoles(), notNullValue());
        Map<String, List<String>> clientRoles = retrieved.getClientRoles();
        assertThat(clientRoles.containsKey("rep-test-client"), is(true));
        assertThat(clientRoles.get("rep-test-client"), hasSize(1));
        assertThat(clientRoles.get("rep-test-client").get(0), is("rep-client-role"));
    }

    @Test
    public void testRoleMappingCannotBeAddedViaRegularGroupsApi() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(orgRep.getId());

        String groupId = createOrgGroup(orgResource, "isolation-test-group");

        // Try to access the org group via the regular groups API — should fail
        try {
            realm.admin().groups().group(groupId).roles().realmLevel().listAll();
            org.junit.jupiter.api.Assertions.fail("Should not be able to access org group via regular groups API");
        } catch (Exception e) {
            assertThat(e.getMessage(), org.hamcrest.Matchers.containsString("400"));
        }
    }

    private String createOrgGroup(OrganizationResource orgResource, String name) {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(name);
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }
}
