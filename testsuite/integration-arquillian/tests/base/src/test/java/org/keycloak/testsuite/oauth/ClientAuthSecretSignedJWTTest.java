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

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.authentication.JWTClientSecretCredentialsProvider;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.OAuthClient;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class ClientAuthSecretSignedJWTTest extends AbstractKeycloakTest {

    private static final Logger logger = Logger.getLogger(ClientAuthSecretSignedJWTTest.class);

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

    @Test
    public void testInvalidIssuer() throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        JWTClientSecretCredentialsProvider jwtProvider = new JWTClientSecretCredentialsProvider() {
            @Override
            protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
                JsonWebToken jwt = super.createRequestToken(clientId, realmInfoUrl);

                jwt.issuer("bad-issuer");

                return jwt;
            }
        };
        String algorithm = Algorithm.HS256;
        jwtProvider.setClientSecret("password", algorithm);
        String jwt = jwtProvider.createSignedRequestToken(oauth.getClientId(), getRealmInfoUrl(), algorithm);
        OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code,
                jwt);

        assertEquals(400, response.getStatusCode());
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
            Assert.fail();
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
        final String clientSecret = "password";
        try {
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(realmName), clientId);
            clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setTokenEndpointAuthSigningAlg(Algorithm.HS512);
            clientResource.update(clientRep);

            oauth.clientId(clientId);
            oauth.doLogin("test-user@localhost", clientSecret);
            events.expectLogin().client(clientId).assertEvent();

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT(clientSecret, 20, Algorithm.HS256));
            assertEquals(400, response.getStatusCode());
            assertEquals("invalid_client", response.getError());
        } catch (Exception e) {
            Assert.fail();
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

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT("password", 20, algorithm));

        assertEquals(200, response.getStatusCode());
        oauth.verifyToken(response.getAccessToken());
        oauth.parseRefreshToken(response.getRefreshToken());
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .client(oauth.getClientId())
                .detail(Details.CLIENT_AUTH_METHOD, JWTClientSecretAuthenticator.PROVIDER_ID)
                .assertEvent();
    }

    // TEST ERRORS

    @Test
    public void testAssertionInvalidSignature() throws Exception {
        oauth.clientId("test-app");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin()
                .client("test-app")
                .assertEvent();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code, getClientSignedJWT("ppassswordd", 20));
        
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

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        String clientSignedJWT = getClientSignedJWT("password", 20);

        OAuthClient.AccessTokenResponse response = doAccessTokenRequest(code, clientSignedJWT);
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

        String code2 = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        response = doAccessTokenRequest(code2, clientSignedJWT);
        events.expectCodeToToken(loginEvent.getDetails().get(Details.CODE_ID), loginEvent.getSessionId())
                .error("invalid_client_credentials")
                .clearDetails()
                .user((String) null)
                .session((String) null)
                .assertEvent();


        assertEquals(400, response.getStatusCode());
        assertEquals("unauthorized_client", response.getError());
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

    private OAuthClient.AccessTokenResponse doAccessTokenRequest(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getAccessTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
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
