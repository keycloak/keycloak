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

package org.keycloak.tests.organization.admin;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the REST endpoints used by the Admin UI for managing organization roles on organization groups.
 * These tests cover the exact endpoints used by the Admin UI:
 * - GET /role-mappings/organizations/available (list available roles)
 * - POST /role-mappings/organizations (add roles)
 * - DELETE /role-mappings/organizations (remove roles)
 */
@KeycloakIntegrationTest
public class OrganizationGroupRoleEndpointTest extends AbstractOrganizationTest {

    private OrganizationRepresentation organization;
    private GroupRepresentation orgGroup;

    @BeforeEach
    public void setup() {
        organization = createOrganization("acme", "Acme");
        
        // Create an organization group via organization resource
        orgGroup = new GroupRepresentation();
        orgGroup.setName("dev-team");
        try (Response response = realm.admin().organizations().get(organization.getId()).groups().addTopLevelGroup(orgGroup)) {
            orgGroup.setId(response.getLocation().getPath().replaceAll(".*/", ""));
        }
    }

    @Test
    public void testGetAvailableOrganizationRoles() {
        // Create organization roles
        RoleRepresentation adminRole = new RoleRepresentation("admin", "Admin role", false);
        RoleRepresentation editorRole = new RoleRepresentation("editor", "Editor role", false);
        
        realm.admin().organizations().get(organization.getId()).roles().create(adminRole).close();
        realm.admin().organizations().get(organization.getId()).roles().create(editorRole).close();
        
        // Get available organization roles for the group via organization resource
        List<RoleRepresentation> availableRoles = realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .getAvailableOrganizationRoleMappings();
        
        // Filter default role if present
        availableRoles = availableRoles.stream()
                .filter(role -> !role.getName().startsWith("default-roles"))
                .toList();
        
        // Should have both created roles available
        assertThat(availableRoles, hasSize(2));
    }

    @Test
    public void testAddOrganizationRolesToGroup() {
        // Create an organization role
        RoleRepresentation adminRole = new RoleRepresentation("admin", "Admin role", false);
        try (Response response = realm.admin().organizations().get(organization.getId()).roles().create(adminRole)) {
            adminRole.setId(response.getLocation().getPath().replaceAll(".*/", ""));
        }
        
        // Add the organization role to the group via organization resource
        realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .addOrganizationRoleMappings(List.of(adminRole));
        
        // Verify role is no longer in available list (proves it was assigned)
        List<RoleRepresentation> availableRoles = realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .getAvailableOrganizationRoleMappings();
        
        boolean adminNotAvailable = availableRoles.stream()
                .noneMatch(role -> role.getName().equals("admin"));
        assertTrue(adminNotAvailable, "Admin role should not be available after being assigned");
    }

    @Test
    public void testRemoveOrganizationRolesFromGroup() {
        // Create and assign an organization role
        RoleRepresentation adminRole = new RoleRepresentation("admin", "Admin role", false);
        try (Response response = realm.admin().organizations().get(organization.getId()).roles().create(adminRole)) {
            adminRole.setId(response.getLocation().getPath().replaceAll(".*/", ""));
        }
        
        realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .addOrganizationRoleMappings(List.of(adminRole));
        
        // Remove the role via organization resource
        realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .deleteOrganizationRoleMappings(List.of(adminRole));
        
        // Verify role is back in available list (proves it was removed)
        List<RoleRepresentation> availableRoles = realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .getAvailableOrganizationRoleMappings();
        
        boolean adminAvailable = availableRoles.stream()
                .anyMatch(role -> role.getName().equals("admin"));
        assertTrue(adminAvailable, "Admin role should be available again after removal");
    }

    @Test
    public void testMultipleOrganizationRolesWorkflow() {
        // Create multiple organization roles
        RoleRepresentation adminRole = new RoleRepresentation("admin", "Admin role", false);
        RoleRepresentation editorRole = new RoleRepresentation("editor", "Editor role", false);
        
        try (Response adminResp = realm.admin().organizations().get(organization.getId()).roles().create(adminRole)) {
            adminRole.setId(adminResp.getLocation().getPath().replaceAll(".*/", ""));
        }
        try (Response editorResp = realm.admin().organizations().get(organization.getId()).roles().create(editorRole)) {
            editorRole.setId(editorResp.getLocation().getPath().replaceAll(".*/", ""));
        }
        
        // Add both roles to group via organization resource
        realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .addOrganizationRoleMappings(List.of(adminRole, editorRole));
        
        // Verify both roles are not available (assigned)
        List<RoleRepresentation> availableRoles = realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .getAvailableOrganizationRoleMappings();
        
        long assigned = availableRoles.stream()
                .filter(role -> role.getName().equals("admin") || role.getName().equals("editor"))
                .count();
        
        assertTrue(assigned == 0, "Both roles should be assigned (not available)");
        
        // Remove one role via organization resource
        realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .deleteOrganizationRoleMappings(List.of(adminRole));
        
        // Verify only admin is available (editor still assigned)
        availableRoles = realm.admin().organizations().get(organization.getId())
                .groups().group(orgGroup.getId())
                .roles()
                .getAvailableOrganizationRoleMappings();
        
        boolean adminAvailable = availableRoles.stream()
                .anyMatch(role -> role.getName().equals("admin"));
        boolean editorNotAvailable = availableRoles.stream()
                .noneMatch(role -> role.getName().equals("editor"));
        
        assertTrue(adminAvailable, "Admin should be available after removal");
        assertTrue(editorNotAvailable, "Editor should still be assigned");
    }

    @Test
    public void testCannotAssignDefaultRoleToGroup() {
        // Get the organization with its default role
        OrganizationRepresentation org = realm.admin().organizations().get(organization.getId()).toRepresentation();
        
        // The default role is automatically created when organization is created
        List<RoleRepresentation> allRoles = realm.admin().organizations().get(organization.getId()).roles().list();
        RoleRepresentation defaultRole = allRoles.stream()
                .filter(role -> role.getName().startsWith("default-roles"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Default role should exist"));
        
        // Attempt to assign the default role to the group via organization resource should fail
        assertThrows(BadRequestException.class, 
                () -> realm.admin().organizations().get(organization.getId())
                        .groups().group(orgGroup.getId())
                        .roles()
                        .addOrganizationRoleMappings(List.of(defaultRole)),
                "Should not allow assigning default organization role to groups");
    }

    @Test
    public void testCannotAssignDefaultRoleAsComposite() {
        // Get the organization with its default role
        OrganizationRepresentation org = realm.admin().organizations().get(organization.getId()).toRepresentation();

        // The default role is automatically created when organization is created
        List<RoleRepresentation> allRoles = realm.admin().organizations().get(organization.getId()).roles().list();
        RoleRepresentation defaultRole = allRoles.stream()
                .filter(role -> role.getName().startsWith("default-roles"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Default role should exist"));

        RoleRepresentation compositeRole = new RoleRepresentation("composite-role", "Composite role", false);

        try (Response response = realm.admin().organizations().get(organization.getId()).roles().create(compositeRole)) {
            compositeRole.setId(response.getLocation().getPath().replaceAll(".*/", ""));
        }

        // Attempt to assign the default role to the group via organization resource should fail
        assertThrows(BadRequestException.class,
                () -> realm.admin().organizations().get(organization.getId())
                        .roles().get(compositeRole.getId())
                        .addComposites(List.of(defaultRole)),
                "Should not allow assigning default organization role to groups");
    }

}
