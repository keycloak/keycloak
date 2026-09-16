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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.OrganizationRoleResource;
import org.keycloak.admin.ui.rest.model.RoleDeleteRequest;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
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

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private static final class RoleNameChangeEventCollector implements ProviderEventListener, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final RoleNameChangeEventCollector INSTANCE = new RoleNameChangeEventCollector();

        private final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(ProviderEvent event) {
            if (event instanceof RoleModel.RoleNameChangeEvent roleNameChangeEvent) {
                events.add(roleNameChangeEvent.getPreviousName() + "->" + roleNameChangeEvent.getNewName());
            }
        }

        private void clear() {
            events.clear();
        }
    }

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

        String realmRoleName = "organization-role";
        RoleRepresentation realmRole = new RoleRepresentation(realmRoleName, "Realm role with the same name", false);
        realm.admin().roles().create(realmRole);
        realm.cleanup().add(r -> r.roles().deleteRole(realmRoleName));
        realmRole = realm.admin().roles().get(realmRoleName).toRepresentation();

        String legacyRoleName = "legacy-role-name-change-event";
        String renamedLegacyRoleName = legacyRoleName + "-renamed";
        realm.admin().roles().create(new RoleRepresentation(legacyRoleName, null, false));
        runOnServer.run(session -> {
            RoleNameChangeEventCollector collector = RoleNameChangeEventCollector.INSTANCE;
            session.getKeycloakSessionFactory().unregister(collector);
            collector.clear();
            session.getKeycloakSessionFactory().register(collector);
        });
        realm.admin().roles().get(legacyRoleName)
                .update(new RoleRepresentation(renamedLegacyRoleName, null, false));
        runOnServer.run(session -> {
            RoleNameChangeEventCollector collector = RoleNameChangeEventCollector.INSTANCE;
            assertThat(collector.events, contains(legacyRoleName + "->" + renamedLegacyRoleName));
            collector.clear();
        });
        realm.admin().roles().get(renamedLegacyRoleName).remove();

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
        runOnServer.run(session -> {
            RoleNameChangeEventCollector collector = RoleNameChangeEventCollector.INSTANCE;
            session.getKeycloakSessionFactory().unregister(collector);
            collector.clear();
            session.getKeycloakSessionFactory().register(collector);
        });
        try {
            try (Response response = roleResource.update(updatedRole)) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
            runOnServer.run(session -> assertTrue(RoleNameChangeEventCollector.INSTANCE.events.isEmpty(),
                    "Organization role rename must not publish the legacy RoleNameChangeEvent"));
        } finally {
            runOnServer.run(session -> session.getKeycloakSessionFactory().unregister(RoleNameChangeEventCollector.INSTANCE));
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
    public void testOrganizationRoleNamespaceIsolation() {
        OrganizationRepresentation organization = createOrganization("ns-isolation");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation orgRole = createOrganizationRole(organizationResource, "shared-role");

        String sharedRoleName = "shared-role";
        realm.admin().roles().create(new RoleRepresentation(sharedRoleName, "Realm role with the same name", false));
        realm.cleanup().add(r -> r.roles().deleteRole(sharedRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(sharedRoleName).toRepresentation();
        assertFalse(orgRole.getId().equals(realmRole.getId()));
        assertEquals(realmRole.getId(), realm.admin().roles().get(sharedRoleName).toRepresentation().getId());

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("ns-isolation-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());
        realm.admin().clients().get(clientId).roles().create(new RoleRepresentation(sharedRoleName, "Client role with the same name", true));
        RoleRepresentation clientRole = realm.admin().clients().get(clientId).roles().get(sharedRoleName).toRepresentation();

        assertThat(realm.admin().roles().list().stream().map(RoleRepresentation::getId).toList(), hasItem(realmRole.getId()));
        assertFalse(realm.admin().roles().list().stream().map(RoleRepresentation::getId).toList().contains(orgRole.getId()));
        assertThat(realm.admin().clients().get(clientId).roles().list().stream().map(RoleRepresentation::getId).toList(), hasItem(clientRole.getId()));
        assertFalse(realm.admin().clients().get(clientId).roles().list().stream().map(RoleRepresentation::getId).toList().contains(orgRole.getId()));
    }

    @Test
    public void testOrganizationRoleDuplicateNameRejected() {
        OrganizationRepresentation organization = createOrganization("dup-role");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        createOrganizationRole(organizationResource, "shared-role");

        try (Response response = organizationResource.roles().create(new RoleRepresentation("shared-role", null, false))) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testOrganizationRoleListingAndPagination() {
        OrganizationRepresentation organization = createOrganization("role-listing");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        createOrganizationRole(organizationResource, "shared-role");
        createOrganizationRole(organizationResource, "01-assigned");
        RoleRepresentation available = createOrganizationRole(organizationResource, "02-available");

        assertEquals(4L, organizationResource.roles().count(null));
        assertThat(organizationResource.roles().list("02-available", 0, 1, false), hasSize(1));
        assertEquals(available.getId(), organizationResource.roles().list("02-available", 0, 1, false).get(0).getId());
        assertThat(organizationResource.roles().list(0, 2), hasSize(2));
    }

    @Test
    public void testOrganizationRoleCompositeIsolation() {
        OrganizationRepresentation organization = createOrganization("composite-isolation");
        OrganizationRepresentation otherOrganization = createOrganization("other-composite-isolation");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        OrganizationResource otherOrganizationResource = realm.admin().organizations().get(otherOrganization.getId());

        RoleRepresentation parent = createOrganizationRole(organizationResource, "shared-role");
        RoleRepresentation assigned = createOrganizationRole(organizationResource, "01-assigned");
        RoleRepresentation available = createOrganizationRole(organizationResource, "02-available");
        RoleRepresentation otherRole = createOrganizationRole(otherOrganizationResource, "shared-role");
        OrganizationRoleResource parentResource = organizationResource.roles().get(parent.getId());

        String sharedRoleName = "shared-role";
        realm.admin().roles().create(new RoleRepresentation(sharedRoleName, "Realm role with the same name", false));
        realm.cleanup().add(r -> r.roles().deleteRole(sharedRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(sharedRoleName).toRepresentation();

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("composite-isolation-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());
        realm.admin().clients().get(clientId).roles().create(new RoleRepresentation(sharedRoleName, "Client role with the same name", true));
        RoleRepresentation clientRole = realm.admin().clients().get(clientId).roles().get(sharedRoleName).toRepresentation();

        parentResource.addComposites(List.of(assigned, realmRole, clientRole));
        assertThrows(BadRequestException.class, () -> parentResource.addComposites(List.of(otherRole)));
        assertThat(parentResource.getRoleComposites().stream().map(RoleRepresentation::getId).toList(),
                containsInAnyOrder(assigned.getId(), realmRole.getId(), clientRole.getId()));
        assertThat(parentResource.getEffectiveRoleComposites(null, 0, 10).stream().map(RoleRepresentation::getId).toList(),
                containsInAnyOrder(assigned.getId(), realmRole.getId(), clientRole.getId()));
        assertThat(parentResource.getRealmRoleComposites().stream().map(RoleRepresentation::getId).toList(),
                contains(realmRole.getId()));
        assertThat(parentResource.getClientRoleComposites(clientId).stream().map(RoleRepresentation::getId).toList(),
                contains(clientRole.getId()));
        assertThat(parentResource.getAvailableRoleComposites("organization", "02", 0, 1).stream()
                .map(RoleRepresentation::getId).toList(), contains(available.getId()));
        assertFalse(parentResource.getAvailableRoleComposites("organization", null, 0, 10).stream()
                .map(RoleRepresentation::getId).toList().contains(otherRole.getId()));
        assertThrows(BadRequestException.class,
                () -> parentResource.getAvailableRoleComposites("unknown", null, 0, 10));
    }

    @Test
    public void testOrganizationRoleMemberAddAtomicity() {
        OrganizationRepresentation organization = createOrganization("member-atomicity");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation role = createOrganizationRole(organizationResource, "atomicity-role");
        OrganizationRoleResource roleResource = organizationResource.roles().get(role.getId());

        MemberRepresentation firstMember = addMember(organizationResource, "atomic-first@member-atomicity.org");
        UserRepresentation firstUser = realm.admin().users().get(firstMember.getId()).toRepresentation();
        UserRepresentation outsider = createUser("atomic-outsider@member-atomicity.org");
        UserRepresentation missingUser = new UserRepresentation();
        missingUser.setId("missing-organization-role-user");

        assertThrows(NotFoundException.class, () -> roleResource.addUserMembers(List.of(firstUser, missingUser)));
        assertTrue(roleResource.getUserMembers().isEmpty());
        assertThrows(BadRequestException.class, () -> roleResource.addUserMembers(List.of(firstUser, outsider)));
        assertTrue(roleResource.getUserMembers().isEmpty());
    }

    @Test
    public void testOrganizationRoleMemberManagement() {
        OrganizationRepresentation organization = createOrganization("member-mgmt");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation role = createOrganizationRole(organizationResource, "mgmt-role");
        OrganizationRoleResource roleResource = organizationResource.roles().get(role.getId());

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("member-mgmt-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());

        MemberRepresentation firstMember = addMember(organizationResource, "mgmt-first@member-mgmt.org");
        MemberRepresentation secondMember = addMember(organizationResource, "mgmt-second@member-mgmt.org");
        UserRepresentation firstUser = realm.admin().users().get(firstMember.getId()).toRepresentation();
        UserRepresentation secondUser = realm.admin().users().get(secondMember.getId()).toRepresentation();
        UserRepresentation missingUser = new UserRepresentation();
        missingUser.setId("missing-organization-role-user");

        roleResource.addUserMembers(List.of(firstUser, secondUser));
        assertEquals(2, roleResource.getUserMembers().size());
        assertThat(roleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                containsInAnyOrder(firstUser.getId(), secondUser.getId()));

        assertFalse(realm.admin().users().get(firstUser.getId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(role.getId()));
        assertFalse(realm.admin().users().get(firstUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(role.getId()));
        assertThrows(NotFoundException.class,
                () -> realm.admin().users().get(firstUser.getId()).roles().realmLevel().add(List.of(role)));
        assertThrows(NotFoundException.class,
                () -> realm.admin().users().get(firstUser.getId()).roles().clientLevel(clientId).add(List.of(role)));
        assertThrows(NotFoundException.class, () -> roleResource.deleteUserMembers(List.of(firstUser, missingUser)));
        assertThat(roleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                containsInAnyOrder(firstUser.getId(), secondUser.getId()));
    }

    @Test
    public void testDefaultRoleBlocksDirectMembership() {
        OrganizationRepresentation organization = createOrganization("default-role-block");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        MemberRepresentation member = addMember(organizationResource, "default-block@default-role-block.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();

        OrganizationRoleResource defaultRole = organizationResource.roles().getDefault();
        assertThat(defaultRole.getUserMembers().stream().map(UserRepresentation::getId).toList(), contains(user.getId()));
        assertThat(defaultRole.getUserMembers("default-block", true, 0, 1).stream()
                .map(UserRepresentation::getId).toList(), contains(user.getId()));
        assertTrue(defaultRole.getUserMembers(1, 1).isEmpty());
        assertTrue(defaultRole.getAvailableUserMembers(null, null, true, 0, 10).isEmpty());
        assertThrows(BadRequestException.class, () -> defaultRole.addUserMembers(List.of(user)));
        assertThrows(BadRequestException.class, () -> defaultRole.deleteUserMembers(List.of(user)));
    }

    @Test
    public void testOrganizationRoleHiddenFromRolesById() {
        OrganizationRepresentation organization = createOrganization("roles-by-id");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation orgRole = createOrganizationRole(organizationResource, "hidden-role");

        String realmRoleName = "roles-by-id-realm-role";
        realm.admin().roles().create(new RoleRepresentation(realmRoleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(realmRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(realmRoleName).toRepresentation();

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("roles-by-id-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());
        String clientRoleName = "roles-by-id-client-role";
        realm.admin().clients().get(clientId).roles().create(new RoleRepresentation(clientRoleName, null, true));
        RoleRepresentation clientRole = realm.admin().clients().get(clientId).roles().get(clientRoleName).toRepresentation();

        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().getRole(orgRole.getId()));
        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().updateRole(orgRole.getId(), orgRole));
        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().deleteRole(orgRole.getId()));
        assertThrows(NotFoundException.class,
                () -> realm.admin().rolesById().addComposites(orgRole.getId(), List.of(realmRole)));
        assertThrows(BadRequestException.class,
                () -> realm.admin().roles().get(realmRole.getName()).addComposites(List.of(orgRole)));
        assertThrows(BadRequestException.class,
                () -> realm.admin().clients().get(clientId).roles().get(clientRole.getName()).addComposites(List.of(orgRole)));
    }

    @Test
    public void testOrganizationRoleIsolationFromGroupsAndScopes() {
        OrganizationRepresentation organization = createOrganization("groups-scopes");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation orgRole = createOrganizationRole(organizationResource, "shared-role");
        RoleRepresentation assigned = createOrganizationRole(organizationResource, "01-assigned");

        String sharedRoleName = "shared-role";
        realm.admin().roles().create(new RoleRepresentation(sharedRoleName, "Realm role with the same name", false));
        realm.cleanup().add(r -> r.roles().deleteRole(sharedRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(sharedRoleName).toRepresentation();

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("groups-scopes-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());
        realm.admin().clients().get(clientId).roles().create(new RoleRepresentation(sharedRoleName, "Client role with the same name", true));
        RoleRepresentation clientRole = realm.admin().clients().get(clientId).roles().get(sharedRoleName).toRepresentation();

        GroupRepresentation group = createGroup(realm.admin(), "groups-scopes-group");
        realm.cleanup().add(r -> r.groups().group(group.getId()).remove());
        assertThrows(NotFoundException.class,
                () -> realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(orgRole)));
        assertThrows(NotFoundException.class,
                () -> realm.admin().groups().group(group.getId()).roles().clientLevel(clientId).add(List.of(orgRole)));

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("groups-scopes-scope");
        clientScope.setProtocol("openid-connect");
        String clientScopeId;
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientScopeId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clientScopes().get(clientScopeId).remove());
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().add(List.of(orgRole)));
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().remove(List.of(orgRole)));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().add(List.of(realmRole));
        assertThat(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(realmRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().remove(List.of(realmRole));
        assertFalse(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(realmRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).add(List.of(orgRole));
        assertThat(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).remove(List.of(clientRole));
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).add(List.of(assigned)));
    }

    @Test
    public void testOrganizationRoleMembersUseDirectMappings() {
        OrganizationRepresentation organization = createOrganization("direct-role-members");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation parent = createOrganizationRole(organizationResource, "direct-role-parent");
        RoleRepresentation child = createOrganizationRole(organizationResource, "direct-role-child");
        OrganizationRoleResource parentResource = organizationResource.roles().get(parent.getId());
        OrganizationRoleResource childResource = organizationResource.roles().get(child.getId());
        parentResource.addComposites(List.of(child));

        MemberRepresentation member = addMember(organizationResource, "direct-role-member@example.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();
        parentResource.addUserMembers(List.of(user));

        assertThat(parentResource.getUserMembers().stream().map(UserRepresentation::getId).toList(), contains(user.getId()));
        assertTrue(childResource.getUserMembers().isEmpty());
        assertThat(childResource.getAvailableUserMembers(null, null, true, 0, 10).stream()
                .map(UserRepresentation::getId).toList(), contains(user.getId()));

        childResource.addUserMembers(List.of(user));
        assertThat(childResource.getUserMembers().stream().map(UserRepresentation::getId).toList(), contains(user.getId()));

        childResource.deleteUserMembers(List.of(user));
        assertTrue(childResource.getUserMembers().isEmpty());
    }

    @Test
    public void testAdminUiRoleMappingsFilterOrganizationRoles() {
        OrganizationRepresentation organization = createOrganization("admin-ui-role-mappings");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "admin-ui-organization-role");
        MemberRepresentation member = addMember(organizationResource, "admin-ui-role-member@example.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();
        organizationResource.roles().get(organizationRole.getId()).addUserMembers(List.of(user));

        String realmRoleName = "admin-ui-realm-role";
        RoleRepresentation realmRole = new RoleRepresentation(realmRoleName, null, false);
        realm.admin().roles().create(realmRole);
        realm.cleanup().add(r -> r.roles().deleteRole(realmRoleName));
        realmRole = realm.admin().roles().get(realmRoleName).toRepresentation();

        String organizationGroupRealmRoleName = "admin-ui-organization-group-realm-role";
        realm.admin().roles().create(new RoleRepresentation(organizationGroupRealmRoleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(organizationGroupRealmRoleName));
        RoleRepresentation organizationGroupRealmRole = realm.admin().roles()
                .get(organizationGroupRealmRoleName).toRepresentation();

        String realmCompositeName = "admin-ui-realm-composite";
        RoleRepresentation realmComposite = new RoleRepresentation(realmCompositeName, null, false);
        realm.admin().roles().create(realmComposite);
        realm.cleanup().add(r -> r.roles().deleteRole(realmCompositeName));
        realmComposite = realm.admin().roles().get(realmCompositeName).toRepresentation();

        ClientRepresentation roleClient = new ClientRepresentation();
        roleClient.setClientId("admin-ui-role-source");
        roleClient.setEnabled(true);
        String roleClientId;
        try (Response response = realm.admin().clients().create(roleClient)) {
            roleClientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(roleClientId).remove());
        realm.admin().clients().get(roleClientId).roles().create(new RoleRepresentation("admin-ui-mapped-client-role", null, true));
        realm.admin().clients().get(roleClientId).roles().create(new RoleRepresentation("admin-ui-available-client-role", null, true));
        realm.admin().clients().get(roleClientId).roles()
                .create(new RoleRepresentation("admin-ui-organization-group-client-role", null, true));
        RoleRepresentation mappedClientRole = realm.admin().clients().get(roleClientId).roles()
                .get("admin-ui-mapped-client-role").toRepresentation();
        RoleRepresentation organizationGroupClientRole = realm.admin().clients().get(roleClientId).roles()
                .get("admin-ui-organization-group-client-role").toRepresentation();

        ClientRepresentation targetClient = new ClientRepresentation();
        targetClient.setClientId("admin-ui-role-target");
        targetClient.setEnabled(true);
        String targetClientId;
        try (Response response = realm.admin().clients().create(targetClient)) {
            targetClientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(targetClientId).remove());

        GroupRepresentation group = createGroup(realm.admin(), "admin-ui-role-group");
        realm.cleanup().add(r -> r.groups().group(group.getId()).remove());
        GroupRepresentation organizationGroup = new GroupRepresentation();
        organizationGroup.setName("admin-ui-organization-role-group");
        try (Response response = organizationResource.groups().addTopLevelGroup(organizationGroup)) {
            organizationGroup.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation organizationChild = new GroupRepresentation();
        organizationChild.setName("admin-ui-organization-role-child");
        try (Response response = organizationResource.groups().group(organizationGroup.getId()).addSubGroup(organizationChild)) {
            organizationChild.setId(ApiUtil.getCreatedId(response));
        }

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("admin-ui-role-client-scope");
        clientScope.setProtocol("openid-connect");
        String clientScopeId;
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            clientScopeId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clientScopes().get(clientScopeId).remove());

        realm.admin().users().get(user.getId()).roles().realmLevel().add(List.of(realmRole));
        realm.admin().users().get(user.getId()).roles().clientLevel(roleClientId).add(List.of(mappedClientRole));
        realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(realmRole));
        realm.admin().groups().group(group.getId()).roles().clientLevel(roleClientId).add(List.of(mappedClientRole));
        organizationResource.groups().group(organizationGroup.getId()).roles().realmLevel()
                .add(List.of(organizationGroupRealmRole));
        organizationResource.groups().group(organizationGroup.getId()).roles().clientLevel(roleClientId)
                .add(List.of(organizationGroupClientRole));
        organizationResource.groups().group(organizationGroup.getId()).addMember(user.getId());
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().add(List.of(realmRole));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(roleClientId).add(List.of(mappedClientRole));
        realm.admin().clients().get(targetClientId).getScopeMappings().realmLevel().add(List.of(realmRole));
        realm.admin().clients().get(targetClientId).getScopeMappings().clientLevel(roleClientId).add(List.of(mappedClientRole));
        realm.admin().roles().get(realmComposite.getName()).addComposites(List.of(realmRole, mappedClientRole));

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget uiExt = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin").path("realms").path(realm.getName()).path("ui-ext")
                    .register(new BearerAuthFilter(adminClient.tokenManager()));

            assertAvailableClientRole(uiExt, "users", user.getId());
            assertAvailableClientRole(uiExt, "groups", group.getId());
            assertAvailableClientRole(uiExt, "clientScopes", clientScopeId);
            assertAvailableClientRole(uiExt, "clients", targetClientId);

            assertThat(getRoleMappings(uiExt, "effective-roles", "groups", group.getId()).stream()
                    .map(role -> role.get("role")).toList(), hasItem("admin-ui-mapped-client-role"));
            assertThat(getRoleMappings(uiExt, "effective-roles-all", "groups", group.getId()).stream()
                    .map(role -> role.get("name")).toList(), hasItem(realmRole.getName()));
            assertThat(getRoleMappings(uiExt, "effective-roles", "groups", organizationGroup.getId()).stream()
                    .map(role -> role.get("role")).toList(), hasItem("admin-ui-organization-group-client-role"));
            assertThat(getRoleMappings(uiExt, "effective-roles-all", "groups", organizationGroup.getId()).stream()
                    .map(role -> role.get("name")).toList(), hasItem(organizationGroupRealmRoleName));

            String organizationId = organization.getId();
            String rootId = runOnServer.fetch(session -> {
                OrganizationModel model = session.getProvider(OrganizationProvider.class).getById(organizationId);
                return session.getProvider(OrganizationProvider.class).getOrganizationGroup(model).getId();
            }, String.class);
            assertNotFound(uiExt.path("effective-roles").path("groups").path(rootId));

            setStoredGroupOrganization(organizationGroup.getId(), null);
            try {
                assertNotFound(uiExt.path("effective-roles-all").path("groups").path(organizationGroup.getId()));
            } finally {
                setStoredGroupOrganization(organizationGroup.getId(), organizationId);
            }

            setStoredGroupType(organizationGroup.getId(), GroupModel.Type.REALM);
            try {
                assertNotFound(uiExt.path("effective-roles").path("groups").path(organizationChild.getId()));
            } finally {
                setStoredGroupType(organizationGroup.getId(), GroupModel.Type.ORGANIZATION);
            }

            setStoredGroupParent(organizationChild.getId(), GroupEntity.TOP_PARENT_ID);
            try {
                assertNotFound(uiExt.path("effective-roles-all").path("groups").path(organizationChild.getId()));
            } finally {
                setStoredGroupParent(organizationChild.getId(), organizationGroup.getId());
            }

            List<Map<String, Object>> effectiveClientRoles = getRoleMappings(uiExt, "effective-roles", "users", user.getId());
            assertThat(effectiveClientRoles.stream().map(role -> role.get("role")).toList(),
                    hasItem("admin-ui-mapped-client-role"));
            assertThat(effectiveClientRoles.stream().map(role -> role.get("role")).toList(),
                    hasItem("admin-ui-organization-group-client-role"));

            List<Map<String, Object>> allEffectiveRoles = getRoleMappings(uiExt, "effective-roles-all", "users", user.getId());
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(), hasItem(realmRole.getName()));
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(),
                    hasItem("admin-ui-mapped-client-role"));
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(),
                    hasItem(organizationGroupRealmRoleName));
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(),
                    hasItem("admin-ui-organization-group-client-role"));
            assertFalse(allEffectiveRoles.stream().map(role -> role.get("name")).toList()
                    .contains(organizationRole.getName()));

            try (Response response = uiExt.path("role-mappings").path("roles").path(realmComposite.getId())
                    .request(MediaType.APPLICATION_JSON).get()) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                Map<String, Object> mappings = response.readEntity(new GenericType<>() {});
                assertTrue(mappings.containsKey("realmMappings"));
                assertTrue(mappings.containsKey("clientMappings"));
            }

            assertNotFound(uiExt.path("available-roles").path("roles").path(organizationRole.getId())
                    .queryParam("first", 0).queryParam("max", 10));
            assertNotFound(uiExt.path("effective-roles-all").path("roles").path(organizationRole.getId()));
            assertNotFound(uiExt.path("role-mappings").path("roles").path(organizationRole.getId()));
            assertNotFound(uiExt.path("role-mapping-delete").path("roles").path(organizationRole.getId()),
                    List.of(new RoleDeleteRequest(realmRole.getId(), realmRole.getName(), null)));
            assertNotFound(uiExt.path("role-mapping-delete").path("users").path(user.getId()),
                    List.of(new RoleDeleteRequest(organizationRole.getId(), organizationRole.getName(), null)));

            assertThat(organizationResource.roles().get(organizationRole.getId()).getUserMembers().stream()
                    .map(UserRepresentation::getId).toList(), contains(user.getId()));

            try (Response response = uiExt.path("role-mapping-delete").path("users").path(user.getId())
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(List.of(new RoleDeleteRequest(realmRole.getId(), realmRole.getName(), null))))) {
                assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
            assertFalse(realm.admin().users().get(user.getId()).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getId).toList().contains(realmRole.getId()));
        }
    }

    private void assertAvailableClientRole(WebTarget uiExt, String type, String id) {
        List<Map<String, Object>> roles = getRoleMappings(uiExt, "available-roles", type, id,
                Map.of("search", "admin-ui-available-client-role", "first", 0, "max", 10));
        assertThat(roles.stream().map(role -> role.get("role")).toList(), contains("admin-ui-available-client-role"));
    }

    private void setStoredGroupOrganization(String groupId, String organizationId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            GroupEntity group = em.find(GroupEntity.class, groupId);
            group.setOrganization(organizationId == null ? null : em.getReference(OrganizationEntity.class, organizationId));
            em.flush();
        });
        realm.admin().clearRealmCache();
    }

    private void setStoredGroupType(String groupId, GroupModel.Type type) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.find(GroupEntity.class, groupId).setType(type.intValue());
            em.flush();
        });
        realm.admin().clearRealmCache();
    }

    private void setStoredGroupParent(String groupId, String parentId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.find(GroupEntity.class, groupId).setParentId(parentId);
            em.flush();
        });
        realm.admin().clearRealmCache();
    }

    private List<Map<String, Object>> getRoleMappings(WebTarget uiExt, String endpoint, String type, String id) {
        return getRoleMappings(uiExt, endpoint, type, id, Map.of());
    }

    private List<Map<String, Object>> getRoleMappings(WebTarget uiExt, String endpoint, String type, String id,
            Map<String, Object> query) {
        WebTarget target = uiExt.path(endpoint).path(type).path(id);
        for (Map.Entry<String, Object> parameter : query.entrySet()) {
            target = target.queryParam(parameter.getKey(), parameter.getValue());
        }
        try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.readEntity(new GenericType<>() {});
        }
    }

    private void assertNotFound(WebTarget target) {
        try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    private void assertNotFound(WebTarget target, List<RoleDeleteRequest> roles) {
        try (Response response = target.request(MediaType.APPLICATION_JSON).post(Entity.json(roles))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    private RoleRepresentation createOrganizationRole(OrganizationResource organization, String name) {
        RoleRepresentation role = new RoleRepresentation(name, name + " description", false);
        try (Response response = organization.roles().create(role)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            return organization.roles().get(ApiUtil.getCreatedId(response)).toRepresentation();
        }
    }

    private RoleRepresentation createRealmRole(String name) {
        realm.admin().roles().create(new RoleRepresentation(name, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(name));
        return realm.admin().roles().get(name).toRepresentation();
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

    @Test
    public void testPreventAdminRoleAsDirectComposite() {
        OrganizationRepresentation organization = createOrganization("prevent-admin-direct");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        final RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();

        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "test-org-role");
        OrganizationRoleResource roleResource = organizationResource.roles().get(organizationRole.getId());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> roleResource.addComposites(List.of(adminRole)));
        assertEquals(400, exception.getResponse().getStatus());
    }

    @Test
    public void testAllAdminRolesBlocked() {
        OrganizationRepresentation organization = createOrganization("all-admin-roles-blocked");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "test-all-admin-roles");
        OrganizationRoleResource roleResource = organizationResource.roles().get(organizationRole.getId());

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);

        String[] adminRolesToTest = {
                AdminRoles.MANAGE_REALM,
                AdminRoles.MANAGE_USERS,
                AdminRoles.MANAGE_CLIENTS,
                AdminRoles.MANAGE_ORGANIZATIONS
        };

        for (String adminRoleName : adminRolesToTest) {
            final RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                    .roles().get(adminRoleName).toRepresentation();

            assertThrows(BadRequestException.class,
                    () -> roleResource.addComposites(List.of(adminRole)),
                    "Should prevent admin role: " + adminRoleName);
        }
    }

    @Test
    public void testOrganizationRoleRejectsExistingTransitiveAdminComposite() {
        OrganizationRepresentation organization = createOrganization("transitive-admin-composite");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation orgRole = createOrganizationRole(organizationResource, "transitive-admin-org-role");
        OrganizationRoleResource orgRoleResource = organizationResource.roles().get(orgRole.getId());

        String customRealmRoleName = "transitive-admin-realm-role";
        realm.admin().roles().create(new RoleRepresentation(customRealmRoleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(customRealmRoleName));
        RoleRepresentation customRealmRole = realm.admin().roles().get(customRealmRoleName).toRepresentation();

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        realm.admin().roles().get(customRealmRoleName).addComposites(List.of(adminRole));

        assertThrows(BadRequestException.class, () -> orgRoleResource.addComposites(List.of(customRealmRole)));

        MemberRepresentation member = addMember(organizationResource, "transitive-admin@transitive-admin-composite.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();
        orgRoleResource.addUserMembers(List.of(user));
        assertThat(orgRoleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                contains(user.getId()));
        assertTrue(orgRoleResource.getRoleComposites().isEmpty());
    }

    @Test
    public void testOrganizationRoleCannotBePoisonedThroughIntermediateRealmRole() {
        OrganizationRepresentation organization = createOrganization("org-role-poison");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation orgRole = createOrganizationRole(organizationResource, "victim-org-role");
        OrganizationRoleResource orgRoleResource = organizationResource.roles().get(orgRole.getId());

        String realmRoleName = "intermediate-realm-role";
        realm.admin().roles().create(new RoleRepresentation(realmRoleName, null, false));
        realm.cleanup().add(r -> r.roles().deleteRole(realmRoleName));
        RoleRepresentation realmRole = realm.admin().roles().get(realmRoleName).toRepresentation();

        orgRoleResource.addComposites(List.of(realmRole));

        MemberRepresentation member = addMember(organizationResource, "victim@org-role-poison.org");
        UserRepresentation user = realm.admin().users().get(member.getId()).toRepresentation();
        orgRoleResource.addUserMembers(List.of(user));
        assertEquals(1, orgRoleResource.getUserMembers().size());
        orgRoleResource.deleteUserMembers(List.of(user));

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        realm.admin().users().get(user.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(adminRole));
        assertThrows(BadRequestException.class, () -> realm.admin().roles().get(realmRoleName).addComposites(List.of(adminRole)));

        assertTrue(realm.admin().roles().get(realmRoleName).getRoleComposites().isEmpty());
        assertThat(realm.admin().users().get(user.getId()).roles().clientLevel(realmManagement.getId()).listAll()
                .stream().map(RoleRepresentation::getId).toList(), hasItem(adminRole.getId()));
        orgRoleResource.addUserMembers(List.of(user));
        assertThat(orgRoleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                contains(user.getId()));
    }

    @Test
    public void testMultiHopCycleAndCompositeBatchCannotPoisonOrganizationRole() {
        OrganizationRepresentation organization = createOrganization("multi-hop-role-graph");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "multi-hop-org-role");
        OrganizationRoleResource organizationRoleResource = organizationResource.roles().get(organizationRole.getId());
        RoleRepresentation first = createRealmRole("multi-hop-first");
        RoleRepresentation second = createRealmRole("multi-hop-second");
        RoleRepresentation rolledBack = createRealmRole("multi-hop-rolled-back");
        RoleRepresentation reverseOrder = createRealmRole("multi-hop-reverse-order");
        RoleRepresentation reverseRolledBack = createRealmRole("multi-hop-reverse-rolled-back");
        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();

        organizationRoleResource.addComposites(List.of(first, reverseOrder));
        realm.admin().roles().get(first.getName()).addComposites(List.of(second));
        realm.admin().roles().get(second.getName()).addComposites(List.of(first));

        organizationRoleResource.getRoleComposites();
        realm.admin().roles().get(first.getName()).getRoleComposites();
        realm.admin().roles().get(second.getName()).getRoleComposites();

        assertThrows(BadRequestException.class, () -> realm.admin().roles().get(second.getName()).addComposites(List.of(rolledBack, adminRole)));
        assertThat(realm.admin().roles().get(second.getName()).getRoleComposites().stream()
                .map(RoleRepresentation::getId).toList(), contains(first.getId()));

        assertThrows(BadRequestException.class, () -> realm.admin().roles().get(reverseOrder.getName()).addComposites(List.of(adminRole, reverseRolledBack)));
        assertTrue(realm.admin().roles().get(reverseOrder.getName()).getRoleComposites().isEmpty());

        String organizationRoleId = organizationRole.getId();
        runOnServer.run(session -> {
            RoleModel storedOrganizationRole = session.getContext().getRealm().getRoleById(organizationRoleId);
            RoleModel storedAdminRole = session.getContext().getRealm()
                    .getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).getRole(AdminRoles.MANAGE_REALM);
            assertFalse(storedOrganizationRole.hasRole(storedAdminRole));
        });
    }

    @Test
    public void testAdministrativeClassificationChangesRejectAnchoredRoles() {
        OrganizationRepresentation organization = createOrganization("classification-change-graph");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "classification-org-role");
        OrganizationRoleResource organizationRoleResource = organizationResource.roles().get(organizationRole.getId());

        String renameRoleName = "classification-safe-role";
        String organizationRoleId = organizationRole.getId();
        String renameRoleId = runOnServer.fetch(session -> {
            RoleModel storedOrganizationRole = session.getContext().getRealm().getRoleById(organizationRoleId);
            RoleModel safeRole = session.roles().addClientRole(session.getContext().getRealm()
                    .getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID), renameRoleName);
            storedOrganizationRole.addCompositeRole(safeRole);
            return safeRole.getId();
        }, String.class);
        runOnServer.run(session -> {
            RoleModel safeRole = session.getContext().getRealm().getRoleById(renameRoleId);
            assertThrows(org.keycloak.models.ModelException.class, () -> safeRole.setName(AdminRoles.MANAGE_USERS));
            assertEquals(renameRoleName, safeRole.getName());
        });
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("classification-safe-client");
        client.setEnabled(true);
        String clientUuid;
        try (Response response = realm.admin().clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        realm.admin().clients().get(clientUuid).roles()
                .create(new RoleRepresentation(AdminRoles.MANAGE_CLIENTS, null, false));
        RoleRepresentation clientRole = realm.admin().clients().get(clientUuid).roles()
                .get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        organizationRoleResource.addComposites(List.of(clientRole));

        ClientRepresentation changedClient = realm.admin().clients().get(clientUuid).toRepresentation();
        changedClient.setClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        assertThrows(BadRequestException.class, () -> realm.admin().clients().get(clientUuid).update(changedClient));
        assertEquals("classification-safe-client", realm.admin().clients().get(clientUuid).toRepresentation().getClientId());

        RoleRepresentation realmAdminName = createRealmRole(AdminRoles.ADMIN);
        organizationRoleResource.addComposites(List.of(realmAdminName));
        String realmAdminRoleId = realmAdminName.getId();
        String originalRealmName = realm.getName();
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel storedRealm = session.getContext().getRealm();
            assertEquals(AdminRoles.ADMIN, storedRealm.getRoleById(realmAdminRoleId).getName());
            assertThrows(org.keycloak.models.ModelException.class, () -> storedRealm.setName("master"));
            assertEquals(originalRealmName, storedRealm.getName());
        });

        String clientRoleId = clientRole.getId();
        runOnServer.run(session -> assertThat(session.getContext().getRealm().getRoleById(organizationRoleId)
                .getCompositesStream().map(RoleModel::getId).toList(),
                containsInAnyOrder(renameRoleId, clientRoleId, realmAdminRoleId)));
    }

    @Test
    public void testNonAdminRolesAllowed() {
        OrganizationRepresentation organization = createOrganization("non-admin-roles-allowed");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());
        MemberRepresentation member = addMember(organizationResource, "non-admin-roles@example.org");
        UserRepresentation orgMember = realm.admin().users().get(member.getId()).toRepresentation();

        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "custom-org-role");
        OrganizationRoleResource roleResource = organizationResource.roles().get(organizationRole.getId());

        String customRealmRoleName = "custom-realm-role";
        RoleRepresentation customRealmRole = new RoleRepresentation(customRealmRoleName, "Custom non-admin realm role", false);
        realm.admin().roles().create(customRealmRole);
        realm.cleanup().add(r -> r.roles().deleteRole(customRealmRoleName));
        customRealmRole = realm.admin().roles().get(customRealmRoleName).toRepresentation();

        roleResource.addComposites(List.of(customRealmRole));
        assertThat(roleResource.getRoleComposites().stream().map(RoleRepresentation::getName).toList(),
                contains(customRealmRoleName));

        roleResource.addUserMembers(List.of(orgMember));
        assertThat(roleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                contains(orgMember.getId()));

        roleResource.deleteUserMembers(List.of(orgMember));
        assertTrue(roleResource.getUserMembers().isEmpty());
    }

    @Test
    public void testMultipleOrganizationIsolation() {
        OrganizationRepresentation org1 = createOrganization("multi-org-iso-org1");
        OrganizationRepresentation org2 = createOrganization("multi-org-iso-org2");

        OrganizationResource org1Resource = realm.admin().organizations().get(org1.getId());
        OrganizationResource org2Resource = realm.admin().organizations().get(org2.getId());

        MemberRepresentation member1 = addMember(org1Resource, "multi-org-member1@example.org");
        UserRepresentation user1 = realm.admin().users().get(member1.getId()).toRepresentation();

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        final RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();

        RoleRepresentation org1Role = createOrganizationRole(org1Resource, "org1-test-role");
        RoleRepresentation org2Role = createOrganizationRole(org2Resource, "org2-test-role");

        OrganizationRoleResource org1RoleResource = org1Resource.roles().get(org1Role.getId());
        OrganizationRoleResource org2RoleResource = org2Resource.roles().get(org2Role.getId());

        assertThrows(BadRequestException.class, () -> org1RoleResource.addComposites(List.of(adminRole)));
        assertThrows(BadRequestException.class, () -> org2RoleResource.addComposites(List.of(adminRole)));

        org1RoleResource.addUserMembers(List.of(user1));
        assertThat(org1RoleResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                contains(user1.getId()));

        assertThrows(BadRequestException.class, () -> org2RoleResource.addUserMembers(List.of(user1)));
    }

    @Test
    public void testEmptyOrganizationCanReceiveRoles() {
        OrganizationRepresentation organization = createOrganization("empty-org-roles");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());

        UserRepresentation nonMemberUser = createUser("non-member-empty-org@example.org");

        RoleRepresentation organizationRole = createOrganizationRole(organizationResource, "empty-org-role");
        OrganizationRoleResource roleResource = organizationResource.roles().get(organizationRole.getId());

        assertThrows(BadRequestException.class, () -> roleResource.addUserMembers(List.of(nonMemberUser)));

        assertTrue(roleResource.getUserMembers().isEmpty());
    }

    @Test
    public void testPreventAdminRoleAssignmentToOrgGroup() {
        OrganizationRepresentation organization = createOrganization("prevent-admin-org-group");
        OrganizationResource organizationResource = realm.admin().organizations().get(organization.getId());

        String orgGroupName = "admin-test-org-group";
        GroupRepresentation orgGroup = new GroupRepresentation();
        orgGroup.setName(orgGroupName);
        try (Response response = organizationResource.groups().addTopLevelGroup(orgGroup)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        GroupRepresentation group = organizationResource.groups().getAll(orgGroupName, null, true, 0, 10, false, false).get(0);

        var realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        final RoleRepresentation adminRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_USERS).toRepresentation();

        assertThrows(BadRequestException.class,
                () -> organizationResource.groups().group(group.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(adminRole)),
                "Should prevent assigning admin role to organization group");
    }

    @Test
    public void testPreventCrossOrganizationRoleAssignmentToGroup() {
        OrganizationRepresentation org1 = createOrganization("cross-org-test-org1");
        OrganizationRepresentation org2 = createOrganization("cross-org-test-org2");
        
        OrganizationResource org1Resource = realm.admin().organizations().get(org1.getId());
        OrganizationResource org2Resource = realm.admin().organizations().get(org2.getId());

        RoleRepresentation org1Role = createOrganizationRole(org1Resource, "same-org-test-role");
        RoleRepresentation org2Role = createOrganizationRole(org2Resource, "cross-org-test-role");

        // Create a group in org1
        String groupName = "cross-org-test-group";
        GroupRepresentation group = new GroupRepresentation();
        group.setName(groupName);
        try (Response response = org1Resource.groups().addTopLevelGroup(group)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        GroupRepresentation org1Group = org1Resource.groups().getAll(groupName, null, true, 0, 10, false, false).get(0);

        var org1RoleMappings = org1Resource.groups().group(org1Group.getId()).roles();
        assertThat(org1RoleMappings.getAvailableOrganizationRoleMappings().stream().map(RoleRepresentation::getId).toList(),
                contains(org1Role.getId()));
        org1RoleMappings.addOrganizationRoleMappings(List.of(org1Role));
        assertThat(org1RoleMappings.getAll().getOrganizationMappings().get(org1.getAlias()).stream()
                .map(RoleRepresentation::getId).toList(), contains(org1Role.getId()));
        assertThrows(BadRequestException.class, () -> org1RoleMappings.addOrganizationRoleMappings(List.of(org2Role)),
                "Should prevent assigning a role from a different organization to a group");
    }

    @Test
    public void testPreventCrossOrganizationRoleDeletionFromGroup() {
        OrganizationRepresentation org1 = createOrganization("cross-org-delete-org1");
        OrganizationRepresentation org2 = createOrganization("cross-org-delete-org2");
        
        OrganizationResource org1Resource = realm.admin().organizations().get(org1.getId());
        OrganizationResource org2Resource = realm.admin().organizations().get(org2.getId());

        RoleRepresentation org2Role = createOrganizationRole(org2Resource, "cross-org-delete-role");

        // Create a group in org1
        String groupName = "cross-org-delete-group";
        GroupRepresentation group = new GroupRepresentation();
        group.setName(groupName);
        try (Response response = org1Resource.groups().addTopLevelGroup(group)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        GroupRepresentation org1Group = org1Resource.groups().getAll(groupName, null, true, 0, 10, false, false).get(0);

        // Attempt to delete org2's role from org1's group
        // This should fail because the role belongs to a different organization
        assertThrows(BadRequestException.class,
                () -> org1Resource.groups().group(org1Group.getId()).roles()
                        .deleteOrganizationRoleMappings(List.of(org2Role)),
                "Should prevent deleting a role from a different organization from a group");
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
