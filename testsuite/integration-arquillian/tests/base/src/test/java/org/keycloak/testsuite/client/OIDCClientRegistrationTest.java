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
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;

import java.util.*;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    private static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        testRealms.get(0).setPrivateKey(PRIVATE_KEY);
        testRealms.get(0).setPublicKey(PUBLIC_KEY);
    }

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri("http://root");
        client.setRedirectUris(Collections.singletonList("http://redirect"));
        return client;
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = createRep();

        OIDCClientRepresentation response = reg.oidc().create(client);

        return response;
    }

    private void assertCreateFail(OIDCClientRepresentation client, int expectedStatusCode) {
        assertCreateFail(client, expectedStatusCode, null);
    }

    private void assertCreateFail(OIDCClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            reg.oidc().create(client);
            Assert.fail("Not expected to successfuly register client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assert.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue("Error response doesn't contain expected text", httpEx.getErrorResponse().contains(expectedErrorContains));
            }
        }
    }

    // KEYCLOAK-3421
    @Test
    public void createClientWithUriFragment() {
        OIDCClientRepresentation client = createRep();
        client.setRedirectUris(Arrays.asList("http://localhost/auth", "http://localhost/auth#fragment", "http://localhost/auth*"));

        assertCreateFail(client, 400, "URI fragment");
    }

    @Test
    public void createClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();

        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertEquals(0, response.getClientSecretExpiresAt().intValue());
        assertNotNull(response.getRegistrationClientUri());
        assertEquals("RegistrationAccessTokenTest", response.getClientName());
        assertEquals("http://root", response.getClientUri());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://redirect", response.getRedirectUris().get(0));
        assertEquals(Arrays.asList("code", "none"), response.getResponseTypes());
        assertEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        Assert.assertNull(response.getUserinfoSignedResponseAlg());
    }

    @Test
    public void getClient() throws ClientRegistrationException {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        assertNotNull(rep);
        assertEquals(response.getRegistrationAccessToken(), rep.getRegistrationAccessToken());
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList("code", "none"), response.getResponseTypes()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes()));
        assertNotNull(response.getClientSecret());
        assertEquals(0, response.getClientSecretExpiresAt().intValue());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
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

    @Test
    public void testSignaturesRequired() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setUserinfoSignedResponseAlg(Algorithm.RS256.toString());
        clientRep.setRequestObjectSigningAlg(Algorithm.RS256.toString());

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals(Algorithm.RS256.toString(), response.getUserinfoSignedResponseAlg());
        Assert.assertEquals(Algorithm.RS256.toString(), response.getRequestObjectSigningAlg());
        Assert.assertNotNull(response.getClientSecret());

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        Assert.assertEquals(config.getUserInfoSignedResponseAlg(), Algorithm.RS256);
        Assert.assertEquals(config.getRequestObjectSignatureAlg(), Algorithm.RS256);
    }

    @Test
    public void createClientImplicitFlow() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();

        // create implicitFlow client and assert it's public client
        clientRep.setResponseTypes(Arrays.asList("id_token token"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        String clientId = response.getClientId();
        ClientRepresentation kcClientRep = getKeycloakClient(clientId);
        Assert.assertTrue(kcClientRep.isPublicClient());

        // Update client to hybrid and check it's not public client anymore
        reg.auth(Auth.token(response));
        response.setResponseTypes(Arrays.asList("id_token token", "code id_token", "code"));
        reg.oidc().update(response);

        kcClientRep = getKeycloakClient(clientId);
        Assert.assertFalse(kcClientRep.isPublicClient());
    }

    private ClientRepresentation getKeycloakClient(String clientId) {
        return ApiUtil.findClientByClientId(adminClient.realms().realm(REALM_NAME), clientId).toRepresentation();
    }


}
