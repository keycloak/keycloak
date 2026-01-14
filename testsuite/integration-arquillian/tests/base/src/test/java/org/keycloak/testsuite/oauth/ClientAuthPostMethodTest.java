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

package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for "client_secret_post" client authentication (clientID + clientSecret sent in the POST body instead of in "Authorization: Basic" header)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthPostMethodTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }


    @Test
    public void testPostAuthentication() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = doAccessTokenRequestPostAuth(code, "password");

        assertEquals(200, response.getStatusCode());

        assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.parseRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());
    }

    @Test
    public void testBasicAuthenticationNotAllowedWhenPostRequested() {
        // Update client to request client_secret_post client authentication method
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), oauth.getClientId());
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRep).setClientSecretAuthenticationAllowedMethod(OIDCLoginProtocol.CLIENT_SECRET_POST);
        client.update(clientRep);

        try {
            // client_secret_basic should not work
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse errorResponse = oauth.doAccessTokenRequest(code);
            assertNull(errorResponse.getAccessToken());
            assertEquals("Invalid method used to get client secret. Client requires method 'client_secret_post' to obtain client secret from the request", errorResponse.getErrorDescription());

            // Try with client_secret_post. Should work
            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response = doAccessTokenRequestPostAuth(code, "password");
            assertEquals(200, response.getStatusCode());
        } finally {
            // Revert
            OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRep).setClientSecretAuthenticationAllowedMethod(null);
            client.update(clientRep);
        }
    }

    @Test
    public void testPostAuthenticationNotAllowedWhenBasicRequested() {
        // Update client to request client_secret_basic client authentication method
        ClientResource client = ApiUtil.findClientByClientId(adminClient.realm("test"), oauth.getClientId());
        ClientRepresentation clientRep = client.toRepresentation();
        OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRep).setClientSecretAuthenticationAllowedMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
        client.update(clientRep);

        try {
            // client_secret_post should not work
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse errorResponse = doAccessTokenRequestPostAuth(code, "password");
            assertNull(errorResponse.getAccessToken());
            assertEquals("Invalid method used to get client secret. Client requires method 'client_secret_basic' to obtain client secret from the request", errorResponse.getErrorDescription());

            // Try with client_secret_basic. Should work
            oauth.openLoginForm();
            code = oauth.parseLoginResponse().getCode();

            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            assertEquals(200, response.getStatusCode());
        } finally {
            // Revert
            OIDCClientSecretConfigWrapper.fromClientRepresentation(clientRep).setClientSecretAuthenticationAllowedMethod(null);
            client.update(clientRep);
        }
    }


    private AccessTokenResponse doAccessTokenRequestPostAuth(String code, String clientSecret) {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getEndpoints().getToken());

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));

            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));


            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);

            try {
                return new AccessTokenResponse(client.execute(post));
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve access token", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
