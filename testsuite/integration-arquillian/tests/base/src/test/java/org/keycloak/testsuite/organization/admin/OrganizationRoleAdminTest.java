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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.junit.Test;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationRoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * Admin tests for Organization Roles functionality.
 */
public class OrganizationRoleAdminTest extends AbstractOrganizationTest {

    @Test
    public void testCreateOrganizationRole() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create organization role
            OrganizationRoleModel role = org.addRole("admin");
            role.setDescription("Administrator role");
            
            // Set role attributes
            role.setSingleAttribute("level", "5");
            role.setAttribute("permissions", List.of("read", "write", "delete"));
            
            assertNotNull("Role should be created", role);
            assertEquals("Role name should match", "admin", role.getName());
            assertEquals("Role description should match", "Administrator role", role.getDescription());
            assertEquals("Role should belong to organization", org.getId(), role.getOrganization().getId());
            assertEquals("Role should have level attribute", "5", role.getFirstAttribute("level"));
            
            List<String> permissions = role.getAttributeStream("permissions").collect(Collectors.toList());
            assertThat(permissions, hasSize(3));
            assertThat(permissions, containsInAnyOrder("read", "write", "delete"));
        });
    }

    @Test
    public void testOrganizationRoleComposites() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create organization roles
            OrganizationRoleModel adminRole = org.addRole("admin");
            OrganizationRoleModel userRole = org.addRole("user");
            OrganizationRoleModel viewerRole = org.addRole("viewer");
            
            // Create realm role for composite
            session.getContext().getRealm().addRole("realm-role");
            
            // Set up composite relationships
            adminRole.addCompositeRole(userRole);
            adminRole.addCompositeRole(viewerRole);
            adminRole.addCompositeRole(session.getContext().getRealm().getRole("realm-role"));
            
            assertTrue("Admin should be composite", adminRole.isComposite());
            assertFalse("User should not be composite", userRole.isComposite());
            
            // Check composite roles
            List<OrganizationRoleModel> orgComposites = adminRole.getCompositeOrganizationRolesStream()
                    .collect(Collectors.toList());
            assertThat(orgComposites, hasSize(2));
            
            List<String> compositeNames = orgComposites.stream()
                    .map(OrganizationRoleModel::getName)
                    .collect(Collectors.toList());
            assertThat(compositeNames, containsInAnyOrder("user", "viewer"));
            
            // Check hasRole functionality
            assertTrue("Admin should have user role", adminRole.hasRole(userRole));
            assertTrue("Admin should have viewer role", adminRole.hasRole(viewerRole));
            assertFalse("User should not have admin role", userRole.hasRole(adminRole));
        });
    }

    @Test
    public void testOrganizationRoleUserAssignment() {
        createOrganization();
        
        // Create test user
        UserRepresentation user = UserBuilder.create()
                .username("roletest-user")
                .enabled(true)
                .build();
        
        try (Response response = testRealm().users().create(user)) {
            String userId = ApiUtil.getCreatedId(response);
            
            testingClient.server().run(session -> {
                OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
                OrganizationModel org = orgProvider.getByAlias("test-org");
                
                // Add user to organization
                UserModel userModel = session.users().getUserById(session.getContext().getRealm(), userId);
                orgProvider.addMember(org, userModel);
                
                // Create organization roles
                OrganizationRoleModel adminRole = org.addRole("admin");
                OrganizationRoleModel userRole = org.addRole("user");
                
                // Grant roles to user
                org.grantRole(userModel, adminRole);
                org.grantRole(userModel, userRole);
                
                // Verify user has roles
                assertTrue("User should have admin role", org.hasRole(userModel, adminRole));
                assertTrue("User should have user role", org.hasRole(userModel, userRole));
                
                // Get user roles
                List<OrganizationRoleModel> userRoles = org.getUserRolesStream(userModel).collect(Collectors.toList());
                assertThat(userRoles, hasSize(2));
                
                List<String> roleNames = userRoles.stream()
                        .map(OrganizationRoleModel::getName)
                        .collect(Collectors.toList());
                assertThat(roleNames, containsInAnyOrder("admin", "user"));
                
                // Get role members
                List<UserModel> adminMembers = org.getRoleMembersStream(adminRole).collect(Collectors.toList());
                assertThat(adminMembers, hasSize(1));
                assertEquals("Should be the test user", userId, adminMembers.get(0).getId());
                
                // Revoke a role
                org.revokeRole(userModel, userRole);
                
                assertFalse("User should not have user role after revoke", org.hasRole(userModel, userRole));
                assertTrue("User should still have admin role", org.hasRole(userModel, adminRole));
                
                List<OrganizationRoleModel> remainingRoles = org.getUserRolesStream(userModel).collect(Collectors.toList());
                assertThat(remainingRoles, hasSize(1));
                assertEquals("Should only have admin role", "admin", remainingRoles.get(0).getName());
            });
        }
    }

    @Test
    public void testOrganizationRoleSearch() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create multiple roles
            org.addRole("admin").setDescription("Administrator");
            org.addRole("user").setDescription("Regular User");
            org.addRole("manager").setDescription("Team Manager");
            org.addRole("admin-assistant").setDescription("Admin Assistant");
            
            // Test search by name
            List<OrganizationRoleModel> adminRoles = org.searchForRolesStream("admin", 0, 10)
                    .collect(Collectors.toList());
            assertThat(adminRoles, hasSize(2)); // admin and admin-assistant
            
            List<String> adminRoleNames = adminRoles.stream()
                    .map(OrganizationRoleModel::getName)
                    .collect(Collectors.toList());
            assertThat(adminRoleNames, containsInAnyOrder("admin", "admin-assistant"));
            
            // Test pagination
            List<OrganizationRoleModel> limitedRoles = org.searchForRolesStream("admin", 0, 1)
                    .collect(Collectors.toList());
            assertThat(limitedRoles, hasSize(1));
            
            // Test getting all roles
            List<OrganizationRoleModel> allRoles = org.getRolesStream().collect(Collectors.toList());
            assertThat(allRoles, hasSize(4));
        });
    }

    @Test
    public void testOrganizationRoleRemoval() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create roles
            org.addRole("admin");
            OrganizationRoleModel userRole = org.addRole("user");
            org.addRole("manager");
            
            // Verify all roles exist
            assertThat(org.getRolesStream().collect(Collectors.toList()), hasSize(3));
            
            // Remove a role
            boolean removed = org.removeRole(userRole);
            assertTrue("Role should be removed", removed);
            
            // Verify role count decreased
            assertThat(org.getRolesStream().collect(Collectors.toList()), hasSize(2));
            
            // Verify specific role is gone
            OrganizationRoleModel notFound = org.getRole("user");
            assertThat(notFound, is(equalTo(null)));
            
            // Verify other roles still exist
            assertNotNull("Admin role should still exist", org.getRole("admin"));
            assertNotNull("Manager role should still exist", org.getRole("manager"));
        });
    }

    @Test
    public void testOrganizationRoleAttributes() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            OrganizationRoleModel role = org.addRole("custom-role");
            
            // Test single attribute
            role.setSingleAttribute("priority", "high");
            assertEquals("Should get priority attribute", "high", role.getFirstAttribute("priority"));
            
            // Test multi-value attribute
            role.setAttribute("capabilities", List.of("create", "read", "update", "delete"));
            
            List<String> capabilities = role.getAttributeStream("capabilities").collect(Collectors.toList());
            assertThat(capabilities, hasSize(4));
            assertThat(capabilities, containsInAnyOrder("create", "read", "update", "delete"));
            
            // Test getting all attributes
            Map<String, List<String>> allAttributes = role.getAttributes();
            assertThat(allAttributes.keySet(), hasSize(2));
            assertTrue("Should have priority attribute", allAttributes.containsKey("priority"));
            assertTrue("Should have capabilities attribute", allAttributes.containsKey("capabilities"));
            
            // Test updating attribute
            role.setSingleAttribute("priority", "medium");
            assertEquals("Priority should be updated", "medium", role.getFirstAttribute("priority"));
            
            // Test removing attribute
            role.removeAttribute("priority");
            assertThat(role.getFirstAttribute("priority"), is(equalTo(null)));
            
            // Verify other attribute is still there
            assertThat(role.getAttributeStream("capabilities").collect(Collectors.toList()), hasSize(4));
        });
    }

    @Test
    public void testOrganizationRoleValidation() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create a role
            OrganizationRoleModel role1 = org.addRole("unique-role");
            assertNotNull("First role should be created", role1);
            
            // Try to create role with same name (should fail or return existing)
            OrganizationRoleModel role2 = org.getRole("unique-role");
            assertEquals("Should get the same role", role1.getId(), role2.getId());
            
            // Test role name requirements
            try {
                org.addRole("");
                fail("Should not allow empty role name");
            } catch (Exception e) {
                // Expected - empty names should not be allowed
            }
            
            try {
                org.addRole(null);
                fail("Should not allow null role name");
            } catch (Exception e) {
                // Expected - null names should not be allowed
            }
        });
    }

    @Test
    public void testOrganizationRoleHierarchy() {
        createOrganization();
        
        testingClient.server().run(session -> {
            OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = orgProvider.getByAlias("test-org");
            
            // Create role hierarchy: admin -> manager -> user -> viewer
            OrganizationRoleModel adminRole = org.addRole("admin");
            OrganizationRoleModel managerRole = org.addRole("manager");
            OrganizationRoleModel userRole = org.addRole("user");
            OrganizationRoleModel viewerRole = org.addRole("viewer");
            
            // Set up hierarchy
            adminRole.addCompositeRole(managerRole);
            managerRole.addCompositeRole(userRole);
            userRole.addCompositeRole(viewerRole);
            
            // Test transitive role checking
            assertTrue("Admin should have manager role", adminRole.hasRole(managerRole));
            assertTrue("Admin should have user role", adminRole.hasRole(userRole));
            assertTrue("Admin should have viewer role", adminRole.hasRole(viewerRole));
            
            assertTrue("Manager should have user role", managerRole.hasRole(userRole));
            assertTrue("Manager should have viewer role", managerRole.hasRole(viewerRole));
            
            assertTrue("User should have viewer role", userRole.hasRole(viewerRole));
            
            // Test reverse - lower roles should not have higher roles
            assertFalse("Viewer should not have user role", viewerRole.hasRole(userRole));
            assertFalse("User should not have manager role", userRole.hasRole(managerRole));
            assertFalse("Manager should not have admin role", managerRole.hasRole(adminRole));
        });
    }
}
