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
package org.keycloak.testsuite.authz.admin;

import java.util.Collections;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ResourcePermissionResource;
import org.keycloak.admin.client.resource.ResourcePermissionsResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionManagementTest extends AbstractPolicyManagementTest {

    @Test
    public void testCreateResourcePermission() {
        AuthorizationResource authorization = getClient().authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Resource A Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addResource("Resource A");
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);
    }

    @Test
    public void testCreateResourceType() {
        AuthorizationResource authorization = getClient().authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Resource A Type Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.setResourceType("test-resource");
        representation.addPolicy("Only Marta Policy");

        assertCreated(authorization, representation);
    }

    @Test
    public void testUpdate() {
        AuthorizationResource authorization = getClient().authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Update Test Resource Permission");
        representation.setDescription("description");
        representation.setDecisionStrategy(DecisionStrategy.CONSENSUS);
        representation.setLogic(Logic.NEGATIVE);
        representation.addResource("Resource A");
        representation.addPolicy("Only Marta Policy", "Only Kolo Policy");

        assertCreated(authorization, representation);

        representation.setName("changed");
        representation.setDescription("changed");
        representation.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        representation.setLogic(Logic.POSITIVE);
        representation.getResources().remove("Resource A");
        representation.addResource("Resource B");
        representation.getPolicies().remove("Only Marta Policy");

        ResourcePermissionsResource permissions = authorization.permissions().resource();
        ResourcePermissionResource permission = permissions.findById(representation.getId());

        permission.update(representation);

        assertRepresentation(representation, permission);

        representation.getResources().clear();
        representation.setResourceType("changed");

        permission.update(representation);

        assertRepresentation(representation, permission);
    }

    @Test
    public void testDelete() {
        AuthorizationResource authorization = getClient().authorization();
        ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();

        representation.setName("Test Delete Permission");
        representation.setResourceType("test-resource");
        representation.addPolicy("Only Marta Policy");

        ResourcePermissionsResource permissions = authorization.permissions().resource();

        try (Response response = permissions.create(representation)) {
            ResourcePermissionRepresentation created = response.readEntity(ResourcePermissionRepresentation.class);

            permissions.findById(created.getId()).remove();

            ResourcePermissionResource removed = permissions.findById(created.getId());

            try {
                removed.toRepresentation();
                fail("Permission not removed");
            } catch (NotFoundException ignore) {

            }
        }
    }

    @Test
    public void failCreateWithSameName() {
        AuthorizationResource authorization = getClient().authorization();
        ResourcePermissionRepresentation permission1 = new ResourcePermissionRepresentation();

        permission1.setName("Conflicting Name Permission");
        permission1.setResourceType("test-resource");
        permission1.addPolicy("Only Marta Policy");

        ResourcePermissionsResource permissions = authorization.permissions().resource();

        permissions.create(permission1).close();

        ResourcePermissionRepresentation permission2 = new ResourcePermissionRepresentation();

        permission2.setName(permission1.getName());

        try (Response response = permissions.create(permission2)) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    private void assertCreated(AuthorizationResource authorization, ResourcePermissionRepresentation representation) {
        ResourcePermissionsResource permissions = authorization.permissions().resource();
        try (Response response = permissions.create(representation)) {
            ResourcePermissionRepresentation created = response.readEntity(ResourcePermissionRepresentation.class);
            ResourcePermissionResource permission = permissions.findById(created.getId());
            assertRepresentation(representation, permission);
        }
    }

    private void assertRepresentation(ResourcePermissionRepresentation representation, ResourcePermissionResource permission) {
        assertRepresentation(representation, permission.toRepresentation(), () -> permission.resources(), () -> Collections.emptyList(), () -> permission.associatedPolicies());
    }
}
