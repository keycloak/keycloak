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
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.RoleModel;
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

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("organization-role-isolation-client");
        client.setEnabled(true);
        String clientId;
        try (Response response = realm.admin().clients().create(client)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            clientId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientId).remove());
        realm.admin().clients().get(clientId).roles().create(new RoleRepresentation(sharedRoleName,
                "Client role with the same name", true));
        RoleRepresentation clientRole = realm.admin().clients().get(clientId).roles().get(sharedRoleName).toRepresentation();
        assertThat(realm.admin().roles().list().stream().map(RoleRepresentation::getId).toList(),
                hasItem(realmRole.getId()));
        assertFalse(realm.admin().roles().list().stream().map(RoleRepresentation::getId).toList().contains(parent.getId()));
        assertThat(realm.admin().clients().get(clientId).roles().list().stream().map(RoleRepresentation::getId).toList(),
                hasItem(clientRole.getId()));
        assertFalse(realm.admin().clients().get(clientId).roles().list().stream()
                .map(RoleRepresentation::getId).toList().contains(parent.getId()));

        RoleRepresentation duplicate = new RoleRepresentation("shared-role", null, false);
        try (Response response = organizationResource.roles().create(duplicate)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }

        assertEquals(4L, organizationResource.roles().count(null));
        assertThat(organizationResource.roles().list("02-available", 0, 1, false), hasSize(1));
        assertEquals(available.getId(), organizationResource.roles().list("02-available", 0, 1, false).get(0).getId());
        assertThat(organizationResource.roles().list(0, 2), hasSize(2));

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
        assertFalse(realm.admin().users().get(firstUser.getId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(parent.getId()));
        assertFalse(realm.admin().users().get(firstUser.getId()).roles().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(parent.getId()));
        assertThrows(NotFoundException.class,
                () -> realm.admin().users().get(firstUser.getId()).roles().realmLevel().add(List.of(parent)));
        assertThrows(NotFoundException.class,
                () -> realm.admin().users().get(firstUser.getId()).roles().clientLevel(clientId).add(List.of(parent)));
        assertThrows(NotFoundException.class, () -> parentResource.deleteUserMembers(List.of(firstUser, missingUser)));
        assertThat(parentResource.getUserMembers().stream().map(UserRepresentation::getId).toList(),
                containsInAnyOrder(firstUser.getId(), secondUser.getId()));

        OrganizationRoleResource defaultRole = organizationResource.roles().getDefault();
        assertThrows(BadRequestException.class, () -> defaultRole.addUserMembers(List.of(firstUser)));
        assertThrows(BadRequestException.class, () -> defaultRole.deleteUserMembers(List.of(firstUser)));

        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().getRole(parent.getId()));
        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().updateRole(parent.getId(), parent));
        assertThrows(NotFoundException.class, () -> realm.admin().rolesById().deleteRole(parent.getId()));
        assertThrows(NotFoundException.class,
                () -> realm.admin().rolesById().addComposites(parent.getId(), List.of(realmRole)));
        assertThrows(BadRequestException.class,
                () -> realm.admin().roles().get(realmRole.getName()).addComposites(List.of(parent)));
        assertThrows(BadRequestException.class,
                () -> realm.admin().clients().get(clientId).roles().get(clientRole.getName()).addComposites(List.of(parent)));

        GroupRepresentation group = createGroup(realm.admin(), "organization-role-isolation-group");
        realm.cleanup().add(r -> r.groups().group(group.getId()).remove());
        assertThrows(NotFoundException.class,
                () -> realm.admin().groups().group(group.getId()).roles().realmLevel().add(List.of(parent)));
        assertThrows(NotFoundException.class,
                () -> realm.admin().groups().group(group.getId()).roles().clientLevel(clientId).add(List.of(parent)));

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
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().remove(List.of(parent)));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().add(List.of(realmRole));
        assertThat(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(realmRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().remove(List.of(realmRole));
        assertFalse(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().realmLevel().listAll().stream()
                .map(RoleRepresentation::getId).toList().contains(realmRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).add(List.of(parent));
        assertThat(realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).listAll().stream()
                .map(RoleRepresentation::getId).toList(), contains(clientRole.getId()));
        realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).remove(List.of(clientRole));
        assertThrows(NotFoundException.class,
                () -> realm.admin().clientScopes().get(clientScopeId).getScopeMappings().clientLevel(clientId).add(List.of(assigned)));

        parentResource.deleteUserMembers(List.of(firstUser, secondUser));
        parentResource.deleteComposites(List.of(assigned, realmRole));
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
        RoleRepresentation mappedClientRole = realm.admin().clients().get(roleClientId).roles()
                .get("admin-ui-mapped-client-role").toRepresentation();

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

            List<Map<String, Object>> effectiveClientRoles = getRoleMappings(uiExt, "effective-roles", "users", user.getId());
            assertThat(effectiveClientRoles.stream().map(role -> role.get("role")).toList(),
                    hasItem("admin-ui-mapped-client-role"));

            List<Map<String, Object>> allEffectiveRoles = getRoleMappings(uiExt, "effective-roles-all", "users", user.getId());
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(), hasItem(realmRole.getName()));
            assertThat(allEffectiveRoles.stream().map(role -> role.get("name")).toList(),
                    hasItem("admin-ui-mapped-client-role"));
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
