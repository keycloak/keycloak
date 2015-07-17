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

package org.keycloak.testsuite.admin.test.client;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Test;
import org.keycloak.testsuite.admin.fragment.FlashMessage;
import org.keycloak.testsuite.admin.model.Client;
import org.keycloak.testsuite.admin.page.settings.ClientPage;


import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.testsuite.admin.AbstractKeycloakTest;

/**
 *
 * @author Filip Kiss
 */
public class AddNewClientTest extends AbstractKeycloakTest<ClientPage> {

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
	@Before
	public void beforeClientTest() {
		navigation.clients();
		page.goToCreateClient();
	}

    @Test
    public void addNewClientTest() {
        Client newClient = new Client("testClient1", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.clients();

        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

    @Test
    public void addNewClientWithBlankNameTest() {
        Client newClient = new Client("", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    @Test
    public void addNewClientWithBlankUriTest() {
        Client newClient = new Client("testClient2", "");
        page.addClient(newClient);
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.clients();
        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

    @Test
    public void addNewClientWithTwoUriTest() {
        Client newClient = new Client("testClient3", "");
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
        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

}
