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
import static org.junit.Assert.fail;


/**
 * Tests for organization group membership - validates the dual explicit membership model.
 *
 * Design decisions tested (from A30):
 * - Users are members of the internal group directly (org membership)
 * - Users are members of org-specific groups (explicit, on top of org membership)
 * - Both memberships are explicit in the database, NOT implicit through hierarchy
 */
public class OrganizationGroupMembershipTest extends AbstractOrganizationTest {

    @Test
    public void testDualMembershipModel() {
        // A30: User should have explicit membership in both internal group and org-group
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create member - this adds them to internal group
        MemberRepresentation member = addMember(orgResource);

        // Create Backend group
        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().addTopLevelGroup(backendRep)) {
            backendId = ApiUtil.getCreatedId(response);
        }

        // Add member to Backend group - this is explicit membership
        orgResource.groups().group(backendId).addMember(member.getId());

        // Verify member is in Backend group
        List<MemberRepresentation> backendMembers = orgResource.groups().group(backendId).getMembers(null, null, false);
        assertThat(backendMembers, hasSize(1));
        assertThat(backendMembers.get(0).getId(), is(member.getId()));

        // Verify member is still an org member (internal group membership)
        List<MemberRepresentation> orgMembers = orgResource.members().list(null, null);
        assertThat(orgMembers, hasSize(1));
        assertThat(orgMembers.get(0).getId(), is(member.getId()));
    }

    @Test
    public void testOrgMembershipRequiredBeforeGroupMembership() {
        // A30 + Q12a: Users must be org members before joining org groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create a user who is NOT an org member
        UserRepresentation nonMember = new UserRepresentation();
        nonMember.setEmail("nonmember@example.com");
        nonMember.setUsername("nonmember");
        nonMember.setEnabled(true);
        String userId;
        try (Response response = testRealm().users().create(nonMember)) {
            userId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().users().get(userId).remove());

        // Create org group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            groupId = ApiUtil.getCreatedId(response);
        }

        // Try to add non-member to org group - should fail
        try {
            orgResource.groups().group(groupId).addMember(userId);
            fail("Should not be able to add non-member to org group");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMemberInMultipleOrgGroups() {
        // User can be member of multiple org groups simultaneously
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Create Engineering group
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        // Create Sales group
        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        String salesId;
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            salesId = ApiUtil.getCreatedId(response);
        }

        // Add member to both groups
        orgResource.groups().group(engineeringId).addMember(member.getId());
        orgResource.groups().group(salesId).addMember(member.getId());

        // Verify member is in both groups
        List<MemberRepresentation> engineeringMembers = orgResource.groups().group(engineeringId).getMembers(null, null, true);
        assertThat(engineeringMembers, hasSize(1));

        List<MemberRepresentation> salesMembers = orgResource.groups().group(salesId).getMembers(null, null, true);
        assertThat(salesMembers, hasSize(1));
    }

    @Test
    public void testRemoveFromGroupDoesNotRemoveFromOrg() {
        // Removing from org-group should NOT remove from organization
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Create and join Backend group
        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().addTopLevelGroup(backendRep)) {
            backendId = ApiUtil.getCreatedId(response);
        }
        orgResource.groups().group(backendId).addMember(member.getId());

        // Remove from Backend group
        orgResource.groups().group(backendId).removeMember(member.getId());

        // Verify removed from Backend
        List<MemberRepresentation> backendMembers = orgResource.groups().group(backendId).getMembers(null, null, true);
        assertThat(backendMembers, hasSize(0));

        // Verify still an org member
        List<MemberRepresentation> orgMembers = orgResource.members().list(null, null);
        assertThat(orgMembers, hasSize(1));
    }

    @Test
    public void testRemoveFromOrgRemovesFromAllOrgGroups() {
        // Q14: Removing from organization should remove from all org groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Create two groups
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        String salesId;
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            salesId = ApiUtil.getCreatedId(response);
        }

        // Add member to both groups
        orgResource.groups().group(engineeringId).addMember(member.getId());
        orgResource.groups().group(salesId).addMember(member.getId());

        // Remove member from organization
        try (Response response = orgResource.members().member(member.getId()).delete()) {
            assertThat(response.getStatus(), is(Status.NO_CONTENT.getStatusCode()));
        }

        // Verify member is removed from both org groups
        List<MemberRepresentation> engineeringMembers = orgResource.groups().group(engineeringId).getMembers(null, null, true);
        assertThat(engineeringMembers, hasSize(0));

        List<MemberRepresentation> salesMembers = orgResource.groups().group(salesId).getMembers(null, null, true);
        assertThat(salesMembers, hasSize(0));
    }

    @Test
    public void testNestedGroupMembershipIsExplicit() {
        // Being member of Backend does NOT implicitly make you member of Engineering
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
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Add member to Backend only
        orgResource.groups().group(backendId).addMember(member.getId());

        // Member should be in Backend
        List<MemberRepresentation> backendMembers = orgResource.groups().group(backendId).getMembers(null, null, true);
        assertThat(backendMembers, hasSize(1));

        // Member should NOT be in Engineering (no implicit membership)
        List<MemberRepresentation> engineeringMembers = orgResource.groups().group(engineeringId).getMembers(null, null, true);
        assertThat(engineeringMembers, hasSize(0));
    }

    @Test
    public void testMemberOfParentAndChild() {
        // User can explicitly be member of both parent and child groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

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

        // Add member to both Engineering AND Backend
        orgResource.groups().group(engineeringId).addMember(member.getId());
        orgResource.groups().group(backendId).addMember(member.getId());

        // Verify explicit membership in both
        List<MemberRepresentation> engineeringMembers = orgResource.groups().group(engineeringId).getMembers(null, null, true);
        assertThat(engineeringMembers, hasSize(1));

        List<MemberRepresentation> backendMembers = orgResource.groups().group(backendId).getMembers(null, null, true);
        assertThat(backendMembers, hasSize(1));
    }

    @Test
    public void testAddSameMemberTwiceReturnsConflict() {
        // Adding the same member twice should return conflict
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            groupId = ApiUtil.getCreatedId(response);
        }

        // Add member first time - should succeed
        orgResource.groups().group(groupId).addMember(member.getId());

        // Add member second time - should return conflict
        try {
            orgResource.groups().group(groupId).addMember(member.getId());
            fail("Should return conflict when adding same member twice");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.CONFLICT.toString()));
        }
    }
}
