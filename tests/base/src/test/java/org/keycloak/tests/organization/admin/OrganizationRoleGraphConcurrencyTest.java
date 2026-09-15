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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationRoleGraphConcurrencyTest {

    private static final String ORGANIZATION_ID = "concurrent-role-graph-org";

    @InjectRealm(config = OrganizationRoleGraphRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void concurrentEdgesCannotCreateAnAdministrativeOrganizationPath() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = session.getProvider(OrganizationProvider.class)
                    .create(ORGANIZATION_ID, "Concurrent role graph", ORGANIZATION_ID);
            RoleModel organizationRole = organization.addRole("concurrent-org-role");
            RoleModel intermediateRole = session.roles().addRealmRole(realm, "concurrent-intermediate-role");
            RoleModel adminRole = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID)
                    .getRole(AdminRoles.MANAGE_REALM);

            return new String[] { realm.getId(), organizationRole.getId(), intermediateRole.getId(), adminRole.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runRace(session.getKeycloakSessionFactory(), ids),
                String[].class);

        assertEquals(1, Arrays.stream(results).filter("COMMITTED"::equals).count());
        assertEquals(1, Arrays.stream(results).filter("REJECTED"::equals).count());

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            RoleModel organizationRole = realm.getRoleById(ids[1]);
            RoleModel intermediateRole = realm.getRoleById(ids[2]);
            RoleModel adminRole = realm.getRoleById(ids[3]);
            RoleModel controlParent = session.roles().addRealmRole(realm, "post-race-control-parent");
            RoleModel controlChild = session.roles().addRealmRole(realm, "post-race-control-child");

            assertFalse(organizationRole.hasRole(adminRole));
            assertTrue(organizationRole.hasRole(intermediateRole) ^ intermediateRole.hasRole(adminRole));

            controlParent.addCompositeRole(controlChild);
            assertTrue(controlParent.hasRole(controlChild));
        });
    }

    @Test
    public void concurrentDefaultSwitchesRemainAtomic() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = session.getProvider(OrganizationProvider.class)
                    .create(ORGANIZATION_ID, "Concurrent default", ORGANIZATION_ID);
            RoleModel first = organization.addRole("first-default-candidate");
            RoleModel second = organization.addRole("second-default-candidate");
            return new String[] { realm.getId(), organization.getId(), first.getId(), second.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runDefaultRace(session.getKeycloakSessionFactory(), ids,
                Operation.SWITCH_FIRST, Operation.SWITCH_SECOND), String[].class);
        assertEquals(2, Arrays.stream(results).filter("COMMITTED"::equals).count());

        assertSingleDefaultMapping(ids, ids[2], ids[3]);
    }

    @Test
    public void concurrentSwitchesToSameDefaultAreIdempotent() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = session.getProvider(OrganizationProvider.class)
                    .create(ORGANIZATION_ID, "Concurrent same default", ORGANIZATION_ID);
            RoleModel candidate = organization.addRole("shared-default-candidate");
            return new String[] { realm.getId(), organization.getId(), candidate.getId(), candidate.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runDefaultRace(session.getKeycloakSessionFactory(), ids,
                Operation.SWITCH_FIRST, Operation.SWITCH_SECOND), String[].class);
        assertEquals(2, Arrays.stream(results).filter("COMMITTED"::equals).count());

        assertSingleDefaultMapping(ids, ids[2]);
    }

    @Test
    public void concurrentSwitchAndDirectGrantCannotBothCommit() {
        String[] ids = runOnServer.fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create(ORGANIZATION_ID, "Concurrent grant", ORGANIZATION_ID);
            RoleModel candidate = organization.addRole("grant-race-candidate");
            UserModel user = session.users().addUser(realm, "grant-race-member");
            organizations.addMember(organization, user);
            return new String[] { realm.getId(), organization.getId(), candidate.getId(), user.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runDefaultRace(session.getKeycloakSessionFactory(), ids,
                Operation.SWITCH_FIRST, Operation.GRANT_SECOND), String[].class);
        assertEquals(1, Arrays.stream(results).filter("COMMITTED"::equals).count());
        assertEquals(1, Arrays.stream(results).filter("REJECTED"::equals).count());

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = session.getProvider(OrganizationProvider.class).getById(ids[1]);
            RoleModel candidate = realm.getRoleById(ids[2]);
            UserModel user = session.users().getUserById(realm, ids[3]);
            GroupModel root = session.getProvider(OrganizationProvider.class).getOrganizationGroup(organization);

            assertFalse(organization.isDefaultRole(candidate) && user.hasDirectRole(candidate));
            assertEquals(1, root.getRoleMappingsStream().count());
            assertTrue(root.hasDirectRole(organization.getDefaultRole()));
        });
    }

    @Test
    public void concurrentSwitchAndVisibleGroupGrantCannotBothCommitWithPreloadedModels() {
        String[] ids = runOnServer.fetch(session -> {
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.create(ORGANIZATION_ID, "Concurrent group grant",
                    ORGANIZATION_ID);
            RoleModel candidate = organization.addRole("group-grant-race-candidate");
            GroupModel group = organizations.createGroup(organization, "concurrent-group", null);
            return new String[] { session.getContext().getRealm().getId(), organization.getId(), candidate.getId(),
                    group.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runDefaultRace(session.getKeycloakSessionFactory(), ids,
                Operation.SWITCH_FIRST, Operation.GROUP_GRANT_SECOND), String[].class);
        assertEquals(1, Arrays.stream(results).filter("COMMITTED"::equals).count());
        assertEquals(1, Arrays.stream(results).filter("REJECTED"::equals).count());

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.getById(ids[1]);
            RoleModel candidate = realm.getRoleById(ids[2]);
            GroupModel group = realm.getGroupById(ids[3]);
            assertFalse(organization.isDefaultRole(candidate) && group.hasDirectRole(candidate));
            assertEquals(1, organizations.getOrganizationGroup(organization).getRoleMappingsStream().count());
        });
    }

    @Test
    public void concurrentSwitchAndCompositeChildCannotBothCommitWithPreloadedModels() {
        String[] ids = runOnServer.fetch(session -> {
            OrganizationModel organization = session.getProvider(OrganizationProvider.class)
                    .create(ORGANIZATION_ID, "Concurrent composite child", ORGANIZATION_ID);
            RoleModel candidate = organization.addRole("composite-child-race-candidate");
            RoleModel parent = organization.addRole("composite-parent");
            return new String[] { session.getContext().getRealm().getId(), organization.getId(), candidate.getId(),
                    parent.getId() };
        }, String[].class);

        String[] results = runOnServer.fetch(session -> runDefaultRace(session.getKeycloakSessionFactory(), ids,
                Operation.SWITCH_FIRST, Operation.COMPOSITE_CHILD_SECOND), String[].class);
        assertEquals(1, Arrays.stream(results).filter("COMMITTED"::equals).count());
        assertEquals(1, Arrays.stream(results).filter("REJECTED"::equals).count());

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationModel organization = session.getProvider(OrganizationProvider.class).getById(ids[1]);
            RoleModel candidate = realm.getRoleById(ids[2]);
            RoleModel parent = realm.getRoleById(ids[3]);
            assertFalse(organization.isDefaultRole(candidate) && parent.hasRole(candidate));
        });
    }

    private void assertSingleDefaultMapping(String[] ids, String... candidates) {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
            OrganizationModel organization = organizations.getById(ids[1]);
            GroupModel root = organizations.getOrganizationGroup(organization);

            assertEquals(1, root.getRoleMappingsStream().count());
            assertTrue(root.hasDirectRole(organization.getDefaultRole()));
            assertTrue(Arrays.stream(candidates).anyMatch(candidate -> candidate.equals(organization.getDefaultRole().getId())));
            assertEquals(1, Arrays.stream(candidates).map(realm::getRoleById).filter(root::hasDirectRole).count());
        });
    }

    private static String[] runRace(KeycloakSessionFactory sessionFactory, String[] ids) {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> organizationEdge = executor.submit(() -> addEdge(sessionFactory, ids[0], ids[1], ids[2],
                    ready, start));
            Future<String> adminEdge = executor.submit(() -> addEdge(sessionFactory, ids[0], ids[2], ids[3],
                    ready, start));

            assertTrue(assertDoesNotThrow(() -> ready.await(20, TimeUnit.SECONDS)),
                    "Concurrent role graph transactions did not reach the start barrier");
            start.countDown();

            return new String[] { assertDoesNotThrow(() -> organizationEdge.get(20, TimeUnit.SECONDS)),
                    assertDoesNotThrow(() -> adminEdge.get(20, TimeUnit.SECONDS)) };
        } finally {
            executor.shutdownNow();
        }
    }

    private static String[] runDefaultRace(KeycloakSessionFactory sessionFactory, String[] ids, Operation firstOperation,
            Operation secondOperation) {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(() -> mutateDefault(sessionFactory, ids, firstOperation, ready, start));
            Future<String> second = executor.submit(() -> mutateDefault(sessionFactory, ids, secondOperation, ready, start));

            assertTrue(assertDoesNotThrow(() -> ready.await(20, TimeUnit.SECONDS)),
                    "Concurrent default transactions did not reach the start barrier");
            start.countDown();

            return new String[] { assertDoesNotThrow(() -> first.get(20, TimeUnit.SECONDS)),
                    assertDoesNotThrow(() -> second.get(20, TimeUnit.SECONDS)) };
        } finally {
            executor.shutdownNow();
        }
    }

    private static String mutateDefault(KeycloakSessionFactory sessionFactory, String[] ids, Operation operation,
            CountDownLatch ready, CountDownLatch start) {
        try {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
                RealmModel realm = session.realms().getRealm(ids[0]);
                session.getContext().setRealm(realm);
                OrganizationModel organization = session.getProvider(OrganizationProvider.class).getById(ids[1]);
                RoleModel candidate = realm.getRoleById(ids[2]);
                RoleModel secondRole = operation == Operation.SWITCH_SECOND
                        || operation == Operation.COMPOSITE_CHILD_SECOND ? realm.getRoleById(ids[3]) : null;
                UserModel user = operation == Operation.GRANT_SECOND
                        ? session.users().getUserById(realm, ids[3]) : null;
                GroupModel group = operation == Operation.GROUP_GRANT_SECOND ? realm.getGroupById(ids[3]) : null;

                ready.countDown();
                await(start);
                switch (operation) {
                    case SWITCH_FIRST -> organization.setDefaultRole(candidate);
                    case SWITCH_SECOND -> organization.setDefaultRole(secondRole);
                    case GRANT_SECOND -> user.grantRole(candidate);
                    case GROUP_GRANT_SECOND -> {
                        if (group != null) group.grantRole(candidate);
                    }
                    case COMPOSITE_CHILD_SECOND -> {
                        if (secondRole != null) secondRole.addCompositeRole(candidate);
                    }
                }
            });
            return "COMMITTED";
        } catch (RuntimeException ignored) {
            return "REJECTED";
        }
    }

    private enum Operation {
        SWITCH_FIRST,
        SWITCH_SECOND,
        GRANT_SECOND,
        GROUP_GRANT_SECOND,
        COMPOSITE_CHILD_SECOND
    }

    private static String addEdge(KeycloakSessionFactory sessionFactory, String realmId, String parentId,
            String childId, CountDownLatch ready, CountDownLatch start) {
        try {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                RoleModel parent = realm.getRoleById(parentId);
                RoleModel child = realm.getRoleById(childId);

                ready.countDown();
                await(start);
                parent.addCompositeRole(child);
            });
            return "COMMITTED";
        } catch (RuntimeException ignored) {
            return "REJECTED";
        }
    }

    private static void await(CountDownLatch latch) {
        assertTrue(assertDoesNotThrow(() -> latch.await(20, TimeUnit.SECONDS)),
                "Timed out waiting for the concurrent role graph start barrier");
    }

    public static final class OrganizationRoleGraphRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
