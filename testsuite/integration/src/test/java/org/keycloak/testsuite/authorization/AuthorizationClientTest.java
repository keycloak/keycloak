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

package org.keycloak.testsuite.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationClientTest extends AbstractPhotozAdminTest {

    @Test
    public void testCreate() throws Exception {
        AuthzClient authzClient = getAuthzClient();
        // create a new resource representation with the information we want
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");
        newResource.setType("urn:hello-world-authz:resources:example");

        newResource.addScope(new ScopeRepresentation("urn:hello-world-authz:scopes:view"));

        ProtectedResource resourceClient = authzClient.protection().resource();
        Set<String> existingResource = resourceClient.findByFilter("name=" + newResource.getName());

        if (!existingResource.isEmpty()) {
            resourceClient.delete(existingResource.iterator().next());
        }

        // create the resource on the server
        RegistrationResponse response = resourceClient.create(newResource);
        String resourceId = response.getId();

        // query the resource using its newly generated id
        ResourceRepresentation resource = resourceClient.findById(resourceId).getResourceDescription();

        assertNotNull(resource.getId());
        assertEquals("New Resource", resource.getName());
    }

    @Test
    public void testUpdate() throws Exception {
        AuthzClient authzClient = getAuthzClient();
        // create a new resource representation with the information we want
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");

        ProtectedResource protectedResource = authzClient.protection().resource();
        ProtectedResource resourceClient = protectedResource;
        Set<String> existingResource = resourceClient.findByFilter("name=" + newResource.getName());

        if (!existingResource.isEmpty()) {
            resourceClient.delete(existingResource.iterator().next());
        }

        // create the resource on the server
        RegistrationResponse response = resourceClient.create(newResource);
        String resourceId = response.getId();

        // query the resource using its newly generated id
        ResourceRepresentation resource = resourceClient.findById(resourceId).getResourceDescription();

        assertNotNull(resource.getId());
        assertEquals("New Resource", resource.getName());

        resource.setType("Changed Type");
        resource.setUri("Changed Uri");
        resource.addScope(new ScopeRepresentation("new-scope"));

        protectedResource.update(resource);

        resource = resourceClient.findById(resourceId).getResourceDescription();

        assertEquals("Changed Type", resource.getType());
        assertEquals("Changed Uri", resource.getUri());

        Set<ScopeRepresentation> scopes = resource.getScopes();

        assertEquals(1, scopes.size());
        assertEquals("new-scope", scopes.iterator().next().getName());

        resource.setScopes(Collections.emptySet());

        protectedResource.update(resource);

        resource = resourceClient.findById(resourceId).getResourceDescription();

        assertEquals(0, resource.getScopes().size());

        resource.setScopes(scopes);

        protectedResource.update(resource);

        resource = resourceClient.findById(resourceId).getResourceDescription();

        assertEquals(1, resource.getScopes().size());
        assertEquals(scopes.iterator().next().getId(), resource.getScopes().iterator().next().getId());
    }

    @Test
    public void testDelete() throws Exception {
        AuthzClient authzClient = getAuthzClient();
        // create a new resource representation with the information we want
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");

        ProtectedResource resourceClient = authzClient.protection().resource();
        Set<String> existingResource = resourceClient.findByFilter("name=" + newResource.getName());

        if (!existingResource.isEmpty()) {
            resourceClient.delete(existingResource.iterator().next());
        }

        // create the resource on the server
        RegistrationResponse response = resourceClient.create(newResource);
        String resourceId = response.getId();

        // query the resource using its newly generated id
        ResourceRepresentation resource = resourceClient.findById(resourceId).getResourceDescription();

        assertNotNull(resource);

        authzClient.protection().resource().delete(resource.getId());

        try {
            resourceClient.findById(resourceId);
            fail();
        } catch (RuntimeException e) {
            HttpResponseException cause = (HttpResponseException) e.getCause();
            assertEquals(404, cause.getStatusCode());
        }
    }

    private AuthzClient getAuthzClient() throws java.io.IOException {
        Configuration configuration = JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/keycloak.json"), Configuration.class);
        return AuthzClient.create(configuration);
    }
}
