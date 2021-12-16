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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_HINT_PARAM;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.AUTH_REQ_ID;
import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.BINDING_MESSAGE;
import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.SUCCEED;
import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.CANCELLED;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelRequest;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.OAuthClient.AuthenticationRequestAcknowledgement;
import org.keycloak.util.JsonSerialization;

/**
 * Test for the FAPI CIBA specifications (still implementer's draft):
 * - Financial-grade API: Client Initiated Backchannel Authentication Profile - https://bitbucket.org/openid/fapi/src/master/Financial_API_WD_CIBA.md
 *
 * Mostly tests the global FAPI policies work as expected
 * This class only tests FAPI CIBA related requirements. OIDC CIBA related requirements has been tested by CIBATest.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@AuthServerContainerExclude(AuthServerContainerExclude.AuthServer.REMOTE)
public class FAPICIBATest extends AbstractClientPoliciesTest {

    private final String clientId = "foo";
    private final String bindingMessage = "bbbbmmmm";
    private final String username = "john";

    @BeforeClass
    public static void verifySSL() {
        // FAPI requires SSL and does not makes sense to test it with disabled SSL
        Assume.assumeTrue("The FAPI test requires SSL to be enabled.", ServerURLs.AUTH_SERVER_SSL_REQUIRED);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        List<UserRepresentation> users = realm.getUsers();

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("john");
        user.setEmail("john@keycloak.org");
        user.setFirstName("Johny");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Arrays.asList(AdminRoles.CREATE_CLIENT, AdminRoles.MANAGE_CLIENTS)));
        users.add(user);

        realm.setUsers(users);

        testRealms.add(realm);
    }

    @Test
    public void testFAPIAdvancedClientRegistration() throws Exception {
        setupPolicyFAPICIBAForAllClient();

        // Register client with clientIdAndSecret - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with signedJWT - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with privateKeyJWT, but unsecured requestUri - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList("http://foo"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientByAdmin("client-x509", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
        });
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
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg().toString());
        Assert.assertFalse(client.isFullScopeAllowed());
    }

    @Test
    public void testFAPICIBASignatureAlgorithms() throws Exception {
        setupPolicyFAPICIBAForAllClient();

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
            Map<String, String> attr = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
            attr.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, Algorithm.ES256);
            clientRep.setAttributes(attr);
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertEquals(Algorithm.ES256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg().toString());
        Assert.assertEquals(Algorithm.ES256, client.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // Test default algorithms set everywhere
        clientUUID = createClientByAdmin("client-jwt-default-alg", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        });
        client = getClientByAdmin(clientUUID);
        clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg().toString());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getUserInfoSignedResponseAlg().toString());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        Assert.assertEquals(Algorithm.PS256, client.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));
        Assert.assertEquals(Algorithm.PS256, client.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

    }

    @Test
    public void testFAPICIBALoginWithPrivateKeyJWT() throws Exception {
        setupPolicyFAPICIBAForAllClient();

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            setClientAuthMethodNeutralSettings(clientRep);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // prepare valid signed authentication request
        AuthorizationEndpointRequestObject requestObject = createFAPIValidAuthorizationEndpointRequestObject(username, bindingMessage);
        String encodedRequestObject = registerSharedAuthenticationRequest(requestObject, clientId, Algorithm.PS256);

        // Get keys of client. Will be used for client authentication and signing of authentication request
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequestWithClientSignedJWT(
                signedJwt, encodedRequestObject, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(response.getStatusCode(), is(equalTo(200)));

        // user Authentication Channel Request
        TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
        AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
        assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
        assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

        // user Authentication Channel completed
        doAuthenticationChannelCallback(testRequest);

        String signedJwt2 = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);

        // user Token Request
        OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequestWithClientSignedJWT(
                signedJwt2, response.getAuthReqId(), () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        verifyBackchannelAuthenticationTokenRequest(tokenRes, clientId, username);

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, username);
    }

    @Test
    public void testFAPICIBAUserAuthenticationCancelled() throws Exception {
        // this test is the same as conformance suite's "fapi-ciba-id1-user-rejects-authentication" test that can only be checked manually
        // by kc-sig-fapi's automated conformance testing environment.
        setupPolicyFAPICIBAForAllClient();

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            setClientAuthMethodNeutralSettings(clientRep);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // prepare valid signed authentication request
        AuthorizationEndpointRequestObject requestObject = createFAPIValidAuthorizationEndpointRequestObject(username, bindingMessage);
        String encodedRequestObject = registerSharedAuthenticationRequest(requestObject, clientId, Algorithm.PS256);

        // Get keys of client. Will be used for client authentication and signing of authentication request
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequestWithClientSignedJWT(
                signedJwt, encodedRequestObject, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(response.getStatusCode(), is(equalTo(200)));

        // user Authentication Channel Request
        TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
        AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
        assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
        assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

        // user Authentication Channel completed
        doAuthenticationChannelCallbackCancelled(testRequest);

        String signedJwt2 = createSignedRequestToken(clientId, privateKey, publicKey, org.keycloak.crypto.Algorithm.PS256);

        // user Token Request
        OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequestWithClientSignedJWT(
                signedJwt2, response.getAuthReqId(), () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.ACCESS_DENIED)));
        assertThat(tokenRes.getErrorDescription(), is(equalTo("not authorized")));
    }

    @Test
    public void testFAPICIBALoginWithMTLS() throws Exception {
        setupPolicyFAPICIBAForAllClient();

        // Register client with X509
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn("EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US");
            setClientAuthMethodNeutralSettings(clientRep);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // prepare valid signed authentication request
        AuthorizationEndpointRequestObject requestObject = createFAPIValidAuthorizationEndpointRequestObject(username, bindingMessage);
        String encodedRequestObject = registerSharedAuthenticationRequest(requestObject, clientId, Algorithm.PS256);

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequestWithMTLS(
                clientId, encodedRequestObject, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(response.getStatusCode(), is(equalTo(200)));

        // user Authentication Channel Request
        TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
        AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
        assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
        assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

        // user Authentication Channel completed
        doAuthenticationChannelCallback(testRequest);

        // user Token Request
        OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequestWithMTLS(
                clientId, response.getAuthReqId(), () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        verifyBackchannelAuthenticationTokenRequest(tokenRes, clientId, username);

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, username);
    }

    @Test
    public void testFAPICIBAWithoutBindingMessage() throws Exception {
        setupPolicyFAPICIBAForAllClient();

        // Register client with X509
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn("EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US");
            setClientAuthMethodNeutralSettings(clientRep);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // prepare invalid signed authentication request lacking binding message
        AuthorizationEndpointRequestObject requestObject = createFAPIValidAuthorizationEndpointRequestObject(username, null);

        String encodedRequestObject = registerSharedAuthenticationRequest(requestObject, clientId, Algorithm.PS256);

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequestWithMTLS(
                clientId, encodedRequestObject, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(response.getStatusCode(), is(equalTo(400)));
        assertThat(response.getError(), is(equalTo(OAuthErrorException.INVALID_REQUEST)));
        assertThat(response.getErrorDescription(), is(equalTo("Missing parameter: binding_message")));
    }

    @Test
    public void testFAPICIBAWithoutSignedAuthenticationRequest() throws Exception {
        setupPolicyFAPICIBAForAllClient();

        // Register client with X509
        String clientUUID = createClientByAdmin("foo", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn("EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US");
            setClientAuthMethodNeutralSettings(clientRep);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        AuthenticationRequestAcknowledgement response = doInvalidBackchannelAuthenticationRequestWithMTLS(clientId, username, bindingMessage, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertThat(response.getStatusCode(), is(equalTo(400)));
        assertThat(response.getError(), is(equalTo(OAuthErrorException.INVALID_REQUEST)));
        assertThat(response.getErrorDescription(), is(equalTo("Missing parameter: 'request' or 'request_uri'")));
    }

    private void setupPolicyFAPICIBAForAllClient() throws Exception {
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable FAPI CIBA for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(FAPI_CIBA_PROFILE_NAME)
                        .addProfile(FAPI1_ADVANCED_PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    private void setClientAuthMethodNeutralSettings(ClientRepresentation clientRep) {
        // for keycloak to get client key to verify signed authentication request by client
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        // activate CIBA grant for client
        Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
        attributes.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "poll");
        attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        clientRep.setAttributes(attributes);
    }

    private AuthorizationEndpointRequestObject createValidAuthorizationEndpointRequestObject(String username, String bindingMessage) throws Exception {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.setScope("openid");
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
        requestObject.setLoginHint(username);
        requestObject.setBindingMessage(bindingMessage);
        return requestObject;
    }

    private AuthorizationEndpointRequestObject createFAPIValidAuthorizationEndpointRequestObject(String username, String bindingMessage) throws Exception {
    	AuthorizationEndpointRequestObject requestObject = createValidAuthorizationEndpointRequestObject(username, bindingMessage);
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(requestObject.getIat());
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME));
        requestObject.issuer(clientId);
        requestObject.id(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        return requestObject;
    }

    private String registerSharedAuthenticationRequest(AuthorizationEndpointRequestObject requestObject, String clientId, String sigAlg) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.generateKeys(sigAlg);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg);

        return oidcClientEndpointsResource.getOIDCRequest();
    }

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequestWithClientSignedJWT(
            String signedJwt, String request, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_PARAM, request));
            CloseableHttpResponse response = sendRequest(oauth.getBackchannelAuthenticationUrl(), parameters, httpClientSupplier);
            return new AuthenticationRequestAcknowledgement(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequestWithMTLS(
            String clientId, String request, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_PARAM, request));
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.CLIENT_ID_PARAM, clientId));
            CloseableHttpResponse response = sendRequest(oauth.getBackchannelAuthenticationUrl(), parameters, httpClientSupplier);
            return new AuthenticationRequestAcknowledgement(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AuthenticationRequestAcknowledgement doInvalidBackchannelAuthenticationRequestWithMTLS(
            String clientId, String username, String bindingMessage, Supplier<CloseableHttpClient> httpClientSupplier) throws Exception {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.CLIENT_ID_PARAM, clientId));
            parameters.add(new BasicNameValuePair(LOGIN_HINT_PARAM, username));
            parameters.add(new BasicNameValuePair(BINDING_MESSAGE, bindingMessage));
            parameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID));
            CloseableHttpResponse response = sendRequest(oauth.getBackchannelAuthenticationUrl(), parameters, httpClientSupplier);
            return new AuthenticationRequestAcknowledgement(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TestAuthenticationChannelRequest doAuthenticationChannelRequest(String bindingMessage) {
        // get Authentication Channel Request keycloak has done on Backchannel Authentication Endpoint from the FIFO queue of testing Authentication Channel Request API
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        TestAuthenticationChannelRequest authenticationChannelReq = oidcClientEndpointsResource.getAuthenticationChannel(bindingMessage);
        return authenticationChannelReq;
    }

    private EventRepresentation doAuthenticationChannelCallback(TestAuthenticationChannelRequest request) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(request.getBearerToken(), SUCCEED);
        assertThat(statusCode, is(equalTo(200)));
        // check login event : ignore user id and other details except for username
        EventRepresentation representation = new EventRepresentation();

        representation.setDetails(Collections.emptyMap());

        return representation;
    }

    private EventRepresentation doAuthenticationChannelCallbackCancelled(TestAuthenticationChannelRequest request) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(request.getBearerToken(), CANCELLED);
        assertThat(statusCode, is(equalTo(200)));
        // check login event : ignore user id and other details except for username
        EventRepresentation representation = new EventRepresentation();

        representation.setDetails(Collections.emptyMap());

        return representation;
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequestWithClientSignedJWT(
            String signedJwt, String authReqId, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(AUTH_REQ_ID, authReqId));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));
            CloseableHttpResponse response = sendRequest(oauth.getBackchannelAuthenticationTokenRequestUrl(), parameters, httpClientSupplier);
            return new OAuthClient.AccessTokenResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequestWithMTLS(
            String clientId, String authReqId, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE));
            parameters.add(new BasicNameValuePair(AUTH_REQ_ID, authReqId));
            parameters.add(new BasicNameValuePair(OIDCLoginProtocol.CLIENT_ID_PARAM, clientId));
            CloseableHttpResponse response = sendRequest(oauth.getBackchannelAuthenticationTokenRequestUrl(), parameters, httpClientSupplier);
            return new OAuthClient.AccessTokenResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyBackchannelAuthenticationTokenRequest(OAuthClient.AccessTokenResponse tokenRes, String clientId, String username) {
        assertThat(tokenRes.getStatusCode(), is(equalTo(200)));
        events.expectAuthReqIdToToken(null, null).clearDetails().user(AssertEvents.isUUID()).client(clientId).assertEvent();

        AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
        assertThat(accessToken.getIssuedFor(), is(equalTo(clientId)));
        Assert.assertNotNull(accessToken.getCertConf().getCertThumbprint());


        RefreshToken refreshToken = oauth.parseRefreshToken(tokenRes.getRefreshToken());
        assertThat(refreshToken.getIssuedFor(), is(equalTo(clientId)));
        assertThat(refreshToken.getAudience()[0], is(equalTo(refreshToken.getIssuer())));

        IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
        assertThat(idToken.getPreferredUsername(), is(equalTo(username)));
        assertThat(idToken.getIssuedFor(), is(equalTo(clientId)));
        assertThat(idToken.getAudience()[0], is(equalTo(idToken.getIssuedFor())));
    }

    private void logoutUserAndRevokeConsent(String clientId, String username) {
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(REALM_NAME), username);
        user.logout();
        List<Map<String, Object>> consents = user.getConsents();
        org.junit.Assert.assertEquals(1, consents.size());
        user.revokeConsent(clientId);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters, Supplier<CloseableHttpClient> httpClientSupplier) throws Exception {
        CloseableHttpClient client = httpClientSupplier.get();
        try {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);
            return client.execute(post);
        } finally {
            oauth.closeClient(client);
        }
    }
}
