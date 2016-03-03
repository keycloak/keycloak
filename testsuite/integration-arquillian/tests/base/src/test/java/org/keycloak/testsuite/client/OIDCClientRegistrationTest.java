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

import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri("http://root");
        client.setRedirectUris(Collections.singletonList("http://redirect"));

        OIDCClientRepresentation response = reg.oidc().create(client);

        return response;
    }

    @Test
    public void createClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();

        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientId());
        assertNull(response.getClientSecretExpiresAt());
        assertNotNull(response.getRegistrationClientUri());
        assertEquals("RegistrationAccessTokenTest", response.getClientName());
        assertEquals("http://root", response.getClientUri());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://redirect", response.getRedirectUris().get(0));
        assertEquals(Arrays.asList("code", "none"), response.getResponseTypes());
        assertEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes());
    }

    @Test
    public void getClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        assertNotNull(rep);
        assertNotEquals(response.getRegistrationAccessToken(), rep.getRegistrationAccessToken());
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList("code", "none"), response.getResponseTypes()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes()));
    }

    @Test
    public void updateClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        response.setRedirectUris(Collections.singletonList("http://newredirect"));
        response.setResponseTypes(Arrays.asList("code", "id_token token", "code id_token token"));
        response.setGrantTypes(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.PASSWORD));

        OIDCClientRepresentation updated = reg.oidc().update(response);

        assertTrue(CollectionUtil.collectionEquals(Collections.singletonList("http://newredirect"), updated.getRedirectUris()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.PASSWORD), updated.getGrantTypes()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.CODE, OIDCResponseType.NONE, OIDCResponseType.ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token"), updated.getResponseTypes()));
    }

    @Test
    public void updateClientError() throws ClientRegistrationException {
        try {
            OIDCClientRepresentation response = create();
            reg.auth(Auth.token(response));
            response.setResponseTypes(Arrays.asList("code", "tokenn"));
            reg.oidc().update(response);
            fail("Not expected to end with success");
        } catch (ClientRegistrationException cre) {
        }
    }

    @Test
    public void deleteClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        reg.oidc().delete(response);
    }

}
