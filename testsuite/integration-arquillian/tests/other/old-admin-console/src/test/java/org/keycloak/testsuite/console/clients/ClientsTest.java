/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.clients.settings.ClientSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;
import static org.keycloak.testsuite.console.clients.AbstractClientTest.createClientRep;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientsTest extends AbstractClientTest {

    @Page
    private ClientSettings clientSettingsPage;
    
    @Before
    public void beforeClientsTest() {
        ClientRepresentation newClient = createClientRep(TEST_CLIENT_ID, OIDC);
        testRealmResource().clients().create(newClient).close();
        
        ClientRepresentation found = findClientByClientId(TEST_CLIENT_ID);
        assertNotNull("Client " + TEST_CLIENT_ID + " was not found.", found);
        clientSettingsPage.setId(found.getId());
    }

    private void create100Clients() {
        List<ClientRepresentation> clients = new ArrayList<>();
        IntStream.range(0, 100)
                .mapToObj(i -> createClientRep(TEST_CLIENT_ID + i, OIDC))
                .forEach(rep -> testRealmResource().clients().create(rep).close());
    }
    
    @Test
    public void clientsCRUD() {
        //create
        clientsPage.table().createClient();
        assertCurrentUrlEquals(createClientPage);
        
        //edit
        clientsPage.navigateTo();
        clientsPage.table().editClient(TEST_CLIENT_ID);
        assertEquals(TEST_CLIENT_ID, clientSettingsPage.form().getClientId());
        
        //delete
        clientsPage.navigateTo();
        clientsPage.table().deleteClient(TEST_CLIENT_ID);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        
        ClientRepresentation found = findClientByClientId(TEST_CLIENT_ID);
        assertNull("Deleted client " + TEST_CLIENT_ID + " was found.", found);
    }


    @Test
    public void clientsNavigationTest() {
        //create 100 clients
        create100Clients();
        String firstPageClient = TEST_CLIENT_ID + 0;
        String secondPageClient = TEST_CLIENT_ID + 22;
        String thirdPageClient = TEST_CLIENT_ID + 41;

        //edit on the 2nd page then go back
        clientsPage.navigateTo();
        clientsPage.table().clickNextPage();
        clientsPage.table().editClient(secondPageClient);
        assertEquals(secondPageClient, clientSettingsPage.form().getClientId());

        //go to the main page and delete
        clientsPage.navigateTo();
        clientsPage.table().clickPrevPage();
        clientsPage.table().deleteClient(TEST_CLIENT_ID);
        modalDialog.confirmDeletion();
        ClientRepresentation found = findClientByClientId(TEST_CLIENT_ID);
        assertNull("Deleted client " + TEST_CLIENT_ID + " was found.", found);

        // go forward two pages then main page
        clientsPage.navigateTo();
        clientsPage.table().clickNextPage();
        clientsPage.table().clickNextPage();
        clientsPage.table().editClient(thirdPageClient);
        assertEquals(thirdPageClient, clientSettingsPage.form().getClientId());
        clientsPage.navigateTo();

        clientsPage.table().clickFirstPage();
        clientsPage.table().editClient(firstPageClient);
        assertEquals(firstPageClient, clientSettingsPage.form().getClientId());
        clientsPage.navigateTo();

    }
}