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
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.*;
import org.junit.Test;

import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.*;
import org.keycloak.testsuite.console.page.clients.settings.ClientSettings;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import org.keycloak.testsuite.util.Timer;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class ClientSettingsTest extends AbstractClientTest {

    @Page
    private ClientSettings clientSettingsPage;

    private ClientRepresentation newClient;

    @Test
    public void crudOIDCConfidential() {
        newClient = createOidcClientRep(CONFIDENTIAL, "oidc-confidential", TEST_REDIRECT_URIS);
        createClient(newClient);
        assertAlertSuccess();

        setExpectedWebOrigins(newClient);
        
        // read & verify
        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);
        
        // update & verify
        newClient.setClientId("oidc-confidential-updated");
        newClient.setName("updatedName");
        
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("http://example2.test/app/*");
        redirectUris.add("http://example2.test/app2/*");
        redirectUris.add("http://example3.test/app/*");
        newClient.setRedirectUris(redirectUris);
        
        List<String> webOrigins = new ArrayList<>();
        webOrigins.clear();
        webOrigins.add("http://example2.test");
        webOrigins.add("http://example3.test");
        newClient.setWebOrigins(webOrigins);
        
        clientSettingsPage.form().setClientId("oidc-confidential-updated");
        clientSettingsPage.form().setName("updatedName");
        clientSettingsPage.form().setRedirectUris(redirectUris);
        clientSettingsPage.form().setWebOrigins(webOrigins);
        clientSettingsPage.form().save();
        assertAlertSuccess();
        
        found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);

        // delete
        clientPage.delete();
        assertAlertSuccess();
        found = findClientByClientId(newClient.getClientId());
        assertNull("Deleted client " + newClient.getClientId() + " was found.", found);
    }

    @Test
    public void createOIDCPublic() {
        newClient = createOidcClientRep(PUBLIC, "oidc-public", TEST_REDIRECT_URIS);
        createClient(newClient);
        assertAlertSuccess();

        setExpectedWebOrigins(newClient);
        
        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);
    }
    
    @Test
    public void createOIDCPublicWithoutRedirectURIs() {
        newClient = createOidcClientRep(PUBLIC, "oidc-public");
        newClient.setStandardFlowEnabled(false);
        createClient(newClient);
        assertAlertSuccess();

        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);
    }

    @Test
    public void createOIDCBearerOnly() {
        newClient = createOidcClientRep(BEARER_ONLY, "oidc-bearer-only");
        createClient(newClient);
        assertAlertSuccess();

        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);
    }

    @Test
    public void createSAML() {
        newClient = createSamlClientRep("saml");
        createClient(newClient);
        assertAlertSuccess();

        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        System.out.println("...." + found.isFrontchannelLogout());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);
        assertClientSettingsEqual(newClient, found);
        assertClientSamlAttributes(getSAMLAttributes(), found.getAttributes());
    }
    
    @Test
    public void invalidSettings() {
        clientsPage.table().createClient();
        createClientPage.form().save();
        assertAlertDanger();

        createClientPage.form().setClientId("test-client");
        createClientPage.form().save();
        assertAlertDanger();
    }

//    @Test
    public void createInconsistentClient() {
        ClientRepresentation c = createOidcClientRep(CONFIDENTIAL, "inconsistent_client");
        c.setPublicClient(true);
        c.setBearerOnly(true);

        Response r = clientsPage.clientsResource().create(c);
        r.close();
        clientSettingsPage.setId(getCreatedId(r));

        c = clientSettingsPage.clientResource().toRepresentation();
        assertTrue(c.isBearerOnly());
        assertTrue(c.isPublicClient());
    }

    public void createClients(String clientIdPrefix, int count) {
        for (int i = 0; i < count; i++) {
            String clientId = String.format("%s%02d", clientIdPrefix, i);
            ClientRepresentation cr = createOidcClientRep(CONFIDENTIAL, clientId, "http://example.test/*");
            Timer.time();
            Response r = testRealmResource().clients().create(cr);
            r.close();
            Timer.time("create client");
        }
    }

//    @Test
    public void clientsPagination() {
        createClients("test_client_", 100);
        clientsPage.navigateTo();
        pause(120000);
    }
}
