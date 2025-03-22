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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.authorization.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.AdminPermissionsSchema.VIEW_MEMBERS;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.authorization.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class UserResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private final String usersType = AdminPermissionsSchema.USERS.getType();

    @BeforeEach
    public void onBeforeEach() {
        for (int i = 0; i < 50; i++) {
            realm.admin().users().create(UserConfigBuilder.create().username("user-" + i).build()).close();
        }
    }

    @AfterEach
    public void onAfterEach() {
        ScopePermissionsResource permissions = getScopePermissionsResource(client);

        for (ScopePermissionRepresentation permission : permissions.findAll(null, null, null, -1, -1)) {
            permissions.findById(permission.getId()).remove();
        }
    }

    @Test
    public void testViewAllUsersUsingUserPolicy() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, usersType, policy, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 50);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(client, usersType, policy, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 50);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedUsers = Set.of("user-0", "user-15", "user-30", "user-45");
        createPermission(client, notAllowedUsers, usersType, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(UserRepresentation::getUsername).noneMatch(notAllowedUsers::contains));
    }

    @Test
    public void testViewUserUsingUserPolicy() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, "user-9", usersType, Set.of(VIEW), policy);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testViewUserUsingGroupPolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("administrators");

        try (Response response = realm.admin().groups().add(rep)) {
            String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            GroupPolicyRepresentation policy = createGroupPolicy(realm, client, "Admin Group Policy", groupId, Logic.POSITIVE);
            createPermission(client, "user-9", usersType, Set.of(VIEW), policy);

        }

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testViewUserUsingRolePolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = new RoleRepresentation();
        role.setName("administrators");
        realm.admin().roles().create(role);

        String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
        role = realm.admin().roles().get(role.getName()).toRepresentation();
        realm.admin().users().get(adminUserId).roles().realmLevel().add(List.of(role));
        RolePolicyRepresentation policy = createRolePolicy(realm, client, "Admin Role Policy", role.getId(), Logic.POSITIVE);
        createPermission(client, "user-9", usersType, Set.of(VIEW), policy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testViewUserUsingMultiplePolicies() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = new RoleRepresentation();
        role.setName(KeycloakModelUtils.generateId());
        realm.admin().roles().create(role);

        String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
        role = realm.admin().roles().get(role.getName()).toRepresentation();
        realm.admin().users().get(adminUserId).roles().realmLevel().add(List.of(role));
        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, client, "Admin Role Policy", role.getId(), Logic.POSITIVE);

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName(KeycloakModelUtils.generateId());
        GroupPolicyRepresentation groupPolicy;

        try (Response response = realm.admin().groups().add(rep)) {
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            groupPolicy = createGroupPolicy(realm, client, "Admin Group Policy", groupId, Logic.POSITIVE);
        }

        createPermission(client, "user-9", usersType, Set.of(VIEW), rolePolicy, groupPolicy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());

        RolePoliciesResource rolePolicyResource = client.admin().authorization().policies().role();
        rolePolicy = rolePolicyResource.findByName(rolePolicy.getName());
        rolePolicy.setLogic(Logic.NEGATIVE);
        rolePolicyResource.findById(rolePolicy.getId()).update(rolePolicy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());
    }

    @Test
    public void testViewGroupMembersPolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        GroupRepresentation group = new GroupRepresentation();
        group.setName(KeycloakModelUtils.generateId());

        Set<String> memberUsernames = Set.of("user-0", "user-15", "user-30", "user-45");

        try (Response response = realm.admin().groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
            for (String username: memberUsernames) {
                String id = realm.admin().users().search(username).get(0).getId();
                realm.admin().users().get(id).joinGroup(group.getId());
            }
        }

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, group.getId(), AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), policy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(memberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(memberUsernames::contains));

        UserPolicyRepresentation negativePolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, realm.admin().users().search("user-0").get(0).getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), negativePolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(UserRepresentation::getUsername).noneMatch("user-0"::equals));
    }

    @Test
    public void testListingUsersWithRolesOnly() {
        List<UserRepresentation> search = realm.admin().users().search("myadmin");
        assertThat(search, Matchers.hasSize(1));

        String userId = search.get(0).getId();
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewUsers = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_USERS).toRepresentation();
        realm.admin().users().get(userId).roles().clientLevel(clientUuid).add(List.of(viewUsers));
        realm.cleanup().add(r -> r.users().get(userId).roles().clientLevel(clientUuid).remove(List.of(viewUsers)));

        assertThat(realmAdminClient.realm(realm.getName()).users().list(), not(empty()));
    }
}
