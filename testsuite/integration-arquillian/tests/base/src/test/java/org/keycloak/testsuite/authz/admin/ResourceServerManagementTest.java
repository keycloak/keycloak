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

package org.keycloak.testsuite.authz.admin;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void testImportSettingsToDifferentClient() throws Exception {
        ClientsResource clientsResource = testRealmResource().clients();
        ClientRepresentation clientRep = JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/client-with-authz-settings.json"), ClientRepresentation.class);
        clientRep.setClientId(KeycloakModelUtils.generateId());
        clientsResource.create(clientRep).close();
        List<ClientRepresentation> clients = clientsResource.findByClientId(clientRep.getClientId());
        assertFalse(clients.isEmpty());
        String clientId = clients.get(0).getId();
        AuthorizationResource authorization = clientsResource.get(clientId).authorization();
        ResourceServerRepresentation settings = authorization.exportSettings();
        assertEquals(PolicyEnforcementMode.PERMISSIVE, settings.getPolicyEnforcementMode());
        assertEquals(DecisionStrategy.UNANIMOUS, settings.getDecisionStrategy());
        assertFalse(authorization.resources().findByName("Resource 1").isEmpty());
        assertFalse(authorization.resources().findByName("Resource 15").isEmpty());
        assertFalse(authorization.resources().findByName("Resource 20").isEmpty());
        assertNotNull(authorization.permissions().resource().findByName("Resource 15 Permission"));
        assertNotNull(authorization.policies().role().findByName("Resource 1 Policy"));
        settings.getPolicies().removeIf(p -> "js".equals(p.getType()));

        ClientRepresentation anotherClientRep = ClientBuilder.create().clientId(KeycloakModelUtils.generateId()).secret("secret").authorizationServicesEnabled(true).serviceAccount().enabled(true).build();
        clientsResource.create(anotherClientRep).close();
        clients = clientsResource.findByClientId(anotherClientRep.getClientId());
        assertFalse(clients.isEmpty());
        ClientRepresentation anotherClient = clients.get(0);
        authorization = clientsResource.get(anotherClient.getId()).authorization();
        authorization.importSettings(settings);
        ResourceServerRepresentation anotherSettings = authorization.exportSettings();
        assertEquals(PolicyEnforcementMode.PERMISSIVE, anotherSettings.getPolicyEnforcementMode());
        assertEquals(DecisionStrategy.UNANIMOUS, anotherSettings.getDecisionStrategy());
        assertFalse(authorization.resources().findByName("Resource 1").isEmpty());
        assertFalse(authorization.resources().findByName("Resource 15").isEmpty());
        assertFalse(authorization.resources().findByName("Resource 20").isEmpty());
        assertNotNull(authorization.permissions().resource().findByName("Resource 15 Permission"));
        assertNotNull(authorization.policies().role().findByName("Resource 1 Policy"));
    }

    @Test
    public void testExportSettings() throws Exception {
        ClientsResource clientsResource = testRealmResource().clients();
        ClientRepresentation clientRep = JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/client-with-authz-settings.json"), ClientRepresentation.class);
        clientRep.setClientId(KeycloakModelUtils.generateId());
        clientsResource.create(clientRep).close();
        List<ClientRepresentation> clients = clientsResource.findByClientId(clientRep.getClientId());
        assertFalse(clients.isEmpty());
        String clientId = clients.get(0).getId();
        AuthorizationResource authorization = clientsResource.get(clientId).authorization();
        ResourceServerRepresentation settings = authorization.exportSettings();
        assertFalse(settings.getResources().stream().map(ResourceRepresentation::getId).anyMatch(Objects::nonNull));
        assertFalse(settings.getScopes().stream().map(ScopeRepresentation::getId).anyMatch(Objects::nonNull));
        assertFalse(settings.getPolicies().stream().map(PolicyRepresentation::getId).anyMatch(Objects::nonNull));
    }
}