/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.client;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.client.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import static org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test for the FAPI 1 specifications:
 * - <a href="https://openid.net/specs/openid-financial-api-part-1-1_0.html#authorization-server">Financial-grade API Security Profile 1.0 - Part 1: Baseline</a>
 * - <a href="https://openid.net/specs/openid-financial-api-part-2-1_0.html#authorization-server">Financial-grade API Security Profile 1.0 - Part 2: Advanced</a>
 * <p>
 * Mostly tests the global FAPI policies work as expected
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FAPI1Test extends AbstractFAPITest {

    @Test
    public void testFAPIBaselineClientAuthenticator() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Try to register client with clientIdAndSecret - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-secret-jwt" - should pass
        clientUUID = createClientByAdmin("client-secret-jwt", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientByAdmin("client-x509", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with default authenticator - should pass. Client authenticator should be "client-jwt"
        clientUUID = createClientByAdmin("client-jwt-2", (ClientRepresentation clientRep) -> {
        });
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        Assert.assertTrue(client.isConsentRequired());
        Assert.assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
    }


    // KEYCLOAK-19555
    @Test
    public void testFAPIBaselineSecureSettingsWhenUseAdminPolicy() throws Exception {
        // Apply policy for admin REST API and Dynamic Client Registration requests
        setupPolicyFAPIBaselineForAdminRESTAndDynamicClientRegistrationRequests();

        // Try to register client with default authenticator - should pass. Client authenticator should be "client-jwt"
        String clientUUID = createClientByAdmin("client-jwt-3", (ClientRepresentation clientRep) -> {
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        Assert.assertTrue(client.isConsentRequired());
        Assert.assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
    }


    @Test
    public void testFAPIBaselineOIDCClientRegistration() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Try to register client with clientIdAndSecret - should fail
        try {
            createClientDynamically(generateSuffixedName("foo"), (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientDynamically("client-jwt", (OIDCClientRepresentation clientRep) -> {
            clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
            clientRep.setJwksUri("https://foo");
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        Assert.assertFalse(client.isFullScopeAllowed());

        // Set new initialToken for register new clients
        setInitialAccessTokenForDynamicClientRegistration();

        // Try to register client with "client-secret-jwt" - should pass
        clientUUID = createClientDynamically("client-secret-jwt", (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_JWT));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Set new initialToken for register new clients
        setInitialAccessTokenForDynamicClientRegistration();

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientDynamically("client-x509", (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        Assert.assertTrue(client.isConsentRequired());
        Assert.assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());

    }


    @Test
    public void testFAPIBaselineRedirectUri() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Try to register redirect_uri like "http://hostname.com" - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setRedirectUris(Collections.singletonList("http://hostname.com")));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register redirect_uri like "https://hostname.com/foo/*" - should fail due the wildcard
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setRedirectUris(Collections.singletonList("https://hostname.com/foo/*")));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register redirect_uri like "https://hostname.com" - should pass
        String clientUUID = createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setRedirectUris(Collections.singletonList("https://hostname.com")));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertNames(client.getRedirectUris(), "https://hostname.com");
        getCleanup().addClientUuid(clientUUID);

        // Try to register client with valid root URL. Makes sure that there is not auto-created redirect URI with wildcard at the end (See KEYCLOAK-19556)
        String clientUUID2 = createClientByAdmin("invalid2", (ClientRepresentation clientRep) -> {
            clientRep.setRootUrl("https://hostname2.com");
            clientRep.setRedirectUris(null);
        });
        ClientRepresentation client2 = getClientByAdmin(clientUUID2);
        Assert.assertNames(client2.getRedirectUris(), "https://hostname2.com");
        getCleanup().addClientUuid(clientUUID2);
    }


    @Test
    public void testFAPIBaselineConfidentialClientLogin() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Register client (default authenticator)
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            clientRep.setSecret("atleast-14chars-password");
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertFalse(client.isPublicClient());
        Assert.assertEquals(JWTClientSecretAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        Assert.assertFalse(client.isFullScopeAllowed());

        checkPKCEWithS256RequiredDuringLogin("foo");

        pkceGenerator = PkceGenerator.s256();
        // Setup PKCE

        checkNonceAndStateForCurrentClientDuringLogin();
        checkRedirectUriForCurrentClientDuringLogin();

        // Check PKCE with S256, redirectUri and nonce/state set. Login should be successful
        successfulLoginAndLogout("foo", "123456", TEST_USERNAME, false, (String code) -> {
            String signedJwt = getClientSecretSignedJWT("atleast-14chars-password", Algorithm.HS256);
            return doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        });
    }


    @Test
    public void testFAPIBaselinePublicClientLogin() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Register client as public client
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> clientRep.setPublicClient(true));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertTrue(client.isPublicClient());

        checkPKCEWithS256RequiredDuringLogin("foo");

        pkceGenerator = PkceGenerator.s256();

        checkNonceAndStateForCurrentClientDuringLogin();
        checkRedirectUriForCurrentClientDuringLogin();

        // Check PKCE with S256, redirectUri and nonce/state set. Login should be successful
        successfulLoginAndLogout("foo", "123456", TEST_USERNAME, false, (String code) -> oauth.accessTokenRequest(code).codeVerifier(pkceGenerator).send());
    }


    @Test
    public void testFAPIAdvancedClientRegistration() throws Exception {
        // Set "advanced" policy
        setupPolicyFAPIAdvancedForAllClient();

        // Register client with clientIdAndSecret - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with signedJWT - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with privateKeyJWT, but unsecured redirectUri - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                clientRep.setRedirectUris(Collections.singletonList("http://foo"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientByAdmin("client-x509", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with default authenticator - should pass. Client authenticator should be "client-jwt"
        clientUUID = createClientByAdmin("client-jwt-2", (ClientRepresentation clientRep) -> {
        });
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, Holder-of-key is enabled, fullScopeAllowed disabled and default signature algorithm.
        Assert.assertTrue(client.isConsentRequired());
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertTrue(clientConfig.isUseMtlsHokToken());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        Assert.assertFalse(client.isFullScopeAllowed());
    }


    @Test
    public void testFAPIAdvancedPublicClientLoginNotPossible() throws Exception {
        setupPolicyFAPIBaselineForAllClient();

        // Register client as public client
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> clientRep.setPublicClient(true));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertTrue(client.isPublicClient());

        // Setup PKCE and nonce
        pkceGenerator = PkceGenerator.s256();

        // Check PKCE with S256, redirectUri and nonce/state set. Login should be successful
        successfulLoginAndLogout("foo", "123456", TEST_USERNAME, false, (String code) -> oauth.accessTokenRequest(code).codeVerifier(pkceGenerator).send());

        // Set "advanced" policy
        setupPolicyFAPIAdvancedForAllClient();

        // Should not be possible to login anymore with public client
        oauth.loginForm().nonce("123456").codeChallenge(pkceGenerator).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_CLIENT, "invalid client access type");
    }

    @Test
    public void testFAPIAdvancedSignatureAlgorithms() throws Exception {
        // Set "advanced" policy
        setupPolicyFAPIAdvancedForAllClient();

        // Test that unsecured algorithm (RS256) is not possible
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
                clientConfig.setIdTokenSignedResponseAlg(Algorithm.RS256);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // Test that secured algorithm is possible to explicitly set
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientCfg = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientCfg.setIdTokenSignedResponseAlg(Algorithm.ES256);
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertEquals(Algorithm.ES256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());

        // Test default algorithms set everywhere
        clientUUID = createClientByAdmin("client-jwt-default-alg", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getUserInfoSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        Assert.assertEquals(Algorithm.PS256, client.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

    }


    @Test
    public void testFAPIAdvancedLoginWithPrivateKeyJWT() throws Exception {
        // Set "advanced" policy
        setupPolicyFAPIAdvancedForAllClient();

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setImplicitFlowEnabled(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check nonce and redirectUri
        oauth.client("foo");
        checkNonceAndStateForCurrentClientDuringLogin();
        checkRedirectUriForCurrentClientDuringLogin();

        // Check login request object required
        oauth.loginForm().nonce("123456").open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Missing parameter: 'request' or 'request_uri'");

        // Create request without 'nbf' . Should fail in FAPI1 advanced client policy
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor("foo");
        requestObject.nbf(null);
        registerRequestObject(requestObject, "foo", Algorithm.PS256, true);
        oauth.loginForm().requestUri(requestUri).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST_URI,"Missing parameter in the 'request' object: nbf");

        // Create valid request object - more extensive testing of 'request' object is in ClientPoliciesTest.testSecureRequestObjectExecutor()
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor("foo");
        requestObject.setNonce("123456"); // Nonce from method "checkNonceAndStateForCurrentClientDuringLogin()"
        registerRequestObject(requestObject, "foo", Algorithm.PS256, true);

        // Check response type
        oauth.loginForm().requestUri(requestUri).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"invalid response_type");

        // Add the response_Type including token. Should fail
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        requestObject.setResponseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        registerRequestObject(requestObject, "foo", Algorithm.PS256, true);
        oauth.loginForm().requestUri(requestUri).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"invalid response_type");

        // Set correct response_type for FAPI 1 Advanced
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        requestObject.setResponseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        registerRequestObject(requestObject, "foo", Algorithm.PS256, true);
        oauth.loginForm().requestUri(requestUri).open();
        loginPage.assertCurrent();

        // Get keys of client. Will be used for client authentication and signing of request object
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();


        String code = loginUserAndGetCode("foo", null, true);

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();

        // Check token not present in the AuthorizationResponse. Check ID Token present, but used as detached signature
        Assert.assertNull(authorizationEndpointResponse.getAccessToken());
        String idTokenParam = authorizationEndpointResponse.getIdToken();
        assertIDTokenAsDetachedSignature(idTokenParam, code);

        // Check HoK required
        String signedJwt = createSignedRequestToken("foo", privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        Assert.assertEquals(OAuthErrorException.INVALID_GRANT,tokenResponse.getError());
        Assert.assertEquals("Client Certification missing for MTLS HoK Token Binding", tokenResponse.getErrorDescription());

        // Login with private-key-jwt client authentication and MTLS added to HttpClient. TokenRequest should be successful now
        oauth.loginForm().requestUri(requestUri).open();
        code = oauth.parseLoginResponse().getCode();
        Assert.assertNotNull(code);

        String signedJwt2 = createSignedRequestToken("foo", privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);

        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt2, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);

        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent("foo", TEST_USERNAME);
    }

    @Test
    public void testFAPIAdvancedLoginWithMTLS() throws Exception {
        // Set "advanced" policy
        setupPolicyFAPIAdvancedForAllClient();

        // Register client with X509
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            clientRep.setImplicitFlowEnabled(true);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check nonce and redirectUri
        oauth.client("foo");
        checkNonceAndStateForCurrentClientDuringLogin();
        checkRedirectUriForCurrentClientDuringLogin();

        // Check login request object required
        oauth.loginForm().nonce("123456").open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Missing parameter: 'request' or 'request_uri'");

        // Set request object and correct responseType
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor("foo");
        requestObject.setNonce("123456"); // Nonce from method "checkNonceAndStateForCurrentClientDuringLogin()"
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        requestObject.setResponseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN);
        registerRequestObject(requestObject, "foo", Algorithm.PS256, true);
        oauth.loginForm().requestUri(requestUri).open();
        loginPage.assertCurrent();

        String code = loginUserAndGetCode("foo", null, true);

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();

        // Check token not present in the AuthorizationResponse. Check ID Token present, but used as detached signature
        Assert.assertNull(authorizationEndpointResponse.getAccessToken());
        String idTokenParam = authorizationEndpointResponse.getIdToken();
        assertIDTokenAsDetachedSignature(idTokenParam, code);

        // Check HoK required
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);

        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent("foo", TEST_USERNAME);
    }

    private void checkPKCEWithS256RequiredDuringLogin(String clientId) {
        // Check PKCE required - login without PKCE should fail
        oauth.client(clientId);
        oauth.openLoginForm();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Missing parameter: code_challenge_method");

        // Check PKCE required - login with "plain" PKCE should fail
        pkceGenerator = PkceGenerator.plain();
        oauth.loginForm().codeChallenge(pkceGenerator).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Invalid parameter: code challenge method is not matching the configured one");
    }

    // Assumption is that clientId is already set in "oauth" client when this method is called. Also, assumption is that PKCE parameters are properly set (in case PKCE required for the client)
    private void checkNonceAndStateForCurrentClientDuringLogin() {
        oauth.loginForm().codeChallenge(pkceGenerator).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Missing parameter: nonce");

        // Check "state" required in non-OIDC request
        oauth.openid(false);
        oauth.loginForm().nonce("123456").codeChallenge(pkceGenerator).open();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST,"Missing parameter: state");
    }

    private void checkRedirectUriForCurrentClientDuringLogin() {
        String origRedirectUri = oauth.getRedirectUri();

        // Check redirect_uri required
        oauth.openid(true);
        oauth.redirectUri(null);
        oauth.loginForm().codeChallenge(pkceGenerator).open();
        assertBrowserWithError("Invalid parameter: redirect_uri");

        // Revert redirectUri
        oauth.redirectUri(origRedirectUri);
    }


    private void setupPolicyFAPIBaselineForAllClient() throws Exception {
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable FAPI Baseline for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(FAPI1_BASELINE_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void setupPolicyFAPIBaselineForAdminRESTAndDynamicClientRegistrationRequests() throws Exception {
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "MyClientUpdaterContextPolicy", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(Arrays.asList(
                                        ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                        ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                        ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                        .addProfile(FAPI1_BASELINE_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void setupPolicyFAPIAdvancedForAllClient() throws Exception {
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable FAPI Advanced for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(FAPI1_ADVANCED_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    // codeToTokenExchanger is supposed to exchange "code" for the accessTokenResponse. It is supposed to send the tokenRequest including proper client authentication
    private void successfulLoginAndLogout(String clientId, String nonce, String username, boolean fragmentResponseModeExpected, Function<String, AccessTokenResponse> codeToTokenExchanger) {
        String code = loginUserAndGetCode(clientId, nonce, fragmentResponseModeExpected);

        AccessTokenResponse tokenResponse = codeToTokenExchanger.apply(code);

        assertSuccessfulTokenResponse(tokenResponse);

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, username);
    }

    private void assertIDTokenAsDetachedSignature(String idTokenParam, String code) {
        Assert.assertNotNull(idTokenParam);
        IDToken idToken = oauth.verifyIDToken(idTokenParam);
        Assert.assertNotNull(idToken.getId());
        Assert.assertEquals("foo", idToken.getIssuedFor());
        Assert.assertNull(idToken.getPreferredUsername());
        Assert.assertNull(idToken.getEmail());
        Assert.assertNull(idToken.getGivenName());
        Assert.assertNull(idToken.getAccessTokenHash());
        Assert.assertEquals(idToken.getNonce(), "123456");
        String state = oauth.parseLoginResponse().getState();
        Assert.assertEquals(idToken.getStateHash(), HashUtils.accessTokenHash(Algorithm.PS256, state));
        Assert.assertEquals(idToken.getCodeHash(), HashUtils.accessTokenHash(Algorithm.PS256, code));
    }

    private String getClientSecretSignedJWT(String secret, String algorithm) {
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider();
        jwtProvider.setClientSecret(secret, algorithm);
        return jwtProvider.createSignedRequestToken(oauth.getClientId(), getRealmInfoUrl(), algorithm);
    }

}
