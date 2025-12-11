/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.authz.admin.permissions;

import java.util.List;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.exportimport.ExportImportTest;

import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExportImportTestFGAPV2 extends ExportImportTest {

    private final String REALM_NAME = "fgap";
    private final String CUSTOM_CLIENT_ID = "imported-permission-client";

    @After
    public void cleanup() {
        removeRealm(REALM_NAME);
    }

    @Test
    public void testCreateRealmWithSwitchOnAndNoAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);
        rep.setAdminPermissionsEnabled(Boolean.TRUE);

        adminClient.realms().create(rep);

        assertTrue(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
    }

    @Test
    public void testCreateRealmWithSwitchOnAndAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);
        rep.setAdminPermissionsEnabled(Boolean.TRUE);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(CUSTOM_CLIENT_ID);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);

        rep.setAdminPermissionsClient(clientRep);
        rep.setClients(List.of(clientRep));

        adminClient.realms().create(rep);

        assertTrue(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client (imported-permission-client) should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());
        assertThat(client.getClientId(), equalTo(CUSTOM_CLIENT_ID));

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
    }

    @Test
    public void testCreateRealmWithSwitchOffAndAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(CUSTOM_CLIENT_ID);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);

        rep.setAdminPermissionsClient(clientRep);
        rep.setClients(List.of(clientRep));

        adminClient.realms().create(rep);

        assertFalse(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client (imported-permission-client) should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());
        assertThat(client.getClientId(), equalTo(CUSTOM_CLIENT_ID));

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());

        // schema should not be available as the realm switch is off
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());
    }

}
