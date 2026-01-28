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
 *
 */

package org.keycloak.testsuite.client;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createEcJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateEcdsaKey;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateSignedDPoPProof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OAuth2_1PublicClientTest extends AbstractFAPITest {

    private static final String OAUTH2_1_PUBLIC_CLIENT_PROFILE_NAME = "oauth-2-1-for-public-client";

    private static final String DPOP_JWT_HEADER_TYPE = "dpop+jwt";

    private KeyPair ecKeyPair;

    private JWK jwkEc;

    private String validRedirectUri;

    private PkceGenerator pkceGenerator;

    @Before
    public void setupValidateRedirectUri() {
        validRedirectUri = AssertEvents.DEFAULT_REDIRECT_URI.replace("localhost", "127.0.0.1");
    }

    @Before
    public void beforeDPoPTest() throws Exception {
        ecKeyPair = generateEcdsaKey("secp256r1");
        jwkEc = createEcJwk(ecKeyPair.getPublic());
    }

    @After
    public void revertPolicies() throws ClientPolicyException {
        oauth.openid(true);
        oauth.responseType(OIDCResponseType.CODE);
        updatePolicies("{}");
        pkceGenerator = null;
    }

    @Test
    public void testOAuth2_1NotAllowImplicitGrant() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);

        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setRedirectUris(Collections.singletonList(validRedirectUri));
        });

        // setup profiles and policies
        setupPolicyOAuth2_1PublicClientForAllClient();

        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();

        // implicit grant
        testProhibitedImplicitOrHybridFlow(false, OIDCResponseType.TOKEN, generateNonce()
        );

        // hybrid grant
        testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.ID_TOKEN,
                generateNonce());

        // hybrid grant
        testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE,
                generateNonce());

        // hybrid grant
        testProhibitedImplicitOrHybridFlow(true, OIDCResponseType.TOKEN + " " + OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN,
                generateNonce());
    }

    @Test
    public void testOAuth2_1NotAllowResourceOwnerPasswordCredentialsGrant() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);

        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.TRUE);
        });

        // setup profiles and policies
        setupPolicyOAuth2_1PublicClientForAllClient();

        oauth.client(clientId);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("resource owner password credentials grant is prohibited.", response.getErrorDescription());
    }

    @Test
    public void testOAuth2_1ProofKeyForCodeExchange() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) ->
                setupValidClientExceptForRedirectUri(clientRep, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep))
        );
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cId)).getPkceCodeChallengeMethod());

        // setup profiles and policies
        setupPolicyOAuth2_1PublicClientForAllClient();

        oauth.redirectUri(validRedirectUri);
        pkceGenerator = null;
        failLoginByNotFollowingPKCEWithoutClientPolicyValidation(clientId);
    }

    @Test
    public void testOAuth2_1RedirectUris() throws Exception {
        // setup profiles and policies
        setupPolicyOAuth2_1PublicClientForAllClient();

        // registration with invalid redirect_uri - fail
        try {
            createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) ->
                    clientRep.setRedirectUris(List.of("http://example.com/app")));
        } catch (ClientRegistrationException cre) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, cre.getMessage());
        }

        // registration with valid redirect_uri- success
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setRedirectUris(List.of(validRedirectUri.replace(":8543/", "/")));
        });
        assertEquals(validRedirectUri.replace(":8543/", "/"), getClientByAdmin(cId).getRedirectUris().get(0));

        // update with valid redirect_uri - fail
        try {
            createClientDynamically(clientId, (OIDCClientRepresentation clientRep) ->
                    clientRep.setRedirectUris(List.of("https://localhost/app")));
        } catch (ClientRegistrationException cre) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, cre.getMessage());
        }

        // authorization with invalid redirect_uri request - fail
        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();

        oauth.redirectUri("https://localhost/app");
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
    }

    @Test
    public void testOAuth2_1DPoPSenderConstrainedToken() throws Exception {
        // registration (auto-config) - success
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) ->
                setupValidClientExceptForRedirectUri(clientRep, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep))
        );
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(getClientByAdmin(cId)).isUseDPoP());

        // setup profiles and policies
        setupPolicyOAuth2_1PublicClientForAllClient();

        // authorization request - success
        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();
        oauth.redirectUri(validRedirectUri);
        oauth.loginForm().codeChallenge(pkceGenerator).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        // token request with DPoP Proof - success
        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEcEncoded).codeVerifier(pkceGenerator.getCodeVerifier()).send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());

        // token refresh request with DPoP Proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEcEncoded).send();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // userinfo request with DPoP Proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), response.getAccessToken());
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofEcEncoded).send();
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());

        oauth.logoutForm().idTokenHint(response.getIdToken()).open();

        // revoke token with a valid DPoP proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getRevocation(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        assertTrue(oauth.tokenRevocationRequest(response.getAccessToken()).accessToken().dpopProof(dpopProofEcEncoded).send().isSuccess());

        oauth.logoutForm().idTokenHint(response.getIdToken()).open();
    }

    private void setupPolicyOAuth2_1PublicClientForAllClient() throws Exception {
        String json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable OAuth 2.1 public client profile for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(OAUTH2_1_PUBLIC_CLIENT_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void setupValidClientExceptForRedirectUri(ClientRepresentation clientRep, OIDCAdvancedConfigWrapper clientConfig) {
        clientRep.setPublicClient(Boolean.TRUE);
        clientRep.setRedirectUris(Collections.singletonList(validRedirectUri));
        clientRep.setImplicitFlowEnabled(false);
        clientRep.setDirectAccessGrantsEnabled(false);
        clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        clientConfig.setPkceCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        clientConfig.setUseDPoP(true);
    }

    private void testProhibitedImplicitOrHybridFlow(boolean isOpenid, String responseType, String nonce) {
        oauth.openid(isOpenid);
        oauth.responseType(responseType);
        oauth.redirectUri(validRedirectUri);
        oauth.loginForm().nonce(nonce).codeChallenge(pkceGenerator).open();
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
        assertEquals("Implicit/Hybrid flow is prohibited.", authorizationEndpointResponse.getErrorDescription());
    }

    private String generateNonce() {
        return SecretGenerator.getInstance().randomString(16);
    }
}
