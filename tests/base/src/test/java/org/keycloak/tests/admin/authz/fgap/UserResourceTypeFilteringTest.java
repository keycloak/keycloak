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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testframework.realm.RoleConfigBuilder;

@KeycloakIntegrationTest
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
    public void testCountWithFilters() {
        assertThat(realmAdminClient.realm(realm.getName()).users().count("user-"), is(0));
        assertThat(realmAdminClient.realm(realm.getName()).users().count(null, null, null, "user-15"), is(0));

        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> allowedUsers = Set.of("user-0", "user-15", "user-30");
        createPermission(client, allowedUsers, usersType, Set.of(VIEW), allowPolicy);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(allowedUsers.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(allowedUsers::contains));

        assertThat(realmAdminClient.realm(realm.getName()).users().count("user-"), is(allowedUsers.size()));
        assertThat(realmAdminClient.realm(realm.getName()).users().count(null, null, null, "user-15"), is(1));
    }

    @Test
    public void testViewUserUsingUserPolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, "user-9", usersType, Set.of(VIEW), policy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
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
        assertTrue(realmAdminClient.realm(realm.getName()).groups().group(group.getId()).members().stream().map(UserRepresentation::getUsername).noneMatch("user-0"::equals));
    }

    @Test
    public void testDenyGroupViewMembersPolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        GroupRepresentation allowedMembers = new GroupRepresentation();
        allowedMembers.setName(KeycloakModelUtils.generateId());

        Set<String> memberUsernames = Set.of("user-0", "user-15", "user-30", "user-45");

        try (Response response = realm.admin().groups().add(allowedMembers)) {
            allowedMembers.setId(ApiUtil.getCreatedId(response));
            addGroupMember(allowedMembers.getId(), memberUsernames);
        }

        GroupRepresentation deniedMembers = new GroupRepresentation();

        deniedMembers.setName(KeycloakModelUtils.generateId());

        Set<String> deniedMemberUsernames = Set.of("user-0", "user-45");

        try (Response response = realm.admin().groups().add(deniedMembers)) {
            deniedMembers.setId(ApiUtil.getCreatedId(response));
            addGroupMember(deniedMembers.getId(), memberUsernames.stream().filter(deniedMemberUsernames::contains).collect(Collectors.toSet()));
        }

        // grant access to se members of a group
        UserPolicyRepresentation permitPolicy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, allowedMembers.getId(), AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), permitPolicy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(memberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(memberUsernames::contains));

        // deny access to the members of another group where access to some users in this group were previously granted
        UserPolicyRepresentation denyPolicy = createUserPolicy(Logic.NEGATIVE, realm, client,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, deniedMembers.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(memberUsernames.size() - deniedMemberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).noneMatch(deniedMemberUsernames::contains));

        // grant access to a specific user that is protected, the permission will have no effect because the user cannot be accessed due to the group permission
        String userId = realm.admin().users().search("user-0").get(0).getId();
        createPermission(client, userId, USERS_RESOURCE_TYPE, Set.of(VIEW), permitPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        Set<String> expected = new HashSet<>(memberUsernames);
        expected.removeAll(deniedMemberUsernames);
        assertFalse(search.isEmpty());
        assertEquals(expected.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(expected::contains));

        // the user is no longer a member of the group that holds members that cannot be accessed, they can be accessed now
        realm.admin().users().get(userId).leaveGroup(deniedMembers.getId());
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        expected = new HashSet<>(memberUsernames);
        expected.removeAll(deniedMemberUsernames);
        expected.add("user-0");
        assertFalse(search.isEmpty());
        assertEquals(expected.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(expected::contains));
    }

    private void addGroupMember(String groupId, Set<String> usernames) {
        for (String username: usernames) {
            String id = realm.admin().users().search(username).get(0).getId();
            realm.admin().users().get(id).joinGroup(groupId);
        }
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

    @Test
    public void testSearchById() {
        UserRepresentation expected = realm.admin().users().search("user-0").get(0);
        assertThat(realmAdminClient.realm(realm.getName()).users().search("id:" + expected.getId(), -1, -1), hasSize(0));
        UserPolicyRepresentation negativePolicy = createUserPolicy(realm, client,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(client, expected.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), negativePolicy);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertThat(search, Matchers.hasSize(1));
        UserRepresentation user = search.get(0);
        assertThat(user.getUsername(), Matchers.is("user-0"));
        assertThat(realmAdminClient.realm(realm.getName()).users().search("id:" + user.getId(), -1, -1), hasSize(1));
    }

    @Test
    public void testViewUserUsingRoleInheritedFromGroup() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = RoleConfigBuilder.create().name("myrole").build();
        realm.admin().roles().create(role);
        role = realm.admin().roles().get(role.getName()).toRepresentation();

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("administrators");

        try (Response response = realm.admin().groups().add(rep)) {
            String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            realm.admin().groups().group(groupId).roles().realmLevel().add(List.of(role));
            RolePolicyRepresentation policy = createRolePolicy(realm, client, "My Role Policy", role.getId(), Logic.POSITIVE);
            createPermission(client, "user-9", usersType, Set.of(VIEW), policy);
        }

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testViewUserUsingRoleInheritedFromCompositeRole() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = RoleConfigBuilder.create().name("myrole").build();

        realm.admin().roles().create(role);
        role = realm.admin().roles().get(role.getName()).toRepresentation();

        RoleRepresentation compositeRole = RoleConfigBuilder.create()
                .name("mycompositerole")
                .composite(true)
                .realmComposite(role.getName())
                .build();
        realm.admin().roles().create(compositeRole);
        compositeRole = realm.admin().roles().get(compositeRole.getName()).toRepresentation();

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("administrators");

        try (Response response = realm.admin().groups().add(rep)) {
            String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            realm.admin().groups().group(groupId).roles().realmLevel().add(List.of(compositeRole));
            RolePolicyRepresentation policy = createRolePolicy(realm, client, "My Role Policy", role.getId(), Logic.POSITIVE);
            createPermission(client, "user-9", usersType, Set.of(VIEW), policy);
        }

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }
}
