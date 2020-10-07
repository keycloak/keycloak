/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.admin.client.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopePermissionManagementTest extends AbstractPolicyManagementTest {

    @Test
    public void testCreateResourceScopePermission() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Resource  A Scope Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addResource("Resource A");
        representation.addScope("read", "execute");
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);
    }

    @Test
    public void testCreateScopePermission() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Read Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addScope("read", "write");
        representation.addPolicy("Only Marta Policy");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Update Test Scope Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addResource("Resource A");
        representation.addScope("read", "execute");
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.getResources().remove("Resource A");
        representation.addResource("Resource B");
        representation.getScopes().remove("execute");
        representation.getPolicies().remove("Only Marta Policy");

        ScopePermissionsResource permissions = authorization.permissions().scope();
        ScopePermissionResource permission = permissions.findById(representation.getId());

        permission.update(representation);

        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Test Delete Permission");
        representation.addScope("execute");
        representation.addPolicy("Only Marta Policy");

        assertCreated(authorization, representation);

        ScopePermissionsResource permissions = authorization.permissions().scope();

        permissions.findById(representation.getId()).remove();

        ScopePermissionResource removed = permissions.findById(representation.getId());

        try {
            removed.toRepresentation();
            fail("Permission not removed");
        } catch (NotFoundException ignore) {

        }
    }

    @Test
    public void failCreateWithSameName() {
        AuthorizationResource authorization = getClient().authorization();
        ScopePermissionRepresentation permission1 = new ScopePermissionRepresentation();

        permission1.setName("Conflicting Name Permission");
        permission1.addScope("read");
        permission1.addPolicy("Only Marta Policy");

        ScopePermissionsResource permissions = authorization.permissions().scope();

        permissions.create(permission1).close();

        ScopePermissionRepresentation permission2 = new ScopePermissionRepresentation();

        permission2.setName(permission1.getName());

        try (Response response = permissions.create(permission2)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    private void assertCreated(AuthorizationResource authorization, ScopePermissionRepresentation representation) {
        ScopePermissionsResource permissions = authorization.permissions().scope();

        try (Response response = permissions.create(representation)) {
            ScopePermissionRepresentation created = response.readEntity(ScopePermissionRepresentation.class);
            ScopePermissionResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertRepresentation(ScopePermissionRepresentation representation, ScopePermissionResource permission) {
        assertRepresentation(representation, permission.toRepresentation(), () -> permission.resources(), () -> permission.scopes(), () -> permission.associatedPolicies());
    }
}
