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

package org.keycloak.tests.admin.authz.fgap;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class PermissionRESTTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    private ManagedUser userAlice;

    @InjectAdminClientFactory
    private AdminClientFactory adminClientFactory;

    @Test
    public void testPreventDeletingAdminPermissionsClient() {
        try {
            client.admin().remove();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }
    }

    @Test
    public void testManageNotAllowedForAdminPermissionsClient() {
        ClientRepresentation representation = client.admin().toRepresentation();
        assertFalse(representation.getAccess().get("manage"));
        assertFalse(representation.getAccess().get("configure"));
    }

    @Test
    public void resourceServerTest() {
        ResourceServerRepresentation rep = new ResourceServerRepresentation();
        rep.setPolicyEnforcementMode(PolicyEnforcementMode.DISABLED);
        rep.setDecisionStrategy(DecisionStrategy.CONSENSUS);

        try {
            client.admin().authorization().update(rep);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        try {
            client.admin().authorization().exportSettings();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        try {
            client.admin().authorization().importSettings(rep);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }
    }

    @Test
    public void scopesTest() {
        ScopeRepresentation manage = client.admin().authorization().scopes().findByName("manage");
        assertThat(manage, notNullValue());

        ScopeRepresentation customScope = new ScopeRepresentation();
        customScope.setName("custom");

        try (Response response = client.admin().authorization().scopes().create(customScope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        try {
            client.admin().authorization().scopes().scope(manage.getId()).update(manage);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        try {
            client.admin().authorization().scopes().scope(manage.getId()).remove();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }
    }

    @Test
    public void resourcesTest() {
        ResourceRepresentation resourceRep = new ResourceRepresentation("resource-1", "manage");
        resourceRep.setType(AdminPermissionsSchema.USERS.getType());
        //it is not expected to create resources directly
        try (Response response = client.admin().authorization().resources().create(resourceRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }
        
        ResourceRepresentation usersResource = client.admin().authorization().resources().searchByName(AdminPermissionsSchema.USERS.getType());
        assertThat(usersResource, notNullValue());

        // updates to 'all resource type' resources not expected
        try {
            client.admin().authorization().resources().resource(usersResource.getId()).update(resourceRep);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        // deletes to 'all resource type' resources not expected
        try {
            client.admin().authorization().resources().resource(usersResource.getId()).remove();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        // this should create a resource for userAlice
        createPermission(client, PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .resources(Set.of(userAlice.getUsername()))
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .build());

        // resourceName should equal to userAlice.getId() by design
        ResourceRepresentation userAliceResourceRep = client.admin().authorization().resources().searchByName(userAlice.getId());
        assertThat(userAliceResourceRep, notNullValue());
        String aliceResourceId = userAliceResourceRep.getId();

        // updates not expected 
        try {
            client.admin().authorization().resources().resource(aliceResourceId).update(userAliceResourceRep);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }

        // delete not expected 
        try {
            client.admin().authorization().resources().resource(aliceResourceId).remove();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(BadRequestException.class));
        }
    }

    @Test
    public void permissionsTest() {
        // no resourceType, valid scopes
        createPermission(client, PermissionBuilder.create()
                .scopes(AdminPermissionsSchema.USERS.getScopes())
                .build(), Response.Status.BAD_REQUEST);

        // valid resourceType, no scopes
        createPermission(client, PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .build(), Response.Status.BAD_REQUEST);

        // valid resourceType, non-existent scopes
        createPermission(client, PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(Set.of("edit", "write", "token-exchange"))
                .build(), Response.Status.BAD_REQUEST);
    }

    @Test
    public void testAdminPermissionsClientAuthorizationAccess() {
        // Create test user with manage-clients role
        UserRepresentation manageClientsUser = createUser("client-manager-test", "password");
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        RoleRepresentation manageClientsRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_CLIENTS).toRepresentation();
        realm.admin().users().get(manageClientsUser.getId()).roles().clientLevel(realmManagement.getId())
                .add(List.of(manageClientsRole));

        // Create test user with manage-realm role
        UserRepresentation realmAdmin = createUser("realm-admin-test", "password");
        RoleRepresentation manageRealmRole = realm.admin().clients().get(realmManagement.getId())
                .roles().get(AdminRoles.MANAGE_REALM).toRepresentation();
        realm.admin().users().get(realmAdmin.getId()).roles().clientLevel(realmManagement.getId())
                .add(List.of(manageRealmRole));

        // Create a regular test client with authorization enabled
        ClientRepresentation regularClient = ClientConfigBuilder.create()
                .clientId("regular-authz-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .authorizationServicesEnabled(true)
                .build();
        try (Response response = realm.admin().clients().create(regularClient)) {
            regularClient.setId(ApiUtil.getCreatedId(response));
        }

        // Test with user having manage-clients permission
        try (Keycloak clientManagerKeycloak = adminClientFactory.create()
                .realm(realm.getName())
                .username(manageClientsUser.getUsername())
                .password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .build()) {

            // User with manage-clients CANNOT create permissions on admin-permissions client (manage-authorization)
            ScopePermissionRepresentation blockedPermission = new ScopePermissionRepresentation();
            blockedPermission.setName("test-blocked-permission");
            blockedPermission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
            blockedPermission.setScopes(Set.of(AdminPermissionsSchema.VIEW));

            try (Response response = clientManagerKeycloak.realm(realm.getName())
                    .clients()
                    .get(client.getId())
                    .authorization()
                    .permissions()
                    .scope()
                    .create(blockedPermission)) {
                assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
            }

            // User with manage-clients CAN access regular client authorization settings
            ResourceServerRepresentation regularClientSettings = clientManagerKeycloak.realm(realm.getName())
                    .clients()
                    .get(regularClient.getId())
                    .authorization()
                    .getSettings();

            assertNotNull(regularClientSettings, "User with manage-clients permission SHOULD be able to access regular client authorization");
        }

        // User with manage-realm CAN create permissions on admin-permissions client
        try (Keycloak realmAdminKeycloak = adminClientFactory.create().realm(realm.getName())
                .username(realmAdmin.getUsername()).password("password")
                .clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {

            ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
            permission.setName("test-realm-admin-permission");
            permission.setResourceType(AdminPermissionsSchema.USERS_RESOURCE_TYPE);
            permission.setScopes(Set.of(AdminPermissionsSchema.VIEW));

            try (Response response = realmAdminKeycloak.realm(realm.getName())
                    .clients()
                    .get(client.getId())
                    .authorization()
                    .permissions()
                    .scope()
                    .create(permission)) {
                assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            }
        }
    }
}
