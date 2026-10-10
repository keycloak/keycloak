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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.RealmAdminAccessTest.ServerConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ServerConfig.class)
public class RealmAdminAccessTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

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

    @Test
    public void testHardcodedRoleMapperDoesNotBypassRealmRolePolicy() {
        String realmName = realm.getName();

        RoleRepresentation customRole = new RoleRepresentation();
        customRole.setName("custom-manager");
        realm.admin().roles().create(customRole);
        customRole = realm.admin().roles().get("custom-manager").toRepresentation();

        // User does NOT have custom-manager — only query-users and view-users
        UserRepresentation attacker = createUser("attacker", "password");
        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation queryUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(attacker.getId()).roles()
                .clientLevel(realmMgmt.getId()).add(List.of(queryUsersRole, viewUsersRole));

        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, adminPermissionsClient,
                "Custom Manager Role Policy", customRole.getId(), Logic.POSITIVE);
        rolePolicy = adminPermissionsClient.authorization().policies().role()
                .findByName(rolePolicy.getName());
        rolePolicy.setFetchRoles(false);
        adminPermissionsClient.authorization().policies().role()
                .findById(rolePolicy.getId()).update(rolePolicy);
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.USERS.getType(),
                rolePolicy, Set.of(AdminPermissionsSchema.MANAGE));

        // Client with a HardcodedRole mapper injecting custom-manager
        ClientRepresentation maliciousClient = ClientBuilder.create("malicious-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .build();
        try (Response response = realm.admin().clients().create(maliciousClient)) {
            maliciousClient.setId(ApiUtil.getCreatedId(response));
        }
        ProtocolMapperRepresentation mapper = toRepresentation(
                HardcodedRole.create("inject-custom-manager", "custom-manager"));
        realm.admin().clients().get(maliciousClient.getId())
                .getProtocolMappers().createMapper(mapper).close();

        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("malicious-client")
                .username("attacker")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("should-not-update");
            try {
                client.realm(realmName).users().get(target.getId()).update(target);
                fail("A HardcodedRole mapper injecting a custom role should not bypass FGAP role policy");
            } catch (ForbiddenException e) {
                // expected
            }
        }
    }

    @Test
    public void testHardcodedRoleMapperDoesNotBypassClientRolePolicy() {
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

        // User does NOT have app-manager — only query-users and view-users
        UserRepresentation attacker = createUser("attacker", "password");
        ClientRepresentation realmMgmt = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation queryUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.QUERY_USERS).toRepresentation();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmMgmt.getId())
                .roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(attacker.getId()).roles()
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

        // Client with a HardcodedRole mapper injecting custom-app.app-manager
        ClientRepresentation maliciousClient = ClientBuilder.create("malicious-client")
                .publicClient()
                .directAccessGrantsEnabled()
                .build();
        try (Response response = realm.admin().clients().create(maliciousClient)) {
            maliciousClient.setId(ApiUtil.getCreatedId(response));
        }
        ProtocolMapperRepresentation mapper = toRepresentation(
                HardcodedRole.create("inject-app-manager", "custom-app.app-manager"));
        realm.admin().clients().get(maliciousClient.getId())
                .getProtocolMappers().createMapper(mapper).close();

        try (Keycloak client = adminClientFactory.create()
                .realm(realmName)
                .clientId("malicious-client")
                .username("attacker")
                .password("password")
                .build()) {
            UserRepresentation target = client.realm(realmName).users().search("myadmin").get(0);
            target.setLastName("should-not-update");
            try {
                client.realm(realmName).users().get(target.getId()).update(target);
                fail("A HardcodedRole mapper injecting a client role should not bypass FGAP role policy");
            } catch (ForbiddenException e) {
                // expected
            }
        }
    }

    @Test
    public void testClientScopeMappingsFilterHiddenRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation visibleClient = new ClientRepresentation();
        visibleClient.setClientId("visible-client");
        visibleClient.setFullScopeAllowed(false);
        try (Response response = realm.admin().clients().create(visibleClient)) {
            visibleClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation secretClient = new ClientRepresentation();
        secretClient.setClientId("secret-client");
        try (Response response = realm.admin().clients().create(secretClient)) {
            secretClient.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation visibleChild = new RoleRepresentation();
        visibleChild.setName("VISIBLE_CHILD");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleChild);
        visibleChild = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_CHILD").toRepresentation();

        RoleRepresentation secretChild = new RoleRepresentation();
        secretChild.setName("SECRET_CHILD");
        realm.admin().clients().get(secretClient.getId()).roles().create(secretChild);
        secretChild = realm.admin().clients().get(secretClient.getId()).roles().get("SECRET_CHILD").toRepresentation();

        RoleRepresentation visibleParent = new RoleRepresentation();
        visibleParent.setName("VISIBLE_PARENT");
        realm.admin().clients().get(visibleClient.getId()).roles().create(visibleParent);
        visibleParent = realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").toRepresentation();
        realm.admin().clients().get(visibleClient.getId()).roles().get("VISIBLE_PARENT").addComposites(List.of(visibleChild, secretChild));

        RoleRepresentation secretRealmChild = new RoleRepresentation();
        secretRealmChild.setName("SECRET_REALM_CHILD");
        realm.admin().roles().create(secretRealmChild);
        secretRealmChild = realm.admin().roles().get("SECRET_REALM_CHILD").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("SECRET_REALM_CHILD").remove());

        RoleRepresentation realmParent = new RoleRepresentation();
        realmParent.setName("REALM_PARENT");
        realm.admin().roles().create(realmParent);
        realmParent = realm.admin().roles().get("REALM_PARENT").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("REALM_PARENT").remove());
        realm.admin().roles().get("REALM_PARENT").addComposites(List.of(secretRealmChild));

        RoleMappingResource scopeMappings = realm.admin().clients().get(visibleClient.getId()).getScopeMappings();
        scopeMappings.realmLevel().add(List.of(realmParent));
        scopeMappings.clientLevel(visibleClient.getId()).add(List.of(visibleParent));
        scopeMappings.clientLevel(secretClient.getId()).add(List.of(secretChild));

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, visibleClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(AdminPermissionsSchema.VIEW), policy);

        String secretRealmChildId = secretRealmChild.getId();
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).rolesById().getRole(secretRealmChildId));
        String secretChildId = secretChild.getId();
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName()).rolesById().getRole(secretChildId));

        scopeMappings = realmAdminClient.realm(realm.getName()).clients().get(visibleClient.getId()).getScopeMappings();
        Set<String> roleNames;

        // GET /clients/{clientUuid}/scope-mappings/realm/composite
        roleNames = toNames(scopeMappings.realmLevel().listEffective());
        assertThat(roleNames, not(hasItem("REALM_PARENT")));
        assertThat(roleNames, not(hasItem("SECRET_REALM_CHILD")));
        roleNames = toNames(scopeMappings.realmLevel().listEffective(false));
        assertThat(roleNames, not(hasItem("REALM_PARENT")));
        assertThat(roleNames, not(hasItem("SECRET_REALM_CHILD")));

        // GET /clients/{clientUuid}/scope-mappings/realm
        assertThat(toNames(scopeMappings.realmLevel().listAll()), empty());

        // GET /clients/{clientUuid}/scope-mappings/clients/{clientUuid}
        assertThat(toNames(scopeMappings.clientLevel(visibleClient.getId()).listAll()), hasItem("VISIBLE_PARENT"));

        // GET /clients/{clientUuid}/scope-mappings/clients/{clientUuid}/composite
        roleNames = toNames(scopeMappings.clientLevel(visibleClient.getId()).listEffective());
        assertThat(roleNames, hasItem("VISIBLE_PARENT"));
        assertThat(roleNames, hasItem("VISIBLE_CHILD"));

        // GET /clients/{clientUuid}/scope-mappings/clients/{hiddenClientUuid}
        assertThat(toNames(scopeMappings.clientLevel(secretClient.getId()).listAll()), empty());

        // GET /clients/{clientUuid}/scope-mappings/clients/{hiddenClientUuid}/composite
        assertThat(toNames(scopeMappings.clientLevel(secretClient.getId()).listEffective()), empty());

        // GET /clients/{clientUuid}/scope-mappings
        MappingsRepresentation mappings = scopeMappings.getAll();
        assertThat(toNames(mappings.getRealmMappings()), empty());
        assertThat(mappings.getClientMappings(), hasKey("visible-client"));
        assertThat(toNames(mappings.getClientMappings().get("visible-client").getMappings()), hasItem("VISIBLE_PARENT"));
        assertThat(mappings.getClientMappings(), not(hasKey("secret-client")));
    }

    @Test
    public void testClientScopeEvaluationFiltersHiddenRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        ClientRepresentation evaluatedClient = new ClientRepresentation();
        evaluatedClient.setClientId("evaluated-client");
        evaluatedClient.setFullScopeAllowed(false);
        try (Response response = realm.admin().clients().create(evaluatedClient)) {
            evaluatedClient.setId(ApiUtil.getCreatedId(response));
        }

        ClientRepresentation hiddenClient = new ClientRepresentation();
        hiddenClient.setClientId("hidden-client");
        try (Response response = realm.admin().clients().create(hiddenClient)) {
            hiddenClient.setId(ApiUtil.getCreatedId(response));
        }

        RoleRepresentation hiddenClientGranted = createClientRole(hiddenClient.getId(), "HIDDEN_CLIENT_GRANTED");
        RoleRepresentation visibleClientGranted = createClientRole(hiddenClient.getId(), "VISIBLE_CLIENT_GRANTED");
        RoleRepresentation hiddenClientNotGranted = createClientRole(hiddenClient.getId(), "HIDDEN_CLIENT_NOT_GRANTED");
        RoleRepresentation visibleClientNotGranted = createClientRole(hiddenClient.getId(), "VISIBLE_CLIENT_NOT_GRANTED");
        RoleRepresentation hiddenRealmGranted = createRealmRole("HIDDEN_REALM_GRANTED");
        RoleRepresentation visibleRealmGranted = createRealmRole("VISIBLE_REALM_GRANTED");
        RoleRepresentation hiddenRealmNotGranted = createRealmRole("HIDDEN_REALM_NOT_GRANTED");
        RoleRepresentation visibleRealmNotGranted = createRealmRole("VISIBLE_REALM_NOT_GRANTED");

        RoleMappingResource scopeMappings = realm.admin().clients().get(evaluatedClient.getId()).getScopeMappings();
        scopeMappings.clientLevel(hiddenClient.getId()).add(List.of(hiddenClientGranted, visibleClientGranted));
        scopeMappings.realmLevel().add(List.of(hiddenRealmGranted, visibleRealmGranted));

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, evaluatedClient.getId(), AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE, Set.of(AdminPermissionsSchema.VIEW), policy);
        createPermission(adminPermissionsClient, Set.of(
                        visibleClientGranted.getId(), visibleClientNotGranted.getId(),
                        visibleRealmGranted.getId(), visibleRealmNotGranted.getId()),
                AdminPermissionsSchema.ROLES.getType(), Set.of(AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE), policy);

        assertThrows(ForbiddenException.class,
                () -> realmAdminClient.realm(realm.getName()).rolesById().getRole(hiddenClientGranted.getId()));
        assertThrows(ForbiddenException.class,
                () -> realmAdminClient.realm(realm.getName()).rolesById().getRole(visibleClientGranted.getId()));

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            List<RoleRepresentation> grantedClientRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), hiddenClient.getId(), "granted");
            assertThat(toNames(grantedClientRoles), equalTo(Set.of("VISIBLE_CLIENT_GRANTED")));
            assertThat(byName(grantedClientRoles, "VISIBLE_CLIENT_GRANTED").getAttributes(), nullValue());

            List<RoleRepresentation> notGrantedClientRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), hiddenClient.getId(), "not-granted");
            assertThat(toNames(notGrantedClientRoles), equalTo(Set.of("VISIBLE_CLIENT_NOT_GRANTED")));

            List<RoleRepresentation> grantedRealmRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), realm.getName(), "granted");
            assertThat(toNames(grantedRealmRoles), equalTo(Set.of("VISIBLE_REALM_GRANTED")));

            List<RoleRepresentation> notGrantedRealmRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), realm.getName(), "not-granted");
            assertThat(toNames(notGrantedRealmRoles), equalTo(Set.of("VISIBLE_REALM_NOT_GRANTED")));
            assertThat(byName(notGrantedRealmRoles, "VISIBLE_REALM_NOT_GRANTED").getAttributes(), nullValue());

            ClientRepresentation update = realm.admin().clients().get(evaluatedClient.getId()).toRepresentation();
            update.setFullScopeAllowed(true);
            realm.admin().clients().get(evaluatedClient.getId()).update(update);

            grantedClientRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), hiddenClient.getId(), "granted");
            assertThat(toNames(grantedClientRoles), equalTo(Set.of("VISIBLE_CLIENT_GRANTED", "VISIBLE_CLIENT_NOT_GRANTED")));
            notGrantedClientRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), hiddenClient.getId(), "not-granted");
            assertThat(toNames(notGrantedClientRoles), empty());

            grantedRealmRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), realm.getName(), "granted");
            assertThat(toNames(grantedRealmRoles), equalTo(Set.of("VISIBLE_REALM_GRANTED", "VISIBLE_REALM_NOT_GRANTED")));
            notGrantedRealmRoles = evaluateScopeMappings(httpClient, evaluatedClient.getId(), realm.getName(), "not-granted");
            assertThat(toNames(notGrantedRealmRoles), empty());
        }
    }

    @Test
    public void testClientScopeScopeMappingsFilterHiddenRoles() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        RoleRepresentation secretRealmChild = new RoleRepresentation();
        secretRealmChild.setName("SECRET_REALM_CHILD");
        realm.admin().roles().create(secretRealmChild);
        secretRealmChild = realm.admin().roles().get("SECRET_REALM_CHILD").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("SECRET_REALM_CHILD").remove());

        RoleRepresentation mappableParent = new RoleRepresentation();
        mappableParent.setName("MAPPABLE_PARENT");
        mappableParent.setAttributes(Map.of("visibility", List.of("mappable")));
        realm.admin().roles().create(mappableParent);
        mappableParent = realm.admin().roles().get("MAPPABLE_PARENT").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("MAPPABLE_PARENT").remove());
        realm.admin().roles().get("MAPPABLE_PARENT").addComposites(List.of(secretRealmChild));

        RoleRepresentation secretRealmRole = new RoleRepresentation();
        secretRealmRole.setName("SECRET_REALM_ROLE");
        realm.admin().roles().create(secretRealmRole);
        secretRealmRole = realm.admin().roles().get("SECRET_REALM_ROLE").toRepresentation();
        realm.cleanup().add(r -> r.roles().get("SECRET_REALM_ROLE").remove());

        ClientRepresentation myclient = realm.admin().clients().findByClientId("myclient").get(0);
        RoleRepresentation clientRole = new RoleRepresentation();
        clientRole.setName("CLIENT_ROLE");
        clientRole.setAttributes(Map.of("visibility", List.of("viewable")));
        realm.admin().clients().get(myclient.getId()).roles().create(clientRole);
        clientRole = realm.admin().clients().get(myclient.getId()).roles().get("CLIENT_ROLE").toRepresentation();

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("test-client-scope");
        clientScope.setProtocol("openid-connect");
        try (Response response = realm.admin().clientScopes().create(clientScope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            clientScope.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.clientScopes().get(clientScope.getId()).remove());
        }

        RoleMappingResource scopeMappings = realm.admin().clientScopes().get(clientScope.getId()).getScopeMappings();
        scopeMappings.realmLevel().add(List.of(mappableParent, secretRealmRole));
        scopeMappings.clientLevel(myclient.getId()).add(List.of(clientRole));

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        // viewing client scopes requires view on all clients, so only the realm roles are hidden from the admin
        createAllPermission(adminPermissionsClient, AdminPermissionsSchema.CLIENTS.getType(), policy, Set.of(AdminPermissionsSchema.VIEW));
        createPermission(adminPermissionsClient, mappableParent.getId(), AdminPermissionsSchema.ROLES.getType(), Set.of(AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE), policy);

        scopeMappings = realmAdminClient.realm(realm.getName()).clientScopes().get(clientScope.getId()).getScopeMappings();
        Set<String> roleNames;

        // GET /client-scopes/{scopeId}/scope-mappings/realm
        roleNames = toNames(scopeMappings.realmLevel().listAll());
        assertThat(roleNames, hasItem("MAPPABLE_PARENT"));
        assertThat(roleNames, not(hasItem("SECRET_REALM_ROLE")));

        // GET /client-scopes/{scopeId}/scope-mappings/realm/composite
        roleNames = toNames(scopeMappings.realmLevel().listEffective());
        assertThat(roleNames, hasItem("MAPPABLE_PARENT"));
        assertThat(roleNames, not(hasItem("SECRET_REALM_ROLE")));
        assertThat(roleNames, not(hasItem("SECRET_REALM_CHILD")));

        // GET /client-scopes/{scopeId}/scope-mappings/realm/composite?briefRepresentation=false
        List<RoleRepresentation> effectiveRoles = scopeMappings.realmLevel().listEffective(false);
        assertThat(toNames(effectiveRoles), hasItem("MAPPABLE_PARENT"));
        assertThat(toNames(effectiveRoles), not(hasItem("SECRET_REALM_ROLE")));
        // attributes are returned only for roles the admin can view
        assertThat(byName(effectiveRoles, "MAPPABLE_PARENT").getAttributes(), nullValue());

        // GET /client-scopes/{scopeId}/scope-mappings/clients/{clientUuid}
        assertThat(toNames(scopeMappings.clientLevel(myclient.getId()).listAll()), hasItem("CLIENT_ROLE"));

        // GET /client-scopes/{scopeId}/scope-mappings/clients/{clientUuid}/composite
        assertThat(toNames(scopeMappings.clientLevel(myclient.getId()).listEffective()), hasItem("CLIENT_ROLE"));

        // GET /client-scopes/{scopeId}/scope-mappings/clients/{clientUuid}/composite?briefRepresentation=false
        effectiveRoles = scopeMappings.clientLevel(myclient.getId()).listEffective(false);
        assertThat(byName(effectiveRoles, "CLIENT_ROLE").getAttributes(), hasEntry("visibility", List.of("viewable")));

        // GET /client-scopes/{scopeId}/scope-mappings
        MappingsRepresentation mappings = scopeMappings.getAll();
        roleNames = toNames(mappings.getRealmMappings());
        assertThat(roleNames, hasItem("MAPPABLE_PARENT"));
        assertThat(roleNames, not(hasItem("SECRET_REALM_ROLE")));
        assertThat(mappings.getClientMappings(), hasKey("myclient"));
    }

    private static Set<String> toNames(List<RoleRepresentation> roles) {
        return roles == null ? Set.of() : roles.stream().map(RoleRepresentation::getName).collect(Collectors.toSet());
    }

    private static RoleRepresentation byName(List<RoleRepresentation> roles, String name) {
        return roles.stream().filter(role -> name.equals(role.getName())).findFirst().orElseThrow();
    }

    private RoleRepresentation createClientRole(String clientId, String roleName) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setAttributes(Map.of("classification", List.of("sensitive")));
        realm.admin().clients().get(clientId).roles().create(role);
        return realm.admin().clients().get(clientId).roles().get(roleName).toRepresentation();
    }

    private RoleRepresentation createRealmRole(String roleName) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleName);
        role.setAttributes(Map.of("classification", List.of("sensitive")));
        realm.admin().roles().create(role);
        return realm.admin().roles().get(roleName).toRepresentation();
    }

    private List<RoleRepresentation> evaluateScopeMappings(Client httpClient, String evaluatedClientId, String roleContainerId, String result) {
        WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                .path("admin").path("realms").path(realm.getName())
                .path("clients").path(evaluatedClientId)
                .path("evaluate-scopes").path("scope-mappings").path(roleContainerId).path(result)
                .register(new BearerAuthFilter(realmAdminClient.tokenManager()));

        try (Response response = target.request(MediaType.APPLICATION_JSON).get()) {
            assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
            return response.readEntity(new GenericType<List<RoleRepresentation>>() {});
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
