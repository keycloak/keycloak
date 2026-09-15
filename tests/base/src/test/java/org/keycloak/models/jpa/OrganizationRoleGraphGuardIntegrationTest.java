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

package org.keycloak.models.jpa;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.CompositeRoleEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
@DatabaseTest
public class OrganizationRoleGraphGuardIntegrationTest {

    @InjectRealm(config = OrganizationRoleGraphRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void directJpaAdaptersAllowSafeClassificationChanges() {
        runOnServer.run(session -> {
            RealmModel realm = directJpa(session).getRealm(session.getContext().getRealm().getId());
            RoleModel role = directJpa(session).addRealmRole(realm, "direct-safe-role");
            ClientModel client = directJpa(session).addClient(realm, "direct-safe-client");
            String originalRealmName = realm.getName();

            role.setName(AdminRoles.ADMIN);
            client.setClientId("direct-renamed-client");
            realm.setName(originalRealmName + "-renamed");
            realm.setName(originalRealmName);
            session.groups().createGroup(realm, "direct-parentless-group").setParent(null);

            assertEquals(AdminRoles.ADMIN, role.getName());
            assertEquals("direct-renamed-client", client.getClientId());
            assertEquals(originalRealmName, realm.getName());
        });
    }

    @Test
    public void directJpaAdaptersAllowUnanchoredAdministrativeClassificationChanges() {
        runOnServer.run(session -> {
            RealmModel adminRealm = directJpa(session).getRealmByName(Config.getAdminRealm());
            ClientModel client = directJpa(session).addClient(adminRealm, "direct-unanchored-client");

            try {
                RoleModel clientIdCandidate = directJpa(session).addClientRole(client, AdminRoles.ADMIN);
                client.setClientId("direct-unanchored" + AdminRoles.APP_SUFFIX);
                RoleModel roleNameCandidate = directJpa(session).addClientRole(client, "direct-unanchored-role");
                roleNameCandidate.setName(AdminRoles.MANAGE_USERS);

                assertEquals("direct-unanchored" + AdminRoles.APP_SUFFIX, client.getClientId());
                assertEquals(AdminRoles.ADMIN, clientIdCandidate.getName());
                assertEquals(AdminRoles.MANAGE_USERS, roleNameCandidate.getName());
            } finally {
                directJpa(session).removeClient(adminRealm, client.getId());
            }
        });
    }

    @Test
    public void directJpaRoleRenameRejectsAnAnchoredAdministrativeRole() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = createOrganization(session, "direct-role-rename-org");
            RoleModel organizationRole = organization.addRole("direct-role-rename-org-role");
            ClientModel client = directJpa(session).getClientByClientId(realm, Constants.REALM_MANAGEMENT_CLIENT_ID);
            RoleModel candidate = directJpa(session).addClientRole(client, "direct-safe-client-role");
            organizationRole.addCompositeRole(candidate);

            RoleModel directCandidate = directJpa(session).getRoleById(realm, candidate.getId());
            assertEquals(OrganizationRoleGraphGuard.UNSAFE_ROLE_GRAPH,
                    assertThrows(ModelValidationException.class, () -> directCandidate.setName(AdminRoles.MANAGE_USERS)).getMessage());
        });
    }

    @Test
    public void directJpaClientRenameRejectsAnchoredAdministrativeRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = createOrganization(session, "direct-client-rename-org");
            RoleModel organizationRole = organization.addRole("direct-client-rename-org-role");
            ClientModel candidate = directJpa(session).addClient(realm, "direct-client-rename-client");
            RoleModel candidateRole = directJpa(session).addClientRole(candidate, AdminRoles.MANAGE_CLIENTS);
            organizationRole.addCompositeRole(candidateRole);

            ClientModel directCandidate = directJpa(session).getClientById(realm, candidate.getId());
            assertEquals(OrganizationRoleGraphGuard.UNSAFE_ROLE_GRAPH, assertThrows(ModelValidationException.class,
                    () -> directCandidate.setClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)).getMessage());
        });
    }

    @Test
    public void directJpaRealmRenameRejectsAnchoredAdministrativeRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = createOrganization(session, "direct-realm-rename-org");
            RoleModel organizationRole = organization.addRole("direct-realm-rename-org-role");
            RoleModel candidate = directJpa(session).addRealmRole(realm, AdminRoles.ADMIN);
            organizationRole.addCompositeRole(candidate);

            RealmModel directRealm = directJpa(session).getRealm(realm.getId());
            assertEquals(OrganizationRoleGraphGuard.UNSAFE_ROLE_GRAPH,
                    assertThrows(ModelValidationException.class, () -> directRealm.setName(Config.getAdminRealm())).getMessage());
        });
    }

    @Test
    public void staleCachedRoleCannotHideAnAdministrativeCompositeFromTheStorageGrantGuard() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = createOrganization(session, "stale-cached-role-org");
            RoleModel storedRole = directJpa(session).addRealmRole(realm, "stale-cached-role");
            RoleModel cachedRole = realm.getRoleById(storedRole.getId());
            RoleModel adminRole = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)
                    .getRole(AdminRoles.MANAGE_REALM);
            GroupModel organizationGroup = session.getProvider(OrganizationProvider.class)
                    .createGroup(organization, "stale-cached-role-group", null);
            cachedRole.getCompositesStream().toList();
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.persist(new CompositeRoleEntity(em.getReference(RoleEntity.class, storedRole.getId()),
                    em.getReference(RoleEntity.class, adminRole.getId())));

            assertEquals(OrganizationRoleGraphGuard.UNSAFE_ROLE_GRAPH, assertThrows(ModelValidationException.class,
                    () -> organizationGroup.grantRole(cachedRole)).getMessage());
        });
    }

    @Test
    public void crossRealmCompositeRolesAreRejectedByTheStorageGuard() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel parent = directJpa(session).addRealmRole(realm, "direct-cross-realm-parent");
            RealmModel otherRealm = directJpa(session).createRealm("direct-cross-realm");
            try {
                RoleModel child = directJpa(session).addRealmRole(otherRealm, "direct-cross-realm-child");
                assertEquals("Composite roles and organization group mappings must stay within the same realm",
                        assertThrows(ModelValidationException.class, () -> parent.addCompositeRole(child)).getMessage());
            } finally {
                directJpa(session).removeRealm(otherRealm.getId());
            }
        });
    }

    @Test
    public void directJpaGuardEnforcesOrganizationGroupAndRoleTopology() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = createOrganization(session, "direct-topology-org");
            OrganizationModel other = createOrganization(session, "direct-topology-other-org");
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            GroupModel root = organizations.getOrganizationGroup(organization);
            GroupModel visible = organizations.createGroup(organization, "direct-topology-visible", null);
            GroupModel parent = organizations.createGroup(organization, "direct-topology-parent", null);
            GroupModel child = organizations.createGroup(organization, "direct-topology-child", parent);
            GroupModel realmGroup = session.groups().createGroup(realm, "direct-topology-realm-group");
            RoleModel organizationRole = organization.addRole("direct-topology-role");
            RoleModel otherRole = other.addRole("direct-topology-other-role");
            RoleModel realmRole = directJpa(session).addRealmRole(realm, "direct-topology-realm-role");
            RealmModel adminRealm = directJpa(session).getRealmByName(Config.getAdminRealm());

            return new String[] { realm.getId(), organization.getId(), root.getId(), visible.getId(), parent.getId(),
                    child.getId(), realmGroup.getId(), organization.getDefaultRole().getId(), organizationRole.getId(),
                    otherRole.getId(), realmRole.getId(), adminRealm.getId() };
        }, String[].class);

        assertGroupGrantRejected(ids, ids[6], ids[8]);
        assertGroupGrantRejected(ids, ids[2], ids[8]);
        assertGroupGrantRejected(ids, ids[3], ids[7]);
        assertCompositeRejected(ids, ids[10], ids[8]);
        assertCompositeRejected(ids, ids[8], ids[7]);
        assertCorruptRealmGroupAllowsRealmRole(ids);

        assertCorruptGroupRejected(ids, group -> group.setOrganization(null), ids[3]);
        assertCorruptOrganizationRejected(ids, organization -> organization.setRealmId(ids[11]), ids[3]);
        assertCorruptGroupRejected(ids, group -> group.setType(GroupModel.Type.REALM.intValue()), ids[5], ids[4]);
        assertCorruptGroupRejected(ids, group -> group.setParentId(ids[3]), ids[3], ids[2]);
        assertCorruptGroupRejected(ids, group -> group.setParentId(GroupEntity.TOP_PARENT_ID), ids[3]);
    }

    private void assertGroupGrantRejected(String[] ids, String groupId, String roleId) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            GroupModel group = session.groups().getGroupById(realm, groupId);
            assertThrows(ModelException.class, () -> group.grantRole(realm.getRoleById(roleId)));
        });
    }

    private void assertCompositeRejected(String[] ids, String parentRoleId, String childRoleId) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            assertThrows(ModelException.class, () -> realm.getRoleById(parentRoleId).addCompositeRole(realm.getRoleById(childRoleId)));
        });
    }

    private void assertCorruptGroupRejected(String[] ids, SerializableConsumer<GroupEntity> mutation,
            String targetGroupId) {
        assertCorruptGroupRejected(ids, mutation, targetGroupId, targetGroupId);
    }

    private void assertCorruptGroupRejected(String[] ids, SerializableConsumer<GroupEntity> mutation,
            String targetGroupId, String mutatedGroupId) {
        runOnServer.run(session -> {
            try {
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                mutation.accept(em.find(GroupEntity.class, mutatedGroupId));
                em.flush();
                RealmModel realm = session.getContext().getRealm();
                GroupModel group = session.groups().getGroupById(realm, targetGroupId);
                assertThrows(ModelException.class, () -> group.grantRole(realm.getRoleById(ids[10])));
            } finally {
                session.getTransactionManager().setRollbackOnly();
            }
        });
    }

    private void assertCorruptOrganizationRejected(String[] ids, SerializableConsumer<OrganizationEntity> mutation,
            String targetGroupId) {
        runOnServer.run(session -> {
            try {
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                mutation.accept(em.find(OrganizationEntity.class, ids[1]));
                em.flush();
                RealmModel realm = session.getContext().getRealm();
                GroupModel group = session.groups().getGroupById(realm, targetGroupId);
                assertThrows(ModelException.class, () -> group.grantRole(realm.getRoleById(ids[10])));
            } finally {
                session.getTransactionManager().setRollbackOnly();
            }
        });
    }

    private void assertCorruptRealmGroupAllowsRealmRole(String[] ids) {
        runOnServer.run(session -> {
            try {
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                em.find(GroupEntity.class, ids[6]).setOrganization(em.getReference(OrganizationEntity.class, ids[1]));
                em.flush();
                RealmModel realm = session.getContext().getRealm();
                GroupModel group = session.groups().getGroupById(realm, ids[6]);
                RoleModel role = realm.getRoleById(ids[10]);
                group.grantRole(role);
                assertTrue(group.hasRole(role));
            } finally {
                session.getTransactionManager().setRollbackOnly();
            }
        });
    }

    @FunctionalInterface
    private interface SerializableConsumer<T> extends java.util.function.Consumer<T>, java.io.Serializable {
    }

    private static JpaRealmProvider directJpa(KeycloakSession session) {
        return (JpaRealmProvider) session.getProvider(RealmProvider.class, JpaRealmProviderFactory.PROVIDER_ID);
    }

    private static OrganizationModel createOrganization(KeycloakSession session, String id) {
        return session.getProvider(OrganizationProvider.class).create(id, id, id);
    }

    public static final class OrganizationRoleGraphRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
