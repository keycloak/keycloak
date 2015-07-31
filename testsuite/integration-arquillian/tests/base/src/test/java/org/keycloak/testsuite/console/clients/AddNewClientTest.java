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

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.testsuite.console.page.fragment.FlashMessage;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.AbstractAdminConsoleTest;
import org.keycloak.testsuite.console.page.clients.Clients;

/**
 *
 * @author Filip Kiss
 */
public class AddNewClientTest extends AbstractAdminConsoleTest {

    @Page
    private Clients page;

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Before
    public void beforeClientTest() {
        navigation.clients();
    }

    private ClientRepresentation createClient(String clientId, String redirectUri) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        if (redirectUri != null) {
            List<String> redirectUris = new ArrayList<>();
            redirectUris.add(redirectUri);
            client.setRedirectUris(redirectUris);
        }
        client.setEnabled(true);
        client.setBearerOnly(false);
        client.setDirectGrantsOnly(false);
        client.setPublicClient(false);
        return client;
    }

    @Test
    public void addNewClientTest() {
        ClientRepresentation newClient = createClient("testClient1", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.clients();

        page.deleteClient(newClient.getClientId());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getClientId()));
    }

    @Test
    public void addNewClientWithBlankNameTest() {
        ClientRepresentation newClient = createClient("", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    @Test
    public void addNewClientWithBlankUriTest() {
        ClientRepresentation newClient = createClient("testClient2", null);
        page.addClient(newClient);
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.clients();
        page.deleteClient(newClient.getClientId());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getClientId()));
    }

    @Test
    public void addNewClientWithTwoUriTest() {
        ClientRepresentation newClient = createClient("testClient3", null);
        page.addClient(newClient);
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.addUri("http://example.com/*");

        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.clients();
        page.deleteClient(newClient.getClientId());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getClientId()));
    }

}
