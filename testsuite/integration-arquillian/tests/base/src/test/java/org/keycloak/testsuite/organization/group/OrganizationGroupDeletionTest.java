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

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.runonserver.RunOnServer;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class OrganizationGroupDeletionTest extends AbstractOrganizationTest {

    @Test
    public void testDeleteOrgDeletesAllGroups() {
        // All org groups should be automatically deleted when org is deleted
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create multiple groups
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        String salesId;
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            salesId = ApiUtil.getCreatedId(response);
        }

        // Create nested group
        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Verify groups exist
        List<GroupRepresentation> groups = orgResource.groups().getAll(null, null, null, null);
        assertThat(groups, hasSize(2)); // Engineering, Sales (Backend is nested)

        // Delete organization
        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // Verify org is deleted
        try {
            testRealm().organizations().get(orgRep.getId()).toRepresentation();
            fail("Organization should have been deleted");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Response.Status.NOT_FOUND.toString()));
        }

        // Cannot verify groups are deleted via org API since org no longer exists
        // Groups should be cascade deleted via internal group deletion
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            GroupModel engineeringGroup = session.groups().getGroupById(realm, engineeringId);
            assertNull(engineeringGroup);

            GroupModel salesGroup = session.groups().getGroupById(realm, salesId);
            assertNull(salesGroup);

            GroupModel backendGroup = session.groups().getGroupById(realm, backendId);
            assertNull(backendGroup);
        });
    }

    @Test
    public void testDeleteOrgWithUnmanagedMembers() {
        // UNMANAGED members should NOT be deleted, just leave groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create unmanaged member
        MemberRepresentation unmanagedMember = addMember(orgResource, "unmanaged@example.com");

        // Create org group and add member
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }
        orgResource.groups().group(groupId).addMember(unmanagedMember.getId());

        // Verify member is in group
        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, true);
        assertThat(groupMembers, hasSize(1));

        // Delete organization
        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // Verify unmanaged user still exists
        UserRepresentation foundUser = testRealm().users().get(unmanagedMember.getId()).toRepresentation();
        assertNotNull(foundUser);
        assertThat(foundUser.getEmail(), is("unmanaged@example.com"));
    }

    @Test
    public void testDeleteOrgWithNestedGroupsAndMembers() {
        // Nested groups and their members should be cleaned up correctly
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Create Engineering -> Backend hierarchy
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Add member to Backend
        orgResource.groups().group(backendId).addMember(member.getId());

        // Delete organization
        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // Member should still exist (unmanaged)
        UserRepresentation foundUser = testRealm().users().get(member.getId()).toRepresentation();
        assertNotNull(foundUser);

        // Cannot verify groups are deleted via org API since org no longer exists
        // Groups should be cascade deleted via internal group deletion
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) session -> {
            RealmModel realm = session.getContext().getRealm();
            GroupModel engineeringGroup = session.groups().getGroupById(realm, engineeringId);
            assertNull(engineeringGroup);

            GroupModel backendGroup = session.groups().getGroupById(realm, backendId);
            assertNull(backendGroup);
        });
    }

    @Test
    public void testDeleteGroupRemovesMembers() {
        // Deleting a group should remove all members from that group
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member1 = addMember(orgResource, "member1@example.com");
        MemberRepresentation member2 = addMember(orgResource, "member2@example.com");

        // Create group and add members
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        orgResource.groups().group(groupId).addMember(member1.getId());
        orgResource.groups().group(groupId).addMember(member2.getId());

        // Verify both members are in group
        List<MemberRepresentation> groupMembers = orgResource.groups().group(groupId).getMembers(null, null, true);
        assertThat(groupMembers, hasSize(2));

        // Delete group
        orgResource.groups().group(groupId).delete();

        // Members should still exist (they're org members)
        List<MemberRepresentation> orgMembers = orgResource.members().list(null, null);
        assertThat(orgMembers, hasSize(2));

        // Group should be deleted
        try {
            orgResource.groups().group(groupId).toRepresentation();
            fail("Group should have been deleted");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Response.Status.NOT_FOUND.toString()));
        }
    }

    @Test
    public void testDeleteNestedGroupDoesNotDeleteParent() {
        // Deleting a child group should not delete parent
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create Engineering -> Backend
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Delete Backend
        orgResource.groups().group(backendId).delete();

        // Engineering should still exist
        GroupRepresentation engineering = orgResource.groups().group(engineeringId).toRepresentation();
        assertNotNull(engineering);
        assertThat(engineering.getName(), is("Engineering"));

        // Backend should be gone
        List<GroupRepresentation> subGroups = orgResource.groups().group(engineeringId).getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(0));
    }

    @Test
    public void testMultiOrgUserRemovedFromOneOrg() {
        // User in multiple orgs, removed from one
        OrganizationRepresentation orgA = createOrganization("OrgA", "orga.com");
        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());

        OrganizationRepresentation orgB = createOrganization("OrgB", "orgb.com");
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Create user in Org A
        MemberRepresentation member = addMember(orgAResource, "multiorg@example.com");

        // Add to Org B as well
        try (Response response = orgBResource.members().addMember(member.getId())) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // Create groups in both orgs
        GroupRepresentation groupA = new GroupRepresentation();
        groupA.setName("EngineeringA");
        String groupAId;
        try (Response response = orgAResource.groups().addTopLevelGroup(groupA)) {
            groupAId = ApiUtil.getCreatedId(response);
        }
        orgAResource.groups().group(groupAId).addMember(member.getId());

        GroupRepresentation groupB = new GroupRepresentation();
        groupB.setName("EngineeringB");
        String groupBId;
        try (Response response = orgBResource.groups().addTopLevelGroup(groupB)) {
            groupBId = ApiUtil.getCreatedId(response);
        }
        orgBResource.groups().group(groupBId).addMember(member.getId());

        // Remove from Org A
        try (Response response = orgAResource.members().member(member.getId()).delete()) {
            assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // User should still exist
        UserRepresentation user = testRealm().users().get(member.getId()).toRepresentation();
        assertNotNull(user);

        // User should no longer be in Org A
        List<MemberRepresentation> orgAMembers = orgAResource.members().list(null, null);
        assertThat(orgAMembers, hasSize(0));

        // User should still be in Org B
        List<MemberRepresentation> orgBMembers = orgBResource.members().list(null, null);
        assertThat(orgBMembers, hasSize(1));

        // User should still be in Org B's group
        List<MemberRepresentation> groupBMembers = orgBResource.groups().group(groupBId).getMembers(null, null, true);
        assertThat(groupBMembers, hasSize(1));
    }
}
