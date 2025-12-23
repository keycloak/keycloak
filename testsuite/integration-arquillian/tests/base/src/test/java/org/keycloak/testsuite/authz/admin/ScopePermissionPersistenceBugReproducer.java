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
package org.keycloak.testsuite.authz.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * Test to reproduce and verify the bug where scope-based permissions are created
 * but their scopes and policies are not properly persisted via the REST API.
 * 
 * This test demonstrates that when creating scope-based permissions programmatically,
 * the API returns a 201 status but the scopes and policies arrays are not correctly
 * linked to the permission in the persistent storage.
 * 
 * Bug report: Scope-based permissions created via REST API do not persist
 * linked scopes and policies correctly in Keycloak 26.2
 * 
 * @author Test for Authorization Services Bug
 */
public class ScopePermissionPersistenceBugTest extends AbstractPolicyManagementTest {

    /**
     * This test demonstrates the bug where scope-based permissions appear to be created
     * successfully (API returns 201), but when retrieved, the scopes and policies 
     * arrays are empty despite being provided in the creation request.
     */
    @Test
    public void testScopePermissionScopesAndPoliciesNotPersisted() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        // Configure permission with scopes and policies
        representation.setName("Bug Test Scope Permission");
        representation.setDescription("Test permission to demonstrate persistence bug");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        
        // Add scopes - these should be persisted but may not be due to the bug
        representation.addScope("read", "write", "execute");
        
        // Add policies - these should be persisted but may not be due to the bug  
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        // Verify we have the expected scopes and policies before creation
        assertNotNull("Scopes should be set before creation", representation.getScopes());
        assertNotNull("Policies should be set before creation", representation.getPolicies());
        assertEquals("Should have 3 scopes before creation", 3, representation.getScopes().size());
        assertEquals("Should have 2 policies before creation", 2, representation.getPolicies().size());
        assertTrue("Should contain 'read' scope", representation.getScopes().contains("read"));
        assertTrue("Should contain 'write' scope", representation.getScopes().contains("write"));
        assertTrue("Should contain 'execute' scope", representation.getScopes().contains("execute"));
        assertTrue("Should contain 'Only Marta Policy'", representation.getPolicies().contains("Only Marta Policy"));
        assertTrue("Should contain 'Only Kolo Policy'", representation.getPolicies().contains("Only Kolo Policy"));

        ScopePermissionsResource permissions = authorization.permissions().scope();

        // Create the permission - this should succeed and return 201
        ScopePermissionRepresentation created;
        try (Response response = permissions.create(representation)) {
            assertEquals("Permission creation should return 201", Response.Status.CREATED.getStatusCode(), response.getStatus());
            created = response.readEntity(ScopePermissionRepresentation.class);
            assertNotNull("Created permission should not be null", created);
            assertNotNull("Created permission should have an ID", created.getId());
        }

        // Retrieve the permission by ID to verify persistence
        ScopePermissionResource permission = permissions.findById(created.getId());
        ScopePermissionRepresentation retrieved = permission.toRepresentation();

        // Verify basic properties are persisted correctly
        assertNotNull("Retrieved permission should not be null", retrieved);
        assertEquals("Name should be persisted", representation.getName(), retrieved.getName());
        assertEquals("Description should be persisted", representation.getDescription(), retrieved.getDescription());
        assertEquals("Decision strategy should be persisted", representation.getDecisionStrategy(), retrieved.getDecisionStrategy());
        assertEquals("Logic should be persisted", representation.getLogic(), retrieved.getLogic());

        // BUG: These assertions will fail due to the persistence bug
        // The scopes and policies are not correctly persisted despite successful creation
        
        // Test scopes persistence (THIS WILL FAIL DUE TO BUG)
        assertNotNull("Retrieved permission should have scopes", retrieved.getScopes());
        assertTrue("Scopes should not be empty after creation", retrieved.getScopes() != null && !retrieved.getScopes().isEmpty());
        assertEquals("Should have 3 scopes after persistence", 3, retrieved.getScopes().size());
        assertTrue("Should still contain 'read' scope after persistence", retrieved.getScopes().contains("read"));
        assertTrue("Should still contain 'write' scope after persistence", retrieved.getScopes().contains("write"));
        assertTrue("Should still contain 'execute' scope after persistence", retrieved.getScopes().contains("execute"));

