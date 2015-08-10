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

import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.page.clients.ClientSettings;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsBooleanAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsListAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsStringAttributes;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;

/**
 *
 * @author Filip Kiss
 * @author tkyjovsk
 */
public class ClientSettingsTest extends AbstractClientTest {

    @Page
    private ClientSettings clientSettings;

    private ClientRepresentation newClient;

    public void crudOIDCConfidential() {
        newClient = createClientRepresentation("oidc-confidential", "http://example.test/app/*");
        createClient(newClient);
        assertFlashMessageSuccess();

        client.backToClientsViaBreadcrumb();
        assertCurrentUrl(clients);
        assertEquals(1, clients.table().searchClients(newClient.getClientId()).size());

        // read & verify
        clients.table().clickClient(newClient);
        ClientRepresentation found = clientSettings.form().getValues();
        assertClientSettingsEqual(newClient, found);

        // update & verify
        // TODO change attributes, add redirect uris and weborigins
        // delete
        // TODO
        
        
        client.backToClientsViaBreadcrumb();
    }

    public void createOIDCPublic() {
        newClient = createClientRepresentation("oidc-public", "http://example.test/app/*");
        newClient.setPublicClient(true);
        createClient(newClient);
        assertFlashMessageSuccess();

        client.backToClientsViaBreadcrumb();
        assertCurrentUrl(clients);
        assertEquals(1, clients.table().searchClients(newClient.getClientId()).size());
    }

    public void createOIDCBearerOnly() {
        newClient = createClientRepresentation("oidc-bearer-only", "http://example.test/app/*");
        newClient.setBearerOnly(true);
        createClient(newClient);
        assertFlashMessageSuccess();

        client.backToClientsViaBreadcrumb();
        assertCurrentUrl(clients);
        assertEquals(1, clients.table().searchClients(newClient.getClientId()).size());
    }

    @Test
    public void successfulCRUD() {
        crudOIDCConfidential();
        createOIDCPublic();
        createOIDCBearerOnly();
    }

    @Test
    public void invalidSettings() {
        emptyName();
        noRedirectUris();
    }

    public void emptyName() {
        newClient = createClientRepresentation(null, "http://example.test/*");
        createClient(newClient);
        assertFlashMessageDanger();
        clientSettings.backToClientsViaBreadcrumb();
    }

    public void noRedirectUris() {
        newClient = createClientRepresentation("testClient2");
        createClient(newClient);
        assertFlashMessageDanger();

        createClient.form().addRedirectUri("http://testUri.com/*");
        createClient.form().save();
        assertFlashMessageSuccess();

        clientSettings.form().setRedirectUris(null);
        clientSettings.form().save();
        assertFlashMessageDanger();
    }

    public void assertClientSettingsEqual(ClientRepresentation c1, ClientRepresentation c2) {
        assertEqualsStringAttributes(c1.getClientId(), c2.getClientId());
        assertEqualsStringAttributes(c1.getName(), c2.getName());
        assertEqualsBooleanAttributes(c1.isEnabled(), c2.isEnabled());
        assertEqualsBooleanAttributes(c1.isConsentRequired(), c2.isConsentRequired());
        assertEqualsBooleanAttributes(c1.isDirectGrantsOnly(), c2.isDirectGrantsOnly());
        assertEqualsStringAttributes(c1.getProtocol(), c2.getProtocol());

        assertEqualsBooleanAttributes(c1.isBearerOnly(), c2.isBearerOnly());
        assertEqualsBooleanAttributes(c1.isPublicClient(), c2.isPublicClient());
        assertEqualsBooleanAttributes(c1.isSurrogateAuthRequired(), c2.isSurrogateAuthRequired());

        assertEqualsBooleanAttributes(c1.isFrontchannelLogout(), c2.isFrontchannelLogout());

        assertEqualsBooleanAttributes(c1.isServiceAccountsEnabled(), c2.isServiceAccountsEnabled());
        assertEqualsListAttributes(c1.getRedirectUris(), c2.getRedirectUris());
        assertEqualsStringAttributes(c1.getBaseUrl(), c2.getBaseUrl());
        assertEqualsStringAttributes(c1.getAdminUrl(), c2.getAdminUrl());
        assertEqualsListAttributes(c1.getWebOrigins(), c2.getWebOrigins());
    }

}
