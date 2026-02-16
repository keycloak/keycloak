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
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class OrganizationGroupUserQueryTest extends AbstractOrganizationTest {

    @Test
    public void testUserGetGroupsDoesNotIncludeOrgGroups() {
        // user.getGroupsStream() should only return realm groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Create realm group
        GroupRepresentation realmGroupRep = new GroupRepresentation();
        realmGroupRep.setName("RealmGroup");
        String realmGroupId;
        try (Response response = testRealm().groups().add(realmGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroupId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroupId).remove());

        // Add user to realm group
        testRealm().users().get(member.getId()).joinGroup(realmGroupId);

        // Create org group
        GroupRepresentation orgGroupRep = new GroupRepresentation();
        orgGroupRep.setName("Engineering");
        String orgGroupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            orgGroupId = ApiUtil.getCreatedId(response);
        }

        // Add user to org group
        orgResource.groups().group(orgGroupId).addMember(member.getId());

        // Get user's group names via UserResource
        List<String> userGroups = testRealm().users().get(member.getId()).groups().stream().map(GroupRepresentation::getName).toList();

        // Should only contain realm group, not org group
        assertThat(userGroups, hasItem("RealmGroup"));
        assertThat(userGroups, not(hasItem("Engineering")));
    }

    @Test
    public void testGetOrgGroupMembersViaOrgAPI() {
        // Query org group members via Organization API
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member1 = addMember(orgResource, "member1@example.com");
        MemberRepresentation member2 = addMember(orgResource, "member2@example.com");

        // Create org group
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");
        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        // Add both members to group
        orgResource.groups().group(groupId).addMember(member1.getId());
        orgResource.groups().group(groupId).addMember(member2.getId());

        // Query group members via org API
        List<MemberRepresentation> members = orgResource.groups().group(groupId).getMembers(null, null, true);
        assertThat(members, hasSize(2));
    }

    @Test
    public void testUserInMultipleOrgGroupsAcrossOrgs() {
        // User in multiple orgs, member of different groups in each
        OrganizationRepresentation orgA = createOrganization("OrgA", "orga.com");
        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());

        OrganizationRepresentation orgB = createOrganization("OrgB", "orgb.com");
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Create user in both orgs
        MemberRepresentation member = addMember(orgAResource, "multiorg@example.com");
        try (Response response = orgBResource.members().addMember(member.getId())) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Create groups in Org A
        GroupRepresentation groupA1 = new GroupRepresentation();
        groupA1.setName("EngineeringA");
        String groupA1Id;
        try (Response response = orgAResource.groups().addTopLevelGroup(groupA1)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupA1Id = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation groupA2 = new GroupRepresentation();
        groupA2.setName("SalesA");
        String groupA2Id;
        try (Response response = orgAResource.groups().addTopLevelGroup(groupA2)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupA2Id = ApiUtil.getCreatedId(response);
        }

        // Create groups in Org B
        GroupRepresentation groupB1 = new GroupRepresentation();
        groupB1.setName("EngineeringB");
        String groupB1Id;
        try (Response response = orgBResource.groups().addTopLevelGroup(groupB1)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupB1Id = ApiUtil.getCreatedId(response);
        }

        // Add user to groups
        orgAResource.groups().group(groupA1Id).addMember(member.getId());
        orgAResource.groups().group(groupA2Id).addMember(member.getId());
        orgBResource.groups().group(groupB1Id).addMember(member.getId());

        // Verify user is in correct groups in Org A
        List<MemberRepresentation> groupA1Members = orgAResource.groups().group(groupA1Id).getMembers(null, null, true);
        assertThat(groupA1Members, hasSize(1));

        List<MemberRepresentation> groupA2Members = orgAResource.groups().group(groupA2Id).getMembers(null, null, true);
        assertThat(groupA2Members, hasSize(1));

        // Verify user is in correct group in Org B
        List<MemberRepresentation> groupB1Members = orgBResource.groups().group(groupB1Id).getMembers(null, null, true);
        assertThat(groupB1Members, hasSize(1));
    }

    @Test
    public void testGroupsCountDoesNotIncludeOrgGroups() {
        // getGroupsCount() should not count org groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        MemberRepresentation member = addMember(orgResource);

        // Add to 2 realm groups
        GroupRepresentation realmGroup1 = new GroupRepresentation();
        realmGroup1.setName("RealmGroup1");
        String realmGroup1Id;
        try (Response response = testRealm().groups().add(realmGroup1)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroup1Id = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroup1Id).remove());
        testRealm().users().get(member.getId()).joinGroup(realmGroup1Id);

        GroupRepresentation realmGroup2 = new GroupRepresentation();
        realmGroup2.setName("RealmGroup2");
        String realmGroup2Id;
        try (Response response = testRealm().groups().add(realmGroup2)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroup2Id = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroup2Id).remove());
        testRealm().users().get(member.getId()).joinGroup(realmGroup2Id);

        // Add to 3 org groups
        for (int i = 1; i <= 3; i++) {
            GroupRepresentation orgGroup = new GroupRepresentation();
            orgGroup.setName("OrgGroup" + i);
            String orgGroupId;
            try (Response response = orgResource.groups().addTopLevelGroup(orgGroup)) {
                assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
                orgGroupId = ApiUtil.getCreatedId(response);
            }
            orgResource.groups().group(orgGroupId).addMember(member.getId());
        }

        // Get user's groups
        List<GroupRepresentation> userGroups = testRealm().users().get(member.getId()).groups();

        // Count should match size (both should be 2, excluding org groups and internal group)
        assertThat(userGroups, hasSize(2));
    }

    @Test
    public void testQueryingNonExistentOrgGroupReturnsNotFound() {
        // Querying a non-existent org group should return 404
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        try {
            orgResource.groups().group("non-existent-id").toRepresentation();
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Response.Status.NOT_FOUND.toString()));
        }
    }
}
