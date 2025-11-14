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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OAuth2_1ConfidentialClientTest extends AbstractFAPITest {

    private static final String OAUTH2_1_CONFIDENTIAL_CLIENT_PROFILE_NAME = "oauth-2-1-for-confidential-client";

    private String validRedirectUri;

    private PkceGenerator pkceGenerator;

    @Before
    public void setupValidateRedirectUri() {
        validRedirectUri = AssertEvents.DEFAULT_REDIRECT_URI.replace("localhost", "127.0.0.1");
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
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            clientRep.setRedirectUris(Collections.singletonList(validRedirectUri));
        });
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, getClientByAdmin(cId).getClientAuthenticatorType());

        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();

        // implicit grant
        testProhibitedImplicitOrHybridFlow(false, OIDCResponseType.TOKEN, generateNonce());

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
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
            clientRep.setDirectAccessGrantsEnabled(true);
        });

        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        // resource owner password credentials grant - fail
        oauth.client(clientId);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(TEST_USERNAME, TEST_USERSECRET);

        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("resource owner password credentials grant is prohibited.", response.getErrorDescription());
    }

    @Test
    public void testOAuth2_1ClientAuthentication() throws Exception {
        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        // register client with clientIdAndSecret - fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // register client with x509 - success
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
            clientRep.setRedirectUris(Collections.singletonList(validRedirectUri.replace(":8543/", "/")));
        });
        verifyClientSettings(getClientByAdmin(cId), X509ClientAuthenticator.PROVIDER_ID);
    }



    @Test
    public void testOAuth2_1ProofKeyForCodeExchange() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) ->
                setupValidClientExceptForRedirectUri(clientRep, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep))
        );
        verifyClientSettings(getClientByAdmin(cId), X509ClientAuthenticator.PROVIDER_ID);

        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        oauth.redirectUri(validRedirectUri);
        failLoginByNotFollowingPKCEWithoutClientPolicyValidation(clientId);
    }

    @Test
    public void testOAuth2_1RedirectUris() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) ->
                setupValidClientExceptForRedirectUri(clientRep, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep))
        );
        verifyClientSettings(getClientByAdmin(cId), X509ClientAuthenticator.PROVIDER_ID);

        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        failUpdateRedirectUrisDynamically(clientId, List.of("https://dev.example.com:8443/*"));
        successUpdateRedirectUrisByAdmin(cId,
                List.of("https://dev.example.com:8443/callback", "https://[::1]/auth/admin",
                        "com.example.app:/oauth2redirect/example-provider", "https://127.0.0.1/auth/admin"));
        failAuthorizationRequest(clientId, TestApplicationResourceUrls.clientRequestUri());
    }

    @Test
    public void testOAuth2_1OAuthMtlsSenderConstrainedToken() throws Exception {
        String clientId = generateSuffixedName(CLIENT_NAME);
        String cId = createClientByAdmin(clientId, (ClientRepresentation clientRep) ->
            setupValidClientExceptForRedirectUri(clientRep, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep))
        );
        verifyClientSettings(getClientByAdmin(cId), X509ClientAuthenticator.PROVIDER_ID);

        // setup profiles and policies
        setupPolicyOAuth2_1ConfidentialClientForAllClient();

        oauth.client(clientId);
        oauth.redirectUri(validRedirectUri);
        pkceGenerator = PkceGenerator.s256();

        AuthorizationEndpointResponse res = oauth.loginForm().codeChallenge(pkceGenerator).doLogin(TEST_USERNAME, TEST_USERSECRET);

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(res.getCode()).codeVerifier(pkceGenerator.getCodeVerifier()).send();
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
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

    private void setupPolicyOAuth2_1ConfidentialClientForAllClient() throws Exception {
        String json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable OAuth 2.1 confidential client profile for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(OAUTH2_1_CONFIDENTIAL_CLIENT_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void setupValidClientExceptForRedirectUri(ClientRepresentation clientRep, OIDCAdvancedConfigWrapper clientConfig ) {
        clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
        clientRep.setRedirectUris(Collections.singletonList(validRedirectUri));
        clientRep.setImplicitFlowEnabled(false);
        clientRep.setDirectAccessGrantsEnabled(false);
        clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
        clientConfig.setAllowRegexPatternComparison(false);
        clientConfig.setPkceCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        clientConfig.setUseMtlsHoKToken(true);
    }

    private String generateNonce() {
        return SecretGenerator.getInstance().randomString(16);
    }

    private void verifyClientSettings(ClientRepresentation clientRep, String clientAuthenticatorType) {
        assertFalse(clientRep.isBearerOnly());
        assertFalse(clientRep.isPublicClient());
        assertEquals(clientAuthenticatorType, clientRep.getClientAuthenticatorType());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).getPkceCodeChallengeMethod());
        assertTrue(OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).isUseMtlsHokToken());
        assertFalse(clientRep.isImplicitFlowEnabled());
        assertFalse(clientRep.isDirectAccessGrantsEnabled());
    }

    private void successUpdateRedirectUrisByAdmin(String cId, List<String> redirectUrisList) {
        try {
            updateClientByAdmin(cId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.setRedirectUris(redirectUrisList);
            });
            ClientRepresentation cRep = getClientByAdmin(cId);
            assertEquals(new HashSet<>(redirectUrisList), new HashSet<>(cRep.getRedirectUris()));
        } catch (ClientPolicyException cpe) {
            fail();
        }
    }

    private void failUpdateRedirectUrisDynamically(String clientId, List<String> redirectUrisList) {
        try {
            updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) ->
                    clientRep.setRedirectUris(redirectUrisList));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
    }

    private void failAuthorizationRequest(String clientId, String redirectUri) {
        oauth.client(clientId);
        oauth.redirectUri(redirectUri);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
    }


}
