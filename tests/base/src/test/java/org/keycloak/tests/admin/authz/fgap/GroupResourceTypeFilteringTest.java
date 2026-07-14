/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERS;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.MANAGE_MEMBERSHIP;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW_MEMBERS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class GroupResourceTypeFilteringTest extends AbstractPermissionTest {

    @InjectAdminClient(mode = InjectAdminClient.Mode.MANAGED_REALM, client = "myclient", user = "myadmin")
    Keycloak realmAdminClient;

    @InjectUser(ref = "alice")
    ManagedUser userAlice;

    @BeforeEach
    public void onBeforeEach() {
        for (int i = 0; i < 50; i++) {
            GroupRepresentation group = new GroupRepresentation();

            group.setName("group-" + i);

            try (Response response = realm.admin().groups().add(group)) {
                group.setId(ApiUtil.getCreatedId(response));
            }

            GroupResource groupResource = realm.admin().groups().group(group.getId());

            for (int j = 0; j < 5; j++) {
                GroupRepresentation subGroup = new GroupRepresentation();

                subGroup.setName("subgroup-" + i + "." + j);

                groupResource.subGroup(subGroup).close();
            }
        }
    }

    @Test
    public void testVisibleSubGroupSearchReturnsStrippedParent() {
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myadmin.getId());

        GroupRepresentation parentGroup = realm.admin().groups().groups("group-0", -1, -1).get(0);
        parentGroup.setAttributes(Map.of(
                "hidden-parent-attribute", List.of("hidden-parent-value")));
        realm.admin().groups().group(parentGroup.getId()).update(parentGroup);

        GroupRepresentation visibleSubGroup = parentGroup.getSubGroups().stream()
                .filter(group -> group.getName().equals("subgroup-0.1"))
                .findFirst()
                .orElseThrow();
        createPermission(adminPermissionsClient, visibleSubGroup.getId(),
                GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(parentGroup.getId()).toRepresentation());

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName())
                .groups().groups(visibleSubGroup.getName(), -1, -1, false);
        assertEquals(1, search.size());

        GroupRepresentation strippedParent = search.get(0);
        assertEquals(parentGroup.getId(), strippedParent.getId());
        assertEquals(parentGroup.getName(), strippedParent.getName());
        assertNull(strippedParent.getAttributes());
        assertNull(strippedParent.getRealmRoles());
        assertNull(strippedParent.getClientRoles());
        assertNull(strippedParent.getAccess());
        assertEquals(1L, strippedParent.getSubGroupCount());

        assertEquals(1, strippedParent.getSubGroups().size());
        assertEquals(visibleSubGroup.getId(), strippedParent.getSubGroups().get(0).getId());
    }

    @Test
    public void testDeepHierarchyWithMultipleHiddenAncestors() {
        String topId = ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create().name("top-group").build()));
        String midId = ApiUtil.getCreatedId(realm.admin().groups().group(topId).subGroup(GroupBuilder.create().name("mid-group").build()));
        String leafId = ApiUtil.getCreatedId(realm.admin().groups().group(midId).subGroup(GroupBuilder.create().name("leaf-group").build()));
        realm.cleanup().add(r -> r.groups().group(topId).remove());

        GroupRepresentation topGroup = realm.admin().groups().group(topId).toRepresentation();
        topGroup.setAttributes(Map.of("secret-attr", List.of("secret-value")));
        realm.admin().groups().group(topId).update(topGroup);

        GroupRepresentation midGroup = realm.admin().groups().group(midId).toRepresentation();
        midGroup.setAttributes(Map.of("secret-attr", List.of("secret-value")));
        realm.admin().groups().group(midId).update(midGroup);

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, leafId, GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(topId).toRepresentation());
        assertThrows(ForbiddenException.class, () -> realmAdminClient.realm(realm.getName())
                .groups().group(midId).toRepresentation());

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName())
                .groups().groups("leaf-group", -1, -1, false);
        assertEquals(1, search.size());

        GroupRepresentation strippedTop = search.get(0);
        assertEquals(topId, strippedTop.getId());
        assertEquals("top-group", strippedTop.getName());
        assertNull(strippedTop.getAttributes());
        assertNull(strippedTop.getRealmRoles());
        assertNull(strippedTop.getClientRoles());
        assertNull(strippedTop.getAccess());
        assertEquals(1L, strippedTop.getSubGroupCount());

        assertEquals(1, strippedTop.getSubGroups().size());
        GroupRepresentation strippedMid = strippedTop.getSubGroups().get(0);
        assertEquals(midId, strippedMid.getId());
        assertEquals("mid-group", strippedMid.getName());
        assertNull(strippedMid.getAttributes());
        assertNull(strippedMid.getRealmRoles());
        assertNull(strippedMid.getClientRoles());
        assertNull(strippedMid.getAccess());
        assertEquals(1L, strippedMid.getSubGroupCount());

        assertEquals(1, strippedMid.getSubGroups().size());
        assertEquals(leafId, strippedMid.getSubGroups().get(0).getId());
    }

    @Test
    public void testStrippedParentSubGroupCountReflectsOnlyVisibleChildren() {
        String parentId = ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create().name("hidden-parent").build()));
        String hiddenChildId = ApiUtil.getCreatedId(realm.admin().groups().group(parentId).subGroup(GroupBuilder.create().name("hidden-child").build()));
        String visibleChildId = ApiUtil.getCreatedId(realm.admin().groups().group(parentId).subGroup(GroupBuilder.create().name("visible-child").build()));
        realm.cleanup().add(r -> r.groups().group(parentId).remove());

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, visibleChildId, GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName())
                .groups().groups("visible-child", -1, -1, false);
        assertEquals(1, search.size());

        GroupRepresentation strippedParent = search.get(0);
        assertEquals(parentId, strippedParent.getId());
        assertEquals(1L, strippedParent.getSubGroupCount());
        assertEquals(1, strippedParent.getSubGroups().size());
        assertEquals(visibleChildId, strippedParent.getSubGroups().get(0).getId());
    }

    @Test
    public void testStrippedParentSubGroupCountWithMultipleVisibleSiblings() {
        String parentId = ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create().name("hidden-parent").build()));
        String child1Id = ApiUtil.getCreatedId(realm.admin().groups().group(parentId).subGroup(GroupBuilder.create().name("visible-sibling-1").build()));
        String child2Id = ApiUtil.getCreatedId(realm.admin().groups().group(parentId).subGroup(GroupBuilder.create().name("visible-sibling-2").build()));
        String hiddenChildId = ApiUtil.getCreatedId(realm.admin().groups().group(parentId).subGroup(GroupBuilder.create().name("hidden-child").build()));
        realm.cleanup().add(r -> r.groups().group(parentId).remove());

        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,
                "Only My Admin User Policy", myadmin.getId());
        createPermission(adminPermissionsClient, child1Id, GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, child2Id, GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName())
                .groups().groups("visible-sibling", -1, -1, false);
        assertEquals(1, search.size());

        GroupRepresentation strippedParent = search.get(0);
        assertEquals(parentId, strippedParent.getId());
        assertEquals(2L, strippedParent.getSubGroupCount());
        assertEquals(2, strippedParent.getSubGroups().size());

        Set<String> childIds = strippedParent.getSubGroups().stream()
                .map(GroupRepresentation::getId)
                .collect(Collectors.toSet());
        assertTrue(childIds.contains(child1Id));
        assertTrue(childIds.contains(child2Id));
        assertFalse(childIds.contains(hiddenChildId));
    }

    @Test
    @DatabaseTest
    public void testViewAllGroupsUsingUserPolicy() {
        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertTrue(search.isEmpty());

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());
    }

    @Test
    @DatabaseTest
    public void testDeniedResourcesPrecedenceOverGrantedResources() {
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertEquals(50, search.size());

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        Set<String> notAllowedGroups = search.stream()
                .filter((g) -> Set.of("group-0", "group-15", "group-30", "group-45").contains(g.getName()))
                .map(GroupRepresentation::getId)
                .collect(Collectors.toSet());
        assertFalse(notAllowedGroups.isEmpty());
        createPermission(adminPermissionsClient, notAllowedGroups, GROUPS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).groups().groups();
        assertFalse(search.isEmpty());
        assertTrue(search.stream().map(GroupRepresentation::getId).noneMatch(notAllowedGroups::contains));
    }

    @Test
    @DatabaseTest
    public void testFilterSubGroups() {
        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createAllPermission(adminPermissionsClient, GROUPS_RESOURCE_TYPE, policy, Set.of(VIEW));

        List<GroupRepresentation> search = realmAdminClient.realm(realm.getName()).groups().groups("group-0", -1, -1);
        assertFalse(search.isEmpty());
        assertEquals(1, search.size());

        GroupRepresentation parentGroup = search.get(0);
        assertEquals(5, parentGroup.getSubGroups().size());
        assertEquals(5, parentGroup.getSubGroupCount());
        GroupRepresentation subGroup = parentGroup.getSubGroups().stream().filter(group -> group.getName().equals("subgroup-0.0")).findFirst().orElse(null);
        assertNotNull(subGroup);

        UserPolicyRepresentation notMyAdminPolicy = createUserPolicy(Logic.NEGATIVE, realm, adminPermissionsClient,"Not My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, subGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW), notMyAdminPolicy);
        search = realmAdminClient.realm(realm.getName()).groups().groups("subgroup-0.0", -1, -1);
        assertTrue(search.isEmpty());

        List<GroupRepresentation> subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups(-1, -1, false);
        assertEquals(4, subGroups.size());
        assertTrue(subGroups.stream().map(GroupRepresentation::getId).noneMatch(subGroup.getId()::equals));
        search = realmAdminClient.realm(realm.getName()).groups().groups("group-0", -1, -1);
        assertFalse(search.isEmpty());
        parentGroup = search.get(0);
        assertEquals(4, parentGroup.getSubGroups().size());
        assertEquals(4, parentGroup.getSubGroupCount());

        subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups(subGroup.getName(), true, -1, -1, true);
        assertTrue(subGroups.isEmpty());
        subGroups = realmAdminClient.realm(realm.getName()).groups().group(parentGroup.getId()).getSubGroups("subgroup-0.1", true, -1, -1, true);
        assertEquals(1, subGroups.size());

        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).getSubGroups(-1, -1, false).size());
        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).getSubGroups(null, false, -1, -1, false).size());
        assertEquals(5, realm.admin().groups().group(parentGroup.getId()).toRepresentation().getSubGroupCount());
    }

    @Test
    public void testGetUserGroups() {
        GroupRepresentation parentGroup = realm.admin().groups().groups("group-0", -1, -1).get(0);
        GroupRepresentation subGroup = realm.admin().groups().groups("subgroup-1.0", -1, -1).get(0);

        userAlice.admin().joinGroup(parentGroup.getId());
        userAlice.admin().joinGroup(subGroup.getId());

        List<GroupRepresentation> groups = userAlice.admin().groups();
        assertEquals(2, groups.size());

        UserPolicyRepresentation policy = createUserPolicy(realm, adminPermissionsClient,"Only My Admin User Policy", realm.admin().users().search("myadmin").get(0).getId());
        createPermission(adminPermissionsClient, userAlice.getId(), USERS_RESOURCE_TYPE, Set.of(VIEW), policy);
        createPermission(adminPermissionsClient, subGroup.getId(), GROUPS_RESOURCE_TYPE, Set.of(VIEW), policy);

        groups = realmAdminClient.realm(realm.getName()).users().get(userAlice.getId()).groups();
        assertEquals(1, groups.size());

        groups = userAlice.admin().groups();
        assertEquals(2, groups.size());
    }

    @Test
    public void testViewGroupNotAffectQueryUsers() {
        // create groups
        String companyId = ApiUtil.getCreatedId(realm.admin().groups().add(GroupBuilder.create().name("Company").build()));
        String sub1Id = ApiUtil.getCreatedId(realm.admin().groups().group(companyId).subGroup(GroupBuilder.create().name("Sub1").build()));
        String sub2Id = ApiUtil.getCreatedId(realm.admin().groups().group(companyId).subGroup(GroupBuilder.create().name("Sub2").build()));
        String depAId = ApiUtil.getCreatedId(realm.admin().groups().group(sub1Id).subGroup(GroupBuilder.create().name("DepartmentA").build()));
        String depBId = ApiUtil.getCreatedId(realm.admin().groups().group(sub1Id).subGroup(GroupBuilder.create().name("DepartmentB").build()));
        String depCId = ApiUtil.getCreatedId(realm.admin().groups().group(sub2Id).subGroup(GroupBuilder.create().name("DepartmentC").build()));
        String depDId = ApiUtil.getCreatedId(realm.admin().groups().group(sub2Id).subGroup(GroupBuilder.create().name("DepartmentD").build()));
        realm.cleanup().add(r -> r.groups().group(companyId).remove());

        GroupRepresentation company = realm.admin().groups().group(companyId).toRepresentation();
        GroupRepresentation sub1 = realm.admin().groups().group(sub1Id).toRepresentation();
        GroupRepresentation sub2 = realm.admin().groups().group(sub2Id).toRepresentation();
        GroupRepresentation depA = realm.admin().groups().group(depAId).toRepresentation();
        GroupRepresentation depB = realm.admin().groups().group(depBId).toRepresentation();
        GroupRepresentation depC = realm.admin().groups().group(depCId).toRepresentation();
        GroupRepresentation depD = realm.admin().groups().group(depDId).toRepresentation();

        // create members
        List<String> userIds = List.of(
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("company-admin").groups("/Company").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("sub1-admin").groups("/Company/Sub1").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("sub2-admin").groups("/Company/Sub2").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("department-a-member").groups("/Company/Sub1/DepartmentA").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("department-b-member").groups("/Company/Sub1/DepartmentB").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("department-c-member").groups("/Company/Sub2/DepartmentC").build())),
                ApiUtil.getCreatedId(realm.admin().users().create(UserBuilder.create().username("department-d-member").groups("/Company/Sub2/DepartmentD").build()))
        );
        realm.cleanup().add(r -> userIds.forEach(userId -> r.users().delete(userId).close()));

        // add myadmin as member of Sub1
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        realm.admin().users().get(myadmin.getId()).joinGroup(sub1.getId());

        // create policies
        GroupPolicyRepresentation allowCompany = createGroupPolicy(realm, adminPermissionsClient, "Allow Company", Logic.POSITIVE, sub1.getId(), sub2.getId());
        GroupPolicyRepresentation allowSub1 = createGroupPolicy(realm, adminPermissionsClient, "Allow Sub1", Logic.POSITIVE, sub1.getId());
        GroupPolicyRepresentation allowSub2 = createGroupPolicy(realm, adminPermissionsClient, "Allow Sub2", Logic.POSITIVE, sub2.getId());

        // create permissions
        createGroupPermission(Set.of(sub1, depA, depB), Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERS, MANAGE_MEMBERSHIP), allowSub1);
        createGroupPermission(Set.of(sub2, depC, depD), Set.of(VIEW, VIEW_MEMBERS, MANAGE_MEMBERS, MANAGE_MEMBERSHIP), allowSub2);
        ScopePermissionRepresentation companyPermission = createGroupPermission(company, Set.of(VIEW), allowCompany);

        // test listing users
        List<String> search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1).stream().map(UserRepresentation::getUsername).toList();
        assertThat(search, containsInAnyOrder("myadmin", "sub1-admin", "department-a-member", "department-b-member"));

        // grant access to view company members
        companyPermission.setScopes(Set.of(VIEW, VIEW_MEMBERS));
        realm.admin().clients().get(adminPermissionsClient.toRepresentation().getId()).authorization().permissions().scope().findById(companyPermission.getId()).update(companyPermission);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1).stream().map(UserRepresentation::getUsername).toList();
        assertThat(search, containsInAnyOrder("company-admin", "myadmin", "sub1-admin", "department-a-member", "department-b-member"));

        // create negative permission on view-members of company group
        GroupPolicyRepresentation disallowSubAdmins = createGroupPolicy(realm, adminPermissionsClient, "Disallow Subadmins", Logic.NEGATIVE, sub1.getId(), sub2.getId());
        createGroupPermission(company, Set.of(VIEW_MEMBERS), disallowSubAdmins);
        search = realmAdminClient.realm(realm.getName()).users().search(null, -1, -1).stream().map(UserRepresentation::getUsername).toList();
        assertThat(search, containsInAnyOrder("myadmin", "sub1-admin", "department-a-member", "department-b-member"));
    }
}
