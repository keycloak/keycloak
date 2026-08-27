/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.RealmAdminAccessTest.ServerConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ServerConfig.class)
public class RealmAdminAccessTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @Test
    public void testRealmAdminAccess() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId("realm-management").get(0);
        RoleRepresentation realmAdminRole = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(realmAdminRole));

        assertThat(realmAdminClient.realm(realm.getName()).users().search("myadmin"), is(not(empty())));
        assertThat(realmAdminClient.realm(realm.getName()).clients().findAll(), is(not(empty())));
        RealmRepresentation realmRep = realmAdminClient.realm(realm.getName()).toRepresentation();

        realmRep.setAdminPermissionsEnabled(!realmRep.isAdminPermissionsEnabled());
        realmAdminClient.realm(realmRep.getRealm()).update(realmRep);
        realmRep.setAdminPermissionsEnabled(!realmRep.isAdminPermissionsEnabled());
        realmAdminClient.realm(realmRep.getRealm()).update(realmRep);

        try {
            assertThat(realmAdminClient.realm("master").clients().findAll(), is(not(empty())));
            fail("Should not have access to other realm");
        } catch (ForbiddenException ignore) {
        }

        RealmRepresentation myrealm = new RealmRepresentation();
        myrealm.setRealm("myrealm");
        myrealm.setEnabled(true);

        try {
            realmAdminClient.realms().create(myrealm);
            fail("Should not have access to create realms");
        } catch (ForbiddenException ignore) {
        }

        try (Keycloak client = adminClientFactory.create().realm("master")
                .username("admin").password("admin").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            try {
                Assertions.assertNotNull(client.serverInfo().getInfo());
                client.realms().create(myrealm);

                assertThat(realmAdminClient.realms().findAll(), hasSize(1));
                assertThat(realmAdminClient.realms().findAll().get(0).getRealm(), is(realm.getName()));

                try {
                    realmAdminClient.realm(myrealm.getRealm()).remove();
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }

                try {
                    assertThat(realmAdminClient.realm(myrealm.getRealm()).users().search(null), is(not(empty())));
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }

                try {
                    assertThat(realmAdminClient.realm(myrealm.getRealm()).clients().findAll(), is(not(empty())));
                    fail("Should not have access to other realm");
                } catch (ForbiddenException ignore) {
                }

                assertWorkflowAccess(client);
            } finally {
                client.realm(myrealm.getRealm()).remove();
            }
        }
    }

    @Test
    public void testRolePolicyRespectsClientScope() {
        String realmName = realm.getName();

        // Create a custom role and a user who has it
        RoleRepresentation customRole = new RoleRepresentation();
        customRole.setName("custom-manager");
        realm.admin().roles().create(customRole);
        customRole = realm.admin().roles().get("custom-manager").toRepresentation();

        UserRepresentation scopedAdmin = createUser("scoped-admin", "password");
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .realmLevel().add(List.of(customRole));

        // Grant query-users and view-users so the admin can reach and list users
        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation queryUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        // Create a role policy requiring custom-manager with fetchRoles=false so that
        // RolePolicyProvider delegates to identity.hasRealmRole() rather than user.hasRole()
        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, adminPermissionsClient,
                "Custom Manager Role Policy", customRole.getId(), Logic.POSITIVE);
        rolePolicy = adminPermissionsClient.authorization().policies().role()
                .findByName(rolePolicy.getName());
        rolePolicy.setFetchRoles(false);
        adminPermissionsClient.authorization().policies().role()
                .findById(rolePolicy.getId()).update(rolePolicy);
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.USERS.getType(),
                rolePolicy, Set.of(AdminPermissionsSchema.MANAGE));

        // Create a restricted client that includes custom-manager in scope
        ClientRepresentation fullScopeClient = ClientBuilder.create("full-scope-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .build();
        try (Response response = realm.admin().clients().create(fullScopeClient)) {
            fullScopeClient.setId(ApiUtil.getCreatedId(response));
        }

        // Verify the permission works with a full-scope client
        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("full-scope-client")
                .username("scoped-admin")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("updated");
            client.realm(realmName).users().get(target.getId()).update(target);
        }

        // Now create a restricted client that does NOT include custom-manager in scope
        ClientRepresentation restrictedClient = ClientBuilder.create("restricted-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .fullScopeEnabled(false)
                .build();
        try (Response response = realm.admin().clients().create(restrictedClient)) {
            restrictedClient.setId(ApiUtil.getCreatedId(response));
        }

        // Add query-users and view-users to scope — custom-manager is NOT in scope
        realm.admin().clients().get(restrictedClient.getId()).getScopeMappings()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        // The role policy should deny because custom-manager is not in the client's scope
        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("restricted-client")
                .username("scoped-admin")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("should-not-update");
            try {
                client.realm(realmName).users().get(target.getId()).update(target);
                fail("Updating a user should be denied when the role required by the policy is not in the client scope");
            } catch (ForbiddenException e) {
                // expected
            }
        }
    }

    @Test
    public void testFetchRolesEnabledBypassesScopeFiltering() {
        String realmName = realm.getName();

        RoleRepresentation customRole = new RoleRepresentation();
        customRole.setName("custom-manager");
        realm.admin().roles().create(customRole);
        customRole = realm.admin().roles().get("custom-manager").toRepresentation();

        UserRepresentation scopedAdmin = createUser("scoped-admin", "password");
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .realmLevel().add(List.of(customRole));

        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation queryUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, adminPermissionsClient,
                "Fetch Roles Policy", customRole.getId(), Logic.POSITIVE);
        rolePolicy = adminPermissionsClient.authorization().policies().role()
                .findByName(rolePolicy.getName());
        rolePolicy.setFetchRoles(true);
        adminPermissionsClient.authorization().policies().role()
                .findById(rolePolicy.getId()).update(rolePolicy);
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.USERS.getType(),
                rolePolicy, Set.of(AdminPermissionsSchema.MANAGE));

        ClientRepresentation restrictedClient = ClientBuilder.create("restricted-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .fullScopeEnabled(false)
                .build();
        try (Response response = realm.admin().clients().create(restrictedClient)) {
            restrictedClient.setId(ApiUtil.getCreatedId(response));
        }

        realm.admin().clients().get(restrictedClient.getId()).getScopeMappings()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        // fetchRoles=true resolves roles directly from the user model, bypassing scope filtering
        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("restricted-client")
                .username("scoped-admin")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("updated-via-fetch-roles");
            client.realm(realmName).users().get(target.getId()).update(target);
        }
    }

    @Test
    public void testClientRolePolicyRespectsClientScope() {
        String realmName = realm.getName();

        // Create a custom client with a client role
        ClientRepresentation customApp = ClientBuilder.create("custom-app")
                .publicClient()
                .directAccessGrantsEnabled()
                .build();
        try (Response response = realm.admin().clients().create(customApp)) {
            customApp.setId(ApiUtil.getCreatedId(response));
        }
        RoleRepresentation appManagerRole = new RoleRepresentation();
        appManagerRole.setName("app-manager");
        realm.admin().clients().get(customApp.getId()).roles().create(appManagerRole);
        appManagerRole = realm.admin().clients().get(customApp.getId())
                .roles().get("app-manager").toRepresentation();

        UserRepresentation scopedAdmin = createUser("scoped-admin", "password");
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .clientLevel(customApp.getId()).add(List.of(appManagerRole));

        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation queryUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(scopedAdmin.getId()).roles()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, adminPermissionsClient,
                "App Manager Policy", appManagerRole.getId(), Logic.POSITIVE);
        rolePolicy = adminPermissionsClient.authorization().policies().role()
                .findByName(rolePolicy.getName());
        rolePolicy.setFetchRoles(false);
        adminPermissionsClient.authorization().policies().role()
                .findById(rolePolicy.getId()).update(rolePolicy);
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.USERS.getType(),
                rolePolicy, Set.of(AdminPermissionsSchema.MANAGE));

        // Full-scope client — client role is in scope, operation should succeed
        ClientRepresentation fullScopeClient = ClientBuilder.create("full-scope-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .build();
        try (Response response = realm.admin().clients().create(fullScopeClient)) {
            fullScopeClient.setId(ApiUtil.getCreatedId(response));
        }

        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("full-scope-client")
                .username("scoped-admin")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("updated");
            client.realm(realmName).users().get(target.getId()).update(target);
        }

        // Restricted-scope client without the client role — operation should be denied
        ClientRepresentation restrictedClient = ClientBuilder.create("restricted-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .fullScopeEnabled(false)
                .build();
        try (Response response = realm.admin().clients().create(restrictedClient)) {
            restrictedClient.setId(ApiUtil.getCreatedId(response));
        }

        realm.admin().clients().get(restrictedClient.getId()).getScopeMappings()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("restricted-client")
                .username("scoped-admin")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("should-not-update");
            try {
                client.realm(realmName).users().get(target.getId()).update(target);
                fail("Updating a user should be denied when the client role required by the policy is not in the client scope");
            } catch (ForbiddenException e) {
                // expected
            }
        }
    }

    private void assertWorkflowAccess(Keycloak serverAdminClient) {
        // server admin can access workflows
        serverAdminClient.realm(realm.getName()).workflows().list();

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId("realm-management").get(0);
        RoleRepresentation realmAdminRole = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.REALM_ADMIN).toRepresentation();

        // can access workflows with realm-admin role
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).add(List.of(realmAdminRole));
        realmAdminClient.realm(realm.getName()).workflows().list();

        // cannot access workflows without realm-admin role
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagement.getId()).remove(List.of(realmAdminRole));

        try {
            realmAdminClient.realm(realm.getName()).workflows().list();
            fail("Should not have access to workflows");
        } catch (ForbiddenException ignore) {
        }

        UserRepresentation masterUserRealmAdmin = UserBuilder.create()
                .username("mymasteradmin")
                .password("password")
                .firstName("f")
                .lastName("l")
                .email("mymasteradmin@keycloak.org")
                .build();
        try (Response response = serverAdminClient.realm("master").users().create(masterUserRealmAdmin)) {
            masterUserRealmAdmin.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation myRealmMasterClient = serverAdminClient.realm("master").clients().findByClientId(realm.getName() + "-realm").get(0);
        RoleRepresentation masterRealmAdminRole = serverAdminClient.realm("master").clients().get(myRealmMasterClient.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        serverAdminClient.realm("master").users().get(masterUserRealmAdmin.getId())
                .roles().clientLevel(myRealmMasterClient.getId()).add(List.of(masterRealmAdminRole));
        try (Keycloak masterRealmAdminClient = adminClientFactory.create().realm("master")
                .username("mymasteradmin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {

            // can not access workflows with manage-realm role in master realm
            try {
                masterRealmAdminClient.realm(realm.getName()).workflows().list();
                fail("Should not have access to manage workflows if user is master realm admin with manage-realm role in a realm");
            } catch (ForbiddenException ignore) {}
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Feature.WORKFLOWS);
        }
    }
}
