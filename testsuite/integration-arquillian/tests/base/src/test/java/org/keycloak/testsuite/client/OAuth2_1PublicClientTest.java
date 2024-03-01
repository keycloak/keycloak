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

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createEcJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateEcdsaKey;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateSignedDPoPProof;

@EnableFeature(value = Profile.Feature.DPOP, skipRestart = true)
public class OAuth2_1PublicClientTest extends AbstractFAPITest {

    private static final String OAUTH2_1_PUBLIC_CLIENT_PROFILE_NAME = "oauth-2-1-for-public-client";

    private static final String DPOP_JWT_HEADER_TYPE = "dpop+jwt";

    private KeyPair ecKeyPair;

    private JWK jwkEc;

    private String validRedirectUri;;

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
        oauth.nonce(null);
        oauth.codeChallenge(null);
        oauth.codeChallengeMethod(null);
        oauth.dpopProof(null);
        updatePolicies("{}");
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

        setValidPkce(clientId);

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

        oauth.clientId(clientId);
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest(null, TEST_USER_NAME, TEST_USER_PASSWORD, null);

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
        oauth.codeChallenge(null);
        oauth.codeChallengeMethod(null);
        oauth.codeVerifier(null);
        failLoginByNotFollowingPKCE(clientId);
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
        setValidPkce(clientId);
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
        setValidPkce(clientId);
        oauth.clientId(clientId);
        oauth.redirectUri(validRedirectUri);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        // token request with DPoP Proof - success
        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getAccessTokenUrl(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofEcEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());

        // token refresh request with DPoP Proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getAccessTokenUrl(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());
        oauth.dpopProof(dpopProofEcEncoded);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // userinfo request with DPoP Proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getUserInfoUrl(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());
        oauth.dpopProof(dpopProofEcEncoded);
        OAuthClient.UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());

        oauth.idTokenHint(response.getIdToken()).openLogout();

        // revoke token with a valid DPoP proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getTokenRevocationUrl(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());
        oauth.dpopProof(dpopProofEcEncoded);
        CloseableHttpResponse closableHttpResponse = oauth.doTokenRevoke(response.getAccessToken(), "access_token", null);
        assertThat(closableHttpResponse, Matchers.statusCodeIsHC(Response.Status.OK));
        String introspectionResponse = oauth.introspectAccessTokenWithClientCredential(clientId, null, response.getAccessToken());
        TokenMetadataRepresentation tokenMetadataRepresentation = JsonSerialization.readValue(introspectionResponse, TokenMetadataRepresentation.class);
        assertFalse(tokenMetadataRepresentation.isActive());

        oauth.idTokenHint(response.getIdToken()).openLogout();
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

    private void setupValidClientExceptForRedirectUri(ClientRepresentation clientRep, OIDCAdvancedConfigWrapper clientConfig ) {
        clientRep.setPublicClient(Boolean.TRUE);
        clientRep.setRedirectUris(Collections.singletonList(validRedirectUri));
        clientRep.setImplicitFlowEnabled(false);
        clientRep.setDirectAccessGrantsEnabled(false);
        clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        clientConfig.setPkceCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        clientConfig.setUseDPoP(true);
    };

    private void testProhibitedImplicitOrHybridFlow(boolean isOpenid, String responseType, String nonce) {
        oauth.openid(isOpenid);
        oauth.responseType(responseType);
        oauth.nonce(nonce);
        oauth.redirectUri(validRedirectUri);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentFragment().get(OAuth2Constants.ERROR));
        assertEquals("Implicit/Hybrid flow is prohibited.", oauth.getCurrentFragment().get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    private void setValidPkce(String clientId) throws Exception {
        oauth.clientId(clientId);
        String codeVerifier = PkceUtils.generateCodeVerifier();
        String codeChallenge = generateS256CodeChallenge(codeVerifier);
        oauth.codeChallenge(codeChallenge);
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.codeVerifier(codeVerifier);
    }

    private String generateNonce() {
        return SecretGenerator.getInstance().randomString(16);
    }
}