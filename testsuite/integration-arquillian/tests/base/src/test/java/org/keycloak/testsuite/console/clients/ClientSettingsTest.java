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

import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;
import org.keycloak.testsuite.console.page.clients.ClientSettings;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsBooleanAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsListAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsStringAttributes;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;
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

    public void crudOIDCConfidential() {
        newClient = createClientRepresentation("oidc-confidential", "http://example.test/app/*");
        createClient(newClient);
        assertFlashMessageSuccess();

        clientPage.backToClientsViaBreadcrumb();
        assertCurrentUrlEquals(clientsPage);
        assertEquals(1, clientsPage.table().searchClients(newClient.getClientId()).size());

        // read & verify
        clientsPage.table().clickClient(newClient);
        ClientRepresentation found = clientSettingsPage.form().getValues();
        assertClientSettingsEqual(newClient, found);

        // update & verify
        // TODO change attributes, add redirect uris and weborigins
        // delete
        // TODO
        clientPage.backToClientsViaBreadcrumb();
    }

    public void createOIDCPublic() {
        newClient = createClientRepresentation("oidc-public", "http://example.test/app/*");
        newClient.setPublicClient(true);
        createClient(newClient);
        assertFlashMessageSuccess();

        clientPage.backToClientsViaBreadcrumb();
        assertCurrentUrlEquals(clientsPage);
        assertEquals(1, clientsPage.table().searchClients(newClient.getClientId()).size());
    }

    public void createOIDCBearerOnly() {
        newClient = createClientRepresentation("oidc-bearer-only", "http://example.test/app/*");
        newClient.setBearerOnly(true);
        createClient(newClient);
        assertFlashMessageSuccess();

        clientPage.backToClientsViaBreadcrumb();
        assertCurrentUrlEquals(clientsPage);
        assertEquals(1, clientsPage.table().searchClients(newClient.getClientId()).size());
    }

    @Test
    public void successfulCRUD() {
        crudOIDCConfidential();
        createOIDCPublic();
        createOIDCBearerOnly();
    }

    @Test
    public void invalidSettings() {
        clientsPage.table().createClient();
        createClientPage.form().save();
        assertFlashMessageDanger();

        createClientPage.form().setClientId("test-client");
        createClientPage.form().save();
        assertFlashMessageDanger();
    }

    public void assertClientSettingsEqual(ClientRepresentation c1, ClientRepresentation c2) {
        assertEqualsStringAttributes(c1.getClientId(), c2.getClientId());
        assertEqualsStringAttributes(c1.getName(), c2.getName());
        assertEqualsBooleanAttributes(c1.isEnabled(), c2.isEnabled());
        assertEqualsBooleanAttributes(c1.isConsentRequired(), c2.isConsentRequired());
        assertEqualsBooleanAttributes(c1.isStandardFlowEnabled(), c2.isStandardFlowEnabled());
        assertEqualsBooleanAttributes(c1.isImplicitFlowEnabled(), c2.isImplicitFlowEnabled());
        assertEqualsBooleanAttributes(c1.isDirectAccessGrantsEnabled(), c2.isDirectAccessGrantsEnabled());
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

//    @Test
    public void createInconsistentClient() {
        ClientRepresentation c = createClientRepresentation("inconsistent_client");
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
            ClientRepresentation cr = createClientRepresentation(clientId, "http://example.test/*");
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
