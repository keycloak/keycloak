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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.authorization.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class UserResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private final String newUserUsername = "new_user";

    @AfterEach
    public void onAfter() {
        ScopePermissionsResource permissions = getScopePermissionsResource();

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }

        realm.admin().users().search(newUserUsername).forEach(user -> realm.admin().users().get(user.getId()).remove());
    }

    @Test
    public void testManageAllPermission() {
        // myadmin shouldn't be able to create user just yet
        try (Response response = realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build())) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        //create all-users permission for "myadmin" (so that myadmin can manage all users in the realm)
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        policy.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(policy).close();
        createAllUserPermission(policy, Set.of(MANAGE));

        // creating user requires manage scope
        String newUserId = ApiUtil.handleCreatedResponse(realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build()));

        // it should be possible to update the user due to fallback to all-users permission
        realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("new@test.com").build());
        assertEquals("new@test.com", realmAdminClient.realm(realm.getName()).users().get(newUserId).toRepresentation().getEmail());
    }

    @Test
    public void testManageUserPermission() {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        policy.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(policy).close();
        createAllUserPermission(policy, Set.of(MANAGE));

        // creating user requires manage scope
        String newUserId = ApiUtil.handleCreatedResponse(realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username(newUserUsername).build()));

        // remove all-users permissions to test user-permission
        ScopePermissionRepresentation allUsersPermission = getScopePermissionsResource().findByName(AdminPermissionsSchema.USERS.getType());
        getScopePermissionsResource().findById(allUsersPermission.getId()).remove();

        // create user-permissions
        UserPolicyRepresentation allowNewUserOnlyForMyAdmin = new UserPolicyRepresentation();
        allowNewUserOnlyForMyAdmin.setName("My Admin User Policy");
        allowNewUserOnlyForMyAdmin.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(allowNewUserOnlyForMyAdmin).close();
        createUserPermission(UserConfigBuilder.create().id(newUserId).build(), Set.of(MANAGE), allowNewUserOnlyForMyAdmin);

        // it should be possible to update the user due to single user-permission
        realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("email@test.com").build());
        assertEquals("email@test.com", realmAdminClient.realm(realm.getName()).users().get(newUserId).toRepresentation().getEmail());

        // remove the user permission
        getScopePermissionsResource().findAll(null, null, null, null, null).forEach(permission -> {
            getScopePermissionsResource().findById(permission.getId()).remove();
        });

        // updating the user should be denied
        try {
            realmAdminClient.realm(realm.getName()).users().get(newUserId).update(UserConfigBuilder.create().email("email@test.com").build());
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }
    }

    @Test
    public void testViewAllPermission() {
        UserPolicyRepresentation policy = new UserPolicyRepresentation();
        policy.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        policy.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(policy).close();
        ScopePermissionRepresentation permission = createAllUserPermission(policy, Set.of(VIEW));
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
        UserPolicyRepresentation allowMyAdminPermission = new UserPolicyRepresentation();
        allowMyAdminPermission.setName("Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        allowMyAdminPermission.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(allowMyAdminPermission).close();
        createAllUserPermission(allowMyAdminPermission, Set.of(VIEW));
        UserPolicyRepresentation denyMyAdminAccessingHisAccountPermission = new UserPolicyRepresentation();
        denyMyAdminAccessingHisAccountPermission.setName("Not My Admin User Policy");
        denyMyAdminAccessingHisAccountPermission.addUser(myadmin.getId());
        denyMyAdminAccessingHisAccountPermission.setLogic(Logic.NEGATIVE);
        client.admin().authorization().policies().user().create(denyMyAdminAccessingHisAccountPermission).close();
        createUserPermission(myadmin, Set.of(VIEW), denyMyAdminAccessingHisAccountPermission);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testViewUserPermissionDenyByDefault() {
        UserPolicyRepresentation disallowMyAdmin = new UserPolicyRepresentation();
        disallowMyAdmin.setName("Not Only My Admin User Policy");
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        disallowMyAdmin.addUser(myadmin.getId());
        disallowMyAdmin.setLogic(Logic.NEGATIVE);
        client.admin().authorization().policies().user().create(disallowMyAdmin).close();
        createAllUserPermission(disallowMyAdmin, Set.of(VIEW));
        UserPolicyRepresentation allowAliceOnlyForMyAdmin = new UserPolicyRepresentation();
        allowAliceOnlyForMyAdmin.setName("My Admin User Policy");
        allowAliceOnlyForMyAdmin.addUser(myadmin.getId());
        client.admin().authorization().policies().user().create(allowAliceOnlyForMyAdmin).close();
        createUserPermission(userAlice.admin().toRepresentation(), Set.of(VIEW), allowAliceOnlyForMyAdmin);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    private ScopePermissionRepresentation createAllUserPermission(UserPolicyRepresentation policy, Set<String> scopes) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .name(AdminPermissionsSchema.USERS.getType())
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(scopes)
                .addPolicies(List.of(policy.getName()))
                .build();

        createPermission(permission);

        return permission;
    }

    private ScopePermissionRepresentation createUserPermission(UserRepresentation user, Set<String> scopes, UserPolicyRepresentation... policies) {
        ScopePermissionRepresentation permission = PermissionBuilder.create()
                .resourceType(AdminPermissionsSchema.USERS.getType())
                .scopes(scopes)
                .resources(Set.of(user.getId()))
                .addPolicies(Arrays.asList(policies).stream().map(AbstractPolicyRepresentation::getName).collect(Collectors.toList()))
                .build();

        createPermission(permission);

        return permission;
    }
}
