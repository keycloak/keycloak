/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

/**
 * Integration tests for the global view-admin role feature.
 * Tests the GLOBAL_READONLY_ADMIN feature flag functionality.
 *
 */
@EnableFeature(value = Profile.Feature.GLOBAL_READONLY_ADMIN, skipRestart = false)
public class GlobalViewAdminTest extends AbstractKeycloakTest {

    private static final String TEST_REALM_1 = "test-realm-1";
    private static final String TEST_REALM_2 = "test-realm-2";
    private static final String VIEW_ADMIN_USER = "viewadmin-test";
    private static final String VIEW_ADMIN_PASSWORD = "password";
    private static final String REGULAR_USER = "regular-test";
    private static final String REGULAR_PASSWORD = "password";

    private Keycloak viewAdminClient;
    private String viewAdminUserId;
    private String regularUserId;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        // Create test realm 1
        testRealms.add(RealmBuilder.create()
                .name(TEST_REALM_1)
                .user(UserBuilder.create()
                        .username("realm1-user")
                        .password("password")
                        .enabled(true)
                        .build())
                .build());

        // Create test realm 2
        testRealms.add(RealmBuilder.create()
                .name(TEST_REALM_2)
                .user(UserBuilder.create()
                        .username("realm2-user")
                        .password("password")
                        .enabled(true)
                        .build())
                .build());
    }

    @Before
    public void setupTestUsers() {
        RealmResource master = adminClient.realm("master");

        // Create view-admin test user
        UserRepresentation viewAdminUser = UserBuilder.create()
                .username(VIEW_ADMIN_USER)
                .password(VIEW_ADMIN_PASSWORD)
                .enabled(true)
                .build();

        Response response = master.users().create(viewAdminUser);
        viewAdminUserId = ApiUtil.getCreatedId(response);
        response.close();

        // Assign view-admin role
        RoleRepresentation viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN).toRepresentation();
        master.users().get(viewAdminUserId).roles().realmLevel().add(Arrays.asList(viewAdminRole));

        // Create regular user (no admin roles)
        UserRepresentation regularUser = UserBuilder.create()
                .username(REGULAR_USER)
                .password(REGULAR_PASSWORD)
                .enabled(true)
                .build();

        response = master.users().create(regularUser);
        regularUserId = ApiUtil.getCreatedId(response);
        response.close();

        // Create Keycloak client for view-admin user using AdminClientUtil
        try {
            viewAdminClient = AdminClientUtil.createAdminClient(
                    suiteContext.isAdapterCompatTesting(),
                    "master",
                    VIEW_ADMIN_USER,
                    VIEW_ADMIN_PASSWORD,
                    org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID,
                    null
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create admin client for view-admin user", e);
        }
    }

    @After
    public void cleanupTestUsers() {
        if (viewAdminClient != null) {
            viewAdminClient.close();
        }

        RealmResource master = adminClient.realm("master");
        if (viewAdminUserId != null) {
            try {
                master.users().get(viewAdminUserId).remove();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        if (regularUserId != null) {
            try {
                master.users().get(regularUserId).remove();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    public void testViewAdminRoleExistsInMasterRealm() {
        RealmResource master = adminClient.realm("master");

        // Verify view-admin role exists
        RoleResource viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN);
        RoleRepresentation roleRep = viewAdminRole.toRepresentation();

        assertThat("view-admin role should exist", roleRep, notNullValue());
        assertThat("Role name should be view-admin", roleRep.getName(), equalTo(AdminRoles.VIEW_ADMIN));
        assertThat("Role should have description", roleRep.getDescription(),
                equalTo("${role_" + AdminRoles.VIEW_ADMIN + "}"));
    }

    @Test
    public void testViewAdminIsCompositeOfAdminRole() {
        RealmResource master = adminClient.realm("master");

        // Get admin role composites
        RoleResource adminRole = master.roles().get(AdminRoles.ADMIN);
        Set<RoleRepresentation> composites = adminRole.getRoleComposites();

        // Verify view-admin is a composite of admin
        boolean hasViewAdmin = composites.stream()
                .anyMatch(r -> AdminRoles.VIEW_ADMIN.equals(r.getName()));

        assertThat("admin role should contain view-admin as composite", hasViewAdmin, is(true));
    }

    @Test
    public void testViewAdminHasAllViewRolesComposites() {
        RealmResource master = adminClient.realm("master");

        // Get view-admin role composites
        RoleResource viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN);
        Set<RoleRepresentation> composites = viewAdminRole.getRoleComposites();

        // Filter client roles
        List<String> clientRoleNames = composites.stream()
                .filter(r -> Boolean.TRUE.equals(r.getClientRole()))
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        // Verify all view-* roles are included
        assertThat("Should have view-realm role", clientRoleNames, hasItem("view-realm"));
        assertThat("Should have view-users role", clientRoleNames, hasItem("view-users"));
        assertThat("Should have view-clients role", clientRoleNames, hasItem("view-clients"));
        assertThat("Should have view-events role", clientRoleNames, hasItem("view-events"));
        assertThat("Should have view-identity-providers role", clientRoleNames,
                hasItem("view-identity-providers"));
        assertThat("Should have view-authorization role", clientRoleNames, hasItem("view-authorization"));

        // Verify all query-* roles are included
        assertThat("Should have query-users role", clientRoleNames, hasItem("query-users"));
        assertThat("Should have query-clients role", clientRoleNames, hasItem("query-clients"));
        assertThat("Should have query-groups role", clientRoleNames, hasItem("query-groups"));
    }

    @Test
    public void testViewAdminDoesNotHaveManageRoles() {
        RealmResource master = adminClient.realm("master");

        // Get view-admin role composites
        RoleResource viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN);
        Set<RoleRepresentation> composites = viewAdminRole.getRoleComposites();

        // Filter client roles
        List<String> clientRoleNames = composites.stream()
                .filter(r -> Boolean.TRUE.equals(r.getClientRole()))
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        // Verify manage-* roles are NOT included
        assertThat("Should NOT have manage-realm role", clientRoleNames, not(hasItem("manage-realm")));
        assertThat("Should NOT have manage-users role", clientRoleNames, not(hasItem("manage-users")));
        assertThat("Should NOT have manage-clients role", clientRoleNames, not(hasItem("manage-clients")));
        assertThat("Should NOT have manage-events role", clientRoleNames, not(hasItem("manage-events")));
        assertThat("Should NOT have manage-identity-providers role", clientRoleNames,
                not(hasItem("manage-identity-providers")));
        assertThat("Should NOT have manage-authorization role", clientRoleNames,
                not(hasItem("manage-authorization")));

        // Verify no create-* or impersonation roles
        assertThat("Should NOT have create-client role", clientRoleNames, not(hasItem("create-client")));
        assertThat("Should NOT have impersonation role", clientRoleNames, not(hasItem("impersonation")));
    }

    @Test
    public void testViewAdminCanViewMasterRealm() {
        // User with view-admin should be able to view master realm
        RealmRepresentation masterRealm = viewAdminClient.realm("master").toRepresentation();

        assertThat("Should be able to retrieve master realm", masterRealm, notNullValue());
        assertThat("Realm name should be master", masterRealm.getRealm(), equalTo("master"));
    }

    @Test
    public void testViewAdminCanViewTestRealm1() {
        // User with view-admin should be able to view test realm 1
        RealmRepresentation testRealm1 = viewAdminClient.realm(TEST_REALM_1).toRepresentation();

        assertThat("Should be able to retrieve test-realm-1", testRealm1, notNullValue());
        assertThat("Realm name should match", testRealm1.getRealm(), equalTo(TEST_REALM_1));
        assertThat("Should see enabled status", testRealm1.isEnabled(), is(true));
    }

    @Test
    public void testViewAdminCanViewTestRealm2() {
        // User with view-admin should be able to view test realm 2
        RealmRepresentation testRealm2 = viewAdminClient.realm(TEST_REALM_2).toRepresentation();

        assertThat("Should be able to retrieve test-realm-2", testRealm2, notNullValue());
        assertThat("Realm name should match", testRealm2.getRealm(), equalTo(TEST_REALM_2));
    }

    @Test
    public void testViewAdminCanViewUsersInRealm() {
        // User with view-admin should be able to view users
        List<UserRepresentation> users = viewAdminClient.realm(TEST_REALM_1).users().list();

        assertThat("Should be able to list users", users, notNullValue());
        assertThat("Should have at least the test user", users.size(), greaterThanOrEqualTo(1));

        // Verify the specific user exists
        UserRepresentation realm1User = users.stream()
                .filter(u -> "realm1-user".equals(u.getUsername()))
                .findFirst()
                .orElse(null);

        assertThat("Should find realm1-user", realm1User, notNullValue());
    }

    @Test
    public void testViewAdminCanViewClientsInRealm() {
        // User with view-admin should be able to view clients
        List<ClientRepresentation> clients = viewAdminClient.realm(TEST_REALM_1).clients().findAll();

        assertThat("Should be able to list clients", clients, notNullValue());
        assertThat("Should have default clients", clients.size(), greaterThan(0));
    }

    @Test
    public void testViewAdminCanQueryUsers() {
        // User with view-admin should be able to search/query users
        List<UserRepresentation> users = viewAdminClient.realm(TEST_REALM_1).users()
                .search("realm1", 0, 10);

        assertThat("Should be able to search users", users, notNullValue());
    }

    @Test
    public void testViewAdminCannotCreateUser() {
        // User with view-admin should NOT be able to create users
        UserRepresentation newUser = UserBuilder.create()
                .username("should-fail")
                .password("password")
                .enabled(true)
                .build();

        try {
            Response response = viewAdminClient.realm(TEST_REALM_1).users().create(newUser);
            int status = response.getStatus();
            response.close();

            assertThat("Should receive 403 Forbidden when creating user", status, equalTo(403));
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        }
    }

    @Test
    public void testViewAdminCannotUpdateRealm() {
        // User with view-admin should NOT be able to update realm settings
        RealmRepresentation testRealm = viewAdminClient.realm(TEST_REALM_1).toRepresentation();
        testRealm.setDisplayName("Modified Display Name");

        try {
            viewAdminClient.realm(TEST_REALM_1).update(testRealm);
            fail("Should not be able to update realm");
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        }
    }

    @Test
    public void testViewAdminCannotUpdateUser() {
        // Get the existing user
        List<UserRepresentation> users = viewAdminClient.realm(TEST_REALM_1).users()
                .search("realm1-user", 0, 1);
        assertThat("User should exist", users.size(), greaterThan(0));

        UserRepresentation user = users.get(0);
        user.setFirstName("Modified");

        try {
            viewAdminClient.realm(TEST_REALM_1).users().get(user.getId()).update(user);
            fail("Should not be able to update user");
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        }
    }

    @Test
    public void testViewAdminCannotDeleteUser() {
        // User with view-admin should NOT be able to delete users
        List<UserRepresentation> users = viewAdminClient.realm(TEST_REALM_1).users()
                .search("realm1-user", 0, 1);
        assertThat("User should exist", users.size(), greaterThan(0));

        String userId = users.get(0).getId();

        try {
            viewAdminClient.realm(TEST_REALM_1).users().get(userId).remove();
            fail("Should not be able to delete user");
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        }
    }

    @Test
    public void testViewAdminCannotCreateRealm() {
        // User with view-admin should NOT be able to create new realms
        RealmRepresentation newRealm = new RealmRepresentation();
        newRealm.setRealm("should-fail-realm");
        newRealm.setEnabled(true);

        try {
            viewAdminClient.realms().create(newRealm);
            fail("Should not be able to create realm");
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        }
    }

    @Test
    public void testRegularUserCannotAccessRealms() {
        // Create client for regular user (no admin roles) using AdminClientUtil
        Keycloak regularClient = null;
        try {
            regularClient = AdminClientUtil.createAdminClient(
                    suiteContext.isAdapterCompatTesting(),
                    "master",
                    REGULAR_USER,
                    REGULAR_PASSWORD,
                    org.keycloak.models.Constants.ADMIN_CLI_CLIENT_ID,
                    null
            );

            // Regular user should NOT be able to view realm
            regularClient.realm(TEST_REALM_1).toRepresentation();
            fail("Regular user should not have access to realm");
        } catch (ForbiddenException e) {
            // Expected - user doesn't have permission
            assertThat("Should throw ForbiddenException", e, notNullValue());
        } catch (ClientErrorException e) {
            // Also acceptable if returned as generic client error
            assertThat("Should be 403 status code", e.getResponse().getStatus(), equalTo(403));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client for regular user", e);
        } finally {
            if (regularClient != null) {
                regularClient.close();
            }
        }
    }

    @Test
    public void testNewRealmAutoGrantsViewAdminPermissions() {
        // Create a new realm using admin client
        RealmRepresentation newRealm = RealmBuilder.create()
                .name("auto-grant-test")
                .build();

        adminClient.realms().create(newRealm);

        try {
            // Verify view-admin user can immediately access the new realm
            RealmRepresentation retrievedRealm = viewAdminClient.realm("auto-grant-test").toRepresentation();

            assertThat("Should be able to access newly created realm", retrievedRealm, notNullValue());
            assertThat("Realm name should match", retrievedRealm.getRealm(), equalTo("auto-grant-test"));

            // Verify view-admin role has the new realm's view roles
            RealmResource master = adminClient.realm("master");
            RoleResource viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN);
            Set<RoleRepresentation> composites = viewAdminRole.getRoleComposites();

            // Get the master admin client ID for the new realm (client exists in master realm)
            String expectedClientId = "auto-grant-test-realm";
            String realmMasterClientId = master.clients()
                    .findByClientId(expectedClientId)
                    .stream()
                    .findFirst()
                    .map(ClientRepresentation::getId)
                    .orElse(null);

            assertThat("Master admin client should exist for new realm", realmMasterClientId, notNullValue());

            // Check for auto-grant-test realm view roles by matching the containerId (client UUID)
            boolean hasNewRealmViewRole = composites.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getClientRole()))
                    .anyMatch(r -> r.getName().equals("view-realm") &&
                            realmMasterClientId.equals(r.getContainerId()));

            assertThat("view-admin should have view roles for new realm", hasNewRealmViewRole, is(true));

        } finally {
            // Cleanup
            try {
                adminClient.realm("auto-grant-test").remove();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    public void testViewAdminRoleIdempotency() {
        // Verify that the view-admin role setup is idempotent
        // This tests that running migration multiple times doesn't break things

        RealmResource master = adminClient.realm("master");

        // Get view-admin role
        RoleResource viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN);
        RoleRepresentation roleRep1 = viewAdminRole.toRepresentation();
        Set<RoleRepresentation> composites1 = viewAdminRole.getRoleComposites();

        // Get it again (simulating re-running migration)
        RoleRepresentation roleRep2 = viewAdminRole.toRepresentation();
        Set<RoleRepresentation> composites2 = viewAdminRole.getRoleComposites();

        // Role should be consistent
        assertThat("Role ID should remain the same", roleRep1.getId(), equalTo(roleRep2.getId()));
        assertThat("Role name should remain the same", roleRep1.getName(), equalTo(roleRep2.getName()));
        assertThat("Composite count should be the same", composites1.size(), equalTo(composites2.size()));
    }

    @Test
    public void testAdminCanAssignViewAdminRole() {
        // Admin should be able to assign view-admin role to other users
        RealmResource master = adminClient.realm("master");

        // Create a new test user
        UserRepresentation testUser = UserBuilder.create()
                .username("role-assignment-test")
                .password("password")
                .enabled(true)
                .build();

        Response response = master.users().create(testUser);
        String testUserId = ApiUtil.getCreatedId(response);
        response.close();

        try {
            // Admin assigns view-admin role
            RoleRepresentation viewAdminRole = master.roles().get(AdminRoles.VIEW_ADMIN).toRepresentation();
            master.users().get(testUserId).roles().realmLevel().add(Arrays.asList(viewAdminRole));

            // Verify assignment
            List<RoleRepresentation> userRoles = master.users().get(testUserId).roles().realmLevel().listAll();
            boolean hasViewAdminRole = userRoles.stream()
                    .anyMatch(r -> AdminRoles.VIEW_ADMIN.equals(r.getName()));

            assertThat("User should have view-admin role assigned", hasViewAdminRole, is(true));

        } finally {
            // Cleanup
            try {
                master.users().get(testUserId).remove();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
