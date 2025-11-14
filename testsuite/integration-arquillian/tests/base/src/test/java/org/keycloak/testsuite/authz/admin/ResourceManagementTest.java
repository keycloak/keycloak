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

package org.keycloak.testsuite.authz.admin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ResourceResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceOwnerRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceManagementTest extends AbstractAuthorizationTest {

    @Test
    public void testCreate() {
        ResourceRepresentation newResource = createResource();

        assertEquals("Test Resource", newResource.getName());
        assertEquals("/test/*", newResource.getUri());
        assertEquals("test-resource", newResource.getType());
        assertEquals("icon-test-resource", newResource.getIconUri());

        Map<String, List<String>> attributes = newResource.getAttributes();

        assertEquals(2, attributes.size());

        assertTrue(attributes.containsKey("a"));
        assertTrue(attributes.containsKey("b"));
        assertTrue(attributes.get("a").containsAll(Arrays.asList("a1", "a2", "a3")));
        assertEquals(3, attributes.get("a").size());
        assertTrue(attributes.get("b").containsAll(Arrays.asList("b1")));
        assertEquals(1, attributes.get("b").size());
    }

    @Test
    public void testCreateWithResourceType() {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("test");
        newResource.setDisplayName("display");
        newResource.setType("some-type");

        newResource = doCreateResource(newResource);

        ResourceResource resource = getClientResource().authorization().resources().resource(newResource.getId());

        assertTrue(resource.permissions().isEmpty());
    }

    @Test
    public void testQueryAssociatedPermissions() {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("r1");
        newResource.setType("some-type");
        newResource.addScope("GET");

        newResource = doCreateResource(newResource);

        ResourceResource resource = getClientResource().authorization().resources().resource(newResource.getId());

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

        permission.setName(newResource.getName());
        permission.addResource(newResource.getName());
        permission.addScope("GET");

        getClientResource().authorization().permissions().scope().create(permission);

        assertFalse(resource.permissions().isEmpty());
    }

    @Test
    public void testQueryTypedResourcePermissions() {
        ResourceRepresentation r1 = new ResourceRepresentation();

        r1.setName("r1");
        r1.setType("some-type");
        r1.addScope("GET");

        r1 = doCreateResource(r1);

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

        permission.setName(r1.getName());
        permission.addResource(r1.getName());
        permission.addScope("GET");

        getClientResource().authorization().permissions().scope().create(permission);

        ResourceRepresentation r2 = new ResourceRepresentation();

        r2.setName("r2");
        r2.setType("some-type");
        r2.addScope("GET");

        r2 = doCreateResource(r2);

        permission = new ScopePermissionRepresentation();

        permission.setName(r2.getName());
        permission.addResource(r2.getName());
        permission.addScope("GET");

        getClientResource().authorization().permissions().scope().create(permission);

        ResourceResource resource2 = getClientResource().authorization().resources().resource(r2.getId());
        List<PolicyRepresentation> permissions = resource2.permissions();

        assertEquals(1, permissions.size());
        assertEquals(r2.getName(), permissions.get(0).getName());

        ResourceResource resource1 = getClientResource().authorization().resources().resource(r1.getId());

        permissions = resource1.permissions();

        assertEquals(1, permissions.size());
        assertEquals(r1.getName(), permissions.get(0).getName());
    }

    @Test
    public void testQueryTypedResourcePermissionsForResourceInstances() {
        ResourceRepresentation r1 = new ResourceRepresentation();

        r1.setName("r1");
        r1.setType("some-type");
        r1.addScope("GET");

        r1 = doCreateResource(r1);

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();

        permission.setName(r1.getName());
        permission.addResource(r1.getName());
        permission.addScope("GET");

        getClientResource().authorization().permissions().scope().create(permission);

        ResourceRepresentation r2 = new ResourceRepresentation();

        r2.setName("r2");
        r2.setType("some-type");
        r2.addScope("GET");

        r2 = doCreateResource(r2);

        permission = new ScopePermissionRepresentation();

        permission.setName(r2.getName());
        permission.addResource(r2.getName());
        permission.addScope("GET");

        getClientResource().authorization().permissions().scope().create(permission);

        ResourceRepresentation rInstance = new ResourceRepresentation();

        rInstance.setName("rInstance");
        rInstance.setType("some-type");
        rInstance.setOwner("marta");
        rInstance.addScope("GET", "POST");

        rInstance = doCreateResource(rInstance);

        List<PolicyRepresentation> permissions = getClientResource().authorization().resources().resource(rInstance.getId()).permissions();

        assertEquals(2, permissions.size());

        permission = new ScopePermissionRepresentation();

        permission.setName("POST permission");
        permission.addScope("POST");

        getClientResource().authorization().permissions().scope().create(permission);

        permissions = getClientResource().authorization().resources().resource(rInstance.getId()).permissions();

        assertEquals(3, permissions.size());
    }

    @Test
    public void failCreateWithSameName() {
        ResourceRepresentation newResource = createResource();

        try {
            doCreateResource(newResource);
            fail("Can not create resources with the same name and owner");
        } catch (Exception e) {
            assertEquals(HttpResponseException.class, e.getCause().getClass());
            assertEquals(409, HttpResponseException.class.cast(e.getCause()).getStatusCode());
        }

        newResource.setName(newResource.getName() + " Another");

        newResource = doCreateResource(newResource);

        assertNotNull(newResource.getId());
        assertEquals("Test Resource Another", newResource.getName());
    }

    @Test
    public void failCreateWithSameNameDifferentOwner() {
        ResourceRepresentation martaResource = createResource("Resource A", "marta", null, null, null);
        ResourceRepresentation koloResource = createResource("Resource A", "kolo", null, null, null);

        assertNotNull(martaResource.getId());
        assertNotNull(koloResource.getId());
        assertNotEquals(martaResource.getId(), koloResource.getId());

        assertEquals(2, getClientResource().authorization().resources().findByName(martaResource.getName()).size());

        List<ResourceRepresentation> martaResources = getClientResource().authorization().resources().findByName(martaResource.getName(), "marta");

        assertEquals(1, martaResources.size());
        assertEquals(martaResource.getId(), martaResources.get(0).getId());

        List<ResourceRepresentation> koloResources = getClientResource().authorization().resources().findByName(martaResource.getName(), "kolo");

        assertEquals(1, koloResources.size());
        assertEquals(koloResource.getId(), koloResources.get(0).getId());
    }

    @Test
    public void testUpdate() {
        ResourceRepresentation resource = createResource();

        resource.setType("changed");
        resource.setIconUri("changed");
        resource.setUri("changed");

        Map<String, List<String>> attributes = resource.getAttributes();

        attributes.remove("a");
        attributes.put("c", Arrays.asList("c1", "c2"));
        attributes.put("b", Arrays.asList("changed"));

        resource = doUpdateResource(resource);

        assertEquals("changed", resource.getIconUri());
        assertEquals("changed", resource.getType());
        assertEquals("changed", resource.getUri());

        attributes = resource.getAttributes();

        assertEquals(2, attributes.size());

        assertFalse(attributes.containsKey("a"));
        assertTrue(attributes.containsKey("b"));
        assertTrue(attributes.get("b").containsAll(Arrays.asList("changed")));
        assertEquals(1, attributes.get("b").size());
        assertTrue(attributes.get("c").containsAll(Arrays.asList("c1", "c2")));
        assertEquals(2, attributes.get("c").size());
    }

    @Test(expected = NotFoundException.class)
    public void testDelete() {
        ResourceRepresentation resource = createResource();

        doRemoveResource(resource);

        getClientResource().authorization().resources().resource(resource.getId()).toRepresentation();
    }

    @Test
    public void testAssociateScopes() {
        ResourceRepresentation updated = createResourceWithDefaultScopes();

        assertEquals(3, updated.getScopes().size());

        assertTrue(containsScope("Scope A", updated));
        assertTrue(containsScope("Scope B", updated));
        assertTrue(containsScope("Scope C", updated));
    }

    @Test
    public void testUpdateScopes() {
        ResourceRepresentation resource = createResourceWithDefaultScopes();
        Set<ScopeRepresentation> scopes = new HashSet<>(resource.getScopes());

        assertEquals(3, scopes.size());
        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope B")));

        resource.setScopes(scopes);

        ResourceRepresentation updated = doUpdateResource(resource);

        assertEquals(2, resource.getScopes().size());

        assertFalse(containsScope("Scope B", updated));
        assertTrue(containsScope("Scope A", updated));
        assertTrue(containsScope("Scope C", updated));

        scopes = new HashSet<>(updated.getScopes());

        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope A")));
        assertTrue(scopes.removeIf(scopeRepresentation -> scopeRepresentation.getName().equals("Scope C")));

        updated.setScopes(scopes);

        updated = doUpdateResource(updated);

        assertEquals(0, updated.getScopes().size());
    }

    private ResourceRepresentation createResourceWithDefaultScopes() {
        ResourceRepresentation resource = createResource();

        assertEquals(0, resource.getScopes().size());

        HashSet<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(createScope("Scope A", "").toRepresentation());
        scopes.add(createScope("Scope B", "").toRepresentation());
        scopes.add(createScope("Scope C", "").toRepresentation());

        resource.setScopes(scopes);

        return doUpdateResource(resource);
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

    private ResourceRepresentation createResource() {
        return createResource("Test Resource", null, "/test/*", "test-resource", "icon-test-resource");
    }

    private ResourceRepresentation createResource(String name, String owner, String uri, String type, String iconUri) {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName(name);
        newResource.setUri(uri);
        newResource.setType(type);
        newResource.setIconUri(iconUri);
        newResource.setOwner(owner != null ? new ResourceOwnerRepresentation(owner) : null);

        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put("a", Arrays.asList("a1", "a2", "a3"));
        attributes.put("b", Arrays.asList("b1"));

        newResource.setAttributes(attributes);

        return doCreateResource(newResource);
    }

    protected ResourceRepresentation doCreateResource(ResourceRepresentation newResource) {
        ResourcesResource resources = getClientResource().authorization().resources();

        try (Response response = resources.create(newResource)) {

            int status = response.getStatus();

            if (status != Response.Status.CREATED.getStatusCode()) {
                throw new RuntimeException(new HttpResponseException("Error", status, "", null));
            }

            ResourceRepresentation stored = response.readEntity(ResourceRepresentation.class);

            return resources.resource(stored.getId()).toRepresentation();
        }
    }

    protected ResourceRepresentation doUpdateResource(ResourceRepresentation resource) {
        ResourcesResource resources = getClientResource().authorization().resources();
        ResourceResource existing = resources.resource(resource.getId());

        existing.update(resource);

        return resources.resource(resource.getId()).toRepresentation();
    }

    protected void doRemoveResource(ResourceRepresentation resource) {
        ResourcesResource resources = getClientResource().authorization().resources();
        resources.resource(resource.getId()).remove();
    }
}