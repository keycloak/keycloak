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
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class OrganizationGroupHierarchyTest extends AbstractOrganizationTest {

    @Test
    public void testTopLevelGroupIsChildOfInternalGroup() {
        // Create a group and set its parent to the internal group
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setName("Engineering");

        String groupId;
        try (Response response = orgResource.groups().addTopLevelGroup(groupRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            groupId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation retrieved = orgResource.groups().group(groupId).toRepresentation();
        assertNotNull(retrieved);
        assertThat(retrieved.getName(), is("Engineering"));

        // Verify it has a parent (the internal group)
        assertThat(retrieved.getParentId(), notNullValue());
    }

    @Test
    public void testNestedGroupHierarchy() {
        // reate Engineering -> Backend hierarchy
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create parent group (Engineering)
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");

        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
            engineeringId = ApiUtil.getCreatedId(response);
        }

        // Create child group (Backend)
        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");

        String backendId;
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
            assertThat(backendId, notNullValue());
        }

        // Backend.getParent() -> Engineering
        GroupRepresentation backend = orgResource.groups().group(backendId).toRepresentation();
        assertThat(backend.getParentId(), is(engineeringId));

        // Verify Engineering contains Backend as subgroup
        List<GroupRepresentation> subGroups = orgResource.groups().group(engineeringId).getSubGroups(null, null, null, null);
        assertThat(subGroups, hasSize(1));
        assertThat(subGroups.get(0).getName(), is("Backend"));
    }

    @Test
    public void testMultiLevelNesting() {
        // Test deeper hierarchy: Engineering -> Backend -> Platform
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Level 1: Engineering
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        // Level 2: Backend
        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        String backendId;
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
            assertThat(backendId, notNullValue());
        }

        // Level 3: Platform
        GroupRepresentation platformRep = new GroupRepresentation();
        platformRep.setName("Platform");
        String platformId;
        try (Response response = orgResource.groups().group(backendId).addSubGroup(platformRep)) {
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            platformId = response.readEntity(GroupRepresentation.class).getId();
            assertThat(platformId, notNullValue());
        }

        // Verify the full chain
        GroupRepresentation platform = orgResource.groups().group(platformId).toRepresentation();
        assertThat(platform.getParentId(), is(backendId));

        GroupRepresentation backend = orgResource.groups().group(backendId).toRepresentation();
        assertThat(backend.getParentId(), is(engineeringId));

        GroupRepresentation engineering = orgResource.groups().group(engineeringId).toRepresentation();
        assertThat(engineering.getParentId(), notNullValue()); // parent is internal group
    }

    @Test
    public void testMultipleTopLevelGroups() {
        // Multiple top-level groups should all be children of internal group
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Create Engineering
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        // Create Sales
        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        String salesId;
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            salesId = ApiUtil.getCreatedId(response);
        }

        // Both should have the same parent (internal group)
        GroupRepresentation engineering = orgResource.groups().group(engineeringId).toRepresentation();
        GroupRepresentation sales = orgResource.groups().group(salesId).toRepresentation();

        assertThat(engineering.getParentId(), notNullValue());
        assertThat(sales.getParentId(), notNullValue());
        assertThat(engineering.getParentId(), is(sales.getParentId()));
    }

    @Test
    public void testComplexHierarchy() {
        // Test a realistic organizational structure:
        // Engineering -> Backend, Frontend
        // Sales -> Enterprise, SMB
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        // Engineering branch
        GroupRepresentation engineeringRep = new GroupRepresentation();
        engineeringRep.setName("Engineering");
        String engineeringId;
        try (Response response = orgResource.groups().addTopLevelGroup(engineeringRep)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation backendRep = new GroupRepresentation();
        backendRep.setName("Backend");
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        GroupRepresentation frontendRep = new GroupRepresentation();
        frontendRep.setName("Frontend");
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(frontendRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Sales branch
        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        String salesId;
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            salesId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation enterpriseRep = new GroupRepresentation();
        enterpriseRep.setName("Enterprise");
        try (Response response = orgResource.groups().group(salesId).addSubGroup(enterpriseRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        GroupRepresentation smbRep = new GroupRepresentation();
        smbRep.setName("SMB");
        try (Response response = orgResource.groups().group(salesId).addSubGroup(smbRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Verify structure
        List<GroupRepresentation> engineeringSubGroups = orgResource.groups().group(engineeringId).getSubGroups(null, null, null, null);
        assertThat(engineeringSubGroups, hasSize(2));

        List<GroupRepresentation> salesSubGroups = orgResource.groups().group(salesId).getSubGroups(null, null, null, null);
        assertThat(salesSubGroups, hasSize(2));
    }

    @Test
    public void testDeleteParentCascadesToChildren() {
        // When deleting a parent group, children should be deleted too
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
            assertThat(response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            backendId = response.readEntity(GroupRepresentation.class).getId();
            assertThat(backendId, notNullValue());
        }

        // Delete Engineering
        orgResource.groups().group(engineeringId).delete();

        // Backend should also be deleted
        try {
            orgResource.groups().group(backendId).toRepresentation();
            fail("Backend group should have been deleted when Engineering was deleted");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(Response.Status.NOT_FOUND.toString()));
        }
    }

    @Test
    public void testGetAllGroupsReturnsTopLevelOnly() {
        // getAll() should return only top-level groups, not nested ones
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
        try (Response response = orgResource.groups().group(engineeringId).addSubGroup(backendRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // Create Sales (top-level)
        GroupRepresentation salesRep = new GroupRepresentation();
        salesRep.setName("Sales");
        try (Response response = orgResource.groups().addTopLevelGroup(salesRep)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }

        // getAll() should return 2 groups (Engineering, Sales), not 3 (Backend is nested)
        List<GroupRepresentation> topLevelGroups = orgResource.groups().getAll(null, null, null, null, true);
        assertThat(topLevelGroups, hasSize(2));
    }
}
