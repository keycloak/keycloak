/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import java.util.List;

import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.util.AdminEventPaths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ClientTest extends AbstractClientTest {

    public static void assertEqualClients(ClientRepresentation expected, ClientRepresentation actual) {
        assertEquals(expected.getClientId(), actual.getClientId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getBaseUrl(), actual.getBaseUrl());

        assertTrue(expected.getRedirectUris().containsAll(actual.getRedirectUris()));
        assertTrue(expected.getWebOrigins().containsAll(actual.getWebOrigins()));
        assertEquals(expected.getRegisteredNodes(), actual.getRegisteredNodes());
    }

    @Test
    public void testCreateClient() {
        createOidcClient("foo");
        ClientRepresentation found = findClientRepresentation("foo");
        assertEquals("foo", found.getName());
    }

    @Test
    public void testDeleteClient() {
        String clientDbId = createOidcClient("deleteMe");

        ClientResource clientRsc = findClientResource("deleteMe");
        assertNotNull(clientRsc);
        clientRsc.remove();
        assertNull(findClientResource("deleteMe"));

        assertAdminEvents.assertEvent(getRealmId(), OperationType.DELETE, AdminEventPaths.clientResourcePath(clientDbId), ResourceType.CLIENT);
    }

    @Test
    public void testUpdateClient() {
        createOidcClient("updateMe");
        ClientRepresentation clientRep = findClientRepresentation("updateMe");
        assertEquals("updateMe", clientRep.getName());

        clientRep.setName("iWasUpdated");
        findClientResource("updateMe").update(clientRep);

        ClientRepresentation updatedClient = findClientRepresentation("iWasUpdated");
        assertNotNull(updatedClient);
        assertEquals("updateMe", updatedClient.getClientId());
        assertEquals("iWasUpdated", updatedClient.getName());

        // Assert admin event
        ClientRepresentation expectedClientRep = new ClientRepresentation();
        expectedClientRep.setClientId("updateMe");
        expectedClientRep.setName("iWasUpdated");
        assertAdminEvents.assertEvent(getRealmId(), OperationType.UPDATE, AdminEventPaths.clientResourcePath(clientRep.getId()), expectedClientRep, ResourceType.CLIENT);
    }

    @Test
    public void testGetAllClients() {
        List<ClientRepresentation> allClients = testRealmResource().clients().findAll();
        assertNotNull(allClients);
        assertFalse(allClients.isEmpty());
    }

    @Test
    public void getClientByIdTest() {
        createOidcClient("byidclient");
        ClientRepresentation rep = findClientRepresentation("byidclient");
        ClientRepresentation gotById = testRealmResource().clients().get(rep.getId()).toRepresentation();
        assertEqualClients(rep, gotById);
    }

}
