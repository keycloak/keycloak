/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for group permissions that extend to children groups (see <a href="https://github.com/keycloak/keycloak/issues/43865">#43865</a>).
 *
 * <p>Uses the hierarchy {@code department > team > subteam}.
 */
@KeycloakIntegrationTest
public class GroupResourceTypeExtendChildrenTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    private GroupRepresentation department;
    private GroupRepresentation team;
    private GroupRepresentation subteam;
    private UserPolicyRepresentation allowMyAdmin;
    private UserPolicyRepresentation denyMyAdmin;

    @BeforeEach
    public void onBefore() {
        department = createGroup("department");
        team = createSubGroup(department, "team");
        subteam = createSubGroup(team, "subteam");
        String myadminId = realm.admin().users().search("myadmin").get(0).getId();
        allowMyAdmin = createUserPolicy(realm, adminPermissionsClient, "Allow My Admin", myadminId);
        denyMyAdmin = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient, "Deny My Admin", myadminId);
    }

    @Test
    public void testExtendChildrenIsPersisted() {
        ScopePermissionRepresentation permission = createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        assertThat(getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).toRepresentation().getExtendChildren(), is(true));

        // not sending the flag on update keeps it
        permission.setExtendChildren(null);
        permission.setDescription("updated");
        getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).update(permission);
        assertThat(getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).toRepresentation().getExtendChildren(), is(true));

        // explicitly disabling it removes it
        permission.setExtendChildren(false);
        getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).update(permission);
        assertThat(getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).toRepresentation().getExtendChildren(), nullValue());
    }

    @Test
    public void testExtendChildrenOnlySupportedForGroups() {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(CLIENTS_RESOURCE_TYPE);
        permission.setScopes(Set.of(VIEW));
        permission.setExtendChildren(true);
        permission.addPolicy(allowMyAdmin.getName());

        createPermission(adminPermissionsClient, permission, Response.Status.BAD_REQUEST);
    }

    @Test
    public void testExtendChildrenRequiresSpecificGroups() {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(GROUPS_RESOURCE_TYPE);
        permission.setScopes(Set.of(VIEW));
        permission.setExtendChildren(true);
        permission.addPolicy(allowMyAdmin.getName());

        createPermission(adminPermissionsClient, permission, Response.Status.BAD_REQUEST);
    }

    @Test
    public void testExtendChildrenNotSupportedOutsideAdminPermissions() {
        ClientResource resourceServer = AdminApiUtil.findClientByClientId(realm.admin(), "myresourceserver");
        ScopeRepresentation scope = new ScopeRepresentation("read");

        try (Response response = resourceServer.authorization().scopes().create(scope)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setScopes(Set.of(scope.getName()));
        permission.setExtendChildren(true);

        createPermission(resourceServer, permission, Response.Status.BAD_REQUEST);
    }

    @Test
    public void testPermissionNotExtendedByDefault() {
        createGroupPermission(department, Set.of(VIEW, MANAGE), false, allowMyAdmin);

        assertCanView(department);
        assertCannotView(team);
        assertCannotView(subteam);
    }

    @Test
    public void testPermissionExtendedToDescendants() {
        createGroupPermission(department, Set.of(VIEW, MANAGE), true, allowMyAdmin);

        assertCanView(department);
        assertCanView(team);
        assertCanView(subteam);
        assertCanManage(subteam);
    }

    @Test
    public void testOnlyExtendedScopesAreInherited() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);

        assertCanView(subteam);
        assertCannotManage(subteam);
    }

    @Test
    public void testDelegatedAdminCanManageSubGroupItCreates() {
        createGroupPermission(department, Set.of(VIEW, MANAGE), true, allowMyAdmin);

        GroupRepresentation newGroup = new GroupRepresentation();
        newGroup.setName("created-by-delegated-admin");

        try (Response response = myRealm().groups().group(team.getId()).subGroup(newGroup)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            newGroup.setId(ApiUtil.getCreatedId(response));
        }

        assertCanManage(newGroup);
    }

    @Test
    public void testChildPermissionTakesPrecedenceOverInherited() {
        createGroupPermission(department, Set.of(VIEW, MANAGE), true, allowMyAdmin);
        // the team has a permission of its own for view
        createGroupPermission(team, Set.of(VIEW), false, denyMyAdmin);

        assertCannotView(team);
        // the team permission is not extended, so the subteam still inherits from the department
        assertCanView(subteam);
        // manage is not claimed by the team, so it is still inherited
        assertCanManage(team);
    }

    @Test
    public void testInheritedPermissionDoesNotDecideScopesOfChildPermission() {
        // the department permission covers more scopes than the team permission
        createGroupPermission(department, Set.of(VIEW, MANAGE), true, denyMyAdmin);
        createGroupPermission(team, Set.of(MANAGE), false, allowMyAdmin);

        // manage is decided by the team permission only
        assertCanManage(team);
        // view is not covered by the team permission, so it is inherited
        assertCannotView(team);
    }

    @Test
    public void testDistantAncestorDoesNotDecideScopesOfCloserAncestor() {
        createGroupPermission(department, Set.of(VIEW, MANAGE), true, denyMyAdmin);
        createGroupPermission(team, Set.of(MANAGE), true, allowMyAdmin);

        // manage is inherited from the team, the closest extended ancestor for that scope
        assertCanManage(subteam);
        // view is only covered by the department
        assertCannotView(subteam);
    }

    @Test
    public void testMemberScopeDeniedByAnyExtendedAncestor() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, denyMyAdmin);
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, allowMyAdmin);

        // the team permission is its own, so it decides
        myGroup(team).members();
        // for member scopes, a deny from any extended ancestor wins over a closer grant
        assertThrows(ForbiddenException.class, () -> myGroup(subteam).members());
    }

    @Test
    public void testClosestExtendedAncestorTakesPrecedence() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        createGroupPermission(team, Set.of(VIEW), true, denyMyAdmin);

        assertCanView(department);
        assertCannotView(team);
        assertCannotView(subteam);
    }

    @Test
    public void testInheritedDenyOverridesAllGroupsGrant() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(team, Set.of(VIEW), true, denyMyAdmin);

        assertCanView(department);
        assertCannotView(team);
        assertCannotView(subteam);
    }

    @Test
    public void testInheritedGrantOverridesAllGroupsDeny() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, denyMyAdmin, Set.of(VIEW));
        createGroupPermission(team, Set.of(VIEW), true, allowMyAdmin);

        assertCannotView(department);
        assertCanView(team);
        assertCanView(subteam);
    }

    @Test
    public void testSearchIncludesDescendants() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        assertThat(searchGroupIds(subteam.getName()), hasItem(subteam.getId()));

        GroupRepresentation other = createGroup("other");
        assertThat(searchGroupIds(other.getName()), not(hasItem(other.getId())));
    }

    @Test
    public void testSearchExcludesDescendantsWithInheritedDeny() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(team, Set.of(VIEW), true, denyMyAdmin);

        assertThat(searchGroupIds(department.getName()), hasItem(department.getId()));
        assertThat(searchGroupIds(subteam.getName()), not(hasItem(subteam.getId())));
    }

    @Test
    public void testUserSearchIncludesMembersOfDescendants() {
        UserRepresentation member = createMember("subteam-member", subteam);

        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);

        assertThat(searchUserIds(member), contains(member.getId()));
    }

    @Test
    public void testUserInheritsMemberScopesFromExtendedAncestor() {
        createGroupPermission(department, Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        // direct checks, search and the group members agree
        assertCanView(member);
        assertCanManage(member);
        assertThat(searchUserIds(member), contains(member.getId()));
        assertThat(myGroup(subteam).members().stream().map(UserRepresentation::getId).toList(), contains(member.getId()));

        assertCannotView(outsider);
        assertThat(searchUserIds(outsider), empty());
    }

    @Test
    public void testUserDeniedByAnyExtendedAncestorMemberScope() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, denyMyAdmin);
        // a closer extended grant does not override a deny from a farther ancestor for member scopes
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
        assertThrows(ForbiddenException.class, () -> myGroup(subteam).members());

        // users outside the hierarchy are only decided by the permission for all users
        assertCanView(outsider);
        assertThat(searchUserIds(outsider), contains(outsider.getId()));
    }

    @Test
    public void testUserDeniedByNonExtendedMemberScopeOfIntermediateGroup() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        // member scopes of a group already protect the members of its subgroups, with or without extend children
        createGroupPermission(team, Set.of(VIEW_MEMBERS), false, denyMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);

        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
        assertThrows(ForbiddenException.class, () -> myGroup(subteam).members());
    }

    @Test
    public void testUserInSeveralGroupsDeniedByInheritedMemberScope() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        GroupRepresentation other = createGroup("other");
        createGroupPermission(other, Set.of(VIEW_MEMBERS), false, allowMyAdmin);
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, denyMyAdmin);
        UserRepresentation member = createMember("subteam-and-other-member", subteam, other);

        // a grant from another group of the user does not override the inherited deny
        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
    }

    @Test
    public void testUserInheritingThroughSeveralGroupsIsGrantedOnce() {
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("team-and-subteam-member", team, subteam);

        assertCanView(member);
        // the user is a member of two groups inheriting the same permission, but is only listed once
        assertThat(searchUserIds(member), contains(member.getId()));
    }

    private ScopePermissionRepresentation createGroupPermission(GroupRepresentation group, Set<String> scopes, boolean extendChildren, UserPolicyRepresentation policy) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(GROUPS_RESOURCE_TYPE);
        permission.setScopes(scopes);
        permission.setResources(Set.of(group.getId()));
        permission.setExtendChildren(extendChildren);
        permission.addPolicy(policy.getName());

        createPermission(adminPermissionsClient, permission);

        return permission;
    }

    private GroupRepresentation createSubGroup(GroupRepresentation parent, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);

        try (Response response = realm.admin().groups().group(parent.getId()).subGroup(group)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            group.setId(ApiUtil.getCreatedId(response));
        }

        return group;
    }

    private RealmResource myRealm() {
        return realmAdminClient.realm(realm.getName());
    }

    private GroupResource myGroup(GroupRepresentation group) {
        return myRealm().groups().group(group.getId());
    }

    private void assertCanView(GroupRepresentation group) {
        assertThat(myGroup(group).toRepresentation().getId(), equalTo(group.getId()));
    }

    private void assertCannotView(GroupRepresentation group) {
        assertThrows(ForbiddenException.class, () -> myGroup(group).toRepresentation());
    }

    private void assertCanManage(GroupRepresentation group) {
        GroupRepresentation rep = realm.admin().groups().group(group.getId()).toRepresentation();
        rep.setDescription("updated by delegated admin");
        myGroup(group).update(rep);
    }

    private void assertCannotManage(GroupRepresentation group) {
        GroupRepresentation rep = realm.admin().groups().group(group.getId()).toRepresentation();
        rep.setDescription("updated by delegated admin");
        assertThrows(ForbiddenException.class, () -> myGroup(group).update(rep));
    }

    private UserRepresentation createMember(String username, GroupRepresentation... groups) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);

        try (Response response = realm.admin().users().create(user)) {
            user.setId(ApiUtil.getCreatedId(response));
        }

        realm.cleanup().add(r -> r.users().delete(user.getId()).close());

        for (GroupRepresentation group : groups) {
            realm.admin().users().get(user.getId()).joinGroup(group.getId());
        }

        return user;
    }

    private void assertCanView(UserRepresentation user) {
        assertThat(myRealm().users().get(user.getId()).toRepresentation().getId(), equalTo(user.getId()));
    }

    private void assertCannotView(UserRepresentation user) {
        assertThrows(ForbiddenException.class, () -> myRealm().users().get(user.getId()).toRepresentation());
    }

    private void assertCanManage(UserRepresentation user) {
        UserRepresentation rep = realm.admin().users().get(user.getId()).toRepresentation();
        rep.setFirstName("updated by delegated admin");
        myRealm().users().get(user.getId()).update(rep);
    }

    private List<String> searchUserIds(UserRepresentation user) {
        return myRealm().users().search(user.getUsername(), true).stream().map(UserRepresentation::getId).toList();
    }

    private Set<String> searchGroupIds(String search) {
        Set<String> ids = new HashSet<>();
        collectIds(myRealm().groups().groups(search, -1, -1, false), ids);
        return ids;
    }

    private void collectIds(List<GroupRepresentation> groups, Set<String> ids) {
        if (groups == null) {
            return;
        }
        for (GroupRepresentation group : groups) {
            ids.add(group.getId());
            collectIds(group.getSubGroups(), ids);
        }
    }
}
