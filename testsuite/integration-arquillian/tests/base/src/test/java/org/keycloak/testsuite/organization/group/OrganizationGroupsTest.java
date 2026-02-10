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

package org.keycloak.testsuite.organization.group;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class OrganizationGroupsTest extends AbstractOrganizationTest {

    @Test
    public void testCreateOrganizationGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        List<GroupRepresentation> groups = orgResource.groups().getAll(null, null, null, null);
        assertThat(groups, hasSize(1));
        assertThat(groups.get(0).getName(), is("test-group"));
        assertThat(groups.get(0).getPath(), is("/test-group"));
    }

    @Test
    public void testCreateDuplicateGroupName() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("duplicate-group");

        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Try to create another group with the same name
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CONFLICT.getStatusCode()));
        }
    }

    @Test
    public void testGetOrganizationGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation retrieved = orgResource.groups().group(groupId).toRepresentation();
        assertNotNull(retrieved);
        assertThat(retrieved.getName(), is("test-group"));
        assertThat(retrieved.getPath(), is("/test-group"));
    }

    @Test
    public void testUpdateOrganizationGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("original-name");
        groupRep.setDescription("Original description");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Update the group
        GroupRepresentation updateRep = new GroupRepresentation();
        updateRep.setName("updated-name");
        updateRep.setDescription("Updated description");

        try (Response response = orgResource.groups().group(groupId).update(updateRep)) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify the update
        GroupRepresentation retrieved = orgResource.groups().group(groupId).toRepresentation();
        assertThat(retrieved.getName(), is("updated-name"));
        assertThat(retrieved.getPath(), is("/updated-name"));
        assertThat(retrieved.getDescription(), is("Updated description"));
    }

    @Test
    public void testDeleteOrganizationGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Delete the group
        orgResource.groups().group(groupId).delete();

        // Verify it's deleted
        List<GroupRepresentation> groups = orgResource.groups().getAll(null, null, 0, 10);
        assertThat(groups, hasSize(0));
    }

    @Test
    public void testCreateSubGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create parent group
        GroupRepresentation parentRep = new GroupRepresentation();
        parentRep.setName("parent-group");

        String parentId;
        try (Response response = orgResource.groups().addTopLevelGroup(parentRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            parentId = ApiUtil.getCreatedId(response);
        }

        // Create subgroup
        GroupRepresentation childRep = new GroupRepresentation();
        childRep.setName("child-group");

        try (Response response = orgResource.groups().group(parentId).addSubGroup(childRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Verify subgroup exists
        List<GroupRepresentation> subGroups = orgResource.groups().group(parentId).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));
        assertThat(subGroups.get(0).getPath(), is("/parent-group/child-group"));
    }

    @Test
    public void testMemberJoinGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a member
        MemberRepresentation member = addMember(orgResource);

        // Create a group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Add member to group
        orgResource.groups().group(groupId).addMember(member.getId());

        // Verify member is in group
        List<MemberRepresentation> members = orgResource.groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(1));
        assertThat(members.get(0).getId(), is(member.getId()));
    }

    @Test
    public void testMemberLeaveGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a member
        MemberRepresentation member = addMember(orgResource);

        // Create a group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Add member to group
        orgResource.groups().group(groupId).addMember(member.getId());

        // Verify member is in group
        List<MemberRepresentation> members = orgResource.groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(1));

        // Remove member from group
        orgResource.groups().group(groupId).removeMember(member.getId());

        // Verify member is not in group
        members = orgResource.groups().group(groupId).getMembers(0, 10, false);
        assertThat(members, hasSize(0));
    }

    @Test
    public void testNonMemberCannotJoinGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a user who is NOT a member of the organization
        UserRepresentation nonMember = new UserRepresentation();
        nonMember.setEmail("nonmember@example.com");
        nonMember.setUsername("nonmember");
        nonMember.setEnabled(true);

        String userId;
        try (Response response = testRealm().users().create(nonMember)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            userId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().users().get(userId).remove());

        // Create a group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Try to add non-member to group - should fail
        try {
            orgResource.groups().group(groupId).addMember(userId);
            fail("Should not be able to add non-member to organization group");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMemberAlreadyInGroup() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a member
        MemberRepresentation member = addMember(orgResource);

        // Create a group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Add member to group
        orgResource.groups().group(groupId).addMember(member.getId());

        // Try to add the same member again - should return conflict
        try {
            orgResource.groups().group(groupId).addMember(member.getId());
            fail("Should return conflict when adding member already in group");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.CONFLICT.toString()));
        }
    }

    @Test
    public void testCannotJoinOrganizationGroupViaUserAPI() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a member
        MemberRepresentation member = addMember(orgResource);

        // Create a group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Try to join group via User API - should fail
        try {
            testRealm().users().get(member.getId()).joinGroup(groupId);
            fail("Should not be able to join organization group via User API");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMoveGroupWithinOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create parent group 1
        GroupRepresentation parent1Rep = new GroupRepresentation();
        parent1Rep.setName("parent1");

        String parent1Id;
        try (Response response = orgResource.groups().addTopLevelGroup(parent1Rep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            parent1Id = ApiUtil.getCreatedId(response);
        }

        // Create parent group 2
        GroupRepresentation parent2Rep = new GroupRepresentation();
        parent2Rep.setName("parent2");

        String parent2Id;
        try (Response response = orgResource.groups().addTopLevelGroup(parent2Rep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            parent2Id = ApiUtil.getCreatedId(response);
        }

        // Create child group under parent1
        GroupRepresentation childRep = new GroupRepresentation();
        childRep.setName("child-group");

        String childId;
        try (Response response = orgResource.groups().group(parent1Id).addSubGroup(childRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            childId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Verify child is under parent1
        List<GroupRepresentation> subGroups = orgResource.groups().group(parent1Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));

        // Move child to parent2
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(childId);
        moveRep.setName("child-group");

        try (Response response = orgResource.groups().group(parent2Id).addSubGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify child is no longer under parent1
        subGroups = orgResource.groups().group(parent1Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(0));

        // Verify child is now under parent2
        subGroups = orgResource.groups().group(parent2Id).getSubGroups(null, null, 0, 10);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("child-group"));
    }

    @Test
    public void testCannotMoveGroupToDifferentOrganization() {
        // Create first organization
        OrganizationRepresentation org1Rep = createOrganization("org1");
        OrganizationResource org1Resource = testRealm().organizations().get(org1Rep.getId());

        // Create second organization
        OrganizationRepresentation org2Rep = createOrganization("org2");
        OrganizationResource org2Resource = testRealm().organizations().get(org2Rep.getId());

        // Create a group in org1
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("test-group");

        String groupId;
        try (Response response = org1Resource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Create a parent in org2
        GroupRepresentation parentRep = new GroupRepresentation();
        parentRep.setName("parent-group");

        String parentId;
        try (Response response = org2Resource.groups().addTopLevelGroup(parentRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            parentId = ApiUtil.getCreatedId(response);
        }

        // Try to move org1's group to org2's parent - should fail
        GroupRepresentation moveRep = new GroupRepresentation();
        moveRep.setId(groupId);
        moveRep.setName("test-group");

        try (Response response = org2Resource.groups().group(parentId).addSubGroup(moveRep)) {
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testSearchGroupsByNameNonExact() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create multiple groups with different names
        GroupRepresentation group1 = new GroupRepresentation();
        group1.setName("sales");
        try (Response response = orgResource.groups().addTopLevelGroup(group1)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        GroupRepresentation group2 = new GroupRepresentation();
        group2.setName("sales-team");
        try (Response response = orgResource.groups().addTopLevelGroup(group2)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        GroupRepresentation group3 = new GroupRepresentation();
        group3.setName("marketing");
        try (Response response = orgResource.groups().addTopLevelGroup(group3)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        GroupRepresentation group4 = new GroupRepresentation();
        group4.setName("engineering");
        try (Response response = orgResource.groups().addTopLevelGroup(group4)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Search for groups containing "sales" with exact=false
        List<GroupRepresentation> results = orgResource.groups().getAll("sales", false, null, null);

        // Should only return groups with "sales" in the name: "sales" and "sales-team"
        assertThat(results, hasSize(2));
    }
}
