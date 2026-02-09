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

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;


public class OrganizationGroupIsolationTest extends AbstractOrganizationTest {

    @Test
    public void testOrgGroupsNotInRealmGroupsAPI() {
        // Org groups should NOT appear in the main realm groups tree
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create org group
        GroupRepresentation orgGroupRep = new GroupRepresentation();
        orgGroupRep.setName("Engineering");
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroupRep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }

        // Create realm group
        GroupRepresentation realmGroupRep = new GroupRepresentation();
        realmGroupRep.setName("RealmGroup");
        String realmGroupId;
        try (Response response = testRealm().groups().add(realmGroupRep)) {
            realmGroupId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroupId).remove());

        // Get all realm groups - should only include realm groups, not org groups
        List<String> realmGroupNames = testRealm().groups().groups().stream().map(GroupRepresentation::getName).toList();

        // Should find only realm groups
        assertThat(realmGroupNames, hasItem("RealmGroup"));
        assertThat(realmGroupNames, not(hasItem("Engineering")));
    }

    @Test
    public void testOrgGroupsIsolatedBetweenOrganizations() {
        // Organizations should not see each other's groups
        OrganizationRepresentation orgA = createOrganization("OrgA", "orga.com");
        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());

        OrganizationRepresentation orgB = createOrganization("OrgB", "orgb.com");
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Create group in Org A
        GroupRepresentation groupA = new GroupRepresentation();
        groupA.setName("Engineering");
        String groupAId;
        try (Response response = orgAResource.groups().addTopLevelGroup(groupA)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            groupAId = ApiUtil.getCreatedId(response);
        }

        // Create group in Org B
        GroupRepresentation groupB = new GroupRepresentation();
        groupB.setName("Sales");
        String groupBId;
        try (Response response = orgBResource.groups().addTopLevelGroup(groupB)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupBId = ApiUtil.getCreatedId(response);
        }

        // Org A should only see its own groups
        List<GroupRepresentation> orgAGroups = orgAResource.groups().getAll(null, null, 0, 10);
        assertThat(orgAGroups, hasSize(1));
        assertThat(orgAGroups.get(0).getName(), is("Engineering"));

        // Org B should only see its own groups
        List<GroupRepresentation> orgBGroups = orgBResource.groups().getAll(null, null, 0, 10);
        assertThat(orgBGroups, hasSize(1));
        assertThat(orgBGroups.get(0).getName(), is("Sales"));

        // Org A cannot access Org B's group
        try {
            orgAResource.groups().group(groupBId).toRepresentation();
            fail("Org A should not be able to access Org B's groups");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }

        // Org B cannot access Org A's group
        try {
            orgBResource.groups().group(groupAId).toRepresentation();
            fail("Org B should not be able to access Org A's groups");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testCannotAddMemberFromDifferentOrg() {
        // Cannot add member of Org A to a group in Org B
        OrganizationRepresentation orgA = createOrganization("OrgA", "orga.com");
        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());

        OrganizationRepresentation orgB = createOrganization("OrgB", "orgb.com");
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Create member in Org A
        MemberRepresentation memberA = addMember(orgAResource, "alice@orga.com");

        // Create group in Org B
        GroupRepresentation groupB = new GroupRepresentation();
        groupB.setName("Sales");
        String groupBId;
        try (Response response = orgBResource.groups().addTopLevelGroup(groupB)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupBId = ApiUtil.getCreatedId(response);
        }

        // Try to add Org A member to Org B group - should fail
        try {
            orgBResource.groups().group(groupBId).addMember(memberA.getId());
            fail("Should not be able to add member from different org");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Status.BAD_REQUEST.toString()));
        }
    }

    @Test
    public void testMultiOrgUserSeesGroupsFromBothOrgs() {
        // User who is member of multiple orgs should see groups from all their orgs
        OrganizationRepresentation orgA = createOrganization("OrgA", "orga.com");
        OrganizationResource orgAResource = testRealm().organizations().get(orgA.getId());

        OrganizationRepresentation orgB = createOrganization("OrgB", "orgb.com");
        OrganizationResource orgBResource = testRealm().organizations().get(orgB.getId());

        // Create a user who is member of both orgs
        String userEmail = "multiorg@example.com";
        MemberRepresentation memberInA = addMember(orgAResource, userEmail);

        // Add same user to Org B
        try (Response response = orgBResource.members().addMember(memberInA.getId())) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Create group in Org A
        GroupRepresentation groupA = new GroupRepresentation();
        groupA.setName("EngineeringA");
        String groupAId;
        try (Response response = orgAResource.groups().addTopLevelGroup(groupA)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupAId = ApiUtil.getCreatedId(response);
        }

        // Create group in Org B
        GroupRepresentation groupB = new GroupRepresentation();
        groupB.setName("SalesB");
        String groupBId;
        try (Response response = orgBResource.groups().addTopLevelGroup(groupB)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupBId = ApiUtil.getCreatedId(response);
        }

        // Add user to both groups
        orgAResource.groups().group(groupAId).addMember(memberInA.getId());
        orgBResource.groups().group(groupBId).addMember(memberInA.getId());

        // User should be in group from Org A
        List<MemberRepresentation> groupAMembers = orgAResource.groups().group(groupAId).getMembers(null, null, true);
        assertThat(groupAMembers, hasSize(1));

        // User should be in group from Org B
        List<MemberRepresentation> groupBMembers = orgBResource.groups().group(groupBId).getMembers(null, null, true);
        assertThat(groupBMembers, hasSize(1));
    }

    @Test
    public void testOrgGroupsSeparateFromRealmGroupsWithSameName() {
        // Org groups and realm groups can have same name without conflict
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        String groupName = "Engineering";

        // Create org group
        GroupRepresentation orgGroupRep = new GroupRepresentation();
        orgGroupRep.setName(groupName);
        String orgGroupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            orgGroupId = ApiUtil.getCreatedId(response);
        }

        // Create realm group with same name
        GroupRepresentation realmGroupRep = new GroupRepresentation();
        realmGroupRep.setName(groupName);
        String realmGroupId;
        try (Response response = testRealm().groups().add(realmGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroupId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroupId).remove());

        // Both should exist and be different groups
        GroupRepresentation orgGroup = orgResource.groups().group(orgGroupId).toRepresentation();
        assertThat(orgGroup.getName(), is(groupName));

        GroupRepresentation realmGroup = testRealm().groups().group(realmGroupId).toRepresentation();
        assertThat(realmGroup.getName(), is(groupName));

        // They should have different IDs
        assertThat(orgGroup.getId(), is(orgGroupId));
        assertThat(realmGroup.getId(), is(realmGroupId));
    }

    @Test
    public void testSearchOrgGroupsDoesNotReturnRealmGroups() {
        // Searching org groups should not return realm groups
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create org group with "Engineering" name
        GroupRepresentation orgGroupRep = new GroupRepresentation();
        orgGroupRep.setName("Engineering");
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Create realm group with similar name
        GroupRepresentation realmGroupRep = new GroupRepresentation();
        realmGroupRep.setName("Engineering-Realm");
        String realmGroupId;
        try (Response response = testRealm().groups().add(realmGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            realmGroupId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().groups().group(realmGroupId).remove());

        // Search org groups for "Engineering"
        List<GroupRepresentation> results = orgResource.groups().getAll("Engineering", null, null, null);

        // Should only find org group, not realm group
        assertThat(results, hasSize(1));
        assertThat(results.get(0).getName(), is("Engineering"));
    }

    @Test
    public void testCannotUseOrgGroupInAuthorizationPolicy() {
        // Organization groups cannot be used in authorization policies - only realm groups are allowed
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create organization group
        GroupRepresentation orgGroupRep = new GroupRepresentation();
        orgGroupRep.setName("Engineering");
        String orgGroupId;
        try (Response response = orgResource.groups().addTopLevelGroup(orgGroupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            orgGroupId = ApiUtil.getCreatedId(response);
        }

        // Create a client with authorization services enabled
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("test-authz-client");
        clientRep.setSecret("secret");
        clientRep.setServiceAccountsEnabled(true);
        clientRep.setAuthorizationServicesEnabled(true);
        clientRep.setPublicClient(false);

        String clientId;
        try (Response response = testRealm().clients().create(clientRep)) {
            clientId = ApiUtil.getCreatedId(response);
        }
        getCleanup().addCleanup(() -> testRealm().clients().get(clientId).remove());

        ClientResource clientResource = testRealm().clients().get(clientId);

        // Try to create a group policy using the organization group - should fail
        GroupPolicyRepresentation policy = new GroupPolicyRepresentation();
        policy.setName("org-group-policy");
        policy.addGroup(orgGroupId);
        policy.setLogic(Logic.POSITIVE);

        try (Response response = clientResource.authorization().policies().group().create(policy)) {
            assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        }
    }
}
