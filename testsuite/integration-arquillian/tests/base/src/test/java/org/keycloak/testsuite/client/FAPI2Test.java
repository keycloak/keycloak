/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the FAPI 2 specifications (still implementer's draft):
 * - <a href="https://openid.bitbucket.io/fapi/fapi-security-profile-2_0.html">FAPI 2.0 Security Profile</a>
 * - <a href="https://openid.bitbucket.io/fapi/fapi-message-signing-2_0.html">FAPI 2.0 Message Signing</a>
 * <p>
 * Mostly tests the global FAPI policies work as expected
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class FAPI2Test extends AbstractFAPI2Test {

    protected static final String clientId = "foo";

    @Test
    public void testFAPI2SecurityProfileClientRegistration() throws Exception {
        testFAPI2ClientRegistration(getSecurityProfileName());
    }

    @Test
    public void testFAPI2SecurityProfileOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(getSecurityProfileName());
    }

    @Test
    public void testFAPI2SecurityProfileSignatureAlgorithms(String profile) throws Exception {
        testFAPI2SignatureAlgorithms(getSecurityProfileName());
    }

    @Test
    public void testFAPI2SecurityProfileLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        // send a pushed authorization request
        oauth.client(clientId);

        PkceGenerator pkceGenerator = PkceGenerator.s256();

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // send an authorization request
        String code = loginUserAndGetCode(clientId, null, false);

        // send a token request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        this.pkceGenerator = pkceGenerator;
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);
        assertSuccessfulTokenResponse(tokenResponse);

        // check HoK required
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2SecurityProfileLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        oauth.client(clientId);

        // without PAR request - should fail
        oauth.openLoginForm();
        assertBrowserWithError("request_uri not included.");

        pkceGenerator = PkceGenerator.s256();

        // requiring hybrid request - should fail
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        ParResponse pResp = oauth.pushedAuthorizationRequest().nonce("123456").codeChallenge(pkceGenerator).send();
        assertEquals(401, pResp.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, pResp.getError());

        // authorization request does not match PAR request - should fail
        oauth.responseType(OIDCResponseType.CODE);
        pResp = oauth.pushedAuthorizationRequest().codeChallenge(pkceGenerator).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        oauth.loginForm().requestUri(requestUri).open();
        oauth.responseMode("query"); // Keycloak uses some default response mode as response type is not valid
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST, "Parameter response_type does not match");
        oauth.responseMode(null);

        oauth.responseType(OIDCResponseType.CODE);

        // an additional parameter in an authorization request that does not exist in a PAR request - should fail
        pResp = oauth.pushedAuthorizationRequest().codeChallenge(pkceGenerator).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        oauth.loginForm().requestUri(requestUri).state("testFAPI2SecurityProfileLoginWithMTLS").open();
        assertBrowserWithError("PAR request did not include necessary parameters");

        // duplicated usage of a PAR request - should fail
        oauth.loginForm().requestUri(requestUri).state("testFAPI2SecurityProfileLoginWithMTLS").codeChallenge(pkceGenerator).open();
        assertBrowserWithError("PAR not found. not issued or used multiple times.");

        // send a pushed authorization request
        pResp = oauth.pushedAuthorizationRequest().nonce("123456").codeChallenge(pkceGenerator).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // send an authorization request
        String code = loginUserAndGetCode(clientId, "123456", false);

        // send a token request
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).codeVerifier(pkceGenerator).send();

        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2MessageSigningClientRegistration() throws Exception {
        testFAPI2ClientRegistration(getMessageSigningName());
    }

    @Test
    public void testFAPI2MessageSigningOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(getMessageSigningName());
    }

    @Test
    public void testFAPI2MessageSigningSignatureAlgorithms(String profile) throws Exception {
        testFAPI2SignatureAlgorithms(getMessageSigningName());
    }


    @Test
    public void testFAPI2MessageSigningLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getMessageSigningName());

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
            clientConfig.setRequestObjectRequired("request or request_uri");
            clientConfig.setAuthorizationSignedResponseAlg(Algorithm.PS256);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getRequestObjectSignatureAlg());

        // Set request object and correct responseType
        oauth.client(clientId);

        pkceGenerator = PkceGenerator.s256();

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(request).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // send an authorization request
        oauth.responseType(OIDCResponseType.CODE);
        oauth.responseMode(OIDCResponseMode.QUERY_JWT.value());
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId, "123456");

        // send a token request
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code).codeVerifier(pkceGenerator).send();

        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2MessageSigningLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getMessageSigningName());

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setRequestObjectRequired("request or request_uri");
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.PS256);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getRequestObjectSignatureAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        oauth.client(clientId);

        pkceGenerator = PkceGenerator.s256();

        // without a request object - should fail
        oauth.responseType(OIDCResponseType.CODE);
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, true);
        oauth.client(clientId);
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth.pushedAuthorizationRequest().nonce("123456").signedJwt(signedJwt).send();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());

        // Set request object and correct responseType
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        pResp = oauth.pushedAuthorizationRequest().signedJwt(signedJwt).request(request).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // send an authorization request
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId, null);

        // send a token request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);
        assertSuccessfulTokenResponse(tokenResponse);
 
        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testSecureClientAuthenticationAssertion() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // Register client with private-key-jwt
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });

        oauth.client(clientId);

        PkceGenerator pkceGenerator = PkceGenerator.s256();

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // Send a push authorization request with invalid 'aud' . Should fail
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/ext/par/request");
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(400, pResp.getStatusCode());

        // Send a push authorization request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl());
        pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // Send an authorization request . Should succeed
        String code = loginUserAndGetCode(clientId, null, false);
        assertNotNull(code);

        // Send a token request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/token");
        this.pkceGenerator = pkceGenerator;
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);
        assertEquals(400, tokenResponse.getStatusCode());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    protected void testFAPI2ClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

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

    protected void testFAPI2OIDCClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Try to register client with clientIdAndSecret - should fail
        try {
            createClientDynamically(generateSuffixedName(clientId), (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC));
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

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientDynamically("client-x509", (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH));
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        Assert.assertTrue(client.isConsentRequired());
        Assert.assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());

    }

    protected String getSecurityProfileName() {
        return FAPI2_SECURITY_PROFILE_NAME;
    }

    protected String getMessageSigningName() {
        return FAPI2_MESSAGE_SIGNING_PROFILE_NAME;
    }
}
