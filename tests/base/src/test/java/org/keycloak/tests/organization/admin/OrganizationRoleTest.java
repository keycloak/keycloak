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
        assertThat(roleResource.getUserMembers(true, 0, 1), hasSize(1));
        assertThat(roleResource.getUserMembers(true, 1, 1), hasSize(1));
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
