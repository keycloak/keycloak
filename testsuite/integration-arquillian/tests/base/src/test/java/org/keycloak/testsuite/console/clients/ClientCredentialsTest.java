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
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.testsuite.console.clients.AbstractClientTest.createOidcClientRep;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.CONFIDENTIAL;
import org.keycloak.testsuite.console.page.clients.credentials.ClientCredentials;
import org.keycloak.testsuite.console.page.clients.credentials.ClientCredentialsGeneratePrivateKeys;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ClientCredentialsTest extends AbstractClientTest {

    private ClientRepresentation newClient;
    
    @Page
    private ClientCredentials clientCredentials;
    @Page
    private ClientCredentialsGeneratePrivateKeys generatePrivateKeysPage;
    
    @Before
    public void beforeClientsTest() {
        newClient = createOidcClientRep(CONFIDENTIAL, TEST_CLIENT_ID, TEST_REDIRECT_URIS);
        testRealmResource().clients().create(newClient).close();
        
        ClientRepresentation found = findClientByClientId(TEST_CLIENT_ID);
        assertNotNull("Client " + TEST_CLIENT_ID + " was not found.", found);
        clientCredentials.setId(found.getId());
        clientCredentials.navigateTo();
    }
    
    @Test
    public void regenerateSecret() {
        clientCredentials.form().selectClientIdAndSecret();
        clientCredentials.form().regenerateSecret();
        assertFlashMessageSuccess();
    }
    
    @Test
    public void regenerateRegistrationAccessToken() {
        clientCredentials.form().regenerateRegistrationAccessToken();
        assertFlashMessageSuccess();
    }
    
    @Test
    public void generateNewKeysAndCert() {
        generatePrivateKeysPage.setId(clientCredentials.getId());
        clientCredentials.form().selectSignedJwt();
        clientCredentials.form().generateNewKeysAndCert();
        assertCurrentUrlEquals(generatePrivateKeysPage);
        
        generatePrivateKeysPage.generateForm().setKeyPassword("pass");
        generatePrivateKeysPage.generateForm().setStorePassword("pass2");
        generatePrivateKeysPage.generateForm().clickGenerateAndDownload();
        
        assertFlashMessageSuccess();
    }
}
