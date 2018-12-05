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
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Errors;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.KeycloakModelUtils;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    private static final String PRIVATE_KEY = "MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=";
    private static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testRealm = testRealms.get(0);
        testRealm.setPrivateKey(PRIVATE_KEY);
        testRealm.setPublicKey(PUBLIC_KEY);

        ClientRepresentation samlApp = KeycloakModelUtils.createClient(testRealm, "saml-client");
        samlApp.setSecret("secret");
        samlApp.setServiceAccountsEnabled(true);
        samlApp.setDirectAccessGrantsEnabled(true);
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

    private void assertGetFail(String clientId, int expectedStatusCode, String expectedErrorContains) {
        try {
            reg.oidc().get(clientId);
            Assert.fail("Not expected to successfully get client");
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
        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;
        try {
            clientRep = createRep();
            clientRep.setUserinfoSignedResponseAlg(Algorithm.ES256.toString());
            clientRep.setRequestObjectSigningAlg(Algorithm.ES256.toString());

            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.ES256.toString(), response.getUserinfoSignedResponseAlg());
            Assert.assertEquals(Algorithm.ES256.toString(), response.getRequestObjectSigningAlg());
            Assert.assertNotNull(response.getClientSecret());

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(config.getUserInfoSignedResponseAlg(), Algorithm.ES256);
            Assert.assertEquals(config.getRequestObjectSignatureAlg(), Algorithm.ES256);

            // update (ES256 to PS256)
            clientRep.setUserinfoSignedResponseAlg(Algorithm.PS256.toString());
            clientRep.setRequestObjectSigningAlg(Algorithm.PS256.toString());
            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.PS256.toString(), response.getUserinfoSignedResponseAlg());
            Assert.assertEquals(Algorithm.PS256.toString(), response.getRequestObjectSigningAlg());

            // keycloak representation
            kcClient = getClient(response.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(config.getUserInfoSignedResponseAlg(), Algorithm.PS256);
            Assert.assertEquals(config.getRequestObjectSignatureAlg(), Algorithm.PS256);
        } finally {
            // back to RS256 for other tests
            clientRep.setUserinfoSignedResponseAlg(Algorithm.RS256.toString());
            clientRep.setRequestObjectSigningAlg(Algorithm.RS256.toString());
            response = reg.oidc().create(clientRep);
        }
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

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
    @Test
    public void testMtlsHoKTokenEnabled() throws Exception {
        // create (no specification)
        OIDCClientRepresentation clientRep = createRep();

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals(Boolean.FALSE, response.getTlsClientCertificateBoundAccessTokens());
        Assert.assertNotNull(response.getClientSecret());

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(!config.isUseMtlsHokToken());

        // update (true)
        reg.auth(Auth.token(response));
        response.setTlsClientCertificateBoundAccessTokens(Boolean.TRUE);
        OIDCClientRepresentation updated = reg.oidc().update(response);
        assertTrue(updated.getTlsClientCertificateBoundAccessTokens().booleanValue());

        // Test Keycloak representation
        kcClient = getClient(updated.getClientId());
        config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(config.isUseMtlsHokToken());

        // update (false)
        reg.auth(Auth.token(updated));
        updated.setTlsClientCertificateBoundAccessTokens(Boolean.FALSE);
        OIDCClientRepresentation reUpdated = reg.oidc().update(updated);
        assertTrue(!reUpdated.getTlsClientCertificateBoundAccessTokens().booleanValue());

        // Test Keycloak representation
        kcClient = getClient(reUpdated.getClientId());
        config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(!config.isUseMtlsHokToken());

    }

    @Test
    public void testIdTokenEncryptedResponse() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
             // create (no specification)
             OIDCClientRepresentation clientRep = createRep();

             response = reg.oidc().create(clientRep);
             Assert.assertEquals(Boolean.FALSE, response.getTlsClientCertificateBoundAccessTokens());
             Assert.assertNotNull(response.getClientSecret());

             // Test Keycloak representation
             ClientRepresentation kcClient = getClient(response.getClientId());
             OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertNull(config.getIdTokenEncryptedResponseAlg());
             Assert.assertNull(config.getIdTokenEncryptedResponseEnc());

             // update (alg RSA1_5, enc A128CBC-HS256)
             reg.auth(Auth.token(response));
             response.setIdTokenEncryptedResponseAlg(JWEConstants.RSA1_5);
             response.setIdTokenEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
             updated = reg.oidc().update(response);
             Assert.assertEquals(JWEConstants.RSA1_5, updated.getIdTokenEncryptedResponseAlg());
             Assert.assertEquals(JWEConstants.A128CBC_HS256, updated.getIdTokenEncryptedResponseEnc());

             // Test Keycloak representation
             kcClient = getClient(updated.getClientId());
             config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertEquals(JWEConstants.RSA1_5, config.getIdTokenEncryptedResponseAlg());
             Assert.assertEquals(JWEConstants.A128CBC_HS256, config.getIdTokenEncryptedResponseEnc());

        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setIdTokenEncryptedResponseAlg(null);
            updated.setIdTokenEncryptedResponseEnc(null);
            reg.oidc().update(updated);
        }
    }

    @Test
    public void testOIDCEndpointCreateWithSamlClient() throws Exception {
        ClientsResource clientsResource = adminClient.realm(TEST).clients();
        ClientRepresentation samlClient = clientsResource.findByClientId("saml-client").get(0);
        String samlClientServiceId = clientsResource.get(samlClient.getId()).getServiceAccountUser().getId();

        String realmManagementId = clientsResource.findByClientId("realm-management").get(0).getId();
        RoleRepresentation role = clientsResource.get(realmManagementId).roles().get("create-client").toRepresentation();

        adminClient.realm(TEST).users().get(samlClientServiceId).roles().clientLevel(realmManagementId).add(Arrays.asList(role));

        String accessToken = oauth.clientId("saml-client").doClientCredentialsGrantAccessTokenRequest("secret").getAccessToken();
        reg.auth(Auth.token(accessToken));

        // change client to saml
        samlClient.setProtocol("saml");
        clientsResource.get(samlClient.getId()).update(samlClient);

        OIDCClientRepresentation client = createRep();
        assertCreateFail(client, 400, Errors.INVALID_CLIENT);

        // revert client
        samlClient.setProtocol("openid-connect");
        clientsResource.get(samlClient.getId()).update(samlClient);
    }

    @Test
    public void testOIDCEndpointGetWithSamlClient() {
        ClientsResource clientsResource = adminClient.realm(TEST).clients();
        ClientRepresentation samlClient = clientsResource.findByClientId("saml-client").get(0);

        reg.auth(Auth.client("saml-client", "secret"));

        // change client to saml
        samlClient.setProtocol("saml");
        clientsResource.get(samlClient.getId()).update(samlClient);

        assertGetFail(samlClient.getClientId(), 400, Errors.INVALID_CLIENT);

        // revert client
        samlClient.setProtocol("openid-connect");
        clientsResource.get(samlClient.getId()).update(samlClient);
    }

    private ClientRepresentation getKeycloakClient(String clientId) {
        return ApiUtil.findClientByClientId(adminClient.realms().realm(REALM_NAME), clientId).toRepresentation();
    }


}
