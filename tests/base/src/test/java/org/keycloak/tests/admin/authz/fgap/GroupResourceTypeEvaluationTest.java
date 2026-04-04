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

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.IMPERSONATE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.IMPERSONATE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class GroupResourceTypeEvaluationTest extends AbstractPermissionTest {

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @InjectUser(ref = "jdoe")
    ManagedUser userJdoe;

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private final String groupName = "top_group";
    private final GroupRepresentation topGroup = new GroupRepresentation();

    @BeforeEach // cannot use @BeforeAll, realm is not initializaed yet
    public void onBefore() {
        topGroup.setName(groupName);
        try (Response response = realm.admin().groups().add(topGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            topGroup.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.groups().group(topGroup.getId()).remove());
        }
        realm.admin().users().get(userAlice.getId()).joinGroup(topGroup.getId());
    }

    @Test
    public void testCanViewUserByViewGroupMembers() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());

        // my admin should NOT be able to see Alice
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertTrue(search.isEmpty());

        // allow my admin to view members of the group where Alice is member of
        createGroupPermission(topGroup, Set.of(VIEW_MEMBERS), allowMyAdminPermission);

        // my admin should be able to see Alice due to her membership and VIEW_MEMBERS permission
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());
    }

    @Test
    public void testCanViewUserByManageGroupMembers() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());

        // my admin should NOT be able to see Alice
        List<UserRepresentation> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertTrue(search.isEmpty());

        // my admin should not be able to manage yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).update(UserConfigBuilder.create().email("email@test.com").build());
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // allow my admin to manage members of the group where Alice is member of
        createGroupPermission(topGroup, Set.of(VIEW_MEMBERS, MANAGE_MEMBERS), allowMyAdminPermission);

        // my admin should be able to see Alice due to her membership and MANAGE_MEMBERS permission
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1);
        assertEquals(1, search.size());
        assertEquals(userAlice.getUsername(), search.get(0).getUsername());

        // my admin should be able to update Alice due to her membership and MANAGE_MEMBERS permission
        realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).update(UserConfigBuilder.create().email("email@test.com").build());
        assertEquals("email@test.com", realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).toRepresentation().getEmail());
    }

    @Test
    public void testManageAllGroups() {
        // myadmin shouldn't be able to create groups just yet
        try (Response response = realmAdminClient.realm(realm.getName()).groups().add(new GroupRepresentation())) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // myadmin shouldn't be able to add child for a group
        try (Response response = realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).subGroup(new GroupRepresentation())) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // myadmin shouldn't be able to map roles for group
        try {
            realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).roles().realmLevel().add(List.of());
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        //create all-groups permission for "myadmin" (so that myadmin can manage all groups in the realm)
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllGroupsPermission(policy, Set.of(VIEW, MANAGE));

        // creating group requires manage scope
        GroupRepresentation group = new GroupRepresentation();
        group.setName("testGroup");
        String testGroupId = ApiUtil.getCreatedId(realmAdminClient.realm(realm.getName()).groups().add(group));
        group.setId(testGroupId);

        // it should be possible to update the group due to fallback to all-groups permission
        group.setName("newGroup");
        realmAdminClient.realm(realm.getName()).groups().group(testGroupId).update(group);
        assertEquals("newGroup", realmAdminClient.realm(realm.getName()).groups().group(testGroupId).toRepresentation().getName());

        // it should be possible to add the child to the group now
        try (Response response = realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).subGroup(group)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // it should be possible to map roles now
        // trying with non existent role as we need to test manage permission for groups (not `auth.roles().requireMapRole(roleModel);`)
        // expecting NotFoundException
        try {
            realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).roles().realmLevel().add(List.of(new RoleRepresentation("non_existent", null, false)));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(NotFoundException.class));
        }
    }

    @Test
    public void testManageGroup() {
        // create group
        GroupRepresentation myGroup = new GroupRepresentation();
        myGroup.setName("my_group");

        try (Response response = realm.admin().groups().add(myGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            myGroup.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.groups().group(myGroup.getId()).remove());
        }

        //create group permission for "myadmin" to manage the myGroup
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createGroupPermission(myGroup, Set.of(VIEW, MANAGE), policy);

        // myadmin shouldn't be able to update the topGroup
        try {
            realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).update(myGroup);
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // it should be possible to update the myGroup
        myGroup.setName("newGroup");
        realmAdminClient.realm(realm.getName()).groups().group(myGroup.getId()).update(myGroup);
        assertEquals("newGroup", realmAdminClient.realm(realm.getName()).groups().group(myGroup.getId()).toRepresentation().getName());

        // it should not be possible to add child to the topGroup
        GroupRepresentation subGroup = new GroupRepresentation();
        subGroup.setName("subGroup");
        try (Response response = realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).subGroup(subGroup)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // it should be possible to add child to the myGroup
        try (Response response = realmAdminClient.realm(realm.getName()).groups().group(myGroup.getId()).subGroup(subGroup)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // it should not be possible to map roles to topGroup
        try {
            realmAdminClient.realm(realm.getName()).groups().group(topGroup.getId()).roles().realmLevel().add(List.of(new RoleRepresentation("non_existent", null, false)));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // it should be possible to map roles to myGroup
        // trying with non existent role as we need to test manage permission for groups (not `auth.roles().requireMapRole(roleModel);`)
        // expecting NotFoundException
        try {
            realmAdminClient.realm(realm.getName()).groups().group(myGroup.getId()).roles().realmLevel().add(List.of(new RoleRepresentation("non_existent", null, false)));
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(NotFoundException.class));
        }
    }

    @Test
    public void testViewGroups() {
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());

        // should not see the groups
        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertThat(search, hasSize(0));

        // create group
        GroupRepresentation myGroup = new GroupRepresentation();
        myGroup.setName("my_group");

        try (Response response = realm.admin().groups().add(myGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            myGroup.setId(ApiUtil.getCreatedId(response));
            realm.cleanup().add(r -> r.groups().group(myGroup.getId()).remove());
        }

        //create permission to view myGroup
        createGroupPermission(myGroup, Set.of(VIEW), policy);

        // myadmin should be able to view only myGroup
        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertThat(search, hasSize(1));
        assertThat(search.get(0).getName(), equalTo(myGroup.getName()));

        // create view all groups permission for myadmin
        createAllGroupsPermission(policy, Set.of(VIEW));

        // now two groups should be returned (myGroup, topGroup)
        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertThat(search, hasSize(2));
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
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllUserPermission(policy, Set.of(MANAGE_GROUP_MEMBERSHIP));

        //create group permission to allow manage membership for the group
        createGroupPermission(topGroup, Set.of(MANAGE_MEMBERSHIP), policy);

        
        //create new user
        String bobId = ApiUtil.getCreatedId(realm.admin().users().create(UserConfigBuilder.create().username("bob").build()));
        realm.cleanup().add(r -> r.users().delete(bobId));

        //check myadmin can manage membership
        realmAdminClient.realm(realm.getName()).users().get(bobId).joinGroup(topGroup.getId());
    }

    @Test
    public void testCreateGroupMembers() {
        //create group permission for "topGroup" to allow "myadmin" view, manage-members and manage-membership
        UserPolicyRepresentation policy = createUserPolicy(realm, client, "Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createGroupPermission(topGroup, Set.of(VIEW, MANAGE_MEMBERSHIP, MANAGE_MEMBERS), policy);
        
        //create new user as realm user should fail
        try (Response response = realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username("bob").build())) {
            assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
        }
        //create new user as member of different group should fail
        try (Response response = realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username("bob").groups("different_group").build())) {
            assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
        }

        String bobId = ApiUtil.getCreatedId(realmAdminClient.realm(realm.getName()).users().create(UserConfigBuilder.create().username("bob").groups("/" + groupName).build()));
        realm.cleanup().add(r -> r.users().delete(bobId));
    }

    @Test
    public void testEvaluateAllResourcePermissionsForSpecificResourcePermission() {
        UserRepresentation adminUser = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowPolicy = createUserPolicy(realm, client, "Only My Admin", adminUser.getId());
        ScopePermissionRepresentation allResourcesPermission = createAllPermission(client, GROUPS_RESOURCE_TYPE, allowPolicy, Set.of(MANAGE, MANAGE_MEMBERSHIP));
        // all resource permissions grants manage scope
        GroupsResource groups = realmAdminClient.realm(realm.getName()).groups();
        groups.group(topGroup.getId()).update(topGroup);

        ScopePermissionRepresentation resourcePermission = createPermission(client, topGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(MANAGE), allowPolicy);
        // both all and specific resource permission grants manage scope
        groups.group(topGroup.getId()).update(topGroup);

        allResourcesPermission = getScopePermissionsResource(client).findByName(allResourcesPermission.getName());
        allResourcesPermission.setScopes(Set.of(MANAGE_MEMBERSHIP));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission does not have the manage scope but the scope is granted by the resource permission
        groups.group(topGroup.getId()).update(topGroup);

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MANAGE_MEMBERSHIP));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // neither the all and specific resource permission grants access to the manage scope
            groups.group(topGroup.getId()).update(topGroup);
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        allResourcesPermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(client).findById(allResourcesPermission.getId()).update(allResourcesPermission);
        // all resource permission grants access again to manage
        groups.group(topGroup.getId()).update(topGroup);

        UserPolicyRepresentation notAllowPolicy = createUserPolicy(Logic.NEGATIVE, realm, client, "Not My Admin", adminUser.getId());
        createPermission(client, topGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(MANAGE), notAllowPolicy);
        try {
            // a specific resource permission that explicitly negates access to the manage scope denies access to the scope
            groups.group(topGroup.getId()).update(topGroup);
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}

        resourcePermission = getScopePermissionsResource(client).findByName(resourcePermission.getName());
        resourcePermission.setScopes(Set.of(MANAGE));
        getScopePermissionsResource(client).findById(resourcePermission.getId()).update(resourcePermission);
        try {
            // the specific resource permission that explicitly negates access to the manage scope denies access to the scope
            // even though there is another resource permission that grants access to the scope - conflict resolution denies by default
            groups.group(topGroup.getId()).update(topGroup);
            fail("Expected Exception wasn't thrown.");
        } catch (ForbiddenException expected) {}
    }

    @Test
    public void testImpersonateMembers() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());

        // my admin should not be able to manage yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        // allow my admin to impersonate members of the group where Alice is member of
        createGroupPermission(topGroup, Set.of(IMPERSONATE_MEMBERS), allowMyAdminPermission);

        realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
        realmAdminClient.tokenManager().logout();
    }

    @Test
    public void testImpersonateMembersFromChildGroups() {
        // my admin should not be able to manage yet
        try {
            realmAdminClient.realm(realm.getName()).users().get(userJdoe.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        }

        GroupRepresentation subGroup = new GroupRepresentation();
        subGroup.setName("testSubGroup");
        String testGroupId = ApiUtil.getCreatedId(realm.admin().groups().add(subGroup));
        subGroup.setId(testGroupId);
        realm.admin().groups().group(topGroup.getId()).subGroup(subGroup).close();
        realm.admin().users().get(userJdoe.getId()).joinGroup(subGroup.getId());
        assertTrue(userJdoe.admin().groups().stream().map(GroupRepresentation::getName).allMatch(subGroup.getName()::equals));

        // allow my admin to impersonate members of the group and its children
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation allowMyAdminPermission = createUserPolicy(realm, client, "Only My Admin User Policy", myadmin.getId());
        createGroupPermission(topGroup, Set.of(IMPERSONATE_MEMBERS), allowMyAdminPermission);

        realmAdminClient.realm(realm.getName()).users().get(userJdoe.getId()).impersonate();
        realmAdminClient.tokenManager().logout();

        UserPolicyRepresentation denyPolicy = createUserPolicy(Logic.NEGATIVE, realm, client, "Deny My Admin User Policy", myadmin.getId());
        createPermission(client, userAlice.getId(), USERS_RESOURCE_TYPE, Set.of(IMPERSONATE), denyPolicy);
        try {
            realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).impersonate();
            fail("Expected Exception wasn't thrown.");
        } catch (Exception ex) {
            assertThat(ex, instanceOf(ForbiddenException.class));
        } finally {
            realmAdminClient.tokenManager().logout();
        }

        realmAdminClient.realm(realm.getName()).users().get(userJdoe.getId()).impersonate();
        realmAdminClient.tokenManager().logout();
    }

    private ScopePermissionRepresentation createAllGroupsPermission(UserPolicyRepresentation policy, Set<String> scopes) {
        return createAllPermission(client, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, policy, scopes);
    }

    private ScopePermissionRepresentation createAllUserPermission(UserPolicyRepresentation policy, Set<String> scopes) {
        return createAllPermission(client, AdminPermissionsSchema.USERS_RESOURCE_TYPE, policy, scopes);
    }
}
