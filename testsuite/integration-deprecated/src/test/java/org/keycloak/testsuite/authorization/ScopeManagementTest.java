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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Test;
import org.keycloak.authorization.model.Scope;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.IOException;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScopeManagementTest extends AbstractPhotozAdminTest {

    @Test
    public void testCreate() throws Exception {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName("New Scope");
        newScope.setIconUri("Icon URI");

        Response response = newScopeRequest().post(Entity.entity(newScope, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ScopeRepresentation scope = response.readEntity(ScopeRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Scope scopeModel = authorizationProvider.getStoreFactory().getScopeStore().findById(scope.getId(), resourceServer.getId());

            assertNotNull(scopeModel);
            assertEquals(scope.getId(), scopeModel.getId());
            assertEquals("New Scope", scopeModel.getName());
            assertEquals("Icon URI", scopeModel.getIconUri());
            assertEquals(resourceServer.getId(), scopeModel.getResourceServer().getId());
        });
    }

    @Test
    public void testUpdate() throws Exception {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName("New Scope");
        newScope.setIconUri("Icon URI");

        Response response = newScopeRequest().post(Entity.entity(newScope, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        newScope.setName("New Scope Changed");
        newScope.setIconUri("Icon URI Changed");

        response = newScopeRequest().post(Entity.entity(newScope, MediaType.APPLICATION_JSON_TYPE));

        ScopeRepresentation scope = response.readEntity(ScopeRepresentation.class);

        onAuthorizationSession(authorizationProvider -> {
            Scope scopeModel = authorizationProvider.getStoreFactory().getScopeStore().findById(scope.getId(), resourceServer.getId());

            assertNotNull(scopeModel);
            assertEquals(scope.getId(), scopeModel.getId());
            assertEquals("New Scope Changed", scopeModel.getName());
            assertEquals("Icon URI Changed", scopeModel.getIconUri());
            assertEquals(resourceServer.getId(), scopeModel.getResourceServer().getId());
        });
    }

    @Test
    public void testFindById() throws Exception {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName("New Scope");
        newScope.setIconUri("Icon URI");

        Response response = newScopeRequest().post(Entity.entity(newScope, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ScopeRepresentation scope = response.readEntity(ScopeRepresentation.class);

        response = newScopeRequest(scope.getId()).get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());

        scope = response.readEntity(ScopeRepresentation.class);

        assertEquals("New Scope", scope.getName());
        assertEquals("Icon URI", scope.getIconUri());
    }

    @Test
    public void testDelete() throws Exception {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName("New Scope");

        Response response = newScopeRequest().post(Entity.entity(newScope, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());

        ScopeRepresentation scope = response.readEntity(ScopeRepresentation.class);

        assertNotNull(scope.getId());

        response = newScopeRequest(scope.getId()).delete();

        assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());

        onAuthorizationSession(authorizationProvider -> {
            Scope scopeModel = authorizationProvider.getStoreFactory().getScopeStore().findById(scope.getId(), resourceServer.getId());

            assertNull(scopeModel);
        });
    }

    private Builder newScopeRequest(String... id) {
        String idPathParam = "";

        if (id.length != 0) {
            idPathParam = "/" + id[0];
        }

        return newClient(getClientByClientId("photoz-restful-api"), "/resource-server/scope" + idPathParam);
    }
}
