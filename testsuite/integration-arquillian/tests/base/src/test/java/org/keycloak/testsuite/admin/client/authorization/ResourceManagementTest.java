/*
  Copyright 2016 Red Hat, Inc. and/or its affiliates
  and other contributors as indicated by the @author tags.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package org.keycloak.testsuite.admin.client.authorization;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ResourceResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceManagementTest extends AbstractAuthorizationTest {

    @Before
    @Override
    public void onBeforeAuthzTests() {
        super.onBeforeAuthzTests();
        enableAuthorizationServices();
    }

    @Test
    public void testCreate() {
        ResourceRepresentation newResource = createResource().toRepresentation();

        assertEquals("Test Resource", newResource.getName());
        assertEquals("/test/*", newResource.getUri());
        assertEquals("test-resource", newResource.getType());
        assertEquals("icon-test-resource", newResource.getIconUri());
    }

    @Test
    public void testUpdate() {
        ResourceResource resourceResource = createResource();
        ResourceRepresentation resource = resourceResource.toRepresentation();

        resource.setType("changed");
        resource.setIconUri("changed");
        resource.setUri("changed");

        resourceResource.update(resource);

        resource = resourceResource.toRepresentation();

        assertEquals("changed", resource.getIconUri());
        assertEquals("changed", resource.getType());
        assertEquals("changed", resource.getUri());
    }

    @Test(expected = NotFoundException.class)
    public void testDelete() {
        ResourceResource resourceResource = createResource();

        resourceResource.remove();

        resourceResource.toRepresentation();
    }

    @Test
    public void testAssociateScopes() {
        ResourceResource resourceResource = createResourceWithDefaultScopes();
        ResourceRepresentation updated = resourceResource.toRepresentation();

        assertEquals(3, updated.getScopes().size());

        assertTrue(containsScope("Scope A", updated));
        assertTrue(containsScope("Scope B", updated));
        assertTrue(containsScope("Scope C", updated));
    }

    @Test
    public void testUpdateScopes() {
        ResourceResource resourceResource = createResourceWithDefaultScopes();
        ResourceRepresentation resource = resourceResource.toRepresentation();
        Set<ScopeRepresentation> scopes = new HashSet<>(resource.getScopes());

        assertEquals(3, scopes.size());
        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope B")));

        resource.setScopes(scopes);

        resourceResource.update(resource);

        ResourceRepresentation updated = resourceResource.toRepresentation();

        assertEquals(2, resource.getScopes().size());

        assertFalse(containsScope("Scope B", updated));
        assertTrue(containsScope("Scope A", updated));
        assertTrue(containsScope("Scope C", updated));

        scopes = new HashSet<>(updated.getScopes());

        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope A")));
        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope C")));

        updated.setScopes(scopes);

        resourceResource.update(updated);

        updated = resourceResource.toRepresentation();

        assertEquals(0, updated.getScopes().size());
    }

    private ResourceResource createResourceWithDefaultScopes() {
        ResourceResource resourceResource = createResource();
        ResourceRepresentation resource = resourceResource.toRepresentation();

        assertEquals(0, resource.getScopes().size());

        HashSet<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(createScope("Scope A", "").toRepresentation());
        scopes.add(createScope("Scope B", "").toRepresentation());
        scopes.add(createScope("Scope C", "").toRepresentation());

        resource.setScopes(scopes);

        resourceResource.update(resource);

        return resourceResource;
    }

    private boolean containsScope(String scopeName, ResourceRepresentation resource) {
        Set<ScopeRepresentation> scopes = resource.getScopes();

        if (scopes != null) {
            for (ScopeRepresentation scope : scopes) {
                if (scope.getName().equals(scopeName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private ResourceResource createResource() {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("Test Resource");
        newResource.setUri("/test/*");
        newResource.setType("test-resource");
        newResource.setIconUri("icon-test-resource");

        ResourcesResource resources = getClientResource().authorization().resources();

        Response response = resources.create(newResource);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ResourceRepresentation stored = response.readEntity(ResourceRepresentation.class);

        return resources.resource(stored.getId());
    }
}