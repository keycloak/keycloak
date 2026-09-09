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
package org.keycloak.tests.client;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AdapterInstallationConfigTest extends AbstractClientRegistrationTest {

    private ClientRepresentation client;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();

        client = ClientBuilder.create()
                .enabled(true)
                .clientId("RegistrationAccessTokenTest")
                .secret("RegistrationAccessTokenTestClientSecret")
                .publicClient(false)
                .registrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken")
                .rootUrl("http://root")
                .build();
        client = createClient(client);
        client.setSecret("RegistrationAccessTokenTestClientSecret");
        String clientId = client.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientId).remove());
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

        assertEquals(getAuthServerRoot().toString(), config.getAuthServerUrl());
        assertEquals("test", config.getRealm());

        assertEquals(1, config.getCredentials().size());
        assertEquals("RegistrationAccessTokenTestClientSecret", config.getCredentials().get("secret"));

        assertEquals(client.getClientId(), config.getResource());
    }

    @Test
    public void getConfigMissingSecret() {
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
        ClientRepresentation client2 = ClientBuilder.create()
                .enabled(true)
                .clientId("RegistrationAccessTokenTest2")
                .secret("RegistrationAccessTokenTestClientSecret")
                .publicClient(false)
                .registrationAccessToken("RegistrationAccessTokenTestRegistrationAccessToken")
                .rootUrl("http://root")
                .build();
        client2 = createClient(client2);
        String client2Id = client2.getId();
        managedRealm.cleanup().add(r -> r.clients().get(client2Id).remove());
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
        ClientRepresentation clientPublic = ClientBuilder.create()
                .enabled(true)
                .clientId("RegistrationAccessTokenTestPublic")
                .publicClient(true)
                .registrationAccessToken("RegistrationAccessTokenTestRegistrationAccessTokenPublic")
                .rootUrl("http://root")
                .build();
        clientPublic = createClient(clientPublic);
        String clientPublicId = clientPublic.getId();
        managedRealm.cleanup().add(r -> r.clients().get(clientPublicId).remove());

        reg.auth(null);

        AdapterConfig config = reg.getAdapterConfig(clientPublic.getClientId());
        assertNotNull(config);

        assertEquals("test", config.getRealm());

        assertEquals(0, config.getCredentials().size());

        assertEquals(clientPublic.getClientId(), config.getResource());
    }

}