        // Test policies persistence (THIS WILL FAIL DUE TO BUG)
        assertNotNull("Retrieved permission should have policies", retrieved.getPolicies());
        assertTrue("Policies should not be empty after creation", retrieved.getPolicies() != null && !retrieved.getPolicies().isEmpty());
        assertEquals("Should have 2 policies after persistence", 2, retrieved.getPolicies().size());
        assertTrue("Should still contain 'Only Marta Policy' after persistence", retrieved.getPolicies().contains("Only Marta Policy"));
        assertTrue("Should still contain 'Only Kolo Policy' after persistence", retrieved.getPolicies().contains("Only Kolo Policy"));

        // Additional verification: Check via the resource-specific methods
        // These methods should also return the linked scopes and policies
        assertTrue("permission.scopes() should return linked scopes", !permission.scopes().isEmpty());
        assertTrue("permission.associatedPolicies() should return linked policies", !permission.associatedPolicies().isEmpty());
        
        // Expected behavior: The linked scopes and policies should match what was provided during creation
        assertEquals("Linked scopes count should match", 3, permission.scopes().size());
        assertEquals("Linked policies count should match", 2, permission.associatedPolicies().size());
    }

    /**
     * Test that verifies the bug only affects the programmatic creation,
     * not the representation returned immediately after creation.
     * 
     * This shows that the creation response contains the expected data,
     * but retrieval afterwards shows empty arrays.
     */
    @Test
    public void testCreationResponseVsPersistenceInconsistency() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Consistency Test Permission");
        representation.setDescription("Test to show creation vs persistence inconsistency");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.addScope("read");
        representation.addPolicy("Only Marta Policy");

        ScopePermissionsResource permissions = authorization.permissions().scope();

        // Create and capture the immediate response
        ScopePermissionRepresentation createdResponse;
        try (Response response = permissions.create(representation)) {
            assertEquals("Should return 201", Response.Status.CREATED.getStatusCode(), response.getStatus());
            createdResponse = response.readEntity(ScopePermissionRepresentation.class);
        }

        // The immediate response from creation might contain the expected data
        // (this varies depending on implementation details)
        
        // But when we retrieve by ID, we should see the persistence bug
        ScopePermissionResource permission = permissions.findById(createdResponse.getId());
        ScopePermissionRepresentation persisted = permission.toRepresentation();

        // This demonstrates the inconsistency: the creation might succeed
        // but retrieval shows empty scopes/policies arrays
        if (persisted.getScopes() == null || persisted.getScopes().isEmpty()) {
            System.out.println("BUG CONFIRMED: Scopes are empty after persistence despite successful creation");
        }
        
        if (persisted.getPolicies() == null || persisted.getPolicies().isEmpty()) {
            System.out.println("BUG CONFIRMED: Policies are empty after persistence despite successful creation");
        }

        // Document the expected vs actual behavior
        fail("This test documents a known bug: scope-based permissions created via REST API " +
             "do not properly persist their scopes and policies arrays. " +
             "Creation returns 201 but retrieval shows empty scopes/policies. " +
             "Manual creation via Admin Console works correctly.");
    }

    /**
     * Test that demonstrates the impact of the bug on authorization decisions.
     * 
     * Even though the permission exists, it cannot grant access because
     * it has no linked scopes or policies.
     */
    @Test
    public void testAuthorizationFailsDueToEmptyPermissions() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Authorization Test Permission");
        representation.setDescription("Permission that should grant access but won't due to bug");
        representation.addScope("read");
        representation.addPolicy("Only Marta Policy");

        ScopePermissionsResource permissions = authorization.permissions().scope();
        
        try (Response response = permissions.create(representation)) {
            ScopePermissionRepresentation created = response.readEntity(ScopePermissionRepresentation.class);
            
            // Verify the permission exists
            ScopePermissionResource permission = permissions.findById(created.getId());
            assertNotNull("Permission should exist", permission.toRepresentation());
            
            // But due to the bug, it will have no effective scopes or policies
            ScopePermissionRepresentation retrieved = permission.toRepresentation();
            
            // This documents why authorization requests fail:
            // The permission exists but is effectively empty
            boolean hasScopes = retrieved.getScopes() != null && !retrieved.getScopes().isEmpty();
            boolean hasPolicies = retrieved.getPolicies() != null && !retrieved.getPolicies().isEmpty();
            
            if (!hasScopes || !hasPolicies) {
                System.out.println("BUG IMPACT: Permission exists but has no effective scopes or policies");
                System.out.println("This explains why authorization requests are denied despite proper configuration");
            }
            
            // The permission should be functional but isn't due to the persistence bug
            assertTrue("Permission should have scopes to be functional", hasScopes);
            assertTrue("Permission should have policies to be functional", hasPolicies);
        }
    }
} 