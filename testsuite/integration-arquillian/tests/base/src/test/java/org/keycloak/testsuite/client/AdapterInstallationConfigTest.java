/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.client;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdapterInstallationConfigTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;
    private ClientRepresentation client2;
    private ClientRepresentation clientPublic;

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        client = new ClientRepresentation();
        client.setEnabled(true);
        client.setClientId("RegistrationAccessTokenTest");
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        client.setPublicClient(false);
        client.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client.setRootUrl("http://root");
        client = createClient(client);
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        getCleanup().addClientUuid(client.getId());

        client2 = new ClientRepresentation();
        client2.setEnabled(true);
        client2.setClientId("RegistrationAccessTokenTest2");
        client2.setSecret("RegistrationAccessTokenTestClientSecret");
        client2.setPublicClient(false);
        client2.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken");
        client2.setRootUrl("http://root");
        client2 = createClient(client2);
        getCleanup().addClientUuid(client2.getId());

        clientPublic = new ClientRepresentation();
        clientPublic.setEnabled(true);
        clientPublic.setClientId("RegistrationAccessTokenTestPublic");
        clientPublic.setPublicClient(true);
        clientPublic.setRegistrationAccessToken("RegistrationAccessTokenTestRegistrationAccessTokenPublic");
        clientPublic.setRootUrl("http://root");
        clientPublic = createClient(clientPublic);
        getCleanup().addClientUuid(clientPublic.getId());
    }

    @Test
    public void getConfigWithRegistrationAccessToken() throws ClientRegistrationException {
        reg.auth(Auth.token(client.getRegistrationAccessToken()));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);
    }

    @Test
    public void getConfig() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), "RegistrationAccessTokenTestClientSecret"));

        AdapterConfig config = reg.getAdapterConfig(client.getClientId());
        assertNotNull(config);

        assertEquals(suiteContext.getAuthServerInfo().getContextRoot() + "/auth/", config.getAuthServerUrl());
        assertEquals("test", config.getRealm());

        assertEquals(1, config.getCredentials().size());
        assertEquals("RegistrationAccessTokenTestClientSecret", config.getCredentials().get("secret"));

        assertEquals(client.getClientId(), config.getResource());
        if (AUTH_SERVER_SSL_REQUIRED) assertEquals(SslRequired.EXTERNAL.name().toLowerCase(), config.getSslRequired());
    }

    @Test
    public void getConfigMissingSecret() throws ClientRegistrationException {
        reg.auth(null);

        try {
            reg.getAdapterConfig(client.getClientId());
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigWrongClient() throws ClientRegistrationException {
        reg.auth(Auth.client(client.getClientId(), client.getSecret()));

        try {
            reg.getAdapterConfig(client2.getClientId());
            fail("Expected 401");
        } catch (ClientRegistrationException e) {
            assertEquals(401, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getConfigPublicClient() throws ClientRegistrationException {
        reg.auth(null);

        AdapterConfig config = reg.getAdapterConfig(clientPublic.getClientId());
        assertNotNull(config);

        assertEquals("test", config.getRealm());

        assertEquals(0, config.getCredentials().size());

        assertEquals(clientPublic.getClientId(), config.getResource());
        if (AUTH_SERVER_SSL_REQUIRED) assertEquals(SslRequired.EXTERNAL.name().toLowerCase(), config.getSslRequired());
    }

}
