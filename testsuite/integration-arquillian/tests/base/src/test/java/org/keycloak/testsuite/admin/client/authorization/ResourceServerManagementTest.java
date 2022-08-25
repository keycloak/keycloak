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

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.util.JsonSerialization;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.NotFoundException;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourceServerManagementTest extends AbstractAuthorizationTest {

    @Test
    public void testCreateAndDeleteResourceServer() throws Exception {
        ClientsResource clientsResource = testRealmResource().clients();

        clientsResource.create(JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/client-with-authz-settings.json"), ClientRepresentation.class)).close();

        List<ClientRepresentation> clients = clientsResource.findByClientId("authz-client");

        assertFalse(clients.isEmpty());

        String clientId = clients.get(0).getId();
        AuthorizationResource settings = clientsResource.get(clientId).authorization();

        assertEquals(PolicyEnforcementMode.PERMISSIVE, settings.exportSettings().getPolicyEnforcementMode());
        assertEquals(DecisionStrategy.UNANIMOUS, settings.exportSettings().getDecisionStrategy());

        assertFalse(settings.resources().findByName("Resource 1").isEmpty());
        assertFalse(settings.resources().findByName("Resource 15").isEmpty());
        assertFalse(settings.resources().findByName("Resource 20").isEmpty());

        assertNotNull(settings.permissions().resource().findByName("Resource 15 Permission"));
        assertNotNull(settings.policies().role().findByName("Resource 1 Policy"));

        clientsResource.get(clientId).remove();

        clients = clientsResource.findByClientId("authz-client");

        assertTrue(clients.isEmpty());
    }

    @Test
    public void testInvalidRequestWhenCallingAuthzEndpoints() throws Exception {
        ClientsResource clientsResource = testRealmResource().clients();
        ClientRepresentation clientRepresentation = JsonSerialization.readValue(
                getClass().getResourceAsStream("/authorization-test/client-with-authz-settings.json"),
                ClientRepresentation.class);

        clientRepresentation.setAuthorizationServicesEnabled(false);
        clientRepresentation.setAuthorizationSettings(null);

        clientsResource.create(clientRepresentation).close();

        List<ClientRepresentation> clients = clientsResource.findByClientId("authz-client");

        assertFalse(clients.isEmpty());

        String clientId = clients.get(0).getId();

        try {
            clientsResource.get(clientId).authorization().getSettings();
            fail("Should fail, authorization not enabled");
        } catch (NotFoundException nfe) {
            // expected
        }
    }
}