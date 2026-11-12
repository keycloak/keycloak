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
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.DELEGATE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.IMPERSONATE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP_OF_MEMBERS;
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

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private GroupRepresentation department;
    private GroupRepresentation team;
    private GroupRepresentation subteam;
    private String myadminId;
    private UserPolicyRepresentation allowMyAdmin;
    private UserPolicyRepresentation denyMyAdmin;

    @BeforeEach
    public void onBefore() {
        department = createGroup("department");
        team = createSubGroup(department, "team");
        subteam = createSubGroup(team, "subteam");
        myadminId = realm.admin().users().search("myadmin").get(0).getId();
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
    public void testExtendChildrenRemovedWhenPermissionTargetsAllGroups() {
        ScopePermissionRepresentation permission = createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);

        // a client unaware of the flag changes the permission to all groups
        permission.setExtendChildren(null);
        permission.setResources(Set.of());
        getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).update(permission);
        assertThat(getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).toRepresentation().getExtendChildren(), nullValue());

        // and later back to a specific group, still without sending the flag
        permission.setResources(Set.of(department.getId()));
        getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).update(permission);
        assertThat(getScopePermissionsResource(adminPermissionsClient).findById(permission.getId()).toRepresentation().getExtendChildren(), nullValue());

        // the permission is not extended to the descendants again
        assertCanView(department);
        assertCannotView(team);
        assertCannotView(subteam);
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
    public void testExtendChildrenRequiresSpecificGroupsWhenReferencedById() {
        // the resource for all groups can also be referenced by the id of its authorization resource
        String allGroupsResourceId = adminPermissionsClient.authorization().resources().findByName(GROUPS_RESOURCE_TYPE).get(0).getId();
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(GROUPS_RESOURCE_TYPE);
        permission.setScopes(Set.of(VIEW));
        permission.setResources(Set.of(department.getId(), allGroupsResourceId));
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
    public void testSearchIncludesDescendantsOfPermissionDenyingAnotherAdmin() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        // the permission only denies another admin, so it grants myadmin even if its policy does not reference it
        createGroupPermission(team, Set.of(VIEW), true, denyAnotherAdmin());

        assertCanView(subteam);
        assertThat(searchGroupIds(subteam.getName()), hasItem(subteam.getId()));
    }

    @Test
    public void testUserSearchIncludesMembersOfDescendantsOfAncestorDenyingAnotherAdmin() {
        createGroupPermission(department, Set.of(VIEW_MEMBERS), false, denyAnotherAdmin());
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);

        assertCanView(member);
        assertThat(searchUserIds(member), contains(member.getId()));
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
    public void testUserDeniedByNonExtendedMemberScopeOfAncestorOfExtendedGroup() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        // the deny is on an ancestor of the extended group rather than on one of its descendants
        createGroupPermission(department, Set.of(VIEW_MEMBERS), false, denyMyAdmin);
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation teamMember = createMember("team-member", team);
        UserRepresentation member = createMember("subteam-member", subteam);

        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
        assertThrows(ForbiddenException.class, () -> myGroup(subteam).members());

        // the own permission of the team takes precedence over the deny of its ancestor for its direct members
        assertCanView(teamMember);
        assertThat(searchUserIds(teamMember), contains(teamMember.getId()));
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

    // --- policy types ---

    @Test
    public void testInheritedPermissionWithGroupPolicy() {
        GroupRepresentation admins = createGroup("admins");
        realm.admin().users().get(myadminId).joinGroup(admins.getId());
        GroupPolicyRepresentation allowAdmins = createGroupPolicy(realm, adminPermissionsClient, "Allow Admins", Logic.POSITIVE, admins.getId());
        createGroupPermission(department, Set.of(VIEW), true, allowAdmins);

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedPermissionWithNegativeGroupPolicyForAnotherGroup() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        GroupRepresentation others = createGroup("others");
        // the policy denies the members of a group myadmin is not a member of, so it grants myadmin. The group policy
        // is skipped by partial evaluation for non-members, so search falls back to the permission for all groups
        // instead of hiding the descendants
        GroupPolicyRepresentation denyOthers = createGroupPolicy(realm, adminPermissionsClient, "Deny Others", Logic.NEGATIVE, others.getId());
        createGroupPermission(team, Set.of(VIEW), true, denyOthers);

        assertViewConsistent(team, true);
        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedMemberScopeWithNegativeGroupPolicyForAnotherGroupOnAncestor() {
        GroupRepresentation others = createGroup("others");
        GroupPolicyRepresentation denyOthers = createGroupPolicy(realm, adminPermissionsClient, "Deny Others", Logic.NEGATIVE, others.getId());
        // the permission on the ancestor can not be decided by partial evaluation, so it does not block the extended grant
        createGroupPermission(department, Set.of(VIEW_MEMBERS), false, denyOthers);
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);

        assertCanView(member);
        assertThat(searchUserIds(member), contains(member.getId()));
    }

    @Test
    public void testInheritedPermissionWithRolePolicy() {
        ClientRepresentation realmManagement = realm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        String queryUsersRoleId = realm.admin().clients().get(realmManagement.getId()).roles().get(AdminRoles.QUERY_USERS).toRepresentation().getId();
        RolePolicyRepresentation allowQueryUsers = createRolePolicy("Allow Query Users", queryUsersRoleId, Logic.POSITIVE);
        createGroupPermission(department, Set.of(VIEW), true, allowQueryUsers);

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedPermissionWithNegativeRolePolicyForAnotherRole() {
        realm.admin().roles().create(new RoleRepresentation("other-role", null, false));
        String otherRoleId = realm.admin().roles().get("other-role").toRepresentation().getId();
        // the policy denies the holders of a role myadmin does not have, so it grants myadmin
        RolePolicyRepresentation denyOtherRole = createRolePolicy("Deny Other Role", otherRoleId, Logic.NEGATIVE);
        createGroupPermission(department, Set.of(VIEW), true, denyOtherRole);

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedPermissionWithAggregatedPolicy() {
        GroupRepresentation admins = createGroup("admins");
        realm.admin().users().get(myadminId).joinGroup(admins.getId());
        GroupPolicyRepresentation allowAdmins = createGroupPolicy(realm, adminPermissionsClient, "Allow Admins", Logic.POSITIVE, admins.getId());
        AggregatePolicyRepresentation allowBoth = createAggregatedPolicy(adminPermissionsClient, "Allow Both", Logic.POSITIVE, DecisionStrategy.UNANIMOUS, allowMyAdmin.getName(), allowAdmins.getName());
        createGroupPermission(department, Set.of(VIEW), true, allowBoth);

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedPermissionWithNegativeAggregatedPolicyForAnotherAdmin() {
        UserRepresentation anotherAdmin = createMember("another-admin");
        UserPolicyRepresentation allowAnotherAdmin = createUserPolicy(realm, adminPermissionsClient, "Allow Another Admin", anotherAdmin.getId());
        // the aggregated policy denies another admin only, so it grants myadmin
        AggregatePolicyRepresentation denyAnotherAdmin = createAggregatedPolicy(adminPermissionsClient, "Deny Another Admin Aggregated", Logic.NEGATIVE, DecisionStrategy.UNANIMOUS, allowAnotherAdmin.getName());
        createGroupPermission(department, Set.of(VIEW), true, denyAnotherAdmin);

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testInheritedDenyWithAggregatedPolicy() {
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        AggregatePolicyRepresentation denyAggregated = createAggregatedPolicy(adminPermissionsClient, "Deny My Admin Aggregated", Logic.NEGATIVE, DecisionStrategy.UNANIMOUS, allowMyAdmin.getName());
        createGroupPermission(team, Set.of(VIEW), true, denyAggregated);

        assertViewConsistent(department, true);
        assertViewConsistent(team, false);
        assertViewConsistent(subteam, false);
    }

    // --- several extended permissions on the same group ---

    @Test
    public void testAllExtendedPermissionsOfGroupMustGrant() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        createGroupPermission(department, Set.of(VIEW), true, denyMyAdmin);

        assertViewConsistent(department, false);
        assertViewConsistent(team, false);
        assertViewConsistent(subteam, false);
    }

    @Test
    public void testSeveralGrantingExtendedPermissionsOfGroup() {
        GroupRepresentation admins = createGroup("admins");
        realm.admin().users().get(myadminId).joinGroup(admins.getId());
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        createGroupPermission(department, Set.of(VIEW), true, createGroupPolicy(realm, adminPermissionsClient, "Allow Admins", Logic.POSITIVE, admins.getId()));

        assertViewConsistent(subteam, true);
    }

    @Test
    public void testAllExtendedMemberScopePermissionsOfGroupMustGrant() {
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, denyMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);

        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
    }

    // --- moving groups ---

    @Test
    public void testGroupMovedOutOfExtendedHierarchyLosesInheritedPermission() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        assertViewConsistent(subteam, true);

        moveToTopLevel(subteam);

        assertViewConsistent(subteam, false);
    }

    @Test
    public void testGroupMovedIntoExtendedHierarchyInheritsPermission() {
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        GroupRepresentation outsider = createGroup("outsider");
        GroupRepresentation outsiderChild = createSubGroup(outsider, "outsider-child");
        assertViewConsistent(outsider, false);

        moveUnder(team, outsider);

        assertViewConsistent(outsider, true);
        assertViewConsistent(outsiderChild, true);
    }

    @Test
    public void testMembersOfGroupMovedIntoExtendedHierarchyInheritMemberScopes() {
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        GroupRepresentation outsider = createGroup("outsider");
        UserRepresentation member = createMember("outsider-member", outsider);
        assertCannotView(member);

        moveUnder(team, outsider);

        assertCanView(member);
        assertThat(searchUserIds(member), contains(member.getId()));

        moveToTopLevel(outsider);

        assertCannotView(member);
        assertThat(searchUserIds(member), empty());
    }

    // --- count ---

    @Test
    public void testUserCountIncludesMembersOfDescendants() {
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        assertThat(myRealm().users().count(member.getUsername()), is(1));
        assertThat(myRealm().users().count(outsider.getUsername()), is(0));
    }

    @Test
    public void testUserCountExcludesMembersOfDescendantsWithInheritedDeny() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(VIEW));
        createGroupPermission(team, Set.of(VIEW_MEMBERS), true, denyMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        assertThat(myRealm().users().count(member.getUsername()), is(0));
        assertThat(myRealm().users().count(outsider.getUsername()), is(1));
    }

    // --- other scopes ---

    @Test
    public void testManageMembershipIsInherited() {
        createAllPermission(adminPermissionsClient, USERS_RESOURCE_TYPE, allowMyAdmin, Set.of(MANAGE_GROUP_MEMBERSHIP));
        createGroupPermission(department, Set.of(MANAGE_MEMBERSHIP), true, allowMyAdmin);
        GroupRepresentation other = createGroup("other");
        UserRepresentation user = createMember("joining-user");

        myRealm().users().get(user.getId()).joinGroup(subteam.getId());
        assertThat(realm.admin().users().get(user.getId()).groups().stream().map(GroupRepresentation::getId).toList(), contains(subteam.getId()));
        myRealm().users().get(user.getId()).leaveGroup(subteam.getId());

        assertThrows(ForbiddenException.class, () -> myRealm().users().get(user.getId()).joinGroup(other.getId()));
    }

    @Test
    public void testManageMembershipOfMembersIsInherited() {
        GroupRepresentation other = createGroup("other");
        createGroupPermission(other, Set.of(MANAGE_MEMBERSHIP), false, allowMyAdmin);
        createGroupPermission(department, Set.of(MANAGE_MEMBERSHIP_OF_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        myRealm().users().get(member.getId()).joinGroup(other.getId());
        assertThat(realm.admin().users().get(member.getId()).groups().stream().map(GroupRepresentation::getId).toList(), hasItem(other.getId()));

        assertThrows(ForbiddenException.class, () -> myRealm().users().get(outsider.getId()).joinGroup(other.getId()));
    }

    @Test
    public void testImpersonateMembersIsInherited() {
        createGroupPermission(department, Set.of(IMPERSONATE_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");

        assertThrows(ForbiddenException.class, () -> myRealm().users().get(outsider.getId()).impersonate());

        // impersonating invalidates the session of the admin client, so a separate client is used
        try (Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl(keycloakUrls.getBase())
                .realm(realm.getName())
                .clientId("myclient")
                .clientSecret("mysecret")
                .username("myadmin")
                .password("password")
                .build()) {
            adminClient.realm(realm.getName()).users().get(member.getId()).impersonate();
        }
    }

    @Test
    public void testDelegateMembersIsInherited() {
        createGroupPermission(department, Set.of(DELEGATE_MEMBERS), true, allowMyAdmin);
        createGroupPermission(team, Set.of(DELEGATE_MEMBERS), true, denyMyAdmin);
        UserRepresentation teamMember = createMember("team-member", team);
        UserRepresentation member = createMember("subteam-member", subteam);
        UserRepresentation outsider = createMember("outsider");
        GroupRepresentation sibling = createSubGroup(department, "sibling");
        UserRepresentation siblingMember = createMember("sibling-member", sibling);

        assertThat(canDelegate(siblingMember), is(true));
        assertThat(canDelegate(teamMember), is(false));
        assertThat(canDelegate(member), is(false));
        assertThat(canDelegate(outsider), is(false));
    }

    // --- disabling and removing ---

    @Test
    public void testDisablingExtendChildrenRemovesInheritedAccess() {
        ScopePermissionRepresentation permission = createGroupPermission(department, Set.of(VIEW, VIEW_MEMBERS), true, allowMyAdmin);
        UserRepresentation member = createMember("subteam-member", subteam);
        assertViewConsistent(subteam, true);
        assertCanView(member);

        ScopePermissionRepresentation rep = permission;
        rep.setId(getScopePermissionsResource(adminPermissionsClient).findByName(permission.getName()).getId());
        rep.setExtendChildren(false);
        getScopePermissionsResource(adminPermissionsClient).findById(rep.getId()).update(rep);

        assertViewConsistent(department, true);
        assertViewConsistent(subteam, false);
        assertThat(searchUserIds(member), empty());

        rep.setExtendChildren(true);
        getScopePermissionsResource(adminPermissionsClient).findById(rep.getId()).update(rep);

        assertViewConsistent(subteam, true);
        assertThat(searchUserIds(member), contains(member.getId()));
    }

    @Test
    public void testRemovingExtendedPermissionRemovesInheritedAccess() {
        ScopePermissionRepresentation permission = createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        assertViewConsistent(subteam, true);

        String id = getScopePermissionsResource(adminPermissionsClient).findByName(permission.getName()).getId();
        getScopePermissionsResource(adminPermissionsClient).findById(id).remove();

        assertViewConsistent(subteam, false);
    }

    // --- one permission for several groups ---

    @Test
    public void testExtendedPermissionForGroupsInDifferentHierarchies() {
        GroupRepresentation other = createGroup("other");
        GroupRepresentation otherChild = createSubGroup(other, "other-child");
        createGroupPermission(Set.of(team, other), Set.of(VIEW), true, allowMyAdmin);

        assertViewConsistent(department, false);
        assertViewConsistent(subteam, true);
        assertViewConsistent(otherChild, true);
    }

    @Test
    public void testExtendedPermissionForGroupAndDescendantOfSameHierarchy() {
        // the permission is the own permission of both groups, so the deny on team in between does not apply to subteam
        createGroupPermission(Set.of(department, subteam), Set.of(VIEW), true, allowMyAdmin);
        createGroupPermission(team, Set.of(VIEW), false, denyMyAdmin);
        GroupRepresentation deep = createSubGroup(subteam, "deep");

        assertViewConsistent(department, true);
        assertViewConsistent(team, false);
        assertViewConsistent(subteam, true);
        assertViewConsistent(deep, true);
    }

    // --- deeper hierarchies and siblings ---

    @Test
    public void testDeepHierarchyWithSiblings() {
        GroupRepresentation level4 = createSubGroup(subteam, "level4");
        GroupRepresentation level5 = createSubGroup(level4, "level5");
        GroupRepresentation sibling = createSubGroup(department, "sibling");
        GroupRepresentation siblingChild = createSubGroup(sibling, "sibling-child");
        GroupRepresentation subteamSibling = createSubGroup(team, "subteam-sibling");
        createGroupPermission(department, Set.of(VIEW), true, allowMyAdmin);
        createGroupPermission(subteam, Set.of(VIEW), true, denyMyAdmin);

        assertViewConsistent(team, true);
        assertViewConsistent(subteamSibling, true);
        assertViewConsistent(sibling, true);
        assertViewConsistent(siblingChild, true);
        assertViewConsistent(subteam, false);
        assertViewConsistent(level4, false);
        assertViewConsistent(level5, false);
    }

    @Test
    public void testDeepHierarchyMemberScopes() {
        GroupRepresentation level4 = createSubGroup(subteam, "level4");
        GroupRepresentation level5 = createSubGroup(level4, "level5");
        GroupRepresentation sibling = createSubGroup(department, "sibling");
        createGroupPermission(department, Set.of(VIEW_MEMBERS), true, allowMyAdmin);
        createGroupPermission(level4, Set.of(VIEW_MEMBERS), false, denyMyAdmin);
        UserRepresentation deepMember = createMember("level5-member", level5);
        UserRepresentation subteamMember = createMember("subteam-member", subteam);
        UserRepresentation siblingMember = createMember("sibling-member", sibling);

        assertCannotView(deepMember);
        assertThat(searchUserIds(deepMember), empty());
        assertCanView(subteamMember);
        assertThat(searchUserIds(subteamMember), contains(subteamMember.getId()));
        assertCanView(siblingMember);
        assertThat(searchUserIds(siblingMember), contains(siblingMember.getId()));
    }

    private ScopePermissionRepresentation createGroupPermission(GroupRepresentation group, Set<String> scopes, boolean extendChildren, AbstractPolicyRepresentation policy) {
        return createGroupPermission(Set.of(group), scopes, extendChildren, policy);
    }

    private ScopePermissionRepresentation createGroupPermission(Set<GroupRepresentation> groups, Set<String> scopes, boolean extendChildren, AbstractPolicyRepresentation policy) {
        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(KeycloakModelUtils.generateId());
        permission.setResourceType(GROUPS_RESOURCE_TYPE);
        permission.setScopes(scopes);
        permission.setResources(groups.stream().map(GroupRepresentation::getId).collect(Collectors.toSet()));
        permission.setExtendChildren(extendChildren);
        permission.addPolicy(policy.getName());

        createPermission(adminPermissionsClient, permission);

        return permission;
    }

    private RolePolicyRepresentation createRolePolicy(String name, String roleId, Logic logic) {
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName(name);
        policy.addRole(roleId);
        policy.setLogic(logic);

        try (Response response = adminPermissionsClient.authorization().policies().role().create(policy)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }

        return policy;
    }

    private void moveUnder(GroupRepresentation parent, GroupRepresentation group) {
        try (Response response = realm.admin().groups().group(parent.getId()).subGroup(realm.admin().groups().group(group.getId()).toRepresentation())) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }
    }

    private void moveToTopLevel(GroupRepresentation group) {
        try (Response response = realm.admin().groups().add(realm.admin().groups().group(group.getId()).toRepresentation())) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }
    }

    private boolean canDelegate(UserRepresentation user) {
        return runOnServer.fetch(DelegateCheck.canDelegate(realm.getName(), myadminId, user.getId()), Boolean.class);
    }

    /**
     * Holds the check run on the server, separately from the test class which references classes that are not available
     * on the server.
     */
    static final class DelegateCheck {

        static FetchOnServer canDelegate(String realmName, String adminId, String userId) {
            return session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                session.getContext().setRealm(realmModel);
                UserModel admin = session.users().getUserById(realmModel, adminId);
                UserModel target = session.users().getUserById(realmModel, userId);
                return AdminPermissions.evaluator(session, realmModel, realmModel, admin).users().canDelegate(target);
            };
        }
    }

    /**
     * Asserts that direct access to the {@code group} and an exact group search agree on whether the admin can view it.
     */
    private void assertViewConsistent(GroupRepresentation group, boolean expected) {
        if (expected) {
            assertCanView(group);
        } else {
            assertCannotView(group);
        }

        Set<String> ids = new HashSet<>();
        collectIds(myRealm().groups().groups(group.getName(), true, -1, -1, false), ids);
        assertThat("search result for " + group.getName(), ids.contains(group.getId()), is(expected));
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

    private UserPolicyRepresentation denyAnotherAdmin() {
        UserRepresentation anotherAdmin = createMember("another-admin");
        return createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient, "Deny Another Admin", anotherAdmin.getId());
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
