/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.keycloak.testsuite.console.clients;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 *
 * @author Filip Kiss
 */
//@Ignore
public class ClientSettingsTest extends AbstractClientTest {

    @Test
    public void addNewClientTest() {
        ClientRepresentation newClient = createClientRepresentation("testClient1", "http://example.com/*");
        createClient(newClient);
        assertFlashMessageSuccess();

        clients.navigateTo();
        clients.table().search(newClient.getClientId());
        clients.table().deleteClient(newClient.getClientId());

        assertFlashMessageSuccess();
        assertNull(clients.table().findClient(newClient.getClientId()));
    }

    @Test
    public void addNewClientWithBlankNameTest() {
        ClientRepresentation newClient = createClientRepresentation("", "http://example.com/*");
        createClient(newClient);
        assertFlashMessageDanger();
    }

    @Test
    public void addNewClientWithBlankUriTest() {
        ClientRepresentation newClient = createClientRepresentation("testClient2", null);
        createClient(newClient);
        assertFlashMessageDanger();

        createClient.form().addRedirectUri("http://testUri.com/*");
        createClient.form().save();
        assertFlashMessageSuccess();

        clients.navigateTo();
        clients.table().search(newClient.getClientId());
        clients.table().deleteClient(newClient.getClientId());
        assertFlashMessageSuccess();
        assertNull(clients.table().findClient(newClient.getClientId()));
    }

    @Test
    public void addNewClientWithTwoUriTest() {
        ClientRepresentation newClient = createClientRepresentation("testClient3", null);
        createClient(newClient);
        assertFlashMessageDanger();

        createClient.form().addRedirectUri("http://testUri.com/*");
        createClient.form().addRedirectUri("http://example.com/*");
        createClient.form().save();
        assertFlashMessageSuccess();

        clients.navigateTo();
        clients.table().search(newClient.getClientId());
        clients.table().deleteClient(newClient.getClientId());
        assertFlashMessageSuccess();
        assertNull(clients.table().findClient(newClient.getClientId()));
    }

}
