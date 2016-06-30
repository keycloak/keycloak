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

import org.junit.After;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ResourceScopeResource;
import org.keycloak.admin.client.resource.ResourceScopesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.admin.client.AbstractClientTest;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractAuthorizationTest extends AbstractClientTest {

    protected static final String RESOURCE_SERVER_CLIENT_ID = "test-resource-server";

    @Before
    public void onBeforeAuthzTests() {
        createOidcClient(RESOURCE_SERVER_CLIENT_ID);

        ClientRepresentation resourceServer = getResourceServer();

        assertEquals(RESOURCE_SERVER_CLIENT_ID, resourceServer.getName());
        assertFalse(resourceServer.getAuthorizationServicesEnabled());
    }

    @After
    public void onAfterAuthzTests() {
        getClientResource().remove();
    }

    protected ClientResource getClientResource() {
        return findClientResource(RESOURCE_SERVER_CLIENT_ID);
    }

    protected ClientRepresentation getResourceServer() {
        return findClientRepresentation(RESOURCE_SERVER_CLIENT_ID);
    }

    protected void enableAuthorizationServices() {
        ClientRepresentation resourceServer = getResourceServer();

        resourceServer.setAuthorizationServicesEnabled(true);
        resourceServer.setServiceAccountsEnabled(true);

        getClientResource().update(resourceServer);
    }

    protected ResourceScopeResource createDefaultScope() {
        return createScope("Test Scope", "Scope Icon");
    }

    protected ResourceScopeResource createScope(String name, String iconUri) {
        ScopeRepresentation newScope = new ScopeRepresentation();

        newScope.setName(name);
        newScope.setIconUri(iconUri);

        ResourceScopesResource resources = getClientResource().authorization().scopes();

        Response response = resources.create(newScope);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        ScopeRepresentation stored = response.readEntity(ScopeRepresentation.class);

        return resources.scope(stored.getId());
    }
}
