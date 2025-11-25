/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.testsuite;

import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test case for Organization Roles functionality.
 */
public class OrganizationRoleTest {

    @Test
    public void testOrganizationRoleCompositeRoles() {
        testingClient.server().run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            
            // Create organization
            OrganizationModel org = orgProvider.create("test-org-composite", "Test Organization Composite", "test-org-composite");
            
            // Create organization roles
            OrganizationRoleModel adminRole = org.addRole("admin");
            OrganizationRoleModel userRole = org.addRole("user");
            OrganizationRoleModel viewerRole = org.addRole("viewer");
            
            // Create realm role for composite
            RoleModel realmRole = realm.addRole("realm-composite-role");
            
            // Set up composite relationships
            adminRole.addCompositeRole(userRole);
            adminRole.addCompositeRole(viewerRole);
            adminRole.addCompositeRole(realmRole);
            
            assertTrue("Admin role should be composite", adminRole.isComposite());
            assertFalse("User role should not be composite", userRole.isComposite());
            
            // Test composite organization roles
            List<OrganizationRoleModel> orgComposites = adminRole.getCompositeOrganizationRolesStream()
                    .collect(Collectors.toList());
            assertEquals("Should have 2 organization composite roles", 2, orgComposites.size());
            
            // Test all composites (including realm roles)
            List<RoleModel> allComposites = adminRole.getCompositesStream().collect(Collectors.toList());
            assertEquals("Should have 3 total composite roles", 3, allComposites.size());
            
            // Test hasRole functionality
            assertTrue("Admin should have user role", adminRole.hasRole(userRole));
            assertTrue("Admin should have viewer role", adminRole.hasRole(viewerRole));
            assertTrue("Admin should have realm role", adminRole.hasRole(realmRole));
            assertFalse("User should not have admin role", userRole.hasRole(adminRole));
            
            // Test removing composite role
            adminRole.removeCompositeRole(viewerRole);
            assertFalse("Admin should no longer have viewer role", adminRole.hasRole(viewerRole));
            assertTrue("Admin should still have user role", adminRole.hasRole(userRole));
            
            List<OrganizationRoleModel> orgCompositesAfterRemoval = adminRole.getCompositeOrganizationRolesStream()
                    .collect(Collectors.toList());
            assertEquals("Should have 1 organization composite role after removal", 1, orgCompositesAfterRemoval.size());
        });
    }

    @Test
    public void testOrganizationRoleWithClientRoleComposites() {
        testingClient.server().run(session -> {
            RealmModel realm = session.getContext().getRealm();
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            
            // Create organization
            OrganizationModel org = orgProvider.create("test-org-client", "Test Organization Client", "test-org-client");
            
            // Create client and client role
            ClientModel client = realm.addClient("test-client");
            RoleModel clientRole = client.addRole("client-role");
            
            // Create organization role
            OrganizationRoleModel orgRole = org.addRole("org-with-client-role");
            
            // Add client role as composite
            orgRole.addCompositeRole(clientRole);
            
            assertTrue("Org role should be composite", orgRole.isComposite());
            assertTrue("Org role should have client role", orgRole.hasRole(clientRole));
            
            // Test getting all composites
            List<RoleModel> composites = orgRole.getCompositesStream().collect(Collectors.toList());
            assertEquals("Should have 1 composite role", 1, composites.size());
            assertEquals("Should be the client role", clientRole.getId(), composites.get(0).getId());
            
            // Remove composite
            orgRole.removeCompositeRole(clientRole);
            assertFalse("Org role should no longer have client role", orgRole.hasRole(clientRole));
            assertFalse("Org role should no longer be composite", orgRole.isComposite());
        });
    }

    @Test
    public void testOrganizationRolePagination() {
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            
            // Create organization
            OrganizationModel org = orgProvider.create("test-org-pagination", "Test Organization Pagination", "test-org-pagination");
            
            // Create multiple roles
            for (int i = 1; i <= 10; i++) {
                org.addRole("role-" + String.format("%02d", i));
            }
            
            // Test pagination
            List<OrganizationRoleModel> firstPage = org.getRolesStream(0, 3).collect(Collectors.toList());
            assertEquals("First page should have 3 roles", 3, firstPage.size());
            
            List<OrganizationRoleModel> secondPage = org.getRolesStream(3, 3).collect(Collectors.toList());
            assertEquals("Second page should have 3 roles", 3, secondPage.size());
            
            List<OrganizationRoleModel> lastPage = org.getRolesStream(9, 3).collect(Collectors.toList());
            assertEquals("Last page should have 1 role", 1, lastPage.size());
            
            // Test search with pagination
            List<OrganizationRoleModel> searchFirstPage = org.searchForRolesStream("role", 0, 5)
                    .collect(Collectors.toList());
            assertEquals("Search first page should have 5 roles", 5, searchFirstPage.size());
            
            List<OrganizationRoleModel> searchSecondPage = org.searchForRolesStream("role", 5, 5)
                    .collect(Collectors.toList());
            assertEquals("Search second page should have 5 roles", 5, searchSecondPage.size());
        });
    }

    @Test
    public void testOrganizationRoleEvents() {
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            
            // Create organization
            OrganizationModel org = orgProvider.create("test-org-events", "Test Organization Events", "test-org-events");
            
            // Create role (this should trigger OrganizationRoleCreatedEvent)
            OrganizationRoleModel role = org.addRole("event-role");
            role.setDescription("Role for testing events");
            
            assertNotNull("Role should be created", role);
            assertEquals("Role name should match", "event-role", role.getName());
            assertEquals("Role description should match", "Role for testing events", role.getDescription());
            
            // Additional role operations
            role.setSingleAttribute("test-attr", "test-value");
            assertEquals("Attribute should be set", "test-value", role.getFirstAttribute("test-attr"));
        });
    }

}