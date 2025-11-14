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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.client.resource.RolePoliciesResource;
import org.keycloak.admin.ui.rest.model.SessionRepresentation;
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
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.RoleConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class UserResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectClient(ref = "test_client")
    ManagedClient testClient;

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

    @Test
    public void testSessionEndpointRespectsUserViewPermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_REALM).toRepresentation();

        // create users
        for (int i = 0; i < 4; i++) {
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserConfigBuilder.create()
                    .username("user" + i)
                    .password("password")
                    .firstName("user")
                    .lastName(Integer.toString(i))
                    .email("user" + i + "@test")
                    .build()));
            // assign view-realm role to user to be able to access the server info endpoint (to create session)
            realm.admin().users().get(userId).roles().clientLevel(clientUuid).add(List.of(viewRealmRole));
        }

        // grant permission to view user1 and user2 to myadmin
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Myadmin user policy", myadmin.getId());
        Set<String> allowedUsers = Set.of("user1", "user2");
        createPermission(client, allowedUsers, usersType, Set.of(VIEW), policy);

        // assign view-realm role to myadmin so that the user can access the sessions endpoint
        realm.admin().users().get(myadmin.getId()).roles().clientLevel(clientUuid).add(List.of(viewRealmRole));
        realm.cleanup().add(r -> r.users().get(myadmin.getId()).roles().clientLevel(clientUuid).remove(List.of(viewRealmRole)));

        // Create sessions for user1, user2 and user3
        Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true);;
        List<Keycloak> keycloakInstances = List.of();
        try {
            keycloakInstances = Stream.of("user1", "user2", "user3")
                    .map(username -> KeycloakBuilder.builder()
                            .serverUrl(keycloakUrls.getBaseUrl().toString())
                            .realm(realm.getName())
                            .grantType(OAuth2Constants.PASSWORD)
                            .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                            .username(username)
                            .password("password")
                            .build())
                    .peek(kc -> kc.serverInfo().getInfo()) // get server info to create the session
                    .toList();

            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin")
                    .path("realms")
                    .path(realm.getName())
                    .path("ui-ext")
                    .path("sessions")
                    .register(new BearerAuthFilter(realmAdminClient.tokenManager()));

            Response response = target.request(MediaType.APPLICATION_JSON).get();

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            List<String> sessions = response.readEntity(new GenericType<List<SessionRepresentation>>() {}).stream().map(SessionRepresentation::getUsername).toList();
            assertThat(sessions, hasSize(allowedUsers.size()));
            assertThat(sessions, hasItems(allowedUsers.toArray(new String[0])));
        } finally {
            //close http client
            httpClient.close();
            //close keycloak instances
            keycloakInstances.forEach(Keycloak::close);
        }
    }

    @Test
    public void testRoleMemberFilteringByViewPermission() {
        // Create client role
        RoleRepresentation role = new RoleRepresentation();
        role.setName("test_role");
        realm.admin().clients().get(testClient.getId()).roles().create(role);
        role = realm.admin().clients().get(testClient.getId()).roles().get(role.getName()).toRepresentation();
        realm.cleanup().add(r -> r.roles().deleteRole("test_role"));

        // assign role to users
        for (String username : List.of("user_x", "user_y", "user_z")) {
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserConfigBuilder.create()
                    .username(username)
                    .password("password")
                    .firstName("user")
                    .lastName(username)
                    .email(username + "@test")
                    .build()));
            realm.admin().users().get(userId).roles().clientLevel(testClient.getId()).add(List.of(role));
            realm.cleanup().add(r -> r.users().delete(userId));
        }

        // Grant myadmin permission to view user_x and user_y, and to view the test client
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Myadmin user policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> allowedUsers = Set.of("user_x", "user_y");
        createPermission(client, allowedUsers, AdminPermissionsSchema.USERS.getType(), Set.of(AdminPermissionsSchema.VIEW), policy);
        createPermission(client, Set.of(testClient.getId()), AdminPermissionsSchema.CLIENTS.getType(), Set.of(AdminPermissionsSchema.VIEW), policy);

        // Query role members as myadmin
        List<String> roleMembers = realmAdminClient.realm(realm.getName()).clients().get(testClient.getId()).roles().get(role.getName()).getUserMembers().stream().map(UserRepresentation::getUsername).toList();

        // Assert only permitted users are returned as role members
        assertThat(roleMembers, hasSize(allowedUsers.size()));
        assertThat(roleMembers, hasItems(allowedUsers.toArray(new String[0])));
    }
}
