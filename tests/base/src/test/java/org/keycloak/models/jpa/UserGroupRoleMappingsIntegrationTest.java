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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.infinispan.Cache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
@DatabaseTest
public class UserGroupRoleMappingsIntegrationTest {

    @InjectRealm(config = OrganizationGroupsRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void directJpaSeparatesPublicGroupsFromRoleMappingMemberships() {
        runOnServer.run(session -> {
            RealmModel realm = directRealms(session).getRealm(session.getContext().getRealm().getId());
            GroupProvider groups = directGroups(session);
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("group-view-org", "Group view", "group-view");
            GroupModel root = organizations.getOrganizationGroup(organization);
            GroupModel hiddenGroup = organizations.createGroup(organization, "00-match-hidden", null);
            RoleModel hiddenRole = session.roles().addRealmRole(realm, "hidden-group-role");
            hiddenGroup.grantRole(hiddenRole);

            GroupModel firstPublic = groups.createGroup(realm, "10-match-one");
            GroupModel secondPublic = groups.createGroup(realm, "20-match-two");
            GroupModel otherPublic = groups.createGroup(realm, "30-other");
            UserModel user = directUsers(session).addUser(realm, "group-view-user");
            assertTrue(organizations.addMember(organization, user));
            user.joinGroup(hiddenGroup);
            user.joinGroup(firstPublic);
            user.joinGroup(secondPublic);
            user.joinGroup(otherPublic);

            RealmModel otherRealm = directRealms(session).createRealm("group-view-other-realm");
            try {
                GroupModel crossRealmGroup = groups.createGroup(otherRealm, "cross-realm");
                user.joinGroup(crossRealmGroup);

                assertEquals(List.of("10-match-one", "20-match-two", "30-other"), names(user.getGroupsStream()));
                assertEquals(List.of("10-match-one", "20-match-two", "30-other"),
                        names(user.getGroupsStream(null, null, null)));
                assertEquals(List.of("10-match-one"), names(user.getGroupsStream(null, 0, 1)));
                assertEquals(List.of("10-match-one"), names(user.getGroupsStream("MATCH", 0, 1)));
                assertEquals(List.of("20-match-two"), names(user.getGroupsStream("match", 1, 1)));
                assertEquals(List.of("20-match-two"), names(user.getGroupsStream(null, 1, 1)));
                assertEquals(List.of("10-match-one"), names(user.getGroupsStream("", 0, 1)));
                assertTrue(user.getGroupsStream("missing", 0, 1).findAny().isEmpty());
                assertTrue(user.getGroupsStream(null, 3, 1).findAny().isEmpty());
                assertTrue(user.getGroupsStream(null, 0, 0).findAny().isEmpty());
                assertEquals(3, user.getGroupsCount());
                assertEquals(3, user.getGroupsCountByNameContaining(null));
                assertEquals(2, user.getGroupsCountByNameContaining("MATCH"));
                assertEquals(0, user.getGroupsCountByNameContaining("missing"));

                assertEquals(Set.of(root.getId(), hiddenGroup.getId(), firstPublic.getId(),
                                secondPublic.getId(), otherPublic.getId()),
                        user.getRoleMappingsGroupsStream().map(GroupModel::getId).collect(Collectors.toSet()));
                assertTrue(user.isMemberOf(hiddenGroup));
                assertTrue(user.hasRole(hiddenRole));
            } finally {
                directRealms(session).removeRealm(otherRealm.getId());
            }

            UserModel unmappedUser = directUsers(session).addUser(realm, "empty-group-role-user");
            assertTrue(unmappedUser.getGroupsStream().findAny().isEmpty());
            assertTrue(unmappedUser.getRoleMappingsGroupsStream().findAny().isEmpty());
            assertTrue(unmappedUser.getRoleMappingsStream().noneMatch(role -> role.isType(RoleModel.Type.ORGANIZATION)));
            assertTrue(RoleUtils.getDeepUserRoleMappings(unmappedUser).stream()
                    .noneMatch(role -> role.isType(RoleModel.Type.ORGANIZATION)));
        });
    }

    @Test
    public void userCachePreservesCompleteGroupsAcrossMissAndHit() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("group-cache-org", "Group cache", "group-cache");
            GroupModel hiddenParent = organizations.createGroup(organization, "00-cache-parent", null);
            GroupModel hiddenGroup = organizations.createGroup(organization, "01-cache-hidden", hiddenParent);
            RoleModel hiddenRole = session.roles().addRealmRole(realm, "cache-hidden-role");
            hiddenParent.grantRole(hiddenRole);
            GroupModel publicGroup = session.groups().createGroup(realm, "10-cache-public");
            UserModel user = session.users().addUser(realm, "group-cache-user");
            assertTrue(organizations.addMember(organization, user));
            user.joinGroup(hiddenGroup);
            user.joinGroup(publicGroup);
            return new String[] { user.getId(), hiddenParent.getId(), hiddenRole.getId() };
        }, String[].class);

        runOnServer.run(session -> assertCachedViews(session, ids, false));
        runOnServer.run(session -> assertCachedViews(session, ids, true));
    }

    @Test
    public void userCacheTracksCommittedMutationsAndRollback() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("group-cache-transaction-org",
                    "Group cache transaction", "group-cache-transaction");
            GroupModel root = organizations.getOrganizationGroup(organization);
            GroupModel hiddenBefore = organizations.createGroup(organization, "00-cache-before", null);
            GroupModel hiddenAfter = organizations.createGroup(organization, "01-cache-after", null);
            RoleModel roleBefore = session.roles().addRealmRole(realm, "cache-role-before");
            RoleModel roleAfter = session.roles().addRealmRole(realm, "cache-role-after");
            GroupModel publicBefore = session.groups().createGroup(realm, "10-cache-before");
            GroupModel publicAfter = session.groups().createGroup(realm, "20-cache-after");
            UserModel user = session.users().addUser(realm, "group-cache-transaction-user");

            hiddenBefore.grantRole(roleBefore);
            assertTrue(organizations.addMember(organization, user));
            user.joinGroup(hiddenBefore);
            user.joinGroup(publicBefore);

            return new String[] { user.getId(), root.getId(), hiddenBefore.getId(), hiddenAfter.getId(),
                    publicBefore.getId(), publicAfter.getId(), roleBefore.getId(), roleAfter.getId() };
        }, String[].class);

        // T2 loads the user through Infinispan and warms the complete and public views.
        runOnServer.run(session -> assertCacheTransactionState(session, ids, false, false));
        runOnServer.run(session -> assertCacheTransactionState(session, ids, false, true));

        // T3 starts from a cache hit and exercises the updated delegate before commit.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, ids[0]);
            GroupModel hiddenBefore = realm.getGroupById(ids[2]);
            GroupModel hiddenAfter = realm.getGroupById(ids[3]);
            GroupModel publicBefore = realm.getGroupById(ids[4]);
            GroupModel publicAfter = realm.getGroupById(ids[5]);
            RoleModel roleBefore = realm.getRoleById(ids[6]);
            RoleModel roleAfter = realm.getRoleById(ids[7]);

            assertTrue(user instanceof CachedUserModel);
            user.leaveGroup(hiddenBefore);
            user.joinGroup(hiddenAfter);
            user.leaveGroup(publicBefore);
            user.joinGroup(publicAfter);
            hiddenBefore.deleteRoleMapping(roleBefore);
            hiddenAfter.grantRole(roleAfter);

            assertFalse(session.users().getUserById(realm, ids[0]) instanceof CachedUserModel);
            assertCacheTransactionState(session, ids, true, null);
        });

        // T4 observes the committed invalidation as a miss, then the next transaction as a hit.
        runOnServer.run(session -> assertCacheTransactionState(session, ids, true, false));
        runOnServer.run(session -> assertCacheTransactionState(session, ids, true, true));

        // Apply the inverse state through the updated delegate, but roll the whole transaction back.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, ids[0]);
            GroupModel hiddenBefore = realm.getGroupById(ids[2]);
            GroupModel hiddenAfter = realm.getGroupById(ids[3]);
            GroupModel publicBefore = realm.getGroupById(ids[4]);
            GroupModel publicAfter = realm.getGroupById(ids[5]);
            RoleModel roleBefore = realm.getRoleById(ids[6]);
            RoleModel roleAfter = realm.getRoleById(ids[7]);

            assertTrue(user instanceof CachedUserModel);
            user.leaveGroup(hiddenAfter);
            user.joinGroup(hiddenBefore);
            user.leaveGroup(publicAfter);
            user.joinGroup(publicBefore);
            hiddenAfter.deleteRoleMapping(roleAfter);
            hiddenBefore.grantRole(roleBefore);

            assertFalse(session.users().getUserById(realm, ids[0]) instanceof CachedUserModel);
            assertCacheTransactionState(session, ids, false, null);
            session.getTransactionManager().setRollbackOnly();
        });

        // A fresh model lookup must retain the last committed state, independently of eviction on rollback.
        runOnServer.run(session -> assertCacheTransactionState(session, ids, true, null));
        runOnServer.run(session -> assertCacheTransactionState(session, ids, true, true));
    }

    @Test
    public void serviceAccountsResolveOrganizationGroupAncestorsAndHomonymousRoles() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create("service-account-org", "Service account",
                    "service-account");
            GroupModel root = organizations.getOrganizationGroup(organization);
            GroupModel parent = organizations.createGroup(organization, "role-parent", null);
            GroupModel child = organizations.createGroup(organization, "role-child", parent);
            RoleModel organizationRole = organization.addRole("shared-role-name");
            RoleModel defaultOrganizationRole = organization.getDefaultRole();
            RoleModel realmRole = session.roles().addRealmRole(realm, "shared-role-name");
            child.grantRole(realmRole);

            ClientModel client = session.clients().addClient(realm, "group-role-service-account");
            new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            UserModel serviceAccount = session.users().getServiceAccount(client);
            assertTrue(organizations.addMember(organization, serviceAccount));
            serviceAccount.grantRole(organizationRole);
            serviceAccount.joinGroup(child);
            serviceAccount.joinGroup(child);

            assertEquals(client.getId(), serviceAccount.getServiceAccountClientLink());
            assertTrue(serviceAccount.getGroupsStream().findAny().isEmpty());
            assertEquals(Set.of(root.getId(), child.getId()), serviceAccount.getRoleMappingsGroupsStream()
                    .map(GroupModel::getId).collect(Collectors.toSet()));
            assertTrue(serviceAccount.isMemberOf(child));
            assertTrue(serviceAccount.isMemberOf(parent));
            assertTrue(serviceAccount.hasRole(defaultOrganizationRole));
            assertFalse(serviceAccount.hasDirectRole(defaultOrganizationRole));
            assertTrue(serviceAccount.hasRole(organizationRole));
            assertTrue(serviceAccount.hasRole(realmRole));

            Set<RoleModel> deepMappings = RoleUtils.getDeepUserRoleMappings(serviceAccount);
            assertTrue(deepMappings.contains(organizationRole));
            assertTrue(deepMappings.contains(realmRole));
            assertEquals(2, deepMappings.stream()
                    .filter(role -> "shared-role-name".equals(role.getName()))
                    .count());

            assertTrue(organizations.removeMember(organization, serviceAccount));
            assertFalse(serviceAccount.hasRole(defaultOrganizationRole));
            assertFalse(serviceAccount.hasRole(organizationRole));
            assertFalse(serviceAccount.hasRole(realmRole));
        });
    }

    private static void assertCachedViews(KeycloakSession session, String[] ids, boolean cacheHit) {
        RealmModel realm = session.getContext().getRealm();
        UserModel directUser = directUsers(session).getUserById(realm, ids[0]);
        Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("users");
        assertEquals(cacheHit, cache.containsKey(directUser.getId()));

        UserModel user = session.users().getUserById(realm, directUser.getId());
        assertTrue(user instanceof CachedUserModel);
        assertEquals(List.of("10-cache-public"), names(user.getGroupsStream()));
        Set<String> completeGroupNames = user.getRoleMappingsGroupsStream().map(GroupModel::getName)
                .collect(Collectors.toSet());
        assertEquals(3, completeGroupNames.size());
        assertTrue(completeGroupNames.containsAll(Set.of("01-cache-hidden", "10-cache-public")));
        assertTrue(user.isMemberOf(realm.getGroupById(ids[1])));
        assertTrue(user.hasRole(realm.getRoleById(ids[2])));
        assertTrue(cache.containsKey(directUser.getId()));
        if (!cacheHit) {
            assertFalse(directUser instanceof CachedUserModel);
        }
    }

    private static void assertCacheTransactionState(KeycloakSession session, String[] ids, boolean after,
            Boolean cacheHit) {
        RealmModel realm = session.getContext().getRealm();
        UserModel directUser = directUsers(session).getUserById(realm, ids[0]);
        Cache<Object, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("users");
        if (cacheHit != null) {
            assertEquals(cacheHit, cache.containsKey(directUser.getId()));
        }

        UserModel user = session.users().getUserById(realm, directUser.getId());
        GroupModel root = realm.getGroupById(ids[1]);
        GroupModel hiddenBefore = realm.getGroupById(ids[2]);
        GroupModel hiddenAfter = realm.getGroupById(ids[3]);
        GroupModel publicBefore = realm.getGroupById(ids[4]);
        GroupModel publicAfter = realm.getGroupById(ids[5]);
        RoleModel roleBefore = realm.getRoleById(ids[6]);
        RoleModel roleAfter = realm.getRoleById(ids[7]);

        if (cacheHit != null) {
            assertTrue(user instanceof CachedUserModel);
        }
        assertEquals(List.of(after ? "20-cache-after" : "10-cache-before"), names(user.getGroupsStream()));
        assertEquals(Set.of(root.getId(), after ? hiddenAfter.getId() : hiddenBefore.getId(),
                        after ? publicAfter.getId() : publicBefore.getId()),
                user.getRoleMappingsGroupsStream().map(GroupModel::getId).collect(Collectors.toSet()));
        assertEquals(!after, user.isMemberOf(hiddenBefore));
        assertEquals(after, user.isMemberOf(hiddenAfter));
        assertEquals(!after, user.isMemberOf(publicBefore));
        assertEquals(after, user.isMemberOf(publicAfter));
        assertEquals(!after, user.hasRole(roleBefore));
        assertEquals(after, user.hasRole(roleAfter));

        Set<RoleModel> deepMappings = RoleUtils.getDeepUserRoleMappings(user);
        assertEquals(!after, deepMappings.contains(roleBefore));
        assertEquals(after, deepMappings.contains(roleAfter));
        if (cacheHit != null) {
            assertTrue(cache.containsKey(directUser.getId()));
        }
    }

    private static List<String> names(java.util.stream.Stream<GroupModel> groups) {
        return groups.map(GroupModel::getName).toList();
    }

    private static JpaRealmProvider directRealms(KeycloakSession session) {
        return (JpaRealmProvider) session.getProvider(RealmProvider.class, JpaRealmProviderFactory.PROVIDER_ID);
    }

    private static GroupProvider directGroups(KeycloakSession session) {
        return session.getProvider(GroupProvider.class, JpaRealmProviderFactory.PROVIDER_ID);
    }

    private static JpaUserProvider directUsers(KeycloakSession session) {
        return (JpaUserProvider) session.getProvider(UserProvider.class, JpaRealmProviderFactory.PROVIDER_ID);
    }

    public static final class OrganizationGroupsRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
