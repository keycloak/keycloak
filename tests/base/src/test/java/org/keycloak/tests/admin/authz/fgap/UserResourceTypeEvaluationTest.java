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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.IMPERSONATE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MAP_ROLES;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.RESET_PASSWORD;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private final String usersType = AdminPermissionsSchema.USERS.getType();

    private final String newUserUsername = "new_user";

    @Test
    public void testSingleUserPermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        // allow my admin to see alice only
        createPermission(client, userAlice.admin().toRepresentation().getId(), usersType, Set.of(VIEW), allowMyAdminPermission);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testImpersonatePermission() {
        // myadmin shouldn't be able to impersonate user just yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        //create all-users permission for "myadmin" (so that myadmin can impersonate all users in the realm)
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, usersType, policy, Set.of(IMPERSONATE));

        // create user permission forbidding the impersonation for userAlice
        UserPolicyRepresentation negativePolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        String cannotImpersonateAlice = createPermission(client, userAlice.getId(), usersType, Set.of(IMPERSONATE), negativePolicy).getName();

        // even though "myadmin" has permission to impersonate all users in realm it should be denied to impersonate userAlice
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // remove the negative permission
        String cannotImpersonateAliceId = getScopePermissionsResource(client).findByName(cannotImpersonateAlice).getId();
        getScopePermissionsResource(client).findById(cannotImpersonateAliceId).remove();

        // need to create a separate client for the impersonation call, otherwise next usage of the 'realmAdminClient' would throw 401
        try (Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080")
                .realm(realm.getName())
                .clientId("myclient")
                .clientSecret("mysecret")
                .username("myadmin")
                .password("password")
                .build()) {
            // now it should be possible to impersonate the user due to fallback to all-users permission
            adminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
        }
    }

    @Test
    public void testManageAllPermission() {
        // myadmin shouldn't be able to create user just yet
        try (Response response = realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build())) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        //create all-users permission for "myadmin" (so that myadmin can manage all users in the realm)
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, usersType, policy, Set.of(VIEW, MANAGE));

        // creating user requires manage scope
        String newUserId = ApiUtil.getCreatedId(realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build()));

        // it should be possible to update the user due to fallback to all-users permission
        realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("new@test.com").build());
        assertEquals("new@test.com", realmAdminClient.realm(realm.getName()).users().get(newUserId).toRepresentation().getEmail());
    }

    @Test
    public void testManageUserPermission() {
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", myadminId);
        ScopePermissionRepresentation allUsersPermission = createAllPermission(client, usersType, policy, Set.of(VIEW, MANAGE));

        // creating user requires manage scope
        String newUserId = ApiUtil.getCreatedId(realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build()));

        // remove all-users permissions to test user-permission
        allUsersPermission = getScopePermissionsResource(client).findByName(allUsersPermission.getName());
        getScopePermissionsResource(client).findById(allUsersPermission.getId()).remove();

        // create user-permissions
        createPermission(client, UserConfigBuilder.create().id(newUserId).build().getId(), usersType, Set.of(VIEW, MANAGE), policy);

        // it should be possible to update the user due to single user-permission
        realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("email@test.com").build());
        assertEquals("email@test.com", realmAdminClient.realm(realm.getName()).users().get(newUserId).toRepresentation().getEmail());

        // remove the user permission
        getScopePermissionsResource(client).findAll(null, null, null, null, null).forEach(permission ->
            getScopePermissionsResource(client).findById(permission.getId()).remove()
        );

        // updating the user should be denied
        try {
            realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("email@test.com").build());
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testViewAllPermission() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        ScopePermissionRepresentation permission = createAllPermission(client, usersType, policy, Set.of(VIEW));
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertFalse(search.isEmpty());

        permission = client.admin().authorization().permissions().scope().findByName(permission.getName());
        permission.setPolicies(Set.of());
        client.admin().authorization().permissions().scope().findById(permission.getId()).update(permission);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertTrue(search.isEmpty());
    }

    @Test
    public void testViewUserPermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client,"Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, usersType, allowMyAdminPermission, Set.of(VIEW));

        UserPolicyRepresentation denyMyAdminAccessingHisAccountPermission = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", myadmin.getId());
        createPermission(client, myadmin.getId(), usersType, Set.of(VIEW), denyMyAdminAccessingHisAccountPermission);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testViewUserPermissionUserMemberOfGroup() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client,"Only My Admin User Policy", myadmin.getId());
        createPermission(client, userAlice.getId(), usersType, Set.of(VIEW), allowMyAdminPermission);

        GroupRepresentation group = new GroupRepresentation();
        group.setName("foo");
        try (Response response = realm.admin().groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
            // See org.keycloak.services.resources.admin.permissions.UserPermissionsV2.canView()
            realm.admin().users().get(myadmin.getId()).joinGroup(group.getId());
            List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
            assertEquals(1, search.size());
            assertEquals(userAlice.getUsername(), search.get(0).getUsername());
        } finally {
            realm.admin().users().get(myadmin.getId()).leaveGroup(group.getId());
        }
    }

    @Test
    public void testViewUserPermissionDenyByDefault() {
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        UserPolicyRepresentation disallowMyAdmin = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", myadminId);
        createAllPermission(client, usersType, disallowMyAdmin, Set.of(VIEW));

        UserPolicyRepresentation allowAliceOnlyForMyAdmin = createUserPolicy(realm, client,"My Admin User Policy", myadminId);
        createPermission(client, userAlice.getId(), usersType, Set.of(VIEW), allowAliceOnlyForMyAdmin);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testMapRoles() {
        RoleRepresentation testRole = new RoleRepresentation();
        testRole.setName("testRole");
        realm.admin().roles().create(testRole);
        realm.cleanup().add(r -> r.roles().get("testRole").remove());

        // myadmin shouldn't be able to map roles to the user just yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).roles().realmLevel().add(List.of(testRole));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        //create all-users permission for "myadmin" (so that myadmin can map roles to users in the realm)
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        ScopePermissionRepresentation allUsersPermission = createAllPermission(client, usersType, policy, Set.of(MAP_ROLES));

        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).roles().realmLevel().add(List.of(testRole));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            // succsessfully checked the requireMapRoles https://github.com/keycloak/keycloak/blob/792b673f49d5faeed8b3bb2c61fb4a3b404df695/services/src/main/java/org/keycloak/services/resources/admin/RoleMapperResource.java#L235
            // expecting here NotFoundException: https://github.com/keycloak/keycloak/blob/792b673f49d5faeed8b3bb2c61fb4a3b404df695/services/src/main/java/org/keycloak/services/resources/admin/RoleMapperResource.java#L243
            assertThat(ex, instanceOf(NotFoundException.class));
            // successful role mapping would require permission to map individual role https://github.com/keycloak/keycloak/blob/792b673f49d5faeed8b3bb2c61fb4a3b404df695/services/src/main/java/org/keycloak/services/resources/admin/RoleMapperResource.java#L245
            // and it's not implemented yet for V2
        }

        // remove all-users permissions to test user-permission
        allUsersPermission = getScopePermissionsResource(client).findByName(allUsersPermission.getName());
        getScopePermissionsResource(client).findById(allUsersPermission.getId()).remove();

        // now myadmin cannot map roles
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).roles().realmLevel().add(List.of(testRole));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // create userPermission
        createPermission(client, userAlice.getId(), usersType, Set.of(MAP_ROLES), policy);

        //check myadmin can map roles
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).roles().realmLevel().add(List.of(testRole));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            // expecting here NotFoundException: https://github.com/keycloak/keycloak/blob/792b673f49d5faeed8b3bb2c61fb4a3b404df695/services/src/main/java/org/keycloak/services/resources/admin/RoleMapperResource.java#L243
            assertThat(ex, instanceOf(NotFoundException.class));
        }
    }

    @Test
    public void testManageGroupMembership() {
        // myadmin shouldn't be able to manage group membership of the user just yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).joinGroup("no-such");
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        //create all-users permission for "myadmin" (so that myadmin can add users into a group)
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        ScopePermissionRepresentation allUsersPermission = createAllPermission(client, usersType, policy, Set.of(MANAGE_GROUP_MEMBERSHIP));

        //check myadmin can manage membership using all-users permission
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).joinGroup("no-such");
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            // expecting here NotFoundException: https://github.com/keycloak/keycloak/blob/b5c95e9f1c58bc500316dd5c0f2d3bb5e197ca99/services/src/main/java/org/keycloak/services/resources/admin/UserResource.java#L1060
            assertThat(ex, instanceOf(NotFoundException.class));
        }

        // remove all-users permissions to test user-permission
        allUsersPermission = getScopePermissionsResource(client).findByName(allUsersPermission.getName());
        getScopePermissionsResource(client).findById(allUsersPermission.getId()).remove();

        // now myadmin cannot manage membership
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).joinGroup("no-such");
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // create userPermission
        createPermission(client, userAlice.getId(), usersType, Set.of(MANAGE_GROUP_MEMBERSHIP), policy);

        //check myadmin can manage membership using individual permission
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).joinGroup("no-such");
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            // expecting here NotFoundException: https://github.com/keycloak/keycloak/blob/b5c95e9f1c58bc500316dd5c0f2d3bb5e197ca99/services/src/main/java/org/keycloak/services/resources/admin/UserResource.java#L1060
            assertThat(ex, instanceOf(NotFoundException.class));
        }
    }

    @Test
    public void testNoTransitiveUserPermissions() {
        //create all-users manage permission for "myadmin"
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, usersType, policy, Set.of(MANAGE));

        // with manage permission it is NOT possible to view
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertTrue(search.isEmpty());

        // with manage permission it is NOT possible to map roles
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).roles().realmLevel().add(List.of(new RoleRepresentation()));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // with manage permission it is NOT possible to manage group membership
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).joinGroup("no-such");
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // with manage permission it is NOT possible to impersonate
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testEvaluateAllResourcePermissionsForSpecificResourcePermission() {
        UserRepresentation adminUser = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, client, "Only My Admin", adminUser.getId());
        ScopePermissionRepresentation allResourcesPermission = createAllPermission(client, usersType, allowPolicy, Set.of(MANAGE, IMPERSONATE));
        // all resource permissions grants manage scope
        UsersResource users = realmAdminClient.realm(realm.getName()).users();
        users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());

        ScopePermissionRepresentation resourcePermission = createPermission(client, userAlice.getId(), usersType, Set.of(MANAGE), allowPolicy);
        // both all and specific resource permission grants manage scope
        users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());

        allResourcesPermission = getScopePermissionsResource(client).findByName(allResourcesPermission.getName());
        allResourcesPermission.setScopes(Set.of(IMPERSONATE));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission does not have the manage scope but the scope is granted by the resource permission
        users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(IMPERSONATE));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // neither the all and specific resource permission grants access to the manage scope
            users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        allResourcesPermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission grants access again to manage
        users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());

        UserPolicyRepresentation notAllowPolicy = createUserPolicy(Logic.NEGATIVE, realm, client, "Not My Admin", adminUser.getId());
        createPermission(client, userAlice.getId(), usersType, Set.of(MANAGE), notAllowPolicy);
        try {
            // a specific resource permission that explicitly negates access to the manage scope denies access to the scope
            users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(IMPERSONATE, MANAGE));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // the specific resource permission that explicitly negates access to the manage scope denies access to the scope
            // even though there is another resource permission that grants access to the scope - conflict resolution denies by default
            users.get(userAlice.getId()).update(userAlice.admin().toRepresentation());
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}
    }

    @Test
    public void testResetPassword() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        UserPolicyRepresentation notAllowMyAdminPermission = createUserPolicy(Logic.NEGATIVE, realm, client, "Not Allow My Admin User Policy", myadmin.getId());

        // allow my admin to see alice only
        ScopePermissionRepresentation managePermission = createPermission(client, userAlice.admin().toRepresentation().getId(), usersType, Set.of(VIEW, MANAGE), allowMyAdminPermission);
        ScopePermissionRepresentation resetPasswordPermission = createPermission(client, userAlice.admin().toRepresentation().getId(), usersType, Set.of(RESET_PASSWORD), notAllowMyAdminPermission);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        try {
            UsersResource users = realmAdminClient.realm(realm.getName()).users();
            users.get(search.get(0).getId()).resetPassword(credential);
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {
        }

        String permissionId = getScopePermissionsResource(client).findByName(resetPasswordPermission.getName()).getId();
        getScopePermissionsResource(client).findById(permissionId).remove();
        createPermission(client, userAlice.admin().toRepresentation().getId(), usersType, Set.of(RESET_PASSWORD), allowMyAdminPermission);

        UsersResource users = realmAdminClient.realm(realm.getName()).users();
        users.get(search.get(0).getId()).resetPassword(credential);

        permissionId = getScopePermissionsResource(client).findByName(managePermission.getName()).getId();
        getScopePermissionsResource(client).findById(permissionId).remove();
        createPermission(client, userAlice.admin().toRepresentation().getId(), usersType, Set.of(VIEW), allowMyAdminPermission);

        users.get(search.get(0).getId()).resetPassword(credential);

        // set credential label - admin UI sets the label upon resetting the password
        List<CredentialRepresentation> credentials = users.get(search.get(0).getId()).credentials();
        assertThat(credentials, hasSize(1));
        users.get(search.get(0).getId()).setCredentialUserLabel(credentials.get(0).getId(), "User Label");
    }

    @Test
    public void testAdminGroupViewPermission() {
        // Create group 'test_admins'
        GroupRepresentation testAdminsGroup = new GroupRepresentation();
        testAdminsGroup.setName("test_admins");
        testAdminsGroup.setId(ApiUtil.getCreatedId(realm.admin().groups().add(testAdminsGroup)));

        // Add user 'myadmin' as a member of 'test_admins'
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        realm.admin().users().get(myadmin.getId()).joinGroup(testAdminsGroup.getId());

        // Create user permission allowing to 'view' all users by members of 'test_admins' group
        GroupPolicyRepresentation allowAdmins = createGroupPolicy(realm, client, "Allow 'test_admins'", testAdminsGroup.getId(), Logic.POSITIVE);
        createAllPermission(client, usersType, allowAdmins, Set.of(VIEW));

        // Create group permission denying to 'manage' specific group: 'test_admins' by members of 'test_admins'
        GroupPolicyRepresentation denyAdmins = createGroupPolicy(realm, client, "Deny Policy", testAdminsGroup.getId(), Logic.NEGATIVE);
        createGroupPermission(testAdminsGroup, Set.of(MANAGE), denyAdmins);

        UserRepresentation representation = realmAdminClient.realm(realm.getName()).users().get(myadmin.getId()).toRepresentation();
        assertThat(representation, notNullValue());
    }

    @Test
    public void testViewUserWithAdminRoleAfterDisablingFgap() {
        // setup permission to allow view all users by myadmin
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(client, usersType, allowMyAdminPermission, Set.of(VIEW));

        // get userAlice user by myadmin
        UserRepresentation representation = realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).toRepresentation();
        assertThat(representation, notNullValue());

        // disable FGAP for realm
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setAdminPermissionsEnabled(Boolean.FALSE);
        realm.admin().update(realmRep);

        //assign view-users role to myadmin
        String realmManagementClientId = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewUsersRole = realm.admin().clients().get(realmManagementClientId).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(realmManagementClientId).add(List.of(viewUsersRole));

        // get userAlice user by myadmin again - it threw NPE before the fix
        // need to use separate Keycloak instance so that new role assignment is picked up
        try (Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBaseUrl().toString())
                .realm(realm.getName())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .username(myadmin.getUsername())
                .password("password")
                .build()) {
            representation = keycloak.realm(realm.getName()).users().get(userAlice.getId()).toRepresentation();
            assertThat(representation, notNullValue());
        }
    }
}
