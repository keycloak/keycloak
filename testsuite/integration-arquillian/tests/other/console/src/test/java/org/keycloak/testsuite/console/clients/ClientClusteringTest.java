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
import org.keycloak.testsuite.console.page.clients.clustering.ClientClustering;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientClusteringTest extends AbstractClientTest {

    private ClientRepresentation newClient;
    private ClientRepresentation found;
    
    @Page
    private ClientClustering clientClusteringPage;
    
    @Before
    public void before() {
        newClient = createClientRep(TEST_CLIENT_ID, OIDC);
        testRealmResource().clients().create(newClient).close();
        
        found = findClientByClientId(TEST_CLIENT_ID);
        assertNotNull("Client " + TEST_CLIENT_ID + " was not found.", found);
        clientClusteringPage.setId(found.getId());
        clientClusteringPage.navigateTo();
    }
    
    @Test
    public void basicConfigurationTest() {
        assertTrue(found.getNodeReRegistrationTimeout() == -1);
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("10", "Seconds");
        clientClusteringPage.form().save();
        assertAlertSuccess();
        assertTrue(findClientByClientId(TEST_CLIENT_ID).getNodeReRegistrationTimeout() == 10);
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("10", "Minutes");
        clientClusteringPage.form().save();
        assertAlertSuccess();
        assertTrue(findClientByClientId(TEST_CLIENT_ID).getNodeReRegistrationTimeout() == 600);
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("1", "Hours");
        clientClusteringPage.form().save();
        assertAlertSuccess();
        assertTrue(findClientByClientId(TEST_CLIENT_ID).getNodeReRegistrationTimeout() == 3600);
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("1", "Days");
        clientClusteringPage.form().save();
        assertAlertSuccess();
        assertTrue(findClientByClientId(TEST_CLIENT_ID).getNodeReRegistrationTimeout() == 86400);
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("", "Days");
        clientClusteringPage.form().save();
        assertAlertDanger();
        
        clientClusteringPage.form().setNodeReRegistrationTimeout("text", "Days");
        clientClusteringPage.form().save();
        assertAlertDanger();
    }
    
    @Test
    public void registerNodeTest() {
        clientClusteringPage.form().addNode("new node");
        assertAlertSuccess();
        assertNotNull(findClientByClientId(TEST_CLIENT_ID).getRegisteredNodes().get("new node"));
        
        clientClusteringPage.form().addNode("");
        assertAlertDanger();
    }
}