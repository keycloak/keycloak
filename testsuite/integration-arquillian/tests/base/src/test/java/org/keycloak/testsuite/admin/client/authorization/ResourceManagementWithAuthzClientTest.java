/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class ResourceManagementWithAuthzClientTest extends ResourceManagementTest {

    private AuthzClient authzClient;

    @Test
    public void testFindMatchingUri() {
        doCreateResource(new ResourceRepresentation("/*", Collections.emptySet(), "/*", null));
        doCreateResource(new ResourceRepresentation("/resources/*", Collections.emptySet(), "/resources/*", null));
        doCreateResource(new ResourceRepresentation("/resources-a/*", Collections.emptySet(), "/resources-a/*", null));
        doCreateResource(new ResourceRepresentation("/resources-b/{pattern}", Collections.emptySet(), "/resources-b/{pattern}", null));
        doCreateResource(new ResourceRepresentation("/resources-c/{pattern}/*", Collections.emptySet(), "/resources-c/{pattern}/*", null));
        doCreateResource(new ResourceRepresentation("/resources/{pattern}/{pattern}/*", Collections.emptySet(), "/resources/{pattern}/{pattern}/*", null));
        doCreateResource(new ResourceRepresentation("/resources/{pattern}/sub-resources/{pattern}/*", Collections.emptySet(), "/resources/{pattern}/sub-resources/{pattern}/*", null));
        doCreateResource(new ResourceRepresentation("/resources/{pattern}/sub-resource", Collections.emptySet(), "/resources/{pattern}/sub-resources/{pattern}/*", null));

        AuthzClient authzClient = getAuthzClient();

        List<ResourceRepresentation> resources = authzClient.protection().resource().findByMatchingUri("/test");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/*", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources-a/test");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources-a/*", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources/*", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources-b/a");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources-b/{pattern}", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources-c/a/b");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources-c/{pattern}/*", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources/a/b/c");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources/{pattern}/{pattern}/*", resources.get(0).getUri());

        resources = authzClient.protection().resource().findByMatchingUri("/resources/a/sub-resources/c/d");

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("/resources/{pattern}/sub-resources/{pattern}/*", resources.get(0).getUri());
    }

    @Test
    public void testFindDeep() {
        ResourceRepresentation resource1 = new ResourceRepresentation("/*", new HashSet<>());

        resource1.addScope("a", "b", "c");
        resource1.setType("type");

        Map<String, List<String>> attributes = new HashMap<>();

        attributes.put("a", Arrays.asList("a"));
        attributes.put("b", Arrays.asList("b"));
        attributes.put("c", Arrays.asList("c"));

        resource1.setAttributes(attributes);

        resource1.setIconUri("icon");
        resource1.setUris(new HashSet<>(Arrays.asList("/a", "/b", "/c")));

        ResourceRepresentation resource = doCreateResource(resource1);
        AuthzClient authzClient = getAuthzClient();
        List<ResourceRepresentation> representations = authzClient.protection().resource().find(resource.getId(), null, null, null, null, null, false, true,null, null);

        assertEquals(1, representations.size());
        assertEquals(resource.getId(), representations.get(0).getId());
        assertEquals(resource.getName(), representations.get(0).getName());
        assertEquals(resource.getIconUri(), representations.get(0).getIconUri());
        assertThat(resource.getUris(), Matchers.containsInAnyOrder(representations.get(0).getUris().toArray()));
        assertThat(resource.getAttributes().entrySet(), Matchers.containsInAnyOrder(representations.get(0).getAttributes().entrySet().toArray()));
    }

    @Override
    protected ResourceRepresentation doCreateResource(ResourceRepresentation newResource) {
        ResourceRepresentation resource = toResourceRepresentation(newResource);

        AuthzClient authzClient = getAuthzClient();
        ResourceRepresentation response = authzClient.protection().resource().create(resource);

        return toResourceRepresentation(authzClient, response.getId());
    }

    @Override
    protected ResourceRepresentation doUpdateResource(ResourceRepresentation resource) {
        AuthzClient authzClient = getAuthzClient();

        authzClient.protection().resource().update(toResourceRepresentation(resource));

        return toResourceRepresentation(authzClient, resource.getId());
    }

    @Override
    protected void doRemoveResource(ResourceRepresentation resource) {
        getAuthzClient().protection().resource().delete(resource.getId());
    }

    private ResourceRepresentation toResourceRepresentation(AuthzClient authzClient, String id) {
        ResourceRepresentation created = authzClient.protection().resource().findById(id);
        ResourceRepresentation resourceRepresentation = new ResourceRepresentation();

        resourceRepresentation.setId(created.getId());
        resourceRepresentation.setName(created.getName());
        resourceRepresentation.setIconUri(created.getIconUri());
        resourceRepresentation.setUris(created.getUris());
        resourceRepresentation.setType(created.getType());
        resourceRepresentation.setOwner(created.getOwner());
        resourceRepresentation.setScopes(created.getScopes().stream().map(scopeRepresentation -> {
            ScopeRepresentation scope = new ScopeRepresentation();

            scope.setId(scopeRepresentation.getId());
            scope.setName(scopeRepresentation.getName());
            scope.setIconUri(scopeRepresentation.getIconUri());

            return scope;
        }).collect(Collectors.toSet()));

        resourceRepresentation.setAttributes(created.getAttributes());

        return resourceRepresentation;
    }

    private ResourceRepresentation toResourceRepresentation(ResourceRepresentation newResource) {
        ResourceRepresentation resource = new ResourceRepresentation();

        resource.setId(newResource.getId());
        resource.setName(newResource.getName());
        resource.setIconUri(newResource.getIconUri());

        if (newResource.getUris() != null && !newResource.getUris().isEmpty()) {
            resource.setUris(newResource.getUris());
        } else {
            resource.setUri(newResource.getUri());
        }

        resource.setType(newResource.getType());

        if (newResource.getOwner() != null) {
            resource.setOwner(newResource.getOwner().getId());
        }

        resource.setScopes(newResource.getScopes().stream().map(scopeRepresentation -> {
            ScopeRepresentation scope = new ScopeRepresentation();

            scope.setName(scopeRepresentation.getName());
            scope.setIconUri(scopeRepresentation.getIconUri());

            return scope;
        }).collect(Collectors.toSet()));

        resource.setAttributes(newResource.getAttributes());


        return resource;
    }

    private AuthzClient getAuthzClient() {
        if (authzClient == null) {
            authzClient = AuthzClient.create(getClass().getResourceAsStream("/authorization-test/default-keycloak.json"));
        }

        return authzClient;
    }
}