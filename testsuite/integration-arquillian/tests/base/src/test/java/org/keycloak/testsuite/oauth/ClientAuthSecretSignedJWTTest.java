/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.client.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@EnableFeature(value = Profile.Feature.CLIENT_SECRET_ROTATION)
public class ClientAuthSecretSignedJWTTest extends AbstractKeycloakTest {

    private static final Logger logger = Logger.getLogger(ClientAuthSecretSignedJWTTest.class);
    private static final String REALM_NAME = "test";
    private static final String PROFILE_NAME = "ClientSecretRotationProfile";
    private static final String POLICY_NAME = "ClientSecretRotationPolicy";
    private static final String OIDC = "openid-connect";

    // BCFIPS approved mode requires at least 112 bits (14 characters) long SecretKey for "client-secret-jwt" authentication
    private static final String CLIENT_SECRET = "atleast-14chars-password";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/client-auth-test/testrealm-jwt-client-secret.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    // TEST SUCCESS

    @Test
    public void testCodeToTokenRequestSuccess() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.HS256);
    }

    @Test
    public void testCodeToTokenRequestSuccessHS384() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.HS384);
    }

    @Test
    public void testCodeToTokenRequestSuccessHS512() throws Exception {
        testCodeToTokenRequestSuccess(Algorithm.HS512);
    }


    // Issue 34547
    @Test
    public void testCodeToTokenRequestSuccessWhenClientHasGeneratedKeys() throws Exception {
        // Test when client has public/private keys generated despite the fact that it uses client-secret for the client authentication (and not those keys)
        ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app").getCertficateResource("jwt.credential").generate();

        testCodeToTokenRequestSuccess(Algorithm.HS256);
    }

    @Test
    public void testCodeToTokenRequestFailureWhenClientHasPrivateKeyJWT() throws Exception {
        // Setup client for "private_key_jwt" authentication
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app");
        client.getCertficateResource("jwt.credential").generate();
        ClientRepresentation clientRep = client.toRepresentation();
        clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        client.update(clientRep);

        // Client should not be able to authenticate with "client_secret_jwt"
        try {
            oauth.clientId("test-app");
            oauth.doLogin("test-user@localhost", "password");
            events.expectLogin().client("test-app").assertEvent();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(CLIENT_SECRET, 20, Algorithm.HS256));
            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        } finally {
            clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            client.update(clientRep);
        }
    }

    @Test
    public void testInvalidIssuer() throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.parseLoginResponse().getCode();
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider() {
            @Override
            protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
                JsonWebToken jwt = super.createRequestToken(clientId, realmInfoUrl);

                jwt.issuer("bad-issuer");

                return jwt;
            }
        };
        String algorithm = Algorithm.HS256;
        jwtProvider.setClientSecret(CLIENT_SECRET, algorithm);
        String jwt = jwtProvider.createSignedRequestToken(oauth.getClientId(), getRealmInfoUrl(), algorithm);
        AccessTokenResponse response = doAccessTokenRequest(code,
                jwt);

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_client", response.getError());
    }

    @Test
    public void testCodeToTokenRequestFailureHS384Enforced() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        final String realmName = "test";
        final String clientId = "test-app";
        try {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(realmName), clientId);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(Algorithm.HS384);
            clientResource.update(clientRep);

            testCodeToTokenRequestSuccess(Algorithm.HS384);
        } catch (Exception e) {
            fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(realmName), clientId);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(null);
            clientResource.update(clientRep);
        }
    }

    @Test
    public void testCodeToTokenRequestFailureHS512Enforced() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        final String realmName = "test";
        final String clientId = "test-app";
        try {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(realmName), clientId);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(Algorithm.HS512);
            clientResource.update(clientRep);

            oauth.clientId(clientId);
            oauth.doLogin("test-user@localhost", "password");
            events.expectLogin().client(clientId).assertEvent();

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(CLIENT_SECRET, 20, Algorithm.HS256));
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_client", response.getError());
        } catch (Exception e) {
            fail();
        } finally {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(realmName), clientId);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(null);
            clientResource.update(clientRep);
        }
    }

    private void testCodeToTokenRequestSuccess(String algorithm) throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("test-app")
                .assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(CLIENT_SECRET, 20, algorithm));

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        oauth.parseRefreshToken(response.getRefreshToken());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(oauth.getClientId())
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID)
                .assertEvent();
    }

    /**
     * When there is a client secret rotation, the client must be able to authenticate itself by the rotated secret and the new secret. (As long as both secrets remain valid)
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS256() throws Exception {
        processAuthenticateWithAlgorithm(Algorithm.HS256, SecretGenerator.SECRET_LENGTH_256_BITS);
    }

    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS384() throws Exception {
        processAuthenticateWithAlgorithm(Algorithm.HS384, SecretGenerator.SECRET_LENGTH_384_BITS);
    }

    @Test
    public void authenticateWithValidClientSecretWhenRotationPolicyIsEnableForHS512() throws Exception {
        processAuthenticateWithAlgorithm(Algorithm.HS512, SecretGenerator.SECRET_LENGTH_512_BITS);
    }

    private void processAuthenticateWithAlgorithm(String algorithm, Integer secretLength) throws Exception{
        String cidConfidential= createClientByAdmin("jwt-client","jwt-client",CLIENT_SECRET,algorithm);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
        configureDefaultProfileAndPolicy();

        String firstSecret = clientResource.generateNewSecret().getValue(); //clientResource.getSecret().getValue();
        assertThat(firstSecret.length(), is(SecretGenerator.equivalentEntropySize(secretLength, SecretGenerator.ALPHANUM.length)));

        //generate new secret, rotate the secret
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));
        assertThat(newSecret.length(), is(SecretGenerator.equivalentEntropySize(secretLength, SecretGenerator.ALPHANUM.length)));

        oauth.clientId("jwt-client");
        oauth.doLogin("test-user@localhost", "password");
        events.expectLogin().client("jwt-client").assertEvent();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(firstSecret, 20, algorithm));
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
    }

    // TEST ERRORS

    @Test
    public void testAssertionInvalidSignature() throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("test-app")
                .assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT("ppassswordd", 20));

        // https://tools.ietf.org/html/rfc6749#section-5.2
        assertEquals(400, response.getStatusCode());
        assertEquals("unauthorized_client", response.getError());
    }

    @Test
    public void testAssertionReuse() throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("test-app")
                .assertEvent();

        String code = oauth.parseLoginResponse().getCode();
        String clientSignedJWT = getClientSignedJWT(CLIENT_SECRET, 20);

        AccessTokenResponse response = doAccessTokenRequest(code, clientSignedJWT);
        assertEquals(200, response.getStatusCode());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(oauth.getClientId())
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID)
                .assertEvent();


        // 2nd attempt to use same clientSignedJWT should fail
        oauth.openLoginForm();
        loginEvent = events.expectLogin()
                .client("test-app")
                .assertEvent();

        String code2 = oauth.parseLoginResponse().getCode();
        response = doAccessTokenRequest(code2, clientSignedJWT);
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .error("invalid_client_credentials")
                .clearDetails()
                .user((String) null)
                .session((String) null)
                .assertEvent();


        assertEquals(OAuthErrorException.INVALID_CLIENT, response.getError());
        assertEquals(400, response.getStatusCode());
    }

    /**
     * After a secret rotation the client should not be able to authenticate after the rotated secret expires
     *
     * @throws Exception
     */
    @Test
    public void authenticateWithInvalidRotatedClientSecretPolicyIsEnable() throws Exception {
        String cidConfidential= createClientByAdmin("jwt-client","jwt-client",CLIENT_SECRET,Algorithm.HS256);
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cidConfidential);
        configureDefaultProfileAndPolicy();
        String firstSecret = clientResource.getSecret().getValue();

        //generate new secret, rotate the secret
        String newSecret = clientResource.generateNewSecret().getValue();
        assertThat(firstSecret, not(equalTo(newSecret)));

        //force rotated secret expiration
        setTimeOffset(31);

        oauth.clientId("jwt-client");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().client("jwt-client").assertEvent();
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(firstSecret, 20, Algorithm.HS256));
        assertThat(response.getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));

    }

    private String getClientSignedJWT(String secret, int timeout) {
        return getClientSignedJWT(secret, timeout, Algorithm.HS256);
    }

    private String getClientSignedJWT(String secret, int timeout, String algorithm) {
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider();
        jwtProvider.setClientSecret(secret, algorithm);
        return jwtProvider.createSignedRequestToken(oauth.getClientId(), getRealmInfoUrl(), algorithm);
    }

    private String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build("test").toString();
    }

    private AccessTokenResponse doAccessTokenRequest(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        try (CloseableHttpClient client = new DefaultHttpClient()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }

    //support methods
    private void configureDefaultProfileAndPolicy() throws Exception {
        // register profiles
        ClientPoliciesUtil.ClientProfileBuilder profileBuilder = new ClientPoliciesUtil.ClientProfileBuilder();
        ClientSecretRotationExecutor.Configuration profileConfig = getClientProfileConfiguration(60, 30, 20);

        doConfigProfileAndPolicy(profileBuilder, profileConfig);
    }

    private void doConfigProfileAndPolicy(ClientPoliciesUtil.ClientProfileBuilder profileBuilder,
                                          ClientSecretRotationExecutor.Configuration profileConfig) throws Exception {
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                profileBuilder.createProfile(PROFILE_NAME, "Enable Client Secret Rotation")
                        .addExecutor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                        .toRepresentation()).toString();
        updateProfiles(json);

        // register policies
        ClientAccessTypeCondition.Configuration config = new ClientAccessTypeCondition.Configuration();
        config.setType(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL));
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME,
                                "Policy for Client Secret Rotation",
                                Boolean.TRUE).addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, config)
                        .addProfile(PROFILE_NAME).toRepresentation()).toString();
        updatePolicies(json);
    }

    protected void updateProfiles(String json) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json,
                    ClientProfilesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesProfilesResource()
                    .updateProfiles(clientProfiles);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update profiles failed",
                    e.getResponse().getStatusInfo().toString());
        } catch (Exception e) {
            throw new ClientPolicyException("update profiles failed", e.getMessage());
        }
    }

    protected void updateProfiles(ClientProfilesRepresentation reps) throws ClientPolicyException {
        updateProfiles(convertToProfilesJson(reps));
    }

    protected void updatePolicies(String json) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = json == null ? null
                    : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource()
                    .updatePolicies(clientPolicies);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed",
                    e.getResponse().getStatusInfo().toString());
        } catch (IOException e) {
            throw new ClientPolicyException("update policies failed", e.getMessage());
        }
    }

    protected String convertToProfilesJson(ClientProfilesRepresentation reps) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    @NotNull
    private ClientSecretRotationExecutor.Configuration getClientProfileConfiguration(
            int expirationPeriod, int rotatedExpirationPeriod, int remainExpirationPeriod) {
        ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
        profileConfig.setExpirationPeriod(expirationPeriod);
        profileConfig.setRotatedExpirationPeriod(rotatedExpirationPeriod);
        profileConfig.setRemainExpirationPeriod(remainExpirationPeriod);
        return profileConfig;
    }

    protected String createClientByAdmin(String clientId, String clientName, String clientSecret,String signAlg) throws ClientPolicyException {
        ClientRepresentation clientRep = getClientRepresentation(clientId, clientName, clientSecret,signAlg);

        Response resp = adminClient.realm(REALM_NAME).clients().create(clientRep);
        if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            String respBody = resp.readEntity(String.class);
            Map<String, String> responseJson = null;
            try {
                responseJson = JsonSerialization.readValue(respBody, Map.class);
            } catch (IOException e) {
                fail();
            }
            throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR),
                    responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
        }
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String cId = ApiUtil.getCreatedId(resp);
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(cId);
        return cId;
    }

    @NotNull
    private ClientRepresentation getClientRepresentation(String clientId, String clientName, String clientSecret, String signAlg) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientId);
        clientRep.setName(clientName);
        clientRep.setSecret(clientSecret);
        clientRep.setAttributes(new HashMap<>());
        clientRep.getAttributes()
                .put(ClientSecretConstants.CLIENT_SECRET_CREATION_TIME,
                        String.valueOf(Time.currentTime()));
        clientRep.setProtocol(OIDC);
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setStandardFlowEnabled(Boolean.TRUE);
        clientRep.setImplicitFlowEnabled(Boolean.TRUE);
        clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
        clientRep.getAttributes().put(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG, signAlg);

        clientRep.setRedirectUris(Collections.singletonList(
                ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        return clientRep;
    }
}
