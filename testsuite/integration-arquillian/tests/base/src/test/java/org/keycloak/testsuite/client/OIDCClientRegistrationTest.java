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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Errors;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.oauth.OAuthClient.AUTH_SERVER_ROOT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientRegistrationTest extends AbstractClientRegistrationTest {

    private static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation testRealm = testRealms.get(0);

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
        client.setFrontChannelLogoutUri("http://frontchannel");
        client.setFrontchannelLogoutSessionRequired(true);
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
        assertEquals(AUTH_SERVER_ROOT + "/realms/" + REALM_NAME + "/clients-registrations/openid-connect/" + response.getClientId(), response.getRegistrationClientUri());
        assertEquals("RegistrationAccessTokenTest", response.getClientName());
        assertEquals("http://root", response.getClientUri());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://redirect", response.getRedirectUris().get(0));
        assertEquals(Arrays.asList("code", "none"), response.getResponseTypes());
        assertEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        Assert.assertNull(response.getUserinfoSignedResponseAlg());
        assertEquals("http://frontchannel", response.getFrontChannelLogoutUri());
        assertTrue(response.getFrontchannelLogoutSessionRequired());
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
        assertEquals(AUTH_SERVER_ROOT + "/realms/" + REALM_NAME + "/clients-registrations/openid-connect/" + response.getClientId(), response.getRegistrationClientUri());
    }

    @Test
    public void updateClient() throws ClientRegistrationException {
        Set<String> realmDefaultClientScopes = adminClient.realm(REALM_NAME).getDefaultDefaultClientScopes().stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(ClientScopeRepresentation::getName).collect(Collectors.toSet());

        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));

        response.setRedirectUris(Collections.singletonList("http://newredirect"));
        response.setResponseTypes(Arrays.asList("code", "id_token token", "code id_token token"));
        response.setGrantTypes(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.PASSWORD));
        OIDCClientRepresentation updated = reg.oidc().update(response);

        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(response.getClientId());
        ClientRepresentation rep = clientResource.toRepresentation();
        Set<String> registeredDefaultClientScopes = new HashSet<>(rep.getDefaultClientScopes());

        assertEquals(AUTH_SERVER_ROOT + "/realms/" + REALM_NAME + "/clients-registrations/openid-connect/" + updated.getClientId(), updated.getRegistrationClientUri());
        assertTrue(CollectionUtil.collectionEquals(Collections.singletonList("http://newredirect"), updated.getRedirectUris()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.PASSWORD), updated.getGrantTypes()));
        assertTrue(CollectionUtil.collectionEquals(Arrays.asList(OAuth2Constants.CODE, OIDCResponseType.NONE, OIDCResponseType.ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token"), updated.getResponseTypes()));
        assertTrue(CollectionUtil.collectionEquals(realmDefaultClientScopes, registeredDefaultClientScopes));
    }

    @Test
    public void createClientResponseTypeNone() throws ClientRegistrationException {
        OIDCClientRepresentation client = createRep();
        client.setResponseTypes(List.of("none"));
        OIDCClientRepresentation response = reg.oidc().create(client);

        assertNotNull(response.getRegistrationAccessToken());
        assertNotNull(response.getClientIdIssuedAt());
        assertNotNull(response.getClientId());
        assertNotNull(response.getClientSecret());
        assertEquals(0, response.getClientSecretExpiresAt().intValue());
        assertEquals(AUTH_SERVER_ROOT + "/realms/" + REALM_NAME + "/clients-registrations/openid-connect/" + response.getClientId(), response.getRegistrationClientUri());
        assertEquals("RegistrationAccessTokenTest", response.getClientName());
        assertEquals("http://root", response.getClientUri());
        assertEquals(1, response.getRedirectUris().size());
        assertEquals("http://redirect", response.getRedirectUris().get(0));
        assertEquals(List.of("code", "none"), response.getResponseTypes());
        assertEquals(Arrays.asList(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN), response.getGrantTypes());
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, response.getTokenEndpointAuthMethod());
        Assert.assertNull(response.getUserinfoSignedResponseAlg());
        assertEquals("http://frontchannel", response.getFrontChannelLogoutUri());
        assertTrue(response.getFrontchannelLogoutSessionRequired());
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
            clientRep.setUserinfoSignedResponseAlg(Algorithm.ES256);
            clientRep.setRequestObjectSigningAlg(Algorithm.ES256);

            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.ES256, response.getUserinfoSignedResponseAlg());
            Assert.assertEquals(Algorithm.ES256, response.getRequestObjectSigningAlg());
            Assert.assertNotNull(response.getClientSecret());

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(config.getUserInfoSignedResponseAlg(), Algorithm.ES256);
            Assert.assertEquals(config.getRequestObjectSignatureAlg(), Algorithm.ES256);

            // update (ES256 to PS256)
            clientRep.setUserinfoSignedResponseAlg(Algorithm.PS256);
            clientRep.setRequestObjectSigningAlg(Algorithm.PS256);
            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.PS256, response.getUserinfoSignedResponseAlg());
            Assert.assertEquals(Algorithm.PS256, response.getRequestObjectSigningAlg());

            // keycloak representation
            kcClient = getClient(response.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(config.getUserInfoSignedResponseAlg(), Algorithm.PS256);
            Assert.assertEquals(config.getRequestObjectSignatureAlg(), Algorithm.PS256);
        } finally {
            // back to RS256 for other tests
            clientRep.setUserinfoSignedResponseAlg(Algorithm.RS256);
            clientRep.setRequestObjectSigningAlg(Algorithm.RS256);
            response = reg.oidc().create(clientRep);
        }
    }

    @Test
    public void createClientImplicitFlow() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();

        clientRep.setResponseTypes(Arrays.asList("id_token token"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);

        String clientId = response.getClientId();
        ClientRepresentation kcClientRep = getKeycloakClient(clientId);
        Assert.assertFalse(kcClientRep.isPublicClient());
        Assert.assertFalse(kcClientRep.isBearerOnly());
        Assert.assertNotNull(kcClientRep.getSecret());
    }

    @Test
    public void createPublicClient() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();

        clientRep.setTokenEndpointAuthMethod("none");
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("none", response.getTokenEndpointAuthMethod());

        String clientId = response.getClientId();
        ClientRepresentation kcClientRep = getKeycloakClient(clientId);
        Assert.assertTrue(kcClientRep.isPublicClient());
        Assert.assertNull(kcClientRep.getSecret());
    }

    @Test
    public void testClientSecretsWithAuthMethod() throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setGrantTypes(Collections.singletonList(OAuth2Constants.CLIENT_CREDENTIALS));
        clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_JWT);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("client_secret_jwt", response.getTokenEndpointAuthMethod());
        Assert.assertNotNull(response.getClientSecret());
        Assert.assertNotNull(response.getClientSecretExpiresAt());

        ClientRepresentation kcClientRep = getKeycloakClient(response.getClientId());
        Assert.assertFalse(kcClientRep.isPublicClient());
        Assert.assertNotNull(kcClientRep.getSecret());

        // Updating
        reg.auth(Auth.token(response));
        response.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        OIDCClientRepresentation updated = reg.oidc().update(response);
        Assert.assertEquals("tls_client_auth", updated.getTokenEndpointAuthMethod());
        Assert.assertNull(updated.getClientSecret());
        Assert.assertNull(updated.getClientSecretExpiresAt());
    }

    @Test
    public void createClientFrontchannelLogoutSettings() throws ClientRegistrationException {
        // When frontchannelLogutSessionRequired is not set, it should be false by default per OIDC Client registration specification
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setFrontchannelLogoutSessionRequired(null);
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals(false, response.getFrontchannelLogoutSessionRequired());

        String clientId = response.getClientId();
        ClientRepresentation kcClientRep = getKeycloakClient(clientId);
        Assert.assertFalse(OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClientRep).isFrontChannelLogoutSessionRequired());
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
    public void testDPoPHoKTokenEnabled() throws Exception {
        // create (no specification)
        OIDCClientRepresentation clientRep = createRep();

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals(Boolean.FALSE, response.getDpopBoundAccessTokens());
        Assert.assertNotNull(response.getClientSecret());

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(!config.isUseDPoP());

        // update (true)
        reg.auth(Auth.token(response));
        response.setDpopBoundAccessTokens(Boolean.TRUE);
        OIDCClientRepresentation updated = reg.oidc().update(response);
        assertTrue(updated.getDpopBoundAccessTokens().booleanValue());

        // Test Keycloak representation
        kcClient = getClient(updated.getClientId());
        config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(config.isUseDPoP());

        // update (false)
        reg.auth(Auth.token(updated));
        updated.setDpopBoundAccessTokens(Boolean.FALSE);
        OIDCClientRepresentation reUpdated = reg.oidc().update(updated);
        assertTrue(!reUpdated.getDpopBoundAccessTokens().booleanValue());

        // Test Keycloak representation
        kcClient = getClient(reUpdated.getClientId());
        config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        assertTrue(!config.isUseDPoP());

    }

    @Test
    public void testUserInfoEncryptedResponse() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
            // create (no specification)
            OIDCClientRepresentation clientRep = createRep();

            response = reg.oidc().create(clientRep);

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertNull(config.getUserInfoEncryptedResponseAlg());
            Assert.assertNull(config.getUserInfoEncryptedResponseEnc());

            // update (alg RSA1_5, enc A128CBC-HS256)
            reg.auth(Auth.token(response));
            response.setUserinfoEncryptedResponseAlg(JWEConstants.RSA1_5);
            response.setUserinfoEncryptedResponseEnc(JWEConstants.A128CBC_HS256);
            updated = reg.oidc().update(response);
            Assert.assertEquals(JWEConstants.RSA1_5, updated.getUserinfoEncryptedResponseAlg());
            Assert.assertEquals(JWEConstants.A128CBC_HS256, updated.getUserinfoEncryptedResponseEnc());

            // Test Keycloak representation
            kcClient = getClient(updated.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(JWEConstants.RSA1_5, config.getUserInfoEncryptedResponseAlg());
            Assert.assertEquals(JWEConstants.A128CBC_HS256, config.getUserInfoEncryptedResponseEnc());

        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setUserinfoEncryptedResponseAlg(null);
            updated.setUserinfoEncryptedResponseEnc(null);
            reg.oidc().update(updated);
        }
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
    public void testIdTokenSignedResponse() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
             // create (no specification)
             OIDCClientRepresentation clientRep = createRep();

             response = reg.oidc().create(clientRep);
             Assert.assertNull(response.getIdTokenSignedResponseAlg());

             // Test Keycloak representation
             ClientRepresentation kcClient = getClient(response.getClientId());
             OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertNull(config.getIdTokenSignedResponseAlg());

             // update
             reg.auth(Auth.token(response));
             response.setIdTokenSignedResponseAlg(Algorithm.ES256);
             updated = reg.oidc().update(response);
             Assert.assertEquals(Algorithm.ES256, updated.getIdTokenSignedResponseAlg());

             // Test Keycloak representation
             kcClient = getClient(updated.getClientId());
             config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertEquals(Algorithm.ES256, config.getIdTokenSignedResponseAlg());
        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setIdTokenSignedResponseAlg(null);
            reg.oidc().update(updated);
        }
    }

    @Test
    public void testTokenEndpointSigningAlg() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
            OIDCClientRepresentation clientRep = createRep();
            clientRep.setTokenEndpointAuthSigningAlg(Algorithm.ES256);

            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.ES256, response.getTokenEndpointAuthSigningAlg());

            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(Algorithm.ES256, config.getTokenEndpointAuthSigningAlg());

            reg.auth(Auth.token(response));
            response.setTokenEndpointAuthSigningAlg(null);
            updated = reg.oidc().update(response);
            Assert.assertEquals(null, response.getTokenEndpointAuthSigningAlg());

            kcClient = getClient(updated.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(null, config.getTokenEndpointAuthSigningAlg());
        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setTokenEndpointAuthSigningAlg(null);
            reg.oidc().update(updated);
        }
    }

    @Test
    public void testAuthorizationResponseSigningAlg() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
            OIDCClientRepresentation clientRep = createRep();
            clientRep.setAuthorizationSignedResponseAlg(Algorithm.PS256);

            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.PS256, response.getAuthorizationSignedResponseAlg());

            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(Algorithm.PS256, config.getAuthorizationSignedResponseAlg());

            reg.auth(Auth.token(response));
            response.setAuthorizationSignedResponseAlg(null);
            updated = reg.oidc().update(response);
            Assert.assertEquals(null, response.getAuthorizationSignedResponseAlg());

            kcClient = getClient(updated.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(null, config.getAuthorizationSignedResponseAlg());
        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setAuthorizationSignedResponseAlg(null);
            reg.oidc().update(updated);
        }
    }

    @Test
    public void testAuthorizationEncryptedResponse() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
            OIDCClientRepresentation clientRep = createRep();
            clientRep.setAuthorizationEncryptedResponseAlg(JWEConstants.RSA1_5);
            clientRep.setAuthorizationEncryptedResponseEnc(JWEConstants.A128CBC_HS256);

            // create
            response = reg.oidc().create(clientRep);
            Assert.assertEquals(JWEConstants.RSA1_5, response.getAuthorizationEncryptedResponseAlg());
            Assert.assertEquals(JWEConstants.A128CBC_HS256, response.getAuthorizationEncryptedResponseEnc());

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(JWEConstants.RSA1_5, config.getAuthorizationEncryptedResponseAlg());
            Assert.assertEquals(JWEConstants.A128CBC_HS256, config.getAuthorizationEncryptedResponseEnc());

            // update
            reg.auth(Auth.token(response));
            response.setAuthorizationEncryptedResponseAlg(null);
            response.setAuthorizationEncryptedResponseEnc(null);
            updated = reg.oidc().update(response);
            Assert.assertNull(updated.getAuthorizationEncryptedResponseAlg());
            Assert.assertNull(updated.getAuthorizationEncryptedResponseEnc());

            // Test Keycloak representation
            kcClient = getClient(updated.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertNull(config.getAuthorizationEncryptedResponseAlg());
            Assert.assertNull(config.getAuthorizationEncryptedResponseEnc());

        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setAuthorizationEncryptedResponseAlg(null);
            updated.setAuthorizationEncryptedResponseEnc(null);
            reg.oidc().update(updated);
        }
    }

    @Test
    public void testCIBASettings() throws Exception {
        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;
        clientRep = createRep();
        clientRep.setBackchannelTokenDeliveryMode("poll");

        response = reg.oidc().create(clientRep);
        Assert.assertEquals("poll", response.getBackchannelTokenDeliveryMode());

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        Assert.assertEquals("poll", kcClient.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT));

        // Create with ping mode (failes due missing clientNotificationEndpoint)
        clientRep.setBackchannelTokenDeliveryMode("ping");
        try {
            reg.oidc().create(clientRep);
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        // Create with ping mode (success)
        clientRep.setBackchannelClientNotificationEndpoint("https://foo/bar");
        response = reg.oidc().create(clientRep);
        Assert.assertEquals("ping", response.getBackchannelTokenDeliveryMode());
        Assert.assertEquals("https://foo/bar", response.getBackchannelClientNotificationEndpoint());

        // Create with push mode (fails)
        clientRep.setBackchannelTokenDeliveryMode("push");
        try {
            reg.oidc().create(clientRep);
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
    }

    @Test
    public void testOIDCEndpointGetWithSamlClient() throws Exception {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));
        assertNotNull(reg.oidc().get(response.getClientId()));
        ClientsResource clientsResource = adminClient.realm(TEST).clients();
        ClientRepresentation client = clientsResource.findByClientId(response.getClientId()).get(0);

        // change client to saml
        client.setProtocol("saml");
        clientsResource.get(client.getId()).update(client);

        assertGetFail(client.getClientId(), 400, Errors.INVALID_CLIENT);
    }

    @Test
    public void testOIDCEndpointGetWithToken() throws Exception {
        OIDCClientRepresentation response = create();
        reg.auth(Auth.token(response));
        assertNotNull(reg.oidc().get(response.getClientId()));
    }

    @Test
    public void testOIDCEndpointGetWithoutToken() throws Exception {
        assertGetFail(create().getClientId(), 401, null);
    }

    @Test
    public void testTlsClientAuthSubjectDn() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
             // create (no specification)
             OIDCClientRepresentation clientRep = createRep();
             clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
             clientRep.setTlsClientAuthSubjectDn("Ein");

             response = reg.oidc().create(clientRep);
             Assert.assertEquals(OIDCLoginProtocol.TLS_CLIENT_AUTH, response.getTokenEndpointAuthMethod());
             Assert.assertEquals("Ein", response.getTlsClientAuthSubjectDn());

             // Test Keycloak representation
             ClientRepresentation kcClient = getClient(response.getClientId());
             OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, kcClient.getClientAuthenticatorType());
             Assert.assertEquals("Ein", config.getTlsClientAuthSubjectDn());
             Assert.assertFalse(config.getAllowRegexPatternComparison());

             // update
             reg.auth(Auth.token(response));
             response.setTlsClientAuthSubjectDn("(.*?)(?:$)");
             updated = reg.oidc().update(response);
             Assert.assertEquals(OIDCLoginProtocol.TLS_CLIENT_AUTH, updated.getTokenEndpointAuthMethod());
             Assert.assertEquals("(.*?)(?:$)", updated.getTlsClientAuthSubjectDn());

             // Test Keycloak representation
             kcClient = getClient(updated.getClientId());
             config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
             Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, kcClient.getClientAuthenticatorType());
             Assert.assertEquals("(.*?)(?:$)", config.getTlsClientAuthSubjectDn());
        } finally {
            // revert
            reg.auth(Auth.token(updated));
            updated.setTokenEndpointAuthMethod(null);
            updated.setTlsClientAuthSubjectDn(null);
            reg.oidc().update(updated);
        }
    }

    private ClientRepresentation getKeycloakClient(String clientId) {
        return ApiUtil.findClientByClientId(adminClient.realms().realm(REALM_NAME), clientId).toRepresentation();
    }

    @Test
    public void testClientWithScope() throws Exception {
        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;
        String clientScope = "phone address";

        clientRep = createRep();
        clientRep.setScope(clientScope);
        response = reg.oidc().create(clientRep);

        Set<String> clientScopes = new HashSet<>(Arrays.asList(clientScope.split(" ")));
        Set<String> registeredClientScopes = new HashSet<>(Arrays.asList(response.getScope().split(" ")));
        assertTrue(clientScopes.equals(registeredClientScopes));

        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(response.getClientId());
        assertTrue(CollectionUtil.collectionEquals(clientResource.toRepresentation().getDefaultClientScopes(), Set.of("basic")));
    }

    @Test
    public void testClientWithNotDefinedScope() throws Exception {
        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;

        String clientScope = "notdefinedscope address";

        clientRep = createRep();
        clientRep.setScope(clientScope);
        try {
            response = reg.oidc().create(clientRep);
            fail("Expected 403");
        } catch (ClientRegistrationException e) {
            assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testClientWithoutScope() throws ClientRegistrationException {
        Set<String> realmOptionalClientScopes = new HashSet<>(adminClient.realm(REALM_NAME).getDefaultOptionalClientScopes()
                .stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(i->i.getName()).collect(Collectors.toList()));

        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;

        clientRep = createRep();
        response = reg.oidc().create(clientRep);

        Set<String> registeredClientScopes = new HashSet<>(Arrays.asList(response.getScope().split(" ")));
        assertTrue(realmOptionalClientScopes.equals(new HashSet<>(registeredClientScopes)));

        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(response.getClientId());
        ClientRepresentation rep = clientResource.toRepresentation();

        Set<String> realmDefaultClientScopes = new HashSet<>(adminClient.realm(REALM_NAME).getDefaultDefaultClientScopes()
                .stream()
                .filter(scope -> Objects.equals(scope.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .map(i->i.getName()).collect(Collectors.toList()));

        Set<String> registeredDefaultClientScopes = new HashSet<>(rep.getDefaultClientScopes());
        assertTrue(realmDefaultClientScopes.equals(new HashSet<>(registeredDefaultClientScopes)));

    }

    @Test
    public void testRequestUris() throws Exception {
        OIDCClientRepresentation clientRep = null;
        OIDCClientRepresentation response = null;

        clientRep = createRep();
        clientRep.setRequestUris(Arrays.asList("http://host/foo", "https://host2/bar"));

        response = reg.oidc().create(clientRep);
        Assert.assertNames(response.getRequestUris(), "http://host/foo", "https://host2/bar");

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        Assert.assertNames(config.getRequestUris(), "http://host/foo", "https://host2/bar");
    }

    @Test
    public void testDefaultAcrValues() throws Exception {
        // Set realm acr-to-loa mapping
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        Map<String, Integer> acrLoaMap = new HashMap<>();
        acrLoaMap.put("copper", 0);
        acrLoaMap.put("silver", 1);
        acrLoaMap.put("gold", 2);
        realmRep.getAttributes().put(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(acrLoaMap));
        adminClient.realm("test").update(realmRep);

        OIDCClientRepresentation clientRep = createRep();
        clientRep.setDefaultAcrValues(Arrays.asList("silver", "foo"));
        try {
            OIDCClientRepresentation response = reg.oidc().create(clientRep);
            fail("Expected 400");
        } catch (ClientRegistrationException e) {
            assertEquals(400, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }

        clientRep.setDefaultAcrValues(Arrays.asList("silver", "gold"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertNames(response.getDefaultAcrValues(), "silver", "gold");

        // Test Keycloak representation
        ClientRepresentation kcClient = getClient(response.getClientId());
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
        Assert.assertNames(config.getAttributeMultivalued(Constants.DEFAULT_ACR_VALUES), "silver", "gold");

        // Revert realm acr-to-loa mappings
        realmRep.getAttributes().remove(Constants.ACR_LOA_MAP);
        adminClient.realm("test").update(realmRep);
    }

    @Test
    public void testPostLogoutRedirectUri() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setPostLogoutRedirectUris(Collections.singletonList("http://redirect/logout"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        assertEquals("http://redirect/logout", response.getPostLogoutRedirectUris().get(0));
    }

    @Test
    public void testPostLogoutRedirectUriPlus() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setPostLogoutRedirectUris(Collections.singletonList("+"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        assertEquals("http://redirect", response.getPostLogoutRedirectUris().get(0));
    }

    @Test
    public void testPostLogoutRedirectUriNull() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        assertEquals("http://redirect", response.getPostLogoutRedirectUris().get(0));
    }

    @Test
    public void testPostLogoutRedirectUriEmpty() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setPostLogoutRedirectUris(new ArrayList<String>());
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        assertEquals("http://redirect", response.getPostLogoutRedirectUris().get(0));
    }

    @Test
    public void testPostLogoutRedirectUriMinus() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setPostLogoutRedirectUris(Collections.singletonList("-"));
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        assertTrue(response.getPostLogoutRedirectUris().isEmpty());
    }
}
