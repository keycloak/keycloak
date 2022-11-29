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
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.console.page.clients.scope.ClientScope;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;

/**
 * Test for the "Scope" tab of client (Client role mappings)
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientScopeTest extends AbstractClientTest {

    private ClientRepresentation newClient;
    private ClientRepresentation found;
    
    @Page
    private ClientScope clientScopePage;
    
    @Before
    public void before() {
        newClient = createClientRep(TEST_CLIENT_ID, OIDC);
        testRealmResource().clients().create(newClient).close();
        
        found = findClientByClientId(TEST_CLIENT_ID);
        assertNotNull("Client " + TEST_CLIENT_ID + " was not found.", found);
        clientScopePage.setId(found.getId());
        clientScopePage.navigateTo();
    }
    
    @Test
    @Ignore //phantomjs sometimes doens't enable "Add Selected >>" button when role is selected
    public void clientScopeTest() {
        assertTrue(found.isFullScopeAllowed());
        clientScopePage.scopeForm().setFullScopeAllowed(false);
        assertAlertSuccess();
        
        found = findClientByClientId(TEST_CLIENT_ID);
        assertFalse(found.isFullScopeAllowed());
        assertNull(getAllMappingsRepresentation().getRealmMappings());
        assertNull(getAllMappingsRepresentation().getClientMappings());
        
        clientScopePage.roleForm().addRealmRole("offline_access");//fails with phantomjs
        assertAlertSuccess();
        
        clientScopePage.roleForm().selectClientRole("account");
        clientScopePage.roleForm().addClientRole("view-profile");//fails with phantomjs
        assertAlertSuccess();
        
        found = findClientByClientId(TEST_CLIENT_ID);
        List<RoleRepresentation> realmMappings = getAllMappingsRepresentation().getRealmMappings();
        assertEquals(1, realmMappings.size());
        assertEquals("offline_access", realmMappings.get(0).getName());
        Map<String, ClientMappingsRepresentation> clientMappings = getAllMappingsRepresentation().getClientMappings();
        assertEquals(1, clientMappings.size());
        assertEquals("view-profile", clientMappings.get("account").getMappings().get(0).getName());
        
        clientScopePage.roleForm().removeAssignedRole("offline_access");
        assertAlertSuccess();//fails with phantomjs
        clientScopePage.roleForm().removeAssignedClientRole("view-profile");
        assertAlertSuccess();//fails with phantomjs
        
        assertNull(getAllMappingsRepresentation().getRealmMappings());
        assertNull(getAllMappingsRepresentation().getClientMappings());
    }
    
    private MappingsRepresentation getAllMappingsRepresentation() {
        return testRealmResource().clients().get(found.getId()).getScopeMappings().getAll();
    }
}