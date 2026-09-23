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

package org.keycloak.tests.organization.group;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationGroupsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedOrganization;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class OrganizationGroupsTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectOrganization
    ManagedOrganization organization;

    @InjectOrganization(ref = "other")
    ManagedOrganization otherOrganization;

    @InjectUser(config = MemberConfig.class)
    ManagedUser member;

    @InjectUser(ref = "non-member", config = NonMemberConfig.class)
    ManagedUser nonMember;

    @Test
    public void testCreateOrganizationGroup() {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");
        groupRep.singleAttribute("department", "Engineering");
        createGroup(organization, groupRep);

        List<GroupRepresentation> groups = groups().getAll(null, null, null, null, null, false, false);
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getName(), is("test-group"));
        assertThat(groups.get(0).getPath(), is("/test-group"));
        assertThat(groups.get(0).getAttributes(), notNullValue());
        assertThat(groups.get(0).getAttributes().get("department"), hasSize(1));
        assertThat(groups.get(0).getAttributes().get("department").get(0), is("Engineering"));

        // retrieve brief rep
        groups = groups().getAll(null, null, null, null, null, true, false);
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getAttributes(), nullValue());
    }

    @Test
    public void testCreateDuplicateGroupName() {
        createGroup("duplicate-group");

        // Try to create another group with the same name
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("duplicate-group");

        try (Response response = groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CONFLICT.getStatusCode()));
        }
    }

    @Test
    public void testGetOrganizationGroup() {
        String groupId = createGroup("test-group");

        GroupRepresentation retrieved = groups().group(groupId).toRepresentation(false);
        assertNotNull(retrieved);
        assertThat(retrieved.getName(), is("test-group"));
        assertThat(retrieved.getPath(), is("/test-group"));
    }

    @Test
    public void testOrgGroupAttributes() {
        // Create group with attributes
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");
        groupRep.setAttributes(new HashMap<>());
        groupRep.getAttributes().put("department", List.of("Engineering"));
        groupRep.getAttributes().put("location", List.of("NYC", "SF"));
        String groupId = createGroup(organization, groupRep);

        // Retrieve and verify attributes are included
        GroupRepresentation retrieved = groups().group(groupId).toRepresentation(false);
        assertNotNull(retrieved.getAttributes());
        assertThat(retrieved.getAttributes().get("department"), hasSize(1));
        assertThat(retrieved.getAttributes().get("department").get(0), is("Engineering"));
        assertThat(retrieved.getAttributes().get("location"), hasSize(2));
    }

    @Test
    public void testOrgGroupRepresentationIncludesEmptyRoleMappings() {
        String groupId = createGroup("test-group");

        GroupRepresentation retrieved = groups().group(groupId).toRepresentation(false);
        assertThat(retrieved, notNullValue());
        assertThat(retrieved.getRealmRoles(), notNullValue());
        assertThat(retrieved.getRealmRoles(), hasSize(0));
        assertThat(retrieved.getClientRoles(), notNullValue());
        assertThat(retrieved.getClientRoles().size(), is(0));
    }

    @Test
    public void testUpdateOrganizationGroup() {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("original-name");
        groupRep.setDescription("Original description");
        String groupId = createGroup(organization, groupRep);

        // Update the group
        GroupRepresentation updateRep = new GroupRepresentation();
        updateRep.setName("updated-name");
        updateRep.setDescription("Updated description");

        try (Response response = groups().group(groupId).update(updateRep)) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify the update
        GroupRepresentation retrieved = groups().group(groupId).toRepresentation(false);
        assertThat(retrieved.getName(), is("updated-name"));
        assertThat(retrieved.getPath(), is("/updated-name"));
        assertThat(retrieved.getDescription(), is("Updated description"));
    }

    @Test
    public void testDeleteOrganizationGroup() {
        String groupId = createGroup("test-group");

        // Delete the group
        groups().group(groupId).delete();

        // Verify it's deleted
        List<GroupRepresentation> groups = groups().getAll(null, null, null, 0, 10, true, false);
        assertThat(groups, hasSize(0));
    }

    @Test
    public void testCreateSubGroup() {
        String parentId = createGroup("parent-group");
        createSubGroup(parentId, "child-group");

        // Verify subgroup exists
        List<GroupRepresentation> subGroups = groups().group(parentId).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));
        assertThat(subGroups.get(0).getPath(), is("/parent-group/child-group"));

        // Verify parent's subgroup count
        GroupRepresentation parentWithCount = groups().group(parentId).toRepresentation(true);
        assertThat(parentWithCount.getSubGroupCount(), is(1L));
    }

    @Test
    public void testMemberJoinGroup() {
        addMember(member);
        String groupId = createGroup("test-group");

        // Add member to group
        groups().group(groupId).addMember(member.getId());

        // Verify member is in group
        List<MemberRepresentation> members = groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getId(), is(member.getId()));
    }

    @Test
    public void testMemberLeaveGroup() {
        addMember(member);
        String groupId = createGroup("test-group");

        // Add member to group
        groups().group(groupId).addMember(member.getId());

        // Verify member is in group
        List<MemberRepresentation> members = groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(1));

        // Remove member from group
        groups().group(groupId).removeMember(member.getId());

        // Verify member is not in group
        members = groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(0));
    }

    @Test
    public void testNonMemberCannotJoinGroup() {
        String groupId = createGroup("test-group");

        // Try to add a user who is NOT a member of the organization to group - should fail
        try {
            groups().group(groupId).addMember(nonMember.getId());
            fail("Should not be able to add non-member to organization group");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMemberAlreadyInGroup() {
        addMember(member);
        String groupId = createGroup("test-group");

        // Add member to group
        groups().group(groupId).addMember(member.getId());

        // Try to add the same member again - should return conflict
        try {
            groups().group(groupId).addMember(member.getId());
            fail("Should return conflict when adding member already in group");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.CONFLICT.toString()));
        }
    }

    @Test
    public void testCannotJoinOrganizationGroupViaUserAPI() {
        addMember(member);
        String groupId = createGroup("test-group");

        // Try to join group via User API - should fail
        try {
            member.admin().joinGroup(groupId);
            fail("Should not be able to join organization group via User API");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMoveGroupWithinOrganization() {
        String parent1Id = createGroup("parent1");
        String parent2Id = createGroup("parent2");

        // Create child group under parent1
        String childId = createSubGroup(parent1Id, "child-group");

        // Verify child is under parent1
        List<GroupRepresentation> subGroups = groups().group(parent1Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));

        // Move child to parent2
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(childId);
        moveRep.setName("child-group");

        try (Response response = groups().group(parent2Id).addSubGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify child is no longer under parent1
        subGroups = groups().group(parent1Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(0));

        // Verify child is now under parent2
        subGroups = groups().group(parent2Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));

        // Verify parent1's count decreased
        GroupRepresentation parent1WithCount = groups().group(parent1Id).toRepresentation(true);
        assertThat(parent1WithCount.getSubGroupCount(), is(0L));

        // Verify parent2's count increased
        GroupRepresentation parent2WithCount = groups().group(parent2Id).toRepresentation(true);
        assertThat(parent2WithCount.getSubGroupCount(), is(1L));
    }

    @Test
    public void testCannotMoveGroupToDifferentOrganization() {
        // Create a group in the first organization
        String groupId = createGroup("test-group");

        // Create a parent in the other organization
        GroupRepresentation parentRep = new GroupRepresentation();
        parentRep.setName("parent-group");
        String parentId = createGroup(otherOrganization, parentRep);

        // Try to move the group to the parent of the other organization - should fail
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(groupId);
        moveRep.setName("test-group");

        try (Response response = otherOrganization.admin().groups().group(parentId).addSubGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testSearchGroupsByNameNonExact() {
        // Create multiple groups with different names
        createGroup("sales");
        createGroup("sales-team");
        createGroup("marketing");
        createGroup("engineering");

        // Search for groups containing "sales" with exact=false
        List<GroupRepresentation> results = groups().getAll("sales", null, false, null, null, true, false);

        // Should only return groups with "sales" in the name: "sales" and "sales-team"
        assertThat(results, hasSize(2));
    }

    @Test
    public void testFindGroupByPathWithNestedOrganizationGroups() {
        // Create a 3-level hierarchy: parent/child/grandchild
        String parentId = createGroup("parent");
        String childId = createSubGroup(parentId, "child");

        List<GroupRepresentation> subGroups = groups().group(parentId).getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child"));
        assertThat(subGroups.get(0).getId(), is(childId));

        createSubGroup(childId, "grandchild");

        // Test resolving the full path - this exercises the recursive getGroupModel path
        GroupRepresentation found = groups().getGroupByPath("/parent/child/grandchild", false);
        assertThat(found, notNullValue());
        assertThat(found.getName(), is("grandchild"));
        assertThat(found.getPath(), is("/parent/child/grandchild"));

        // Test resolving intermediate paths
        GroupRepresentation foundChild = groups().getGroupByPath("/parent/child", false);
        assertThat(foundChild, notNullValue());
        assertThat(foundChild.getName(), is("child"));
        assertThat(foundChild.getPath(), is("/parent/child"));

        // Test resolving top-level path
        GroupRepresentation foundParent = groups().getGroupByPath("/parent", false);
        assertThat(foundParent, notNullValue());
        assertThat(foundParent.getName(), is("parent"));
        assertThat(foundParent.getPath(), is("/parent"));

        // Test non-existent path
        try {
            groups().getGroupByPath("/parent/nonexistent", false);
            fail("Should have thrown NotFoundException");
        } catch (NotFoundException e) {
            // Expected
        }
    }

    @Test
    public void testFindGroupByPathWithBriefRepresentation() {
        // Create a realm role and a client role to be mapped to the group
        RoleRepresentation realmRole = new RoleRepresentation("brief-rep-realm-role", "Test realm role", false);
        realm.admin().roles().create(realmRole);
        realm.cleanup().add(r -> r.roles().deleteRole("brief-rep-realm-role"));
        RoleRepresentation createdRealmRole = realm.admin().roles().get("brief-rep-realm-role").toRepresentation();

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("brief-rep-client");
        clientRep.setEnabled(true);
        String clientUuid;
        try (Response response = realm.admin().clients().create(clientRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            clientUuid = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.clients().get(clientUuid).remove());
        realm.admin().clients().get(clientUuid).roles().create(new RoleRepresentation("brief-rep-client-role", "Test client role", false));
        RoleRepresentation createdClientRole = realm.admin().clients().get(clientUuid).roles().get("brief-rep-client-role").toRepresentation();

        GroupRepresentation parentRep = new GroupRepresentation();
        parentRep.setName("parent");
        parentRep.singleAttribute("department", "Engineering");
        String parentId = createGroup(organization, parentRep);

        groups().group(parentId).roles().realmLevel().add(List.of(createdRealmRole));
        groups().group(parentId).roles().clientLevel(clientUuid).add(List.of(createdClientRole));

        // briefRepresentation = false returns the full representation
        GroupRepresentation full = groups().getGroupByPath("/parent", false, false);
        assertThat(full.getName(), is("parent"));
        assertThat(full.getPath(), is("/parent"));
        assertThat(full.getAttributes().get("department").get(0), is("Engineering"));
        assertThat(full.getRealmRoles(), containsInAnyOrder("brief-rep-realm-role"));
        assertThat(full.getClientRoles().get("brief-rep-client"), containsInAnyOrder("brief-rep-client-role"));

        // briefRepresentation = true omits attributes and role mappings
        GroupRepresentation brief = groups().getGroupByPath("/parent", true, false);
        assertThat(brief.getName(), is("parent"));
        assertThat(brief.getPath(), is("/parent"));
        assertThat(brief.getAttributes(), nullValue());
        assertThat(brief.getRealmRoles(), nullValue());
        assertThat(brief.getClientRoles(), nullValue());

        // briefRepresentation defaults to true when the parameter is not sent
        GroupRepresentation defaultRep = groups().getGroupByPath("/parent", false);
        assertThat(defaultRep.getName(), is("parent"));
        assertThat(defaultRep.getPath(), is("/parent"));
        assertThat(defaultRep.getAttributes(), nullValue());
        assertThat(defaultRep.getRealmRoles(), nullValue());
        assertThat(defaultRep.getClientRoles(), nullValue());
    }

    @Test
    public void testSubGroupCountQueryParameter() {
        // Create parent with 3 subgroups
        String parentId = createGroup("parent");
        for (int i = 1; i <= 3; i++) {
            createSubGroup(parentId, "child-" + i);
        }

        // Test with subGroupsCount = true
        GroupRepresentation withCount = groups().group(parentId).toRepresentation(true);
        assertThat(withCount.getSubGroupCount(), is(3L));

        // Test with subGroupsCount = false (default)
        GroupRepresentation withoutCount = groups().group(parentId).toRepresentation(false);
        assertThat(withoutCount.getSubGroupCount(), nullValue());

        // Test group with no subgroups
        String leafId = createGroup("leaf-group");

        GroupRepresentation leafWithCount = groups().group(leafId).toRepresentation(true);
        assertThat(leafWithCount.getSubGroupCount(), is(0L));

        // Test getAll() with subGroupsCount parameter
        List<GroupRepresentation> allWithCount = groups().getAll(null, null, null, null, null, false, true);
        assertThat(allWithCount, hasSize(2)); // parent, leaf-group
        allWithCount.forEach(g -> assertThat(g.getSubGroupCount(), notNullValue()));

        List<GroupRepresentation> allWithoutCount = groups().getAll(null, null, null, null, null, false, false);
        assertThat(allWithoutCount, hasSize(2));
        allWithoutCount.forEach(g -> assertThat(g.getSubGroupCount(), nullValue()));

        // Test getGroupByPath with subGroupsCount
        GroupRepresentation byPathWithCount = groups().getGroupByPath("/parent", true);
        assertThat(byPathWithCount.getSubGroupCount(), is(3L));

        GroupRepresentation byPathWithoutCount = groups().getGroupByPath("/parent", false);
        assertThat(byPathWithoutCount.getSubGroupCount(), nullValue());
    }

    @Test
    public void testMoveGroupToTopLevel() {
        String parentId = createGroup("parent-group");
        String childId = createSubGroup(parentId, "child-group");

        // Verify initial state: parent has 1 subgroup
        List<GroupRepresentation> subGroups = groups().group(parentId).getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));

        // Verify child's path shows it's under parent
        GroupRepresentation child = groups().group(childId).toRepresentation(false);
        assertThat(child.getPath(), is("/parent-group/child-group"));

        // Move child to top-level by calling addTopLevelGroup with ID
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(childId);
        moveRep.setName("child-group");
        try (Response response = groups().addTopLevelGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify child is now top-level (path should not include parent)
        child = groups().group(childId).toRepresentation(false);
        assertThat(child.getPath(), is("/child-group"));

        // Verify parent no longer has subgroups
        subGroups = groups().group(parentId).getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(0));

        // Verify getAll returns both as top-level groups
        List<GroupRepresentation> topLevelGroups = groups().getAll(null, null, null, null, null, true, false);
        assertThat(topLevelGroups, hasSize(2));
        Set<String> groupNames = topLevelGroups.stream().map(GroupRepresentation::getName).collect(Collectors.toSet());
        assertThat(groupNames, containsInAnyOrder("parent-group", "child-group"));
    }

    @Test
    public void testMoveGroupToTopLevelValidation() {
        // Create a realm group (not org group)
        GroupRepresentation realmGroupRep = new GroupRepresentation();
        realmGroupRep.setName("realm-group");
        String realmGroupId;
        try (Response response = realm.admin().groups().add(realmGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroupId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> r.groups().group(realmGroupId).remove());

        // Try to move realm group to org top-level - should fail
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(realmGroupId);
        moveRep.setName("realm-group");
        try (Response response = groups().addTopLevelGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        }

        // Try to move non-existent group - should fail
        GroupRepresentation nonExistentRep = new GroupRepresentation();
        nonExistentRep.setId("non-existent-id");
        nonExistentRep.setName("non-existent");
        try (Response response = groups().addTopLevelGroup(nonExistentRep)) {
            assertThat(response.getStatus(), is(Status.NOT_FOUND.getStatusCode()));
        }
    }

    private OrganizationGroupsResource groups() {
        return organization.admin().groups();
    }

    private String createGroup(String name) {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(name);
        return createGroup(organization, groupRep);
    }

    private String createGroup(ManagedOrganization org, GroupRepresentation groupRep) {
        String groupId;
        try (Response response = org.admin().groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }
        deleteGroupAfterTest(org, groupId);
        return groupId;
    }

    private String createSubGroup(String parentId, String name) {
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName(name);

        String groupId;
        try (Response response = groups().group(parentId).addSubGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = response.readEntity(GroupRepresentation.class).getId();
        }
        // the subgroup is usually deleted together with its parent, but not if the test moved it elsewhere
        deleteGroupAfterTest(organization, groupId);
        return groupId;
    }

    private void deleteGroupAfterTest(ManagedOrganization org, String groupId) {
        org.cleanup().add(o -> {
            try {
                o.groups().group(groupId).delete();
            } catch (NotFoundException ignored) {
                // already deleted by the test itself or together with its parent
            }
        });
    }

    private void addMember(ManagedUser user) {
        try (Response response = organization.admin().members().addMember(user.getId())) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
        organization.cleanup().add(o -> o.members().member(user.getId()).delete().close());
    }

    public static class MemberConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("member").email("member@example.org").emailVerified(true);
        }
    }

    public static class NonMemberConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("nonmember").email("nonmember@example.org").emailVerified(true);
        }
    }
}
