/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.authorization;

import org.junit.Test;
import org.keycloak.authorization.model.Resource;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceManagementTest extends AbstractPhotozAdminTest {

    @Test
    public void testCreate() throws Exception {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");
        newResource.setType("Resource Type");
        newResource.setIconUri("Resource Icon URI");
        newResource.setUri("Resource URI");

        Response response = newResourceRequest().post(Entity.entity(newResource, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ResourceRepresentation resource = response.readEntity(ResourceRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Resource resourceModel = authorizationProvider.getStoreFactory().getResourceStore().findById(resource.getId(), resourceServer.getId());

            assertNotNull(resourceModel);
            assertEquals(resource.getId(), resourceModel.getId());
            assertEquals("New Resource", resourceModel.getName());
            assertEquals("Resource Type", resourceModel.getType());
            assertEquals("Resource Icon URI", resourceModel.getIconUri());
            assertEquals("Resource URI", resourceModel.getUri());
            assertEquals(resourceServer.getId(), resourceModel.getResourceServer().getId());
        });
    }

    @Test
    public void testUpdate() throws Exception {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");
        newResource.setType("Resource Type");
        newResource.setIconUri("Resource Icon URI");
        newResource.setUri("Resource URI");

        Response response = newResourceRequest().post(Entity.entity(newResource, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        newResource.setName("New Resource Changed");
        newResource.setType("Resource Type Changed");
        newResource.setIconUri("Resource Icon URI Changed");
        newResource.setUri("Resource URI Changed");

        response = newResourceRequest().post(Entity.entity(newResource, MediaType.APPLICATION_JSON_TYPE));

        ResourceRepresentation resource = response.readEntity(ResourceRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Resource resourceModel = authorizationProvider.getStoreFactory().getResourceStore().findById(resource.getId(), resourceServer.getId());

            assertNotNull(resourceModel);
            assertEquals(resource.getId(), resourceModel.getId());
            assertEquals("New Resource Changed", resourceModel.getName());
            assertEquals("Resource Type Changed", resourceModel.getType());
            assertEquals("Resource Icon URI Changed", resourceModel.getIconUri());
            assertEquals("Resource URI Changed", resourceModel.getUri());
            assertEquals(resourceServer.getId(), resourceModel.getResourceServer().getId());
        });
    }

    @Test
    public void testFindById() throws Exception {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");
        newResource.setType("Resource Type");
        newResource.setIconUri("Resource Icon URI");
        newResource.setUri("Resource URI");

        Response response = newResourceRequest().post(Entity.entity(newResource, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ResourceRepresentation resource = response.readEntity(ResourceRepresentation.class);

        response = newResourceRequest(resource.getId()).get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        resource = response.readEntity(ResourceRepresentation.class);

        assertEquals("New Resource", resource.getName());
        assertEquals("Resource Type", resource.getType());
        assertEquals("Resource Icon URI", resource.getIconUri());
        assertEquals("Resource URI", resource.getUri());
    }

    @Test
    public void testDelete() throws Exception {
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");

        Response response = newResourceRequest().post(Entity.entity(newResource, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ResourceRepresentation resource = response.readEntity(ResourceRepresentation.class);

        assertNotNull(resource.getId());

        response = newResourceRequest(resource.getId()).delete();

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        onAuthorizationSession(authorizationProvider -> {
            Resource resourceModel = authorizationProvider.getStoreFactory().getResourceStore().findById(resource.getId(), resourceServer.getId());

            assertNull(resourceModel);
        });
    }

    private Builder newResourceRequest(String... id) {
        String idPathParam = "";

        if (id.length != 0) {
            idPathParam = "/" + id[0];
        }

        return newClient(getClientByClientId("photoz-restful-api"), "/resource-server/resource" + idPathParam);
    }
}
