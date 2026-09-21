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

import jakarta.ws.rs.ForbiddenException;
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
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.RoleBuilder;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
            realm.admin().users().create(UserBuilder.create().username("user-" + i).build()).close();
        }
    }

    @Test
    @DatabaseTest
    public void testViewAllUsersUsingUserPolicy() {
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, usersType, policy, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 50);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    @DatabaseTest
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, usersType, policy, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 50);
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedUsers = Set.of("user-0", "user-15", "user-30", "user-45");
        createPermission(adminPermissionsClient, notAllowedUsers, usersType, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(UserRepresentation::getUsername).noneMatch(notAllowedUsers::contains));
    }

    @Test
    @DatabaseTest
    public void testCountWithFilters() {
        assertThat(realmAdminClient.realm(realm.getName()).users().count("user-"), is(0));
        assertThat(realmAdminClient.realm(realm.getName()).users().count(null, null, null, "user-15"), is(0));

        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> allowedUsers = Set.of("user-0", "user-15", "user-30");
        createPermission(adminPermissionsClient, allowedUsers, usersType, Set.of(VIEW), allowPolicy);

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

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), policy);

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
            GroupPolicyRepresentation policy = createGroupPolicy(realm, adminPermissionsClient, "Admin Group Policy", Logic.POSITIVE, groupId);
            createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), policy);

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
        RolePolicyRepresentation policy = createRolePolicy(realm, adminPermissionsClient, "Admin Role Policy", role.getId(), Logic.POSITIVE);
        createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), policy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testNegativeAggregateWithUnsupportedChildDeniesWithCompetingAllow() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowAll = createUserPolicy(
                realm, adminPermissionsClient, "Allow All Users Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowAll, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(null, 0, 50);
        assertThat(search, hasSize(50));

        TimePolicyRepresentation timePolicy = new TimePolicyRepresentation();
        timePolicy.setName("Always Matching Time Policy");
        try (Response response = adminPermissionsClient.authorization().policies().time()
                .create(timePolicy)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        AggregatePolicyRepresentation aggregatePolicy = createAggregatedPolicy(
                adminPermissionsClient, "Negative Aggregate With Time Child Policy",
                Logic.NEGATIVE, DecisionStrategy.AFFIRMATIVE, timePolicy.getName());

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), aggregatePolicy);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search, is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));

        search = realmAdminClient.realm(realm.getName())
                .users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
    }

    @Test
    public void testAggregatePolicyWithNegativeChildDenyOverridesTypeWideAllow() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowAll = createUserPolicy(
                realm, adminPermissionsClient, "Allow All Users Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowAll, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(null, 0, 50);
        assertThat(search, hasSize(50));

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Not My Admin User Policy", myadmin.getId());
        AggregatePolicyRepresentation aggregatePolicy = createAggregatedPolicy(
                adminPermissionsClient, "Positive Aggregate With Negative Child Policy",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, denyMyAdmin.getName());
        Set<String> deniedUsers = Set.of("user-0", "user-15", "user-30");

        createPermission(adminPermissionsClient, deniedUsers, usersType, Set.of(VIEW), aggregatePolicy);

        search = realmAdminClient.realm(realm.getName())
                .users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(),
                not(hasItems("user-0", "user-15", "user-30")));

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testNestedAggregateDenyOverridesTypeWideAllow() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowAll = createUserPolicy(
                realm, adminPermissionsClient, "Allow All Users Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowAll, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(null, 0, 50);
        assertThat(search, hasSize(50));

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Not My Admin User Policy", myadmin.getId());
        AggregatePolicyRepresentation innerAggregate = createAggregatedPolicy(
                adminPermissionsClient, "Inner Aggregate With Negative Child",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, denyMyAdmin.getName());
        AggregatePolicyRepresentation outerAggregate = createAggregatedPolicy(
                adminPermissionsClient, "Outer Aggregate Wrapping Inner",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, innerAggregate.getName());

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), outerAggregate);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search, is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));

        search = realmAdminClient.realm(realm.getName())
                .users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
    }

    @Test
    public void testGroupPolicyExtendChildrenInsideAggregateDenyOverridesTypeWideAllow() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowAll = createUserPolicy(
                realm, adminPermissionsClient, "Allow All Users Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowAll, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(null, 0, 50);
        assertThat(search, hasSize(50));

        GroupRepresentation parentGroup = createGroup("fgap-parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("fgap-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(myadmin.getId()).joinGroup(childGroup.getId());

        GroupPolicyRepresentation denyParentSubtree = new GroupPolicyRepresentation();
        denyParentSubtree.setName("Deny Parent Subtree Policy");
        denyParentSubtree.setLogic(Logic.NEGATIVE);
        denyParentSubtree.addGroup(parentGroup.getId(), true);
        try (Response response = adminPermissionsClient.authorization().policies().group()
                .create(denyParentSubtree)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        AggregatePolicyRepresentation aggregatePolicy = createAggregatedPolicy(
                adminPermissionsClient, "Aggregate Wrapping Group Deny Policy",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, denyParentSubtree.getName());

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), aggregatePolicy);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search, is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));

        search = realmAdminClient.realm(realm.getName())
                .users().search(null, -1, -1);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
    }

    @Test
    public void testAggregatePolicyUnanimousWithMixedChildren() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                Logic.POSITIVE, realm, adminPermissionsClient, "Allow My Admin User Policy", myadmin.getId());
        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin User Policy", myadmin.getId());
        AggregatePolicyRepresentation aggregatePolicy = createAggregatedPolicy(
                adminPermissionsClient, "Unanimous Aggregate With Mixed Children",
                Logic.POSITIVE, DecisionStrategy.UNANIMOUS, allowMyAdmin.getName(), denyMyAdmin.getName());

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), aggregatePolicy);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search, is(empty()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testAggregateWithSupportedAndUnsupportedChildren() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                Logic.POSITIVE, realm, adminPermissionsClient, "Allow My Admin User Policy", myadmin.getId());

        TimePolicyRepresentation timePolicy = new TimePolicyRepresentation();
        timePolicy.setName("Always Matching Time Policy");
        try (Response response = adminPermissionsClient.authorization().policies().time()
                .create(timePolicy)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        AggregatePolicyRepresentation aggregatePolicy = createAggregatedPolicy(
                adminPermissionsClient, "Aggregate With Supported And Unsupported Children",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, allowMyAdmin.getName(), timePolicy.getName());

        createPermission(adminPermissionsClient, "user-15", usersType, Set.of(VIEW), aggregatePolicy);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search("user-15", 0, 10);
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getUsername(), is("user-15"));
    }

    @Test
    public void testNestedNegativeAggregateWithUnsupportedGrandchildDenies() {
        TimePolicyRepresentation timePolicy = new TimePolicyRepresentation();
        timePolicy.setName("Always Matching Time Policy");
        try (Response response = adminPermissionsClient.authorization().policies().time()
                .create(timePolicy)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        AggregatePolicyRepresentation innerAggregate = createAggregatedPolicy(
                adminPermissionsClient, "Inner Aggregate With Unsupported Child",
                Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, timePolicy.getName());
        AggregatePolicyRepresentation outerAggregate = createAggregatedPolicy(
                adminPermissionsClient, "Outer Negative Aggregate Wrapping Inner",
                Logic.NEGATIVE, DecisionStrategy.AFFIRMATIVE, innerAggregate.getName());

        UserRepresentation targetUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, targetUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), outerAggregate);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(targetUser.getUsername(), 0, 10);
        assertThat(search, is(empty()));
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
        RolePolicyRepresentation rolePolicy = createRolePolicy(realm, adminPermissionsClient, "Admin Role Policy", role.getId(), Logic.POSITIVE);

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName(KeycloakModelUtils.generateId());
        GroupPolicyRepresentation groupPolicy;

        try (Response response = realm.admin().groups().add(rep)) {
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            groupPolicy = createGroupPolicy(realm, adminPermissionsClient, "Admin Group Policy", Logic.POSITIVE, groupId);
        }

        createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), rolePolicy, groupPolicy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());

        RolePoliciesResource rolePolicyResource = adminPermissionsClient.authorization().policies().role();
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

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, group.getId(), AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), policy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(memberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(memberUsernames::contains));

        UserPolicyRepresentation negativePolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, realm.admin().users().search("user-0").get(0).getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), negativePolicy);
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
        UserPolicyRepresentation permitPolicy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, allowedMembers.getId(), AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), permitPolicy);

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(memberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(memberUsernames::contains));

        // deny access to the members of another group where access to some users in this group were previously granted
        UserPolicyRepresentation denyPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, deniedMembers.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(memberUsernames.size() - deniedMemberUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).noneMatch(deniedMemberUsernames::contains));

        // grant access to a specific user that is protected, the permission will have no effect because the user cannot be accessed due to the group permission
        String userId = realm.admin().users().search("user-0").get(0).getId();
        createPermission(adminPermissionsClient, userId, USERS_RESOURCE_TYPE, Set.of(VIEW), permitPolicy);
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
        UserPolicyRepresentation negativePolicy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, expected.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), negativePolicy);
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertThat(search, Matchers.hasSize(1));
        UserRepresentation user = search.get(0);
        assertThat(user.getUsername(), Matchers.is("user-0"));
        assertThat(realmAdminClient.realm(realm.getName()).users().search("id:" + user.getId(), -1, -1), hasSize(1));
    }

    @Test
    public void testBruteForceUserEndpointSearchByIdFilteredByViewPermission() {
        UserRepresentation allowed = realm.admin().users().search("user-0").get(0);
        UserRepresentation denied = realm.admin().users().search("user-1").get(0);

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, allowed.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), policy);

        try (Client httpClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            WebTarget target = httpClient.target(keycloakUrls.getBaseUrl().toString())
                    .path("admin")
                    .path("realms")
                    .path(realm.getName())
                    .path("ui-ext")
                    .path("brute-force-user")
                    .register(new BearerAuthFilter(realmAdminClient.tokenManager()));

            Response allowedResponse = target.queryParam("search", "id:" + allowed.getId())
                    .request(MediaType.APPLICATION_JSON).get();
            assertThat(allowedResponse.getStatus(), is(Response.Status.OK.getStatusCode()));
            List<UserRepresentation> allowedResult = allowedResponse.readEntity(new GenericType<>() {});
            assertThat(allowedResult, hasSize(1));
            assertThat(allowedResult.get(0).getUsername(), is("user-0"));

            Response deniedResponse = target.queryParam("search", "id:" + denied.getId())
                    .request(MediaType.APPLICATION_JSON).get();
            assertThat(deniedResponse.getStatus(), is(Response.Status.OK.getStatusCode()));
            List<UserRepresentation> deniedResult = deniedResponse.readEntity(new GenericType<>() {});
            assertThat(deniedResult, is(empty()));
        }
    }

    @Test
    public void testViewUserUsingRoleInheritedFromGroup() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = RoleBuilder.create().name("myrole").build();
        realm.admin().roles().create(role);
        role = realm.admin().roles().get(role.getName()).toRepresentation();

        GroupRepresentation rep = new GroupRepresentation();
        rep.setName("administrators");

        try (Response response = realm.admin().groups().add(rep)) {
            String adminUserId = realm.admin().users().search("myadmin").get(0).getId();
            String groupId = ApiUtil.getCreatedId(response);
            realm.admin().users().get(adminUserId).joinGroup(groupId);
            realm.admin().groups().group(groupId).roles().realmLevel().add(List.of(role));
            RolePolicyRepresentation policy = createRolePolicy(realm, adminPermissionsClient, "My Role Policy", role.getId(), Logic.POSITIVE);
            createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), policy);
        }

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testViewUserUsingRoleInheritedFromCompositeRole() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        RoleRepresentation role = RoleBuilder.create().name("myrole").build();

        realm.admin().roles().create(role);
        role = realm.admin().roles().get(role.getName()).toRepresentation();

        RoleRepresentation compositeRole = RoleBuilder.create()
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
            RolePolicyRepresentation policy = createRolePolicy(realm, adminPermissionsClient, "My Role Policy", role.getId(), Logic.POSITIVE);
            createPermission(adminPermissionsClient, "user-9", usersType, Set.of(VIEW), policy);
        }

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());
    }

    @Test
    public void testExtendChildrenGroupDenyExcludesDeniedUserFromSearch() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("fgap-parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("fgap-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(myadmin.getId()).joinGroup(childGroup.getId());

        GroupPolicyRepresentation denyParentSubtree = new GroupPolicyRepresentation();
        denyParentSubtree.setName("Deny Parent Subtree Policy");
        denyParentSubtree.setLogic(Logic.NEGATIVE);
        denyParentSubtree.addGroup(parentGroup.getId(), true);
        try (Response response = adminPermissionsClient.authorization().policies().group()
                .create(denyParentSubtree)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), denyParentSubtree);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testExtendChildrenGroupDenyThreeLevelHierarchy() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation grandparentGroup = createGroup("fgap-grandparent-" + KeycloakModelUtils.generateId());
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName("fgap-parent-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(grandparentGroup.getId()).subGroup(parentGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            parentGroup.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("fgap-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(myadmin.getId()).joinGroup(childGroup.getId());

        GroupPolicyRepresentation denyGrandparentSubtree = new GroupPolicyRepresentation();
        denyGrandparentSubtree.setName("Deny Grandparent Subtree Policy");
        denyGrandparentSubtree.setLogic(Logic.NEGATIVE);
        denyGrandparentSubtree.addGroup(grandparentGroup.getId(), true);
        try (Response response = adminPermissionsClient.authorization().policies().group()
                .create(denyGrandparentSubtree)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, deniedUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), denyGrandparentSubtree);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testExactGroupPolicyOnParentDoesNotDenyChildMember() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        GroupRepresentation parentGroup = createGroup("fgap-exact-parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("fgap-exact-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(myadmin.getId()).joinGroup(childGroup.getId());

        GroupPolicyRepresentation exactParentPolicy = new GroupPolicyRepresentation();
        exactParentPolicy.setName("Exact Parent Group Policy");
        exactParentPolicy.setLogic(Logic.POSITIVE);
        exactParentPolicy.addGroup(parentGroup.getId(), false);
        try (Response response = adminPermissionsClient.authorization().policies().group()
                .create(exactParentPolicy)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        UserRepresentation targetUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, targetUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), exactParentPolicy);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(targetUser.getId()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, targetUser.getUsername()), is(1));
    }

    @Test
    public void testMultiDefinitionGroupPolicyGrantsWhenOneDefinitionMatches() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);

        GroupRepresentation unrelatedGroup = createGroup("fgap-unrelated-" + KeycloakModelUtils.generateId());
        GroupRepresentation adminGroup = createGroup("fgap-admin-direct-" + KeycloakModelUtils.generateId());
        realm.admin().users().get(myadmin.getId()).joinGroup(adminGroup.getId());

        GroupPolicyRepresentation multiGroupPolicy = new GroupPolicyRepresentation();
        multiGroupPolicy.setName("Multi Group Policy");
        multiGroupPolicy.setLogic(Logic.POSITIVE);
        multiGroupPolicy.addGroup(unrelatedGroup.getId(), false);
        multiGroupPolicy.addGroup(adminGroup.getId(), false);
        try (Response response = adminPermissionsClient.authorization().policies().group()
                .create(multiGroupPolicy)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        UserRepresentation targetUser = realm.admin().users().search("user-15").get(0);
        createPermission(adminPermissionsClient, targetUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), multiGroupPolicy);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(targetUser.getId()));
    }

    @Test
    public void testSessionEndpointRespectsUserViewPermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        String clientUuid = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(clientUuid).roles().get(AdminRoles.VIEW_REALM).toRepresentation();

        // create users
        for (int i = 0; i < 4; i++) {
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create()
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
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Myadmin user policy", myadmin.getId());
        Set<String> allowedUsers = Set.of("user1", "user2");
        createPermission(adminPermissionsClient, allowedUsers, usersType, Set.of(VIEW), policy);

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
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create()
                    .username(username)
                    .password("password")
                    .firstName("user")
                    .lastName(username)
                    .email(username + "@test")
                    .build()));
            realm.admin().users().get(userId).roles().clientLevel(testClient.getId()).add(List.of(role));
            realm.cleanup().add(r -> r.users().delete(userId).close());
        }

        // Grant myadmin permission to view user_x and user_y, and to view the test client
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Myadmin user policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> allowedUsers = Set.of("user_x", "user_y");
        createPermission(adminPermissionsClient, allowedUsers, AdminPermissionsSchema.USERS.getType(), Set.of(AdminPermissionsSchema.VIEW), policy);
        createPermission(adminPermissionsClient, Set.of(testClient.getId()), AdminPermissionsSchema.CLIENTS.getType(), Set.of(AdminPermissionsSchema.VIEW), policy);

        // Query role members as myadmin
        List<String> roleMembers = realmAdminClient.realm(realm.getName()).clients().get(testClient.getId()).roles().get(role.getName()).getUserMembers().stream().map(UserRepresentation::getUsername).toList();

        // Assert only permitted users are returned as role members
        assertThat(roleMembers, hasSize(allowedUsers.size()));
        assertThat(roleMembers, hasItems(allowedUsers.toArray(new String[0])));
    }

    @Test
    public void testRealmRoleMemberFilteringByViewPermission() {
        RoleRepresentation role = new RoleRepresentation();
        role.setName("test_realm_role");
        realm.admin().roles().create(role);
        role = realm.admin().roles().get(role.getName()).toRepresentation();
        realm.cleanup().add(r -> r.roles().deleteRole("test_realm_role"));

        for (String username : List.of("user_x", "user_y", "user_z")) {
            String userId = ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create()
                    .username(username)
                    .password("password")
                    .firstName("user")
                    .lastName(username)
                    .email(username + "@test")
                    .build()));
            realm.admin().users().get(userId).roles().realmLevel().add(List.of(role));
            realm.cleanup().add(r -> r.users().delete(userId).close());
        }

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient, "Myadmin user policy",
                realm.admin().users().search("myadmin").get(0).getId());
        Set<String> allowedUsers = Set.of("user_x", "user_y");
        createPermission(adminPermissionsClient, allowedUsers, AdminPermissionsSchema.USERS.getType(),
                Set.of(AdminPermissionsSchema.VIEW), policy);

        String realmMgmtClientId = realm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0).getId();
        RoleRepresentation viewRealmRole = realm.admin().clients().get(realmMgmtClientId)
                .roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        realm.admin().users().get(myadminId).roles().clientLevel(realmMgmtClientId).add(List.of(viewRealmRole));
        realm.cleanup().add(r -> r.users().get(r.users().search("myadmin").get(0).getId())
                .roles().clientLevel(realmMgmtClientId).remove(List.of(viewRealmRole)));

        List<String> roleMembers = realmAdminClient.realm(realm.getName())
                .roles().get(role.getName()).getUserMembers().stream()
                .map(UserRepresentation::getUsername).toList();

        assertThat(roleMembers, hasSize(allowedUsers.size()));
        assertThat(roleMembers, hasItems(allowedUsers.toArray(new String[0])));
    }

    @Test
    public void testParentGroupDenyExcludesChildMemberFromSearch() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Only My Admin User Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("fgap-denied-parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("fgap-denied-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation deniedUser = realm.admin().users().search("user-15").get(0);
        realm.admin().users().get(deniedUser.getId()).joinGroup(childGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testMultiLevelHierarchyDenyExpansion() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation grandchildGroup = new GroupRepresentation();
        grandchildGroup.setName("grandchild-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(childGroup.getId()).subGroup(grandchildGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            grandchildGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation deniedUser = realm.admin().users().search("user-41").get(0);
        realm.admin().users().get(deniedUser.getId()).joinGroup(grandchildGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, deniedUser.getUsername()), is(0));
    }

    @Test
    public void testDirectUserPermissionDoesNotOverrideAncestorGroupDeny() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation targetUser = realm.admin().users().search("user-20").get(0);
        realm.admin().users().get(targetUser.getId()).joinGroup(childGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        // grant direct VIEW on the user — does not override the ancestor group deny
        createPermission(adminPermissionsClient, targetUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);

        // direct GET denied — group membership deny takes precedence over user-level permission
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(targetUser.getId()).toRepresentation());

        // search excluded — consistent with direct auth
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(targetUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, targetUser.getUsername()), is(0));
    }

    @Test
    public void testDenyParentAllowChildSubtraction() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation allowedChild = new GroupRepresentation();
        allowedChild.setName("allowed-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(allowedChild)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            allowedChild.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation deniedSibling = new GroupRepresentation();
        deniedSibling.setName("denied-sibling-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(deniedSibling)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            deniedSibling.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation allowedUser = realm.admin().users().search("user-2").get(0);
        realm.admin().users().get(allowedUser.getId()).joinGroup(allowedChild.getId());
        UserRepresentation deniedUser = realm.admin().users().search("user-3").get(0);
        realm.admin().users().get(deniedUser.getId()).joinGroup(deniedSibling.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);
        createPermission(adminPermissionsClient, allowedChild.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);

        // direct auth: user-2 in allowedChild (has explicit allow) should be accessible
        UserRepresentation fetched = realmAdminClient.realm(realm.getName())
                .users().get(allowedUser.getId()).toRepresentation();
        assertThat(fetched.getUsername(), is(allowedUser.getUsername()));

        // direct auth: user-3 in deniedSibling (no allow, parent deny cascades) should be denied
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(deniedUser.getId()).toRepresentation());

        // search must be consistent with direct auth
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(allowedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(allowedUser.getId()));

        search = realmAdminClient.realm(realm.getName())
                .users().search(deniedUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(deniedUser.getId())));
    }

    @Test
    public void testAllowChildDoesNotCascadeToGrandchild() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }
        GroupRepresentation grandchildGroup = new GroupRepresentation();
        grandchildGroup.setName("grandchild-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(childGroup.getId()).subGroup(grandchildGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            grandchildGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation userInChild = realm.admin().users().search("user-4").get(0);
        realm.admin().users().get(userInChild.getId()).joinGroup(childGroup.getId());
        UserRepresentation userInGrandchild = realm.admin().users().search("user-5").get(0);
        realm.admin().users().get(userInGrandchild.getId()).joinGroup(grandchildGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);
        createPermission(adminPermissionsClient, childGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);

        // direct auth: userInChild's direct group (child) has an explicit allow — accessible
        UserRepresentation fetched = realmAdminClient.realm(realm.getName())
                .users().get(userInChild.getId()).toRepresentation();
        assertThat(fetched.getUsername(), is(userInChild.getUsername()));

        // direct auth: userInGrandchild's direct group (grandchild) has no policies — walk hits child (allow) then parent (deny) — denied
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(userInGrandchild.getId()).toRepresentation());

        // search consistent with direct auth
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(userInChild.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(userInChild.getId()));

        search = realmAdminClient.realm(realm.getName())
                .users().search(userInGrandchild.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(userInGrandchild.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, userInGrandchild.getUsername()), is(0));
    }

    @Test
    public void testUserInBothAllowedGroupAndDeniedChildGroup() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        GroupRepresentation allowedGroup = createGroup("allowed-" + KeycloakModelUtils.generateId());
        GroupRepresentation deniedParent = createGroup("denied-parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation deniedChild = new GroupRepresentation();
        deniedChild.setName("denied-child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(deniedParent.getId()).subGroup(deniedChild)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            deniedChild.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation user = realm.admin().users().search("user-6").get(0);
        realm.admin().users().get(user.getId()).joinGroup(allowedGroup.getId());
        realm.admin().users().get(user.getId()).joinGroup(deniedChild.getId());

        createPermission(adminPermissionsClient, allowedGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);
        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, deniedParent.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(user.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(user.getId())));
    }

    @Test
    public void testMultipleDeniedParentsWithIndependentSubtrees() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parent1 = createGroup("parent1-" + KeycloakModelUtils.generateId());
        GroupRepresentation child1 = new GroupRepresentation();
        child1.setName("child1-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parent1.getId()).subGroup(child1)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            child1.setId(ApiUtil.getCreatedId(response));
        }

        GroupRepresentation parent2 = createGroup("parent2-" + KeycloakModelUtils.generateId());
        GroupRepresentation child2 = new GroupRepresentation();
        child2.setName("child2-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parent2.getId()).subGroup(child2)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            child2.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation userInChild1 = realm.admin().users().search("user-7").get(0);
        realm.admin().users().get(userInChild1.getId()).joinGroup(child1.getId());
        UserRepresentation userInChild2 = realm.admin().users().search("user-8").get(0);
        realm.admin().users().get(userInChild2.getId()).joinGroup(child2.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, Set.of(parent1.getId(), parent2.getId()),
                GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(userInChild1.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(userInChild1.getId())));

        search = realmAdminClient.realm(realm.getName())
                .users().search(userInChild2.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(userInChild2.getId())));

        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, userInChild1.getUsername()), is(0));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, userInChild2.getUsername()), is(0));
    }

    @Test
    public void testAllowedParentGroupDoesNotExpandToDescendantMembers() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation directMember = realm.admin().users().search("user-10").get(0);
        realm.admin().users().get(directMember.getId()).joinGroup(parentGroup.getId());

        UserRepresentation descendantMember = realm.admin().users().search("user-11").get(0);
        realm.admin().users().get(descendantMember.getId()).joinGroup(childGroup.getId());

        // allow VIEW_MEMBERS on parent group only — no type-level or direct user permissions
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);

        // direct member of parent group appears in search
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(directMember.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(directMember.getId()));

        // direct GET on descendant member succeeds — direct auth walks child → parent and finds the allow
        UserRepresentation fetched = realmAdminClient.realm(realm.getName())
                .users().get(descendantMember.getId()).toRepresentation();
        assertThat(fetched.getUsername(), is(descendantMember.getUsername()));

        // but search does NOT find the descendant member — allowed groups are not expanded to descendants
        search = realmAdminClient.realm(realm.getName())
                .users().search(descendantMember.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(descendantMember.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, descendantMember.getUsername()), is(0));

        // workaround 1: grant VIEW_MEMBERS on the child group explicitly — descendant member becomes visible
        createPermission(adminPermissionsClient, childGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);

        search = realmAdminClient.realm(realm.getName())
                .users().search(descendantMember.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(descendantMember.getId()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, descendantMember.getUsername()), is(1));
    }

    @Test
    public void testAllowedParentGroupDoesNotExpandToDescendantMembersDirectPermissionWorkaround() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation descendantMember = realm.admin().users().search("user-12").get(0);
        realm.admin().users().get(descendantMember.getId()).joinGroup(childGroup.getId());

        // allow VIEW_MEMBERS on parent group only
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), allowMyAdmin);

        // direct GET succeeds but search does not find the descendant member
        UserRepresentation fetched = realmAdminClient.realm(realm.getName())
                .users().get(descendantMember.getId()).toRepresentation();
        assertThat(fetched.getUsername(), is(descendantMember.getUsername()));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(descendantMember.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(descendantMember.getId())));

        // workaround 2: grant direct VIEW on the user — descendant member becomes visible
        createPermission(adminPermissionsClient, descendantMember.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);

        search = realmAdminClient.realm(realm.getName())
                .users().search(descendantMember.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                hasItems(descendantMember.getId()));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, descendantMember.getUsername()), is(1));
    }

    @Test
    @DatabaseTest
    public void testGroupDenyOverridesTypeLevelAndDirectResourcePermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        // type-level permission: admin can view ALL users
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 50);
        assertEquals(50, search.size());

        // create a group, add user-0 as member, then deny view-members on that group
        GroupRepresentation deniedGroup = createGroup("denied-group-" + KeycloakModelUtils.generateId());
        UserRepresentation targetUser = realm.admin().users().search("user-0").get(0);
        realm.admin().users().get(targetUser.getId()).joinGroup(deniedGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, deniedGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        // user-0 should not be visible because of the group deny
        search = realmAdminClient.realm(realm.getName()).users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(targetUser.getId())));

        // add a direct resource-level VIEW permission on user-0
        createPermission(adminPermissionsClient, targetUser.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), allowMyAdmin);

        // user-0 is still not visible — group deny overrides direct resource authorization
        search = realmAdminClient.realm(realm.getName()).users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(targetUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, targetUser.getUsername()), is(0));
    }

    @Test
    public void testGroupDenyOverridesDirectResourcePermission() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        // direct resource-level VIEW on three specific users (no type-level permission)
        Set<String> allowedUsernames = Set.of("user-0", "user-1", "user-2");
        createPermission(adminPermissionsClient, allowedUsernames, usersType, Set.of(VIEW), allowMyAdmin);

        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(allowedUsernames.size(), search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).allMatch(allowedUsernames::contains));

        // create a group containing user-0 and deny view-members on that group
        GroupRepresentation deniedGroup = createGroup("denied-group-" + KeycloakModelUtils.generateId());
        UserRepresentation targetUser = realm.admin().users().search("user-0").get(0);
        realm.admin().users().get(targetUser.getId()).joinGroup(deniedGroup.getId());

        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, deniedGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        // user-0 is no longer visible — group deny overrides the direct resource permission
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(),
                not(hasItems("user-0")));

        // user-1 and user-2 remain visible
        assertThat(search.stream().map(UserRepresentation::getUsername).toList(),
                hasItems("user-1", "user-2"));
        assertEquals(2, search.size());
    }

    @Test
    public void testUnrelatedChildPolicyDoesNotBypassAncestorDeny() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdmin = createUserPolicy(
                realm, adminPermissionsClient, "Allow My Admin Policy", myadmin.getId());

        // type-level allow: admin can VIEW all users
        createAllPermission(adminPermissionsClient, usersType, allowMyAdmin, Set.of(VIEW));

        GroupRepresentation parentGroup = createGroup("parent-" + KeycloakModelUtils.generateId());
        GroupRepresentation childGroup = new GroupRepresentation();
        childGroup.setName("child-" + KeycloakModelUtils.generateId());
        try (Response response = realm.admin().groups().group(parentGroup.getId()).subGroup(childGroup)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            childGroup.setId(ApiUtil.getCreatedId(response));
        }

        UserRepresentation targetUser = realm.admin().users().search("user-30").get(0);
        realm.admin().users().get(targetUser.getId()).joinGroup(childGroup.getId());

        // deny VIEW_MEMBERS on parent — should cascade to child
        UserPolicyRepresentation denyMyAdmin = createUserPolicy(
                Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin Policy", myadmin.getId());
        createPermission(adminPermissionsClient, parentGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW_MEMBERS), denyMyAdmin);

        // allow MANAGE_MEMBERS on child — unrelated scope, must not suppress the parent deny for VIEW_MEMBERS
        createPermission(adminPermissionsClient, childGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(MANAGE_MEMBERS), allowMyAdmin);

        // direct GET must be denied — parent VIEW_MEMBERS deny cascades regardless of child's MANAGE_MEMBERS allow
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .users().get(targetUser.getId()).toRepresentation());

        // search must be consistent with direct auth
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName())
                .users().search(targetUser.getUsername(), 0, 10);
        assertThat(search.stream().map(UserRepresentation::getId).toList(),
                not(hasItems(targetUser.getId())));
        assertThat(realmAdminClient.realm(realm.getName()).users()
                .count(null, null, null, targetUser.getUsername()), is(0));
    }

    @Test
    public void testViewGroupMembersPolicyUsingAggregatedPolicy() {
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        GroupRepresentation fooGroup = createGroup(KeycloakModelUtils.generateId());
        UserRepresentation fooUser = createUser(KeycloakModelUtils.generateId());
        realm.admin().users().get(fooUser.getId()).joinGroup(fooGroup.getId());
        GroupRepresentation fooGroupManager = createGroup(KeycloakModelUtils.generateId());

        UserRepresentation barUser = createUser(KeycloakModelUtils.generateId());
        GroupRepresentation barGroup = createGroup(KeycloakModelUtils.generateId());
        realm.admin().users().get(barUser.getId()).joinGroup(barGroup.getId());
        GroupRepresentation barGroupManager = createGroup(KeycloakModelUtils.generateId());

        GroupPolicyRepresentation fooGroupManagerPolicy = createGroupPolicy(realm, adminPermissionsClient, "Foo Group Policy", Logic.POSITIVE, fooGroupManager.getId());
        GroupPolicyRepresentation barGroupManagerPolicy = createGroupPolicy(realm, adminPermissionsClient, "Bar Group Policy", Logic.POSITIVE, barGroupManager.getId());
        AggregatePolicyRepresentation aggregatedPolicy = createAggregatedPolicy(adminPermissionsClient, "Foo and Bar Group Policy", Logic.POSITIVE, DecisionStrategy.AFFIRMATIVE, fooGroupManagerPolicy.getName(), barGroupManagerPolicy.getName());

        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, aggregatedPolicy, Set.of(VIEW_MEMBERS, MANAGE_MEMBERSHIP, MANAGE_MEMBERS));

        realm.admin().users().get(myadmin.getId()).joinGroup(fooGroupManager.getId());
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(3, search.size());
        assertTrue(search.stream().map(UserRepresentation::getUsername).anyMatch(fooUser.getUsername()::equals));
        assertTrue(search.stream().map(UserRepresentation::getUsername).anyMatch(barUser.getUsername()::equals));

        aggregatedPolicy.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        adminPermissionsClient.authorization().policies().aggregate().findById(aggregatedPolicy.getId()).update(aggregatedPolicy);
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertTrue(search.isEmpty());

        realm.admin().users().get(myadmin.getId()).joinGroup(barGroupManager.getId());
        search = realmAdminClient.realm(realm.getName()).users().search(null, 0, 10);
        assertEquals(3, search.size());
    }
}
