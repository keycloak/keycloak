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

package org.keycloak.tests.organization.admin;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationRoleResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationRoleTest extends AbstractOrganizationTest {

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void testOrganizationRoleLifecycleAndEvents() {
        OrganizationRepresentation organization = createOrganization();
        assertThat(realm.admin().organizations().searchByAttribute("key:value1").stream()
                .map(OrganizationRepresentation::getId).toList(), hasItem(organization.getId()));
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        MemberRepresentation member = addMember(organizationResource, "role-member@neworg.org");
        MemberRepresentation secondMember = addMember(organizationResource, "second-role-member@neworg.org");
        MemberRepresentation availableMember = addMember(organizationResource, "available-role-member@neworg.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();
        UserRepresentation secondUser = realm.admin().users().get(secondMember.getId()).toRepresentation();

        String realmRoleName = "organization-role-composite";
        RoleRepresentation realmRole = new RoleRepresentation(realmRoleName, "Realm role composite", false);
        realm.admin().roles().create(realmRole);
        realm.cleanup().add(r -> r.roles().deleteRole(realmRoleName));
        realmRole = realm.admin().roles().get(realmRoleName).toRepresentation();

        adminEvents.clear();

        RoleRepresentation role = new RoleRepresentation("organization-role", "Organization role", false);
        role.setAttributes(Map.of("department", List.of("engineering")));

        String roleId;
        try (Response response = organizationResource.roles().create(role)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            roleId = ApiUtil.getCreatedId(response);
        }

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.ORGANIZATION_ROLE)
                .resourcePath(AdminEventPaths.organizationRoleResourcePath(organization.getId(), roleId))
                .representation(role);

        assertThat(organizationResource.roles().list(false).stream().map(RoleRepresentation::getId).toList(), hasItem(roleId));

        OrganizationRoleResource roleResource = organizationResource.roles().get(roleId);
        RoleRepresentation storedRole = roleResource.toRepresentation();
        assertEquals(role.getName(), storedRole.getName());
        assertEquals(role.getDescription(), storedRole.getDescription());
        assertEquals(role.getAttributes(), storedRole.getAttributes());

        RoleRepresentation updatedRole = new RoleRepresentation("organization-role-renamed", "Updated organization role", false);
        updatedRole.setAttributes(Map.of("department", List.of("platform")));
        try (Response response = roleResource.update(updatedRole)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.ORGANIZATION_ROLE)
                .resourcePath(AdminEventPaths.organizationRoleResourcePath(organization.getId(), roleId))
                .representation(updatedRole);

        roleResource.addComposites(List.of());
        roleResource.deleteComposites(List.of());
        assertNull(adminEvents.poll());

        assertThat(roleResource.getAvailableRoleComposites("realm", realmRoleName, 0, 10).stream()
                .map(RoleRepresentation::getId).toList(), contains(realmRole.getId()));

        roleResource.addComposites(List.of(realmRole));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.ORGANIZATION_ROLE)
                .resourcePath(AdminEventPaths.organizationRoleCompositesPath(organization.getId(), roleId))
                .representation(List.of(realmRole));
        assertThat(roleResource.getRoleComposites().stream().map(RoleRepresentation::getName).toList(), contains(realmRole.getName()));
        assertThat(roleResource.getEffectiveRoleComposites(null, 0, 10).stream()
                .map(RoleRepresentation::getName).toList(), contains(realmRole.getName()));

        roleResource.deleteComposites(List.of(realmRole));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.ORGANIZATION_ROLE)
                .resourcePath(AdminEventPaths.organizationRoleCompositesPath(organization.getId(), roleId))
                .representation(List.of(realmRole));

        RoleRepresentation missingRole = new RoleRepresentation();
        missingRole.setId("missing-role");
        assertThrows(NotFoundException.class, () -> roleResource.addComposites(List.of(missingRole)));
        assertNull(adminEvents.poll());

        roleResource.addUserMembers(List.of(user, secondUser));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.ORGANIZATION_ROLE_MAPPING)
                .resourcePath(AdminEventPaths.organizationRoleUsersPath(organization.getId(), roleId));
        assertThat(roleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(), containsInAnyOrder(user.getId(), secondUser.getId()));
        assertThat(roleResource.getUserMembers(null, true, 0, 1), hasSize(1));
        assertThat(roleResource.getUserMembers(null, true, 1, 1), hasSize(1));
        assertThat(roleResource.getUserMembers("second-role-member", true, 0, 10).stream()
                .map(UserRepresentation::getId).toList(), contains(secondUser.getId()));
        assertTrue(roleResource.getUserMembers("missing-role-member", true, 0, 10).isEmpty());
        List<String> firstSearchPage = roleResource.getUserMembers("role-member@neworg.org", true, 0, 1).stream()
                .map(UserRepresentation::getId).toList();
        List<String> secondSearchPage = roleResource.getUserMembers("role-member@neworg.org", true, 1, 1).stream()
                .map(UserRepresentation::getId).toList();
        assertThat(firstSearchPage, hasSize(1));
        assertThat(secondSearchPage, hasSize(1));
        assertThat(List.of(firstSearchPage.get(0), secondSearchPage.get(0)), containsInAnyOrder(user.getId(), secondUser.getId()));
        assertThat(roleResource.getAvailableUserMembers("available-role-member", false, true, null, null).stream()
                .map(UserRepresentation::getId).toList(), contains(availableMember.getId()));

        try (Keycloak restrictedAdmin = createRestrictedAdmin()) {
            adminEvents.clear();
            OrganizationRoleResource restrictedRole = restrictedAdmin.realm(realm.getName()).organizations()
                    .get(organization.getId()).roles().get(roleId);
            assertTrue(restrictedRole.getAvailableUserMembers(null, null, true, 0, 10).isEmpty());
        }

        roleResource.deleteUserMembers(List.of(user, secondUser));
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.ORGANIZATION_ROLE_MAPPING)
                .resourcePath(AdminEventPaths.organizationRoleUsersPath(organization.getId(), roleId));

        try (Response response = roleResource.remove()) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.ORGANIZATION_ROLE)
                .resourcePath(AdminEventPaths.organizationRoleResourcePath(organization.getId(), roleId));
    }

    @Test
    public void testOrganizationRoleValidationIsolationAndAtomicity() {
        OrganizationRepresentation organization = createOrganization("role-boundaries");
        OrganizationRepresentation otherOrganization = createOrganization("other-role-boundaries");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        OrganizationResource otherOrganizationResource = realm.admin().organizations().get(otherOrganization.getId());

        RoleRepresentation parent = createOrganizationRole(organizationResource, "shared-role");
        RoleRepresentation assigned = createOrganizationRole(organizationResource, "01-assigned");
        RoleRepresentation available = createOrganizationRole(organizationResource, "02-available");
        RoleRepresentation otherRole = createOrganizationRole(otherOrganizationResource, "shared-role");
        OrganizationRoleResource parentResource = organizationResource.roles().get(parent.getId());

        String sharedRoleName = "shared-role";
        RoleRepresentation realmRoleToCreate = new RoleRepresentation(sharedRoleName, "Realm role with the same name", false);
        realm.admin().roles().create(realmRoleToCreate);
        realm.cleanup().add(r -> r.roles().deleteRole(sharedRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(sharedRoleName).toRepresentation();
        assertFalse(parent.getId().equals(realmRole.getId()));
        assertEquals(realmRole.getId(), realm.admin().roles().get("shared-role").toRepresentation().getId());

        RoleRepresentation duplicate = new RoleRepresentation("shared-role", null, false);
        try (Response response = organizationResource.roles().create(duplicate)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }

        assertEquals(4L, organizationResource.roles().count(null));
        assertThat(organizationResource.roles().list("02-available", 0, 1, false), hasSize(1));
        assertEquals(available.getId(), organizationResource.roles().list("02-available", 0, 1, false).get(0).getId());
        assertThat(organizationResource.roles().list(0, 2), hasSize(2));

        parentResource.addComposites(List.of(assigned, realmRole));
        assertThrows(BadRequestException.class, () -> parentResource.addComposites(List.of(otherRole)));
        assertThat(parentResource.getRoleComposites().stream().map(RoleRepresentation::getId).toList(),
                containsInAnyOrder(assigned.getId(), realmRole.getId()));
        assertThat(parentResource.getEffectiveRoleComposites(null, 0, 10).stream().map(RoleRepresentation::getId).toList(),
                containsInAnyOrder(assigned.getId(), realmRole.getId()));
        assertThat(parentResource.getAvailableRoleComposites("organization", "02", 0, 1).stream()
                .map(RoleRepresentation::getId).toList(), contains(available.getId()));
        assertFalse(parentResource.getAvailableRoleComposites("organization", null, 0, 10).stream()
                .map(RoleRepresentation::getId).toList().contains(otherRole.getId()));
        assertThrows(BadRequestException.class,
                () -> parentResource.getAvailableRoleComposites("unknown", null, 0, 10));

        MemberRepresentation firstMember = addMember(organizationResource, "atomic-first@role-boundaries.org");
        MemberRepresentation secondMember = addMember(organizationResource, "atomic-second@role-boundaries.org");
        UserRepresentation firstUser = realm.admin().users().get(firstMember.getId()).toRepresentation();
        UserRepresentation secondUser = realm.admin().users().get(secondMember.getId()).toRepresentation();
        UserRepresentation outsider = createUser("atomic-outsider@role-boundaries.org");
        UserRepresentation missingUser = new UserRepresentation();
        missingUser.setId("missing-organization-role-user");

        assertThrows(NotFoundException.class, () -> parentResource.addUserMembers(List.of(firstUser, missingUser)));
        assertTrue(parentResource.getUserMembers().isEmpty());
        assertThrows(BadRequestException.class, () -> parentResource.addUserMembers(List.of(firstUser, outsider)));
        assertTrue(parentResource.getUserMembers().isEmpty());

        parentResource.addUserMembers(List.of(firstUser, secondUser));
        assertThrows(NotFoundException.class, () -> parentResource.deleteUserMembers(List.of(firstUser, missingUser)));
        assertThat(parentResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                containsInAnyOrder(firstUser.getId(), secondUser.getId()));

        OrganizationRoleResource defaultRole = organizationResource.roles().getDefault();
        assertThrows(BadRequestException.class, () -> defaultRole.addUserMembers(List.of(firstUser)));
        assertThrows(BadRequestException.class, () -> defaultRole.deleteUserMembers(List.of(firstUser)));

        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().getRole(parent.getId()));
        assertThrows(NotFoundException.class,
                () -> realm.admin().rolesById().addComposites(parent.getId(), List.of(realmRole)));

        GroupRepresentation group = createGroup(realm.admin(), "organization-role-isolation-group");
        realm.cleanup().add(r -> r.groups().group(group.getId()).remove());
        assertThrows(NotFoundException.class,
                () -> realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(parent)));

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("organization-role-isolation-scope");
        clientScope.setProtocol("openid-connect");
        String clientScopeId;
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientScopeId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clientScopes().get(clientScopeId).remove());
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().add(List.of(parent)));

        parentResource.deleteUserMembers(List.of(firstUser, secondUser));
        parentResource.deleteComposites(List.of(assigned, realmRole));
    }

    private RoleRepresentation createOrganizationRole(OrganizationResource organization, String name) {
        RoleRepresentation role = new RoleRepresentation(name, name + " description", false);
        try (Response response = organization.roles().create(role)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            return organization.roles().get(ApiUtil.getCreatedId(response)).toRepresentation();
        }
    }

    private UserRepresentation createUser(String username) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .email(username)
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(user)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            user.setId(ApiUtil.getCreatedId(response));
        }
        realm.cleanup().add(r -> r.users().get(user.getId()).remove());
        return user;
    }

    private Keycloak createRestrictedAdmin() {
        String username = "organization-role-restricted-admin";
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .password("password")
                .name("Organization", "Role Admin")
                .email("organization-role-restricted-admin@example.test")
                .emailVerified(true)
                .enabled(true)
                .build();
        String userId;
        try (Response response = realm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.users().get(userId).remove());

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        List<RoleRepresentation> adminRoles = List.of(AdminRoles.QUERY_USERS, AdminRoles.MANAGE_ORGANIZATIONS).stream()
                .map(name -> realm.admin().clients().get(realmManagement.getId()).roles().get(name).toRepresentation())
                .toList();
        realm.admin().users().get(userId).roles().clientLevel(realmManagement.getId()).add(adminRoles);

        return KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .username(username)
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build();
    }
}
