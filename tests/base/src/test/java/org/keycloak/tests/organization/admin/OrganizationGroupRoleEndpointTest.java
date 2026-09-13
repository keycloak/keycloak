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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationGroupResource;
import org.keycloak.admin.client.resource.OrganizationGroupsResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.jpa.entities.CompositeRoleEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.GroupRoleMappingEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the organization-context endpoints used by the Admin Console to manage regular organization roles on
 * visible organization groups. The organization's internal root and its default role remain private invariants.
 */
@KeycloakIntegrationTest
public class OrganizationGroupRoleEndpointTest extends AbstractOrganizationTest {

    private static final class FailingGroupUpdateListener implements ProviderEventListener, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final FailingGroupUpdateListener INSTANCE = new FailingGroupUpdateListener();

        private volatile boolean armed;

        @Override
        public void onEvent(ProviderEvent event) {
            if (armed && event instanceof GroupModel.GroupUpdatedEvent) {
                armed = false;
                throw new IllegalStateException("injected organization group role mapping failure");
            }
        }
    }

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private OrganizationRepresentation organization;
    private OrganizationResource organizationResource;
    private OrganizationGroupsResource groups;
    private GroupRepresentation orgGroup;

    @BeforeEach
    public void setup() {
        organization = createOrganization("acme", "Acme");
        organizationResource = realm.admin().organizations().get(organization.getId());
        groups = organizationResource.groups();
        orgGroup = new GroupRepresentation();
        orgGroup.setName("dev-team");
        try (Response response = groups.addTopLevelGroup(orgGroup)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            orgGroup.setId(ApiUtil.getCreatedId(response));
        }
    }

    @Test
    public void availableRolesTrackDirectMappingsAndExcludeTheDefaultByIdentity() {
        RoleRepresentation admin = createRole("admin");
        RoleRepresentation editor = createRole("default-roles-business-editor");
        RoleMappingResource mappings = mappings();

        assertThat(roleNames(mappings.getAvailableOrganizationRoleMappings()),
                containsInAnyOrder(admin.getName(), editor.getName()));

        mappings.addOrganizationRoleMappings(List.of(admin));
        assertThat(roleNames(mappings.getAvailableOrganizationRoleMappings()), containsInAnyOrder(editor.getName()));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(admin.getName()));

        mappings.deleteOrganizationRoleMappings(List.of(admin));
        assertThat(roleNames(mappings.getAvailableOrganizationRoleMappings()),
                containsInAnyOrder(admin.getName(), editor.getName()));
        assertThat(mappedRoleNames(mappings.getAll()), empty());
    }

    @Test
    public void addAndDeleteAreBatchCapableAndIdempotent() {
        RoleRepresentation admin = createRole("admin");
        RoleRepresentation editor = createRole("editor");
        RoleMappingResource mappings = mappings();

        mappings.addOrganizationRoleMappings(List.of(admin, editor, admin));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(admin.getName(), editor.getName()));
        assertThat(roleNames(mappings.getAvailableOrganizationRoleMappings()), empty());

        mappings.deleteOrganizationRoleMappings(List.of(admin, admin));
        mappings.deleteOrganizationRoleMappings(List.of(admin));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(editor.getName()));

        mappings.deleteOrganizationRoleMappings(List.of(editor));
        assertThat(mappedRoleNames(mappings.getAll()), empty());
    }

    @Test
    public void invalidMixedBatchesDoNotPartiallyAddOrDelete() {
        RoleRepresentation valid = createRole("batch-role");
        RoleRepresentation missing = new RoleRepresentation();
        missing.setId("missing-role-id");
        RoleMappingResource mappings = mappings();

        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(valid, missing)));
        assertThat(mappedRoleNames(mappings.getAll()), empty());
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(missing, valid)));
        assertThat(mappedRoleNames(mappings.getAll()), empty());

        mappings.addOrganizationRoleMappings(List.of(valid));
        assertThrows(BadRequestException.class, () -> mappings.deleteOrganizationRoleMappings(List.of(valid, missing)));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(valid.getName()));
        assertThrows(BadRequestException.class, () -> mappings.deleteOrganizationRoleMappings(List.of(missing, valid)));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(valid.getName()));
    }

    @Test
    public void validatesPayloadTypeOwnershipAndGenericGroupContext() {
        RoleMappingResource mappings = mappings();
        RoleRepresentation role = createRole("authoritative-name");
        RoleRepresentation missingId = new RoleRepresentation();
        RoleRepresentation missingRole = new RoleRepresentation();
        missingRole.setId("missing-role-id");

        mappings.addOrganizationRoleMappings(List.of());
        mappings.deleteOrganizationRoleMappings(List.of());
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(null));
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(Collections.singletonList(null)));
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(missingId)));
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(missingRole)));
        assertThrows(BadRequestException.class, () -> mappings.deleteOrganizationRoleMappings(null));
        assertThrows(BadRequestException.class, () -> mappings.deleteOrganizationRoleMappings(Collections.singletonList(null)));
        assertThrows(BadRequestException.class, () -> mappings.deleteOrganizationRoleMappings(List.of(missingId)));

        RoleRepresentation misleadingMetadata = new RoleRepresentation();
        misleadingMetadata.setId(role.getId());
        misleadingMetadata.setName("not-the-authoritative-name");
        mappings.addOrganizationRoleMappings(List.of(misleadingMetadata));
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(role.getName()));
        mappings.deleteOrganizationRoleMappings(List.of(misleadingMetadata));

        RoleRepresentation realmRole = new RoleRepresentation("realm-role", null, false);
        realm.admin().roles().create(realmRole);
        realmRole = realm.admin().roles().get(realmRole.getName()).toRepresentation();
        RoleRepresentation finalRealmRole = realmRole;
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(finalRealmRole)));

        OrganizationRepresentation otherOrganization = createOrganization("other", "Other");
        RoleRepresentation foreign = createRole(otherOrganization, "foreign-role");
        assertThrows(BadRequestException.class, () -> mappings.addOrganizationRoleMappings(List.of(foreign)));

        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("realm-group");
        try (Response response = realm.admin().groups().add(realmGroup)) {
            realmGroup.setId(ApiUtil.getCreatedId(response));
        }
        RoleMappingResource genericMappings = realm.admin().groups().group(realmGroup.getId()).roles();
        assertThat(genericMappings.getAvailableOrganizationRoleMappings(), empty());
        assertThrows(BadRequestException.class, () -> genericMappings.addOrganizationRoleMappings(List.of(role)));
        assertThrows(BadRequestException.class, () -> genericMappings.deleteOrganizationRoleMappings(List.of(role)));
    }

    @Test
    public void rejectsTheDefaultRoleAndDefaultAsACompositeChild() {
        RoleRepresentation defaultRole = organizationResource.roles().getDefault().toRepresentation();
        assertThrows(BadRequestException.class, () -> mappings().addOrganizationRoleMappings(List.of(defaultRole)));
        assertTrue(mappings().getAvailableOrganizationRoleMappings().stream()
                .noneMatch(role -> defaultRole.getId().equals(role.getId())));

        RoleRepresentation composite = createRole("composite-role");
        assertThrows(BadRequestException.class, () -> organizationResource.roles().get(composite.getId()).addComposites(List.of(defaultRole)));
    }

    @Test
    public void providerFailuresRollbackWholeAddAndDeleteBatches() {
        RoleRepresentation first = createRole("rollback-first");
        RoleRepresentation second = createRole("rollback-second");
        RoleMappingResource mappings = mappings();

        armFailingGroupUpdateListener();
        try {
            assertThrows(WebApplicationException.class, () -> mappings.addOrganizationRoleMappings(List.of(first, second)));
        } finally {
            disarmFailingGroupUpdateListener();
        }
        assertThat(mappedRoleNames(mappings.getAll()), empty());

        mappings.addOrganizationRoleMappings(List.of(first, second));
        armFailingGroupUpdateListener();
        try {
            assertThrows(WebApplicationException.class, () -> mappings.deleteOrganizationRoleMappings(List.of(first, second)));
        } finally {
            disarmFailingGroupUpdateListener();
        }
        assertThat(mappedRoleNames(mappings.getAll()), containsInAnyOrder(first.getName(), second.getName()));
    }

    @Test
    public void corruptRoleGraphsAndStoredMappingsStayUnavailable() {
        RoleRepresentation role = createRole("corrupt-role-graph");
        String roleId = role.getId();
        String groupId = orgGroup.getId();
        String organizationId = organization.getId();
        String[] storedIds = runOnServer.fetch(session -> {
            var realm = session.getContext().getRealm();
            var organization = session.getProvider(OrganizationProvider.class).getById(organizationId);
            var adminRole = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)
                    .getRole(AdminRoles.MANAGE_REALM);
            return new String[] { adminRole.getId(), organization.getDefaultRole().getId() };
        }, String[].class);

        persistComposite(roleId, storedIds[0]);
        try {
            assertTrue(mappings().getAvailableOrganizationRoleMappings().stream()
                    .noneMatch(candidate -> roleId.equals(candidate.getId())));
            assertThrows(BadRequestException.class, () -> mappings().addOrganizationRoleMappings(List.of(role)));
        } finally {
            removeComposite(roleId, storedIds[0]);
        }

        persistGroupMapping(groupId, storedIds[1]);
        try {
            assertThat(mappedRoleNames(mappings().getAll()), empty());
            assertThrows(BadRequestException.class, () -> groups.group(groupId).toRepresentation(false));
        } finally {
            removeGroupMapping(groupId, storedIds[1]);
        }
    }

    @Test
    public void genericAndCorruptOrganizationGroupPathsStayUnavailable() {
        GroupRepresentation target = new GroupRepresentation();
        target.setName("move-target");
        try (Response response = groups.addTopLevelGroup(target)) {
            target.setId(ApiUtil.getCreatedId(response));
        }

        String organizationId = organization.getId();
        String groupId = orgGroup.getId();
        String rootId = runOnServer.fetch(session -> {
            OrganizationModel model = session.getProvider(OrganizationProvider.class).getById(organizationId);
            return session.getProvider(OrganizationProvider.class).getOrganizationGroup(model).getId();
        }, String.class);
        setGroupParent(groupId, GroupEntity.TOP_PARENT_ID);
        try {
            assertThrows(NotFoundException.class, () -> groups.group(groupId).toRepresentation(false));
            GroupRepresentation move = new GroupRepresentation();
            move.setId(groupId);
            move.setName(orgGroup.getName());
            try (Response response = groups.group(target.getId()).addSubGroup(move)) {
                assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            }
        } finally {
            setGroupParent(groupId, rootId);
        }
    }

    @Test
    public void internalRootIsUnavailableThroughEveryPublicGroupOperation() {
        MemberRepresentation member = addMember(organizationResource);
        String organizationId = organization.getId();
        String[] rootDetails = runOnServer.fetch(session -> {
            OrganizationModel model = session.getProvider(OrganizationProvider.class).getById(organizationId);
            var root = session.getProvider(OrganizationProvider.class).getOrganizationGroup(model);
            return new String[] { root.getId(), root.getName() };
        }, String[].class);
        GroupRepresentation root = new GroupRepresentation();
        root.setId(rootDetails[0]);
        root.setName(rootDetails[1]);
        OrganizationGroupResource rootResource = groups.group(root.getId());

        assertThrows(NotFoundException.class, () -> rootResource.toRepresentation(false));
        try (Response response = rootResource.update(root)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        assertThrows(NotFoundException.class, rootResource::delete);
        assertThrows(NotFoundException.class, () -> rootResource.roles().getAll());
        assertThrows(NotFoundException.class, () -> rootResource.getSubGroups(null, null, null, null));
        assertThrows(NotFoundException.class, () -> rootResource.getMembers(null, null, true));
        assertThrows(NotFoundException.class, () -> rootResource.addMember(member.getId()));
        assertThrows(NotFoundException.class, () -> rootResource.removeMember(member.getId()));

        try (Response response = groups.addTopLevelGroup(root)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        try (Response response = groups.group(orgGroup.getId()).addSubGroup(root)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
        assertThrows(NotFoundException.class, () -> groups.getGroupByPath("/" + root.getName(), false));
    }

    private RoleMappingResource mappings() {
        return groups.group(orgGroup.getId()).roles();
    }

    private RoleRepresentation createRole(String name) {
        return createRole(organization, name);
    }

    private RoleRepresentation createRole(OrganizationRepresentation owner, String name) {
        RoleRepresentation role = new RoleRepresentation(name, null, false);
        try (Response response = realm.admin().organizations().get(owner.getId()).roles().create(role)) {
            role.setId(ApiUtil.getCreatedId(response));
        }
        return role;
    }

    private void armFailingGroupUpdateListener() {
        runOnServer.run(session -> {
            FailingGroupUpdateListener listener = FailingGroupUpdateListener.INSTANCE;
            session.getKeycloakSessionFactory().unregister(listener);
            listener.armed = true;
            session.getKeycloakSessionFactory().register(listener);
        });
    }

    private void disarmFailingGroupUpdateListener() {
        runOnServer.run(session -> {
            FailingGroupUpdateListener listener = FailingGroupUpdateListener.INSTANCE;
            listener.armed = false;
            session.getKeycloakSessionFactory().unregister(listener);
        });
    }

    private void persistComposite(String parentRoleId, String childRoleId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.persist(new CompositeRoleEntity(em.getReference(RoleEntity.class, parentRoleId),
                    em.getReference(RoleEntity.class, childRoleId)));
        });
        realm.admin().clearRealmCache();
    }

    private void removeComposite(String parentRoleId, String childRoleId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CompositeRoleEntity mapping = em.find(CompositeRoleEntity.class,
                    new CompositeRoleEntity.Key(em.getReference(RoleEntity.class, parentRoleId),
                            em.getReference(RoleEntity.class, childRoleId)));
            em.remove(mapping);
        });
        realm.admin().clearRealmCache();
    }

    private void persistGroupMapping(String groupId, String roleId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            GroupRoleMappingEntity mapping = new GroupRoleMappingEntity();
            mapping.setGroup(em.getReference(GroupEntity.class, groupId));
            mapping.setRoleId(roleId);
            em.persist(mapping);
        });
        realm.admin().clearRealmCache();
    }

    private void removeGroupMapping(String groupId, String roleId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.createQuery("delete from GroupRoleMappingEntity mapping where mapping.group.id = :groupId and mapping.roleId = :roleId")
                    .setParameter("groupId", groupId)
                    .setParameter("roleId", roleId)
                    .executeUpdate();
        });
        realm.admin().clearRealmCache();
    }

    private void setGroupParent(String groupId, String parentId) {
        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.find(GroupEntity.class, groupId).setParentId(parentId);
            em.flush();
        });
        realm.admin().clearRealmCache();
    }

    private static List<String> roleNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).toList();
    }

    private List<String> mappedRoleNames(MappingsRepresentation mappings) {
        Map<String, List<RoleRepresentation>> organizationMappings = mappings.getOrganizationMappings();
        if (organizationMappings == null) {
            return List.of();
        }
        List<RoleRepresentation> roles = organizationMappings.get(organization.getAlias());
        assertNotNull(roles);
        return roles.stream().map(RoleRepresentation::getName).toList();
    }
}
